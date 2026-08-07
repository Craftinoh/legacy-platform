package it.legacynetwork.chickenwars.player.equipment;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Materiali associati a ogni tier di equipaggiamento.
 *
 * <p>Il tier resta un concetto astratto nello stato autorevole; qui viene
 * tradotto nei materiali 1.8 effettivamente consegnati, in modo che un server
 * possa cambiare la resa senza toccare la progressione.</p>
 */
public final class EquipmentSettings {

    private final Map<ArmorTier, Material> leggings;
    private final Map<ArmorTier, Material> boots;
    private final Map<SwordTier, Material> swords;
    private final Map<ToolTier, Material> pickaxes;
    private final Map<ToolTier, Material> axes;
    private final Material shears;
    private final Material helmet;
    private final Material chestplate;
    private final ToolTier minimumToolTier;

    private EquipmentSettings(Builder builder) {
        this.leggings = builder.leggings;
        this.boots = builder.boots;
        this.swords = builder.swords;
        this.pickaxes = builder.pickaxes;
        this.axes = builder.axes;
        this.shears = builder.shears;
        this.helmet = builder.helmet;
        this.chestplate = builder.chestplate;
        this.minimumToolTier = builder.minimumToolTier;
    }

    /**
     * Costruisce le impostazioni leggendo la sezione {@code equipment}.
     *
     * <p>Ogni voce assente o non valida ricade sul materiale predefinito, cosi'
     * una configurazione parziale resta utilizzabile.</p>
     *
     * @param section sezione da leggere, eventualmente nulla
     * @return le impostazioni risultanti, mai nulle
     */
    public static EquipmentSettings fromSection(ConfigurationSection section) {
        Builder builder = new Builder();
        if (section == null) {
            return builder.build();
        }

        readArmor(section.getConfigurationSection("armor"), builder);
        readEnumMaterials(section.getConfigurationSection("sword"),
                builder.swords, SwordTier.class);
        readEnumMaterials(section.getConfigurationSection("pickaxe"),
                builder.pickaxes, ToolTier.class);
        readEnumMaterials(section.getConfigurationSection("axe"),
                builder.axes, ToolTier.class);

        Material configuredShears = parse(section.getString("shears"));
        if (configuredShears != null) {
            builder.shears = configuredShears;
        }
        Material configuredHelmet = parse(section.getString("helmet"));
        if (configuredHelmet != null) {
            builder.helmet = configuredHelmet;
        }
        Material configuredChestplate = parse(section.getString("chestplate"));
        if (configuredChestplate != null) {
            builder.chestplate = configuredChestplate;
        }

        ToolTier minimum = parseToolTier(section.getString("minimum-tool-tier"));
        if (minimum != null && minimum != ToolTier.NONE) {
            builder.minimumToolTier = minimum;
        }
        return builder.build();
    }

    private static void readArmor(ConfigurationSection section, Builder builder) {
        if (section == null) {
            return;
        }
        for (ArmorTier tier : ArmorTier.values()) {
            ConfigurationSection tierSection =
                    section.getConfigurationSection(tier.name());
            if (tierSection == null) {
                continue;
            }
            Material configuredLeggings = parse(tierSection.getString("leggings"));
            if (configuredLeggings != null) {
                builder.leggings.put(tier, configuredLeggings);
            }
            Material configuredBoots = parse(tierSection.getString("boots"));
            if (configuredBoots != null) {
                builder.boots.put(tier, configuredBoots);
            }
        }
    }

    private static <T extends Enum<T>> void readEnumMaterials(
            ConfigurationSection section, Map<T, Material> target,
            Class<T> type) {
        if (section == null) {
            return;
        }
        for (T value : type.getEnumConstants()) {
            Material material = parse(section.getString(value.name()));
            if (material != null) {
                target.put(value, material);
            }
        }
    }

    private static Material parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT));
    }

    private static ToolTier parseToolTier(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return ToolTier.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * @return il materiale dei leggings, oppure {@code null} se il tier non ne
     *         prevede
     */
    public Material getLeggings(ArmorTier tier) {
        return tier == null ? null : leggings.get(tier);
    }

    /**
     * @return il materiale degli stivali, oppure {@code null}
     */
    public Material getBoots(ArmorTier tier) {
        return tier == null ? null : boots.get(tier);
    }

    /**
     * @return il materiale della spada, oppure {@code null}
     */
    public Material getSword(SwordTier tier) {
        return tier == null ? null : swords.get(tier);
    }

    /**
     * @return il materiale del piccone, oppure {@code null} per {@code NONE}
     */
    public Material getPickaxe(ToolTier tier) {
        return tier == null ? null : pickaxes.get(tier);
    }

    /**
     * @return il materiale dell'ascia, oppure {@code null} per {@code NONE}
     */
    public Material getAxe(ToolTier tier) {
        return tier == null ? null : axes.get(tier);
    }

    public Material getShears() {
        return shears;
    }

    public Material getHelmet() {
        return helmet;
    }

    public Material getChestplate() {
        return chestplate;
    }

    /**
     * Tier minimo sotto il quale piccone e ascia non possono scendere.
     */
    public ToolTier getMinimumToolTier() {
        return minimumToolTier;
    }

    /**
     * Alza il tier al minimo configurato quando il downgrade lo ha superato.
     */
    public ToolTier clampToMinimum(ToolTier tier) {
        if (tier == null || tier == ToolTier.NONE) {
            return ToolTier.NONE;
        }
        return tier.getLevel() < minimumToolTier.getLevel()
                ? minimumToolTier : tier;
    }

    /**
     * Valori predefiniti coerenti con i materiali disponibili su 1.8.8.
     */
    private static final class Builder {

        private final Map<ArmorTier, Material> leggings =
                new EnumMap<ArmorTier, Material>(ArmorTier.class);
        private final Map<ArmorTier, Material> boots =
                new EnumMap<ArmorTier, Material>(ArmorTier.class);
        private final Map<SwordTier, Material> swords =
                new EnumMap<SwordTier, Material>(SwordTier.class);
        private final Map<ToolTier, Material> pickaxes =
                new EnumMap<ToolTier, Material>(ToolTier.class);
        private final Map<ToolTier, Material> axes =
                new EnumMap<ToolTier, Material>(ToolTier.class);

        private Material shears = Material.SHEARS;
        private Material helmet = Material.LEATHER_HELMET;
        private Material chestplate = Material.LEATHER_CHESTPLATE;
        private ToolTier minimumToolTier = ToolTier.TIER_1;

        private Builder() {
            leggings.put(ArmorTier.LEATHER, Material.LEATHER_LEGGINGS);
            leggings.put(ArmorTier.CHAINMAIL, Material.CHAINMAIL_LEGGINGS);
            leggings.put(ArmorTier.IRON, Material.IRON_LEGGINGS);
            leggings.put(ArmorTier.DIAMOND, Material.DIAMOND_LEGGINGS);

            boots.put(ArmorTier.LEATHER, Material.LEATHER_BOOTS);
            boots.put(ArmorTier.CHAINMAIL, Material.CHAINMAIL_BOOTS);
            boots.put(ArmorTier.IRON, Material.IRON_BOOTS);
            boots.put(ArmorTier.DIAMOND, Material.DIAMOND_BOOTS);

            swords.put(SwordTier.WOOD, Material.WOOD_SWORD);
            swords.put(SwordTier.STONE, Material.STONE_SWORD);
            swords.put(SwordTier.IRON, Material.IRON_SWORD);
            swords.put(SwordTier.DIAMOND, Material.DIAMOND_SWORD);

            pickaxes.put(ToolTier.TIER_1, Material.WOOD_PICKAXE);
            pickaxes.put(ToolTier.TIER_2, Material.STONE_PICKAXE);
            pickaxes.put(ToolTier.TIER_3, Material.IRON_PICKAXE);
            pickaxes.put(ToolTier.TIER_4, Material.DIAMOND_PICKAXE);

            axes.put(ToolTier.TIER_1, Material.WOOD_AXE);
            axes.put(ToolTier.TIER_2, Material.STONE_AXE);
            axes.put(ToolTier.TIER_3, Material.IRON_AXE);
            axes.put(ToolTier.TIER_4, Material.DIAMOND_AXE);
        }

        private EquipmentSettings build() {
            return new EquipmentSettings(this);
        }
    }
}
