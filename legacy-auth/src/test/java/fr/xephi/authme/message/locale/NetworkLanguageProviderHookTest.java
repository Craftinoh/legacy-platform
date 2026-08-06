package fr.xephi.authme.message.locale;

import fr.xephi.authme.TestHelper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link NetworkLanguageProviderHook}.
 * <p>
 * The hook must never require the Apteris modules to be present: these tests exercise it with the
 * service missing, with a provider that answers, and with a provider that throws, all without any
 * dependency on {@code language-common} or LegacyLobby.
 */
public class NetworkLanguageProviderHookTest {

    private static final String SERVICE_CLASS_NAME = "it.legacynetwork.language.PlayerLanguageProvider";

    @BeforeClass
    public static void setup() {
        TestHelper.setupLogger();
    }

    /** Stands in for {@code it.legacynetwork.language.Language}: only {@code getCode()} is used. */
    public static class FakeLanguage {
        private final String code;

        FakeLanguage(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    /** Stands in for the registered service interface; named as the real one is looked up by name. */
    public interface PlayerLanguageProvider {
        FakeLanguage getLanguage(UUID playerId);
    }

    /**
     * Test double that replaces the Bukkit services manager lookups.
     */
    private static class TestHook extends NetworkLanguageProviderHook {
        private final Collection<Class<?>> knownServices;
        private final Object provider;

        TestHook(Collection<Class<?>> knownServices, Object provider) {
            this.knownServices = knownServices;
            this.provider = provider;
        }

        @Override
        protected Collection<Class<?>> getKnownServices() {
            return knownServices;
        }

        @Override
        protected Object loadService(Class<?> service) {
            return provider;
        }

        @Override
        protected String getServiceClassName() {
            // Stand-in for it.legacynetwork.language.PlayerLanguageProvider, which is not on the
            // test classpath: AuthMe must build and run without the Apteris modules.
            return PlayerLanguageProvider.class.getName();
        }
    }

    @Test
    public void shouldReturnItalianFromProvider() {
        // given
        NetworkLanguageProviderHook hook = createHook(playerId -> new FakeLanguage("it"));

        // when / then
        assertThat(hook.getLanguageCode(UUID.randomUUID()), equalTo("it"));
    }

    @Test
    public void shouldReturnEnglishFromProvider() {
        // given
        NetworkLanguageProviderHook hook = createHook(playerId -> new FakeLanguage("en"));

        // when / then
        assertThat(hook.getLanguageCode(UUID.randomUUID()), equalTo("en"));
    }

    @Test
    public void shouldReturnNullWhenServiceIsNotRegistered() {
        // given - AuthMe running without LegacyLobby: no such service is known to Bukkit
        NetworkLanguageProviderHook hook = new TestHook(Collections.emptyList(), null);

        // when / then
        assertThat(hook.getLanguageCode(UUID.randomUUID()), nullValue());
    }

    @Test
    public void shouldReturnNullWhenServicesManagerIsUnavailable() {
        // given - e.g. called before the server is up
        NetworkLanguageProviderHook hook = new TestHook(null, null);

        // when / then
        assertThat(hook.getLanguageCode(UUID.randomUUID()), nullValue());
    }

    @Test
    public void shouldReturnNullWhenServiceIsKnownButNoProviderIsRegistered() {
        // given
        NetworkLanguageProviderHook hook =
            new TestHook(Collections.singletonList(PlayerLanguageProvider.class), null);

        // when / then
        assertThat(hook.getLanguageCode(UUID.randomUUID()), nullValue());
    }

    @Test
    public void shouldReturnNullWhenProviderThrows() {
        // given
        NetworkLanguageProviderHook hook = createHook(playerId -> {
            throw new IllegalStateException("provider is broken");
        });

        // when / then - the exception must not escape to the caller
        assertThat(hook.getLanguageCode(UUID.randomUUID()), nullValue());
    }

    @Test
    public void shouldReturnNullWhenProviderYieldsNoLanguage() {
        // given
        NetworkLanguageProviderHook hook = createHook(playerId -> null);

        // when / then
        assertThat(hook.getLanguageCode(UUID.randomUUID()), nullValue());
    }

    @Test
    public void shouldReturnNullForNullPlayerId() {
        // given
        NetworkLanguageProviderHook hook = createHook(playerId -> new FakeLanguage("it"));

        // when / then
        assertThat(hook.getLanguageCode(null), nullValue());
    }

    @Test
    public void shouldPickUpProviderRegisteredAfterAFailedLookup() {
        // given - first no service is registered, then one appears (late plugin enable)
        final Collection<Class<?>> knownServices = new java.util.ArrayList<>();
        NetworkLanguageProviderHook hook = new TestHook(knownServices,
            (PlayerLanguageProvider) playerId -> new FakeLanguage("it"));
        assertThat(hook.getLanguageCode(UUID.randomUUID()), nullValue());

        // when
        knownServices.add(PlayerLanguageProvider.class);

        // then - resolution is lazy, so no restart is needed
        assertThat(hook.getLanguageCode(UUID.randomUUID()), equalTo("it"));
    }

    @Test
    public void shouldResolveAgainAfterReload() {
        // given
        NetworkLanguageProviderHook hook = createHook(playerId -> new FakeLanguage("it"));
        assertThat(hook.getLanguageCode(UUID.randomUUID()), equalTo("it"));

        // when
        hook.reload();

        // then - the cached handles were dropped but resolution still works
        assertThat(hook.getLanguageCode(UUID.randomUUID()), equalTo("it"));
    }

    @Test
    public void shouldIgnoreUnrelatedServices() {
        // given - other plugins' services must not be mistaken for the language provider
        NetworkLanguageProviderHook hook =
            new TestHook(Collections.singletonList(Runnable.class), (Runnable) () -> { });

        // when / then
        assertThat(hook.getLanguageCode(UUID.randomUUID()), nullValue());
    }

    @Test
    public void shouldLookUpTheNetworkServiceByItsFullyQualifiedName() {
        // given / when - the production hook, not the test double
        NetworkLanguageProviderHook hook = new NetworkLanguageProviderHook();

        // then - the name must stay in sync with language-common, which AuthMe does not depend on
        assertThat(hook.getServiceClassName(), equalTo(SERVICE_CLASS_NAME));
    }

    private static NetworkLanguageProviderHook createHook(PlayerLanguageProvider provider) {
        return new TestHook(Collections.singletonList(PlayerLanguageProvider.class), provider);
    }
}
