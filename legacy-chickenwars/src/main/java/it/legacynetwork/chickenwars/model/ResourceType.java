package it.legacynetwork.chickenwars.model;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.Locale;

/**
 * Risorse raccoglibili dai generatori.
 *
 * <p>Ogni risorsa e' anche una valuta accettata dallo shop.</p>
 */
public enum ResourceType {

    IRON(Material.IRON_INGOT, ChatColor.WHITE, "Ferro", "Iron"),
    GOLD(Material.GOLD_INGOT, ChatColor.GOLD, "Oro", "Gold"),
    DIAMOND(Material.DIAMOND, ChatColor.AQUA, "Diamanti", "Diamond"),
    EMERALD(Material.EMERALD, ChatColor.GREEN, "Smeraldi", "Emerald"),
    FEATHER(Material.FEATHER, ChatColor.YELLOW, "Piume", "Feather");

    private final Material material;
    private final ChatColor color;
    private final String italianName;
    private final String englishName;

    ResourceType(Material material, ChatColor color,
                 String italianName, String englishName) {
        this.material = material;
        this.color = color;
        this.italianName = italianName;
        this.englishName = englishName;
    }

    /**
     * Converte un nome di risorsa, ignorando maiuscole e spazi.
     *
     * @return la risorsa, oppure {@code null} se il nome non corrisponde
     */
    public static ResourceType fromString(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (ResourceType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Risolve la risorsa corrispondente a un materiale raccolto.
     *
     * @return la risorsa, oppure {@code null} se il materiale non e' una valuta
     */
    public static ResourceType fromMaterial(Material material) {
        if (material == null) {
            return null;
        }
        for (ResourceType type : values()) {
            if (type.material == material) {
                return type;
            }
        }
        return null;
    }

    public Material getMaterial() {
        return material;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getItalianName() {
        return italianName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getDisplayName(String language) {
        return "it".equalsIgnoreCase(language) ? italianName : englishName;
    }
}
