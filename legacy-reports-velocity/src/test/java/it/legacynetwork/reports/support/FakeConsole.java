package it.legacynetwork.reports.support;

import it.legacynetwork.reports.message.ChatLine;
import it.legacynetwork.reports.platform.CommandActor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Console del proxy: nessun UUID, tutti i permessi.
 */
public final class FakeConsole implements CommandActor {

    private final List<ChatLine> received = new ArrayList<>();

    @Override
    public UUID uniqueId() {
        return null;
    }

    @Override
    public String name() {
        return "CONSOLE";
    }

    @Override
    public boolean hasPermission(String node) {
        return true;
    }

    @Override
    public void send(ChatLine line) {
        received.add(line);
    }

    public List<ChatLine> received() {
        return Collections.unmodifiableList(received);
    }

    public String text() {
        StringBuilder builder = new StringBuilder();
        for (ChatLine line : received) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line.plainText());
        }
        return builder.toString();
    }
}
