package it.legacynetwork.regions.selection;

import org.bukkit.entity.Player;

public interface SelectionProvider {
    boolean isAvailable();
    int[] getSelection(Player player);
}
