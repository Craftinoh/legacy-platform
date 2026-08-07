package it.legacynetwork.chickenwars.chicken;

import it.legacynetwork.chickenwars.upgrade.RoyalUpgradeDefinition;
import it.legacynetwork.chickenwars.upgrade.RoyalUpgradeType;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeService;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeState;

/**
 * Traduce i livelli reali acquistati in effetti sulla Gallina Reale.
 *
 * <p>Vitality e Armor hanno nature diverse: la prima e' una mutazione da
 * applicare una sola volta per livello, la seconda una lettura consultata a
 * ogni colpo. Tenerle insieme rende evidente che entrambe leggono lo stesso
 * stato autorevole.</p>
 */
public final class RoyalUpgradeApplier {

    private final TeamUpgradeService upgrades;

    public RoyalUpgradeApplier(TeamUpgradeService upgrades) {
        if (upgrades == null) {
            throw new IllegalArgumentException("Servizio upgrade mancante");
        }
        this.upgrades = upgrades;
    }

    /**
     * Frazione di danno assorbita dalla Royal Armor della squadra.
     *
     * @return un valore compreso fra 0 e 1
     */
    public double resolveArmorReduction(String arenaId, String teamId) {
        RoyalUpgradeDefinition definition = upgrades.getCatalog()
                .getRoyalUpgrade(RoyalUpgradeType.ROYAL_ARMOR);
        TeamUpgradeState state = upgrades.peekState(arenaId, teamId);
        if (definition == null || state == null) {
            return 0.0D;
        }
        double reduction = definition.getCumulativeValue(
                state.getRoyalLevel(RoyalUpgradeType.ROYAL_ARMOR));
        return Math.max(0.0D, Math.min(1.0D, reduction));
    }

    /**
     * Converte in salute massima i livelli di Vitality non ancora applicati.
     *
     * <p>Ogni livello viene convertito una volta sola: riaprire il menu o
     * ricreare l'entita' non produce un secondo incremento. Su una gallina
     * sconfitta non viene applicato nulla.</p>
     *
     * @return l'incremento realmente aggiunto ora
     */
    public double applyVitality(String arenaId, String teamId,
                                RoyalChicken chicken) {
        if (chicken == null) {
            return 0.0D;
        }
        RoyalUpgradeDefinition definition = upgrades.getCatalog()
                .getRoyalUpgrade(RoyalUpgradeType.ROYAL_VITALITY);
        TeamUpgradeState state = upgrades.peekState(arenaId, teamId);
        if (definition == null || state == null) {
            return 0.0D;
        }

        int target = Math.min(state.getRoyalLevel(RoyalUpgradeType.ROYAL_VITALITY),
                definition.getMaximumLevel());
        int applied = chicken.getAppliedVitalityLevel();
        double total = 0.0D;
        while (applied < target) {
            double granted = chicken.getVitals()
                    .increaseMaximum(definition.getValueAt(applied + 1));
            if (granted <= 0.0D) {
                // Gallina gia' sconfitta: il livello resta non applicato.
                break;
            }
            applied++;
            total += granted;
        }
        chicken.setAppliedVitalityLevel(applied);
        return total;
    }

    /**
     * Livello reale posseduto dalla squadra.
     */
    public int getRoyalLevel(String arenaId, String teamId,
                             RoyalUpgradeType type) {
        TeamUpgradeState state = upgrades.peekState(arenaId, teamId);
        return state == null ? 0 : state.getRoyalLevel(type);
    }
}
