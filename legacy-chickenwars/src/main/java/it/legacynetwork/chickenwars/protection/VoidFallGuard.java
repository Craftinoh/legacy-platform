package it.legacynetwork.chickenwars.protection;

import it.legacynetwork.chickenwars.arena.ArenaDefinition;
import org.bukkit.entity.Player;

/**
 * Condizione unica di caduta nel vuoto.
 *
 * <p>Esiste una sola formula, consultata da tutti i listener: la quota del
 * giocatore confrontata con il {@code voidY} dell'arena piu' una tolleranza
 * configurabile. Nessun altro punto del codice ricalcola la condizione.</p>
 */
public final class VoidFallGuard {

    private volatile double tolerance;

    public VoidFallGuard(double tolerance) {
        setTolerance(tolerance);
    }

    /**
     * Aggiorna la tolleranza dopo un reload.
     *
     * @param tolerance blocchi sopra {@code voidY} entro cui vale la protezione
     */
    public void setTolerance(double tolerance) {
        this.tolerance = Math.max(0.0D, tolerance);
    }

    public double getTolerance() {
        return tolerance;
    }

    /**
     * Indica se il giocatore sta cadendo nel vuoto dell'arena.
     *
     * @param player giocatore da controllare
     * @param arena  arena di riferimento, eventualmente nulla
     * @return {@code true} se la protezione anti-drop deve essere attiva
     */
    public boolean isFallingIntoVoid(Player player, ArenaDefinition arena) {
        if (player == null || arena == null) {
            return false;
        }
        if (player.getWorld() == null) {
            return false;
        }
        return isFallingIntoVoid(player.getWorld().getName(),
                player.getLocation().getY(), arena);
    }

    /**
     * Stessa decisione presa dal listener, espressa su valori semplici.
     *
     * <p>E' il metodo realmente usato in produzione: la variante con
     * {@code Player} si limita a estrarre mondo e quota, quindi i test
     * esercitano l'intera regola, controllo del mondo compreso.</p>
     *
     * @param worldName mondo in cui si trova il giocatore
     * @param playerY   quota del giocatore
     * @param arena     arena di riferimento, eventualmente nulla
     * @return {@code true} se la protezione anti-drop deve attivarsi
     */
    public boolean isFallingIntoVoid(String worldName, double playerY,
                                     ArenaDefinition arena) {
        if (worldName == null || arena == null || arena.getWorld() == null) {
            return false;
        }
        if (!worldName.equalsIgnoreCase(arena.getWorld())) {
            return false;
        }
        return isBelow(playerY, arena.getVoidY(), tolerance);
    }

    /**
     * Confronto puro, estraibile per test automatizzati.
     *
     * @param playerY   coordinata Y del giocatore
     * @param voidY     soglia del vuoto dell'arena
     * @param tolerance tolleranza configurabile (non negativa)
     * @return {@code true} se la protezione anti-drop deve attivarsi
     */
    static boolean isBelow(double playerY, double voidY, double tolerance) {
        return playerY <= voidY + Math.max(0.0D, tolerance);
    }
}
