package it.legacynetwork.items.definition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CustomItemLanguage {
    private final String name;
    private final List<String> lore;

    public CustomItemLanguage(String name, List<String> lore) {
        this.name = name;
        this.lore = lore != null
                ? Collections.unmodifiableList(lore)
                : Collections.emptyList();
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public static CustomItemLanguage fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        String name = (String) map.get("name");
        Object loreObj = map.get("lore");
        List<String> lore = null;
        if (loreObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> rawList = (List<Object>) loreObj;
            java.util.List<String> list = new java.util.ArrayList<>();
            for (Object o : rawList) {
                list.add(String.valueOf(o));
            }
            lore = list;
        }
        return new CustomItemLanguage(name != null ? name : "", lore);
    }
}
