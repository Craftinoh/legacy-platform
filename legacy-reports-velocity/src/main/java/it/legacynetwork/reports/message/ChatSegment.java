package it.legacynetwork.reports.message;

import java.util.Optional;

/**
 * Pezzo di riga gia' tradotto.
 *
 * <p>Contiene testo, eventuale suggerimento al passaggio del mouse e
 * l'eventuale comando da proporre o eseguire al click. E' volutamente povero e
 * indipendente da Adventure: la traduzione avviene prima, la trasformazione in
 * componenti dopo, e in mezzo resta un valore che i test possono leggere.</p>
 */
public final class ChatSegment {

    private final String text;
    private final String hover;
    private final String suggestCommand;
    private final String runCommand;

    private ChatSegment(String text, String hover, String suggestCommand,
                        String runCommand) {
        if (text == null) {
            throw new IllegalArgumentException("Testo del segmento mancante");
        }
        this.text = text;
        this.hover = hover;
        this.suggestCommand = suggestCommand;
        this.runCommand = runCommand;
    }

    public static ChatSegment text(String text) {
        return new ChatSegment(text, null, null, null);
    }

    /**
     * Pulsante che scrive il comando nella barra di chat.
     */
    public static ChatSegment suggest(String text, String hover,
                                      String command) {
        return new ChatSegment(text, hover, command, null);
    }

    /**
     * Pulsante che esegue direttamente il comando.
     */
    public static ChatSegment run(String text, String hover, String command) {
        return new ChatSegment(text, hover, null, command);
    }

    /**
     * Testo con solo suggerimento al passaggio del mouse.
     */
    public static ChatSegment hint(String text, String hover) {
        return new ChatSegment(text, hover, null, null);
    }

    public String getText() {
        return text;
    }

    public Optional<String> getHover() {
        return Optional.ofNullable(hover);
    }

    public Optional<String> getSuggestCommand() {
        return Optional.ofNullable(suggestCommand);
    }

    public Optional<String> getRunCommand() {
        return Optional.ofNullable(runCommand);
    }

    public boolean isClickable() {
        return suggestCommand != null || runCommand != null;
    }

    @Override
    public String toString() {
        return text;
    }
}
