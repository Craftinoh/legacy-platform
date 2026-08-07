package it.legacynetwork.chickenwars.game;

import it.legacynetwork.chickenwars.arena.ArenaManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Unico task ripetitivo del plugin.
 *
 * <p>Tutte le arene, i generatori e le galline avanzano da qui: il numero di
 * task registrati resta pari a uno indipendentemente da quante partite siano in
 * corso, come richiesto dai vincoli di prestazione.</p>
 */
public final class GameLoopTask implements Runnable {

    private final ArenaManager arenas;

    private BukkitTask task;

    public GameLoopTask(ArenaManager arenas) {
        this.arenas = arenas;
    }

    /**
     * Avvia il ciclo, se non e' gia' attivo.
     */
    public void start(JavaPlugin plugin) {
        if (task != null) {
            return;
        }
        task = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this, 1L, 1L);
    }

    /**
     * Arresta il ciclo e libera il riferimento al task.
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public boolean isRunning() {
        return task != null;
    }

    @Override
    public void run() {
        arenas.tickAll();
    }
}
