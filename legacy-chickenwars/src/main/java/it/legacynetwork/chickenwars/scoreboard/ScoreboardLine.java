package it.legacynetwork.chickenwars.scoreboard;

import org.bukkit.ChatColor;

/**
 * Riga di scoreboard suddivisa in prefisso, voce univoca e suffisso.
 *
 * <p>Su 1.8 una riga puo' contenere al massimo 16 caratteri di prefisso e 16 di
 * suffisso; la voce centrale serve solo come chiave e viene resa invisibile
 * usando un codice colore diverso per ogni riga.</p>
 */
public final class ScoreboardLine {

    private static final int SEGMENT_LIMIT = 16;

    private final String prefix;
    private final String entry;
    private final String suffix;

    private ScoreboardLine(String prefix, String entry, String suffix) {
        this.prefix = prefix;
        this.entry = entry;
        this.suffix = suffix;
    }

    /**
     * Costruisce la riga distribuendo il testo tra prefisso e suffisso.
     *
     * @param index indice della riga, usato per generare una voce univoca
     * @param text  testo gia' colorato
     * @return la riga risultante
     */
    public static ScoreboardLine of(int index, String text) {
        String value = text == null ? "" : text;
        String uniqueEntry = uniqueEntry(index);

        if (value.length() <= SEGMENT_LIMIT) {
            return new ScoreboardLine(value, uniqueEntry, "");
        }

        int split = SEGMENT_LIMIT;
        // Evita di spezzare un codice colore a meta'.
        if (value.charAt(split - 1) == ChatColor.COLOR_CHAR) {
            split--;
        }
        String prefix = value.substring(0, split);
        String remainder = ChatColor.getLastColors(prefix) + value.substring(split);
        if (remainder.length() > SEGMENT_LIMIT) {
            remainder = remainder.substring(0, SEGMENT_LIMIT);
        }
        return new ScoreboardLine(prefix, uniqueEntry, remainder);
    }

    /**
     * Voce invisibile e univoca, ottenuta combinando i codici colore legacy.
     */
    private static String uniqueEntry(int index) {
        ChatColor[] colors = ChatColor.values();
        ChatColor first = colors[index % colors.length];
        ChatColor second = colors[(index / colors.length) % colors.length];
        return first.toString() + second.toString() + ChatColor.RESET;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getEntry() {
        return entry;
    }

    public String getSuffix() {
        return suffix;
    }
}
