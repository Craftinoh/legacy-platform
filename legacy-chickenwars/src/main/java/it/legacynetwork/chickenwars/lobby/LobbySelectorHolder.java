package it.legacynetwork.chickenwars.lobby;

import it.legacynetwork.chickenwars.mode.MatchMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** L'identita' del menu e la modalita' risiedono nell'holder, non nel titolo. */
public final class LobbySelectorHolder implements InventoryHolder {
    private final Map<Integer,MatchMode> modes;private Inventory inventory;
    public LobbySelectorHolder(Map<Integer,MatchMode> modes){this.modes=Collections.unmodifiableMap(new LinkedHashMap<Integer,MatchMode>(modes));}
    public MatchMode modeAt(int slot){return modes.get(Integer.valueOf(slot));}
    public void attach(Inventory inventory){if(this.inventory!=null)throw new IllegalStateException("Inventario gia' collegato");this.inventory=inventory;}
    @Override public Inventory getInventory(){return inventory;}
}
