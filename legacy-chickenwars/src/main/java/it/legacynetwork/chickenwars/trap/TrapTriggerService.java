package it.legacynetwork.chickenwars.trap;

import it.legacynetwork.chickenwars.effect.EffectAdapter;
import it.legacynetwork.chickenwars.upgrade.RoyalUpgradeDefinition;
import it.legacynetwork.chickenwars.upgrade.RoyalUpgradeType;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeService;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeState;
import it.legacynetwork.chickenwars.upgrade.TrapDefinition;
import it.legacynetwork.chickenwars.upgrade.TrapEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Attivazione delle trappole all'ingresso di un nemico nella base.
 *
 * <p>La coda autorevole resta quella di {@link TeamUpgradeState}: qui non
 * esiste una seconda coda. La trappola viene estratta atomicamente prima di
 * applicare gli effetti e rimessa in testa soltanto se nessun effetto e' stato
 * accettato, quindi un errore dell'adapter non la consuma e un'applicazione
 * parziale non la duplica.</p>
 *
 * <p>Il fronte di ingresso e' responsabilita' di {@link BaseEntryTracker}: qui
 * si assume che l'attraversamento sia gia' stato riconosciuto una sola
 * volta.</p>
 */
public final class TrapTriggerService {

    private final TeamUpgradeService upgrades;
    private final EffectAdapter effects;

    public TrapTriggerService(TeamUpgradeService upgrades,
                              EffectAdapter effects) {
        if (upgrades == null || effects == null) {
            throw new IllegalArgumentException("Servizio trappole incompleto");
        }
        this.upgrades = upgrades;
        this.effects = effects;
    }

    /**
     * Attiva, se possibile, la prima trappola in coda della squadra colpita.
     *
     * @param request contesto gia' risolto dell'ingresso
     * @return l'esito, mai nullo
     */
    public TrapTriggerResult trigger(TrapTriggerRequest request) {
        if (request == null || !request.isGameRunning()
                || request.getArenaId() == null
                || request.getOwnerTeamId() == null
                || request.getIntruderId() == null
                || !request.isIntruderEligible()
                || request.isFriendly()) {
            return TrapTriggerResult.notEligible();
        }

        TeamUpgradeState state = upgrades.peekState(request.getArenaId(),
                request.getOwnerTeamId());
        if (state == null || !state.hasTraps()) {
            return TrapTriggerResult.noTrap();
        }

        // Estrazione atomica: due nemici che entrano insieme non possono
        // ottenere la stessa trappola.
        String trapId = state.pollTrap();
        if (trapId == null) {
            return TrapTriggerResult.noTrap();
        }

        TrapDefinition trap = upgrades.getCatalog().getTrap(trapId);
        if (trap == null) {
            // Definizione sparita dopo un reload: rimettere in coda una voce
            // inutilizzabile la farebbe riprovare all'infinito.
            return TrapTriggerResult.of(TrapTriggerResult.Type.FAILED, trapId,
                    0, 0, false, 0, state.getTrapCount(),
                    new ArrayList<UUID>());
        }

        int attempted = 0;
        int intruderApplied = 0;
        for (TrapEffect effect : trap.getIntruderEffects()) {
            attempted++;
            if (effects.apply(request.getIntruderId(), effect.getType(),
                    effect.getDurationTicks(), effect.getAmplifier())) {
                intruderApplied++;
            }
        }

        int bonusTicks = guardBonusTicks(request.getArenaId(),
                request.getOwnerTeamId());
        int defenderApplied = 0;
        int defenderDuration = 0;
        List<UUID> affected = new ArrayList<UUID>();
        for (UUID defender : request.getDefenders()) {
            if (defender == null || defender.equals(request.getIntruderId())) {
                continue;
            }
            boolean touched = false;
            for (TrapEffect effect : trap.getDefenderEffects()) {
                attempted++;
                int duration = effect.getDurationTicks() + bonusTicks;
                defenderDuration = Math.max(defenderDuration, duration);
                if (effects.apply(defender, effect.getType(), duration,
                        effect.getAmplifier())) {
                    defenderApplied++;
                    touched = true;
                }
            }
            if (touched) {
                affected.add(defender);
            }
        }

        boolean revealed = false;
        if (trap.revealsInvisibility()) {
            // L'allarme vale come attivazione anche quando l'intruso non era
            // invisibile: il suo scopo primario e' la segnalazione.
            revealed = effects.clear(request.getIntruderId(),
                    PotionEffectType.INVISIBILITY);
        }

        if (attempted > 0 && intruderApplied + defenderApplied == 0) {
            // Nessun effetto accettato: la trappola torna esattamente in testa.
            state.requeueFirst(trapId);
            return TrapTriggerResult.of(TrapTriggerResult.Type.FAILED, trapId,
                    0, 0, revealed, 0, state.getTrapCount(),
                    new ArrayList<UUID>());
        }

        return TrapTriggerResult.of(TrapTriggerResult.Type.TRIGGERED, trapId,
                intruderApplied, defenderApplied, revealed, defenderDuration,
                state.getTrapCount(), affected);
    }

    /**
     * Bonus di durata concesso da Royal Guard agli effetti dei difensori.
     *
     * <p>Il valore proviene interamente dalla configurazione: nessun
     * bilanciamento e' scritto nel codice.</p>
     *
     * @return il bonus in tick, zero se l'upgrade non e' posseduto
     */
    public int guardBonusTicks(String arenaId, String teamId) {
        RoyalUpgradeDefinition definition =
                upgrades.getCatalog().getRoyalUpgrade(RoyalUpgradeType.ROYAL_GUARD);
        TeamUpgradeState state = upgrades.peekState(arenaId, teamId);
        if (definition == null || state == null) {
            return 0;
        }
        return definition.getCumulativeDurationTicks(
                state.getRoyalLevel(RoyalUpgradeType.ROYAL_GUARD));
    }
}
