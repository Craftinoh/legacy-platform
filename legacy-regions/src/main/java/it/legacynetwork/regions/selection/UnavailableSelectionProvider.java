package it.legacynetwork.regions.selection;

import org.bukkit.entity.Player;

public final class UnavailableSelectionProvider implements SelectionProvider {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public RegionSelection getSelection(Player player) {
        return null;
    }
}
