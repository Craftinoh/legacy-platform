package it.legacynetwork.lobby.message;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageProvider;
import it.legacynetwork.language.TranslationInstaller;
import it.legacynetwork.lobby.placeholder.PlaceholderService;
import it.legacynetwork.lobby.util.LegacyColorTranslator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MessageService {
    private final File messagesLocation;
    private final PlaceholderService placeholderService;
    private final PlayerLanguageProvider languageProvider;
    private final String fallbackLanguage;
    private final boolean directoryMode;

    private volatile Map<String, Map<String, MessageCategory>> catalogs =
            Collections.emptyMap();

    /**
     * Legacy single-catalog constructor retained for existing tests and custom
     * integrations. New runtime code should use the directory constructor.
     */
    public MessageService(File messagesFile, PlaceholderService placeholderService) {
        this.messagesLocation = messagesFile;
        this.placeholderService = placeholderService;
        this.languageProvider = null;
        this.fallbackLanguage = "en";
        this.directoryMode = false;
    }

    public MessageService(File translationsDirectory,
                          PlaceholderService placeholderService,
                          PlayerLanguageProvider languageProvider,
                          String fallbackLanguage) {
        this.messagesLocation = translationsDirectory;
        this.placeholderService = placeholderService;
        this.languageProvider = languageProvider;
        this.fallbackLanguage = normalizeLanguage(fallbackLanguage);
        this.directoryMode = true;
    }

    public void load() {
        Map<String, Map<String, MessageCategory>> loadedCatalogs =
                new LinkedHashMap<String, Map<String, MessageCategory>>();

        if (!directoryMode) {
            Map<String, MessageCategory> single = loadCatalog(messagesLocation);
            if (!single.isEmpty()) {
                loadedCatalogs.put(fallbackLanguage, single);
                loadedCatalogs.put("en", single);
            }
            catalogs = immutableCatalogs(loadedCatalogs);
            return;
        }

        for (String language : TranslationInstaller.ALL_LANGUAGES) {
            File file = new File(messagesLocation,
                    "messages_" + language + ".yml");
            Map<String, MessageCategory> catalog = loadCatalog(file);
            if (!catalog.isEmpty()) {
                loadedCatalogs.put(language, catalog);
            }
        }
        catalogs = immutableCatalogs(loadedCatalogs);
    }

    public boolean isEnabled(String key) {
        MessageCategory category = categoryFor(null, key);
        return category != null && category.isEnabled();
    }

    public List<String> getMessages(Player player, String key) {
        MessageCategory category = categoryFor(player, key);
        if (category == null || !category.isEnabled()) {
            return Collections.emptyList();
        }
        List<String> rendered = new ArrayList<String>();
        for (String raw : category.getText()) {
            String result = raw;
            result = result.replace("{player}",
                    player != null ? player.getName() : "???");
            if (placeholderService.isAvailable() && player != null) {
                result = placeholderService.apply(player, result);
            }
            result = LegacyColorTranslator.translate(result);
            rendered.add(result);
        }
        return rendered;
    }

    public void send(Player player, String key) {
        for (String message : getMessages(player, key)) {
            player.sendMessage(message);
        }
    }

    private MessageCategory categoryFor(Player player, String key) {
        Map<String, MessageCategory> catalog = catalogFor(resolveLanguage(player));
        MessageCategory category = catalog.get(key);
        if (category != null) {
            return category;
        }
        Map<String, MessageCategory> english = catalogFor("en");
        return english.get(key);
    }

    private Map<String, MessageCategory> catalogFor(String language) {
        Map<String, MessageCategory> catalog = catalogs.get(language);
        if (catalog == null) {
            catalog = catalogs.get("en");
        }
        if (catalog == null) {
            catalog = catalogs.get(fallbackLanguage);
        }
        if (catalog == null && !catalogs.isEmpty()) {
            catalog = catalogs.values().iterator().next();
        }
        return catalog != null ? catalog
                : Collections.<String, MessageCategory>emptyMap();
    }

    private String resolveLanguage(Player player) {
        if (player == null || languageProvider == null) {
            return fallbackLanguage;
        }
        try {
            Language language = languageProvider.getLanguage(player.getUniqueId());
            if (language != null) {
                return normalizeLanguage(language.getCode());
            }
        } catch (RuntimeException ignored) {
            // Fall through to configured fallback.
        } catch (LinkageError ignored) {
            // Fall through to configured fallback.
        }
        return fallbackLanguage;
    }

    private Map<String, MessageCategory> loadCatalog(File file) {
        if (file == null || !file.isFile()) {
            return Collections.emptyMap();
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<String, MessageCategory> loaded =
                new LinkedHashMap<String, MessageCategory>();
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            boolean enabled = section.getBoolean("enabled", true);
            List<String> text;
            Object textRaw = section.get("text");
            if (textRaw instanceof List) {
                text = section.getStringList("text");
            } else if (textRaw instanceof String) {
                text = Collections.singletonList((String) textRaw);
            } else {
                text = Collections.emptyList();
            }
            loaded.put(key, new MessageCategory(enabled, text));
        }
        return Collections.unmodifiableMap(loaded);
    }

    private Map<String, Map<String, MessageCategory>> immutableCatalogs(
            Map<String, Map<String, MessageCategory>> source) {
        return Collections.unmodifiableMap(
                new LinkedHashMap<String, Map<String, MessageCategory>>(source));
    }

    private static String normalizeLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            return "en";
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_');
        return Language.findByInput(normalized)
                .map(Language::getCode)
                .orElse("en");
    }

    private static final class MessageCategory {
        private final boolean enabled;
        private final List<String> text;

        MessageCategory(boolean enabled, List<String> text) {
            this.enabled = enabled;
            this.text = Collections.unmodifiableList(
                    new ArrayList<String>(text));
        }

        boolean isEnabled() {
            return enabled;
        }

        List<String> getText() {
            return text;
        }
    }
}
