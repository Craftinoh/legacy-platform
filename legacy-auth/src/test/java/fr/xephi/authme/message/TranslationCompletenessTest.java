package fr.xephi.authme.message;

import fr.xephi.authme.message.locale.SupportedLanguages;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Checks every shipped translation against the English reference.
 * <p>
 * The test fails when a language file is missing, is not valid UTF-8, is not valid YAML, misses a
 * key, defines an unknown key, has an empty value, loses a placeholder, gains a foreign
 * placeholder, or drops a colour code that the English message has.
 * <p>
 * {@code %nl%} is treated as a layout tag rather than a data placeholder: a translation may add or
 * omit a line break without changing the meaning, so it is excluded from the placeholder
 * comparison. Every other {@code %tag} must appear in exactly the same set as in English — their
 * order within the sentence is free.
 */
public class TranslationCompletenessTest {

    private static final File MESSAGES_FOLDER = new File("src/main/resources/messages");
    private static final String REFERENCE_LANGUAGE = "en";

    /** Data placeholders such as %username or %captcha_code; %nl% is handled separately. */
    private static final Pattern PLACEHOLDER = Pattern.compile("%[a-zA-Z_]+");
    /** Legacy colour codes such as &c or &2. */
    private static final Pattern COLOR_CODE = Pattern.compile("&[0-9a-fk-orA-FK-OR]");

    @Test
    public void shouldShipAFileForEverySupportedLanguage() {
        List<String> missing = new ArrayList<>();
        for (String language : SupportedLanguages.getCodes()) {
            if (!messageFile(language).exists()) {
                missing.add(language);
            }
        }
        assertTrue("No message file shipped for: " + missing, missing.isEmpty());
    }

    @Test
    public void shouldHaveCompleteAndConsistentTranslations() {
        Map<String, String> reference = readMessages(REFERENCE_LANGUAGE);
        assertTrue("The English reference file should not be empty", reference.size() > 50);

        List<String> problems = new ArrayList<>();
        for (String language : SupportedLanguages.getCodes()) {
            if (REFERENCE_LANGUAGE.equals(language)) {
                continue;
            }
            problems.addAll(checkLanguage(language, reference));
        }

        if (!problems.isEmpty()) {
            fail("Found " + problems.size() + " translation problem(s):"
                + System.lineSeparator() + String.join(System.lineSeparator(), problems));
        }
    }

    private List<String> checkLanguage(String language, Map<String, String> reference) {
        List<String> problems = new ArrayList<>();
        Map<String, String> messages = readMessages(language);

        for (Map.Entry<String, String> entry : reference.entrySet()) {
            String key = entry.getKey();
            String englishText = entry.getValue();
            String translated = messages.get(key);

            if (translated == null) {
                problems.add(language + ": missing key '" + key + "'");
                continue;
            }
            if (translated.trim().isEmpty()) {
                problems.add(language + ": empty value for key '" + key + "'");
                continue;
            }

            Set<String> expectedTags = placeholders(englishText);
            Set<String> actualTags = placeholders(translated);
            if (!expectedTags.equals(actualTags)) {
                Set<String> lost = new LinkedHashSet<>(expectedTags);
                lost.removeAll(actualTags);
                Set<String> added = new LinkedHashSet<>(actualTags);
                added.removeAll(expectedTags);
                problems.add(language + ": placeholders differ for '" + key + "'"
                    + (lost.isEmpty() ? "" : " lost=" + lost)
                    + (added.isEmpty() ? "" : " unknown=" + added));
            }

            if (COLOR_CODE.matcher(englishText).find() && !COLOR_CODE.matcher(translated).find()) {
                problems.add(language + ": colour code dropped for '" + key + "'");
            }
        }

        for (String key : messages.keySet()) {
            if (!reference.containsKey(key)) {
                problems.add(language + ": unknown key '" + key + "' (not present in English)");
            }
        }
        return problems;
    }

    /**
     * Returns the data placeholders of a message. {@code %nl%} is stripped first: it is a line
     * break, and translations may legitimately place line breaks differently.
     *
     * @param text the message text
     * @return the set of placeholders used
     */
    private static Set<String> placeholders(String text) {
        Set<String> tags = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(text.replace("%nl%", " "));
        while (matcher.find()) {
            tags.add(matcher.group());
        }
        return tags;
    }

    /**
     * Reads a message file, failing the test if it is not valid UTF-8 or not valid YAML.
     *
     * @param language the language to read
     * @return the messages, flattened to dotted keys
     */
    private static Map<String, String> readMessages(String language) {
        File file = messageFile(language);
        if (!file.exists()) {
            fail("No message file for language '" + language + "' at " + file);
        }

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + file, e);
        }
        // Strict decoding: any byte sequence that is not valid UTF-8 fails the test.
        try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(bytes));
        } catch (CharacterCodingException e) {
            fail("Message file for '" + language + "' is not valid UTF-8: " + file);
        }

        YamlConfiguration configuration = new YamlConfiguration();
        try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()),
            StandardCharsets.UTF_8)) {
            configuration.load(reader);
        } catch (Exception e) {
            fail("Message file for '" + language + "' is not valid YAML: " + e.getMessage());
        }

        Map<String, String> messages = new TreeMap<>();
        for (String key : configuration.getKeys(true)) {
            if (!configuration.isConfigurationSection(key)) {
                Object value = configuration.get(key);
                messages.put(key, value == null ? "" : value.toString());
            }
        }
        return messages;
    }

    private static File messageFile(String language) {
        return new File(MESSAGES_FOLDER, "messages_" + language + ".yml");
    }
}
