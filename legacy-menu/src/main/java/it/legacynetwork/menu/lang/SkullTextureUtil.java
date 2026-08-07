package it.legacynetwork.menu.lang;

import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkullTextureUtil {
    private static final Pattern TEXTURE_URL = Pattern.compile(
            "\\\"url\\\"\\s*:\\s*\\\"(https?://textures\\.minecraft\\.net/texture/[a-f0-9]{32,128})\\\"");

    private SkullTextureUtil() {
    }

    public static void applyTexture(SkullMeta meta,
                                    String base64,
                                    UUID deterministicUuid) {
        if (meta == null || !isValidTexture(base64)) {
            return;
        }
        try {
            Class<?> gameProfileClass = Class.forName(
                    "com.mojang.authlib.GameProfile");
            Constructor<?> profileConstructor =
                    gameProfileClass.getConstructor(
                            UUID.class, String.class);
            Object profile = profileConstructor.newInstance(
                    deterministicUuid, "LegacyLangIcon");

            Class<?> propertyClass = Class.forName(
                    "com.mojang.authlib.properties.Property");
            Constructor<?> propertyConstructor =
                    propertyClass.getConstructor(
                            String.class, String.class);
            Object property = propertyConstructor.newInstance(
                    "textures", base64);

            Method getPropertiesMethod = profile.getClass()
                    .getMethod("getProperties");
            Object properties = getPropertiesMethod.invoke(profile);
            Method putMethod = properties.getClass()
                    .getMethod("put", Object.class, Object.class);
            putMethod.invoke(properties, "textures", property);

            if (!applyWithMethod(meta, profile, gameProfileClass)) {
                applyWithField(meta, profile);
            }
        } catch (ReflectiveOperationException ignored) {
            // The caller will still receive a normal selectable player head.
        } catch (LinkageError ignored) {
            // Authlib is unavailable or incompatible on this server build.
        }
    }

    private static boolean applyWithMethod(SkullMeta meta,
                                           Object profile,
                                           Class<?> gameProfileClass) {
        Class<?> type = meta.getClass();
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(
                        "setProfile", gameProfileClass);
                method.setAccessible(true);
                method.invoke(meta, profile);
                return true;
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return false;
    }

    private static void applyWithField(SkullMeta meta,
                                       Object profile)
            throws ReflectiveOperationException {
        Class<?> type = meta.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("profile");
                field.setAccessible(true);
                field.set(meta, profile);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException("profile");
    }

    public static boolean isValidTexture(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(
                    base64.trim());
            String json = new String(
                    decoded, StandardCharsets.UTF_8)
                    .toLowerCase(Locale.ROOT);
            if (!json.contains("\"textures\"")
                    || !json.contains("\"skin\"")) {
                return false;
            }
            Matcher matcher = TEXTURE_URL.matcher(json);
            return matcher.find();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
