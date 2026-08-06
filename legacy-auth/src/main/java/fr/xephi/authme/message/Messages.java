package fr.xephi.authme.message;

import com.google.common.collect.ImmutableMap;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.locale.LocaleResolver;
import fr.xephi.authme.util.expiring.Duration;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Class for retrieving and sending translatable messages to players.
 */
public class Messages {

    // Custom Authme tag replaced to new line
    private static final String NEWLINE_TAG = "%nl%";

    // Global tag replacements
    private static final String USERNAME_TAG = "%username%";
    private static final String DISPLAYNAME_TAG = "%displayname%";

    /** Contains the keys of the singular messages for time units. */
    private static final Map<TimeUnit, MessageKey> TIME_UNIT_SINGULARS = ImmutableMap.<TimeUnit, MessageKey>builder()
        .put(TimeUnit.SECONDS, MessageKey.SECOND)
        .put(TimeUnit.MINUTES, MessageKey.MINUTE)
        .put(TimeUnit.HOURS, MessageKey.HOUR)
        .put(TimeUnit.DAYS, MessageKey.DAY).build();

    /** Contains the keys of the plural messages for time units. */
    private static final Map<TimeUnit, MessageKey> TIME_UNIT_PLURALS = ImmutableMap.<TimeUnit, MessageKey>builder()
        .put(TimeUnit.SECONDS, MessageKey.SECONDS)
        .put(TimeUnit.MINUTES, MessageKey.MINUTES)
        .put(TimeUnit.HOURS, MessageKey.HOURS)
        .put(TimeUnit.DAYS, MessageKey.DAYS).build();

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(EmailService.class);

    private MessagesFileHandler messagesFileHandler;
    private LocaleResolver localeResolver;

    /*
     * Constructor.
     */
    @Inject
    Messages(MessagesFileHandler messagesFileHandler, LocaleResolver localeResolver) {
        this.messagesFileHandler = messagesFileHandler;
        this.localeResolver = localeResolver;
    }

    /**
     * Send the given message code to the player.
     *
     * @param sender The entity to send the message to
     * @param key The key of the message to send
     */
    public void send(CommandSender sender, MessageKey key) {
        String[] lines = retrieve(key, sender);
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }

    /**
     * Send the given message code to the player with the given tag replacements. Note that this method
     * logs an error if the number of supplied replacements doesn't correspond to the number of tags
     * the message key contains.
     *
     * @param sender The entity to send the message to
     * @param key The key of the message to send
     * @param replacements The replacements to apply for the tags
     */
    public void send(CommandSender sender, MessageKey key, String... replacements) {
        String message = retrieveSingle(sender, key, replacements);
        for (String line : message.split("\n")) {
            sender.sendMessage(line);
        }
    }

    /**
     * Retrieve the message from the text file and return it split by new line as an array.
     *
     * @param key The message key to retrieve
     * @param sender The entity to send the message to
     * @return The message split by new lines
     */
    public String[] retrieve(MessageKey key, CommandSender sender) {
        String message = retrieveMessage(key, sender);
        if (message.isEmpty()) {
            // Return empty array instead of array with 1 empty string as entry
            return new String[0];
        }
        return message.split("\n");
    }

    /**
     * Returns the textual representation for the given duration.
     * Note that this class only supports the time units days, hour, minutes and seconds.
     *
     * @param duration the duration to build a text of
     * @return text of the duration
     */
    public String formatDuration(Duration duration) {
        return formatDuration(duration, localeResolver.getConsoleLanguage());
    }

    /**
     * Returns the textual representation for the given duration, in the language of the given
     * recipient. Used so that the time unit of a duration embedded in a message is translated the
     * same way as the message that carries it.
     * Note that this class only supports the time units days, hour, minutes and seconds.
     *
     * @param duration the duration to build a text of
     * @param sender the entity the duration will be shown to
     * @return text of the duration
     */
    public String formatDuration(Duration duration, CommandSender sender) {
        return formatDuration(duration, localeResolver.getLanguage(sender));
    }

    private String formatDuration(Duration duration, String language) {
        long value = duration.getDuration();
        MessageKey timeUnitKey = value == 1
            ? TIME_UNIT_SINGULARS.get(duration.getTimeUnit())
            : TIME_UNIT_PLURALS.get(duration.getTimeUnit());

        return value + " " + retrieveMessage(timeUnitKey, "", language);
    }

    /**
     * Retrieve the message from the text file.
     *
     * @param key The message key to retrieve
     * @param sender The entity to send the message to
     * @return The message from the file
     */
    private String retrieveMessage(MessageKey key, CommandSender sender) {
        String message = messagesFileHandler.getMessage(key.getKey(), localeResolver.getLanguage(sender));
        String displayName = sender.getName();
        if (sender instanceof Player) {
            displayName = ((Player) sender).getDisplayName();
        }

        return ChatColor.translateAlternateColorCodes('&', message)
                .replace(NEWLINE_TAG, "\n")
                .replace(USERNAME_TAG, sender.getName())
                .replace(DISPLAYNAME_TAG, displayName);
    }

    /**
     * Retrieve the message from the text file.
     *
     * @param key The message key to retrieve
     * @param name The name of the entity to send the message to
     * @return The message from the file
     */
    private String retrieveMessage(MessageKey key, String name, String language) {
        String message = messagesFileHandler.getMessage(key.getKey(), language);

        return ChatColor.translateAlternateColorCodes('&', message)
                .replace(NEWLINE_TAG, "\n")
                .replace(USERNAME_TAG, name)
                .replace(DISPLAYNAME_TAG, name);
    }

    /**
     * Retrieve the given message code with the given tag replacements. Note that this method
     * logs an error if the number of supplied replacements doesn't correspond to the number of tags
     * the message key contains.
     *
     * @param sender The entity to send the message to
     * @param key The key of the message to send
     * @param replacements The replacements to apply for the tags
     * @return The message from the file with replacements
     */
    public String retrieveSingle(CommandSender sender, MessageKey key, String... replacements) {
        String message = retrieveMessage(key, sender);
        String[] tags = key.getTags();
        if (replacements.length == tags.length) {
            for (int i = 0; i < tags.length; ++i) {
                message = message.replace(tags[i], replacements[i]);
            }
        } else {
            logger.warning("Invalid number of replacements for message key '" + key + "'");
        }
        return message;
    }

    /**
     * Retrieve the given message code with the given tag replacements. Note that this method
     * logs an error if the number of supplied replacements doesn't correspond to the number of tags
     * the message key contains.
     *
     * @param name The name of the entity to send the message to
     * @param key The key of the message to send
     * @param replacements The replacements to apply for the tags
     * @return The message from the file with replacements
     */
    public String retrieveSingle(String name, MessageKey key, String... replacements) {
        return retrieveSingle(null, name, key, replacements);
    }

    /**
     * Retrieve the given message code with the given tag replacements, in the language of the
     * player with the given unique id.
     * <p>
     * This overload exists for the phases in which the recipient is known but no {@link Player}
     * object is available yet — most notably the kick messages built while handling
     * {@code AsyncPlayerPreLoginEvent}, where the unique id is the only handle on the recipient.
     * Note that a client locale cannot be read at that point; unless the network provider knows the
     * player, the configured fallback language is used.
     *
     * @param playerId The unique id of the entity to send the message to, may be null
     * @param name The name of the entity to send the message to
     * @param key The key of the message to send
     * @param replacements The replacements to apply for the tags
     * @return The message from the file with replacements
     */
    public String retrieveSingle(UUID playerId, String name, MessageKey key, String... replacements) {
        String language = playerId == null
            ? localeResolver.getConsoleLanguage()
            : localeResolver.getLanguage(playerId);
        String message = retrieveMessage(key, name, language);
        String[] tags = key.getTags();
        if (replacements.length == tags.length) {
            for (int i = 0; i < tags.length; ++i) {
                message = message.replace(tags[i], replacements[i]);
            }
        } else {
            logger.warning("Invalid number of replacements for message key '" + key + "'");
        }
        return message;
    }
}
