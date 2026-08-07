package it.legacynetwork.chickenwars.player.equipment;

/**
 * Fornisce i livelli di incantesimo derivanti dagli upgrade di squadra.
 *
 * <p>Il servizio equipaggiamento consulta questo punto ogni volta che ricrea
 * armatura e spada: quando gli upgrade Protection e Sharpness verranno
 * introdotti bastera' registrare un'implementazione diversa, senza modificare
 * la gestione dell'equipaggiamento.</p>
 */
public interface TeamEnchantProvider {

    /**
     * Implementazione attiva finche' gli upgrade di squadra non esistono.
     */
    TeamEnchantProvider NONE = new TeamEnchantProvider() {
        @Override
        public int getProtectionLevel(String teamId) {
            return 0;
        }

        @Override
        public int getSharpnessLevel(String teamId) {
            return 0;
        }
    };

    /**
     * Livello di Protection da applicare a ogni pezzo di armatura.
     *
     * @param teamId squadra del giocatore, eventualmente nulla
     * @return il livello, oppure {@code 0} per nessun incantesimo
     */
    int getProtectionLevel(String teamId);

    /**
     * Livello di Sharpness da applicare alla spada.
     *
     * @param teamId squadra del giocatore, eventualmente nulla
     * @return il livello, oppure {@code 0} per nessun incantesimo
     */
    int getSharpnessLevel(String teamId);
}
