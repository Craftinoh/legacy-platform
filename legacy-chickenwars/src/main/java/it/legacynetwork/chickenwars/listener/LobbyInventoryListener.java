package it.legacynetwork.chickenwars.listener;

import it.legacynetwork.chickenwars.lobby.LobbySelectorHolder;
import it.legacynetwork.chickenwars.lobby.LobbySelectorService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;

/** Protegge top e bottom inventory mentre il selettore lobby è aperto. */
public final class LobbyInventoryListener implements Listener {
    private final LobbySelectorService selector;
    public LobbyInventoryListener(LobbySelectorService selector){this.selector=selector;}
    @EventHandler public void onClick(InventoryClickEvent event){Inventory top=event.getView().getTopInventory();if(top==null||!(top.getHolder() instanceof LobbySelectorHolder))return;event.setCancelled(true);if(event.getWhoClicked() instanceof org.bukkit.entity.Player&&event.getClickedInventory()==top&&event.getClick()==ClickType.LEFT&&event.getRawSlot()>=0){if(selector.select((org.bukkit.entity.Player)event.getWhoClicked(),(LobbySelectorHolder)top.getHolder(),event.getRawSlot()))event.getWhoClicked().closeInventory();}}
    @EventHandler public void onDrag(InventoryDragEvent event){Inventory top=event.getView().getTopInventory();if(top!=null&&top.getHolder() instanceof LobbySelectorHolder)event.setCancelled(true);}
    @EventHandler public void onDrop(PlayerDropItemEvent event){Inventory top=event.getPlayer().getOpenInventory().getTopInventory();if(top!=null&&top.getHolder() instanceof LobbySelectorHolder)event.setCancelled(true);}
}
