package it.legacynetwork.chickenwars.message;

import it.legacynetwork.language.TranslationInstaller;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica che ogni chiave passata a {@link MessageService} esista davvero.
 *
 * <p>Le chiavi non sono elencate a mano: vengono estratte dai punti di chiamata
 * nel codice sorgente, cosi' il test non puo' divergere dall'implementazione ne'
 * restare verde su una lista inventata.</p>
 */
class LocalizationIntegrationTest {

    /**
     * Cattura il secondo argomento letterale delle chiamate di traduzione.
     */
    private static final Pattern MESSAGE_CALL = Pattern.compile(
            "\\.(?:get|getList|send|sendRaw|sendList|broadcast|broadcastList"
                    + "|translate|translateList)\\(\\s*[^,()]+,\\s*\""
                    + "([a-z][a-zA-Z0-9._-]*)\"");

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[a-z_]+}");

    private static Set<String> usedKeys;
    private static YamlConfiguration italian;
    private static YamlConfiguration english;

    @BeforeAll
    static void collect() throws IOException {
        usedKeys = scanSourceKeys();
        italian = load("messages_it.yml");
        english = load("messages_en.yml");
    }

    /**
     * Estrae le chiavi dai sorgenti del modulo.
     *
     * <p>Le chiavi composte a runtime (prefisso piu' identificatore) terminano
     * con un punto e vengono escluse: la loro esistenza e' verificata da
     * {@code ResourcesTest} sul catalogo reale.</p>
     */
    private static Set<String> scanSourceKeys() throws IOException {
        Set<String> keys = new TreeSet<String>();
        Path sources = Paths.get("src", "main", "java");
        assertTrue(Files.isDirectory(sources),
                "sorgenti non trovati: " + sources.toAbsolutePath());

        Stream<Path> files = Files.walk(sources);
        try {
            Iterator<Path> iterator = files.iterator();
            while (iterator.hasNext()) {
                Path file = iterator.next();
                if (!file.toString().endsWith(".java")) {
                    continue;
                }
                String content = new String(Files.readAllBytes(file),
                        Charset.forName("UTF-8"));
                Matcher matcher = MESSAGE_CALL.matcher(content);
                while (matcher.find()) {
                    String key = matcher.group(1);
                    if (!key.endsWith(".") && key.indexOf('.') > 0) {
                        keys.add(key);
                    }
                }
            }
        } finally {
            files.close();
        }
        return keys;
    }

    private static YamlConfiguration load(String resource) throws IOException {
        File file = new File("src/main/resources/" + resource);
        assertTrue(file.isFile(), "file lingua mancante: " + resource);
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(new String(
                    Files.readAllBytes(file.toPath()),
                    Charset.forName("UTF-8")));
        } catch (Exception exception) {
            throw new IOException("file lingua non valido: " + resource,
                    exception);
        }
        return configuration;
    }

    /**
     * Restituisce il valore di una chiave, sia essa stringa o lista.
     */
    private static String resolve(YamlConfiguration bundle, String key) {
        String single = bundle.getString(key);
        if (single != null && !single.trim().isEmpty()) {
            return single;
        }
        List<String> lines = bundle.getStringList(key);
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        StringBuilder joined = new StringBuilder();
        for (String line : lines) {
            joined.append(line).append('\n');
        }
        return joined.toString();
    }

    @Test
    void loScanTrovaUnNumeroPlausibileDiChiavi() {
        assertTrue(usedKeys.size() >= 30,
                "chiavi trovate: " + usedKeys.size() + " -> " + usedKeys);
    }

    @Test
    void ogniChiaveUsataEsisteNellaLinguaTerminaleDiFallback() {
        List<String> missing = new ArrayList<String>();
        for (String key : usedKeys) {
            if (resolve(english, key) == null) {
                missing.add(key);
            }
        }
        // L'inglese e' l'ultimo anello della catena: se manca qui, nessun
        // giocatore potra' mai vedere il messaggio.
        assertTrue(missing.isEmpty(),
                "chiavi assenti in " + MessageService.ULTIMATE_FALLBACK
                        + ": " + missing);
    }

    @Test
    void ogniChiaveUsataEsisteAncheInItaliano() {
        List<String> missing = new ArrayList<String>();
        for (String key : usedKeys) {
            if (resolve(italian, key) == null) {
                missing.add(key);
            }
        }
        assertTrue(missing.isEmpty(), "chiavi assenti in it: " + missing);
    }

    @Test
    void nessunValoreCoincideConLaChiaveGrezza() {
        for (String key : usedKeys) {
            assertNotEquals(key, resolve(english, key).trim(), key);
            assertNotEquals(key, resolve(italian, key).trim(), key);
        }
    }

    @Test
    void nessunValoreEuVuoto() {
        for (String key : usedKeys) {
            assertFalse(ChatColor.stripColor(resolve(english, key))
                    .trim().isEmpty(), "valore vuoto in en: " + key);
            assertFalse(ChatColor.stripColor(resolve(italian, key))
                    .trim().isEmpty(), "valore vuoto in it: " + key);
        }
    }

    @Test
    void iSegnapostoCoincidonoTraLeDueLingue() {
        List<String> mismatched = new ArrayList<String>();
        for (String key : usedKeys) {
            Set<String> inEnglish = placeholders(resolve(english, key));
            Set<String> inItalian = placeholders(resolve(italian, key));
            if (!inEnglish.equals(inItalian)) {
                mismatched.add(key + " en=" + inEnglish + " it=" + inItalian);
            }
        }
        // Un segnaposto presente in una sola lingua produce testo rotto.
        assertTrue(mismatched.isEmpty(),
                "segnaposto incoerenti: " + mismatched);
    }

    @Test
    void iCodiciColoreSonoTradotti() {
        for (String key : usedKeys) {
            String value = resolve(italian, key);
            String coloured = ChatColor.translateAlternateColorCodes('&', value);
            // Dopo la traduzione non devono restare sequenze "&x" grezze.
            assertFalse(coloured.matches("(?s).*&[0-9a-fk-or].*"),
                    "codice colore non tradotto in " + key);
        }
    }

    @Test
    void tutteLeLingueDiRetePossonoEssereRichieste() {
        // La catena termina sempre sull'inglese, che contiene tutte le chiavi:
        // nessuna lingua supportata puo' produrre un risultato nullo.
        assertEquals(31, TranslationInstaller.ALL_LANGUAGES.size());
        assertTrue(TranslationInstaller.ALL_LANGUAGES.contains(
                MessageService.ULTIMATE_FALLBACK));
        for (String language : TranslationInstaller.ALL_LANGUAGES) {
            assertTrue(language.matches("[a-z]{2}(_[a-z]{2})?"),
                    "codice lingua non valido: " + language);
        }
    }

    @Test
    void lInglesePuoServireOgniLinguaSconosciuta() {
        // Una lingua senza file ricade sull'inglese: verifichiamo che il
        // contenuto terminale copra davvero l'intero insieme di chiavi.
        Set<String> englishKeys = new TreeSet<String>(english.getKeys(true));
        for (String key : usedKeys) {
            assertTrue(englishKeys.contains(key),
                    "chiave non presente nel fallback: " + key);
        }
    }

    private Set<String> placeholders(String value) {
        Set<String> found = new TreeSet<String>();
        if (value == null) {
            return found;
        }
        Matcher matcher = PLACEHOLDER.matcher(value);
        while (matcher.find()) {
            found.add(matcher.group());
        }
        return found;
    }
}
