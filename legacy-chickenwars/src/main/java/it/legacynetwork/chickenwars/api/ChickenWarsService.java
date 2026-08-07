package it.legacynetwork.chickenwars.api;

import it.legacynetwork.chickenwars.arena.ArenaDefinition;
import it.legacynetwork.chickenwars.game.Game;
import it.legacynetwork.chickenwars.game.GameTeam;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * API pubblica di ChickenWars, registrata nel {@code ServicesManager} di Bukkit.
 *
 * <p>Esempio d'uso da un altro plugin:</p>
 * <pre>
 * ChickenWarsService service =
 *         Bukkit.getServicesManager().load(ChickenWarsService.class);
 * if (service != null &amp;&amp; service.isPlaying(player)) {
 *     GameTeam team = service.getTeamOf(player);
 * }
 * </pre>
 */
public interface ChickenWarsService {

    /**
     * Elenca tutte le arene configurate, valide o meno.
     */
    Collection<ArenaDefinition> getArenas();

    /**
     * Elenca tutte le partite gestite dal plugin.
     */
    Collection<Game> getGames();

    /**
     * Restituisce la partita associata a un'arena.
     *
     * @return la partita, oppure {@code null} se l'arena non esiste
     */
    Game getGame(String arenaId);

    /**
     * Restituisce la partita che ospita il giocatore indicato.
     *
     * @return la partita, oppure {@code null} se il giocatore non partecipa
     */
    Game getGameOf(UUID playerId);

    /**
     * Indica se il giocatore sta partecipando attivamente a una partita.
     */
    boolean isPlaying(Player player);

    /**
     * Restituisce la squadra del giocatore.
     *
     * @return la squadra, oppure {@code null} se il giocatore non ne ha una
     */
    GameTeam getTeamOf(Player player);

    /**
     * Individua la partita piu' adatta a un ingresso rapido.
     *
     * @return la partita, oppure {@code null} se nessuna e' disponibile
     */
    Game findBestGame();

    /**
     * Versione dell'API, incrementata a ogni modifica non retrocompatibile.
     */
    int getApiVersion();
}
