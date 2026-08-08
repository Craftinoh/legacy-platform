package it.legacynetwork.language;

/**
 * Plugin che mette a disposizione della rete il proprio provider di lingua.
 *
 * <p>Esiste per consentire a un altro plugin del proxy di ottenere la lingua
 * reale di un giocatore senza conoscere le classi di chi la gestisce: il tipo
 * condiviso e' questa interfaccia, non l'implementazione. Chi la cerca resta
 * quindi con una dipendenza facoltativa.</p>
 */
public interface PlayerLanguageProviderHolder {

    /**
     * Provider da usare per risolvere la lingua dei giocatori.
     *
     * @return il provider, oppure {@code null} se non ancora inizializzato
     */
    PlayerLanguageProvider languageProvider();
}
