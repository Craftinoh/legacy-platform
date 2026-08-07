package it.legacynetwork.chickenwars.arena;

import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameServices;
import it.legacynetwork.chickenwars.model.ArenaState;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Registro delle arene e delle partite attive.
 *
 * <p>Ogni arena valida possiede una sola partita, ricreata dopo ogni
 * conclusione. Le arene incomplete restano nello stato {@code SETUP} e non sono
 * accessibili ai giocatori.</p>
 */
public final class ArenaManager {

    private final GameServices services;
    private final File arenaFolder;
    private final Logger logger;

    private final Map<String, ArenaDefinition> definitions =
            new LinkedHashMap<String, ArenaDefinition>();
    private final Map<String, Game> games = new LinkedHashMap<String, Game>();

    public ArenaManager(GameServices services, File arenaFolder, Logger logger) {
        this.services = services;
        this.arenaFolder = arenaFolder;
        this.logger = logger;
    }

    /**
     * Carica tutte le arene dalla cartella configurata, sostituendo le
     * precedenti.
     *
     * <p>Le partite in corso vengono chiuse: il metodo va usato solo all'avvio o
     * durante un reload esplicito.</p>
     *
     * @return il numero di arene caricate
     */
    public int loadAll() {
        shutdown();
        definitions.clear();
        games.clear();

        if (!arenaFolder.isDirectory() && !arenaFolder.mkdirs()) {
            logger.warning("Impossibile creare la cartella arenas.");
            return 0;
        }

        for (ArenaDefinition definition :
                ArenaConfigLoader.loadAll(arenaFolder, logger)) {
            register(definition);
        }
        return definitions.size();
    }

    /**
     * Registra una definizione e crea la partita corrispondente.
     */
    public void register(ArenaDefinition definition) {
        if (definition == null) {
            return;
        }
        definitions.put(definition.getId(), definition);

        List<String> missing = definition.findMissing();
        if (!missing.isEmpty()) {
            definition.setEnabled(false);
            logger.warning("Arena " + definition.getId()
                    + " incompleta: " + missing.get(0)
                    + (missing.size() > 1 ? " (+" + (missing.size() - 1)
                    + " altri problemi)" : "") + ".");
        }

        Game game = new Game(services, definition);
        game.setState(definition.isEnabled()
                ? ArenaState.WAITING
                : (missing.isEmpty() ? ArenaState.DISABLED : ArenaState.SETUP));
        games.put(definition.getId(), game);
    }

    /**
     * Crea una nuova arena vuota e la salva su file.
     *
     * @return la definizione creata, oppure {@code null} se l'ID esiste gia'
     */
    public ArenaDefinition create(String arenaId) {
        if (arenaId == null || arenaId.trim().isEmpty()) {
            return null;
        }
        String normalized = arenaId.trim().toLowerCase(Locale.ROOT);
        if (definitions.containsKey(normalized)) {
            return null;
        }
        ArenaDefinition definition = new ArenaDefinition(normalized);
        if (!save(definition)) {
            return null;
        }
        register(definition);
        return definition;
    }

    /**
     * Elimina un'arena, la relativa partita e il file su disco.
     *
     * @return {@code true} se l'arena esisteva ed e' stata rimossa
     */
    public boolean delete(String arenaId) {
        ArenaDefinition definition = getDefinition(arenaId);
        if (definition == null) {
            return false;
        }
        Game game = games.remove(definition.getId());
        if (game != null) {
            game.cleanup();
        }
        definitions.remove(definition.getId());

        File file = new File(arenaFolder, definition.getId() + ".yml");
        return !file.exists() || file.delete();
    }

    /**
     * Salva su file la definizione indicata.
     */
    public boolean save(ArenaDefinition definition) {
        if (definition == null) {
            return false;
        }
        return ArenaConfigLoader.save(
                new File(arenaFolder, definition.getId() + ".yml"),
                definition, logger);
    }

    /**
     * Ricrea la partita di un'arena, per applicare modifiche di configurazione.
     *
     * @return la nuova partita, oppure {@code null} se l'arena non esiste
     */
    public Game rebuildGame(String arenaId) {
        ArenaDefinition definition = getDefinition(arenaId);
        if (definition == null) {
            return null;
        }
        Game previous = games.get(definition.getId());
        if (previous != null) {
            previous.cleanup();
        }
        Game game = new Game(services, definition);
        game.setState(definition.isEnabled() ? ArenaState.WAITING : ArenaState.SETUP);
        games.put(definition.getId(), game);
        return game;
    }

    /**
     * Individua la partita che sta ospitando il giocatore indicato.
     *
     * @return la partita, oppure {@code null} se il giocatore non gioca
     */
    public Game getGameOf(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        for (Game game : games.values()) {
            if (game.contains(playerId)) {
                return game;
            }
        }
        return null;
    }

    public Game getGameOf(Player player) {
        return player == null ? null : getGameOf(player.getUniqueId());
    }

    /**
     * Individua la partita piu' adatta a un ingresso rapido.
     *
     * <p>Viene preferita l'arena piu' popolata tra quelle che accettano ancora
     * giocatori, per far partire prima le partite quasi complete.</p>
     *
     * @return la partita scelta, oppure {@code null} se nessuna e' disponibile
     */
    public Game findBestGame() {
        Game best = null;
        for (Game game : games.values()) {
            if (!game.canJoin()) {
                continue;
            }
            if (best == null || game.getPlayerCount() > best.getPlayerCount()) {
                best = game;
            }
        }
        return best;
    }

    public Game getGame(String arenaId) {
        return arenaId == null
                ? null : games.get(arenaId.trim().toLowerCase(Locale.ROOT));
    }

    public ArenaDefinition getDefinition(String arenaId) {
        return arenaId == null
                ? null : definitions.get(arenaId.trim().toLowerCase(Locale.ROOT));
    }

    public Collection<ArenaDefinition> getDefinitions() {
        return definitions.values();
    }

    public Collection<Game> getGames() {
        return games.values();
    }

    /**
     * Elenca gli ID delle arene, utile per il completamento dei comandi.
     */
    public List<String> getArenaIds() {
        return new ArrayList<String>(definitions.keySet());
    }

    /**
     * Fa avanzare tutte le partite di un tick.
     */
    public void tickAll() {
        for (Game game : new ArrayList<Game>(games.values())) {
            try {
                game.tick();
            } catch (RuntimeException exception) {
                logger.warning("Errore nel ciclo dell'arena "
                        + game.getDefinition().getId() + ": "
                        + exception.getMessage());
            }
        }
    }

    /**
     * Chiude tutte le partite, rimuovendo entita' e ripristinando le mappe.
     */
    public void shutdown() {
        for (Game game : new ArrayList<Game>(games.values())) {
            try {
                for (Player player : game.getOnlinePlayers()) {
                    game.leave(player, true);
                }
                game.cleanup();
                game.getRestore().restore(game.getDefinition());
            } catch (RuntimeException exception) {
                logger.warning("Errore durante la chiusura dell'arena "
                        + game.getDefinition().getId() + ": "
                        + exception.getMessage());
            }
        }
    }
}
