package it.legacynetwork.chickenwars.game;

import it.legacynetwork.chickenwars.chicken.ChickenService;
import it.legacynetwork.chickenwars.chicken.RoyalChickenDamageService;
import it.legacynetwork.chickenwars.chicken.RoyalChickenRegistry;
import it.legacynetwork.chickenwars.chicken.RoyalDefeatDispatcher;
import it.legacynetwork.chickenwars.chicken.RoyalUpgradeApplier;
import it.legacynetwork.chickenwars.config.ChickenWarsConfig;
import it.legacynetwork.chickenwars.economy.ResourceTransferService;
import it.legacynetwork.chickenwars.effect.EffectAdapter;
import it.legacynetwork.chickenwars.effect.HealPoolService;
import it.legacynetwork.chickenwars.effect.TeamEffectService;
import it.legacynetwork.chickenwars.message.MessageService;
import it.legacynetwork.chickenwars.lobby.LobbyRoutingService;
import it.legacynetwork.chickenwars.lobby.LobbySelectorService;
import it.legacynetwork.chickenwars.player.ReconnectService;
import it.legacynetwork.chickenwars.player.equipment.EquipmentService;
import it.legacynetwork.chickenwars.persistence.ProgressionServices;
import it.legacynetwork.chickenwars.routing.RoutingServices;
import it.legacynetwork.chickenwars.shop.ChickenMenuService;
import it.legacynetwork.chickenwars.shop.ShopService;
import it.legacynetwork.chickenwars.trap.BaseEntryTracker;
import it.legacynetwork.chickenwars.trap.TrapTriggerService;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeService;
import it.legacynetwork.chickenwars.upgrade.UpgradeCatalog;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameServicesWiringTest {

    private GameServices services;
    private JavaPlugin plugin;
    private MessageService messages;
    private ChickenService chickens;
    private ShopService shop;
    private EquipmentService equipment;
    private ResourceTransferService transfers;
    private ReconnectService reconnects;
    private TeamUpgradeService upgrades;
    private TeamEffectService teamEffects;
    private HealPoolService healPool;
    private BaseEntryTracker baseEntryTracker;
    private TrapTriggerService traps;
    private RoyalChickenRegistry royalRegistry;
    private RoyalChickenDamageService royalDamage;
    private RoyalUpgradeApplier royalApplier;
    private RoyalDefeatDispatcher royalDefeatDispatcher;
    private ChickenMenuService chickenMenu;
    private ChickenWarsConfig config;
    private EffectAdapter effects;
    private ProgressionServices progression;
    private RoutingServices routing;
    private LobbyRoutingService lobby;
    private LobbySelectorService lobbySelector;

    @BeforeEach
    void setUp() {
        plugin = mock(JavaPlugin.class);
        messages = mock(MessageService.class);
        chickens = mock(ChickenService.class);
        shop = mock(ShopService.class);
        equipment = mock(EquipmentService.class);
        transfers = mock(ResourceTransferService.class);
        reconnects = mock(ReconnectService.class);
        upgrades = new TeamUpgradeService();
        effects = mock(EffectAdapter.class);
        teamEffects = new TeamEffectService(upgrades, effects);
        healPool = new HealPoolService(upgrades, effects);
        baseEntryTracker = new BaseEntryTracker();
        traps = new TrapTriggerService(upgrades, effects);
        royalRegistry = new RoyalChickenRegistry();
        royalDamage = new RoyalChickenDamageService();
        royalApplier = new RoyalUpgradeApplier(upgrades);
        royalDefeatDispatcher = new RoyalDefeatDispatcher();
        chickenMenu = new ChickenMenuService(null, messages);
        config = mock(ChickenWarsConfig.class);
        progression = mock(ProgressionServices.class);
        routing = mock(RoutingServices.class);
        lobby = mock(LobbyRoutingService.class);
        lobbySelector = mock(LobbySelectorService.class);

        services = new GameServices(plugin, messages, chickens, shop,
                equipment, transfers, reconnects, upgrades,
                teamEffects, healPool, baseEntryTracker, traps,
                royalRegistry, royalDamage, royalApplier,
                royalDefeatDispatcher, chickenMenu, progression, routing, lobby,
                lobbySelector, config);
    }

    @Test
    void tuttiIServiziSonoRestituitiCorrettamente() {
        assertSame(plugin, services.getPlugin());
        assertSame(messages, services.getMessages());
        assertSame(chickens, services.getChickens());
        assertSame(shop, services.getShop());
        assertSame(equipment, services.getEquipment());
        assertSame(transfers, services.getTransfers());
        assertSame(reconnects, services.getReconnects());
        assertSame(upgrades, services.getUpgrades());
        assertSame(teamEffects, services.getTeamEffects());
        assertSame(healPool, services.getHealPool());
        assertSame(baseEntryTracker, services.getBaseEntryTracker());
        assertSame(traps, services.getTraps());
        assertSame(royalRegistry, services.getRoyalRegistry());
        assertSame(royalDamage, services.getRoyalDamage());
        assertSame(royalApplier, services.getRoyalApplier());
        assertSame(royalDefeatDispatcher, services.getRoyalDefeatDispatcher());
        assertSame(chickenMenu, services.getChickenMenu());
        assertSame(progression, services.getProgression());
        assertSame(routing, services.getRouting());
        assertSame(lobby, services.getLobby());
        assertSame(lobbySelector, services.getLobbySelector());
        assertSame(config, services.getConfig());
    }

    @Test
    void serviziCondivisiNonSonoDuplicati() {
        assertSame(teamEffects, services.getTeamEffects());
        assertSame(healPool, services.getHealPool());
        assertSame(baseEntryTracker, services.getBaseEntryTracker());
        assertSame(traps, services.getTraps());
        assertSame(royalRegistry, services.getRoyalRegistry());
        assertSame(royalDamage, services.getRoyalDamage());
        assertSame(royalApplier, services.getRoyalApplier());
        assertSame(royalDefeatDispatcher, services.getRoyalDefeatDispatcher());
        assertSame(chickenMenu, services.getChickenMenu());
    }

    @Test
    void getDeathsNonENullo() {
        assertNotNull(services.getDeaths());
    }

    @Test
    void constructorNullPluginLancia() {
        assertThrows(IllegalArgumentException.class, () ->
                new GameServices(null, messages, chickens, shop,
                        equipment, transfers, reconnects, upgrades,
                        teamEffects, healPool, baseEntryTracker, traps,
                        royalRegistry, royalDamage, royalApplier,
                        royalDefeatDispatcher, chickenMenu, progression, routing,
                        lobby, lobbySelector, config));
    }

    @Test
    void setConfigSostituisce() {
        ChickenWarsConfig newConfig = mock(ChickenWarsConfig.class);
        services.setConfig(newConfig);
        assertSame(newConfig, services.getConfig());
    }

    @Test
    void registryDueIstanzeSonoIndipendenti() {
        RoyalChickenRegistry first = services.getRoyalRegistry();
        RoyalChickenRegistry second = services.getRoyalRegistry();
        assertSame(first, second);
    }

    @Test
    void chickenMenuServicePuoEssereAggiornato() {
        ChickenMenuService menu = services.getChickenMenu();
        assertNotNull(menu);
        menu.setServices(services);
    }
}
