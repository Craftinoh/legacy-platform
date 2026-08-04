package it.legacynetwork.lobby.message;

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
import java.util.Map;

public final class MessageService {
    private final File messagesFile;
    private final PlaceholderService placeholderService;
    private Map<String, MessageCategory> categories = Collections.emptyMap();

    public MessageService(File messagesFile, PlaceholderService placeholderService) {
        this.messagesFile = messagesFile;
        this.placeholderService = placeholderService;
    }

    public void load() {
        if (!messagesFile.exists()) {
            categories = Collections.emptyMap();
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(messagesFile);
        Map<String, MessageCategory> loaded = new LinkedHashMap<>();
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
        this.categories = Collections.unmodifiableMap(loaded);
    }

    public boolean isEnabled(String key) {
        MessageCategory category = categories.get(key);
        return category != null && category.isEnabled();
    }

    public List<String> getMessages(Player player, String key) {
        MessageCategory category = categories.get(key);
        if (category == null || !category.isEnabled()) {
            return Collections.emptyList();
        }
        List<String> rendered = new ArrayList<>();
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
        List<String> messages = getMessages(player, key);
        for (String message : messages) {
            player.sendMessage(message);
        }
    }

    private static final class MessageCategory {
        private final boolean enabled;
        private final List<String> text;

        MessageCategory(boolean enabled, List<String> text) {
            this.enabled = enabled;
            this.text = Collections.unmodifiableList(new ArrayList<>(text));
        }

        boolean isEnabled() {
            return enabled;
        }

        List<String> getText() {
            return text;
        }
    }
}
