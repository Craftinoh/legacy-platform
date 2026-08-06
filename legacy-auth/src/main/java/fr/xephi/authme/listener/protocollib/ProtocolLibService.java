package fr.xephi.authme.listener.protocollib;

import ch.jalu.injector.annotations.NoFieldScan;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RestrictionSettings;

import javax.inject.Inject;

/**
 * Manages the ProtocolLib based features.
 * <p>
 * LegacyAuth removed the inventory hiding feature that used to live here, so the only remaining
 * packet adapter is the one denying tab completion before login.
 */
@NoFieldScan
public class ProtocolLibService implements SettingsDependent {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(ProtocolLibService.class);

    /* Packet Adapters */
    private TabCompletePacketAdapter tabCompletePacketAdapter;

    /* Settings */
    private boolean denyTabCompleteBeforeLogin;

    /* Service */
    private boolean isEnabled;
    private final AuthMe plugin;
    private final PlayerCache playerCache;

    @Inject
    ProtocolLibService(AuthMe plugin, Settings settings, PlayerCache playerCache) {
        this.plugin = plugin;
        this.playerCache = playerCache;
        reload(settings);
    }

    /**
     * Set up the ProtocolLib packet adapters.
     */
    public void setup() {
        // Check if ProtocolLib is enabled on the server.
        if (!plugin.getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            if (denyTabCompleteBeforeLogin) {
                logger.warning("WARNING! The denyTabComplete feature requires ProtocolLib! Disabling it...");
            }

            this.isEnabled = false;
            return;
        }

        // Set up packet adapters
        if (denyTabCompleteBeforeLogin) {
            if (tabCompletePacketAdapter == null) {
                tabCompletePacketAdapter = new TabCompletePacketAdapter(plugin, playerCache);
                tabCompletePacketAdapter.register();
            }
        } else if (tabCompletePacketAdapter != null) {
            tabCompletePacketAdapter.unregister();
            tabCompletePacketAdapter = null;
        }

        this.isEnabled = true;
    }

    /**
     * Stops all features based on ProtocolLib.
     */
    public void disable() {
        isEnabled = false;

        if (tabCompletePacketAdapter != null) {
            tabCompletePacketAdapter.unregister();
            tabCompletePacketAdapter = null;
        }
    }

    /**
     * Returns whether the ProtocolLib based features are currently active.
     *
     * @return true if ProtocolLib is available and the features are set up
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void reload(Settings settings) {
        this.denyTabCompleteBeforeLogin = settings.getProperty(RestrictionSettings.DENY_TABCOMPLETE_BEFORE_LOGIN);
        setup();
    }

}
