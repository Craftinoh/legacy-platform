package it.legacynetwork.screenshare.violation;

/**
 * Porta verso il sistema di provvedimenti.
 *
 * <p>Esiste per non incastrare la logica dei controlli con un plugin che oggi
 * non c'e'. LegacyPunishments non e' ancora stato scritto: quando lo sara',
 * bastera' un adapter che implementa questa interfaccia. Fino ad allora
 * l'unica implementazione registra e basta.</p>
 */
public interface ScreenshareViolationHandler {

    /**
     * Riceve una violazione appena accertata.
     *
     * <p>Non deve bloccare: viene invocata dal flusso di chiusura sessione.</p>
     */
    void handle(ScreenshareViolation violation);
}
