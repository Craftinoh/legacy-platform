package it.legacynetwork.chickenwars.upgrade;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Stato autorevole degli upgrade di una squadra.
 *
 * <p>Esiste una sola istanza per squadra e per partita: e' condivisa da tutti i
 * membri e sopravvive a morte, respawn e reconnect. Le sessioni dei singoli
 * giocatori non ne conservano copie.</p>
 *
 * <p>Le mutazioni sono sincronizzate e la riserva del livello avviene prima di
 * qualunque addebito: due membri che acquistano nello stesso istante non
 * possono ottenere lo stesso livello due volte.</p>
 */
public final class TeamUpgradeState {

    private final String teamId;
    private final Map<TeamUpgradeType, Integer> levels =
            new EnumMap<TeamUpgradeType, Integer>(TeamUpgradeType.class);
    private final Map<RoyalUpgradeType, Integer> royalLevels =
            new EnumMap<RoyalUpgradeType, Integer>(RoyalUpgradeType.class);
    private final Deque<String> trapQueue = new ArrayDeque<String>();

    public TeamUpgradeState(String teamId) {
        if (teamId == null || teamId.trim().isEmpty()) {
            throw new IllegalArgumentException("ID squadra mancante");
        }
        this.teamId = teamId;
    }

    public String getTeamId() {
        return teamId;
    }

    // ------------------------------------------------------------------
    // Upgrade di squadra
    // ------------------------------------------------------------------

    /**
     * Livello attualmente posseduto, zero se mai acquistato.
     */
    public synchronized int getLevel(TeamUpgradeType type) {
        Integer level = levels.get(type);
        return level == null ? 0 : level.intValue();
    }

    /**
     * Riserva il livello successivo, se consentito dal massimo.
     *
     * <p>Va invocata prima dell'addebito: chi non ottiene la riserva non deve
     * pagare nulla.</p>
     *
     * @param maximumLevel livello massimo previsto dalla configurazione
     * @return il livello riservato, oppure {@code 0} se non disponibile
     */
    public synchronized int reserveNextLevel(TeamUpgradeType type,
                                             int maximumLevel) {
        if (type == null) {
            return 0;
        }
        int current = getLevel(type);
        if (current >= maximumLevel) {
            return 0;
        }
        int next = current + 1;
        levels.put(type, Integer.valueOf(next));
        return next;
    }

    /**
     * Annulla una riserva quando l'addebito o l'applicazione falliscono.
     */
    public synchronized void rollbackLevel(TeamUpgradeType type,
                                           int reservedLevel) {
        if (type == null || getLevel(type) != reservedLevel) {
            return;
        }
        int previous = reservedLevel - 1;
        if (previous <= 0) {
            levels.remove(type);
        } else {
            levels.put(type, Integer.valueOf(previous));
        }
    }

    /**
     * Indica se l'upgrade ha raggiunto il livello massimo.
     */
    public synchronized boolean isMaxed(TeamUpgradeType type,
                                        int maximumLevel) {
        return getLevel(type) >= maximumLevel;
    }

    // ------------------------------------------------------------------
    // Upgrade della Gallina Reale
    // ------------------------------------------------------------------

    public synchronized int getRoyalLevel(RoyalUpgradeType type) {
        Integer level = royalLevels.get(type);
        return level == null ? 0 : level.intValue();
    }

    /**
     * Riserva il livello successivo di un potenziamento reale.
     *
     * @return il livello riservato, oppure {@code 0} se non disponibile
     */
    public synchronized int reserveNextRoyalLevel(RoyalUpgradeType type,
                                                  int maximumLevel) {
        if (type == null) {
            return 0;
        }
        int current = getRoyalLevel(type);
        if (current >= maximumLevel) {
            return 0;
        }
        int next = current + 1;
        royalLevels.put(type, Integer.valueOf(next));
        return next;
    }

    public synchronized void rollbackRoyalLevel(RoyalUpgradeType type,
                                                 int reservedLevel) {
        if (type == null || getRoyalLevel(type) != reservedLevel) {
            return;
        }
        int previous = reservedLevel - 1;
        if (previous <= 0) {
            royalLevels.remove(type);
        } else {
            royalLevels.put(type, Integer.valueOf(previous));
        }
    }

    // ------------------------------------------------------------------
    // Coda trappole
    // ------------------------------------------------------------------

    /**
     * Accoda una trappola rispettando il limite configurato.
     *
     * @return la posizione occupata a partire da zero, oppure {@code -1}
     */
    public synchronized int queueTrap(String trapId, int maximum) {
        if (trapId == null || trapId.trim().isEmpty()
                || trapQueue.size() >= maximum) {
            return -1;
        }
        int position = trapQueue.size();
        trapQueue.addLast(trapId.trim().toLowerCase());
        return position;
    }

    /**
     * Rimuove l'ultima trappola accodata, per annullare un acquisto fallito.
     */
    public synchronized void removeLastTrap() {
        trapQueue.pollLast();
    }

    /**
     * Estrae la prossima trappola da attivare, in ordine di inserimento.
     *
     * @return l'identificatore, oppure {@code null} se la coda e' vuota
     */
    public synchronized String pollTrap() {
        return trapQueue.pollFirst();
    }

    /**
     * Legge la prossima trappola senza estrarla.
     *
     * @return l'identificatore, oppure {@code null} se la coda e' vuota
     */
    public synchronized String peekTrap() {
        return trapQueue.peekFirst();
    }

    /**
     * Rimette una trappola in testa alla coda.
     *
     * <p>Serve ad annullare un'estrazione la cui applicazione e' stata
     * interamente rifiutata: la posizione FIFO originale viene conservata,
     * quindi il giocatore non perde la trappola pagata.</p>
     */
    public synchronized void requeueFirst(String trapId) {
        if (trapId != null && !trapId.trim().isEmpty()) {
            trapQueue.addFirst(trapId.trim().toLowerCase());
        }
    }

    /**
     * Copia della coda, dalla prossima all'ultima.
     */
    public synchronized List<String> getTrapQueue() {
        return new ArrayList<String>(trapQueue);
    }

    public synchronized int getTrapCount() {
        return trapQueue.size();
    }

    public synchronized boolean hasTraps() {
        return !trapQueue.isEmpty();
    }

    /**
     * Azzera l'intero stato, a fine partita o allo scaricamento dell'arena.
     */
    public synchronized void clear() {
        levels.clear();
        royalLevels.clear();
        trapQueue.clear();
    }

    @Override
    public String toString() {
        return "TeamUpgradeState{" + teamId + ", " + levels
                + ", royal=" + royalLevels + ", trappole=" + trapQueue + '}';
    }
}
