package it.legacynetwork.combat;

import it.legacynetwork.combat.command.LegacyCombatCommand;
import it.legacynetwork.combat.config.CombatConfig;
import it.legacynetwork.combat.fireball.FireballListener;
import it.legacynetwork.combat.fireball.FireballTracker;
import it.legacynetwork.combat.hit.HitListener;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public final class LegacyCombatPlugin extends JavaPlugin {

    private CombatConfig combatConfig;
    private boolean debug;
    private FireballTracker fireballTracker;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reload();
        fireballTracker = new FireballTracker();
        getServer().getPluginManager().registerEvents(new HitListener(this), this);
        getServer().getPluginManager().registerEvents(new FireballListener(this), this);
        getCommand("legacycombat").setExecutor(new LegacyCombatCommand(this));
        getLogger().info("LegacyCombat inizializzato.");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        if (fireballTracker != null) {
            fireballTracker.cleanup();
        }
    }

    public void reload() {
        reloadConfig();
        combatConfig = CombatConfig.from(getConfig());
        debug = getConfig().getBoolean("debug.enabled", false);
    }

    public CombatConfig getHitConfig() {
        return combatConfig;
    }

    public CombatConfig getFireballConfig() {
        return combatConfig;
    }

    public boolean isDebug() {
        return debug;
    }

    public FireballTracker getFireballTracker() {
        return fireballTracker;
    }
}
