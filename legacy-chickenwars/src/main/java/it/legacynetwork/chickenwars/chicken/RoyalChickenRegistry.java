package it.legacynetwork.chickenwars.chicken;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Registro autorevole delle entita' Gallina Reale.
 *
 * <p>Associa l'UUID dell'entita' all'arena e alla squadra proprietaria: e' il
 * solo modo consentito per riconoscere una Gallina Reale. Nome, nome
 * visualizzato, prossimita' e tipo di entita' non sono identificatori
 * affidabili.</p>
 */
public final class RoyalChickenRegistry {

    /**
     * Voce del registro: arena e squadra proprietarie di un'entita'.
     */
    public static final class Entry {

        private final UUID entityId;
        private final String arenaId;
        private final String teamId;

        Entry(UUID entityId, String arenaId, String teamId) {
            this.entityId = entityId;
            this.arenaId = arenaId;
            this.teamId = teamId;
        }

        public UUID getEntityId() {
            return entityId;
        }

        public String getArenaId() {
            return arenaId;
        }

        public String getTeamId() {
            return teamId;
        }

        /**
         * Indica se la voce appartiene all'arena indicata.
         */
        public boolean belongsTo(String otherArenaId) {
            return arenaId.equalsIgnoreCase(String.valueOf(otherArenaId));
        }

        @Override
        public String toString() {
            return "Entry{" + entityId + " -> " + arenaId + '/' + teamId + '}';
        }
    }

    private final Map<UUID, Entry> byEntity = new LinkedHashMap<UUID, Entry>();
    private final Map<String, UUID> byTeam = new LinkedHashMap<String, UUID>();

    private static String teamKey(String arenaId, String teamId) {
        return String.valueOf(arenaId).toLowerCase(Locale.ROOT) + '/'
                + String.valueOf(teamId).toLowerCase(Locale.ROOT);
    }

    /**
     * Registra l'entita' di una squadra, sostituendo la precedente.
     *
     * <p>La registrazione e' idempotente rispetto alla squadra: se la squadra
     * aveva gia' una Gallina, la voce vecchia viene rimossa, quindi non possono
     * mai esistere due entita' registrate per la stessa squadra.</p>
     *
     * @return la voce registrata, oppure {@code null} se i dati sono incompleti
     */
    public synchronized Entry register(UUID entityId, String arenaId,
                                       String teamId) {
        if (entityId == null || arenaId == null || teamId == null) {
            return null;
        }
        String key = teamKey(arenaId, teamId);
        UUID previous = byTeam.get(key);
        if (previous != null && !previous.equals(entityId)) {
            byEntity.remove(previous);
        }
        Entry entry = new Entry(entityId, arenaId, teamId);
        byEntity.put(entityId, entry);
        byTeam.put(key, entityId);
        return entry;
    }

    /**
     * Voce associata a un'entita'.
     *
     * @return la voce, oppure {@code null} se l'entita' non e' una Gallina
     */
    public synchronized Entry lookup(UUID entityId) {
        return entityId == null ? null : byEntity.get(entityId);
    }

    /**
     * Entita' registrata per una squadra.
     *
     * @return l'UUID, oppure {@code null} se la squadra non ne ha
     */
    public synchronized UUID getEntity(String arenaId, String teamId) {
        if (arenaId == null || teamId == null) {
            return null;
        }
        return byTeam.get(teamKey(arenaId, teamId));
    }

    /**
     * Indica se l'entita' e' registrata come Gallina Reale.
     */
    public synchronized boolean isRoyalChicken(UUID entityId) {
        return entityId != null && byEntity.containsKey(entityId);
    }

    /**
     * Rimuove la voce di un'entita'.
     *
     * @return {@code true} se l'entita' era registrata
     */
    public synchronized boolean unregister(UUID entityId) {
        if (entityId == null) {
            return false;
        }
        Entry removed = byEntity.remove(entityId);
        if (removed == null) {
            return false;
        }
        String key = teamKey(removed.getArenaId(), removed.getTeamId());
        if (entityId.equals(byTeam.get(key))) {
            byTeam.remove(key);
        }
        return true;
    }

    /**
     * Rimuove tutte le voci di un'arena, al termine o allo scaricamento.
     *
     * @return il numero di voci rimosse
     */
    public synchronized int clearArena(String arenaId) {
        if (arenaId == null) {
            return 0;
        }
        int removed = 0;
        for (Entry entry : new ArrayList<Entry>(byEntity.values())) {
            if (entry.belongsTo(arenaId)) {
                byEntity.remove(entry.getEntityId());
                byTeam.remove(teamKey(entry.getArenaId(), entry.getTeamId()));
                removed++;
            }
        }
        return removed;
    }

    /**
     * Azzera l'intero registro, allo spegnimento del plugin.
     */
    public synchronized void clearAll() {
        byEntity.clear();
        byTeam.clear();
    }

    public synchronized int size() {
        return byEntity.size();
    }
}
