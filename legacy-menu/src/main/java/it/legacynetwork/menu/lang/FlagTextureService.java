package it.legacynetwork.menu.lang;

import it.legacynetwork.language.Language;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class FlagTextureService {
    private final File pluginDataFolder;
    private Map<String, ItemStack> iconCache =
            Collections.emptyMap();
    private Map<String, String> countryNames =
            Collections.emptyMap();
    private ItemStack fallbackIcon;

    public FlagTextureService(File pluginDataFolder) {
        this.pluginDataFolder = pluginDataFolder;
        reload();
    }

    public void reload() {
        Map<String, ItemStack> cache =
                new HashMap<String, ItemStack>();
        Map<String, String> countries =
                new HashMap<String, String>();

        fallbackIcon = new ItemStack(
                Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta fallbackMeta = fallbackIcon.getItemMeta();
        if (fallbackMeta != null) {
            fallbackMeta.setDisplayName("§fLanguage");
            fallbackIcon.setItemMeta(fallbackMeta);
        }

        YamlConfiguration bundled = loadBundledConfiguration();
        File externalFile = new File(
                pluginDataFolder, "flag-textures.yml");
        YamlConfiguration external = externalFile.isFile()
                ? YamlConfiguration.loadConfiguration(externalFile)
                : new YamlConfiguration();

        for (Language language : Language.values()) {
            String code = normalize(language.getCode());
            String path = "languages." + code;

            String country = firstNonBlank(
                    external.getString(path + ".country"),
                    bundled.getString(path + ".country"),
                    language.getEnglishName());
            countries.put(code, country);

            String externalTexture = external.getString(
                    path + ".texture", "");
            String bundledTexture = bundled.getString(
                    path + ".texture", "");
            String texture = SkullTextureUtil.isValidTexture(
                    externalTexture)
                    ? externalTexture.trim()
                    : bundledTexture == null
                    ? ""
                    : bundledTexture.trim();

            if (!SkullTextureUtil.isValidTexture(texture)) {
                continue;
            }

            try {
                ItemStack baseItem = new ItemStack(
                        Material.SKULL_ITEM, 1, (short) 3);
                SkullMeta meta =
                        (SkullMeta) baseItem.getItemMeta();
                if (meta == null) {
                    continue;
                }
                UUID uuid = UUID.nameUUIDFromBytes(
                        ("legacy-language-flag-" + code)
                                .getBytes(StandardCharsets.UTF_8));
                SkullTextureUtil.applyTexture(
                        meta, texture, uuid);
                baseItem.setItemMeta(meta);
                cache.put(code, baseItem);
            } catch (RuntimeException ignored) {
                // A normal player head remains available as safe fallback.
            } catch (LinkageError ignored) {
                // Authlib may differ on unsupported server builds.
            }
        }

        this.iconCache = Collections.unmodifiableMap(cache);
        this.countryNames = Collections.unmodifiableMap(countries);
    }

    public ItemStack getBaseIcon(String languageCode) {
        ItemStack base = iconCache.get(normalize(languageCode));
        return (base != null ? base : fallbackIcon).clone();
    }

    public String getCountryName(String languageCode) {
        String country = countryNames.get(normalize(languageCode));
        return country == null || country.trim().isEmpty()
                ? "-"
                : country;
    }

    public boolean hasCustomTexture(String languageCode) {
        return iconCache.containsKey(normalize(languageCode));
    }

    private YamlConfiguration loadBundledConfiguration() {
        InputStream input = FlagTextureService.class
                .getClassLoader()
                .getResourceAsStream("flag-textures.yml");
        if (input == null) {
            return new YamlConfiguration();
        }
        try (InputStreamReader reader = new InputStreamReader(
                input, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception ignored) {
            return new YamlConfiguration();
        }
    }

    private String firstNonBlank(String first,
                                 String second,
                                 String fallback) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }
        return fallback == null ? "-" : fallback;
    }

    private String normalize(String code) {
        if (code == null) {
            return "en";
        }
        return code.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_');
    }
}
