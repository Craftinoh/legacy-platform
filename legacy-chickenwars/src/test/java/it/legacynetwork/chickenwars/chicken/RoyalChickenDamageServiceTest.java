package it.legacynetwork.chickenwars.chicken;

import it.legacynetwork.chickenwars.model.ChickenState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Danno centralizzato della Gallina Reale.
 *
 * <p>La salute autorevole e' quella del dominio: qui si verifica che la
 * riduzione dell'armatura reale sia applicata una volta sola e che i colpi non
 * validi non cambino nulla.</p>
 */
class RoyalChickenDamageServiceTest {

    private static final String OWNER = "red";
    private static final String ENEMY = "blue";
    private static final double DELTA = 0.0001D;

    private RoyalChickenDamageService service;
    private RoyalChicken chicken;
    private UUID attacker;

    @BeforeEach
    void setUp() {
        service = new RoyalChickenDamageService();
        chicken = new RoyalChicken(OWNER, null,
                ChickenSettings.fromSection(null));
        chicken.releaseProtection();
        attacker = UUID.randomUUID();
    }

    private RoyalDamageRequest.Builder enemyHit(double damage) {
        return RoyalDamageRequest.builder()
                .attacker(attacker, ENEMY)
                .owner(OWNER)
                .rawDamage(damage);
    }

    // ------------------------------------------------------------------
    // Colpi validi
    // ------------------------------------------------------------------

    @Test
    void unNemicoValidoInfliggeDanno() {
        RoyalDamageResult result = service.damage(chicken,
                enemyHit(10.0D).build());

        assertSame(RoyalDamageResult.Type.DAMAGED, result.getType());
        assertEquals(10.0D, result.getAppliedDamage(), DELTA);
        // Scudo 25: il colpo viene interamente assorbito.
        assertEquals(15, chicken.getVitals().getDisplayShield());
    }

    @Test
    void laRiduzioneArmaturaAbbassaIlDanno() {
        RoyalDamageResult result = service.damage(chicken,
                enemyHit(20.0D).damageReduction(0.25D).build());

        assertEquals(20.0D, result.getRawDamage(), DELTA);
        assertEquals(15.0D, result.getAppliedDamage(), DELTA);
    }

    @Test
    void laRiduzioneNonProduceMaiDannoNegativo() {
        assertEquals(0.0D,
                RoyalChickenDamageService.applyReduction(10.0D, 1.5D), DELTA);
        assertEquals(10.0D,
                RoyalChickenDamageService.applyReduction(10.0D, -2.0D), DELTA);
        assertEquals(0.0D,
                RoyalChickenDamageService.applyReduction(0.0D, 0.5D), DELTA);
    }

    @Test
    void unaRiduzioneTotaleAnnullaIlColpo() {
        RoyalDamageResult result = service.damage(chicken,
                enemyHit(10.0D).damageReduction(1.0D).build());

        assertTrue(result.isIgnored());
        assertEquals(25, chicken.getVitals().getDisplayShield());
    }

    @Test
    void ilColpoLetaleProduceLaSconfitta() {
        RoyalDamageResult result = service.damage(chicken,
                enemyHit(1000.0D).build());

        assertSame(RoyalDamageResult.Type.DEFEATED, result.getType());
        assertTrue(result.getOutcome().isFatal());
        assertFalse(chicken.isAlive());
    }

    // ------------------------------------------------------------------
    // Colpi ignorati
    // ------------------------------------------------------------------

    @Test
    void unAlleatoNonInfliggeDanno() {
        RoyalDamageResult result = service.damage(chicken,
                RoyalDamageRequest.builder()
                        .attacker(attacker, OWNER)
                        .owner(OWNER)
                        .rawDamage(10.0D)
                        .build());

        assertTrue(result.isIgnored());
        assertEquals(25, chicken.getVitals().getDisplayShield());
    }

    @Test
    void unoSpettatoreOEliminatoNonInfliggeDanno() {
        RoyalDamageResult result = service.damage(chicken,
                enemyHit(10.0D).attackerPlaying(false).build());

        assertTrue(result.isIgnored());
        assertEquals(25, chicken.getVitals().getDisplayShield());
    }

    @Test
    void ilDannoAmbientaleSenzaAggressoreVieneIgnorato() {
        RoyalDamageResult result = service.damage(chicken,
                RoyalDamageRequest.builder()
                        .owner(OWNER)
                        .rawDamage(10.0D)
                        .build());

        assertTrue(result.isIgnored());
    }

    @Test
    void aPartitaNonInCorsoNessunDanno() {
        RoyalDamageResult result = service.damage(chicken,
                enemyHit(10.0D).gameRunning(false).build());

        assertTrue(result.isIgnored());
    }

    @Test
    void ilDannoNulloONegativoVieneIgnorato() {
        assertTrue(service.damage(chicken, enemyHit(0.0D).build()).isIgnored());
        assertTrue(service.damage(chicken, enemyHit(-5.0D).build()).isIgnored());
        assertEquals(25, chicken.getVitals().getDisplayShield());
    }

    @Test
    void unaGallinaProtettaNonSubisceDanno() {
        RoyalChicken protectedChicken = new RoyalChicken(OWNER, null,
                ChickenSettings.fromSection(null));

        assertSame(ChickenState.PROTECTED, protectedChicken.getState());
        assertTrue(service.damage(protectedChicken,
                enemyHit(10.0D).build()).isIgnored());
    }

    @Test
    void unaGallinaGiaSconfittaNonSubisceAltriColpi() {
        service.damage(chicken, enemyHit(1000.0D).build());

        RoyalDamageResult second = service.damage(chicken,
                enemyHit(10.0D).build());

        assertTrue(second.isIgnored());
        assertSame(ChickenState.DEAD, chicken.getState());
    }

    @Test
    void argomentiAssentiNonProduconoEffetti() {
        assertTrue(service.damage(null, enemyHit(10.0D).build()).isIgnored());
        assertTrue(service.damage(chicken, null).isIgnored());
    }

    // ------------------------------------------------------------------
    // Vitality
    // ------------------------------------------------------------------

    @Test
    void vitalityAlzaIlMassimoSenzaCurareLaGallina() {
        service.damage(chicken, enemyHit(45.0D).build());
        int before = chicken.getVitals().getDisplayHealth();

        chicken.getVitals().increaseMaximum(25.0D);

        assertEquals(125.0D, chicken.getVitals().getMaxHealth(), DELTA);
        // La salute cresce del solo delta, non torna al massimo.
        assertEquals(before + 25, chicken.getVitals().getDisplayHealth());
        assertTrue(chicken.getVitals().getDisplayHealth()
                < chicken.getVitals().getMaxHealth());
    }

    @Test
    void vitalityNonSuperaMaiIlNuovoMassimo() {
        chicken.getVitals().increaseMaximum(30.0D);

        assertEquals(130.0D, chicken.getVitals().getMaxHealth(), DELTA);
        assertEquals(130, chicken.getVitals().getDisplayHealth());
        assertTrue(chicken.getVitals().isFullHealth());
    }

    @Test
    void vitalityNonAgisceSuUnaGallinaSconfitta() {
        service.damage(chicken, enemyHit(1000.0D).build());

        assertEquals(0.0D, chicken.getVitals().increaseMaximum(50.0D), DELTA);
        assertFalse(chicken.isAlive());
    }

    @Test
    void unIncrementoNonPositivoNonCambiaNulla() {
        assertEquals(0.0D, chicken.getVitals().increaseMaximum(0.0D), DELTA);
        assertEquals(0.0D, chicken.getVitals().increaseMaximum(-10.0D), DELTA);
        assertEquals(100.0D, chicken.getVitals().getMaxHealth(), DELTA);
    }
}
