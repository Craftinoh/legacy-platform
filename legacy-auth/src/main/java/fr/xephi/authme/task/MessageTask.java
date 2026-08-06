package fr.xephi.authme.task;

import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Message shown to a player in a regular interval as long as he is not logged in.
 * <p>
 * The task deliberately stores only the recipient, the {@link MessageKey} and the non-localized
 * replacements: the text is translated on every single run, right before it is sent. Storing an
 * already translated string here — as was done previously — froze the message in whatever language
 * the player had when they joined, so a language change made through the network {@code /lang}
 * command only took effect after a reconnect.
 */
public class MessageTask extends BukkitRunnable {

    private final Player player;
    private final Messages messages;
    private final MessageKey messageKey;
    private final String[] args;
    private boolean isMuted;

    /**
     * Constructor.
     *
     * @param player the player to send the message to
     * @param messages the messages service, used to translate at send time
     * @param messageKey the key of the message to repeat
     * @param args the non-localized replacements of the message key
     */
    public MessageTask(Player player, Messages messages, MessageKey messageKey, String... args) {
        this.player = player;
        this.messages = messages;
        this.messageKey = messageKey;
        this.args = args;
        isMuted = false;
    }

    public void setMuted(boolean isMuted) {
        this.isMuted = isMuted;
    }

    @Override
    public void run() {
        if (isMuted || !player.isOnline()) {
            return;
        }
        // Resolved here, and not when the task was created, so that the current language is used.
        player.sendMessage(messages.retrieveSingle(player, messageKey, args).split("\n"));
    }
}
