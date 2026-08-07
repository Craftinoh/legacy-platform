package it.legacynetwork.chickenwars.config;

import it.legacynetwork.chickenwars.model.ResourceType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Intervalli e quantita' dei generatori, per risorsa e per livello.
 */
public final class GeneratorSettings {

    private final Map<ResourceType, List<GeneratorTier>> tiers;
    private final int maximumGroundItems;
    private final boolean itemStacking;
    private final double mergeRadius;

    private GeneratorSettings(Map<ResourceType, List<GeneratorTier>> tiers,
                              int maximumGroundItems, boolean itemStacking,
                              double mergeRadius) {
        EnumMap<ResourceType, List<GeneratorTier>> copy =
                new EnumMap<ResourceType, List<GeneratorTier>>(ResourceType.class);
        for (Map.Entry<ResourceType, List<GeneratorTier>> entry : tiers.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableList(
                    new ArrayList<GeneratorTier>(entry.getValue())));
        }
        this.tiers = Collections.unmodifiableMap(copy);
        this.maximumGroundItems = maximumGroundItems;
        this.itemStacking = itemStacking;
        this.mergeRadius = mergeRadius;
    }

    /**
     * Costruisce le impostazioni dalla sezione {@code generators} di config.yml.
     *
     * <p>Le risorse non descritte nel file ricevono valori predefiniti coerenti
     * con il ritmo classico del minigame.</p>
     *
     * @param section sezione da leggere, eventualmente nulla
     * @return le impostazioni risultanti, mai nulle
     */
    public static GeneratorSettings fromSection(ConfigurationSection section) {
        EnumMap<ResourceType, List<GeneratorTier>> result =
                new EnumMap<ResourceType, List<GeneratorTier>>(ResourceType.class);
        for (ResourceType type : ResourceType.values()) {
            result.put(type, defaultTiers(type));
        }

        int maximumGroundItems = 32;
        boolean itemStacking = true;
        double mergeRadius = 2.0D;

        if (section != null) {
            maximumGroundItems = Math.max(1,
                    section.getInt("maximum-ground-items", maximumGroundItems));
            itemStacking = section.getBoolean("item-stacking", itemStacking);
            mergeRadius = Math.max(0.5D,
                    section.getDouble("merge-radius", mergeRadius));

            ConfigurationSection typesSection =
                    section.getConfigurationSection("types");
            if (typesSection != null) {
                for (ResourceType type : ResourceType.values()) {
                    List<GeneratorTier> parsed = readTiers(
                            typesSection.getConfigurationSection(type.name()));
                    if (parsed != null && !parsed.isEmpty()) {
                        result.put(type, parsed);
                    }
                }
            }
        }

        return new GeneratorSettings(result, maximumGroundItems, itemStacking,
                mergeRadius);
    }

    private static List<GeneratorTier> readTiers(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        ConfigurationSection levels = section.getConfigurationSection("levels");
        if (levels == null) {
            return null;
        }
        List<GeneratorTier> parsed = new ArrayList<GeneratorTier>();
        int level = 1;
        while (levels.contains(String.valueOf(level))) {
            ConfigurationSection levelSection =
                    levels.getConfigurationSection(String.valueOf(level));
            if (levelSection == null) {
                break;
            }
            double seconds = levelSection.getDouble("interval-seconds", 1.0D);
            int amount = levelSection.getInt("amount", 1);
            parsed.add(new GeneratorTier((int) Math.round(seconds * 20.0D), amount));
            level++;
        }
        return parsed;
    }

    private static List<GeneratorTier> defaultTiers(ResourceType type) {
        List<GeneratorTier> defaults = new ArrayList<GeneratorTier>();
        switch (type) {
            case IRON:
                defaults.add(new GeneratorTier(30, 1));
                defaults.add(new GeneratorTier(20, 1));
                defaults.add(new GeneratorTier(14, 1));
                break;
            case GOLD:
                defaults.add(new GeneratorTier(140, 1));
                defaults.add(new GeneratorTier(100, 1));
                defaults.add(new GeneratorTier(70, 1));
                break;
            case DIAMOND:
                defaults.add(new GeneratorTier(600, 1));
                defaults.add(new GeneratorTier(460, 1));
                defaults.add(new GeneratorTier(340, 1));
                break;
            case EMERALD:
                defaults.add(new GeneratorTier(1300, 1));
                defaults.add(new GeneratorTier(1000, 1));
                defaults.add(new GeneratorTier(760, 1));
                break;
            case FEATHER:
            default:
                defaults.add(new GeneratorTier(200, 1));
                defaults.add(new GeneratorTier(150, 1));
                defaults.add(new GeneratorTier(110, 1));
                break;
        }
        return defaults;
    }

    /**
     * Restituisce i parametri per la risorsa e il livello indicati.
     *
     * <p>I livelli oltre l'ultimo configurato riusano l'ultimo disponibile.</p>
     *
     * @param type  risorsa richiesta
     * @param level livello del generatore, a partire da 1
     * @return i parametri corrispondenti, mai nulli
     */
    public GeneratorTier getTier(ResourceType type, int level) {
        List<GeneratorTier> available = tiers.get(type);
        if (available == null || available.isEmpty()) {
            return new GeneratorTier(20, 1);
        }
        int index = Math.max(1, level) - 1;
        if (index >= available.size()) {
            index = available.size() - 1;
        }
        return available.get(index);
    }

    /** Numero massimo di livelli configurati per la risorsa indicata. */
    public int getMaximumLevel(ResourceType type) {
        List<GeneratorTier> available = tiers.get(type);
        return available == null || available.isEmpty() ? 1 : available.size();
    }

    public int getMaximumGroundItems() {
        return maximumGroundItems;
    }

    public boolean isItemStacking() {
        return itemStacking;
    }

    public double getMergeRadius() {
        return mergeRadius;
    }
}
