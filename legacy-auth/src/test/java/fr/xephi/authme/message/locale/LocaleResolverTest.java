package fr.xephi.authme.message.locale;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ApterisLanguageSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link LocaleResolver}.
 */
@RunWith(MockitoJUnitRunner.class)
public class LocaleResolverTest {

    @Mock
    private Settings settings;

    @Mock
    private NetworkLanguageProviderHook networkProviderHook;

    @Mock
    private ClientLocaleProvider clientLocaleProvider;

    @InjectMocks
    private LocaleResolver localeResolver;

    @Before
    public void defaultSettings() {
        given(settings.getProperty(PluginSettings.MESSAGES_LANGUAGE)).willReturn("en");
        given(settings.getProperty(ApterisLanguageSettings.ENABLED)).willReturn(true);
        given(settings.getProperty(ApterisLanguageSettings.PER_PLAYER_LOCALE)).willReturn(true);
        given(settings.getProperty(ApterisLanguageSettings.USE_NETWORK_PROVIDER)).willReturn(true);
        given(settings.getProperty(ApterisLanguageSettings.USE_CLIENT_LOCALE)).willReturn(true);
        given(settings.getProperty(ApterisLanguageSettings.FALLBACK)).willReturn("en");
    }

    @Test
    public void shouldUseItalianFromNetworkProvider() {
        // given
        Player player = mockPlayer();
        given(networkProviderHook.getLanguageCode(player.getUniqueId())).willReturn("it");

        // when / then
        assertThat(localeResolver.getLanguage(player), equalTo("it"));
    }

    @Test
    public void shouldUseEnglishFromNetworkProvider() {
        // given
        Player player = mockPlayer();
        given(networkProviderHook.getLanguageCode(player.getUniqueId())).willReturn("en");

        // when / then
        assertThat(localeResolver.getLanguage(player), equalTo("en"));
    }

    @Test
    public void shouldFallBackToClientLocaleWhenProviderIsAbsent() {
        // given - a hook with no registered service answers null
        Player player = mockPlayer();
        given(networkProviderHook.getLanguageCode(player.getUniqueId())).willReturn(null);
        given(clientLocaleProvider.getLanguageCode(player)).willReturn("it");

        // when / then
        assertThat(localeResolver.getLanguage(player), equalTo("it"));
    }

    @Test
    public void shouldFallBackToConfiguredLanguageWhenNothingIsKnown() {
        // given
        Player player = mockPlayer();
        given(settings.getProperty(ApterisLanguageSettings.FALLBACK)).willReturn("it");
        given(networkProviderHook.getLanguageCode(player.getUniqueId())).willReturn(null);
        given(clientLocaleProvider.getLanguageCode(player)).willReturn(null);

        // when / then
        assertThat(localeResolver.getLanguage(player), equalTo("it"));
    }

    @Test
    public void shouldFallBackToEnglishWhenConfiguredFallbackIsBlank() {
        // given
        Player player = mockPlayer();
        given(settings.getProperty(ApterisLanguageSettings.FALLBACK)).willReturn("  ");
        given(networkProviderHook.getLanguageCode(player.getUniqueId())).willReturn(null);
        given(clientLocaleProvider.getLanguageCode(player)).willReturn(null);

        // when / then
        assertThat(localeResolver.getLanguage(player), equalTo("en"));
    }

    @Test
    public void shouldNormalizeUnknownLanguageCodeWithoutFailing() {
        // given - the resolver does not judge whether a language exists; it normalizes and passes
        // it on, and the message file handler falls back when there is no such translation.
        Player player = mockPlayer();
        given(networkProviderHook.getLanguageCode(player.getUniqueId())).willReturn("  KLINGON ");

        // when / then
        assertThat(localeResolver.getLanguage(player), equalTo("klingon"));
    }

    @Test
    public void shouldUseConfiguredLanguageForConsole() {
        // given
        given(settings.getProperty(PluginSettings.MESSAGES_LANGUAGE)).willReturn("it");
        CommandSender console = mock(CommandSender.class);

        // when / then
        assertThat(localeResolver.getLanguage(console), equalTo("it"));
        assertThat(localeResolver.getConsoleLanguage(), equalTo("it"));
        verifyNoInteractions(networkProviderHook, clientLocaleProvider);
    }

    @Test
    public void shouldUseConfiguredLanguageWhenFeatureIsDisabled() {
        // given
        given(settings.getProperty(ApterisLanguageSettings.PER_PLAYER_LOCALE)).willReturn(false);
        given(settings.getProperty(PluginSettings.MESSAGES_LANGUAGE)).willReturn("de");
        Player player = mockPlayer();

        // when / then
        assertThat(localeResolver.getLanguage(player), equalTo("de"));
        verifyNoInteractions(networkProviderHook, clientLocaleProvider);
    }

    @Test
    public void shouldSkipNetworkProviderWhenSwitchedOff() {
        // given
        given(settings.getProperty(ApterisLanguageSettings.USE_NETWORK_PROVIDER)).willReturn(false);
        Player player = mockPlayer();
        given(clientLocaleProvider.getLanguageCode(player)).willReturn("it");

        // when / then
        assertThat(localeResolver.getLanguage(player), equalTo("it"));
        verifyNoInteractions(networkProviderHook);
    }

    @Test
    public void shouldSkipClientLocaleWhenSwitchedOff() {
        // given
        given(settings.getProperty(ApterisLanguageSettings.USE_CLIENT_LOCALE)).willReturn(false);
        given(settings.getProperty(ApterisLanguageSettings.FALLBACK)).willReturn("en");
        Player player = mockPlayer();
        given(networkProviderHook.getLanguageCode(player.getUniqueId())).willReturn(null);

        // when / then
        assertThat(localeResolver.getLanguage(player), equalTo("en"));
        verifyNoInteractions(clientLocaleProvider);
    }

    @Test
    public void shouldResolveByUuidWhenNoPlayerObjectIsAvailable() {
        // given - the pre-login case: only the unique id is known
        UUID playerId = UUID.randomUUID();
        given(networkProviderHook.getLanguageCode(playerId)).willReturn("it");

        // when / then
        assertThat(localeResolver.getLanguage(playerId), equalTo("it"));
        verifyNoInteractions(clientLocaleProvider);
    }

    @Test
    public void shouldReuseLocaleDetectedEarlierForTheSamePlayer() {
        // given - the locale is detected while the player is online...
        Player player = mockPlayer();
        UUID playerId = player.getUniqueId();
        given(networkProviderHook.getLanguageCode(playerId)).willReturn(null);
        given(clientLocaleProvider.getLanguageCode(player)).willReturn("it");
        assertThat(localeResolver.getLanguage(player), equalTo("it"));

        // when - ...and is later requested without a player object, as during pre-login
        String language = localeResolver.getLanguage(playerId);

        // then
        assertThat(language, equalTo("it"));
    }

    @Test
    public void shouldForgetDetectedLocalesOnReload() {
        // given
        Player player = mockPlayer();
        UUID playerId = player.getUniqueId();
        given(networkProviderHook.getLanguageCode(playerId)).willReturn(null);
        given(clientLocaleProvider.getLanguageCode(player)).willReturn("it");
        localeResolver.getLanguage(player);

        // when
        localeResolver.reload();

        // then - the cache is empty again, so the configured fallback applies
        assertThat(localeResolver.getLanguage(playerId), equalTo("en"));
    }

    @Test
    public void shouldTreatNullSenderAsConsole() {
        // given
        given(settings.getProperty(PluginSettings.MESSAGES_LANGUAGE)).willReturn("it");

        // when / then
        assertThat(localeResolver.getLanguage((CommandSender) null), equalTo("it"));
        assertThat(localeResolver.getLanguage((UUID) null), equalTo("it"));
    }

    private static Player mockPlayer() {
        Player player = mock(Player.class);
        given(player.getUniqueId()).willReturn(UUID.randomUUID());
        return player;
    }
}
