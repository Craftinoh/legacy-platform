package it.legacynetwork.chickenwars.upgrade;

import it.legacynetwork.chickenwars.mode.ModeProfile;
import it.legacynetwork.chickenwars.shop.ItemCost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Catalogo immutabile di upgrade di squadra, trappole e potenziamenti reali.
 *
 * <p>Viene sostituito interamente a ogni reload, cosi' nessun menu aperto
 * osserva uno stato parziale.</p>
 */
public final class UpgradeCatalog {

    private final Map<TeamUpgradeType, TeamUpgradeDefinition> teamUpgrades;
    private final Map<RoyalUpgradeType, RoyalUpgradeDefinition> royalUpgrades;
    private final Map<String, TrapDefinition> traps;
    private final List<Map<String, ItemCost>> trapPositionCosts;
    private final int maximumTraps;
    private final List<String> warnings;

    UpgradeCatalog(Map<TeamUpgradeType, TeamUpgradeDefinition> teamUpgrades,
                   Map<RoyalUpgradeType, RoyalUpgradeDefinition> royalUpgrades,
                   Map<String, TrapDefinition> traps,
                   List<Map<String, ItemCost>> trapPositionCosts,
                   int maximumTraps, List<String> warnings) {
        this.teamUpgrades = Collections.unmodifiableMap(
                new EnumMap<TeamUpgradeType, TeamUpgradeDefinition>(teamUpgrades));
        this.royalUpgrades = Collections.unmodifiableMap(
                new EnumMap<RoyalUpgradeType, RoyalUpgradeDefinition>(
                        royalUpgrades));
        this.traps = Collections.unmodifiableMap(
                new LinkedHashMap<String, TrapDefinition>(traps));
        List<Map<String, ItemCost>> copiedCosts =
                new ArrayList<Map<String, ItemCost>>();
        for (Map<String, ItemCost> costs : trapPositionCosts) {
            copiedCosts.add(Collections.unmodifiableMap(
                    new LinkedHashMap<String, ItemCost>(costs)));
        }
        this.trapPositionCosts = Collections.unmodifiableList(copiedCosts);
        this.maximumTraps = maximumTraps;
        this.warnings = Collections.unmodifiableList(
                new ArrayList<String>(warnings));
    }

    /**
     * Catalogo vuoto, usato quando la configurazione non e' leggibile.
     */
    public static UpgradeCatalog empty() {
        return new UpgradeCatalog(
                new EnumMap<TeamUpgradeType, TeamUpgradeDefinition>(
                        TeamUpgradeType.class),
                new EnumMap<RoyalUpgradeType, RoyalUpgradeDefinition>(
                        RoyalUpgradeType.class),
                new LinkedHashMap<String, TrapDefinition>(),
                new ArrayList<Map<String, ItemCost>>(), 3,
                Collections.singletonList("upgrades.yml assente o illeggibile"));
    }

    /**
     * @return la definizione, oppure {@code null} se assente o disabilitata
     */
    public TeamUpgradeDefinition getTeamUpgrade(TeamUpgradeType type) {
        TeamUpgradeDefinition definition = teamUpgrades.get(type);
        return definition != null && definition.isEnabled() ? definition : null;
    }

    /**
     * @return la definizione, oppure {@code null} se assente o disabilitata
     */
    public RoyalUpgradeDefinition getRoyalUpgrade(RoyalUpgradeType type) {
        RoyalUpgradeDefinition definition = royalUpgrades.get(type);
        return definition != null && definition.isEnabled() ? definition : null;
    }

    /**
     * @return la trappola, oppure {@code null} se assente o disabilitata
     */
    public TrapDefinition getTrap(String trapId) {
        if (trapId == null) {
            return null;
        }
        TrapDefinition definition =
                traps.get(trapId.trim().toLowerCase(Locale.ROOT));
        return definition != null && definition.isEnabled() ? definition : null;
    }

    /**
     * Costo della trappola che occuperebbe la posizione indicata.
     *
     * <p>La posizione parte da zero: la prima trappola in coda costa meno delle
     * successive, come previsto dalla configurazione.</p>
     *
     * @param position posizione che verrebbe occupata
     * @param profile  profilo economico della partita
     * @return il costo, oppure {@code null} se la posizione non e' listata
     */
    public ItemCost getTrapCost(int position, ModeProfile profile) {
        if (profile == null) {
            return null;
        }
        return getTrapCost(position, profile.getPricingProfile());
    }

    /**
     * @return il costo, oppure {@code null} se la posizione non e' listata
     */
    public ItemCost getTrapCost(int position, String profileId) {
        if (position < 0 || position >= trapPositionCosts.size()
                || profileId == null) {
            return null;
        }
        return trapPositionCosts.get(position)
                .get(profileId.toLowerCase(Locale.ROOT));
    }

    public Map<TeamUpgradeType, TeamUpgradeDefinition> getTeamUpgrades() {
        return teamUpgrades;
    }

    public Map<RoyalUpgradeType, RoyalUpgradeDefinition> getRoyalUpgrades() {
        return royalUpgrades;
    }

    public Map<String, TrapDefinition> getTraps() {
        return traps;
    }

    /**
     * Numero massimo di trappole in coda per squadra.
     */
    public int getMaximumTraps() {
        return maximumTraps;
    }

    /**
     * Anomalie non fatali rilevate al caricamento.
     */
    public List<String> getWarnings() {
        return warnings;
    }

    public boolean isEmpty() {
        return teamUpgrades.isEmpty() && royalUpgrades.isEmpty()
                && traps.isEmpty();
    }
}
