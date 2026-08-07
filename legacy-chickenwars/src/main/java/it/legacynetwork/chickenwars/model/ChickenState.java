package it.legacynetwork.chickenwars.model;

/**
 * Stato corrente della Gallina Reale.
 */
public enum ChickenState {

    /** Nessuna condizione particolare attiva. */
    NORMAL,
    /** Protetta dalla fase iniziale della partita. */
    PROTECTED,
    /** Sta assorbendo danno tramite lo scudo. */
    SHIELDED,
    /** Ha subito danno di recente. */
    DAMAGED,
    /** Sta rigenerando vita o scudo. */
    HEALING,
    /** Temporaneamente invulnerabile. */
    INVULNERABLE,
    /** Eliminata: la squadra non puo' piu' rinascere. */
    DEAD;

    /**
     * Indica se in questo stato la gallina puo' subire danno.
     */
    public boolean isDamageable() {
        return this != PROTECTED && this != INVULNERABLE && this != DEAD;
    }
}
