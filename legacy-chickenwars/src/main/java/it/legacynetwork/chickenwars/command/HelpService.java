package it.legacynetwork.chickenwars.command;

import it.legacynetwork.chickenwars.message.MessageService;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Guida in gioco divisa per sezioni.
 *
 * <p>Le sezioni riservate allo staff non compaiono ai giocatori normali, ne'
 * nell'indice ne' nei suggerimenti del comando.</p>
 */
public final class HelpService {

    private final MessageService messages;

    public HelpService(MessageService messages) {
        this.messages = messages;
    }

    /**
     * Mostra una sezione della guida.
     *
     * <p>Un argomento assente mostra l'indice; un argomento sconosciuto o non
     * consentito segnala l'errore e ripiega sull'indice.</p>
     *
     * @param topicName nome o alias della sezione, eventualmente nullo
     */
    public void send(CommandSender sender, String topicName) {
        HelpTopic topic = resolve(topicName, isAdmin(sender));
        if (topic == null) {
            messages.send(sender, "help.unknown-topic",
                    "{topic}", topicName == null ? "" : topicName,
                    "{topics}", joinTopics(sender));
            sendTopic(sender, HelpTopic.GENERAL);
            return;
        }
        sendTopic(sender, topic);
    }

    /**
     * Individua la sezione da mostrare.
     *
     * <p>Metodo puro, senza dipendenze da Bukkit: e' il punto in cui si decide
     * se ripiegare sull'indice, e non deve mai delegare a {@code send} per non
     * introdurre ricorsione.</p>
     *
     * @param topicName nome digitato, eventualmente nullo o vuoto
     * @param admin     indica se il destinatario ha i permessi di staff
     * @return la sezione da mostrare, oppure {@code null} se non consentita
     */
    static HelpTopic resolve(String topicName, boolean admin) {
        if (topicName == null || topicName.trim().isEmpty()) {
            return HelpTopic.GENERAL;
        }
        HelpTopic topic = HelpTopic.find(topicName);
        if (topic == null || (topic.requiresAdmin() && !admin)) {
            return null;
        }
        return topic;
    }

    /**
     * Stampa le righe di una sezione. Non richiama mai {@link #send}.
     */
    private void sendTopic(CommandSender sender, HelpTopic topic) {
        for (String line : messages.getList(sender, topic.getMessageKey())) {
            sender.sendMessage(line);
        }
        if (topic == HelpTopic.GENERAL) {
            messages.sendRaw(sender, "help.topics-hint",
                    "{topics}", joinTopics(sender));
        }
    }

    /**
     * Elenca in una riga le sezioni visibili al destinatario.
     */
    private String joinTopics(CommandSender sender) {
        StringBuilder builder = new StringBuilder();
        for (String name : getVisibleTopics(sender)) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(name);
        }
        return builder.toString();
    }

    /**
     * Nomi delle sezioni consultabili dal destinatario, indice escluso.
     */
    public List<String> getVisibleTopics(CommandSender sender) {
        boolean admin = isAdmin(sender);
        List<String> names = new ArrayList<String>();
        for (HelpTopic topic : HelpTopic.values()) {
            if (topic == HelpTopic.GENERAL || (topic.requiresAdmin() && !admin)) {
                continue;
            }
            names.add(topic.getCanonicalName());
        }
        return names;
    }

    private boolean isAdmin(CommandSender sender) {
        return sender != null && sender.hasPermission(HelpTopic.ADMIN_PERMISSION);
    }
}
