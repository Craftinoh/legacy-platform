package it.legacynetwork.reports.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Riga di chat composta da segmenti gia' tradotti.
 *
 * <p>L'interfaccia staff di questo plugin e' fatta di righe come questa: niente
 * inventari, niente finestre, solo testo cliccabile.</p>
 */
public final class ChatLine {

    private final List<ChatSegment> segments;

    private ChatLine(List<ChatSegment> segments) {
        this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
    }

    public static ChatLine of(ChatSegment... segments) {
        return new ChatLine(Arrays.asList(segments));
    }

    public static ChatLine of(List<ChatSegment> segments) {
        return new ChatLine(segments);
    }

    public static ChatLine text(String text) {
        return new ChatLine(
                Collections.singletonList(ChatSegment.text(text)));
    }

    public List<ChatSegment> getSegments() {
        return segments;
    }

    /**
     * Testo semplice della riga, senza informazioni di click.
     *
     * <p>Usato dai test e dai messaggi di disconnessione, dove i pulsanti non
     * hanno senso.</p>
     */
    public String plainText() {
        StringBuilder builder = new StringBuilder();
        for (ChatSegment segment : segments) {
            builder.append(segment.getText());
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return plainText();
    }
}
