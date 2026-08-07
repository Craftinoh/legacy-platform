package it.legacynetwork.chickenwars.chicken;

import it.legacynetwork.chickenwars.model.ChickenState;

/**
 * Unico punto in cui si decide e si applica il danno alla Gallina Reale.
 *
 * <p>La salute autorevole resta in {@link ChickenVitals}: l'entita' Bukkit non
 * e' la fonte della verita' e viene soltanto sincronizzata a valle.</p>
 *
 * <p>La riduzione dell'armatura reale e' applicata qui e in nessun altro
 * punto, cosi' non puo' essere sommata due volte da listener diversi.</p>
 */
public final class RoyalChickenDamageService {

    /**
     * Riceve un tentativo di danno e ne applica le conseguenze.
     *
     * @param chicken gallina bersaglio
     * @param request contesto gia' risolto del colpo
     * @return l'esito osservabile, mai nullo
     */
    public RoyalDamageResult damage(RoyalChicken chicken,
                                    RoyalDamageRequest request) {
        if (chicken == null || request == null) {
            return RoyalDamageResult.ignored();
        }
        if (!request.isGameRunning()) {
            return RoyalDamageResult.ignored();
        }
        if (chicken.getState() == ChickenState.DEAD || !chicken.isAlive()) {
            // Una gallina gia' sconfitta non subisce altri colpi.
            return RoyalDamageResult.ignored();
        }
        if (!chicken.getState().isDamageable()) {
            return RoyalDamageResult.ignored();
        }
        if (request.getAttackerId() == null || !request.isAttackerPlaying()) {
            // Danno ambientale, spettatori, eliminati ed estranei sono esclusi.
            return RoyalDamageResult.ignored();
        }
        if (request.isFriendly()) {
            return RoyalDamageResult.ignored();
        }

        double raw = request.getRawDamage();
        if (raw <= 0.0D) {
            return RoyalDamageResult.ignored();
        }

        double applied = applyReduction(raw, request.getDamageReduction());
        if (applied <= 0.0D) {
            return RoyalDamageResult.ignored();
        }

        DamageOutcome outcome = chicken.damage(applied, request.getAttackerId());
        if (outcome.isNoop()) {
            return RoyalDamageResult.ignored();
        }
        return RoyalDamageResult.of(outcome.isFatal()
                        ? RoyalDamageResult.Type.DEFEATED
                        : RoyalDamageResult.Type.DAMAGED,
                raw, applied, outcome);
    }

    /**
     * Applica la riduzione dell'armatura reale.
     *
     * <p>Il risultato non e' mai negativo e non supera mai il danno grezzo.</p>
     *
     * @param raw       danno richiesto
     * @param reduction frazione assorbita, limitata a [0, 1]
     * @return il danno residuo
     */
    public static double applyReduction(double raw, double reduction) {
        if (raw <= 0.0D) {
            return 0.0D;
        }
        double clamped = Math.max(0.0D, Math.min(1.0D, reduction));
        return Math.max(0.0D, raw * (1.0D - clamped));
    }
}
