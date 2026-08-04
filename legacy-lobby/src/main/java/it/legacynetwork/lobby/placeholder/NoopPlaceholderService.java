package it.legacynetwork.lobby.placeholder;

import org.bukkit.entity.Player;

public final class NoopPlaceholderService implements PlaceholderService {

    @Override
    public String apply(Player player, String text) {
        return text;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
