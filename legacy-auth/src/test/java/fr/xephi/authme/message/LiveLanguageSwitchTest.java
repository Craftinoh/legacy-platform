package fr.xephi.authme.message;

import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.message.locale.ClientLocaleProvider;
import fr.xephi.authme.message.locale.LocaleResolver;
import fr.xephi.authme.message.locale.NetworkLanguageProviderHook;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import fr.xephi.authme.settings.properties.ApterisLanguageSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verifies that a language change made while a player is in limbo is honoured by the very next
 * message of a repeating or delayed task, without a reconnect, a reload, or the task being
 * recreated.
 * <p>
 * This is the regression test for the bug where {@link MessageTask} stored an already translated
 * string: the prompt kept being sent in the language the player had when they joined.
 */
public class LiveLanguageSwitchTest {

    /** Provider whose answer can be changed between task runs, like the network /lang command does. */
    private static final class MutableProvider {
        private String language;

        void setLanguage(String language) {
            this.language = language;
        }
    }

    private MutableProvider provider;
    private LocaleResolver localeResolver;
    private Messages messages;
    private MessagesFileHandler fileHandler;

    @Before
    public void setUpServices() {
        provider = new MutableProvider();

        Settings settings = mock(Settings.class);
        given(settings.getProperty(PluginSettings.MESSAGES_LANGUAGE)).willReturn("en");
        given(settings.getProperty(ApterisLanguageSettings.ENABLED)).willReturn(true);
        given(settings.getProperty(ApterisLanguageSettings.PER_PLAYER_LOCALE)).willReturn(true);
        given(settings.getProperty(ApterisLanguageSettings.USE_NETWORK_PROVIDER)).willReturn(true);
        given(settings.getProperty(ApterisLanguageSettings.USE_CLIENT_LOCALE)).willReturn(false);
        given(settings.getProperty(ApterisLanguageSettings.FALLBACK)).willReturn("en");

        // Stands in for the network service: its answer changes between runs, exactly as it does
        // when a player runs /lang on the proxy.
        NetworkLanguageProviderHook hook = mock(NetworkLanguageProviderHook.class);
        given(hook.getLanguageCode(any(UUID.class))).willAnswer(invocation -> provider.language);

        localeResolver = newLocaleResolver(settings, hook, mock(ClientLocaleProvider.class));

        // The file handler answers with the language it was asked for, so the assertions can show
        // exactly which language each individual send resolved to.
        fileHandler = mock(MessagesFileHandler.class);
        given(fileHandler.getMessage(anyString(), anyString()))
            .willAnswer(invocation -> invocation.getArgument(1) + ":" + invocation.getArgument(0));
        messages = new Messages(fileHandler, localeResolver);
    }

    /**
     * Builds a real {@link LocaleResolver}. Its constructor is package-private in another package,
     * so it is invoked reflectively rather than widening the production visibility for a test.
     *
     * @param settings the settings to use
     * @param hook the network provider hook
     * @param clientLocaleProvider the client locale provider
     * @return a real locale resolver
     */
    private static LocaleResolver newLocaleResolver(Settings settings,
                                                    NetworkLanguageProviderHook hook,
                                                    ClientLocaleProvider clientLocaleProvider) {
        try {
            java.lang.reflect.Constructor<LocaleResolver> constructor =
                LocaleResolver.class.getDeclaredConstructor(
                    Settings.class, NetworkLanguageProviderHook.class, ClientLocaleProvider.class);
            constructor.setAccessible(true);
            return constructor.newInstance(settings, hook, clientLocaleProvider);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create LocaleResolver", e);
        }
    }

    private Player onlinePlayer() {
        Player player = mock(Player.class);
        given(player.getUniqueId()).willReturn(UUID.randomUUID());
        given(player.isOnline()).willReturn(true);
        given(player.getName()).willReturn("Tester");
        given(player.getDisplayName()).willReturn("Tester");
        return player;
    }

    @Test
    public void test1_shouldSwitchRegistrationPromptFromItalianToEnglishOnNextRun() {
        // given - the player joins with Italian
        provider.setLanguage("it");
        Player player = onlinePlayer();
        MessageTask task = new MessageTask(player, messages, MessageKey.REGISTER_MESSAGE);

        // when - the prompt is sent once
        task.run();

        // then - it is Italian
        verify(player).sendMessage(new String[]{"it:registration.register_request"});

        // when - the player switches to English through /lang and the *same* task runs again:
        // no reconnect, no reload, no task recreation
        provider.setLanguage("en");
        task.run();

        // then - the next message is English
        verify(player).sendMessage(new String[]{"en:registration.register_request"});
    }

    @Test
    public void test2_shouldSwitchLoginPromptFromEnglishToRussian() {
        // given
        provider.setLanguage("en");
        Player player = onlinePlayer();
        MessageTask task = new MessageTask(player, messages, MessageKey.LOGIN_MESSAGE);
        task.run();
        verify(player).sendMessage(new String[]{"en:login.login_request"});

        // when
        provider.setLanguage("ru");
        task.run();

        // then
        verify(player).sendMessage(new String[]{"ru:login.login_request"});
    }

    @Test
    public void test3_shouldUseLanguageAtRunTimeNotAtCreationTime() {
        // given - the task is created while the player is Italian...
        provider.setLanguage("it");
        Player player = onlinePlayer();
        MessageTask task = new MessageTask(player, messages, MessageKey.REGISTER_MESSAGE);

        // when - ...but the language changes before it ever runs
        provider.setLanguage("en");
        task.run();

        // then - the message that is actually sent is English
        verify(player).sendMessage(new String[]{"en:registration.register_request"});
    }

    @Test
    public void test4_shouldPickUpProviderRegisteredAfterJoin() {
        // given - no provider answer at join time, so the fallback is used
        provider.setLanguage(null);
        Player player = onlinePlayer();
        MessageTask task = new MessageTask(player, messages, MessageKey.REGISTER_MESSAGE);
        task.run();
        verify(player).sendMessage(new String[]{"en:registration.register_request"});

        // when - the service starts answering later on, without a restart
        provider.setLanguage("it");
        task.run();

        // then
        verify(player).sendMessage(new String[]{"it:registration.register_request"});
    }

    @Test
    public void shouldTranslateCaptchaPromptWithItsCodeOnEveryRun() {
        // given - a repeating prompt that carries a non-localized replacement
        provider.setLanguage("it");
        given(fileHandler.getMessage(anyString(), anyString()))
            .willAnswer(invocation -> invocation.getArgument(1) + ":captcha %captcha_code");
        Player player = onlinePlayer();
        MessageTask task = new MessageTask(
            player, messages, MessageKey.CAPTCHA_FOR_REGISTRATION_REQUIRED, "1234");

        // when
        task.run();
        provider.setLanguage("de");
        task.run();

        // then - the language changes but the replacement is kept
        verify(player).sendMessage(new String[]{"it:captcha 1234"});
        verify(player).sendMessage(new String[]{"de:captcha 1234"});
    }

    @Test
    public void shouldTranslateTimeoutKickMessageWhenItRuns() {
        // given
        provider.setLanguage("it");
        Player player = onlinePlayer();
        PlayerCache playerCache = mock(PlayerCache.class);
        given(playerCache.isAuthenticated("Tester")).willReturn(false);
        TimeoutTask task = new TimeoutTask(player, messages, playerCache);

        // when - the language changes between scheduling and the kick
        provider.setLanguage("en");
        task.run();

        // then
        verify(player).kickPlayer("en:login.timeout_error");
    }

    @Test
    public void shouldNotSendAnythingWhenMuted() {
        // given
        provider.setLanguage("it");
        Player player = onlinePlayer();
        MessageTask task = new MessageTask(player, messages, MessageKey.REGISTER_MESSAGE);
        task.setMuted(true);

        // when
        task.run();

        // then
        org.mockito.Mockito.verify(player, org.mockito.Mockito.never()).sendMessage(any(String[].class));
        org.mockito.Mockito.verifyNoMoreInteractions(fileHandler);
    }

    @Test
    public void shouldNotSendToAPlayerThatWentOffline() {
        // given
        provider.setLanguage("it");
        Player player = onlinePlayer();
        given(player.isOnline()).willReturn(false);
        MessageTask task = new MessageTask(player, messages, MessageKey.REGISTER_MESSAGE);

        // when
        task.run();

        // then
        org.mockito.Mockito.verifyNoMoreInteractions(fileHandler);
    }

    @Test
    public void shouldPreferProviderOverPreviouslyDetectedClientLocale() {
        // given - a client locale was detected earlier, then the provider learns the player's choice
        Player player = onlinePlayer();
        localeResolver.setExplicitLanguage(player.getUniqueId(), "it");
        provider.setLanguage("fr");

        // when / then - the provider wins
        assertThat(localeResolver.getLanguage(player), equalTo("fr"));

        // and when the provider goes silent, the explicit choice is used
        provider.setLanguage(null);
        assertThat(localeResolver.getLanguage(player), equalTo("it"));
    }
}
