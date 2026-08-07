package it.legacynetwork.chickenwars.shop;

import it.legacynetwork.chickenwars.economy.ResourceWallet;
import it.legacynetwork.chickenwars.message.MessageService;
import it.legacynetwork.chickenwars.mode.ModeProfile;
import it.legacynetwork.chickenwars.model.TeamColor;
import it.legacynetwork.chickenwars.persistence.QuickBuyPresetRecord;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.player.equipment.EquipmentService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Menu dello shop organizzato per categorie.
 *
 * <p>La riga superiore contiene le categorie, la seconda un separatore, il
 * resto gli articoli. Ogni acquisto viene delegato a
 * {@link EquipmentService}: qui restano soltanto rendering, feedback e
 * protezione dell'inventario.</p>
 */
public final class ShopService {

    private static final int MENU_SIZE = 54;
    private static final int SEPARATOR_ROW = 9;
    private static final int FIRST_ITEM_SLOT = 18;

    private final MessageService messages;
    private final EquipmentService equipment;
    private final QuickBuyService quickBuy;
    private final QuickBuyMenuController controller =
            new QuickBuyMenuController();

    private volatile ShopConfiguration configuration = ShopConfiguration.empty();

    public ShopService(MessageService messages, EquipmentService equipment,
                       QuickBuyService quickBuy) {
        this.messages = messages;
        this.equipment = equipment;
        this.quickBuy = quickBuy;
    }

    public void setConfiguration(ShopConfiguration configuration) {
        this.configuration = configuration == null
                ? ShopConfiguration.empty() : configuration;
    }

    public ShopConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Servizio dei preset, condiviso con comandi e menu.
     */
    public QuickBuyService getQuickBuy() {
        return quickBuy;
    }

    public boolean isAvailable() {
        return !configuration.isEmpty();
    }

    // ------------------------------------------------------------------
    // Apertura e rendering
    // ------------------------------------------------------------------

    /**
     * Apre lo shop sulla categoria indicata.
     *
     * @param player     destinatario
     * @param arenaId    arena di provenienza, usata per validare i click
     * @param categoryId categoria da mostrare, eventualmente nulla
     * @param session    sessione con lo stato autorevole
     * @param color      colore squadra
     * @param profile    profilo modalita', per risolvere i prezzi
     */
    public void open(Player player, String arenaId, String categoryId,
                     PlayerSession session, TeamColor color,
                     ModeProfile profile) {
        if (player == null || session == null || !isAvailable()) {
            return;
        }
        ShopCategoryDefinition category =
                configuration.getCategoryOrFirst(categoryId);
        if (category == null) {
            return;
        }

        ShopMenuHolder holder = new ShopMenuHolder(arenaId, category.getId());
        openView(player, holder, session, color, profile);
    }

    /**
     * Apre o riapre l'inventario nella vista corrente dell'holder.
     *
     * <p>Su 1.8 il titolo di un inventario aperto non e' modificabile: cambiare
     * vista significa creare una nuova finestra. L'holder viene riusato, quindi
     * arena, categoria e articolo in attesa sopravvivono al passaggio.</p>
     */
    public void openView(Player player, ShopMenuHolder holder,
                         PlayerSession session, TeamColor color,
                         ModeProfile profile) {
        String titleKey = holder.getView() == ShopMenuView.PRESETS
                ? "shop.presets.title" : "shop.title";
        Inventory inventory = Bukkit.createInventory(holder, MENU_SIZE,
                messages.get(player, titleKey));
        render(inventory, holder, player, session, color, profile);
        player.openInventory(inventory);
    }

    /**
     * Ridisegna il contenuto del menu gia' aperto.
     */
    public void render(Inventory inventory, ShopMenuHolder holder,
                       Player player, PlayerSession session, TeamColor color,
                       ModeProfile profile) {
        inventory.clear();

        if (holder.getView() == ShopMenuView.PRESETS) {
            renderPresets(inventory, player);
            return;
        }

        ShopCategoryDefinition category =
                configuration.getCategoryOrFirst(holder.getCategoryId());
        if (category == null) {
            return;
        }

        for (ShopCategoryDefinition available : configuration.getCategories()) {
            inventory.setItem(available.getSlot(), createCategoryIcon(
                    player, available, available.getId().equals(category.getId())));
        }

        ItemStack separator = createSeparator();
        for (int slot = SEPARATOR_ROW; slot < SEPARATOR_ROW + 9; slot++) {
            inventory.setItem(slot, separator);
        }

        if (category.isQuickBuy()) {
            renderQuickBuy(inventory, holder, player, session, color, profile);
            return;
        }

        int cursor = FIRST_ITEM_SLOT;
        for (ShopItemDefinition item : category.getItems()) {
            int slot = item.getSlot() >= 0 ? item.getSlot() : cursor++;
            if (slot < FIRST_ITEM_SLOT || slot >= MENU_SIZE) {
                continue;
            }
            inventory.setItem(slot,
                    createItemIcon(player, session, color, item, profile));
        }
    }

    private void renderQuickBuy(Inventory inventory, ShopMenuHolder holder,
                                Player player, PlayerSession session,
                                TeamColor color, ModeProfile profile) {
        Map<Integer, ShopItemDefinition> slots =
                quickBuy.resolveSlots(player.getUniqueId(), configuration);

        for (Map.Entry<Integer, ShopItemDefinition> entry : slots.entrySet()) {
            inventory.setItem(entry.getKey().intValue(), createItemIcon(
                    player, session, color, entry.getValue(), profile));
        }

        boolean placing = holder.hasPendingItem();
        ItemStack empty = placing
                ? createTargetSlotIcon(player) : createEmptyQuickBuySlot(player);
        for (int slot = FIRST_ITEM_SLOT; slot < MENU_SIZE; slot++) {
            if (QuickBuyService.isQuickBuySlot(slot)
                    && inventory.getItem(slot) == null) {
                inventory.setItem(slot, empty);
            }
        }

        if (placing) {
            // Durante la scelta della destinazione ogni slot e' un bersaglio:
            // l'indicazione resta visibile finche' l'articolo non e' collocato.
            inventory.setItem(QuickBuyMenuController.PRESETS_BUTTON_SLOT,
                    createPendingBanner(player, holder.getPendingItemId()));
        } else {
            inventory.setItem(QuickBuyMenuController.PRESETS_BUTTON_SLOT,
                    createButton(player, Material.BOOK,
                            "shop.presets.open", "shop.presets.open-lore"));
        }
        inventory.setItem(QuickBuyMenuController.BACK_TO_SHOP_SLOT,
                createButton(player, Material.ARROW,
                        "shop.nav.back-shop", "shop.nav.back-shop-lore"));
    }

    /**
     * Disegna la gestione dei preset.
     */
    private void renderPresets(Inventory inventory, Player player) {
        List<QuickBuyPresetRecord> presets =
                quickBuy.list(player.getUniqueId());
        String selected =
                quickBuy.getSelected(player.getUniqueId()).getPresetId();

        for (int index = 0; index < presets.size(); index++) {
            int slot = controller.presetSlot(index);
            if (slot < 0) {
                break;
            }
            QuickBuyPresetRecord preset = presets.get(index);
            inventory.setItem(slot, createPresetIcon(player, preset,
                    preset.getPresetId().equals(selected), presets.size()));
        }

        boolean atLimit = quickBuy.isAtLimit(player);
        inventory.setItem(QuickBuyMenuController.CREATE_PRESET_SLOT,
                createButton(player,
                        atLimit ? Material.BARRIER : Material.NETHER_STAR,
                        atLimit ? "shop.presets.limit-reached"
                                : "shop.presets.create",
                        atLimit ? "shop.presets.limit-reached-lore"
                                : "shop.presets.create-lore",
                        "{limit}", String.valueOf(
                                quickBuy.getPresetLimit(player))));
        inventory.setItem(QuickBuyMenuController.BACK_TO_QUICK_BUY_SLOT,
                createButton(player, Material.ARROW,
                        "shop.nav.back-quickbuy", "shop.nav.back-quickbuy-lore"));
        inventory.setItem(QuickBuyMenuController.BACK_TO_SHOP_SLOT,
                createButton(player, Material.ARROW,
                        "shop.nav.back-shop", "shop.nav.back-shop-lore"));
    }

    @SuppressWarnings("deprecation")
    private ItemStack createPresetIcon(Player player,
                                       QuickBuyPresetRecord preset,
                                       boolean selected, int total) {
        ItemStack icon = new ItemStack(
                selected ? Material.ENCHANTED_BOOK : Material.BOOK);
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) {
            return icon;
        }
        meta.setDisplayName(messages.get(player,
                selected ? "shop.presets.entry-active"
                        : "shop.presets.entry-inactive",
                "{preset}", preset.getDisplayName()));

        List<String> lore = new ArrayList<String>();
        lore.add(messages.get(player, "shop.presets.entry-slots",
                "{amount}", String.valueOf(preset.getSlots().size())));
        lore.add("");
        if (!selected) {
            lore.addAll(messages.getList(player, "shop.presets.entry-select"));
        }
        if (total > 1) {
            lore.addAll(messages.getList(player, "shop.presets.entry-delete"));
        } else {
            lore.addAll(messages.getList(player, "shop.presets.entry-last"));
        }
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
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

    private ItemStack createPendingBanner(Player player, String itemId) {
        ShopItemDefinition item = configuration.getItem(itemId);
        String name = item == null ? String.valueOf(itemId)
                : plain(messages.get(player, item.getNameKey()));
        return createButton(player, Material.HOPPER,
                "shop.quickbuy.placing", "shop.quickbuy.placing-lore",
                "{item}", name);
    }

    @SuppressWarnings("deprecation")
    private ItemStack createCategoryIcon(Player player,
                                         ShopCategoryDefinition category,
                                         boolean selected) {
        ItemStack icon = new ItemStack(category.getIcon(), 1,
                (short) category.getIconData());
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            String name = messages.get(player, category.getNameKey());
            meta.setDisplayName((selected ? ChatColor.GREEN.toString()
                    + ChatColor.BOLD : ChatColor.YELLOW.toString()) + name);
            if (selected) {
                meta.setLore(messages.getList(player, "shop.category-selected"));
            }
            icon.setItemMeta(meta);
        }
        return icon;
    }

    /**
     * Compone l'icona di un articolo con nome, descrizione, costo e stato.
     */
    @SuppressWarnings("deprecation")
    public ItemStack createItemIcon(Player player, PlayerSession session,
                                    TeamColor color, ShopItemDefinition item,
                                    ModeProfile profile) {
        byte data = item.isTeamColored() && color != null
                ? color.getWoolData() : item.getData();
        ItemStack icon = new ItemStack(item.getMaterial(), item.getAmount(),
                (short) data);

        if (item.isTeamColored() && color != null) {
            ItemMeta colored = icon.getItemMeta();
            if (colored instanceof LeatherArmorMeta) {
                ((LeatherArmorMeta) colored).setColor(color.getArmorColor());
                icon.setItemMeta(colored);
            }
        }

        ItemCost cost = configuration.resolveCost(item.getId(), profile);
        PurchaseResult state = evaluate(player, session, item, cost);

        ItemMeta meta = icon.getItemMeta();
        if (meta == null) {
            return icon;
        }
        meta.setDisplayName(messages.get(player, item.getNameKey()));

        List<String> lore = new ArrayList<String>(
                messages.getList(player, item.getLoreKey()));
        lore.add("");
        if (cost == null) {
            lore.add(messages.get(player, "shop.lore.unavailable"));
        } else {
            lore.add(messages.get(player, "shop.lore.cost",
                    "{amount}", String.valueOf(cost.getAmount()),
                    "{currency}", messages.get(player,
                            "shop.currency." + cost.getCurrency().name()
                                    .toLowerCase())));
            lore.add(messages.get(player, loreStateKey(state)));
        }
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private String loreStateKey(PurchaseResult state) {
        switch (state) {
            case SUCCESS:
                return "shop.lore.affordable";
            case NOT_ENOUGH:
                return "shop.lore.not-affordable";
            case ALREADY_OWNED:
                return "shop.lore.owned";
            case LOWER_TIER:
                return "shop.lore.lower-tier";
            case NO_PERMISSION:
                return "shop.lore.no-permission";
            case INVENTORY_FULL:
                return "shop.lore.inventory-full";
            case UNAVAILABLE:
            default:
                return "shop.lore.unavailable";
        }
    }

    /**
     * Calcola lo stato d'acquisto senza modificare nulla.
     */
    private PurchaseResult evaluate(Player player, PlayerSession session,
                                    ShopItemDefinition item, ItemCost cost) {
        if (cost == null) {
            return PurchaseResult.UNAVAILABLE;
        }
        if (item.getPermission() != null
                && !player.hasPermission(item.getPermission())) {
            return PurchaseResult.NO_PERMISSION;
        }
        PurchaseResult tier = equipment.checkTier(session, item);
        if (!tier.isSuccess()) {
            return tier;
        }
        return ResourceWallet.count(player, cost.getCurrency())
                >= cost.getAmount()
                ? PurchaseResult.SUCCESS : PurchaseResult.NOT_ENOUGH;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createSeparator() {
        ItemStack separator =
                new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta meta = separator.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.setLore(Collections.<String>emptyList());
            separator.setItemMeta(meta);
        }
        return separator;
    }

    /**
     * Segnala uno slot come destinazione durante la scelta della posizione.
     */
    @SuppressWarnings("deprecation")
    private ItemStack createTargetSlotIcon(Player player) {
        ItemStack target =
                new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);
        ItemMeta meta = target.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(messages.get(player, "shop.quickbuy.target"));
            meta.setLore(messages.getList(player, "shop.quickbuy.target-lore"));
            target.setItemMeta(meta);
        }
        return target;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createEmptyQuickBuySlot(Player player) {
        ItemStack empty =
                new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
        ItemMeta meta = empty.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(messages.get(player, "shop.quickbuy.empty"));
            meta.setLore(messages.getList(player, "shop.quickbuy.empty-lore"));
            empty.setItemMeta(meta);
        }
        return empty;
    }

    // ------------------------------------------------------------------
    // Click
    // ------------------------------------------------------------------

    /**
     * Gestisce un click nel menu shop.
     *
     * @param shiftClick indica se il click assegna l'articolo al Quick Buy
     * @return {@code true} se il click e' stato interpretato dallo shop
     */
    public boolean handleClick(Player player, ShopMenuHolder holder, int slot,
                               ShopClickType click, PlayerSession session,
                               TeamColor color, ModeProfile profile) {
        if (player == null || holder == null || session == null
                || click == null || slot < 0 || slot >= MENU_SIZE) {
            return false;
        }
        Inventory inventory = player.getOpenInventory().getTopInventory();

        if (holder.getView() == ShopMenuView.PRESETS) {
            execute(controller.resolvePresetClick(slot, click,
                            presetIds(player)),
                    player, holder, session, color, profile, inventory);
            return true;
        }

        if (slot < 9) {
            switchCategory(player, holder, slot, session, color, profile,
                    inventory);
            return true;
        }
        if (slot < FIRST_ITEM_SLOT) {
            return true;
        }

        ShopCategoryDefinition category =
                configuration.getCategoryOrFirst(holder.getCategoryId());
        if (category == null) {
            return true;
        }

        QuickBuyAction action;
        if (category.isQuickBuy()) {
            ShopItemDefinition assigned = quickBuy
                    .resolveSlots(player.getUniqueId(), configuration)
                    .get(Integer.valueOf(slot));
            action = controller.resolveQuickBuyClick(slot, click,
                    assigned == null ? null : assigned.getId(),
                    holder.getPendingItemId());
        } else {
            ShopItemDefinition assigned = findItemAtSlot(category, slot);
            action = controller.resolveShopClick(slot, click,
                    assigned == null ? null : assigned.getId());
        }
        execute(action, player, holder, session, color, profile, inventory);
        return true;
    }

    /**
     * Applica l'azione decisa dal controller.
     *
     * <p>Ogni mutazione passa da {@link QuickBuyService}: il menu non conserva
     * una propria copia dei preset.</p>
     */
    private void execute(QuickBuyAction action, Player player,
                         ShopMenuHolder holder, PlayerSession session,
                         TeamColor color, ModeProfile profile,
                         Inventory inventory) {
        switch (action.getType()) {
            case PURCHASE:
                purchaseById(player, session, color, action.getItemId(),
                        profile);
                break;
            case PICK_UP_ITEM:
                startPlacement(player, holder, action.getItemId());
                break;
            case PLACE_ITEM:
                placePendingItem(player, holder, action);
                break;
            case CLEAR_SLOT:
                clearQuickBuySlot(player, action.getSlot());
                break;
            case OPEN_PRESETS:
                holder.clearPendingItem();
                holder.setView(ShopMenuView.PRESETS);
                openView(player, holder, session, color, profile);
                play(player, Sound.CLICK, 1.6F);
                return;
            case OPEN_QUICK_BUY:
                holder.setView(ShopMenuView.SHOP);
                holder.setCategoryId(ShopCategoryDefinition.QUICK_BUY_ID);
                openView(player, holder, session, color, profile);
                play(player, Sound.CLICK, 1.6F);
                return;
            case OPEN_SHOP:
                holder.clearPendingItem();
                holder.setView(ShopMenuView.SHOP);
                holder.setCategoryId(firstBuyableCategoryId());
                openView(player, holder, session, color, profile);
                play(player, Sound.CLICK, 1.6F);
                return;
            case CREATE_PRESET:
                createPreset(player);
                break;
            case SELECT_PRESET:
                selectPreset(player, action.getPresetId());
                break;
            case DELETE_PRESET:
                deletePreset(player, action.getPresetId());
                break;
            case NONE:
            default:
                return;
        }
        render(inventory, holder, player, session, color, profile);
    }

    private void switchCategory(Player player, ShopMenuHolder holder, int slot,
                                PlayerSession session, TeamColor color,
                                ModeProfile profile, Inventory inventory) {
        for (ShopCategoryDefinition category : configuration.getCategories()) {
            if (category.getSlot() != slot) {
                continue;
            }
            holder.setCategoryId(category.getId());
            render(inventory, holder, player, session, color, profile);
            play(player, Sound.CLICK, 1.6F);
            return;
        }
    }

    // ------------------------------------------------------------------
    // Azioni sugli slot Quick Buy
    // ------------------------------------------------------------------

    /**
     * Prende in carico un articolo e porta il giocatore a scegliere lo slot.
     */
    private void startPlacement(Player player, ShopMenuHolder holder,
                                String itemId) {
        ShopItemDefinition item = configuration.getItem(itemId);
        if (item == null) {
            messages.send(player, "shop.quickbuy.item-missing");
            play(player, Sound.VILLAGER_NO, 1.0F);
            return;
        }
        holder.setPendingItemId(itemId);
        holder.setCategoryId(ShopCategoryDefinition.QUICK_BUY_ID);
        messages.send(player, "shop.quickbuy.choose-slot",
                "{item}", plain(messages.get(player, item.getNameKey())));
        play(player, Sound.CLICK, 1.4F);
    }

    /**
     * Colloca l'articolo in attesa, sostituendo quello eventualmente presente.
     */
    private void placePendingItem(Player player, ShopMenuHolder holder,
                                  QuickBuyAction action) {
        ShopItemDefinition item = configuration.getItem(action.getItemId());
        if (item == null) {
            holder.clearPendingItem();
            messages.send(player, "shop.quickbuy.item-missing");
            play(player, Sound.VILLAGER_NO, 1.0F);
            return;
        }

        ShopItemDefinition previous = quickBuy
                .resolveSlots(player.getUniqueId(), configuration)
                .get(Integer.valueOf(action.getSlot()));
        boolean changed = quickBuy.assign(player.getUniqueId(),
                action.getSlot(), item.getId());
        holder.clearPendingItem();

        String name = plain(messages.get(player, item.getNameKey()));
        if (!changed) {
            // Stesso articolo nello stesso slot: nessun record duplicato.
            messages.send(player, "shop.quickbuy.unchanged", "{item}", name);
            play(player, Sound.CLICK, 1.0F);
            return;
        }
        messages.send(player,
                previous == null ? "shop.quickbuy.assigned"
                        : "shop.quickbuy.replaced",
                "{item}", name,
                "{previous}", previous == null ? "" : plain(
                        messages.get(player, previous.getNameKey())));
        play(player, Sound.NOTE_PLING, 1.8F);
    }

    /**
     * Svuota uno slot Quick Buy tramite il servizio.
     */
    private void clearQuickBuySlot(Player player, int slot) {
        if (quickBuy.assign(player.getUniqueId(), slot, null)) {
            messages.send(player, "shop.quickbuy.removed");
            play(player, Sound.NOTE_PLING, 0.8F);
            return;
        }
        // Slot gia' vuoto: operazione idempotente, nessun errore.
        messages.send(player, "shop.quickbuy.empty-slot");
        play(player, Sound.CLICK, 1.0F);
    }

    // ------------------------------------------------------------------
    // Azioni sui preset
    // ------------------------------------------------------------------

    private void createPreset(Player player) {
        if (quickBuy.isAtLimit(player)) {
            messages.send(player, "shop.quickbuy.preset-limit",
                    "{limit}", String.valueOf(quickBuy.getPresetLimit(player)));
            play(player, Sound.VILLAGER_NO, 1.0F);
            return;
        }
        String presetId = quickBuy.nextPresetId(player.getUniqueId());
        if (!quickBuy.create(player, presetId)) {
            messages.send(player, "shop.quickbuy.preset-limit",
                    "{limit}", String.valueOf(quickBuy.getPresetLimit(player)));
            play(player, Sound.VILLAGER_NO, 1.0F);
            return;
        }
        quickBuy.select(player.getUniqueId(), presetId);
        messages.send(player, "shop.quickbuy.preset-created",
                "{preset}", presetId);
        play(player, Sound.LEVEL_UP, 1.8F);
    }

    private void selectPreset(Player player, String presetId) {
        if (!quickBuy.select(player.getUniqueId(), presetId)) {
            messages.send(player, "shop.quickbuy.preset-missing",
                    "{preset}", String.valueOf(presetId));
            play(player, Sound.VILLAGER_NO, 1.0F);
            return;
        }
        messages.send(player, "shop.quickbuy.preset-selected",
                "{preset}", presetId);
        play(player, Sound.CLICK, 1.8F);
    }

    private void deletePreset(Player player, String presetId) {
        QuickBuyService.DeleteResult result =
                quickBuy.delete(player.getUniqueId(), presetId);
        switch (result) {
            case DELETED:
                messages.send(player, "shop.quickbuy.preset-deleted",
                        "{preset}", presetId);
                play(player, Sound.NOTE_PLING, 0.8F);
                return;
            case LAST_PRESET:
                messages.send(player, "shop.quickbuy.preset-last");
                play(player, Sound.VILLAGER_NO, 1.0F);
                return;
            case NOT_FOUND:
            default:
                messages.send(player, "shop.quickbuy.preset-missing",
                        "{preset}", String.valueOf(presetId));
                play(player, Sound.VILLAGER_NO, 1.0F);
        }
    }

    // ------------------------------------------------------------------
    // Supporto
    // ------------------------------------------------------------------

    private List<String> presetIds(Player player) {
        List<String> ids = new ArrayList<String>();
        for (QuickBuyPresetRecord preset : quickBuy.list(player.getUniqueId())) {
            ids.add(preset.getPresetId());
        }
        return ids;
    }

    /**
     * Prima categoria diversa dal Quick Buy, usata dal ritorno allo shop.
     */
    private String firstBuyableCategoryId() {
        for (ShopCategoryDefinition category : configuration.getCategories()) {
            if (!category.isQuickBuy()) {
                return category.getId();
            }
        }
        return ShopCategoryDefinition.QUICK_BUY_ID;
    }

    private void purchaseById(Player player, PlayerSession session,
                              TeamColor color, String itemId,
                              ModeProfile profile) {
        ShopItemDefinition item = configuration.getItem(itemId);
        if (item == null) {
            messages.send(player, "shop.quickbuy.item-missing");
            play(player, Sound.VILLAGER_NO, 1.0F);
            return;
        }
        purchase(player, session, color, item, profile);
    }

    private void play(Player player, Sound sound, float pitch) {
        player.playSound(player.getLocation(), sound, 1.0F, pitch);
    }

    /**
     * Esegue un acquisto e comunica l'esito al giocatore.
     *
     * @return l'esito, mai nullo
     */
    public PurchaseResult purchase(Player player, PlayerSession session,
                                   TeamColor color, ShopItemDefinition item,
                                   ModeProfile profile) {
        ItemCost cost = configuration.resolveCost(item.getId(), profile);
        if (cost == null) {
            messages.send(player, PurchaseResult.UNAVAILABLE.getMessageKey());
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0F, 1.0F);
            return PurchaseResult.UNAVAILABLE;
        }

        PurchaseResult result =
                equipment.purchase(player, session, color, item, cost);
        String itemName = plain(messages.get(player, item.getNameKey()));

        if (result.isSuccess()) {
            messages.send(player, result.getMessageKey(), "{item}", itemName);
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 2.0F);
            return result;
        }

        messages.send(player, result.getMessageKey(),
                "{item}", itemName,
                "{amount}", String.valueOf(Math.max(0, cost.getAmount()
                        - ResourceWallet.count(player, cost.getCurrency()))),
                "{currency}", messages.get(player,
                        "shop.currency." + cost.getCurrency().name()
                                .toLowerCase()));
        player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0F, 1.0F);
        return result;
    }

    private ShopItemDefinition findItemAtSlot(ShopCategoryDefinition category,
                                              int slot) {
        int cursor = FIRST_ITEM_SLOT;
        for (ShopItemDefinition item : category.getItems()) {
            int itemSlot = item.getSlot() >= 0 ? item.getSlot() : cursor++;
            if (itemSlot == slot) {
                return item;
            }
        }
        return null;
    }

    private String plain(String coloured) {
        return ChatColor.stripColor(coloured);
    }
}
