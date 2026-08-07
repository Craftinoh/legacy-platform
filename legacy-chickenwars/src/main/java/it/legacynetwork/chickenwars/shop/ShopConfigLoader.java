package it.legacynetwork.chickenwars.shop;

import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.player.equipment.ArmorTier;
import it.legacynetwork.chickenwars.player.equipment.SwordTier;
import it.legacynetwork.chickenwars.player.equipment.ToolTier;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lettura e validazione della configurazione dello shop.
 *
 * <p>Nessuna voce errata interrompe il caricamento: l'articolo o il prezzo non
 * valido viene scartato e descritto in
 * {@link ShopConfiguration#getWarnings()}, cosi' che un refuso non renda
 * inutilizzabile l'intero shop.</p>
 *
 * <p>Il loader lavora su {@link ConfigurationSection}, quindi e' verificabile
 * con test unitari senza un server attivo.</p>
 */
public final class ShopConfigLoader {

    /** Profili economici attesi, allineati ai profili modalita'. */
    public static final List<String> EXPECTED_PROFILES =
            Arrays.asList("solo_duel", "doubles", "trio");

    private ShopConfigLoader() {
    }

    /**
     * Costruisce il catalogo a partire dalla radice di {@code shop.yml}.
     *
     * @param root sezione radice, eventualmente nulla
     * @return il catalogo, mai nullo; vuoto se non e' stato letto nulla
     */
    public static ShopConfiguration load(ConfigurationSection root) {
        List<String> warnings = new ArrayList<String>();
        if (root == null) {
            warnings.add("shop.yml assente o illeggibile");
            return withWarnings(warnings);
        }

        ConfigurationSection categoriesSection =
                root.getConfigurationSection("categories");
        if (categoriesSection == null) {
            warnings.add("sezione 'categories' mancante");
            return withWarnings(warnings);
        }

        Map<String, ShopCategoryDefinition> categories =
                readCategories(categoriesSection, warnings);
        if (categories.isEmpty()) {
            warnings.add("nessuna categoria valida");
            return withWarnings(warnings);
        }

        Map<String, ShopItemDefinition> items;
        Map<String, PricingProfile> profiles;

        if (root.isConfigurationSection("items")) {
            items = readItems(root.getConfigurationSection("items"),
                    categories, warnings);
            profiles = readPricing(root.getConfigurationSection("pricing"),
                    items, warnings);
        } else {
            // Formato storico: articoli annidati nelle categorie con prezzo
            // inline. Viene convertito applicando lo stesso listino a tutti i
            // profili, cosi' una configurazione vecchia resta utilizzabile.
            items = readLegacyItems(categoriesSection, categories, warnings);
            profiles = buildLegacyProfiles(categoriesSection, items, warnings);
            warnings.add("formato shop.yml storico: convertito in memoria, "
                    + "aggiornarlo al formato con 'items' e 'pricing'");
        }

        for (ShopItemDefinition item : items.values()) {
            ShopCategoryDefinition category = categories.get(item.getCategoryId());
            if (category != null) {
                category.addItem(item);
            }
        }

        List<ShopCategoryDefinition> ordered =
                new ArrayList<ShopCategoryDefinition>(categories.values());

        return new ShopConfiguration(ordered, items, profiles, warnings);
    }

    private static ShopConfiguration withWarnings(List<String> warnings) {
        return new ShopConfiguration(
                new ArrayList<ShopCategoryDefinition>(),
                new LinkedHashMap<String, ShopItemDefinition>(),
                new LinkedHashMap<String, PricingProfile>(),
                warnings);
    }

    // ------------------------------------------------------------------
    // Categorie
    // ------------------------------------------------------------------

    private static Map<String, ShopCategoryDefinition> readCategories(
            ConfigurationSection section, List<String> warnings) {
        Map<String, ShopCategoryDefinition> categories =
                new LinkedHashMap<String, ShopCategoryDefinition>();

        for (String rawId : section.getKeys(false)) {
            String id = rawId.trim().toLowerCase(Locale.ROOT);
            ConfigurationSection categorySection =
                    section.getConfigurationSection(rawId);
            if (categorySection == null) {
                warnings.add("categoria '" + rawId + "': voce non valida");
                continue;
            }
            if (categories.containsKey(id)) {
                warnings.add("categoria '" + id + "': duplicata, ignorata");
                continue;
            }

            Material icon = parseMaterial(categorySection.getString("icon"));
            if (icon == null) {
                warnings.add("categoria '" + id + "': materiale icona non valido ("
                        + categorySection.getString("icon") + ")");
                continue;
            }

            int slot = categorySection.getInt("slot", categories.size());
            if (slot < 0 || slot > 8) {
                warnings.add("categoria '" + id + "': slot " + slot
                        + " fuori dalla riga di navigazione, usato "
                        + categories.size());
                slot = categories.size();
            }

            categories.put(id, new ShopCategoryDefinition(id, icon,
                    (byte) categorySection.getInt("icon-data", 0), slot));
        }
        return categories;
    }

    // ------------------------------------------------------------------
    // Articoli
    // ------------------------------------------------------------------

    private static Map<String, ShopItemDefinition> readItems(
            ConfigurationSection section,
            Map<String, ShopCategoryDefinition> categories,
            List<String> warnings) {
        Map<String, ShopItemDefinition> items =
                new LinkedHashMap<String, ShopItemDefinition>();
        if (section == null) {
            warnings.add("sezione 'items' mancante");
            return items;
        }

        for (String rawId : section.getKeys(false)) {
            String id = rawId.trim().toLowerCase(Locale.ROOT);
            ConfigurationSection itemSection =
                    section.getConfigurationSection(rawId);
            if (itemSection == null) {
                warnings.add("articolo '" + rawId + "': voce non valida");
                continue;
            }
            if (items.containsKey(id)) {
                warnings.add("articolo '" + id + "': duplicato, ignorato");
                continue;
            }

            String categoryId = String.valueOf(
                    itemSection.getString("category", "")).trim()
                    .toLowerCase(Locale.ROOT);
            if (!categories.containsKey(categoryId)) {
                warnings.add("articolo '" + id + "': categoria sconosciuta '"
                        + categoryId + "'");
                continue;
            }

            ShopItemDefinition item = readItem(id, categoryId, itemSection,
                    warnings);
            if (item != null) {
                items.put(id, item);
            }
        }
        return items;
    }

    private static ShopItemDefinition readItem(String id, String categoryId,
                                               ConfigurationSection section,
                                               List<String> warnings) {
        Material material = parseMaterial(section.getString("material"));
        if (material == null) {
            warnings.add("articolo '" + id + "': materiale non valido ("
                    + section.getString("material") + ")");
            return null;
        }

        ShopTierKind kind = ShopTierKind.fromString(section.getString("tier"));
        if (kind == null) {
            warnings.add("articolo '" + id + "': tipo tier sconosciuto ("
                    + section.getString("tier") + ")");
            return null;
        }

        ShopItemDefinition.Builder builder =
                new ShopItemDefinition.Builder(id, categoryId)
                        .material(material)
                        .data((byte) section.getInt("data", 0))
                        .amount(section.getInt("amount", 1))
                        .teamColored(section.getBoolean("team-color", false))
                        .repeatable(section.getBoolean("repeatable",
                                !kind.isEquipment()))
                        .slot(section.getInt("slot", -1))
                        .tierKind(kind)
                        .permission(section.getString("permission"));

        if (!applyTierLevel(id, kind, section.getString("level"), builder,
                warnings)) {
            return null;
        }

        builder.enchantments(readEnchantments(id, section, warnings));
        builder.effects(readEffects(id, section, warnings));
        return builder.build();
    }

    /**
     * Applica il livello di progressione richiesto dal tipo di articolo.
     *
     * @return {@code false} se il livello e' obbligatorio ma non valido
     */
    private static boolean applyTierLevel(String id, ShopTierKind kind,
                                          String rawLevel,
                                          ShopItemDefinition.Builder builder,
                                          List<String> warnings) {
        if (!kind.requiresLevel()) {
            return true;
        }
        if (rawLevel == null || rawLevel.trim().isEmpty()) {
            warnings.add("articolo '" + id + "': manca 'level' per il tier "
                    + kind.name());
            return false;
        }
        String level = rawLevel.trim().toUpperCase(Locale.ROOT);

        try {
            switch (kind) {
                case ARMOR:
                    builder.armorTier(ArmorTier.valueOf(level));
                    return true;
                case SWORD:
                    builder.swordTier(SwordTier.valueOf(level));
                    return true;
                case PICKAXE:
                case AXE:
                    ToolTier toolTier = ToolTier.valueOf(level);
                    if (toolTier == ToolTier.NONE) {
                        warnings.add("articolo '" + id
                                + "': NONE non e' un livello acquistabile");
                        return false;
                    }
                    builder.toolTier(toolTier);
                    return true;
                default:
                    return true;
            }
        } catch (IllegalArgumentException exception) {
            warnings.add("articolo '" + id + "': livello '" + level
                    + "' non valido per il tier " + kind.name());
            return false;
        }
    }

    private static Map<Enchantment, Integer> readEnchantments(
            String id, ConfigurationSection section, List<String> warnings) {
        Map<Enchantment, Integer> enchantments =
                new LinkedHashMap<Enchantment, Integer>();
        ConfigurationSection enchantSection =
                section.getConfigurationSection("enchantments");
        if (enchantSection == null) {
            return enchantments;
        }
        for (String key : enchantSection.getKeys(false)) {
            Enchantment enchantment =
                    Enchantment.getByName(key.toUpperCase(Locale.ROOT));
            if (enchantment == null) {
                warnings.add("articolo '" + id + "': incantesimo sconosciuto "
                        + key);
                continue;
            }
            int level = enchantSection.getInt(key, 1);
            if (level <= 0) {
                warnings.add("articolo '" + id + "': livello incantesimo non "
                        + "valido per " + key);
                continue;
            }
            enchantments.put(enchantment, Integer.valueOf(level));
        }
        return enchantments;
    }

    private static List<PotionEffect> readEffects(String id,
                                                  ConfigurationSection section,
                                                  List<String> warnings) {
        List<PotionEffect> effects = new ArrayList<PotionEffect>();
        for (String raw : section.getStringList("effects")) {
            PotionEffect effect = parseEffect(raw);
            if (effect == null) {
                warnings.add("articolo '" + id + "': effetto non valido " + raw);
                continue;
            }
            effects.add(effect);
        }
        return effects;
    }

    // ------------------------------------------------------------------
    // Prezzi
    // ------------------------------------------------------------------

    private static Map<String, PricingProfile> readPricing(
            ConfigurationSection section,
            Map<String, ShopItemDefinition> items,
            List<String> warnings) {
        Map<String, PricingProfile> profiles =
                new LinkedHashMap<String, PricingProfile>();
        if (section == null) {
            warnings.add("sezione 'pricing' mancante");
            return profiles;
        }

        for (String rawProfile : section.getKeys(false)) {
            String profileId = rawProfile.trim().toLowerCase(Locale.ROOT);
            ConfigurationSection profileSection =
                    section.getConfigurationSection(rawProfile);
            if (profileSection == null) {
                warnings.add("profilo '" + rawProfile + "': voce non valida");
                continue;
            }
            profiles.put(profileId, new PricingProfile(profileId,
                    readCosts(profileId, profileSection, items, warnings)));
        }

        for (String expected : EXPECTED_PROFILES) {
            if (!profiles.containsKey(expected)) {
                warnings.add("profilo prezzi '" + expected + "' mancante");
            }
        }
        return profiles;
    }

    private static Map<String, ItemCost> readCosts(
            String profileId, ConfigurationSection section,
            Map<String, ShopItemDefinition> items, List<String> warnings) {
        Map<String, ItemCost> costs = new LinkedHashMap<String, ItemCost>();

        for (String rawItem : section.getKeys(false)) {
            String itemId = rawItem.trim().toLowerCase(Locale.ROOT);
            if (!items.containsKey(itemId)) {
                warnings.add("profilo '" + profileId + "': articolo sconosciuto '"
                        + itemId + "'");
                continue;
            }
            ConfigurationSection costSection =
                    section.getConfigurationSection(rawItem);
            if (costSection == null) {
                warnings.add("profilo '" + profileId + "': prezzo non valido per '"
                        + itemId + "'");
                continue;
            }

            ResourceType currency =
                    ResourceType.fromString(costSection.getString("currency"));
            if (currency == null) {
                warnings.add("profilo '" + profileId + "': valuta sconosciuta '"
                        + costSection.getString("currency") + "' per '"
                        + itemId + "'");
                continue;
            }

            int amount = costSection.getInt("amount", -1);
            if (amount < 0) {
                warnings.add("profilo '" + profileId + "': prezzo negativo o "
                        + "mancante per '" + itemId + "'");
                continue;
            }

            costs.put(itemId, new ItemCost(currency, amount));
        }

        for (String itemId : items.keySet()) {
            if (!costs.containsKey(itemId)) {
                warnings.add("profilo '" + profileId + "': articolo '" + itemId
                        + "' senza prezzo, non sara' acquistabile");
            }
        }
        return costs;
    }

    // ------------------------------------------------------------------
    // Compatibilita' con il formato storico
    // ------------------------------------------------------------------

    private static Map<String, ShopItemDefinition> readLegacyItems(
            ConfigurationSection categoriesSection,
            Map<String, ShopCategoryDefinition> categories,
            List<String> warnings) {
        Map<String, ShopItemDefinition> items =
                new LinkedHashMap<String, ShopItemDefinition>();

        for (String rawCategory : categoriesSection.getKeys(false)) {
            String categoryId = rawCategory.trim().toLowerCase(Locale.ROOT);
            if (!categories.containsKey(categoryId)) {
                continue;
            }
            ConfigurationSection itemsSection = categoriesSection
                    .getConfigurationSection(rawCategory + ".items");
            if (itemsSection == null) {
                continue;
            }
            for (String rawItem : itemsSection.getKeys(false)) {
                String id = rawItem.trim().toLowerCase(Locale.ROOT);
                ConfigurationSection itemSection =
                        itemsSection.getConfigurationSection(rawItem);
                if (itemSection == null || items.containsKey(id)) {
                    continue;
                }
                ShopItemDefinition item =
                        readItem(id, categoryId, itemSection, warnings);
                if (item != null) {
                    items.put(id, item);
                }
            }
        }
        return items;
    }

    private static Map<String, PricingProfile> buildLegacyProfiles(
            ConfigurationSection categoriesSection,
            Map<String, ShopItemDefinition> items,
            List<String> warnings) {
        Map<String, ItemCost> costs = new LinkedHashMap<String, ItemCost>();

        for (String rawCategory : categoriesSection.getKeys(false)) {
            ConfigurationSection itemsSection = categoriesSection
                    .getConfigurationSection(rawCategory + ".items");
            if (itemsSection == null) {
                continue;
            }
            for (String rawItem : itemsSection.getKeys(false)) {
                String id = rawItem.trim().toLowerCase(Locale.ROOT);
                if (!items.containsKey(id)) {
                    continue;
                }
                ConfigurationSection itemSection =
                        itemsSection.getConfigurationSection(rawItem);
                if (itemSection == null) {
                    continue;
                }
                ResourceType currency =
                        ResourceType.fromString(itemSection.getString("currency"));
                int price = itemSection.getInt("price", -1);
                if (currency == null || price < 0) {
                    warnings.add("articolo storico '" + id
                            + "': valuta o prezzo non validi");
                    continue;
                }
                costs.put(id, new ItemCost(currency, price));
            }
        }

        Map<String, PricingProfile> profiles =
                new LinkedHashMap<String, PricingProfile>();
        for (String profileId : EXPECTED_PROFILES) {
            profiles.put(profileId, new PricingProfile(profileId, costs));
        }
        return profiles;
    }

    // ------------------------------------------------------------------
    // Utilita'
    // ------------------------------------------------------------------

    private static Material parseMaterial(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Interpreta la forma {@code TIPO:durata_secondi:livello}.
     *
     * @return l'effetto, oppure {@code null} se il testo non e' valido
     */
    private static PotionEffect parseEffect(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String[] parts = raw.split(":");
        PotionEffectType type = PotionEffectType.getByName(
                parts[0].trim().toUpperCase(Locale.ROOT));
        if (type == null) {
            return null;
        }
        try {
            int seconds = parts.length > 1
                    ? Integer.parseInt(parts[1].trim()) : 10;
            int amplifier = parts.length > 2
                    ? Integer.parseInt(parts[2].trim()) : 0;
            if (seconds <= 0 || amplifier < 0) {
                return null;
            }
            return new PotionEffect(type, seconds * 20, amplifier);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
