package it.legacynetwork.chickenwars.upgrade;

import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.shop.ItemCost;
import it.legacynetwork.chickenwars.shop.ShopConfigLoader;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lettura e validazione di {@code upgrades.yml}.
 *
 * <p>Una voce errata non interrompe il caricamento: viene scartata e descritta
 * in {@link UpgradeCatalog#getWarnings()}, cosi' un refuso non rende
 * inutilizzabile l'intero sistema di upgrade.</p>
 *
 * <p>Lavora su {@link ConfigurationSection}, quindi e' verificabile con test
 * unitari senza un server attivo.</p>
 */
public final class UpgradeConfigLoader {

    private static final int DEFAULT_MAXIMUM_TRAPS = 3;

    private UpgradeConfigLoader() {
    }

    /**
     * Costruisce il catalogo dalla radice di {@code upgrades.yml}.
     *
     * @param root sezione radice, eventualmente nulla
     * @return il catalogo, mai nullo
     */
    public static UpgradeCatalog load(ConfigurationSection root) {
        List<String> warnings = new ArrayList<String>();
        if (root == null) {
            warnings.add("upgrades.yml assente o illeggibile");
            return emptyWith(warnings);
        }

        Map<TeamUpgradeType, TeamUpgradeDefinition> teamUpgrades =
                readTeamUpgrades(root.getConfigurationSection("team-upgrades"),
                        warnings);
        Map<RoyalUpgradeType, RoyalUpgradeDefinition> royalUpgrades =
                readRoyalUpgrades(root.getConfigurationSection("royal-upgrades"),
                        warnings);

        ConfigurationSection trapSection =
                root.getConfigurationSection("traps");
        int maximumTraps = trapSection == null ? DEFAULT_MAXIMUM_TRAPS
                : Math.max(1, trapSection.getInt("maximum", DEFAULT_MAXIMUM_TRAPS));
        List<Map<String, ItemCost>> trapCosts = readTrapCosts(
                trapSection == null ? null
                        : trapSection.getConfigurationSection("position-costs"),
                maximumTraps, warnings);
        Map<String, TrapDefinition> traps = readTraps(
                trapSection == null ? null
                        : trapSection.getConfigurationSection("types"),
                warnings);

        return new UpgradeCatalog(teamUpgrades, royalUpgrades, traps,
                trapCosts, maximumTraps, warnings);
    }

    private static UpgradeCatalog emptyWith(List<String> warnings) {
        return new UpgradeCatalog(
                new EnumMap<TeamUpgradeType, TeamUpgradeDefinition>(
                        TeamUpgradeType.class),
                new EnumMap<RoyalUpgradeType, RoyalUpgradeDefinition>(
                        RoyalUpgradeType.class),
                new LinkedHashMap<String, TrapDefinition>(),
                new ArrayList<Map<String, ItemCost>>(),
                DEFAULT_MAXIMUM_TRAPS, warnings);
    }

    // ------------------------------------------------------------------
    // Upgrade di squadra
    // ------------------------------------------------------------------

    private static Map<TeamUpgradeType, TeamUpgradeDefinition> readTeamUpgrades(
            ConfigurationSection section, List<String> warnings) {
        Map<TeamUpgradeType, TeamUpgradeDefinition> result =
                new EnumMap<TeamUpgradeType, TeamUpgradeDefinition>(
                        TeamUpgradeType.class);
        if (section == null) {
            warnings.add("sezione 'team-upgrades' mancante");
            return result;
        }

        for (String rawId : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(rawId);
            if (entry == null) {
                warnings.add("upgrade '" + rawId + "': voce non valida");
                continue;
            }
            TeamUpgradeType type = TeamUpgradeType.fromString(rawId);
            if (type == null) {
                warnings.add("upgrade '" + rawId + "': tipo sconosciuto");
                continue;
            }
            if (result.containsKey(type)) {
                warnings.add("upgrade '" + rawId + "': duplicato, ignorato");
                continue;
            }

            Material icon = parseMaterial(entry.getString("icon"));
            if (icon == null) {
                warnings.add("upgrade '" + rawId + "': materiale non valido ("
                        + entry.getString("icon") + ")");
                continue;
            }

            PotionEffectType effect = null;
            if (type.getApplication() != TeamUpgradeType.Application.ENCHANT) {
                effect = parseEffectType(entry.getString("effect"));
                if (effect == null) {
                    warnings.add("upgrade '" + rawId + "': effetto sconosciuto ("
                            + entry.getString("effect") + ")");
                    continue;
                }
            }

            List<UpgradeLevel> levels = readLevels(rawId,
                    entry.getConfigurationSection("levels"), warnings);
            if (levels.isEmpty()) {
                warnings.add("upgrade '" + rawId + "': nessun livello valido");
                continue;
            }

            result.put(type, new TeamUpgradeDefinition(
                    type.name().toLowerCase(Locale.ROOT), type, icon,
                    (byte) entry.getInt("icon-data", 0),
                    entry.getInt("slot", -1),
                    entry.getBoolean("enabled", true), effect, levels));
        }
        return result;
    }

    private static Map<RoyalUpgradeType, RoyalUpgradeDefinition>
            readRoyalUpgrades(ConfigurationSection section,
                              List<String> warnings) {
        Map<RoyalUpgradeType, RoyalUpgradeDefinition> result =
                new EnumMap<RoyalUpgradeType, RoyalUpgradeDefinition>(
                        RoyalUpgradeType.class);
        if (section == null) {
            warnings.add("sezione 'royal-upgrades' mancante");
            return result;
        }

        for (String rawId : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(rawId);
            if (entry == null) {
                warnings.add("upgrade reale '" + rawId + "': voce non valida");
                continue;
            }
            RoyalUpgradeType type = RoyalUpgradeType.fromString(rawId);
            if (type == null) {
                warnings.add("upgrade reale '" + rawId + "': tipo sconosciuto");
                continue;
            }
            if (result.containsKey(type)) {
                warnings.add("upgrade reale '" + rawId + "': duplicato");
                continue;
            }
            Material icon = parseMaterial(entry.getString("icon"));
            if (icon == null) {
                warnings.add("upgrade reale '" + rawId
                        + "': materiale non valido");
                continue;
            }
            List<UpgradeLevel> levels = readLevels(rawId,
                    entry.getConfigurationSection("levels"), warnings);
            if (levels.isEmpty()) {
                warnings.add("upgrade reale '" + rawId
                        + "': nessun livello valido");
                continue;
            }
            result.put(type, new RoyalUpgradeDefinition(type, icon,
                    (byte) entry.getInt("icon-data", 0),
                    entry.getInt("slot", -1),
                    entry.getBoolean("enabled", true), levels));
        }
        return result;
    }

    /**
     * Legge i livelli richiedendo una sequenza contigua a partire da uno.
     */
    private static List<UpgradeLevel> readLevels(String ownerId,
                                                 ConfigurationSection section,
                                                 List<String> warnings) {
        List<UpgradeLevel> levels = new ArrayList<UpgradeLevel>();
        if (section == null) {
            warnings.add("'" + ownerId + "': sezione 'levels' mancante");
            return levels;
        }

        int expected = 1;
        while (section.isConfigurationSection(String.valueOf(expected))) {
            ConfigurationSection levelSection =
                    section.getConfigurationSection(String.valueOf(expected));
            Map<String, ItemCost> costs =
                    readCosts(ownerId + " livello " + expected,
                            levelSection.getConfigurationSection("cost"),
                            warnings);
            if (costs.isEmpty()) {
                warnings.add("'" + ownerId + "' livello " + expected
                        + ": nessun costo valido, livello scartato");
                break;
            }
            levels.add(new UpgradeLevel(expected, costs,
                    levelSection.getInt("amplifier", expected - 1),
                    levelSection.getDouble("value", 0.0D),
                    levelSection.getInt("duration-ticks", 0)));
            expected++;
        }

        // Un livello dichiarato oltre la sequenza contigua indica un buco.
        for (String key : section.getKeys(false)) {
            Integer parsed = parseInteger(key);
            if (parsed != null && parsed.intValue() > levels.size()) {
                warnings.add("'" + ownerId + "': livello " + parsed
                        + " fuori sequenza, atteso " + (levels.size() + 1));
            }
        }
        return levels;
    }

    // ------------------------------------------------------------------
    // Trappole
    // ------------------------------------------------------------------

    private static List<Map<String, ItemCost>> readTrapCosts(
            ConfigurationSection section, int maximumTraps,
            List<String> warnings) {
        List<Map<String, ItemCost>> costs =
                new ArrayList<Map<String, ItemCost>>();
        if (section == null) {
            warnings.add("sezione 'traps.position-costs' mancante");
            return costs;
        }
        for (int position = 1; position <= maximumTraps; position++) {
            ConfigurationSection entry =
                    section.getConfigurationSection(String.valueOf(position));
            if (entry == null) {
                warnings.add("trappole: costo mancante per la posizione "
                        + position);
                break;
            }
            Map<String, ItemCost> parsed = readCosts(
                    "trappola posizione " + position, entry, warnings);
            if (parsed.isEmpty()) {
                warnings.add("trappole: costi non validi per la posizione "
                        + position);
                break;
            }
            costs.add(parsed);
        }
        return costs;
    }

    private static Map<String, TrapDefinition> readTraps(
            ConfigurationSection section, List<String> warnings) {
        Map<String, TrapDefinition> traps =
                new LinkedHashMap<String, TrapDefinition>();
        if (section == null) {
            warnings.add("sezione 'traps.types' mancante");
            return traps;
        }

        for (String rawId : section.getKeys(false)) {
            String id = rawId.trim().toLowerCase(Locale.ROOT);
            ConfigurationSection entry = section.getConfigurationSection(rawId);
            if (entry == null) {
                warnings.add("trappola '" + rawId + "': voce non valida");
                continue;
            }
            if (traps.containsKey(id)) {
                warnings.add("trappola '" + id + "': duplicata, ignorata");
                continue;
            }
            Material icon = parseMaterial(entry.getString("icon"));
            if (icon == null) {
                warnings.add("trappola '" + id + "': materiale non valido");
                continue;
            }

            List<TrapEffect> intruder = readEffects(id, "intruder-effects",
                    entry, warnings);
            List<TrapEffect> defender = readEffects(id, "defender-effects",
                    entry, warnings);
            boolean reveals = entry.getBoolean("reveals-invisibility", false);
            if (intruder.isEmpty() && defender.isEmpty() && !reveals) {
                warnings.add("trappola '" + id + "': nessun effetto utile");
                continue;
            }

            traps.put(id, new TrapDefinition(id, icon,
                    (byte) entry.getInt("icon-data", 0),
                    entry.getInt("slot", -1),
                    entry.getBoolean("enabled", true), reveals,
                    intruder, defender));
        }
        return traps;
    }

    private static List<TrapEffect> readEffects(String trapId, String path,
                                                ConfigurationSection entry,
                                                List<String> warnings) {
        List<TrapEffect> effects = new ArrayList<TrapEffect>();
        for (String raw : entry.getStringList(path)) {
            String[] parts = String.valueOf(raw).split(":");
            PotionEffectType type = parseEffectType(parts[0]);
            if (type == null) {
                warnings.add("trappola '" + trapId + "': effetto sconosciuto "
                        + raw);
                continue;
            }
            try {
                int seconds = parts.length > 1
                        ? Integer.parseInt(parts[1].trim()) : 5;
                int amplifier = parts.length > 2
                        ? Integer.parseInt(parts[2].trim()) : 0;
                if (seconds <= 0 || amplifier < 0) {
                    warnings.add("trappola '" + trapId
                            + "': durata o livello non validi in " + raw);
                    continue;
                }
                effects.add(new TrapEffect(type, amplifier, seconds * 20));
            } catch (NumberFormatException exception) {
                warnings.add("trappola '" + trapId + "': effetto malformato "
                        + raw);
            }
        }
        return effects;
    }

    // ------------------------------------------------------------------
    // Costi
    // ------------------------------------------------------------------

    /**
     * Legge i costi per ogni profilo economico atteso.
     */
    private static Map<String, ItemCost> readCosts(String context,
                                                   ConfigurationSection section,
                                                   List<String> warnings) {
        Map<String, ItemCost> costs = new LinkedHashMap<String, ItemCost>();
        if (section == null) {
            warnings.add(context + ": sezione costi mancante");
            return costs;
        }

        for (String profileId : ShopConfigLoader.EXPECTED_PROFILES) {
            ConfigurationSection entry =
                    section.getConfigurationSection(profileId);
            if (entry == null) {
                warnings.add(context + ": profilo '" + profileId + "' mancante");
                continue;
            }
            ResourceType currency =
                    ResourceType.fromString(entry.getString("currency"));
            if (currency == null) {
                warnings.add(context + ": valuta sconosciuta '"
                        + entry.getString("currency") + "' in " + profileId);
                continue;
            }
            int amount = entry.getInt("amount", -1);
            if (amount < 0) {
                warnings.add(context + ": prezzo negativo o mancante in "
                        + profileId);
                continue;
            }
            costs.put(profileId, new ItemCost(currency, amount));
        }
        return costs;
    }

    private static Material parseMaterial(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT));
    }

    private static PotionEffectType parseEffectType(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return PotionEffectType.getByName(raw.trim().toUpperCase(Locale.ROOT));
    }

    private static Integer parseInteger(String raw) {
        try {
            return Integer.valueOf(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
