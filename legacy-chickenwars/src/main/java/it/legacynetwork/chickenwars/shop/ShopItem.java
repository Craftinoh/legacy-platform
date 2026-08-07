package it.legacynetwork.chickenwars.shop;

import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.model.TeamColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Articolo acquistabile nello shop.
 *
 * <p>L'articolo e' immutabile: ogni acquisto produce una nuova {@link ItemStack}
 * cosi' che modifiche successive non alterino la configurazione.</p>
 */
public final class ShopItem {

    private final String id;
    private final String categoryId;
    private final Material material;
    private final byte data;
    private final int amount;
    private final String italianName;
    private final String englishName;
    private final List<String> italianLore;
    private final List<String> englishLore;
    private final int slot;
    private final ResourceType currency;
    private final int price;
    private final boolean teamColored;
    private final Map<Enchantment, Integer> enchantments;
    private final List<PotionEffect> effects;

    ShopItem(String id, String categoryId, Material material, byte data, int amount,
             String italianName, String englishName, List<String> italianLore,
             List<String> englishLore, int slot, ResourceType currency, int price,
             boolean teamColored, Map<Enchantment, Integer> enchantments,
             List<PotionEffect> effects) {
        this.id = id;
        this.categoryId = categoryId;
        this.material = material;
        this.data = data;
        this.amount = Math.max(1, amount);
        this.italianName = italianName;
        this.englishName = englishName;
        this.italianLore = Collections.unmodifiableList(
                new ArrayList<String>(italianLore));
        this.englishLore = Collections.unmodifiableList(
                new ArrayList<String>(englishLore));
        this.slot = slot;
        this.currency = currency;
        this.price = Math.max(0, price);
        this.teamColored = teamColored;
        this.enchantments = Collections.unmodifiableMap(
                new LinkedHashMap<Enchantment, Integer>(enchantments));
        this.effects = Collections.unmodifiableList(
                new ArrayList<PotionEffect>(effects));
    }

    /**
     * Crea l'oggetto realmente consegnato al giocatore.
     *
     * @param color colore della squadra, usato per cuoio, lana e vetro
     * @return una nuova pila pronta per l'inventario
     */
    @SuppressWarnings("deprecation")
    public ItemStack createStack(TeamColor color) {
        byte finalData = teamColored && color != null ? color.getWoolData() : data;
        ItemStack stack = new ItemStack(material, amount, (short) finalData);

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (teamColored && color != null && meta instanceof LeatherArmorMeta) {
                ((LeatherArmorMeta) meta).setColor(color.getArmorColor());
            }
            stack.setItemMeta(meta);
        }

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            stack.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }
        return stack;
    }

    /**
     * Crea l'icona mostrata nel menu, completa di nome, descrizione e prezzo.
     *
     * @param language   lingua del giocatore
     * @param color      colore della squadra
     * @param affordable indica se il giocatore possiede le risorse necessarie
     * @return l'icona da inserire nell'inventario
     */
    @SuppressWarnings("deprecation")
    public ItemStack createIcon(String language, TeamColor color,
                                boolean affordable) {
        ItemStack icon = createStack(color);
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) {
            return icon;
        }

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                getDisplayName(language)));

        List<String> lore = new ArrayList<String>();
        for (String line : getLore(language)) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        lore.add("");
        lore.add((affordable ? ChatColor.GREEN : ChatColor.RED)
                + "Costo: " + currency.getColor() + price + " "
                + currency.getDisplayName(language));
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    public String getDisplayName(String language) {
        if ("it".equalsIgnoreCase(language)) {
            return italianName;
        }
        return englishName == null || englishName.isEmpty()
                ? italianName : englishName;
    }

    public List<String> getLore(String language) {
        if ("it".equalsIgnoreCase(language)) {
            return italianLore;
        }
        return englishLore.isEmpty() ? italianLore : englishLore;
    }

    public String getId() {
        return id;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public int getSlot() {
        return slot;
    }

    public ResourceType getCurrency() {
        return currency;
    }

    public int getPrice() {
        return price;
    }

    public boolean isTeamColored() {
        return teamColored;
    }

    public List<PotionEffect> getEffects() {
        return effects;
    }

    public Material getMaterial() {
        return material;
    }
}
