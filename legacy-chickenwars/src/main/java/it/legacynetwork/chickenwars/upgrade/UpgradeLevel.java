package it.legacynetwork.chickenwars.upgrade;

import it.legacynetwork.chickenwars.shop.ItemCost;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un singolo livello di un upgrade.
 *
 * <p>Il prezzo dipende dal profilo economico della partita, quindi non e' un
 * valore unico ma una mappa profilo verso costo.</p>
 */
public final class UpgradeLevel {

    private final int level;
    private final Map<String, ItemCost> costs;
    private final int amplifier;
    private final double value;
    private final int durationTicks;

    UpgradeLevel(int level, Map<String, ItemCost> costs, int amplifier,
                 double value, int durationTicks) {
        this.level = level;
        this.costs = Collections.unmodifiableMap(
                new LinkedHashMap<String, ItemCost>(costs));
        this.amplifier = amplifier;
        this.value = value;
        this.durationTicks = durationTicks;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Costo nel profilo economico indicato.
     *
     * @return il costo, oppure {@code null} se il livello non e' listato
     */
    public ItemCost getCost(String profileId) {
        return profileId == null ? null : costs.get(profileId.toLowerCase());
    }

    public Map<String, ItemCost> getCosts() {
        return costs;
    }

    /**
     * Amplificatore dell'effetto, ovvero livello enchant o potion meno uno.
     */
    public int getAmplifier() {
        return amplifier;
    }

    /**
     * Valore numerico generico: salute aggiuntiva, riduzione danno, ecc.
     */
    public double getValue() {
        return value;
    }

    /**
     * Durata dell'effetto in tick; zero indica effetto permanente gestito
     * dal ciclo di partita.
     */
    public int getDurationTicks() {
        return durationTicks;
    }

    @Override
    public String toString() {
        return "UpgradeLevel{" + level + ", amp=" + amplifier
                + ", value=" + value + '}';
    }
}
