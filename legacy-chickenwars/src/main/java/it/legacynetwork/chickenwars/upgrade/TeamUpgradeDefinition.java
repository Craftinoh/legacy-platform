package it.legacynetwork.chickenwars.upgrade;

import it.legacynetwork.chickenwars.shop.ItemCost;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Definizione configurabile di un upgrade di squadra.
 *
 * <p>I livelli sono ordinati e contigui a partire da uno: il loader rifiuta le
 * sequenze incoerenti, cosi' il servizio puo' assumere che il livello
 * successivo sia sempre {@code corrente + 1}.</p>
 */
public final class TeamUpgradeDefinition {

    private final String id;
    private final TeamUpgradeType type;
    private final Material icon;
    private final byte iconData;
    private final int slot;
    private final boolean enabled;
    private final PotionEffectType effect;
    private final List<UpgradeLevel> levels;

    TeamUpgradeDefinition(String id, TeamUpgradeType type, Material icon,
                          byte iconData, int slot, boolean enabled,
                          PotionEffectType effect, List<UpgradeLevel> levels) {
        this.id = id;
        this.type = type;
        this.icon = icon;
        this.iconData = iconData;
        this.slot = slot;
        this.enabled = enabled;
        this.effect = effect;
        this.levels = Collections.unmodifiableList(
                new ArrayList<UpgradeLevel>(levels));
    }

    /**
     * Livello indicato, a partire da uno.
     *
     * @return il livello, oppure {@code null} se fuori intervallo
     */
    public UpgradeLevel getLevel(int level) {
        if (level < 1 || level > levels.size()) {
            return null;
        }
        return levels.get(level - 1);
    }

    /**
     * Costo del livello indicato nel profilo economico della partita.
     *
     * @return il costo, oppure {@code null} se non acquistabile
     */
    public ItemCost getCost(int level, String profileId) {
        UpgradeLevel target = getLevel(level);
        return target == null ? null : target.getCost(profileId);
    }

    public int getMaximumLevel() {
        return levels.size();
    }

    public String getId() {
        return id;
    }

    public TeamUpgradeType getType() {
        return type;
    }

    public Material getIcon() {
        return icon;
    }

    public byte getIconData() {
        return iconData;
    }

    public int getSlot() {
        return slot;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return l'effetto pozione applicato, oppure {@code null} per gli enchant
     */
    public PotionEffectType getEffect() {
        return effect;
    }

    public List<UpgradeLevel> getLevels() {
        return levels;
    }

    public String getNameKey() {
        return type.getNameKey();
    }

    public String getLoreKey() {
        return type.getLoreKey();
    }
}
