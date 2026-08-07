package it.legacynetwork.chickenwars.shop;

import it.legacynetwork.chickenwars.message.MessageService;
import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.model.TeamColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import java.util.Collections;
import java.util.List;

/**
 * Apertura del menu shop e gestione degli acquisti.
 *
 * <p>Il menu e' composto da una riga di categorie in alto, un separatore e la
 * griglia degli articoli. Gli acquisti verificano sempre le risorse realmente
 * possedute prima di consegnare l'oggetto.</p>
 */
public final class ShopService {

    private static final int MENU_SIZE = 54;
    private static final int SEPARATOR_ROW = 9;
    private static final int FIRST_ITEM_SLOT = 18;

    private final MessageService messages;

    private volatile ShopCatalog catalog = ShopCatalog.empty();

    public ShopService(MessageService messages) {
        this.messages = messages;
    }

    public void setCatalog(ShopCatalog catalog) {
        this.catalog = catalog == null ? ShopCatalog.empty() : catalog;
    }

    public ShopCatalog getCatalog() {
        return catalog;
    }

    public boolean isAvailable() {
        return !catalog.isEmpty();
    }

    /**
     * Apre lo shop sulla categoria indicata.
     *
     * @param player     giocatore destinatario
     * @param arenaId    arena di provenienza, usata per validare i click
     * @param categoryId categoria da mostrare, eventualmente nulla
     * @param color      colore squadra, per gli articoli colorati
     */
    public void open(Player player, String arenaId, String categoryId,
                     TeamColor color) {
        if (player == null || catalog.isEmpty()) {
            return;
        }
        ShopCategory category = catalog.getCategoryOrFirst(categoryId);
        if (category == null) {
            return;
        }

        ShopMenuHolder holder = new ShopMenuHolder(arenaId, category.getId());
        String language = messages.getLanguage(player);
        Inventory inventory = Bukkit.createInventory(holder, MENU_SIZE,
                messages.get(player, "shop.title"));

        render(inventory, category, language, color, player);
        player.openInventory(inventory);
    }

    private void render(Inventory inventory, ShopCategory category,
                        String language, TeamColor color, Player player) {
        inventory.clear();

        for (ShopCategory available : catalog.getCategories()) {
            int slot = available.getSlot();
            if (slot < 0 || slot > 8) {
                continue;
            }
            inventory.setItem(slot, available.createIcon(language,
                    available.getId().equals(category.getId())));
        }

        ItemStack separator = createSeparator();
        for (int slot = SEPARATOR_ROW; slot < SEPARATOR_ROW + 9; slot++) {
            inventory.setItem(slot, separator);
        }

        int cursor = FIRST_ITEM_SLOT;
        for (ShopItem item : category.getItems()) {
            int slot = item.getSlot() >= 0 ? item.getSlot() : cursor++;
            if (slot < FIRST_ITEM_SLOT || slot >= MENU_SIZE) {
                continue;
            }
            boolean affordable = countCurrency(player, item.getCurrency())
                    >= item.getPrice();
            inventory.setItem(slot, item.createIcon(language, color, affordable));
        }
    }

    @SuppressWarnings("deprecation")
    private ItemStack createSeparator() {
        ItemStack separator = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta meta = separator.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.setLore(Collections.<String>emptyList());
            separator.setItemMeta(meta);
        }
        return separator;
    }

    /**
     * Gestisce un click nel menu shop.
     *
     * @param player   giocatore che ha cliccato
     * @param holder   holder dell'inventario aperto
     * @param slot     slot cliccato
     * @param color    colore squadra del giocatore
     * @return {@code true} se il click e' stato interpretato dallo shop
     */
    public boolean handleClick(Player player, ShopMenuHolder holder, int slot,
                               TeamColor color) {
        if (player == null || holder == null || slot < 0 || slot >= MENU_SIZE) {
            return false;
        }

        if (slot < 9) {
            for (ShopCategory category : catalog.getCategories()) {
                if (category.getSlot() == slot) {
                    holder.setCategoryId(category.getId());
                    render(player.getOpenInventory().getTopInventory(), category,
                            messages.getLanguage(player), color, player);
                    player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.6F);
                    return true;
                }
            }
            return true;
        }

        if (slot < FIRST_ITEM_SLOT) {
            return true;
        }

        ShopCategory category = catalog.getCategoryOrFirst(holder.getCategoryId());
        if (category == null) {
            return true;
        }

        ShopItem target = findItemAtSlot(category, slot);
        if (target == null) {
            return true;
        }
        purchase(player, target, color);
        render(player.getOpenInventory().getTopInventory(), category,
                messages.getLanguage(player), color, player);
        return true;
    }

    private ShopItem findItemAtSlot(ShopCategory category, int slot) {
        int cursor = FIRST_ITEM_SLOT;
        for (ShopItem item : category.getItems()) {
            int itemSlot = item.getSlot() >= 0 ? item.getSlot() : cursor++;
            if (itemSlot == slot) {
                return item;
            }
        }
        return null;
    }

    /**
     * Esegue l'acquisto, verificando risorse e spazio nell'inventario.
     *
     * @return {@code true} se l'articolo e' stato consegnato
     */
    public boolean purchase(Player player, ShopItem item, TeamColor color) {
        String language = messages.getLanguage(player);
        int owned = countCurrency(player, item.getCurrency());
        if (owned < item.getPrice()) {
            messages.send(player, "shop.not-enough",
                    "{amount}", String.valueOf(item.getPrice() - owned),
                    "{currency}", item.getCurrency().getDisplayName(language));
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0F, 1.0F);
            return false;
        }

        if (player.getInventory().firstEmpty() == -1) {
            messages.send(player, "shop.inventory-full");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0F, 1.0F);
            return false;
        }

        removeCurrency(player, item.getCurrency(), item.getPrice());
        player.getInventory().addItem(item.createStack(color));
        for (PotionEffect effect : item.getEffects()) {
            player.addPotionEffect(effect, true);
        }
        player.updateInventory();

        messages.send(player, "shop.purchased",
                "{item}", ChatColor.stripColor(ChatColor.translateAlternateColorCodes(
                        '&', item.getDisplayName(language))));
        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 2.0F);
        return true;
    }

    /**
     * Conta quante unita' della valuta il giocatore possiede.
     */
    public int countCurrency(Player player, ResourceType currency) {
        if (player == null || currency == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == currency.getMaterial()) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private void removeCurrency(Player player, ResourceType currency, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != currency.getMaterial()) {
                continue;
            }
            int taken = Math.min(stack.getAmount(), remaining);
            remaining -= taken;
            if (stack.getAmount() - taken <= 0) {
                player.getInventory().setItem(i, null);
            } else {
                stack.setAmount(stack.getAmount() - taken);
                player.getInventory().setItem(i, stack);
            }
        }
        player.updateInventory();
    }

    /**
     * Elenca le categorie disponibili, utile per i comandi di diagnostica.
     */
    public List<ShopCategory> getCategories() {
        return catalog.getCategories();
    }
}
