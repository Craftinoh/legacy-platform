package it.legacynetwork.chickenwars.chicken;

import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Conservazione dell'UUID durante la sconfitta della Gallina Reale.
 *
 * <p>{@code ChickenService.playDeath} rimuove l'entita' e azzera il
 * riferimento: leggere l'UUID dopo quella chiamata produce sempre
 * {@code null}. La sequenza corretta, quella applicata da
 * {@code Game.handleChickenDefeat}, cattura l'identificatore prima, poi
 * deregistra e descrive la sconfitta con lo stesso valore.</p>
 */
class RoyalDefeatEntityIdTest {

    private static final String ARENA = "a1";
    private static final String TEAM = "red";

    private ChickenService chickens;
    private RoyalChickenRegistry registry;
    private RoyalDefeatDispatcher dispatcher;
    private List<RoyalDefeat> dispatched;
    private RoyalChicken chicken;
    private UUID entityId;

    @BeforeEach
    void setUp() {
        chickens = new ChickenService(Logger.getLogger("test"));
        registry = new RoyalChickenRegistry();
        dispatcher = new RoyalDefeatDispatcher();
        dispatched = new ArrayList<RoyalDefeat>();
        dispatcher.register(defeat -> dispatched.add(defeat));

        // Nido assente e entita' non valida: playDeath percorre il ramo che
        // non richiede un mondo caricato, quindi resta verificabile senza
        // server pur eseguendo il codice di produzione.
        chicken = new RoyalChicken(TEAM, null, ChickenSettings.fromSection(null));
        chicken.releaseProtection();

        entityId = UUID.randomUUID();
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getUniqueId()).thenReturn(entityId);
        when(entity.isValid()).thenReturn(false);
        chicken.setEntity(entity);

        registry.register(entityId, ARENA, TEAM);
    }

    /**
     * Porta la gallina allo stesso stato in cui la trova la produzione.
     *
     * <p>{@code handleChickenDefeat} viene raggiunto solo dopo un esito
     * {@code DEFEATED}: partire da una gallina ancora viva descriverebbe una
     * situazione che non si verifica.</p>
     */
    private void dealLethalDamageOnce() {
        if (!chicken.isAlive()) {
            return;
        }
        RoyalDamageResult lethal = new RoyalChickenDamageService().damage(
                chicken, RoyalDamageRequest.builder()
                        .attacker(UUID.randomUUID(), "blue")
                        .owner(TEAM)
                        .gameRunning(true)
                        .attackerPlaying(true)
                        .rawDamage(9999.0D)
                        .damageReduction(0.0D)
                        .build());
        assertTrue(lethal.isDefeated(), "la gallina doveva essere sconfitta");
    }

    /**
     * Riproduce la sequenza di {@code Game.handleChickenDefeat}.
     *
     * @return la sconfitta prodotta, oppure {@code null} se gia' notificata
     */
    private RoyalDefeat defeat(UUID attackerId) {
        dealLethalDamageOnce();
        if (!chicken.markDefeated()) {
            return null;
        }
        UUID captured = chicken.getEntity() == null
                ? null : chicken.getEntity().getUniqueId();

        chickens.playDeath(chicken, ChickenSettings.fromSection(null));

        if (captured != null) {
            registry.unregister(captured);
        }
        RoyalDefeat defeat = new RoyalDefeat(ARENA, TEAM, captured, attackerId,
                System.currentTimeMillis());
        dispatcher.dispatch(defeat);
        return defeat;
    }

    // ------------------------------------------------------------------
    // Stato prima della sconfitta
    // ------------------------------------------------------------------

    @Test
    void primaDellaMorteLEntitaEsisteEdEuRegistrata() {
        assertNotNull(chicken.getEntity());
        assertEquals(entityId, chicken.getEntity().getUniqueId());
        assertTrue(registry.isRoyalChicken(entityId));
        assertEquals(entityId, registry.getEntity(ARENA, TEAM));
    }

    /**
     * E' la causa del difetto corretto: dopo {@code playDeath} il riferimento
     * non esiste piu', quindi l'UUID va catturato prima.
     */
    @Test
    void playDeathAzzeraIlRiferimentoAllEntita() {
        chickens.playDeath(chicken, ChickenSettings.fromSection(null));

        assertNull(chicken.getEntity(),
                "playDeath deve rimuovere l'entita'");
    }

    // ------------------------------------------------------------------
    // Sconfitta
    // ------------------------------------------------------------------

    @Test
    void laSconfittaConservaLUuidDellEntita() {
        RoyalDefeat defeat = defeat(UUID.randomUUID());

        assertNotNull(defeat);
        assertNotNull(defeat.getEntityId(),
                "RoyalDefeat.entityId non deve essere nullo");
        assertEquals(entityId, defeat.getEntityId());
    }

    @Test
    void laSconfittaRiportaArenaSquadraEAggressore() {
        UUID attackerId = UUID.randomUUID();

        RoyalDefeat defeat = defeat(attackerId);

        assertEquals(ARENA, defeat.getArenaId());
        assertEquals(TEAM, defeat.getTeamId());
        assertEquals(attackerId, defeat.getAttackerId());
    }

    @Test
    void dopoLaSconfittaIlRegistryNonHaVociStantie() {
        defeat(UUID.randomUUID());

        assertNull(registry.lookup(entityId));
        assertFalse(registry.isRoyalChicken(entityId));
        assertNull(registry.getEntity(ARENA, TEAM));
        assertEquals(0, registry.size());
    }

    @Test
    void laPuliziaNonToccaLeAltreArene() {
        UUID otherId = UUID.randomUUID();
        registry.register(otherId, "a2", TEAM);

        defeat(UUID.randomUUID());

        assertNull(registry.lookup(entityId));
        assertNotNull(registry.lookup(otherId));
        assertEquals(1, registry.size());
    }

    // ------------------------------------------------------------------
    // Eventi duplicati
    // ------------------------------------------------------------------

    @Test
    void unSoloDispatchAncheConDueEventi() {
        assertNotNull(defeat(UUID.randomUUID()));
        assertNull(defeat(UUID.randomUUID()),
                "la seconda sconfitta non deve essere notificata");

        assertEquals(1, dispatched.size());
        assertEquals(entityId, dispatched.get(0).getEntityId());
    }

    @Test
    void ilSecondoEventoNonRideregistraNulla() {
        defeat(UUID.randomUUID());
        UUID reused = UUID.randomUUID();
        registry.register(reused, ARENA, TEAM);

        defeat(UUID.randomUUID());

        // Il secondo passaggio esce su markDefeated: nessuna rimozione extra.
        assertNotNull(registry.lookup(reused));
        assertEquals(1, registry.size());
    }

    @Test
    void dopoLaSconfittaIDanniSonoIgnorati() {
        defeat(UUID.randomUUID());

        RoyalDamageResult result = new RoyalChickenDamageService().damage(
                chicken, RoyalDamageRequest.builder()
                        .attacker(UUID.randomUUID(), "blue")
                        .owner(TEAM)
                        .gameRunning(true)
                        .attackerPlaying(true)
                        .rawDamage(10.0D)
                        .damageReduction(0.0D)
                        .build());

        assertTrue(result.isIgnored());
        assertEquals(1, dispatched.size());
    }

    @Test
    void unaGallinaSenzaEntitaProduceUnaSconfittaSenzaUuid() {
        chicken.setEntity(null);
        registry.unregister(entityId);

        RoyalDefeat defeat = defeat(UUID.randomUUID());

        assertNotNull(defeat);
        assertNull(defeat.getEntityId());
        assertEquals(1, dispatched.size());
    }
}
