package it.legacynetwork.chickenwars.death;

import java.util.UUID;

/**
 * Descrizione deterministica di una morte da elaborare.
 *
 * <p>Contiene soltanto identificatori e causa: nessun oggetto Bukkit, cosi'
 * l'orchestratore puo' essere verificato interamente con test unitari.</p>
 */
public final class DeathContext {

    private final UUID victimId;
    private final UUID killerId;
    private final DeathCause cause;

    private DeathContext(UUID victimId, UUID killerId, DeathCause cause) {
        if (victimId == null) {
            throw new IllegalArgumentException("UUID vittima mancante");
        }
        this.victimId = victimId;
        this.cause = cause == null ? DeathCause.UNKNOWN : cause;
        // Un giocatore non puo' essere l'uccisore di se stesso: il suicidio
        // viene normalizzato subito, cosi' nessun controllo a valle lo ripete.
        this.killerId = killerId != null && killerId.equals(victimId)
                ? null : killerId;
    }

    /**
     * Morte causata da un evento di gioco, con eventuale ultimo aggressore.
     */
    public static DeathContext of(UUID victimId, UUID killerId,
                                  DeathCause cause) {
        return new DeathContext(victimId, killerId, cause);
    }

    /**
     * Morte prodotta da un abbandono durante il combattimento.
     */
    public static DeathContext combatLogout(UUID victimId, UUID killerId) {
        return new DeathContext(victimId, killerId, DeathCause.COMBAT_LOGOUT);
    }

    public UUID getVictimId() {
        return victimId;
    }

    /**
     * @return il candidato uccisore, oppure {@code null} se assente o suicidio
     */
    public UUID getKillerId() {
        return killerId;
    }

    public DeathCause getCause() {
        return cause;
    }

    public boolean isCombatLogout() {
        return cause == DeathCause.COMBAT_LOGOUT;
    }

    /**
     * Indica se la morte chiude la sessione senza attendere un respawn.
     */
    public boolean closesSession() {
        return cause.closesSession();
    }

    @Override
    public String toString() {
        return "DeathContext{" + victimId + " <- " + killerId
                + ", " + cause + '}';
    }
}
