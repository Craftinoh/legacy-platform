package it.legacynetwork.chickenwars.message;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageProvider;
import it.legacynetwork.language.TranslationInstaller;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Traduzioni di ChickenWars su tutte le lingue di Network Language.
 *
 * <p>La lingua del giocatore viene risolta tramite {@link PlayerLanguageProvider}
 * e usata cosi' com'e': non viene collassata su italiano o inglese. Quando una
 * chiave manca nella lingua richiesta si applica la catena di fallback
 * lingua del giocatore, lingua configurata, inglese.</p>
 *
 * <p>I file vengono cercati sia nella cartella dati del plugin
 * ({@code messages_xx.yml}) sia nella sottocartella {@code translations/},
 * cosi' da restare compatibili con l'installatore centrale.</p>
 */
public final class MessageService {

    /** Ultimo anello della catena di fallback. */
    public static final String ULTIMATE_FALLBACK = "en";

    private static final String TRANSLATIONS_DIR = "translations";

    private final JavaPlugin plugin;

    private volatile Map<String, YamlConfiguration> bundles =
            Collections.emptyMap();
    private volatile String fallbackLanguage = ULTIMATE_FALLBACK;
    private volatile String prefix = "";
    private volatile PlayerLanguageProvider languageProvider;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Ricarica tutte le traduzioni disponibili su disco.
     *
     * @param fallbackLanguage codice lingua usato quando quella del giocatore
     *                         non contiene la chiave richiesta
     * @return {@code true} se almeno una lingua e' stata caricata
     */
    public boolean reload(String fallbackLanguage) {
        Map<String, YamlConfiguration> loaded =
                new LinkedHashMap<String, YamlConfiguration>();

        for (String code : TranslationInstaller.ALL_LANGUAGES) {
            YamlConfiguration configuration = loadFirstAvailable(code);
            if (configuration != null) {
                loaded.put(code, configuration);
            }
        }

        if (loaded.isEmpty()) {
            plugin.getLogger().warning(
                    "Nessun file di traduzione trovato: messaggi non disponibili.");
            return false;
        }

        String normalizedFallback = normalize(fallbackLanguage);
        if (!loaded.containsKey(normalizedFallback)) {
            normalizedFallback = loaded.containsKey(ULTIMATE_FALLBACK)
                    ? ULTIMATE_FALLBACK : loaded.keySet().iterator().next();
        }

        this.bundles = Collections.unmodifiableMap(loaded);
        this.fallbackLanguage = normalizedFallback;
        this.prefix = ChatColor.translateAlternateColorCodes('&',
                rawLookup(normalizedFallback, "prefix", ""));
        this.languageProvider = null;

        plugin.getLogger().info("Traduzioni caricate: " + loaded.size()
                + " lingue (fallback " + normalizedFallback + ").");
        return true;
    }

    /**
     * Cerca il file di una lingua prima in {@code translations/}, poi nella
     * cartella dati, mantenendo compatibilita' con le installazioni esistenti.
     */
    private YamlConfiguration loadFirstAvailable(String code) {
        String fileName = "messages_" + code + ".yml";
        YamlConfiguration fromTranslations = loadQuietly(
                new File(new File(plugin.getDataFolder(), TRANSLATIONS_DIR),
                        fileName));
        if (fromTranslations != null) {
            return fromTranslations;
        }
        return loadQuietly(new File(plugin.getDataFolder(), fileName));
    }

    private YamlConfiguration loadQuietly(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(file);
            return configuration;
        } catch (IOException exception) {
            plugin.getLogger().warning("Impossibile leggere " + file.getName()
                    + ": " + exception.getMessage());
        } catch (InvalidConfigurationException exception) {
            plugin.getLogger().warning("File traduzione non valido "
                    + file.getName() + ": " + exception.getMessage());
        }
        return null;
    }

    /**
     * Traduce una chiave per il destinatario indicato.
     *
     * @param sender       destinatario, usato per risolvere la lingua
     * @param key          chiave del messaggio
     * @param replacements coppie {@code segnaposto, valore}
     * @return il testo tradotto e colorato, mai nullo
     */
    public String get(CommandSender sender, String key, String... replacements) {
        return translate(getLanguage(sender), key, replacements);
    }

    /**
     * Traduce una chiave in una lingua specifica.
     *
     * @param languageCode codice lingua richiesto
     * @param key          chiave del messaggio
     * @param replacements coppie {@code segnaposto, valore}
     * @return il testo tradotto e colorato, mai nullo
     */
    public String translate(String languageCode, String key,
                            String... replacements) {
        String raw = findRaw(languageCode, key);
        if (raw == null) {
            return ChatColor.RED + "messaggio mancante: " + key;
        }
        return colorize(applyReplacements(raw, replacements));
    }

    /**
     * Traduce una lista di righe, utile per broadcast e riepiloghi.
     *
     * @return le righe tradotte, vuote se la chiave non esiste
     */
    public List<String> getList(CommandSender sender, String key,
                                String... replacements) {
        return translateList(getLanguage(sender), key, replacements);
    }

    /**
     * Traduce una lista di righe in una lingua specifica.
     */
    public List<String> translateList(String languageCode, String key,
                                      String... replacements) {
        List<String> raw = findRawList(languageCode, key);
        List<String> result = new ArrayList<String>();
        for (String line : raw) {
            result.add(colorize(applyReplacements(line, replacements)));
        }
        return result;
    }

    /**
     * Indica se una chiave esiste nella lingua indicata o nei suoi fallback.
     */
    public boolean hasKey(String languageCode, String key) {
        return findRaw(languageCode, key) != null
                || !findRawList(languageCode, key).isEmpty();
    }

    /**
     * Invia un messaggio tradotto, preceduto dal prefisso configurato.
     */
    public void send(CommandSender sender, String key, String... replacements) {
        if (sender == null) {
            return;
        }
        String message = get(sender, key, replacements);
        if (message.trim().isEmpty()) {
            return;
        }
        sender.sendMessage(prefix + message);
    }

    /**
     * Invia un messaggio tradotto senza prefisso.
     */
    public void sendRaw(CommandSender sender, String key, String... replacements) {
        if (sender == null) {
            return;
        }
        sender.sendMessage(get(sender, key, replacements));
    }

    /**
     * Invia piu' righe tradotte, senza prefisso, al destinatario indicato.
     */
    public void sendList(CommandSender sender, String key,
                         String... replacements) {
        if (sender == null) {
            return;
        }
        for (String line : getList(sender, key, replacements)) {
            sender.sendMessage(line);
        }
    }

    /**
     * Invia lo stesso messaggio a piu' giocatori, traducendolo per ciascuno.
     */
    public void broadcast(Collection<? extends Player> targets, String key,
                          String... replacements) {
        if (targets == null) {
            return;
        }
        for (Player player : targets) {
            if (player != null && player.isOnline()) {
                send(player, key, replacements);
            }
        }
    }

    /**
     * Invia piu' righe tradotte, senza prefisso, a un gruppo di giocatori.
     */
    public void broadcastList(Collection<? extends Player> targets, String key,
                              String... replacements) {
        if (targets == null) {
            return;
        }
        for (Player player : targets) {
            if (player == null || !player.isOnline()) {
                continue;
            }
            sendList(player, key, replacements);
        }
    }

    /**
     * Cerca il testo grezzo percorrendo la catena di fallback.
     *
     * @return il testo, oppure {@code null} se assente in tutta la catena
     */
    private String findRaw(String languageCode, String key) {
        for (String candidate : fallbackChain(languageCode)) {
            YamlConfiguration bundle = bundles.get(candidate);
            if (bundle == null) {
                continue;
            }
            String value = bundle.getString(key);
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private List<String> findRawList(String languageCode, String key) {
        for (String candidate : fallbackChain(languageCode)) {
            YamlConfiguration bundle = bundles.get(candidate);
            if (bundle == null) {
                continue;
            }
            List<String> value = bundle.getStringList(key);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return Collections.emptyList();
    }

    private String rawLookup(String languageCode, String key, String fallback) {
        String value = findRaw(languageCode, key);
        return value == null ? fallback : value;
    }

    /**
     * Ordine di ricerca: lingua richiesta, lingua configurata, inglese.
     */
    private List<String> fallbackChain(String languageCode) {
        List<String> chain = new ArrayList<String>(3);
        String requested = normalize(languageCode);
        chain.add(requested);
        if (!chain.contains(fallbackLanguage)) {
            chain.add(fallbackLanguage);
        }
        if (!chain.contains(ULTIMATE_FALLBACK)) {
            chain.add(ULTIMATE_FALLBACK);
        }
        return chain;
    }

    private String applyReplacements(String raw, String... replacements) {
        String result = raw == null ? "" : raw;
        if (replacements == null) {
            return result;
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String placeholder = replacements[i];
            String value = replacements[i + 1];
            if (placeholder != null) {
                result = result.replace(placeholder, value == null ? "" : value);
            }
        }
        return result;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Risolve il codice lingua del destinatario.
     *
     * <p>Restituisce il codice reale della lingua scelta dal giocatore, senza
     * ridurlo a un insieme di due lingue.</p>
     *
     * @return un codice lingua non nullo
     */
    public String getLanguage(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return fallbackLanguage;
        }
        PlayerLanguageProvider provider = resolveLanguageProvider();
        if (provider == null) {
            return fallbackLanguage;
        }
        try {
            UUID playerId = ((Player) sender).getUniqueId();
            Language language = provider.getLanguage(playerId);
            return language == null
                    ? fallbackLanguage : normalize(language.getCode());
        } catch (RuntimeException exception) {
            return fallbackLanguage;
        } catch (LinkageError error) {
            languageProvider = null;
            return fallbackLanguage;
        }
    }

    private PlayerLanguageProvider resolveLanguageProvider() {
        PlayerLanguageProvider provider = languageProvider;
        if (provider != null) {
            return provider;
        }
        try {
            provider = Bukkit.getServicesManager().load(
                    PlayerLanguageProvider.class);
            languageProvider = provider;
            return provider;
        } catch (RuntimeException exception) {
            return null;
        } catch (LinkageError error) {
            return null;
        }
    }

    private String normalize(String language) {
        if (language == null) {
            return ULTIMATE_FALLBACK;
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_');
        return normalized.isEmpty() ? ULTIMATE_FALLBACK : normalized;
    }

    /**
     * Codici lingua effettivamente caricati.
     */
    public Collection<String> getLoadedLanguages() {
        return bundles.keySet();
    }

    public String getFallbackLanguage() {
        return fallbackLanguage;
    }

    public String getPrefix() {
        return prefix;
    }

    public void close() {
        languageProvider = null;
    }
}
