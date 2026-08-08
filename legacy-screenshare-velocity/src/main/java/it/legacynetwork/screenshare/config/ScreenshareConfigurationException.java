package it.legacynetwork.screenshare.config;

/**
 * Configurazione che il plugin non puo' onorare.
 *
 * <p>Preferibile a un valore accettato e poi ignorato: chi ha scritto il file
 * vede subito che cosa non e' disponibile.</p>
 */
public final class ScreenshareConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ScreenshareConfigurationException(String message) {
        super(message);
    }
}
