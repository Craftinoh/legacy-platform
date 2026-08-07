package it.legacynetwork.chickenwars.chicken;

/**
 * Destinatario della sconfitta di una Gallina Reale.
 *
 * <p>E' il punto di innesto previsto per il Royal Collapse: chi lo implementera'
 * ricevera' la sconfitta gia' validata e non dovra' riconoscere l'entita', ne'
 * difendersi dagli eventi duplicati.</p>
 */
public interface RoyalDefeatListener {

    /**
     * Notifica una sconfitta, chiamata esattamente una volta per gallina.
     */
    void onRoyalDefeated(RoyalDefeat defeat);
}
