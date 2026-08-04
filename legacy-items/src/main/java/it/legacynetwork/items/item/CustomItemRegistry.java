package it.legacynetwork.items.item;

import it.legacynetwork.items.definition.CustomItemAction;
import it.legacynetwork.items.definition.CustomItemActionType;
import it.legacynetwork.items.definition.CustomItemClickActions;
import it.legacynetwork.items.definition.CustomItemDefinition;
import it.legacynetwork.items.definition.CustomItemFlags;
import it.legacynetwork.items.definition.CustomItemLanguage;
import it.legacynetwork.items.definition.CustomItemTrigger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CustomItemRegistry {
    private final Map<String, CustomItemDefinition> items;

    public CustomItemRegistry(Map<String, CustomItemDefinition> items) {
        this.items = items;
    }

    public CustomItemDefinition get(String id) {
        return items.get(id);
    }

    public List<CustomItemDefinition> getAll() {
        return new ArrayList<>(items.values());
    }

    public List<CustomItemDefinition> getByTrigger(CustomItemTrigger trigger) {
        List<CustomItemDefinition> result = new ArrayList<>();
        for (CustomItemDefinition def : items.values()) {
            if (def.isEnabled() && def.getTriggers().contains(trigger)) {
                result.add(def);
            }
        }
        return result;
    }

    public boolean hasItem(String id) {
        return items.containsKey(id);
    }

    public static CustomItemRegistry load(File file) {
        Map<String, CustomItemDefinition> items = new LinkedHashMap<>();
        if (!file.exists()) {
            return new CustomItemRegistry(items);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            return new CustomItemRegistry(items);
        }
        for (String itemId : itemsSection.getKeys(false)) {
            try {
                CustomItemDefinition def = loadItem(itemId,
                        itemsSection.getConfigurationSection(itemId));
                if (def != null) {
                    items.put(itemId, def);
                }
            } catch (RuntimeException e) {
                System.err.println(
                        "[LegacyItems] Errore caricamento item " + itemId + ": "
                                + e.getMessage());
            }
        }
        return new CustomItemRegistry(items);
    }

    private static CustomItemDefinition loadItem(
            String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        boolean enabled = section.getBoolean("enabled", true);
        String material = section.getString("material", "STONE");
        int data = section.getInt("data", 0);
        int amount = Math.max(1, section.getInt("amount", 1));
        int slot = section.getInt("slot", 1);
        if (slot < 1 || slot > 36) {
            System.err.println("[LegacyItems] Slot invalido per " + id + ": " + slot);
            return null;
        }
        String skullOwner = section.getString("skull-owner", "");

        List<CustomItemTrigger> triggers = new ArrayList<>();
        for (String triggerStr : section.getStringList("triggers")) {
            CustomItemTrigger trigger = CustomItemTrigger.fromString(triggerStr);
            if (trigger != null) {
                triggers.add(trigger);
            }
        }

        String worldMode = section.getString("worlds.mode", "ALL");
        List<String> worldValues = section.getStringList("worlds.values");

        boolean permRequired = section.getBoolean("permission.required", false);
        String permNode = section.getString("permission.node", "");

        Map<String, CustomItemLanguage> languages = new LinkedHashMap<>();
        ConfigurationSection langSection = section.getConfigurationSection("languages");
        if (langSection != null) {
            for (String langCode : langSection.getKeys(false)) {
                Map<String, Object> langMap = new LinkedHashMap<>();
                ConfigurationSection singleLang =
                        langSection.getConfigurationSection(langCode);
                if (singleLang == null) {
                    continue;
                }
                for (String key : singleLang.getKeys(false)) {
                    langMap.put(key, singleLang.get(key));
                }
                CustomItemLanguage lang = CustomItemLanguage.fromMap(langMap);
                if (lang != null) {
                    languages.put(langCode, lang);
                }
            }
        }

        ConfigurationSection flagsSection = section.getConfigurationSection("flags");
        CustomItemFlags flags = new CustomItemFlags(
                getBool(flagsSection, "prevent-drop", true),
                getBool(flagsSection, "prevent-move", true),
                getBool(flagsSection, "prevent-swap", true),
                getBool(flagsSection, "prevent-consume", true),
                getBool(flagsSection, "prevent-damage", true),
                getBool(flagsSection, "replace-existing", true),
                getBool(flagsSection, "unique", true));

        Map<String, CustomItemClickActions> actions = new LinkedHashMap<>();
        ConfigurationSection actionsSection = section.getConfigurationSection("actions");
        if (actionsSection != null) {
            for (String actionKey : actionsSection.getKeys(false)) {
                ConfigurationSection clickSection =
                        actionsSection.getConfigurationSection(actionKey);
                if (clickSection == null) {
                    continue;
                }
                int cooldownMillis = clickSection.getInt("cooldown-milliseconds",
                        clickSection.getInt("cooldown.milliseconds", 0));
                boolean cancelEvent = clickSection.getBoolean("cancel-event", true);
                List<CustomItemAction> execute = new ArrayList<>();
                for (Object execObj : clickSection.getList("execute",
                        new ArrayList<>())) {
                    if (execObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> execMap = (Map<String, Object>) execObj;
                        CustomItemAction action = loadAction(execMap, cooldownMillis,
                                cancelEvent);
                        if (action != null) {
                            execute.add(action);
                        }
                    }
                }
                actions.put(actionKey.replace("-", "_"),
                        new CustomItemClickActions(cooldownMillis, cancelEvent, execute));
            }
        }

        Map<String, Integer> enchantments = new LinkedHashMap<>();
        ConfigurationSection enchantSection =
                section.getConfigurationSection("enchantments");
        if (enchantSection != null) {
            for (String enchKey : enchantSection.getKeys(false)) {
                enchantments.put(enchKey, enchantSection.getInt(enchKey, 1));
            }
        }

        List<String> itemFlags = section.getStringList("item-flags");

        return new CustomItemDefinition(id, enabled, material, data, amount, slot,
                skullOwner, triggers, worldMode, worldValues, permRequired, permNode,
                languages, flags, actions, enchantments, itemFlags);
    }

    private static CustomItemAction loadAction(
            Map<String, Object> map, int defaultCooldown, boolean defaultCancel) {
        String typeStr = (String) map.get("type");
        CustomItemActionType type = CustomItemActionType.fromString(typeStr);
        if (type == null) {
            return null;
        }
        Object valueObj = map.get("value");
        String value = null;
        Map<String, String> translatedValues = null;
        if (valueObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> rawMap = (Map<String, Object>) valueObj;
            translatedValues = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                translatedValues.put(entry.getKey(),
                        entry.getValue() != null ? entry.getValue().toString() : "");
            }
        } else if (valueObj != null) {
            value = valueObj.toString();
        }
        return new CustomItemAction(type, value, translatedValues, defaultCooldown,
                defaultCancel);
    }

    private static boolean getBool(ConfigurationSection section,
                                    String key, boolean def) {
        if (section == null) {
            return def;
        }
        return section.getBoolean(key, def);
    }
}
