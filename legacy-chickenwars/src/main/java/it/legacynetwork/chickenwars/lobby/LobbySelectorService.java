package it.legacynetwork.chickenwars.lobby;

import it.legacynetwork.chickenwars.message.MessageService;
import it.legacynetwork.chickenwars.mode.MatchMode;
import it.legacynetwork.chickenwars.mode.ModeProfileRegistry;
import it.legacynetwork.chickenwars.persistence.ProfileLifecycleService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Selettore modalità 1.8.8, privo di stato autorevole oltre all'holder. */
public final class LobbySelectorService {
    private final LobbyRoutingService routing;private final MessageService messages;
    private final ProfileLifecycleService profiles;
    public LobbySelectorService(LobbyRoutingService routing,MessageService messages,ProfileLifecycleService profiles){this.routing=routing;this.messages=messages;this.profiles=profiles;}
    public void open(Player player){Map<Integer,MatchMode> slots=new LinkedHashMap<Integer,MatchMode>();slots.put(1,MatchMode.DUEL);slots.put(3,MatchMode.SOLO);slots.put(5,MatchMode.DOUBLES);slots.put(7,MatchMode.TRIO);LobbySelectorHolder holder=new LobbySelectorHolder(slots);Inventory inventory=Bukkit.createInventory(holder,9,messages.get(player,"lobby.selector"));holder.attach(inventory);for(Map.Entry<Integer,MatchMode> entry:slots.entrySet())inventory.setItem(entry.getKey().intValue(),item(entry.getValue()));player.openInventory(inventory);}
    private ItemStack item(MatchMode mode){ItemStack item=new ItemStack(Material.EGG);ItemMeta meta=item.getItemMeta();meta.setDisplayName(ChatColor.YELLOW+mode.name());item.setItemMeta(meta);return item;}
    public boolean select(Player player,LobbySelectorHolder holder,int slot){MatchMode mode=holder==null?null:holder.modeAt(slot);if(mode==null)return false;if(ModeProfileRegistry.defaults().get(mode).isTracked()&&!profiles.mayEnterTracked(player.getUniqueId())){messages.send(player,"persistence.profile-unavailable");return false;}return routing.join(player,mode,System.currentTimeMillis());}
}
