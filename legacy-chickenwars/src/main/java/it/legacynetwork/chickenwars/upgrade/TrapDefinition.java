package it.legacynetwork.chickenwars.upgrade;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Trappola acquistabile e messa in coda dalla squadra.
 *
 * <p>Il costo non e' qui: dipende dalla posizione occupata nella coda ed e'
 * quindi definito una volta sola nel catalogo.</p>
 */
public final class TrapDefinition {

    private final String id;
    private final Material icon;
    private final byte iconData;
    private final int slot;
    private final boolean enabled;
    private final boolean revealsInvisibility;
    private final List<TrapEffect> intruderEffects;
    private final List<TrapEffect> defenderEffects;

    TrapDefinition(String id, Material icon, byte iconData, int slot,
                   boolean enabled, boolean revealsInvisibility,
                   List<TrapEffect> intruderEffects,
                   List<TrapEffect> defenderEffects) {
        this.id = id;
        this.icon = icon;
        this.iconData = iconData;
        this.slot = slot;
        this.enabled = enabled;
        this.revealsInvisibility = revealsInvisibility;
        this.intruderEffects = Collections.unmodifiableList(
                new ArrayList<TrapEffect>(intruderEffects));
        this.defenderEffects = Collections.unmodifiableList(
                new ArrayList<TrapEffect>(defenderEffects));
    }

    public String getId() {
        return id;
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
     * Indica se la trappola annulla l'invisibilita' dell'intruso.
     */
    public boolean revealsInvisibility() {
        return revealsInvisibility;
    }

    /** Effetti applicati a chi ha attivato la trappola. */
    public List<TrapEffect> getIntruderEffects() {
        return intruderEffects;
    }

    /** Effetti applicati ai difensori presenti nella base. */
    public List<TrapEffect> getDefenderEffects() {
        return defenderEffects;
    }

    public String getNameKey() {
        return "trap." + id.toLowerCase(Locale.ROOT) + ".name";
    }

    public String getLoreKey() {
        return "trap." + id.toLowerCase(Locale.ROOT) + ".lore";
    }
}
