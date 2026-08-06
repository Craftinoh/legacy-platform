package it.legacynetwork.language.backend;

import it.legacynetwork.language.Language;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public final class LanguageMessageListener implements PluginMessageListener {
    private final LanguageBackendPlugin plugin;

    public LanguageMessageListener(LanguageBackendPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player carrier,
                                         byte[] message) {
        if (!"legacy:language".equals(channel)) {
            return;
        }
        try {
            String payload = new String(message, "UTF-8");
            String[] parts = payload.split("\\|");
            if (parts.length < 3) return;
            String type = parts[0];
            if ("SYNC".equals(type)) {
                java.util.UUID uuid = java.util.UUID.fromString(parts[1]);
                String code = parts[2];
                String locale = parts.length > 3 ? parts[3] : code;
                Language lang = Language.findByInput(code).orElse(Language.ENGLISH);
                plugin.updateState(uuid, lang, locale);
            }
        } catch (Exception ignored) {
        }
    }
}
