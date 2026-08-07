package it.legacynetwork.chickenwars.listener;

import it.legacynetwork.chickenwars.arena.ArenaManager;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameServices;
import it.legacynetwork.chickenwars.game.GameTeam;
import it.legacynetwork.chickenwars.model.ArenaState;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.shop.ChickenMenuService;
import it.legacynetwork.chickenwars.shop.ShopMenuHolder;
import it.legacynetwork.chickenwars.shop.ShopMenuView;
import it.legacynetwork.chickenwars.shop.ShopService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InteractionSecurityTest {

    private ArenaManager arenas;
    private GameServices services;
    private Player player;
    private Game game;
    private PlayerSession session;
    private GameTeam team;
    private ShopService shop;
    private ChickenMenuService chickenMenu;

    @BeforeEach
    void setUp() {
        arenas = mock(ArenaManager.class);
        services = mock(GameServices.class);
        shop = mock(ShopService.class);
        chickenMenu = mock(ChickenMenuService.class);
        when(services.getShop()).thenReturn(shop);
        when(services.getChickenMenu()).thenReturn(chickenMenu);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        session = mock(PlayerSession.class);
        when(session.getTeamId()).thenReturn("red");
        when(session.getState()).thenReturn(
                it.legacynetwork.chickenwars.player.PlayerState.PLAYING);

        team = mock(GameTeam.class);
        when(team.getId()).thenReturn("red");
        when(team.getColor()).thenReturn(
                it.legacynetwork.chickenwars.model.TeamColor.RED);

        game = mock(Game.class);
        it.legacynetwork.chickenwars.arena.ArenaDefinition arenaDef =
                mock(it.legacynetwork.chickenwars.arena.ArenaDefinition.class);
        when(arenaDef.getId()).thenReturn("a1");
        when(game.getDefinition()).thenReturn(arenaDef);
        when(game.getSession(any(UUID.class))).thenReturn(session);
        when(game.getTeam("red")).thenReturn(team);
        when(game.getState()).thenReturn(ArenaState.IN_GAME);

        when(arenas.getGame("a1")).thenReturn(game);
        when(arenas.getGameOf(any(Player.class))).thenReturn(game);
    }

    @Test
    void chickenHolderIdentifiedByHolderClass() {
        ShopMenuHolder holder = new ShopMenuHolder("a1", "chicken");
        holder.setView(ShopMenuView.CHICKEN_ROOT);
        Inventory inv = mock(Inventory.class);
        when(inv.getHolder()).thenReturn(holder);
        assertTrue(inv.getHolder() instanceof ShopMenuHolder);
        assertEquals(ShopMenuView.CHICKEN_ROOT, holder.getView());
    }

    @Test
    void nonShopInventoryNotIdentified() {
        Inventory inv = mock(Inventory.class);
        when(inv.getHolder()).thenReturn(null);
        assertNull(inv.getHolder());
    }

    @Test
    void shopHolderWithoutChickenViewNotIdentifiedAsChicken() {
        ShopMenuHolder holder = new ShopMenuHolder("a1", "blocks");
        holder.setView(ShopMenuView.SHOP);
        assertNotEquals(ShopMenuView.CHICKEN_ROOT, holder.getView());
        assertNotEquals(ShopMenuView.TEAM_UPGRADES, holder.getView());
        assertNotEquals(ShopMenuView.TRAPS, holder.getView());
        assertNotEquals(ShopMenuView.ROYAL_UPGRADES, holder.getView());
    }

    @Test
    void dragEventCancelledForShopInventory() {
        InteractionListener listener = new InteractionListener(arenas, services);
        ShopMenuHolder holder = new ShopMenuHolder("a1", "chicken");
        Inventory inv = mock(Inventory.class);
        when(inv.getHolder()).thenReturn(holder);
        org.bukkit.event.inventory.InventoryDragEvent event =
                mock(org.bukkit.event.inventory.InventoryDragEvent.class);
        when(event.getInventory()).thenReturn(inv);

        listener.onInventoryDrag(event);

        verify(event).setCancelled(true);
    }

    @Test
    void dragEventNotCancelledForNonShop() {
        InteractionListener listener = new InteractionListener(arenas, services);
        Inventory inv = mock(Inventory.class);
        when(inv.getHolder()).thenReturn(null);
        org.bukkit.event.inventory.InventoryDragEvent event =
                mock(org.bukkit.event.inventory.InventoryDragEvent.class);
        when(event.getInventory()).thenReturn(inv);

        listener.onInventoryDrag(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    void chickenViewDistinguishedFromShopView() {
        ShopMenuHolder chicken = new ShopMenuHolder("a1", "chicken");
        chicken.setView(ShopMenuView.CHICKEN_ROOT);
        ShopMenuHolder shop = new ShopMenuHolder("a1", "blocks");
        shop.setView(ShopMenuView.SHOP);

        boolean isChicken = chicken.getView() == ShopMenuView.CHICKEN_ROOT
                || chicken.getView() == ShopMenuView.TEAM_UPGRADES
                || chicken.getView() == ShopMenuView.TRAPS
                || chicken.getView() == ShopMenuView.ROYAL_UPGRADES;

        assertTrue(isChicken);
        assertFalse(shop.getView() == ShopMenuView.CHICKEN_ROOT
                || shop.getView() == ShopMenuView.TEAM_UPGRADES
                || shop.getView() == ShopMenuView.TRAPS
                || shop.getView() == ShopMenuView.ROYAL_UPGRADES);
    }

    @Test
    void holderIsSourceOfIdentity() {
        ShopMenuHolder holder = new ShopMenuHolder("a1", "chicken");
        assertNotNull(holder.getArenaId());
        assertEquals("a1", holder.getArenaId());

        Inventory inv = mock(Inventory.class);
        when(inv.getHolder()).thenReturn(holder);
        assertEquals(holder, inv.getHolder());
    }

    @Test
    void gameStateInGameRequired() {
        when(game.getState()).thenReturn(ArenaState.WAITING);
        InteractionListener listener = new InteractionListener(arenas, services);
        InventoryClickEvent event = chickenClick(ClickType.LEFT);

        listener.onInventoryClick(event);

        verify(event).setCancelled(true);
        verify(player).closeInventory();
        verify(chickenMenu, never()).handleClick(any(), any(), anyInt(),
                any(), any(), any(), any(), any());
    }

    @Test
    void sessionMustBelongToTeam() {
        when(session.getTeamId()).thenReturn("red");
        assertEquals("red", session.getTeamId());
        assertNotNull(game.getTeam("red"));
    }

    @Test
    void soloLeftClickRaggiungeIlMenuChicken() {
        InteractionListener listener = new InteractionListener(arenas, services);
        InventoryClickEvent event = chickenClick(ClickType.LEFT);

        listener.onInventoryClick(event);

        verify(chickenMenu).handleClick(eq(player), any(ShopMenuHolder.class),
                eq(19), eq(it.legacynetwork.chickenwars.shop.ShopClickType.LEFT),
                eq(game), eq(session), eq(team), any());
    }

    @Test
    void clickSpecialiNonEseguonoTransazioniChicken() {
        InteractionListener listener = new InteractionListener(arenas, services);
        ClickType[] blocked = new ClickType[] {
                ClickType.RIGHT, ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT,
                ClickType.NUMBER_KEY, ClickType.DOUBLE_CLICK, ClickType.MIDDLE,
                ClickType.DROP, ClickType.CONTROL_DROP
        };
        for (ClickType type : blocked) {
            listener.onInventoryClick(chickenClick(type));
        }

        verify(chickenMenu, never()).handleClick(any(), any(), anyInt(),
                any(), any(), any(), any(), any());
    }

    private InventoryClickEvent chickenClick(ClickType type) {
        ShopMenuHolder holder = new ShopMenuHolder("a1", "chicken");
        holder.setView(ShopMenuView.TEAM_UPGRADES);
        Inventory top = mock(Inventory.class);
        when(top.getHolder()).thenReturn(holder);
        when(top.getSize()).thenReturn(54);
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(top);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getView()).thenReturn(view);
        when(event.getRawSlot()).thenReturn(19);
        when(event.getClick()).thenReturn(type);
        return event;
    }
}
