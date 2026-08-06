package it.legacynetwork.menu.lang;

import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public final class SkullTextureUtil {

    private SkullTextureUtil() {
    }

    public static void applyTexture(SkullMeta meta, String base64, UUID deterministicUuid) {
        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Constructor<?> profileConstructor = gameProfileClass.getConstructor(UUID.class, String.class);
            Object profile = profileConstructor.newInstance(deterministicUuid, "LegacyLangIcon");

            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Constructor<?> propertyConstructor = propertyClass.getConstructor(String.class, String.class);
            Object property = propertyConstructor.newInstance("textures", base64);

            Method getPropertiesMethod = profile.getClass().getMethod("getProperties");
            Object properties = getPropertiesMethod.invoke(profile);
            Method putMethod = properties.getClass().getMethod("put", Object.class, Object.class);
            putMethod.invoke(properties, "textures", property);

            Method setProfileMethod = meta.getClass().getMethod("setProfile", gameProfileClass);
            setProfileMethod.invoke(meta, profile);
        } catch (Exception ignored) {
        }
    }

    public static boolean isValidTexture(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            String json = new String(decoded, StandardCharsets.UTF_8).toLowerCase();
            return json.contains("\"url\"")
                    && json.contains("https://")
                    && json.contains("\"textures\"")
                    && json.contains("\"skin\"");
        } catch (Exception e) {
            return false;
        }
    }
}
