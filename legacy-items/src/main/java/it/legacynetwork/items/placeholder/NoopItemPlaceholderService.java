package it.legacynetwork.items.placeholder;

import org.bukkit.entity.Player;

public final class NoopItemPlaceholderService implements ItemPlaceholderService {

    @Override
    public String apply(Player player, String text) {
        return text;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
