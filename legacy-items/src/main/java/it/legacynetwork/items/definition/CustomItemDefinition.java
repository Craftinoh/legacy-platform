package it.legacynetwork.items.definition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CustomItemDefinition {
    private final String id;
    private final boolean enabled;
    private final String material;
    private final int data;
    private final int amount;
    private final int slot;
    private final String skullOwner;
    private final List<CustomItemTrigger> triggers;
    private final String worldMode;
    private final List<String> worldValues;
    private final boolean permissionRequired;
    private final String permissionNode;
    private final Map<String, CustomItemLanguage> languages;
    private final CustomItemFlags flags;
    private final Map<String, CustomItemClickActions> actions;
    private final Map<String, Integer> enchantments;
    private final List<String> itemFlags;

    public CustomItemDefinition(String id,
                                 boolean enabled,
                                 String material,
                                 int data,
                                 int amount,
                                 int slot,
                                 String skullOwner,
                                 List<CustomItemTrigger> triggers,
                                 String worldMode,
                                 List<String> worldValues,
                                 boolean permissionRequired,
                                 String permissionNode,
                                 Map<String, CustomItemLanguage> languages,
                                 CustomItemFlags flags,
                                 Map<String, CustomItemClickActions> actions,
                                 Map<String, Integer> enchantments,
                                 List<String> itemFlags) {
        this.id = id;
        this.enabled = enabled;
        this.material = material;
        this.data = data;
        this.amount = amount;
        this.slot = slot;
        this.skullOwner = skullOwner;
        this.triggers = triggers != null
                ? Collections.unmodifiableList(triggers)
                : Collections.emptyList();
        this.worldMode = worldMode != null ? worldMode : "ALL";
        this.worldValues = worldValues != null
                ? Collections.unmodifiableList(worldValues)
                : Collections.emptyList();
        this.permissionRequired = permissionRequired;
        this.permissionNode = permissionNode;
        this.languages = languages != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(languages))
                : Collections.emptyMap();
        this.flags = flags;
        this.actions = actions != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(actions))
                : Collections.emptyMap();
        this.enchantments = enchantments != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(enchantments))
                : Collections.emptyMap();
        this.itemFlags = itemFlags != null
                ? Collections.unmodifiableList(itemFlags)
                : Collections.emptyList();
    }

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getMaterial() {
        return material;
    }

    public int getData() {
        return data;
    }

    public int getAmount() {
        return amount;
    }

    public int getSlot() {
        return slot;
    }

    public String getSkullOwner() {
        return skullOwner;
    }

    public List<CustomItemTrigger> getTriggers() {
        return triggers;
    }

    public String getWorldMode() {
        return worldMode;
    }

    public List<String> getWorldValues() {
        return worldValues;
    }

    public boolean isPermissionRequired() {
        return permissionRequired;
    }

    public String getPermissionNode() {
        return permissionNode;
    }

    public Map<String, CustomItemLanguage> getLanguages() {
        return languages;
    }

    public CustomItemLanguage getLanguage(String code) {
        CustomItemLanguage lang = languages.get(code);
        if (lang == null) {
            lang = languages.get("en");
        }
        return lang;
    }

    public CustomItemFlags getFlags() {
        return flags;
    }

    public Map<String, CustomItemClickActions> getActions() {
        return actions;
    }

    public Map<String, Integer> getEnchantments() {
        return enchantments;
    }

    public List<String> getItemFlags() {
        return itemFlags;
    }

    public boolean isAllowedInWorld(String worldName) {
        if ("ALL".equalsIgnoreCase(worldMode)) {
            return true;
        }
        String normalized = worldName.toLowerCase();
        boolean contains = worldValues.stream()
                .anyMatch(w -> w.toLowerCase().equals(normalized));
        if ("WHITELIST".equalsIgnoreCase(worldMode)) {
            return contains;
        }
        if ("BLACKLIST".equalsIgnoreCase(worldMode)) {
            return !contains;
        }
        return true;
    }
}
