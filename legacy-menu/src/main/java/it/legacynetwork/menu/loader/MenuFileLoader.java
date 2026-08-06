package it.legacynetwork.menu.loader;

import it.legacynetwork.menu.model.MenuDefinition;
import it.legacynetwork.menu.model.MenuItem;
import it.legacynetwork.menu.model.MenuItemAction;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MenuFileLoader {

    private MenuFileLoader() {
    }

    public static MenuDefinition load(File file) {
        Yaml yaml = new Yaml();
        Map<?, ?> root;
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {
            Object loaded = yaml.load(reader);
            if (!(loaded instanceof Map)) {
                throw new RuntimeException("Empty or invalid menu file: " + file.getName());
            }
            root = (Map<?, ?>) loaded;
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load menu file: "
                    + file.getName(), exception);
        }

        String id = stringValue(root.get("id"));
        if (id == null || id.trim().isEmpty()) {
            id = stripExtension(file.getName());
        }

        Object enabledValue = root.get("enabled");
        boolean enabled = !(enabledValue instanceof Boolean)
                || ((Boolean) enabledValue).booleanValue();
        int size = getInt(root, "size", 9);

        if (size <= 0 || size > 54 || size % 9 != 0) {
            throw new RuntimeException("Invalid menu size " + size
                    + " in " + file.getName()
                    + ". Must be between 9 and 54 and a multiple of 9.");
        }

        Map<String, String> titles = loadTitles(root);
        Map<Integer, MenuItem> items = new LinkedHashMap<Integer, MenuItem>();
        Map<?, ?> itemsRaw = getMap(root, "items");
        if (itemsRaw != null) {
            for (Map.Entry<?, ?> itemEntry : itemsRaw.entrySet()) {
                Map<?, ?> itemData = castMap(itemEntry.getValue());
                if (itemData == null) {
                    continue;
                }

                String material = stringValue(itemData.get("material"));
                if (material == null || material.trim().isEmpty()) {
                    material = "STONE";
                }
                int data = getInt(itemData, "data", 0);
                int amount = Math.max(1, getInt(itemData, "amount", 1));
                int slot = getInt(itemData, "slot", 1);
                if (slot < 1 || slot > size) {
                    throw new RuntimeException("Invalid slot " + slot + " for item "
                            + yamlKey(itemEntry.getKey()) + " in " + file.getName());
                }

                Map<String, String> itemNames = new LinkedHashMap<String, String>();
                Map<String, List<String>> itemLores =
                        new LinkedHashMap<String, List<String>>();
                loadItemTranslations(itemData, itemNames, itemLores);

                Map<String, List<MenuItemAction>> actions = loadActions(itemData);
                items.put(Integer.valueOf(slot), new MenuItem(material, data, amount,
                        slot, itemNames, itemLores, actions));
            }
        }

        return new MenuDefinition(id, enabled, size, titles, items);
    }

    private static Map<String, String> loadTitles(Map<?, ?> root) {
        Map<String, String> titles = new LinkedHashMap<String, String>();

        Map<?, ?> titlesSection = getMap(root, "titles");
        if (titlesSection != null) {
            for (Map.Entry<?, ?> entry : titlesSection.entrySet()) {
                String value = stringValue(entry.getValue());
                if (value != null) {
                    titles.put(yamlKey(entry.getKey()), value);
                }
            }
        }

        if (titles.isEmpty()) {
            Map<?, ?> legacyLanguages = getMap(root, "languages");
            if (legacyLanguages != null) {
                for (Map.Entry<?, ?> langEntry : legacyLanguages.entrySet()) {
                    Map<?, ?> langData = castMap(langEntry.getValue());
                    if (langData != null) {
                        String value = stringValue(langData.get("title"));
                        if (value != null) {
                            titles.put(yamlKey(langEntry.getKey()), value);
                        }
                    }
                }
            }
        }

        if (titles.isEmpty()) {
            String legacyTitle = stringValue(root.get("title"));
            if (legacyTitle != null) {
                titles.put("en", legacyTitle);
            }
        }
        if (!titles.containsKey("en")) {
            titles.put("en", "");
        }
        return titles;
    }

    private static void loadItemTranslations(
            Map<?, ?> itemData,
            Map<String, String> names,
            Map<String, List<String>> lores) {
        Map<?, ?> translations = getMap(itemData, "translations");
        if (translations == null) {
            translations = getMap(itemData, "languages");
        }

        if (translations != null) {
            for (Map.Entry<?, ?> langEntry : translations.entrySet()) {
                Map<?, ?> langData = castMap(langEntry.getValue());
                if (langData == null) {
                    continue;
                }
                String languageCode = yamlKey(langEntry.getKey());
                String name = stringValue(langData.get("name"));
                if (name != null) {
                    names.put(languageCode, name);
                }
                List<String> lore = stringList(langData.get("lore"));
                if (lore != null) {
                    lores.put(languageCode, lore);
                }
            }
        }

        if (!names.containsKey("en")) {
            String legacyName = stringValue(itemData.get("name"));
            if (legacyName != null) {
                names.put("en", legacyName);
            }
        }
        if (!lores.containsKey("en")) {
            List<String> legacyLore = stringList(itemData.get("lore"));
            if (legacyLore != null) {
                lores.put("en", legacyLore);
            }
        }
    }

    private static Map<String, List<MenuItemAction>> loadActions(
            Map<?, ?> itemData) {
        Map<String, List<MenuItemAction>> actions =
                new LinkedHashMap<String, List<MenuItemAction>>();
        Object rawActions = itemData.get("actions");

        if (rawActions instanceof List) {
            List<MenuItemAction> parsed = parseActionList((List<?>) rawActions);
            if (!parsed.isEmpty()) {
                actions.put("CLICK", parsed);
            }
            return actions;
        }

        Map<?, ?> actionMap = castMap(rawActions);
        if (actionMap == null) {
            return actions;
        }

        for (Map.Entry<?, ?> actionEntry : actionMap.entrySet()) {
            List<MenuItemAction> parsed = new ArrayList<MenuItemAction>();
            Object value = actionEntry.getValue();
            if (value instanceof List) {
                parsed.addAll(parseActionList((List<?>) value));
            } else {
                MenuItemAction single = parseAction(value);
                if (single != null) {
                    parsed.add(single);
                }
            }
            if (!parsed.isEmpty()) {
                actions.put(normalizeClickKey(yamlKey(actionEntry.getKey())), parsed);
            }
        }
        return actions;
    }

    private static List<MenuItemAction> parseActionList(List<?> values) {
        List<MenuItemAction> parsed = new ArrayList<MenuItemAction>();
        for (Object value : values) {
            MenuItemAction action = parseAction(value);
            if (action != null) {
                parsed.add(action);
            }
        }
        return parsed;
    }

    private static MenuItemAction parseAction(Object raw) {
        if (raw instanceof String) {
            String text = ((String) raw).trim();
            if (text.isEmpty()) {
                return null;
            }
            int separator = text.indexOf(':');
            String type = separator >= 0 ? text.substring(0, separator) : text;
            String value = separator >= 0 ? text.substring(separator + 1) : "";
            return new MenuItemAction(type.trim(), value.trim());
        }

        Map<?, ?> map = castMap(raw);
        if (map == null) {
            return null;
        }
        String type = stringValue(map.get("type"));
        if (type == null || type.trim().isEmpty()) {
            return null;
        }
        String value = stringValue(map.get("value"));
        return new MenuItemAction(type.trim(), value != null ? value : "");
    }

    private static String normalizeClickKey(String key) {
        if (key == null) {
            return "CLICK";
        }
        String normalized = key.trim().toUpperCase().replace('-', '_');
        if ("LEFT_CLICK".equals(normalized)) {
            return "LEFT";
        }
        if ("RIGHT_CLICK".equals(normalized)) {
            return "RIGHT";
        }
        return normalized;
    }

    private static int getInt(Map<?, ?> map, String key,
                              int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static Map<?, ?> getMap(Map<?, ?> map, String key) {
        return castMap(map.get(key));
    }

    private static Map<?, ?> castMap(Object value) {
        if (value instanceof Map) {
            return (Map<?, ?>) value;
        }
        return null;
    }

    private static String yamlKey(Object key) {
        // SnakeYAML's YAML 1.1 resolver interprets the unquoted key "no"
        // as Boolean.FALSE. It is a supported language code here, so restore
        // the intended key instead of throwing ClassCastException.
        if (Boolean.FALSE.equals(key)) {
            return "no";
        }
        if (Boolean.TRUE.equals(key)) {
            return "yes";
        }
        return key == null ? "" : key.toString();
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List)) {
            return null;
        }
        List<String> result = new ArrayList<String>();
        for (Object entry : (List<?>) value) {
            result.add(entry == null ? "" : entry.toString());
        }
        return result;
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
