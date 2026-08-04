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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MenuFileLoader {

    private MenuFileLoader() {
    }

    public static MenuDefinition load(File file) {
        Yaml yaml = new Yaml();
        Map<String, Object> root;
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {
            root = (Map<String, Object>) yaml.load(reader);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load menu file: "
                    + file.getName(), e);
        }
        if (root == null) {
            throw new RuntimeException("Empty menu file: " + file.getName());
        }

        String id = (String) root.get("id");
        boolean enabled = Boolean.TRUE.equals(root.get("enabled"));
        int size = getInt(root, "size", 9);

        if (size <= 0 || size % 9 != 0) {
            throw new RuntimeException("Invalid menu size " + size
                    + " in " + file.getName() + ". Must be >0 and multiple of 9.");
        }

        Map<String, String> title = new LinkedHashMap<String, String>();
        Map<String, Object> languages = getMap(root, "languages");
        if (languages != null) {
            for (Map.Entry<String, Object> langEntry : languages.entrySet()) {
                Map<String, Object> langData = castMap(langEntry.getValue());
                if (langData != null && langData.get("title") instanceof String) {
                    title.put(langEntry.getKey(), (String) langData.get("title"));
                }
            }
        }

        Map<Integer, MenuItem> items = new LinkedHashMap<Integer, MenuItem>();
        Map<String, Object> itemsRaw = getMap(root, "items");
        if (itemsRaw != null) {
            for (Map.Entry<String, Object> itemEntry : itemsRaw.entrySet()) {
                Map<String, Object> itemData = castMap(itemEntry.getValue());
                if (itemData == null) {
                    continue;
                }

                String material = (String) itemData.getOrDefault("material", "STONE");
                int data = getInt(itemData, "data", 0);
                int amount = getInt(itemData, "amount", 1);
                int slot = getInt(itemData, "slot", 1);

                Map<String, String> itemName = new LinkedHashMap<String, String>();
                Map<String, List<String>> itemLore = new LinkedHashMap<String, List<String>>();
                Map<String, Object> itemLanguages = getMap(itemData, "languages");
                if (itemLanguages != null) {
                    for (Map.Entry<String, Object> langEntry : itemLanguages.entrySet()) {
                        Map<String, Object> langData = castMap(langEntry.getValue());
                        if (langData == null) {
                            continue;
                        }
                        if (langData.get("name") instanceof String) {
                            itemName.put(langEntry.getKey(), (String) langData.get("name"));
                        }
                        if (langData.get("lore") instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> loreLines = (List<String>) langData.get("lore");
                            itemLore.put(langEntry.getKey(), loreLines);
                        }
                    }
                }

                Map<String, List<MenuItemAction>> actions = new LinkedHashMap<String, List<MenuItemAction>>();
                Map<String, Object> actionsRaw = getMap(itemData, "actions");
                if (actionsRaw != null) {
                    for (Map.Entry<String, Object> actionEntry : actionsRaw.entrySet()) {
                        Object value = actionEntry.getValue();
                        List<Map<String, Object>> actionList;
                        if (value instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> raw = (List<Map<String, Object>>) value;
                            actionList = raw;
                        } else if (value instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> single = (Map<String, Object>) value;
                            actionList = new ArrayList<Map<String, Object>>();
                            actionList.add(single);
                        } else {
                            continue;
                        }
                        List<MenuItemAction> parsedActions = new ArrayList<MenuItemAction>();
                        for (Map<String, Object> actionData : actionList) {
                            String type = actionData.get("type") != null
                                    ? actionData.get("type").toString() : "";
                            String val = actionData.get("value") != null
                                    ? actionData.get("value").toString() : "";
                            parsedActions.add(new MenuItemAction(type, val));
                        }
                        actions.put(actionEntry.getKey(), parsedActions);
                    }
                }

                items.put(slot, new MenuItem(material, data, amount, slot,
                        itemName, itemLore, actions));
            }
        }

        return new MenuDefinition(id, enabled, size, title, items);
    }

    private static int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }
}
