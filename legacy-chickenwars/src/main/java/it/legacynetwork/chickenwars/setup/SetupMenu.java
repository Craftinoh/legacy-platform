package it.legacynetwork.chickenwars.setup;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Menu dell'editor, riconosciuto tramite il proprio holder.
 */
public final class SetupMenu implements InventoryHolder {

    /** Tipologie di menu disponibili nell'editor. */
    public enum Type {
        /** Lobby, spettatori e angoli della regione. */
        POSITIONS,
        /** Elenco squadre configurate e selezione di quella attiva. */
        TEAMS,
        /** Colori disponibili per creare una nuova squadra. */
        TEAM_COLORS,
        /** Tipi di generatore da posizionare. */
        GENERATORS
    }

    private final Type type;
    private final String arenaId;

    public SetupMenu(Type type, String arenaId) {
        this.type = type;
        this.arenaId = arenaId;
    }

    public Type getType() {
        return type;
    }

    public String getArenaId() {
        return arenaId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
