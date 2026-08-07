package it.legacynetwork.chickenwars.model;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.ChatColor;

import java.util.Locale;

/**
 * Colori squadra supportati, con la corrispondenza verso chat, lana e armature.
 *
 * <p>Tutti i valori usati sono disponibili su Spigot 1.8.8.</p>
 */
public enum TeamColor {

    RED(ChatColor.RED, DyeColor.RED, Color.RED, "Rosso"),
    BLUE(ChatColor.BLUE, DyeColor.BLUE, Color.BLUE, "Blu"),
    GREEN(ChatColor.DARK_GREEN, DyeColor.GREEN, Color.GREEN, "Verde"),
    YELLOW(ChatColor.YELLOW, DyeColor.YELLOW, Color.YELLOW, "Giallo"),
    AQUA(ChatColor.AQUA, DyeColor.LIGHT_BLUE, Color.AQUA, "Aqua"),
    WHITE(ChatColor.WHITE, DyeColor.WHITE, Color.WHITE, "Bianco"),
    PINK(ChatColor.LIGHT_PURPLE, DyeColor.PINK, Color.fromRGB(255, 105, 180), "Rosa"),
    GRAY(ChatColor.DARK_GRAY, DyeColor.GRAY, Color.GRAY, "Grigio"),
    ORANGE(ChatColor.GOLD, DyeColor.ORANGE, Color.ORANGE, "Arancione"),
    PURPLE(ChatColor.DARK_PURPLE, DyeColor.PURPLE, Color.PURPLE, "Viola"),
    BLACK(ChatColor.BLACK, DyeColor.BLACK, Color.BLACK, "Nero"),
    LIME(ChatColor.GREEN, DyeColor.LIME, Color.LIME, "Lime");

    private final ChatColor chatColor;
    private final DyeColor dyeColor;
    private final Color armorColor;
    private final String italianName;

    TeamColor(ChatColor chatColor, DyeColor dyeColor, Color armorColor,
              String italianName) {
        this.chatColor = chatColor;
        this.dyeColor = dyeColor;
        this.armorColor = armorColor;
        this.italianName = italianName;
    }

    /**
     * Converte un nome di colore, ignorando maiuscole e spazi.
     *
     * @return il colore, oppure {@code null} se il nome non corrisponde
     */
    public static TeamColor fromString(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (TeamColor color : values()) {
            if (color.name().equals(normalized)) {
                return color;
            }
        }
        return null;
    }

    public ChatColor getChatColor() {
        return chatColor;
    }

    public DyeColor getDyeColor() {
        return dyeColor;
    }

    public Color getArmorColor() {
        return armorColor;
    }

    public String getItalianName() {
        return italianName;
    }

    /**
     * Dato usato per lana, vetro colorato e argilla colorata su 1.8.
     */
    public byte getWoolData() {
        return dyeColor.getWoolData();
    }

    /**
     * Iniziale mostrata nella scoreboard compatta.
     */
    public String getInitial() {
        return italianName.substring(0, 1).toUpperCase(Locale.ROOT);
    }
}
