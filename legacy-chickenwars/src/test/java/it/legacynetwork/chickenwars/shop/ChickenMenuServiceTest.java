package it.legacynetwork.chickenwars.shop;

import it.legacynetwork.chickenwars.effect.EffectAdapter;
import it.legacynetwork.chickenwars.effect.TeamEffectService;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameServices;
import it.legacynetwork.chickenwars.game.GameTeam;
import it.legacynetwork.chickenwars.message.MessageService;
import it.legacynetwork.chickenwars.mode.MatchMode;
import it.legacynetwork.chickenwars.mode.ModeProfile;
import it.legacynetwork.chickenwars.mode.ModeProfileRegistry;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.upgrade.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChickenMenuServiceTest {

    private ChickenMenuService menuService;
    private GameServices services;
    private Game game;
    private GameTeam team;
    private Player player;
    private PlayerSession session;
    private ModeProfile profile;
    private TeamUpgradeService upgrades;
    private TeamEffectService teamEffects;
    private MessageService messages;
    private MockedStatic<Bukkit> bukkitMock;

    @BeforeEach
    void setUp() throws Exception {
        org.bukkit.inventory.Inventory mockInventory = mock(org.bukkit.inventory.Inventory.class);
        when(mockInventory.getSize()).thenReturn(54);

        org.bukkit.inventory.ItemFactory itemFactory =
                mock(org.bukkit.inventory.ItemFactory.class);
        ItemMeta mockMeta = mock(ItemMeta.class);
        when(itemFactory.getItemMeta(any(Material.class))).thenReturn(mockMeta);

        bukkitMock = mockStatic(Bukkit.class);
        bukkitMock.when(() -> Bukkit.createInventory(any(), anyInt(), anyString()))
                .thenReturn(mockInventory);
        bukkitMock.when(() -> Bukkit.getItemFactory()).thenReturn(itemFactory);

        messages = mock(MessageService.class);
        when(messages.get(any(), anyString())).thenAnswer(inv -> inv.getArgument(1));
        when(messages.get(any(), anyString(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(1));
        when(messages.get(any(), anyString(), any(), any(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(1));
        when(messages.getList(any(), anyString()))
                .thenReturn(java.util.Collections.<String>emptyList());
        when(messages.getList(any(), anyString(), any(), any()))
                .thenReturn(java.util.Collections.<String>emptyList());

        upgrades = new TeamUpgradeService();
        String yaml = "team-upgrades:\n"
                + "  PROTECTION:\n"
                + "    icon: IRON_CHESTPLATE\n"
                + "    levels:\n"
                + "      1:\n"
                + "        amplifier: 0\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 2}\n"
                + "  SHARPNESS:\n"
                + "    icon: IRON_SWORD\n"
                + "    levels:\n"
                + "      1:\n"
                + "        amplifier: 0\n"
                + "        cost:\n"
                + "          solo_duel: {currency: IRON, amount: 10}\n"
                + "royal-upgrades:\n"
                + "  ROYAL_ARMOR:\n"
                + "    icon: IRON_CHESTPLATE\n"
                + "    levels:\n"
                + "      1:\n"
                + "        value: 0.25\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 2}\n"
                + "  ROYAL_VITALITY:\n"
                + "    icon: GOLDEN_APPLE\n"
                + "    levels:\n"
                + "      1:\n"
                + "        value: 25.0\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 5}\n"
                + "  ROYAL_GUARD:\n"
                + "    icon: IRON_FENCE\n"
                + "    levels:\n"
                + "      1:\n"
                + "        duration-ticks: 40\n"
                + "        cost:\n"
                + "          solo_duel: {currency: DIAMOND, amount: 2}\n";
        org.bukkit.configuration.file.YamlConfiguration config =
                new org.bukkit.configuration.file.YamlConfiguration();
        config.loadFromString(yaml);
        upgrades.setCatalog(UpgradeConfigLoader.load(config));

        FakeEffectAdapter effects = new FakeEffectAdapter();
        teamEffects = new TeamEffectService(upgrades, effects);

        services = mock(GameServices.class);
        when(services.getUpgrades()).thenReturn(upgrades);
        when(services.getMessages()).thenReturn(messages);
        when(services.getTeamEffects()).thenReturn(teamEffects);

        it.legacynetwork.chickenwars.chicken.RoyalUpgradeApplier applier =
                new it.legacynetwork.chickenwars.chicken.RoyalUpgradeApplier(upgrades);
        when(services.getRoyalApplier()).thenReturn(applier);

        when(services.getShop()).thenReturn(mock(ShopService.class));

        menuService = new ChickenMenuService(services, messages);

        game = mock(Game.class);
        it.legacynetwork.chickenwars.arena.ArenaDefinition arenaDef =
                mock(it.legacynetwork.chickenwars.arena.ArenaDefinition.class);
        when(arenaDef.getId()).thenReturn("a1");
        when(game.getDefinition()).thenReturn(arenaDef);
        when(game.getState()).thenReturn(
                it.legacynetwork.chickenwars.model.ArenaState.IN_GAME);
        when(game.getSession(any())).thenReturn(mock(PlayerSession.class));

        it.legacynetwork.chickenwars.arena.TeamDefinition teamDef =
                mock(it.legacynetwork.chickenwars.arena.TeamDefinition.class);
        when(teamDef.getId()).thenReturn("red");

        team = mock(GameTeam.class);
        when(team.getId()).thenReturn("red");
        when(team.getDefinition()).thenReturn(teamDef);
        when(team.getColor()).thenReturn(
                it.legacynetwork.chickenwars.model.TeamColor.RED);

        session = mock(PlayerSession.class);
        when(session.getState()).thenReturn(
                it.legacynetwork.chickenwars.player.PlayerState.PLAYING);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getLocation()).thenReturn(
                new org.bukkit.Location(null, 0, 65, 0));

        org.bukkit.inventory.PlayerInventory playerInv =
                mock(org.bukkit.inventory.PlayerInventory.class);
        when(playerInv.getContents()).thenReturn(new ItemStack[36]);
        when(player.getInventory()).thenReturn(playerInv);

        org.bukkit.inventory.InventoryView view =
                mock(org.bukkit.inventory.InventoryView.class);
        when(view.getTopInventory()).thenReturn(mockInventory);
        when(player.getOpenInventory()).thenReturn(view);

        profile = ModeProfileRegistry.defaults().get(MatchMode.SOLO);
    }

    @AfterEach
    void tearDown() {
        if (bukkitMock != null) {
            bukkitMock.close();
        }
    }

    @Test
    void purchaseProtectionSuccess() {
        giveDiamonds(5);
        ShopMenuHolder holder = new ShopMenuHolder("a1", "chicken");
        holder.setView(ShopMenuView.TEAM_UPGRADES);
        menuService.handleClick(player, holder, 19, ShopClickType.LEFT,
                game, session, team, profile);
        assertEquals(1, upgrades.peekState("a1", "red")
                .getLevel(TeamUpgradeType.PROTECTION));
        verify(game).applyTeamUpgrade(team, TeamUpgradeType.PROTECTION);
    }

    @Test
    void purchaseMaxLevelFails() {
        giveDiamonds(10);
        upgrades.getState("a1", "red")
                .reserveNextLevel(TeamUpgradeType.PROTECTION, 1);
        ShopMenuHolder holder = new ShopMenuHolder("a1", "chicken");
        holder.setView(ShopMenuView.TEAM_UPGRADES);
        menuService.handleClick(player, holder, 19, ShopClickType.LEFT,
                game, session, team, profile);
        assertEquals(1, upgrades.peekState("a1", "red")
                .getLevel(TeamUpgradeType.PROTECTION));
    }

    @Test
    void purchaseNotEnoughResources() {
        ShopMenuHolder holder = new ShopMenuHolder("a1", "chicken");
        holder.setView(ShopMenuView.TEAM_UPGRADES);
        menuService.handleClick(player, holder, 19, ShopClickType.LEFT,
                game, session, team, profile);
        assertEquals(0, upgrades.peekState("a1", "red")
                .getLevel(TeamUpgradeType.PROTECTION));
    }

    @Test
    void purchaseRoyalArmorSuccess() {
        giveDiamonds(10);
        ShopMenuHolder holder = new ShopMenuHolder("a1", "chicken");
        holder.setView(ShopMenuView.ROYAL_UPGRADES);
        int slot = findRoyalSlot(RoyalUpgradeType.ROYAL_ARMOR);
        menuService.handleClick(player, holder, slot, ShopClickType.LEFT,
                game, session, team, profile);
        assertEquals(1, upgrades.peekState("a1", "red")
                .getRoyalLevel(RoyalUpgradeType.ROYAL_ARMOR));
    }

    @Test
    void purchaseRoyalVitalitySuccess() {
        giveDiamonds(10);
        ShopMenuHolder holder = new ShopMenuHolder("a1", "chicken");
        holder.setView(ShopMenuView.ROYAL_UPGRADES);
        int slot = findRoyalSlot(RoyalUpgradeType.ROYAL_VITALITY);
        menuService.handleClick(player, holder, slot, ShopClickType.LEFT,
                game, session, team, profile);
        assertEquals(1, upgrades.peekState("a1", "red")
                .getRoyalLevel(RoyalUpgradeType.ROYAL_VITALITY));
    }

    @Test
    void purchaseRoyalGuardSuccess() {
        giveDiamonds(10);
        ShopMenuHolder holder = new ShopMenuHolder("a1", "chicken");
        holder.setView(ShopMenuView.ROYAL_UPGRADES);
        int slot = findRoyalSlot(RoyalUpgradeType.ROYAL_GUARD);
        menuService.handleClick(player, holder, slot, ShopClickType.LEFT,
                game, session, team, profile);
        assertEquals(1, upgrades.peekState("a1", "red")
                .getRoyalLevel(RoyalUpgradeType.ROYAL_GUARD));
    }

    @Test
    void shiftClickIgnored() {
        giveDiamonds(10);
        ShopMenuHolder holder = new ShopMenuHolder("a1", "chicken");
        holder.setView(ShopMenuView.TEAM_UPGRADES);
        menuService.handleClick(player, holder, 19,
                ShopClickType.SHIFT_LEFT, game, session, team, profile);
        assertEquals(0, getProtectionLevel());
    }

    @Test
    void rightClickIgnored() {
        giveDiamonds(10);
        ShopMenuHolder holder = new ShopMenuHolder("a1", "chicken");
        holder.setView(ShopMenuView.TEAM_UPGRADES);
        menuService.handleClick(player, holder, 19,
                ShopClickType.RIGHT, game, session, team, profile);
        assertEquals(0, getProtectionLevel());
    }

    @Test
    void singleTransactionPerClick() {
        giveDiamonds(20);
        ShopMenuHolder holder = new ShopMenuHolder("a1", "chicken");
        holder.setView(ShopMenuView.TEAM_UPGRADES);
        menuService.handleClick(player, holder, 19, ShopClickType.LEFT,
                game, session, team, profile);
        assertEquals(1, upgrades.peekState("a1", "red")
                .getLevel(TeamUpgradeType.PROTECTION));
        menuService.handleClick(player, holder, 19, ShopClickType.LEFT,
                game, session, team, profile);
        assertEquals(1, upgrades.peekState("a1", "red")
                .getLevel(TeamUpgradeType.PROTECTION));
    }

    @Test
    void endingNonConsenteAcquisti() {
        when(game.getState()).thenReturn(
                it.legacynetwork.chickenwars.model.ArenaState.ENDING);
        giveDiamonds(5);
        ShopMenuHolder holder = new ShopMenuHolder("a1", "chicken");
        holder.setView(ShopMenuView.TEAM_UPGRADES);

        menuService.handleClick(player, holder, 19, ShopClickType.LEFT,
                game, session, team, profile);

        assertNull(upgrades.peekState("a1", "red"));
        verify(game, never()).applyTeamUpgrade(any(), any());
    }

    @Test
    void navigationRootToShop() {
        ShopMenuHolder holder = new ShopMenuHolder("a1", "chicken");
        holder.setView(ShopMenuView.CHICKEN_ROOT);
        menuService.handleClick(player, holder, 45,
                ShopClickType.LEFT, game, session, team, profile);
    }

    @Test
    void handleClickRootBackChiude() {
        ShopMenuHolder holder = new ShopMenuHolder("a1", "chicken");
        holder.setView(ShopMenuView.CHICKEN_ROOT);
        menuService.handleClick(player, holder, 49,
                ShopClickType.LEFT, game, session, team, profile);
    }

    @Test
    void nullParametersHandledGracefully() {
        assertDoesNotThrow(() ->
                menuService.handleClick(null, null, 0, null, null, null, null,
                        null));
    }

    @Test
    void holderViewCategorizationWorks() {
        ShopMenuHolder holder = new ShopMenuHolder("a1", "chicken");
        holder.setView(ShopMenuView.CHICKEN_ROOT);
        assertEquals(ShopMenuView.CHICKEN_ROOT, holder.getView());
        holder.setView(ShopMenuView.TEAM_UPGRADES);
        assertEquals(ShopMenuView.TEAM_UPGRADES, holder.getView());
        holder.setView(ShopMenuView.TRAPS);
        assertEquals(ShopMenuView.TRAPS, holder.getView());
        holder.setView(ShopMenuView.ROYAL_UPGRADES);
        assertEquals(ShopMenuView.ROYAL_UPGRADES, holder.getView());
    }

    private void giveDiamonds(int amount) {
        ItemStack diamond = new ItemStack(Material.DIAMOND, amount);
        ItemStack[] contents = new ItemStack[36];
        contents[0] = diamond;
        when(player.getInventory().getContents()).thenReturn(contents);
    }

    private int getProtectionLevel() {
        TeamUpgradeState state = upgrades.peekState("a1", "red");
        return state == null ? 0 : state.getLevel(TeamUpgradeType.PROTECTION);
    }

    private int findRoyalSlot(RoyalUpgradeType type) {
        int index = 0;
        for (RoyalUpgradeType t : RoyalUpgradeType.values()) {
            if (upgrades.getCatalog().getRoyalUpgrade(t) != null) {
                if (t == type) return 19 + index;
                index++;
            }
        }
        return -1;
    }

    private static class FakeEffectAdapter implements EffectAdapter {
        @Override
        public boolean apply(UUID playerId,
                             org.bukkit.potion.PotionEffectType type,
                             int durationTicks, int amplifier) {
            return true;
        }

        @Override
        public boolean clear(UUID playerId,
                             org.bukkit.potion.PotionEffectType type) {
            return true;
        }
    }
}
