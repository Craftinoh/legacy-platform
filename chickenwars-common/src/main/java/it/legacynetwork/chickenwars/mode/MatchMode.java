package it.legacynetwork.chickenwars.mode;

import java.util.Locale;

/**
 * Modalita' ufficiali di ChickenWars.
 *
 * <p>DUEL e' una modalita' di allenamento: usa il profilo economico di SOLO,
 * ma non produce statistiche o ricompense permanenti.</p>
 */
public enum MatchMode {

    DUEL("duel"),
    SOLO("solo"),
    DOUBLES("doubles"),
    TRIO("trio");

    private final String key;

    MatchMode(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static MatchMode fromString(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if ("1v1".equals(normalized) || "duel_1v1".equals(normalized)
                || "training".equals(normalized)) {
            return DUEL;
        }
        if ("1s".equals(normalized) || "solos".equals(normalized)) {
            return SOLO;
        }
        if ("2v2".equals(normalized) || "double".equals(normalized)
                || "2s".equals(normalized)) {
            return DOUBLES;
        }
        if ("3v3".equals(normalized) || "triples".equals(normalized)
                || "3s".equals(normalized)) {
            return TRIO;
        }
        for (MatchMode mode : values()) {
            if (mode.key.equals(normalized) || mode.name().equalsIgnoreCase(raw)) {
                return mode;
            }
        }
        return null;
    }

    /**
     * Deduce la modalita' per le vecchie arene che non possiedono ancora la
     * chiave {@code arena.mode}.
     */
    public static MatchMode infer(int teamCount, int playersPerTeam) {
        int safePlayersPerTeam = Math.max(1, playersPerTeam);
        if (safePlayersPerTeam >= 3) {
            return TRIO;
        }
        if (safePlayersPerTeam == 2) {
            return DOUBLES;
        }
        return teamCount > 0 && teamCount <= 2 ? DUEL : SOLO;
    }
}
