package it.legacynetwork.chickenwars.upgrade;

/**
 * Esito di un tentativo di acquisto di un upgrade o di una trappola.
 *
 * <p>Ogni esito corrisponde a una chiave localizzata: il testo mostrato al
 * giocatore resta fuori dal codice Java.</p>
 */
public enum UpgradeResult {

    /** Acquisto completato e livello applicato. */
    SUCCESS("upgrade.purchased"),
    /** L'upgrade ha gia' raggiunto il livello massimo. */
    MAX_LEVEL("upgrade.max-level"),
    /** Risorse insufficienti. */
    NOT_ENOUGH("upgrade.not-enough"),
    /** Definizione assente o disabilitata. */
    UNAVAILABLE("upgrade.unavailable"),
    /** Nessun prezzo per il profilo economico della partita. */
    NO_PRICE("upgrade.no-price"),
    /** Il giocatore non appartiene a una squadra. */
    NO_TEAM("upgrade.no-team"),
    /** La partita non e' in corso. */
    NOT_IN_GAME("upgrade.not-in-game"),
    /** La coda delle trappole e' piena. */
    TRAP_QUEUE_FULL("trap.queue-full");

    private final String messageKey;

    UpgradeResult(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
