package it.legacynetwork.chickenwars.shop;

import it.legacynetwork.chickenwars.economy.ResourceWallet;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameServices;
import it.legacynetwork.chickenwars.game.GameTeam;
import it.legacynetwork.chickenwars.message.MessageService;
import it.legacynetwork.chickenwars.mode.ModeProfile;
import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.model.TeamColor;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.upgrade.RoyalUpgradeDefinition;
import it.legacynetwork.chickenwars.upgrade.RoyalUpgradeType;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeDefinition;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeState;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeType;
import it.legacynetwork.chickenwars.upgrade.TrapDefinition;
import it.legacynetwork.chickenwars.upgrade.UpgradeCatalog;
import it.legacynetwork.chickenwars.upgrade.UpgradeLevel;
import it.legacynetwork.chickenwars.upgrade.UpgradeResult;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ChickenMenuService {

    private static final int MENU_SIZE = 54;
    private static final int ROOT_SLOT_TEAM = 20;
    private static final int ROOT_SLOT_TRAPS = 22;
    private static final int ROOT_SLOT_ROYAL = 24;
    private static final int BACK_SLOT = 49;
    private static final int SHOP_SLOT = 45;
    private static final int FIRST_ITEM_SLOT = 19;

    private final MessageService messages;
    private volatile GameServices services;

    public ChickenMenuService(GameServices services, MessageService messages) {
        this.services = services;
        this.messages = messages;
    }

    public void setServices(GameServices services) {
        this.services = services;
    }

    public void openChickenRoot(Player player, Game game, PlayerSession session,
                                GameTeam team, ModeProfile profile) {
        String arenaId = game.getDefinition().getId();
        ShopMenuHolder holder = new ShopMenuHolder(arenaId, "chicken");
        holder.setView(ShopMenuView.CHICKEN_ROOT);
        Inventory inv = Bukkit.createInventory(holder, MENU_SIZE,
                messages.get(player, "chicken.menu.title"));
        renderChickenRoot(inv, player, game, session, team, profile);
        player.openInventory(inv);
    }

    public void renderChickenRoot(Inventory inv, Player player, Game game,
                                  PlayerSession session, GameTeam team,
                                  ModeProfile profile) {
        inv.clear();
        UpgradeCatalog catalog = services.getUpgrades().getCatalog();

        inv.setItem(ROOT_SLOT_TEAM, createButton(player, Material.IRON_CHESTPLATE,
                "chicken.menu.team-upgrades", "chicken.menu.team-upgrades-lore"));
        inv.setItem(ROOT_SLOT_TRAPS, createButton(player, Material.TRIPWIRE_HOOK,
                "chicken.menu.traps", "chicken.menu.traps-lore"));
        inv.setItem(ROOT_SLOT_ROYAL, createButton(player, Material.GOLDEN_APPLE,
                "chicken.menu.royal-upgrades", "chicken.menu.royal-upgrades-lore"));

        inv.setItem(BACK_SLOT, createButton(player, Material.BARRIER,
                "chicken.menu.close", "chicken.menu.close-lore"));
        inv.setItem(SHOP_SLOT, createButton(player, Material.CHEST,
                "chicken.menu.shop", "chicken.menu.shop-lore"));

        if (team.getChicken() != null) {
            double health = team.getChicken().getVitals().getHealth();
            double max = team.getChicken().getVitals().getMaxHealth();
            String state = team.getChicken().isAlive() ? "alive" : "dead";
            inv.setItem(4, createInfoItem(player, Material.EGG,
                    "chicken.status." + state,
                    "{health}", String.valueOf((int) health),
                    "{max}", String.valueOf((int) max)));
        }
    }

    public void openTeamUpgrades(Player player, Game game, PlayerSession session,
                                 GameTeam team, ModeProfile profile) {
        String arenaId = game.getDefinition().getId();
        ShopMenuHolder holder = new ShopMenuHolder(arenaId, "chicken");
        holder.setView(ShopMenuView.TEAM_UPGRADES);
        Inventory inv = Bukkit.createInventory(holder, MENU_SIZE,
                messages.get(player, "chicken.menu.team-upgrades-title"));
        renderTeamUpgrades(inv, player, game, session, team, profile);
        player.openInventory(inv);
    }

    public void renderTeamUpgrades(Inventory inv, Player player, Game game,
                                   PlayerSession session, GameTeam team,
                                   ModeProfile profile) {
        inv.clear();
        UpgradeCatalog catalog = services.getUpgrades().getCatalog();
        TeamUpgradeState state = services.getUpgrades().peekState(
                game.getDefinition().getId(), team.getId());

        int slot = FIRST_ITEM_SLOT;
        for (TeamUpgradeType type : TeamUpgradeType.values()) {
            TeamUpgradeDefinition def = catalog.getTeamUpgrade(type);
            if (def == null) {
                continue;
            }
            inv.setItem(slot, createTeamUpgradeIcon(player, game, team, def,
                    state, profile));
            slot++;
        }

        inv.setItem(BACK_SLOT, createButton(player, Material.ARROW,
                "chicken.menu.back", "chicken.menu.back-lore"));
        inv.setItem(SHOP_SLOT, createButton(player, Material.CHEST,
                "chicken.menu.shop", "chicken.menu.shop-lore"));
    }

    @SuppressWarnings("deprecation")
    private ItemStack createTeamUpgradeIcon(Player player, Game game,
                                            GameTeam team,
                                            TeamUpgradeDefinition def,
                                            TeamUpgradeState state,
                                            ModeProfile profile) {
        int level = state == null ? 0 : state.getLevel(def.getType());
        int maxLevel = def.getMaximumLevel();
        boolean maxed = level >= maxLevel;

        ItemStack icon = new ItemStack(def.getIcon(), 1, def.getIconData());
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) {
            return icon;
        }
        meta.setDisplayName(messages.get(player, def.getNameKey()));
        List<String> lore = new ArrayList<String>();
        lore.addAll(messages.getList(player, def.getLoreKey()));
        lore.add("");
        lore.add(messages.get(player, "upgrade.level",
                "{level}", String.valueOf(level),
                "{max}", String.valueOf(maxLevel)));

        if (!maxed) {
            UpgradeLevel nextLevel = def.getLevel(level + 1);
            ItemCost cost = nextLevel == null ? null
                    : nextLevel.getCost(profile.getPricingProfile());
            if (cost != null) {
                lore.add(messages.get(player, "upgrade.next-cost",
                        "{amount}", String.valueOf(cost.getAmount()),
                        "{currency}", messages.get(player,
                                "shop.currency." + cost.getCurrency().name()
                                        .toLowerCase())));
                boolean canAfford = ResourceWallet.count(player,
                        cost.getCurrency()) >= cost.getAmount();
                lore.add(messages.get(player,
                        canAfford ? "shop.lore.affordable"
                                : "shop.lore.not-affordable"));
            }
        } else {
            lore.add(messages.get(player, "upgrade.max-level",
                    "{upgrade}", messages.get(player, def.getNameKey())));
        }

        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    public void openTraps(Player player, Game game, PlayerSession session,
                          GameTeam team, ModeProfile profile) {
        String arenaId = game.getDefinition().getId();
        ShopMenuHolder holder = new ShopMenuHolder(arenaId, "chicken");
        holder.setView(ShopMenuView.TRAPS);
        Inventory inv = Bukkit.createInventory(holder, MENU_SIZE,
                messages.get(player, "chicken.menu.traps-title"));
        renderTraps(inv, player, game, session, team, profile);
        player.openInventory(inv);
    }

    @SuppressWarnings("deprecation")
    public void renderTraps(Inventory inv, Player player, Game game,
                            PlayerSession session, GameTeam team,
                            ModeProfile profile) {
        inv.clear();
        UpgradeCatalog catalog = services.getUpgrades().getCatalog();
        TeamUpgradeState state = services.getUpgrades().peekState(
                game.getDefinition().getId(), team.getId());
        int trapCount = state == null ? 0 : state.getTrapCount();
        int maxTraps = catalog.getMaximumTraps();

        int slot = FIRST_ITEM_SLOT;
        for (TrapDefinition def : catalog.getTraps().values()) {
            if (slot >= BACK_SLOT) {
                break;
            }
            inv.setItem(slot, createTrapIcon(player, game, team, def,
                    trapCount, maxTraps, profile));
            slot++;
        }

        List<String> queue = state == null
                ? new ArrayList<String>() : state.getTrapQueue();
        for (int i = 0; i < maxTraps && i < 9; i++) {
            Material mat = i < queue.size() ? Material.ENDER_PEARL
                    : Material.GLASS_BOTTLE;
            String name = i < queue.size()
                    ? messages.get(player, "trap." + queue.get(i) + ".name")
                    : messages.get(player, "trap.queue-empty-slot");
            inv.setItem(36 + i, createSimpleButton(mat, name));
        }

        inv.setItem(BACK_SLOT, createButton(player, Material.ARROW,
                "chicken.menu.back", "chicken.menu.back-lore"));
        inv.setItem(SHOP_SLOT, createButton(player, Material.CHEST,
                "chicken.menu.shop", "chicken.menu.shop-lore"));
    }

    @SuppressWarnings("deprecation")
    private ItemStack createTrapIcon(Player player, Game game, GameTeam team,
                                     TrapDefinition def, int trapCount,
                                     int maxTraps, ModeProfile profile) {
        ItemStack icon = new ItemStack(def.getIcon(), 1, def.getIconData());
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) {
            return icon;
        }
        meta.setDisplayName(messages.get(player, def.getNameKey()));
        List<String> lore = new ArrayList<String>();
        lore.addAll(messages.getList(player, def.getLoreKey()));
        lore.add("");

        boolean full = trapCount >= maxTraps;
        lore.add(messages.get(player, "trap.queue-count",
                "{count}", String.valueOf(trapCount),
                "{max}", String.valueOf(maxTraps)));

        if (!full) {
            ItemCost cost = services.getUpgrades().getCatalog()
                    .getTrapCost(trapCount, profile);
            if (cost != null) {
                lore.add(messages.get(player, "upgrade.next-cost",
                        "{amount}", String.valueOf(cost.getAmount()),
                        "{currency}", messages.get(player,
                                "shop.currency." + cost.getCurrency().name()
                                        .toLowerCase())));
                boolean canAfford = ResourceWallet.count(player,
                        cost.getCurrency()) >= cost.getAmount();
                lore.add(messages.get(player,
                        canAfford ? "shop.lore.affordable"
                                : "shop.lore.not-affordable"));
            }
        } else {
            lore.add(messages.get(player, "trap.queue-full"));
        }

        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    public void openRoyalUpgrades(Player player, Game game, PlayerSession session,
                                  GameTeam team, ModeProfile profile) {
        String arenaId = game.getDefinition().getId();
        ShopMenuHolder holder = new ShopMenuHolder(arenaId, "chicken");
        holder.setView(ShopMenuView.ROYAL_UPGRADES);
        Inventory inv = Bukkit.createInventory(holder, MENU_SIZE,
                messages.get(player, "chicken.menu.royal-upgrades-title"));
        renderRoyalUpgrades(inv, player, game, session, team, profile);
        player.openInventory(inv);
    }

    @SuppressWarnings("deprecation")
    public void renderRoyalUpgrades(Inventory inv, Player player, Game game,
                                    PlayerSession session, GameTeam team,
                                    ModeProfile profile) {
        inv.clear();
        UpgradeCatalog catalog = services.getUpgrades().getCatalog();
        TeamUpgradeState state = services.getUpgrades().peekState(
                game.getDefinition().getId(), team.getId());

        if (team.getChicken() != null) {
            double health = team.getChicken().getVitals().getHealth();
            double max = team.getChicken().getVitals().getMaxHealth();
            inv.setItem(4, createInfoItem(player, Material.EGG,
                    "chicken.status.alive",
                    "{health}", String.valueOf((int) health),
                    "{max}", String.valueOf((int) max)));
        }

        int slot = FIRST_ITEM_SLOT;
        for (RoyalUpgradeType type : RoyalUpgradeType.values()) {
            RoyalUpgradeDefinition def = catalog.getRoyalUpgrade(type);
            if (def == null) {
                continue;
            }
            inv.setItem(slot, createRoyalUpgradeIcon(player, game, team, def,
                    state, profile));
            slot++;
        }

        inv.setItem(BACK_SLOT, createButton(player, Material.ARROW,
                "chicken.menu.back", "chicken.menu.back-lore"));
        inv.setItem(SHOP_SLOT, createButton(player, Material.CHEST,
                "chicken.menu.shop", "chicken.menu.shop-lore"));
    }

    @SuppressWarnings("deprecation")
    private ItemStack createRoyalUpgradeIcon(Player player, Game game,
                                             GameTeam team,
                                             RoyalUpgradeDefinition def,
                                             TeamUpgradeState state,
                                             ModeProfile profile) {
        int level = state == null ? 0
                : state.getRoyalLevel(def.getType());
        int maxLevel = def.getMaximumLevel();
        boolean maxed = level >= maxLevel;

        ItemStack icon = new ItemStack(def.getIcon(), 1, def.getIconData());
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) {
            return icon;
        }
        meta.setDisplayName(messages.get(player, def.getNameKey()));
        List<String> lore = new ArrayList<String>();
        lore.addAll(messages.getList(player, def.getLoreKey()));
        lore.add("");
        lore.add(messages.get(player, "upgrade.level",
                "{level}", String.valueOf(level),
                "{max}", String.valueOf(maxLevel)));

        if (!maxed) {
            UpgradeLevel nextLevel = def.getLevel(level + 1);
            ItemCost cost = nextLevel == null ? null
                    : nextLevel.getCost(profile.getPricingProfile());
            if (cost != null) {
                lore.add(messages.get(player, "upgrade.next-cost",
                        "{amount}", String.valueOf(cost.getAmount()),
                        "{currency}", messages.get(player,
                                "shop.currency." + cost.getCurrency().name()
                                        .toLowerCase())));
                boolean canAfford = ResourceWallet.count(player,
                        cost.getCurrency()) >= cost.getAmount();
                lore.add(messages.get(player,
                        canAfford ? "shop.lore.affordable"
                                : "shop.lore.not-affordable"));
            }
        } else {
            lore.add(messages.get(player, "upgrade.max-level",
                    "{upgrade}", messages.get(player, def.getNameKey())));
        }

        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    public void handleClick(Player player, ShopMenuHolder holder, int slot,
                            ShopClickType click, Game game,
                            PlayerSession session, GameTeam team,
                            ModeProfile profile) {
        if (player == null || holder == null || game == null || session == null
                || team == null || profile == null
                || game.getState() != it.legacynetwork.chickenwars.model.ArenaState.IN_GAME
                || !session.getState().isActive()) {
            return;
        }
        Inventory inv = player.getOpenInventory().getTopInventory();

        if (click.isShift() || click.isRight()) {
            return;
        }

        ShopMenuView view = holder.getView();
        switch (view) {
            case CHICKEN_ROOT:
                handleRootClick(player, game, session, team, profile, slot, inv);
                break;
            case TEAM_UPGRADES:
                handleTeamUpgradeClick(player, game, session, team, profile,
                        slot, inv);
                break;
            case TRAPS:
                handleTrapClick(player, game, session, team, profile, slot, inv);
                break;
            case ROYAL_UPGRADES:
                handleRoyalUpgradeClick(player, game, session, team, profile,
                        slot, inv);
                break;
            default:
                break;
        }
    }

    private void handleRootClick(Player player, Game game, PlayerSession session,
                                 GameTeam team, ModeProfile profile, int slot,
                                 Inventory inv) {
        if (slot == ROOT_SLOT_TEAM) {
            openTeamUpgrades(player, game, session, team, profile);
            playSound(player, Sound.CLICK, 1.6F);
        } else if (slot == ROOT_SLOT_TRAPS) {
            openTraps(player, game, session, team, profile);
            playSound(player, Sound.CLICK, 1.6F);
        } else if (slot == ROOT_SLOT_ROYAL) {
            openRoyalUpgrades(player, game, session, team, profile);
            playSound(player, Sound.CLICK, 1.6F);
        } else if (slot == BACK_SLOT) {
            player.closeInventory();
        } else if (slot == SHOP_SLOT) {
            services.getShop().open(player, game.getDefinition().getId(), null,
                    session, team.getColor(), profile);
        }
    }

    private void handleTeamUpgradeClick(Player player, Game game,
                                        PlayerSession session, GameTeam team,
                                        ModeProfile profile, int slot,
                                        Inventory inv) {
        if (slot == BACK_SLOT) {
            openChickenRoot(player, game, session, team, profile);
            playSound(player, Sound.CLICK, 1.6F);
            return;
        }
        if (slot == SHOP_SLOT) {
            services.getShop().open(player, game.getDefinition().getId(), null,
                    session, team.getColor(), profile);
            return;
        }

        UpgradeCatalog catalog = services.getUpgrades().getCatalog();
        int index = 0;
        for (TeamUpgradeType type : TeamUpgradeType.values()) {
            if (catalog.getTeamUpgrade(type) == null) {
                continue;
            }
            if (FIRST_ITEM_SLOT + index == slot) {
                purchaseTeamUpgrade(player, game, team, type, profile, inv);
                return;
            }
            index++;
        }
    }

    private void purchaseTeamUpgrade(Player player, Game game, GameTeam team,
                                     TeamUpgradeType type, ModeProfile profile,
                                     Inventory inv) {
        String arenaId = game.getDefinition().getId();
        UpgradeResult result = services.getUpgrades().purchase(player,
                arenaId, team.getId(), type, profile);

        if (result == UpgradeResult.SUCCESS) {
            game.applyTeamUpgrade(team, type);

            TeamUpgradeState state = services.getUpgrades().peekState(
                    arenaId, team.getId());
            int level = state == null ? 0 : state.getLevel(type);

            messages.send(player, "upgrade.purchased",
                    "{upgrade}", messages.get(player, type.getNameKey()),
                    "{level}", String.valueOf(level));
            playSound(player, Sound.NOTE_PLING, 2.0F);
        } else {
            messages.send(player, upgradeResultKey(result));
            playSound(player, Sound.VILLAGER_NO, 1.0F);
        }
        renderTeamUpgrades(inv, player, game,
                game.getSession(player.getUniqueId()), team, profile);
    }

    private void handleTrapClick(Player player, Game game, PlayerSession session,
                                 GameTeam team, ModeProfile profile, int slot,
                                 Inventory inv) {
        if (slot == BACK_SLOT) {
            openChickenRoot(player, game, session, team, profile);
            playSound(player, Sound.CLICK, 1.6F);
            return;
        }
        if (slot == SHOP_SLOT) {
            services.getShop().open(player, game.getDefinition().getId(), null,
                    session, team.getColor(), profile);
            return;
        }

        UpgradeCatalog catalog = services.getUpgrades().getCatalog();
        int index = 0;
        for (TrapDefinition def : catalog.getTraps().values()) {
            if (FIRST_ITEM_SLOT + index == slot) {
                purchaseTrap(player, game, team, def.getId(), profile, inv);
                return;
            }
            index++;
        }
    }

    private void purchaseTrap(Player player, Game game, GameTeam team,
                              String trapId, ModeProfile profile,
                              Inventory inv) {
        String arenaId = game.getDefinition().getId();
        UpgradeResult result = services.getUpgrades().purchaseTrap(
                ResourceWallet.adapterFor(player), arenaId, team.getId(),
                trapId, profile);

        if (result == UpgradeResult.SUCCESS) {
            TeamUpgradeState state = services.getUpgrades().peekState(
                    arenaId, team.getId());
            int position = state == null ? 0 : state.getTrapCount();
            messages.send(player, "trap.queued",
                    "{trap}", messages.get(player, "trap." + trapId + ".name"),
                    "{position}", String.valueOf(position),
                    "{max}", String.valueOf(
                            services.getUpgrades().getCatalog().getMaximumTraps()));
            playSound(player, Sound.NOTE_PLING, 2.0F);
        } else {
            messages.send(player, upgradeResultKey(result));
            playSound(player, Sound.VILLAGER_NO, 1.0F);
        }
        renderTraps(inv, player, game,
                game.getSession(player.getUniqueId()), team, profile);
    }

    private void handleRoyalUpgradeClick(Player player, Game game,
                                         PlayerSession session, GameTeam team,
                                         ModeProfile profile, int slot,
                                         Inventory inv) {
        if (slot == BACK_SLOT) {
            openChickenRoot(player, game, session, team, profile);
            playSound(player, Sound.CLICK, 1.6F);
            return;
        }
        if (slot == SHOP_SLOT) {
            services.getShop().open(player, game.getDefinition().getId(), null,
                    session, team.getColor(), profile);
            return;
        }

        UpgradeCatalog catalog = services.getUpgrades().getCatalog();
        int index = 0;
        for (RoyalUpgradeType type : RoyalUpgradeType.values()) {
            if (catalog.getRoyalUpgrade(type) == null) {
                continue;
            }
            if (FIRST_ITEM_SLOT + index == slot) {
                purchaseRoyalUpgrade(player, game, team, type, profile, inv);
                return;
            }
            index++;
        }
    }

    private void purchaseRoyalUpgrade(Player player, Game game, GameTeam team,
                                      RoyalUpgradeType type, ModeProfile profile,
                                      Inventory inv) {
        String arenaId = game.getDefinition().getId();
        UpgradeResult result = services.getUpgrades().purchaseRoyal(
                ResourceWallet.adapterFor(player), arenaId, team.getId(),
                type, profile);

        if (result == UpgradeResult.SUCCESS) {
            if (type == RoyalUpgradeType.ROYAL_VITALITY
                    && team.getChicken() != null) {
                services.getRoyalApplier().applyVitality(
                        arenaId, team.getId(), team.getChicken());
            }
            TeamUpgradeState state = services.getUpgrades().peekState(
                    arenaId, team.getId());
            int level = state == null ? 0 : state.getRoyalLevel(type);
            messages.send(player, "upgrade.purchased",
                    "{upgrade}", messages.get(player, type.getNameKey()),
                    "{level}", String.valueOf(level));
            playSound(player, Sound.NOTE_PLING, 2.0F);
        } else {
            messages.send(player, upgradeResultKey(result));
            playSound(player, Sound.VILLAGER_NO, 1.0F);
        }
        renderRoyalUpgrades(inv, player, game,
                game.getSession(player.getUniqueId()), team, profile);
    }

    private ItemStack createButton(Player player, Material material,
                                   String nameKey, String loreKey,
                                   String... replacements) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(messages.get(player, nameKey, replacements));
            meta.setLore(messages.getList(player, loreKey, replacements));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createInfoItem(Player player, Material material,
                                     String key, String... replacements) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(messages.get(player, key, replacements));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSimpleButton(Material material, String name) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + name);
            button.setItemMeta(meta);
        }
        return button;
    }

    private void playSound(Player player, Sound sound, float pitch) {
        player.playSound(player.getLocation(), sound, 1.0F, pitch);
    }

    private String upgradeResultKey(UpgradeResult result) {
        switch (result) {
            case MAX_LEVEL: return "upgrade.max-level";
            case NOT_ENOUGH: return "upgrade.not-enough";
            case UNAVAILABLE: return "upgrade.unavailable";
            case NO_PRICE: return "upgrade.no-price";
            case NO_TEAM: return "upgrade.no-team";
            case NOT_IN_GAME: return "upgrade.not-in-game";
            case TRAP_QUEUE_FULL: return "trap.queue-full";
            default: return "upgrade.unavailable";
        }
    }
}
