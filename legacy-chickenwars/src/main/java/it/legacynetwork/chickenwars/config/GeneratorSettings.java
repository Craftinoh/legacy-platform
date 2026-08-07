package it.legacynetwork.chickenwars.config;

import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.generator.CatchUpPolicy;
import it.legacynetwork.chickenwars.generator.ChunkPolicy;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Intervalli e quantita' dei generatori, per risorsa e per livello.
 */
public final class GeneratorSettings {

    private final Map<ResourceType, List<GeneratorTier>> tiers;
    private final int maximumGroundItems;
    private final boolean itemStacking;
    private final double mergeRadius;
    private final boolean enabled;
    private final CatchUpPolicy catchUpPolicy;
    private final int maximumCatchUpDrops;
    private final ChunkPolicy chunkPolicy;
    private final Map<String, Double> intervalMultipliers;
    private final Map<String, Double> amountMultipliers;

    private GeneratorSettings(Map<ResourceType, List<GeneratorTier>> tiers,
                              int maximumGroundItems, boolean itemStacking,
                              double mergeRadius, boolean enabled,
                              CatchUpPolicy catchUpPolicy,
                              int maximumCatchUpDrops, ChunkPolicy chunkPolicy,
                              Map<String, Double> intervalMultipliers,
                              Map<String, Double> amountMultipliers) {
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
        this.enabled = enabled;
        this.catchUpPolicy = catchUpPolicy;
        this.maximumCatchUpDrops = maximumCatchUpDrops;
        this.chunkPolicy = chunkPolicy;
        this.intervalMultipliers = Collections.unmodifiableMap(
                new java.util.LinkedHashMap<String, Double>(intervalMultipliers));
        this.amountMultipliers = Collections.unmodifiableMap(
                new java.util.LinkedHashMap<String, Double>(amountMultipliers));
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
        boolean enabled = true;
        CatchUpPolicy catchUpPolicy = CatchUpPolicy.LIMITED;
        int maximumCatchUpDrops = 2;
        ChunkPolicy chunkPolicy = ChunkPolicy.REQUIRE_LOADED;
        Map<String, Double> intervalMultipliers =
                new java.util.LinkedHashMap<String, Double>();
        Map<String, Double> amountMultipliers =
                new java.util.LinkedHashMap<String, Double>();

        if (section != null) {
            maximumGroundItems = Math.max(1,
                    section.getInt("maximum-ground-items", maximumGroundItems));
            itemStacking = section.getBoolean("item-stacking", itemStacking);
            mergeRadius = Math.max(0.5D,
                    section.getDouble("merge-radius", mergeRadius));
            enabled = section.getBoolean("enabled", enabled);
            maximumCatchUpDrops = Math.max(1,
                    section.getInt("maximum-catch-up-drops", 2));
            try {
                catchUpPolicy = CatchUpPolicy.valueOf(section.getString(
                        "catch-up-policy", "LIMITED").trim().toUpperCase(Locale.ROOT));
                chunkPolicy = ChunkPolicy.valueOf(section.getString(
                        "chunk-policy", "REQUIRE_LOADED").trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Politica generatori non valida", exception);
            }

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
            ConfigurationSection profiles =
                    section.getConfigurationSection("profiles");
            if (profiles != null) {
                for (String profileId : profiles.getKeys(false)) {
                    double interval = profiles.getDouble(profileId
                            + ".interval-multiplier", 1.0D);
                    double amount = profiles.getDouble(profileId
                            + ".amount-multiplier", 1.0D);
                    if (interval <= 0.0D || amount <= 0.0D
                            || Double.isNaN(interval) || Double.isNaN(amount)) {
                        throw new IllegalArgumentException(
                                "Profilo generatori non valido: " + profileId);
                    }
                    String normalized = profileId.trim()
                            .toLowerCase(Locale.ROOT);
                    intervalMultipliers.put(normalized, Double.valueOf(interval));
                    amountMultipliers.put(normalized, Double.valueOf(amount));
                }
            }
        }

        return new GeneratorSettings(result, maximumGroundItems, itemStacking,
                mergeRadius, enabled, catchUpPolicy, maximumCatchUpDrops,
                chunkPolicy, intervalMultipliers, amountMultipliers);
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

    /** Applica il bilanciamento del profilo economico della modalita'. */
    public GeneratorTier getTier(ResourceType type, int level,
                                 String profileId) {
        GeneratorTier base = getTier(type, level);
        String key = profileId == null ? "" : profileId.trim()
                .toLowerCase(Locale.ROOT);
        Double interval = intervalMultipliers.get(key);
        Double amount = amountMultipliers.get(key);
        return new GeneratorTier((int) Math.round(base.getIntervalTicks()
                * (interval == null ? 1.0D : interval.doubleValue())),
                (int) Math.round(base.getAmount()
                        * (amount == null ? 1.0D : amount.doubleValue())));
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

    public boolean isEnabled() { return enabled; }
    public CatchUpPolicy getCatchUpPolicy() { return catchUpPolicy; }
    public int getMaximumCatchUpDrops() { return maximumCatchUpDrops; }
    public ChunkPolicy getChunkPolicy() { return chunkPolicy; }
}
