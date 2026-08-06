package it.legacynetwork.menu.lang;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlagTextureService {
    private final File pluginDataFolder;
    private Map<String, ItemStack> iconCache;
    private ItemStack fallbackIcon;

    public FlagTextureService(File pluginDataFolder) {
        this.pluginDataFolder = pluginDataFolder;
        reload();
    }

    public void reload() {
        Map<String, ItemStack> cache = new HashMap<String, ItemStack>();

        // Empty or invalid textures still produce a selectable 1.8-compatible
        // player head instead of filling the language menu with paper items.
        fallbackIcon = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta fallbackMeta = fallbackIcon.getItemMeta();
        if (fallbackMeta != null) {
            fallbackMeta.setDisplayName("\u00a7fLanguage");
            fallbackIcon.setItemMeta(fallbackMeta);
        }

        File file = new File(pluginDataFolder, "flag-textures.yml");
        YamlConfiguration config = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();

        if (config.isConfigurationSection("languages")) {
            for (String code : config.getConfigurationSection("languages")
                    .getKeys(false)) {
                String texture = config.getString(
                        "languages." + code + ".texture", "");
                if (texture == null || texture.trim().isEmpty()
                        || !SkullTextureUtil.isValidTexture(texture)) {
                    continue;
                }
                try {
                    ItemStack baseItem = new ItemStack(
                            Material.SKULL_ITEM, 1, (short) 3);
                    SkullMeta meta = (SkullMeta) baseItem.getItemMeta();
                    if (meta != null) {
                        UUID uuid = UUID.nameUUIDFromBytes(
                                ("lang-" + code)
                                        .getBytes(StandardCharsets.UTF_8));
                        SkullTextureUtil.applyTexture(meta, texture, uuid);
                        baseItem.setItemMeta(meta);
                        cache.put(code, baseItem);
                    }
                } catch (RuntimeException ignored) {
                    // The generic skull remains available as a safe fallback.
                }
            }
        }

        this.iconCache = Collections.unmodifiableMap(cache);
    }

    public ItemStack getBaseIcon(String languageCode) {
        ItemStack base = iconCache.get(languageCode);
        return (base != null ? base : fallbackIcon).clone();
    }
}
