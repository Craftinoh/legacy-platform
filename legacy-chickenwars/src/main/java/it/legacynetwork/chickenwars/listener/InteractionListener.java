package it.legacynetwork.chickenwars.listener;

import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.chicken.RoyalChickenRegistry;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameServices;
import it.legacynetwork.chickenwars.game.GameTeam;
import it.legacynetwork.chickenwars.model.ArenaState;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.shop.ShopClickType;
import it.legacynetwork.chickenwars.shop.ShopMenuHolder;
import it.legacynetwork.chickenwars.shop.ShopMenuView;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.ClickType;
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
                    session, team == null ? null : team.getColor(),
                    game.getDefinition().getModeProfile());
            return;
        }

        if (game.isUpgradesNpc(event.getRightClicked())) {
            event.setCancelled(true);
            GameTeam team = game.getTeam(session.getTeamId());
            if (team != null) {
                services.getChickenMenu().openChickenRoot(player, game, session,
                        team, game.getDefinition().getModeProfile());
            }
            return;
        }

        RoyalChickenRegistry.Entry royal = services.getRoyalRegistry()
                .lookup(event.getRightClicked().getUniqueId());
        GameTeam owner = royal != null
                && royal.belongsTo(game.getDefinition().getId())
                ? game.getTeam(royal.getTeamId()) : null;
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
        Inventory top = event.getView() == null
                ? event.getInventory() : event.getView().getTopInventory();
        ShopMenuHolder holder = resolveHolder(top);
        if (holder == null) {
            return;
        }

        event.setCancelled(true);
        if (event.getRawSlot() < 0
                || event.getRawSlot() >= top.getSize()) {
            return;
        }

        Game game = arenas.getGame(holder.getArenaId());
        if (game == null) {
            player.closeInventory();
            return;
        }
        PlayerSession session = game.getSession(player.getUniqueId());
        if (session == null || !session.getState().isActive()
                || game.getState() != ArenaState.IN_GAME) {
            player.closeInventory();
            return;
        }

        GameTeam team = game.getTeam(session.getTeamId());
        if (team == null) {
            player.closeInventory();
            return;
        }

        if (holder.getView() == ShopMenuView.CHICKEN_ROOT
                || holder.getView() == ShopMenuView.TEAM_UPGRADES
                || holder.getView() == ShopMenuView.TRAPS
                || holder.getView() == ShopMenuView.ROYAL_UPGRADES) {
            if (event.getClick() != ClickType.LEFT) {
                return;
            }
            services.getChickenMenu().handleClick(player, holder,
                    event.getRawSlot(),
                    ShopClickType.of(event.isShiftClick(), event.isRightClick()),
                    game, session, team, game.getDefinition().getModeProfile());
            return;
        }

        services.getShop().handleClick(player, holder, event.getRawSlot(),
                ShopClickType.of(event.isShiftClick(), event.isRightClick()),
                session, team.getColor(),
                game.getDefinition().getModeProfile());
    }

    /**
     * Impedisce di trascinare oggetti dentro il menu dello shop.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView() == null
                ? event.getInventory() : event.getView().getTopInventory();
        if (resolveHolder(top) != null) {
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
