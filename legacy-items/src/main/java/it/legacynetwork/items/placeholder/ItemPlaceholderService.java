package it.legacynetwork.items.placeholder;

import org.bukkit.entity.Player;

public interface ItemPlaceholderService {

    String apply(Player player, String text);

    boolean isAvailable();
}
