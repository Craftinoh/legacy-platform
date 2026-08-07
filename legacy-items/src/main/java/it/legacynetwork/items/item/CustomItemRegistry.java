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
        return new ArrayList<CustomItemDefinition>(items.values());
    }

    public List<CustomItemDefinition> getByTrigger(CustomItemTrigger trigger) {
        List<CustomItemDefinition> result = new ArrayList<CustomItemDefinition>();
        for (CustomItemDefinition definition : items.values()) {
            if (definition.isEnabled()
                    && definition.getTriggers().contains(trigger)) {
                result.add(definition);
            }
        }
        return result;
    }

    public boolean hasItem(String id) {
        return items.containsKey(id);
    }

    public static CustomItemRegistry load(File file) {
        Map<String, CustomItemDefinition> items =
                new LinkedHashMap<String, CustomItemDefinition>();
        if (!file.exists()) {
            return new CustomItemRegistry(items);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection itemsSection =
                config.getConfigurationSection("items");
        if (itemsSection == null) {
            return new CustomItemRegistry(items);
        }
        for (String itemId : itemsSection.getKeys(false)) {
            try {
                CustomItemDefinition definition = loadItem(itemId,
                        itemsSection.getConfigurationSection(itemId));
                if (definition != null) {
                    items.put(itemId, definition);
                }
            } catch (RuntimeException exception) {
                System.err.println("[LegacyItems] Errore caricamento item "
                        + itemId + ": " + exception.getMessage());
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

        List<CustomItemTrigger> triggers = loadTriggers(section);
        String worldMode = section.getString("worlds.mode", "ALL");
        List<String> worldValues = section.getStringList("worlds.values");

        PermissionData permission = loadPermission(section);
        Map<String, CustomItemLanguage> languages = loadLanguages(section);

        ConfigurationSection flagsSection =
                section.getConfigurationSection("flags");
        CustomItemFlags flags = new CustomItemFlags(
                getBool(flagsSection, "prevent-drop", true),
                getBool(flagsSection, "prevent-move", true),
                getBool(flagsSection, "prevent-swap", true),
                getBool(flagsSection, "prevent-consume", true),
                getBool(flagsSection, "prevent-damage", true),
                getBool(flagsSection, "replace-existing", true),
                getBool(flagsSection, "unique", true));

        Map<String, CustomItemClickActions> actions = loadActions(section);

        Map<String, Integer> enchantments =
                new LinkedHashMap<String, Integer>();
        ConfigurationSection enchantSection =
                section.getConfigurationSection("enchantments");
        if (enchantSection != null) {
            for (String enchantment : enchantSection.getKeys(false)) {
                enchantments.put(enchantment,
                        Integer.valueOf(enchantSection.getInt(enchantment, 1)));
            }
        }

        List<String> itemFlags = section.getStringList("item-flags");

        return new CustomItemDefinition(id, enabled, material, data, amount,
                slot, skullOwner, triggers, worldMode, worldValues,
                permission.required, permission.node, languages, flags,
                actions, enchantments, itemFlags);
    }

    private static List<CustomItemTrigger> loadTriggers(
            ConfigurationSection section) {
        List<CustomItemTrigger> triggers =
                new ArrayList<CustomItemTrigger>();
        for (String triggerText : section.getStringList("triggers")) {
            CustomItemTrigger trigger = CustomItemTrigger.fromString(triggerText);
            if (trigger != null && !triggers.contains(trigger)) {
                triggers.add(trigger);
            }
        }

        // Schema v2 keeps the simple lobby item concise. When no explicit
        // trigger is configured, the item must still be delivered to players.
        if (triggers.isEmpty()) {
            triggers.add(CustomItemTrigger.JOIN);
            triggers.add(CustomItemTrigger.RESPAWN);
            triggers.add(CustomItemTrigger.WORLD_CHANGE);
        }
        return triggers;
    }

    private static PermissionData loadPermission(ConfigurationSection section) {
        Object rawPermission = section.get("permission");
        if (rawPermission instanceof String) {
            String node = ((String) rawPermission).trim();
            return new PermissionData(!node.isEmpty(), node);
        }
        boolean required = section.getBoolean("permission.required", false);
        String node = section.getString("permission.node", "");
        return new PermissionData(required, node != null ? node : "");
    }

    private static Map<String, CustomItemLanguage> loadLanguages(
            ConfigurationSection section) {
        Map<String, CustomItemLanguage> languages =
                new LinkedHashMap<String, CustomItemLanguage>();
        ConfigurationSection languageSection =
                section.getConfigurationSection("translations");
        if (languageSection == null) {
            languageSection = section.getConfigurationSection("languages");
        }

        if (languageSection != null) {
            for (String languageCode : languageSection.getKeys(false)) {
                ConfigurationSection singleLanguage =
                        languageSection.getConfigurationSection(languageCode);
                if (singleLanguage == null) {
                    continue;
                }
                Map<String, Object> languageMap =
                        new LinkedHashMap<String, Object>();
                for (String key : singleLanguage.getKeys(false)) {
                    languageMap.put(key, singleLanguage.get(key));
                }
                CustomItemLanguage language =
                        CustomItemLanguage.fromMap(languageMap);
                if (language != null) {
                    languages.put(languageCode.toLowerCase(), language);
                }
            }
        }

        if (!languages.containsKey("en")) {
            Map<String, Object> legacyEnglish =
                    new LinkedHashMap<String, Object>();
            String legacyName = section.getString("name");
            List<String> legacyLore = section.getStringList("lore");
            if (legacyName != null) {
                legacyEnglish.put("name", legacyName);
            }
            if (legacyLore != null && !legacyLore.isEmpty()) {
                legacyEnglish.put("lore", legacyLore);
            }
            CustomItemLanguage legacy =
                    CustomItemLanguage.fromMap(legacyEnglish);
            if (legacy != null) {
                languages.put("en", legacy);
            }
        }
        return languages;
    }

    private static Map<String, CustomItemClickActions> loadActions(
            ConfigurationSection section) {
        Map<String, CustomItemClickActions> actions =
                new LinkedHashMap<String, CustomItemClickActions>();
        Object rawActions = section.get("actions");

        if (rawActions instanceof List) {
            List<CustomItemAction> parsed = parseSimpleActions((List<?>) rawActions);
            if (!parsed.isEmpty()) {
                actions.put("right_click",
                        new CustomItemClickActions(500, true, parsed));
            }
            return actions;
        }

        ConfigurationSection actionsSection =
                section.getConfigurationSection("actions");
        if (actionsSection == null) {
            return actions;
        }

        for (String actionKey : actionsSection.getKeys(false)) {
            Object actionValue = actionsSection.get(actionKey);
            if (actionValue instanceof List) {
                List<CustomItemAction> parsed =
                        parseSimpleActions((List<?>) actionValue);
                if (!parsed.isEmpty()) {
                    actions.put(normalizeActionKey(actionKey),
                            new CustomItemClickActions(500, true, parsed));
                }
                continue;
            }

            ConfigurationSection clickSection =
                    actionsSection.getConfigurationSection(actionKey);
            if (clickSection == null) {
                continue;
            }
            int cooldownMillis = clickSection.getInt("cooldown-milliseconds",
                    clickSection.getInt("cooldown.milliseconds", 0));
            boolean cancelEvent = clickSection.getBoolean("cancel-event", true);
            List<CustomItemAction> execute =
                    new ArrayList<CustomItemAction>();
            for (Object executeObject : clickSection.getList("execute",
                    new ArrayList<Object>())) {
                if (executeObject instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> executeMap =
                            (Map<String, Object>) executeObject;
                    CustomItemAction action = loadAction(executeMap,
                            cooldownMillis, cancelEvent);
                    if (action != null) {
                        execute.add(action);
                    }
                } else {
                    CustomItemAction action = parseSimpleAction(executeObject);
                    if (action != null) {
                        execute.add(action);
                    }
                }
            }
            actions.put(normalizeActionKey(actionKey),
                    new CustomItemClickActions(cooldownMillis,
                            cancelEvent, execute));
        }
        return actions;
    }

    private static List<CustomItemAction> parseSimpleActions(List<?> values) {
        List<CustomItemAction> actions = new ArrayList<CustomItemAction>();
        for (Object value : values) {
            CustomItemAction action = parseSimpleAction(value);
            if (action != null) {
                actions.add(action);
            }
        }
        return actions;
    }

    private static CustomItemAction parseSimpleAction(Object value) {
        if (!(value instanceof String)) {
            return null;
        }
        String text = ((String) value).trim();
        if (text.isEmpty()) {
            return null;
        }
        int separator = text.indexOf(':');
        String typeText = separator >= 0
                ? text.substring(0, separator) : text;
        String actionValue = separator >= 0
                ? text.substring(separator + 1) : "";
        CustomItemActionType type =
                CustomItemActionType.fromString(typeText.trim());
        if (type == null) {
            return null;
        }
        return new CustomItemAction(type, actionValue.trim(), null, 500, true);
    }

    private static String normalizeActionKey(String key) {
        String normalized = key == null ? "right_click"
                : key.trim().toLowerCase().replace('-', '_');
        if ("right".equals(normalized) || "click".equals(normalized)) {
            return "right_click";
        }
        if ("left".equals(normalized)) {
            return "left_click";
        }
        return normalized;
    }

    private static CustomItemAction loadAction(
            Map<String, Object> map, int defaultCooldown,
            boolean defaultCancel) {
        Object typeObject = map.get("type");
        CustomItemActionType type = CustomItemActionType.fromString(
                typeObject != null ? typeObject.toString() : null);
        if (type == null) {
            return null;
        }
        Object valueObject = map.get("value");
        String value = null;
        Map<String, String> translatedValues = null;
        if (valueObject instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> rawMap = (Map<String, Object>) valueObject;
            translatedValues = new LinkedHashMap<String, String>();
            for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                translatedValues.put(entry.getKey(),
                        entry.getValue() != null
                                ? entry.getValue().toString() : "");
            }
        } else if (valueObject != null) {
            value = valueObject.toString();
        }
        return new CustomItemAction(type, value, translatedValues,
                defaultCooldown, defaultCancel);
    }

    private static boolean getBool(ConfigurationSection section,
                                   String key, boolean defaultValue) {
        if (section == null) {
            return defaultValue;
        }
        return section.getBoolean(key, defaultValue);
    }

    private static final class PermissionData {
        private final boolean required;
        private final String node;

        private PermissionData(boolean required, String node) {
            this.required = required;
            this.node = node;
        }
    }
}
