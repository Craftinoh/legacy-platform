package it.legacynetwork.chickenwars.shop;

import it.legacynetwork.chickenwars.model.ResourceType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Catalogo dello shop caricato da {@code shop.yml}.
 *
 * <p>Le voci non interpretabili vengono segnalate e ignorate singolarmente, cosi'
 * che un errore di configurazione non renda inutilizzabile l'intero shop.</p>
 */
public final class ShopCatalog {

    private final List<ShopCategory> categories;
    private final Map<String, ShopItem> itemsById;

    private ShopCatalog(List<ShopCategory> categories,
                        Map<String, ShopItem> itemsById) {
        this.categories = Collections.unmodifiableList(categories);
        this.itemsById = Collections.unmodifiableMap(itemsById);
    }

    /**
     * Catalogo vuoto, usato quando il file non e' leggibile.
     */
    public static ShopCatalog empty() {
        return new ShopCatalog(new ArrayList<ShopCategory>(),
                new LinkedHashMap<String, ShopItem>());
    }

    /**
     * Carica il catalogo dal file indicato.
     *
     * @return il catalogo, oppure {@code null} se il file non e' leggibile
     */
    public static ShopCatalog load(File file, Logger logger) {
        if (file == null || !file.isFile()) {
            logger.warning("shop.yml non trovato: shop disabilitato.");
            return null;
        }

        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(file);
        } catch (IOException exception) {
            logger.warning("Impossibile leggere shop.yml: " + exception.getMessage());
            return null;
        } catch (InvalidConfigurationException exception) {
            logger.warning("shop.yml non valido: " + exception.getMessage());
            return null;
        }

        ConfigurationSection categoriesSection =
                configuration.getConfigurationSection("categories");
        if (categoriesSection == null) {
            logger.warning("shop.yml privo della sezione categories.");
            return null;
        }

        List<ShopCategory> categories = new ArrayList<ShopCategory>();
        Map<String, ShopItem> itemsById = new LinkedHashMap<String, ShopItem>();

        for (String categoryId : categoriesSection.getKeys(false)) {
            ConfigurationSection section =
                    categoriesSection.getConfigurationSection(categoryId);
            if (section == null) {
                continue;
            }
            Material icon = parseMaterial(section.getString("icon"), logger,
                    "categoria " + categoryId);
            if (icon == null) {
                continue;
            }
            ShopCategory category = new ShopCategory(
                    categoryId.toLowerCase(Locale.ROOT),
                    section.getString("name", categoryId),
                    section.getString("name-en", ""),
                    icon,
                    (byte) section.getInt("icon-data", 0),
                    section.getInt("slot", categories.size()));

            ConfigurationSection itemsSection =
                    section.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String itemId : itemsSection.getKeys(false)) {
                    ShopItem item = readItem(itemsSection.getConfigurationSection(itemId),
                            itemId.toLowerCase(Locale.ROOT), category.getId(), logger);
                    if (item != null) {
                        category.addItem(item);
                        itemsById.put(item.getId(), item);
                    }
                }
            }
            categories.add(category);
        }

        if (categories.isEmpty()) {
            logger.warning("shop.yml non contiene categorie valide.");
            return null;
        }
        return new ShopCatalog(categories, itemsById);
    }

    private static ShopItem readItem(ConfigurationSection section, String itemId,
                                     String categoryId, Logger logger) {
        if (section == null) {
            return null;
        }
        String context = "articolo " + itemId;
        Material material = parseMaterial(section.getString("material"), logger, context);
        if (material == null) {
            return null;
        }

        ResourceType currency = ResourceType.fromString(section.getString("currency"));
        if (currency == null) {
            logger.warning("Shop: " + context + " ignorato, valuta non valida.");
            return null;
        }

        Map<Enchantment, Integer> enchantments =
                new LinkedHashMap<Enchantment, Integer>();
        ConfigurationSection enchantSection =
                section.getConfigurationSection("enchantments");
        if (enchantSection != null) {
            for (String key : enchantSection.getKeys(false)) {
                Enchantment enchantment = Enchantment.getByName(
                        key.toUpperCase(Locale.ROOT));
                if (enchantment == null) {
                    logger.warning("Shop: incantesimo sconosciuto " + key
                            + " in " + context + ".");
                    continue;
                }
                enchantments.put(enchantment, enchantSection.getInt(key, 1));
            }
        }

        List<PotionEffect> effects = new ArrayList<PotionEffect>();
        for (String raw : section.getStringList("effects")) {
            PotionEffect effect = parseEffect(raw, logger, context);
            if (effect != null) {
                effects.add(effect);
            }
        }

        return new ShopItem(itemId, categoryId, material,
                (byte) section.getInt("data", 0),
                section.getInt("amount", 1),
                section.getString("name", itemId),
                section.getString("name-en", ""),
                section.getStringList("lore"),
                section.getStringList("lore-en"),
                section.getInt("slot", -1),
                currency,
                section.getInt("price", 1),
                section.getBoolean("team-color", false),
                enchantments,
                effects);
    }

    /**
     * Interpreta la forma {@code TIPO:durata_secondi:livello}.
     */
    private static PotionEffect parseEffect(String raw, Logger logger, String context) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split(":");
        PotionEffectType type = PotionEffectType.getByName(
                parts[0].trim().toUpperCase(Locale.ROOT));
        if (type == null) {
            logger.warning("Shop: effetto sconosciuto " + raw + " in " + context + ".");
            return null;
        }
        try {
            int seconds = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 10;
            int amplifier = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0;
            return new PotionEffect(type, Math.max(1, seconds) * 20,
                    Math.max(0, amplifier));
        } catch (NumberFormatException exception) {
            logger.warning("Shop: effetto malformato " + raw + " in " + context + ".");
            return null;
        }
    }

    private static Material parseMaterial(String raw, Logger logger, String context) {
        if (raw == null) {
            logger.warning("Shop: materiale mancante in " + context + ".");
            return null;
        }
        Material material = Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT));
        if (material == null) {
            logger.warning("Shop: materiale sconosciuto " + raw
                    + " in " + context + ".");
        }
        return material;
    }

    public List<ShopCategory> getCategories() {
        return categories;
    }

    /**
     * @return la categoria richiesta, oppure la prima disponibile
     */
    public ShopCategory getCategoryOrFirst(String categoryId) {
        if (categoryId != null) {
            for (ShopCategory category : categories) {
                if (category.getId().equalsIgnoreCase(categoryId)) {
                    return category;
                }
            }
        }
        return categories.isEmpty() ? null : categories.get(0);
    }

    public ShopItem getItem(String itemId) {
        return itemId == null ? null : itemsById.get(itemId.toLowerCase(Locale.ROOT));
    }

    public boolean isEmpty() {
        return categories.isEmpty();
    }
}
