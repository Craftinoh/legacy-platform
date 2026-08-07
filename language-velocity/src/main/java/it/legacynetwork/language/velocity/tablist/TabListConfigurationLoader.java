package it.legacynetwork.language.velocity.tablist;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TabListConfigurationLoader {

    private TabListConfigurationLoader() {
    }

    public static TabListConfiguration load(File file) {
        return load(file, null);
    }

    /**
     * Loads the external configuration while filling only missing languages
     * from the bundled default. Administrator values always take precedence.
     */
    public static TabListConfiguration load(File file,
                                            InputStream bundledDefaults) {
        Map<String, Object> root = loadYaml(file);
        Map<String, Object> defaults = loadYaml(bundledDefaults);

        if (root == null) {
            root = defaults;
        }
        if (root == null) {
            return disabled();
        }

        boolean enabled = getBool(root, "enabled", true);
        int sendDelay = getInt(root, "send-delay.milliseconds", 250);
        int updateTicks = Math.max(1, getInt(root, "update.ticks", 40));
        boolean debug = getBool(root, "debug", false);
        String fallbackLanguage = getString(root,
                "fallback.language", "en");
        String fallbackServer = getString(root,
                "fallback.server-name", "proxy");

        Map<String, TabListLanguageSection> languages =
                readLanguages(defaults);
        languages.putAll(readLanguages(root));

        return new TabListConfiguration(enabled, sendDelay, updateTicks,
                debug, fallbackLanguage, fallbackServer, languages);
    }

    private static Map<String, TabListLanguageSection> readLanguages(
            Map<String, Object> root) {
        Map<String, TabListLanguageSection> languages =
                new LinkedHashMap<String, TabListLanguageSection>();
        if (root == null) {
            return languages;
        }
        Object rawLanguages = root.get("languages");
        if (!(rawLanguages instanceof Map)) {
            return languages;
        }

        Map<?, ?> langMap = (Map<?, ?>) rawLanguages;
        for (Map.Entry<?, ?> entry : langMap.entrySet()) {
            if (!(entry.getValue() instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> section =
                    (Map<String, Object>) entry.getValue();
            String languageCode = normalizeLanguageKey(entry.getKey());
            List<String> header = readStringList(section, "header");
            List<String> footer = readStringList(section, "footer");
            languages.put(languageCode,
                    new TabListLanguageSection(header, footer));
        }
        return languages;
    }

    private static String normalizeLanguageKey(Object key) {
        // YAML 1.1 parsers may interpret an unquoted "no" key as false.
        if (Boolean.FALSE.equals(key)) {
            return "no";
        }
        return String.valueOf(key).trim().toLowerCase().replace('-', '_');
    }

    private static TabListConfiguration disabled() {
        return new TabListConfiguration(false, 250, 40, false,
                "en", "proxy",
                new LinkedHashMap<String, TabListLanguageSection>());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        try (InputStream input = new FileInputStream(file)) {
            return loadYaml(input);
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml(InputStream input) {
        if (input == null) {
            return null;
        }
        try (InputStream stream = input;
             InputStreamReader reader = new InputStreamReader(
                     stream, StandardCharsets.UTF_8)) {
            Object loaded = new Yaml().load(reader);
            if (loaded instanceof Map) {
                return (Map<String, Object>) loaded;
            }
        } catch (Exception ignored) {
            // Invalid defaults simply do not participate in the overlay.
        }
        return null;
    }

    private static List<String> readStringList(
            Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            List<String> result = new ArrayList<String>();
            for (Object item : (List<?>) value) {
                result.add(String.valueOf(item));
            }
            return result;
        }
        if (value instanceof String) {
            List<String> result = new ArrayList<String>();
            result.add((String) value);
            return result;
        }
        return new ArrayList<String>();
    }

    private static boolean getBool(Map<String, Object> root,
                                   String path, boolean def) {
        Object value = getPath(root, path);
        return value instanceof Boolean ? ((Boolean) value).booleanValue() : def;
    }

    private static int getInt(Map<String, Object> root,
                              String path, int def) {
        Object value = getPath(root, path);
        return value instanceof Number ? ((Number) value).intValue() : def;
    }

    private static String getString(Map<String, Object> root,
                                    String path, String def) {
        Object value = getPath(root, path);
        return value instanceof String ? (String) value : def;
    }

    private static Object getPath(Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Object current = root;
        for (String part : parts) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(part);
        }
        return current;
    }
}
