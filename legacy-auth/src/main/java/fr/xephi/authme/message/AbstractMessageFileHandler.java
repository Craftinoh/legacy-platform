package fr.xephi.authme.message;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.message.locale.SupportedLanguages;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static fr.xephi.authme.message.MessagePathHelper.DEFAULT_LANGUAGE;

/**
 * Handles a YAML message file with a default file fallback.
 */
public abstract class AbstractMessageFileHandler implements Reloadable {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(AbstractMessageFileHandler.class);

    @DataFolder
    @Inject
    private File dataFolder;

    @Inject
    private Settings settings;

    private String filename;
    private FileConfiguration configuration;
    private final String defaultFile;

    /**
     * Lazily loaded configurations of languages other than the configured one, keyed by language
     * code. Values are {@link #MISSING} for languages that have no message file at all, so that a
     * missing file is looked up only once. Cleared on every {@link #reload()}.
     */
    private final Map<String, FileConfiguration> configurationsByLanguage = new ConcurrentHashMap<>();

    /** Marker for a language that has no message file, to avoid repeated lookups. */
    private static final FileConfiguration MISSING = new YamlConfiguration();

    protected AbstractMessageFileHandler() {
        this.defaultFile = createFilePath(DEFAULT_LANGUAGE);
    }

    @Override
    @PostConstruct
    public void reload() {
        String language = settings.getProperty(PluginSettings.MESSAGES_LANGUAGE);
        filename = createFilePath(language);
        File messagesFile = initializeFile(filename);
        FileConfiguration loaded = YamlConfiguration.loadConfiguration(messagesFile);
        // An unreadable file yields an empty configuration; keeping the previous snapshot is far
        // better than serving "Error retrieving message" for every key until the next reload.
        if (configuration == null || loaded.getKeys(true).size() > 0) {
            configuration = loaded;
        } else {
            logger.warning("Messages file '" + filename + "' is empty or invalid; "
                + "keeping the previously loaded messages");
        }
        configurationsByLanguage.clear();
        extractBundledLanguageFiles();
    }

    /**
     * Copies every shipped language file that is not present in the data folder yet.
     * <p>
     * This runs once on startup and on every reload, never while a message is being sent: sending
     * must not touch the file system. Existing files are never overwritten, so administrator
     * customizations and hand-made translations are preserved.
     */
    private void extractBundledLanguageFiles() {
        for (String language : SupportedLanguages.getCodes()) {
            String filePath = createFilePath(language);
            File file = new File(dataFolder, filePath);
            if (!file.exists() && FileUtils.getResourceFromJar(filePath) != null) {
                FileUtils.copyFileFromResource(file, filePath);
            }
        }
    }

    protected String getLanguage() {
        return settings.getProperty(PluginSettings.MESSAGES_LANGUAGE);
    }

    protected File getUserLanguageFile() {
        return new File(dataFolder, filename);
    }

    protected String getFilename() {
        return filename;
    }

    /**
     * Returns whether the message file configuration has an entry at the given path.
     *
     * @param path the path to verify
     * @return true if an entry exists for the path in the messages file, false otherwise
     */
    public boolean hasSection(String path) {
        return configuration.get(path) != null;
    }

    /**
     * Returns the message for the given key.
     *
     * @param key the key to retrieve the message for
     * @return the message
     */
    public String getMessage(String key) {
        String message = configuration.getString(key);
        return message == null
            ? "Error retrieving message '" + key + "'"
            : message;
    }

    /**
     * Returns the message for the given key only if it exists,
     * i.e. without falling back to the default file.
     *
     * @param key the key to retrieve the message for
     * @return the message, or {@code null} if not available
     */
    public String getMessageIfExists(String key) {
        return configuration.getString(key);
    }

    /**
     * Returns whether the message file of the given language has an entry at the given path,
     * falling back to the configured language when the language has no file of its own.
     *
     * @param path the path to verify
     * @param language the language code to look the path up in
     * @return true if an entry exists for the path, false otherwise
     */
    public boolean hasSection(String path, String language) {
        FileConfiguration languageConfiguration = getConfigurationFor(language);
        if (languageConfiguration != null && languageConfiguration.get(path) != null) {
            return true;
        }
        return hasSection(path);
    }

    /**
     * Returns the message for the given key in the given language.
     * <p>
     * When the language has no message file, or the file has no entry for the key, the message of
     * the configured language is returned; the same error placeholder as
     * {@link #getMessage(String)} is used as a last resort. A language that is merely incomplete
     * therefore yields translated messages where it has them and the configured language elsewhere.
     *
     * @param key the key to retrieve the message for
     * @param language the language code to retrieve the message in
     * @return the message
     */
    public String getMessage(String key, String language) {
        String message = getMessageIfExists(key, language);
        if (message != null) {
            return message;
        }
        // Fall back to the configured language, then to English, then to AuthMe's diagnostic text.
        message = configuration.getString(key);
        if (message != null) {
            return message;
        }
        message = getMessageIfExists(key, DEFAULT_LANGUAGE);
        return message == null
            ? "Error retrieving message '" + key + "'"
            : message;
    }

    /**
     * Returns the message for the given key in the given language only if it exists, i.e. without
     * falling back to the configured language.
     *
     * @param key the key to retrieve the message for
     * @param language the language code to retrieve the message in
     * @return the message, or {@code null} if not available
     */
    public String getMessageIfExists(String key, String language) {
        FileConfiguration languageConfiguration = getConfigurationFor(language);
        return languageConfiguration == null
            ? null
            : languageConfiguration.getString(key);
    }

    /**
     * Returns the configuration for the given language, loading it on first use. Returns null when
     * the language is the configured one — callers then use the already loaded configuration — or
     * when no message file exists for it.
     *
     * @param language the language code
     * @return the configuration for the language, or null if not applicable
     */
    private FileConfiguration getConfigurationFor(String language) {
        if (language == null || language.equals(settings.getProperty(PluginSettings.MESSAGES_LANGUAGE))) {
            return null;
        }

        FileConfiguration cached = configurationsByLanguage.computeIfAbsent(language, this::loadLanguageFile);
        return cached == MISSING ? null : cached;
    }

    /**
     * Loads the message file of a language other than the configured one, reading only: the shipped
     * files were already extracted by {@link #reload()}, so sending a message never writes to disk.
     *
     * @param language the language code to load the message file of
     * @return the loaded configuration, or {@link #MISSING} when the language has no file
     */
    private FileConfiguration loadLanguageFile(String language) {
        File file = new File(dataFolder, createFilePath(language));
        if (!file.exists()) {
            return MISSING;
        }
        FileConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        return loaded.getKeys(true).isEmpty() ? MISSING : loaded;
    }

    /**
     * Creates the path to the messages file for the given language code.
     *
     * @param language the language code
     * @return path to the message file for the given language
     */
    protected abstract String createFilePath(String language);

    /**
     * Copies the messages file from the JAR to the local messages/ folder if it doesn't exist.
     *
     * @param filePath path to the messages file to use
     * @return the messages file to use
     */
    @VisibleForTesting
    File initializeFile(String filePath) {
        File file = new File(dataFolder, filePath);
        // Check that JAR file exists to avoid logging an error
        if (FileUtils.getResourceFromJar(filePath) != null && FileUtils.copyFileFromResource(file, filePath)) {
            return file;
        }

        if (FileUtils.copyFileFromResource(file, defaultFile)) {
            return file;
        } else {
            logger.warning("Wanted to copy default messages file '" + defaultFile + "' from JAR but it didn't exist");
            return null;
        }
    }
}
