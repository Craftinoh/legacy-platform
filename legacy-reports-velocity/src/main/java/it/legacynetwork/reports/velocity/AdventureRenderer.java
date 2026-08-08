package it.legacynetwork.reports.velocity;

import it.legacynetwork.reports.message.ChatLine;
import it.legacynetwork.reports.message.ChatSegment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Trasforma una riga gia' tradotta in un componente Adventure.
 *
 * <p>Ultimo anello della catena: il testo arriva da NetworkLanguage, i colori
 * dai codici {@code &} usati nel resto della rete, e qui si aggiungono soltanto
 * i comportamenti — suggerimento al passaggio del mouse e comando al click.</p>
 */
public final class AdventureRenderer {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private AdventureRenderer() {
    }

    public static Component render(ChatLine line) {
        Component result = Component.empty();
        for (ChatSegment segment : line.getSegments()) {
            result = result.append(render(segment));
        }
        return result;
    }

    public static Component render(ChatSegment segment) {
        Component component = LEGACY.deserialize(segment.getText());
        if (segment.getHover().isPresent()) {
            component = component.hoverEvent(HoverEvent.showText(
                    LEGACY.deserialize(segment.getHover().get())));
        }
        if (segment.getRunCommand().isPresent()) {
            component = component.clickEvent(
                    ClickEvent.runCommand(segment.getRunCommand().get()));
        } else if (segment.getSuggestCommand().isPresent()) {
            component = component.clickEvent(
                    ClickEvent.suggestCommand(
                            segment.getSuggestCommand().get()));
        }
        return component;
    }
}
