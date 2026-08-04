package it.legacynetwork.items.language;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PlayerLanguageAccessor {
    private final BukkitServicePlayerLanguageProvider bukkitProvider;
    private final DefaultPlayerLanguageProvider fallbackProvider;

    public PlayerLanguageAccessor(BukkitServicePlayerLanguageProvider bukkitProvider,
                                   DefaultPlayerLanguageProvider fallbackProvider) {
        this.bukkitProvider = bukkitProvider;
        this.fallbackProvider = fallbackProvider;
    }

    public Language getLanguage(Player player) {
        if (player == null) {
            return fallbackProvider.getLanguage(null);
        }
        PlayerLanguageProvider provider = bukkitProvider.get();
        if (provider != null) {
            return provider.getLanguage(player.getUniqueId());
        }
        return fallbackProvider.getLanguage(player.getUniqueId());
    }

    public String getLanguageCode(Player player) {
        return getLanguage(player).getCode();
    }
}
