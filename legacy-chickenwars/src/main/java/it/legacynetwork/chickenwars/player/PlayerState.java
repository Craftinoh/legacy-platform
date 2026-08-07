package it.legacynetwork.chickenwars.player;

/**
 * Condizione di un giocatore rispetto alla partita in corso.
 */
public enum PlayerState {

    /** In attesa nella lobby pre-partita. */
    LOBBY,
    /** In gioco, con squadra assegnata. */
    PLAYING,
    /** In attesa del respawn dopo una morte non definitiva. */
    RESPAWNING,
    /** Eliminato o entrato volontariamente come spettatore. */
    SPECTATOR;

    /** Indica se il giocatore partecipa attivamente al combattimento. */
    public boolean isActive() {
        return this == PLAYING;
    }

    /** Indica se il giocatore occupa uno slot della propria squadra. */
    public boolean occupiesTeamSlot() {
        return this == PLAYING || this == RESPAWNING;
    }
}
