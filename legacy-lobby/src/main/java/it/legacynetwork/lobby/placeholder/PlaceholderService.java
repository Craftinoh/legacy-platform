package it.legacynetwork.lobby.placeholder;

import org.bukkit.entity.Player;

public interface PlaceholderService {

    String apply(Player player, String text);

    boolean isAvailable();
}
