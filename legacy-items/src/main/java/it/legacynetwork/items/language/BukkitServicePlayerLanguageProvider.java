package it.legacynetwork.items.language;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageProvider;
import org.bukkit.Bukkit;

public final class BukkitServicePlayerLanguageProvider {
    public PlayerLanguageProvider get() {
        return Bukkit.getServicesManager().load(PlayerLanguageProvider.class);
    }
}
