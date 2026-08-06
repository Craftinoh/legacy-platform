package fr.xephi.authme.message.locale;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;

/**
 * Optional, lazily resolved bridge to the network's {@code PlayerLanguageProvider} service
 * (registered by LegacyLobby through the Bukkit {@code ServicesManager}).
 * <p>
 * The hook is deliberately implemented with reflection and has <b>no</b> compile-time or runtime
 * dependency on LegacyLobby or on {@code language-common}:
 * <ul>
 *   <li>AuthMe starts normally when the service was never registered.</li>
 *   <li>AuthMe starts normally when LegacyLobby is not installed at all.</li>
 *   <li>The service may be registered after AuthMe has been enabled; it is looked up lazily
 *       on every request, so late registration is picked up without a restart.</li>
 *   <li>If the provider throws, the failure is swallowed and the caller falls back to the
 *       next language source.</li>
 * </ul>
 * The service {@link Class} is taken from the {@code ServicesManager} itself rather than being
 * loaded by name from AuthMe's own class loader. This is what makes the lookup correct: LegacyLobby
 * shades {@code language-common} into its own jar, so the registered interface belongs to
 * LegacyLobby's plugin class loader. Loading a second copy of the interface here would produce a
 * different {@code Class} object, and {@code ServicesManager.load()} — which keys services by
 * {@code Class} identity — would never match it.
 */
public class NetworkLanguageProviderHook implements Reloadable {

    private static final String SERVICE_CLASS_NAME = "it.legacynetwork.language.PlayerLanguageProvider";
    private static final String GET_LANGUAGE_METHOD = "getLanguage";
    private static final String GET_CODE_METHOD = "getCode";

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(NetworkLanguageProviderHook.class);

    /** Cached reflective handles; all reset by {@link #reload()}. */
    private Class<?> serviceClass;
    private Method getLanguageMethod;
    private Method getCodeMethod;
    /** Guards against spamming the console when the provider misbehaves. */
    private boolean failureLogged;

    NetworkLanguageProviderHook() {
    }

    /**
     * Returns the language code the network provider associates with the given player.
     *
     * @param playerId the unique id of the player
     * @return the language code (e.g. {@code "it"}), or null when unavailable
     */
    public String getLanguageCode(UUID playerId) {
        if (playerId == null) {
            return null;
        }

        try {
            Class<?> service = resolveServiceClass();
            if (service == null) {
                return null;
            }

            Object provider = loadService(service);
            if (provider == null) {
                return null;
            }

            if (getLanguageMethod == null) {
                getLanguageMethod = service.getMethod(GET_LANGUAGE_METHOD, UUID.class);
            }
            Object language = getLanguageMethod.invoke(provider, playerId);
            if (language == null) {
                return null;
            }

            if (getCodeMethod == null || !getCodeMethod.getDeclaringClass().isInstance(language)) {
                getCodeMethod = language.getClass().getMethod(GET_CODE_METHOD);
            }
            Object code = getCodeMethod.invoke(language);
            return code == null ? null : code.toString();
        } catch (Exception | NoClassDefFoundError e) {
            // The provider is optional: never let a broken or absent hook break message sending.
            if (!failureLogged) {
                failureLogged = true;
                logger.warning("Could not read the language from the network PlayerLanguageProvider; "
                    + "falling back to the configured language. Reason: " + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
            }
            invalidateHandles();
            return null;
        }
    }

    /**
     * Finds the registered service interface among the services known to Bukkit, matching it by
     * fully qualified name so that the {@code Class} identity is the one used at registration time.
     *
     * @return the service class, or null if no such service is registered
     */
    private Class<?> resolveServiceClass() {
        if (serviceClass != null) {
            return serviceClass;
        }
        Collection<Class<?>> knownServices = getKnownServices();
        if (knownServices == null) {
            return null;
        }
        String expectedName = getServiceClassName();
        for (Class<?> knownService : knownServices) {
            if (expectedName.equals(knownService.getName())) {
                serviceClass = knownService;
                return serviceClass;
            }
        }
        return null;
    }

    /**
     * Returns the fully qualified name of the service interface to look for. Extracted for testing.
     *
     * @return the service interface name
     */
    protected String getServiceClassName() {
        return SERVICE_CLASS_NAME;
    }

    /**
     * Returns the service classes known to the Bukkit services manager. Extracted for testing.
     *
     * @return the known service classes, or null when Bukkit is unavailable
     */
    protected Collection<Class<?>> getKnownServices() {
        if (Bukkit.getServer() == null) {
            return null;
        }
        return Bukkit.getServicesManager().getKnownServices();
    }

    /**
     * Loads the registered provider instance for the given service class. Extracted for testing.
     *
     * @param service the service class to load a provider for
     * @return the provider instance, or null if none is registered
     */
    protected Object loadService(Class<?> service) {
        return Bukkit.getServicesManager().load(service);
    }

    /**
     * Drops the cached reflective handles so that a re-registered — or newly registered —
     * service is resolved from scratch. Called on {@code /authme reload} and on disable.
     */
    @Override
    public void reload() {
        invalidateHandles();
        failureLogged = false;
    }

    private void invalidateHandles() {
        serviceClass = null;
        getLanguageMethod = null;
        getCodeMethod = null;
    }
}
