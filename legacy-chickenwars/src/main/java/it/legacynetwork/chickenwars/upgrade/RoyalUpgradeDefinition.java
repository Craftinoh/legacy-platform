package it.legacynetwork.chickenwars.upgrade;

import it.legacynetwork.chickenwars.shop.ItemCost;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Definizione configurabile di un potenziamento della Gallina Reale.
 */
public final class RoyalUpgradeDefinition {

    private final RoyalUpgradeType type;
    private final Material icon;
    private final byte iconData;
    private final int slot;
    private final boolean enabled;
    private final List<UpgradeLevel> levels;

    RoyalUpgradeDefinition(RoyalUpgradeType type, Material icon, byte iconData,
                           int slot, boolean enabled,
                           List<UpgradeLevel> levels) {
        this.type = type;
        this.icon = icon;
        this.iconData = iconData;
        this.slot = slot;
        this.enabled = enabled;
        this.levels = Collections.unmodifiableList(
                new ArrayList<UpgradeLevel>(levels));
    }

    /**
     * @return il livello richiesto, oppure {@code null} se fuori intervallo
     */
    public UpgradeLevel getLevel(int level) {
        if (level < 1 || level > levels.size()) {
            return null;
        }
        return levels.get(level - 1);
    }

    /**
     * @return il costo nel profilo indicato, oppure {@code null}
     */
    public ItemCost getCost(int level, String profileId) {
        UpgradeLevel target = getLevel(level);
        return target == null ? null : target.getCost(profileId);
    }

    /**
     * Somma dei valori fino al livello indicato.
     *
     * <p>Serve per gli effetti cumulativi come la salute aggiuntiva, dove il
     * totale dipende da tutti i livelli acquistati.</p>
     */
    public double getCumulativeValue(int level) {
        double total = 0.0D;
        for (int index = 1; index <= level && index <= levels.size(); index++) {
            total += levels.get(index - 1).getValue();
        }
        return total;
    }

    /**
     * Somma delle durate dichiarate fino al livello indicato.
     *
     * <p>Royal Guard la usa come bonus alla durata degli effetti concessi ai
     * difensori dalla Contro-Offensiva: il valore resta interamente
     * configurabile per livello.</p>
     *
     * @return il totale in tick, mai negativo
     */
    public int getCumulativeDurationTicks(int level) {
        int total = 0;
        for (int index = 1; index <= level && index <= levels.size(); index++) {
            total += Math.max(0, levels.get(index - 1).getDurationTicks());
        }
        return total;
    }

    /**
     * Valore del solo livello indicato, senza cumulo.
     */
    public double getValueAt(int level) {
        UpgradeLevel target = getLevel(level);
        return target == null ? 0.0D : target.getValue();
    }

    public int getMaximumLevel() {
        return levels.size();
    }

    public RoyalUpgradeType getType() {
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
