package it.legacynetwork.lobby.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public final class PlaceholderApiService implements PlaceholderService {

    @Override
    public String apply(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
