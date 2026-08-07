package it.legacynetwork.chickenwars.listener;

import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameServices;
import it.legacynetwork.chickenwars.game.GameTeam;
import it.legacynetwork.chickenwars.model.ArenaState;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.shop.ShopMenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Interazioni con venditori, Gallina Reale e menu dello shop.
 */
public final class InteractionListener implements Listener {

    private final ArenaManager arenas;
    private final GameServices services;

    public InteractionListener(ArenaManager arenas, GameServices services) {
        this.arenas = arenas;
        this.services = services;
    }

    /**
     * Apre lo shop sui venditori e consente di nutrire la propria gallina.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Game game = arenas.getGameOf(player);
        if (game == null) {
            return;
        }

        PlayerSession session = game.getSession(player.getUniqueId());
        if (session == null || !session.getState().isActive()
                || game.getState() != ArenaState.IN_GAME) {
            event.setCancelled(true);
            return;
        }

        if (game.isShopNpc(event.getRightClicked())) {
            event.setCancelled(true);
            GameTeam team = game.getTeam(session.getTeamId());
            services.getShop().open(player, game.getDefinition().getId(), null,
                    team == null ? null : team.getColor());
            return;
        }

        GameTeam owner = game.findChickenOwner(event.getRightClicked());
        if (owner == null) {
            return;
        }

        event.setCancelled(true);
        ItemStack held = player.getItemInHand();
        if (held == null
                || held.getType() != services.getConfig()
                .getChicken().getFeedMaterial()) {
            return;
        }
        if (game.feedChicken(owner, player)) {
            consumeOne(player, held);
        }
    }

    private void consumeOne(Player player, ItemStack held) {
        if (held.getAmount() <= 1) {
            player.setItemInHand(null);
        } else {
            held.setAmount(held.getAmount() - 1);
            player.setItemInHand(held);
        }
        player.updateInventory();
    }

    /**
     * Instrada i click del menu shop e impedisce di prelevarne le icone.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        ShopMenuHolder holder = resolveHolder(event.getInventory());
        if (holder == null) {
            return;
        }

        event.setCancelled(true);
        if (event.getRawSlot() < 0
                || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }

        Game game = arenas.getGame(holder.getArenaId());
        if (game == null) {
            player.closeInventory();
            return;
        }
        PlayerSession session = game.getSession(player.getUniqueId());
        if (session == null || !session.getState().isActive()) {
            player.closeInventory();
            return;
        }

        GameTeam team = game.getTeam(session.getTeamId());
        services.getShop().handleClick(player, holder, event.getRawSlot(),
                team == null ? null : team.getColor());
    }

    /**
     * Impedisce di trascinare oggetti dentro il menu dello shop.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (resolveHolder(event.getInventory()) != null) {
            event.setCancelled(true);
        }
    }

    private ShopMenuHolder resolveHolder(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        InventoryHolder holder = inventory.getHolder();
        return holder instanceof ShopMenuHolder ? (ShopMenuHolder) holder : null;
    }
}
