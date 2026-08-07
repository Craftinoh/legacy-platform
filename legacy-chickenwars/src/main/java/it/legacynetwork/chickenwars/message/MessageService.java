package it.legacynetwork.chickenwars.message;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageProvider;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Traduzioni di ChickenWars, con risoluzione della lingua tramite
 * {@link PlayerLanguageProvider} quando il servizio di rete e' disponibile.
 *
 * <p>In assenza del servizio lingua viene usata la lingua di fallback indicata
 * in {@code config.yml}, quindi il plugin resta pienamente funzionante da
 * solo.</p>
 */
public final class MessageService {

    private final JavaPlugin plugin;

    private volatile YamlConfiguration italian = new YamlConfiguration();
    private volatile YamlConfiguration english = new YamlConfiguration();
    private volatile String fallbackLanguage = "it";
    private volatile String prefix = "";
    private volatile PlayerLanguageProvider languageProvider;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Ricarica i file di traduzione.
     *
     * @param italianFile      nome del file italiano
     * @param englishFile      nome del file inglese
     * @param fallbackLanguage lingua usata quando quella del giocatore manca
     * @return {@code true} se entrambi i file sono stati caricati
     */
    public boolean reload(String italianFile, String englishFile,
                          String fallbackLanguage) {
        try {
            YamlConfiguration newItalian =
                    loadStrict(new File(plugin.getDataFolder(), italianFile));
            YamlConfiguration newEnglish =
                    loadStrict(new File(plugin.getDataFolder(), englishFile));

            this.italian = newItalian;
            this.english = newEnglish;
            this.fallbackLanguage = normalize(fallbackLanguage);
            this.prefix = ChatColor.translateAlternateColorCodes('&',
                    messagesFor(this.fallbackLanguage).getString("prefix", ""));
            this.languageProvider = null;
            return true;
        } catch (IOException exception) {
            plugin.getLogger().warning("Impossibile caricare i messaggi: "
                    + exception.getMessage());
            return false;
        } catch (InvalidConfigurationException exception) {
            plugin.getLogger().warning("File messaggi non valido: "
                    + exception.getMessage());
            return false;
        }
    }

    private YamlConfiguration loadStrict(File file)
            throws IOException, InvalidConfigurationException {
        if (!file.isFile()) {
            throw new IOException("File non trovato: " + file.getName());
        }
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.load(file);
        return configuration;
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
        String language = resolveLanguage(sender);
        String raw = findRaw(language, key);
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
        String language = resolveLanguage(sender);
        List<String> raw = messagesFor(language).getStringList(key);
        if (raw == null || raw.isEmpty()) {
            raw = messagesFor(fallbackLanguage).getStringList(key);
        }
        List<String> result = new ArrayList<String>();
        if (raw == null) {
            return result;
        }
        for (String line : raw) {
            result.add(colorize(applyReplacements(line, replacements)));
        }
        return result;
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
            for (String line : getList(player, key, replacements)) {
                player.sendMessage(line);
            }
        }
    }

    private String findRaw(String language, String key) {
        String value = messagesFor(language).getString(key);
        if (value == null || value.trim().isEmpty()) {
            value = messagesFor(fallbackLanguage).getString(key);
        }
        return value;
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

    private YamlConfiguration messagesFor(String language) {
        return "it".equalsIgnoreCase(language) ? italian : english;
    }

    /**
     * Risolve il codice lingua del destinatario.
     *
     * @return {@code "it"} oppure {@code "en"}, mai nullo
     */
    public String getLanguage(CommandSender sender) {
        return resolveLanguage(sender);
    }

    private String resolveLanguage(CommandSender sender) {
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
            provider = Bukkit.getServicesManager().load(PlayerLanguageProvider.class);
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
            return "it";
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        return "it".equals(normalized) ? "it" : "en";
    }

    public String getPrefix() {
        return prefix;
    }

    public void close() {
        languageProvider = null;
    }
}
