package it.legacynetwork.chickenwars.player.equipment;

import it.legacynetwork.chickenwars.player.PlayerSession;

/**
 * Fornisce i livelli di incantesimo derivanti dagli upgrade di squadra.
 *
 * <p>Il servizio equipaggiamento consulta questo punto ogni volta che ricrea
 * armatura e spada, quindi Protection e Sharpness restano allineati dopo
 * respawn e reconnect senza duplicare incantesimi.</p>
 */
public interface TeamEnchantProvider {

    /**
     * Implementazione usata quando nessun upgrade e' disponibile.
     */
    TeamEnchantProvider NONE = new TeamEnchantProvider() {
        @Override
        public int getProtectionLevel(PlayerSession session) {
            return 0;
        }

        @Override
        public int getSharpnessLevel(PlayerSession session) {
            return 0;
        }
    };

    /**
     * Livello di Protection da applicare a ogni pezzo di armatura.
     *
     * <p>Riceve la sessione e non il solo ID squadra: gli ID di squadra si
     * ripetono fra arene diverse, quindi da soli non identificano lo stato
     * degli upgrade.</p>
     *
     * @param session sessione del giocatore, eventualmente nulla
     * @return il livello, oppure {@code 0} per nessun incantesimo
     */
    int getProtectionLevel(PlayerSession session);

    /**
     * Livello di Sharpness da applicare alla spada.
     *
     * @param session sessione del giocatore, eventualmente nulla
     * @return il livello, oppure {@code 0} per nessun incantesimo
     */
    int getSharpnessLevel(PlayerSession session);
}
