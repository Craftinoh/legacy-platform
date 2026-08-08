package it.legacynetwork.screenshare.violation;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Gestore predefinito: registra e non punisce.
 *
 * <p>Non esegue comandi, non contatta la console e non conosce alcun sistema di
 * ban. Scrive una riga tecnica sul log del proxy: l'audit visibile allo staff
 * passa dalle traduzioni, questa riga serve a chi legge i file.</p>
 */
public final class AuditOnlyScreenshareViolationHandler
        implements ScreenshareViolationHandler {

    private final Consumer<String> sink;

    public AuditOnlyScreenshareViolationHandler(Consumer<String> sink) {
        if (sink == null) {
            throw new IllegalArgumentException(
                    "Destinazione del registro mancante");
        }
        this.sink = sink;
    }

    @Override
    public void handle(ScreenshareViolation violation) {
        StringBuilder line = new StringBuilder();
        line.append("screenshare-violation type=")
                .append(violation.getType().name())
                .append(" session=").append(violation.getSessionId())
                .append(" target=").append(violation.getTargetId())
                .append(" staff=").append(violation.getStaffId()
                        .map(Object::toString).orElse("-"))
                .append(" report=").append(violation.getReportId()
                        .map(Object::toString).orElse("-"))
                .append(" suggested=")
                .append(violation.getSuggestedCategory().name())
                .append(" at=").append(violation.getOccurredAt());
        for (Map.Entry<String, String> entry
                : violation.getContext().entrySet()) {
            line.append(' ').append(entry.getKey()).append('=')
                    .append(entry.getValue());
        }
        sink.accept(line.toString());
    }
}
