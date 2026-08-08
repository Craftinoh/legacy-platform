package it.legacynetwork.screenshare.support;

import it.legacynetwork.screenshare.violation.ScreenshareViolation;
import it.legacynetwork.screenshare.violation.ScreenshareViolationHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gestore di violazioni che si limita a raccoglierle.
 */
public final class RecordingViolationHandler
        implements ScreenshareViolationHandler {

    private final List<ScreenshareViolation> received = new ArrayList<>();

    @Override
    public void handle(ScreenshareViolation violation) {
        received.add(violation);
    }

    public List<ScreenshareViolation> received() {
        return Collections.unmodifiableList(received);
    }

    public int count() {
        return received.size();
    }

    public ScreenshareViolation last() {
        if (received.isEmpty()) {
            throw new IllegalStateException("Nessuna violazione ricevuta");
        }
        return received.get(received.size() - 1);
    }
}
