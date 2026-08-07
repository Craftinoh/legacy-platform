package it.legacynetwork.chickenwars.death;

import it.legacynetwork.chickenwars.economy.ResourceTransfer;

/**
 * Risultato osservabile dell'elaborazione di una morte.
 *
 * <p>Permette a chiamanti e test di distinguere una morte realmente elaborata
 * da un evento duplicato, senza ispezionare lo stato interno dei servizi.</p>
 */
public final class DeathOutcome {

    private final boolean processed;
    private final long sequence;
    private final ResourceTransfer transfer;
    private final boolean killerRewarded;

    private DeathOutcome(boolean processed, long sequence,
                         ResourceTransfer transfer, boolean killerRewarded) {
        this.processed = processed;
        this.sequence = sequence;
        this.transfer = transfer == null ? ResourceTransfer.empty() : transfer;
        this.killerRewarded = killerRewarded;
    }

    /**
     * Morte elaborata per la prima volta.
     */
    static DeathOutcome processed(long sequence, ResourceTransfer transfer,
                                  boolean killerRewarded) {
        return new DeathOutcome(true, sequence, transfer, killerRewarded);
    }

    /**
     * Evento riferito a una morte gia' elaborata: nessun effetto ripetuto.
     */
    static DeathOutcome duplicate(long sequence) {
        return new DeathOutcome(false, sequence, ResourceTransfer.empty(), false);
    }

    /**
     * Evento ignorato perche' privo dei dati necessari.
     */
    static DeathOutcome ignored() {
        return new DeathOutcome(false, 0L, ResourceTransfer.empty(), false);
    }

    /** Indica se questa chiamata ha realmente applicato gli effetti. */
    public boolean isProcessed() {
        return processed;
    }

    /** Sequenza di morte usata, utile per diagnostica e test. */
    public long getSequence() {
        return sequence;
    }

    /** Dettaglio delle risorse spostate. */
    public ResourceTransfer getTransfer() {
        return transfer;
    }

    /** Indica se un uccisore valido ha ricevuto o accodato risorse. */
    public boolean isKillerRewarded() {
        return killerRewarded;
    }

    @Override
    public String toString() {
        return "DeathOutcome{processed=" + processed
                + ", sequence=" + sequence
                + ", rewarded=" + killerRewarded + '}';
    }
}
