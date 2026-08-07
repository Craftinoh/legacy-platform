package it.legacynetwork.chickenwars.death;

import it.legacynetwork.chickenwars.economy.ResourceInventory;
import it.legacynetwork.chickenwars.economy.ResourceTransferService;
import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.player.equipment.ArmorTier;
import it.legacynetwork.chickenwars.player.equipment.EquipmentService;
import it.legacynetwork.chickenwars.player.equipment.SwordTier;
import it.legacynetwork.chickenwars.player.equipment.ToolTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Abbandono in combattimento convertito in morte.
 *
 * <p>Verifica quantita' trasferite, contenuto della coda premio, tier iniziali
 * e finali, numero di invocazioni, sequenza usata e stato finale della
 * sessione.</p>
 */
class CombatLogoutTest {

    private ResourceTransferService transfers;
    private EquipmentService equipment;
    private PlayerDeathProcessor processor;
    private UUID victimId;
    private UUID killerId;

    @BeforeEach
    void setUp() {
        transfers = new ResourceTransferService();
        equipment = DeathTestSupport.equipmentService();
        processor = new PlayerDeathProcessor(transfers, equipment);
        victimId = UUID.randomUUID();
        killerId = UUID.randomUUID();
    }

    private DeathOutcome logout(PlayerSession session,
                                ResourceInventory victimInventory,
                                ResourceInventory killerInventory,
                                UUID attacker, boolean eligible) {
        return processor.process(
                DeathContext.combatLogout(victimId, attacker), session,
                victimInventory, killerInventory,
                new DeathTestSupport.RecordingEligibility(eligible));
    }

    // ------------------------------------------------------------------
    // Rilevamento del combattimento (stessa logica usata in produzione)
    // ------------------------------------------------------------------

    @Test
    void quitDuranteCombattimentoHaUnAggressoreValido() {
        PlayerSession session = DeathTestSupport.session(victimId);
        session.recordDamager(killerId);

        assertSame(killerId, session.getValidDamager(10));
    }

    @Test
    void quitFuoriDalCombattimentoNonHaAggressore() {
        PlayerSession session = DeathTestSupport.session(victimId);

        assertNull(session.getValidDamager(10));
    }

    @Test
    void aggressoreScadutoNonEuPiuValido() throws InterruptedException {
        PlayerSession session = DeathTestSupport.session(victimId);
        session.recordDamager(killerId);

        // La finestra di credito e' azzerata: appena il tempo avanza il colpo
        // non e' piu' attribuibile.
        Thread.sleep(5L);

        assertNull(session.getValidDamager(0));
    }

    @Test
    void ilSuicidioNonProduceUnUccisore() {
        DeathContext context = DeathContext.combatLogout(victimId, victimId);

        assertNull(context.getKillerId());
    }

    // ------------------------------------------------------------------
    // Trasferimento risorse
    // ------------------------------------------------------------------

    @Test
    void leRisorseVengonoConsegnateAllAggressoreValido() {
        PlayerSession session = DeathTestSupport.session(victimId);
        DeathTestSupport.CountingInventory killerInventory =
                new DeathTestSupport.CountingInventory(
                        ResourceInventory.empty());

        DeathOutcome outcome = logout(session,
                DeathTestSupport.inventoryOf(ResourceType.IRON, 24),
                killerInventory, killerId, true);

        assertTrue(outcome.isProcessed());
        assertTrue(outcome.isKillerRewarded());
        assertEquals(24, outcome.getTransfer().getDelivered()
                .get(ResourceType.IRON).intValue());
        assertEquals(24, killerInventory.snapshot()
                .get(ResourceType.IRON).intValue());
        assertFalse(outcome.getTransfer().hasQueued());
    }

    @Test
    void tutteELeCinqueValuteVengonoSpostate() {
        PlayerSession session = DeathTestSupport.session(victimId);
        ResourceInventory killerInventory = ResourceInventory.empty();

        DeathOutcome outcome = logout(session,
                DeathTestSupport.allCurrencies(7), killerInventory,
                killerId, true);

        assertEquals(ResourceType.values().length,
                outcome.getTransfer().getDelivered().size());
        for (ResourceType type : ResourceType.values()) {
            assertEquals(7, killerInventory.count(type), type.name());
        }
    }

    @Test
    void senzaAggressoreValidoLeRisorseSpariscono() {
        PlayerSession session = DeathTestSupport.session(victimId);
        DeathTestSupport.CountingInventory victimInventory =
                new DeathTestSupport.CountingInventory(
                        DeathTestSupport.inventoryOf(ResourceType.GOLD, 12));
        DeathTestSupport.CountingInventory killerInventory =
                new DeathTestSupport.CountingInventory(
                        ResourceInventory.empty());

        DeathOutcome outcome = logout(session, victimInventory,
                killerInventory, killerId, false);

        assertTrue(outcome.isProcessed());
        assertFalse(outcome.isKillerRewarded());
        // La vittima e' comunque stata svuotata, ma nessuno ha ricevuto nulla.
        assertEquals(1, victimInventory.getWithdrawAllCalls());
        assertEquals(0, victimInventory.count(ResourceType.GOLD));
        assertEquals(0, killerInventory.getDepositCalls());
        assertTrue(killerInventory.snapshot().isEmpty());
    }

    @Test
    void aggressoreAssenteNonRicevoNulla() {
        PlayerSession session = DeathTestSupport.session(victimId);
        DeathTestSupport.CountingInventory victimInventory =
                new DeathTestSupport.CountingInventory(
                        DeathTestSupport.inventoryOf(ResourceType.IRON, 5));

        DeathOutcome outcome = logout(session, victimInventory, null,
                null, true);

        assertTrue(outcome.isProcessed());
        assertFalse(outcome.isKillerRewarded());
        assertEquals(0, victimInventory.count(ResourceType.IRON));
    }

    @Test
    void inventarioPienoAccodaLEccedenzaUnaVoltaSola() {
        PlayerSession session = DeathTestSupport.session(victimId);

        DeathOutcome outcome = logout(session,
                DeathTestSupport.inventoryOf(ResourceType.DIAMOND, 9),
                DeathTestSupport.fullInventory(), killerId, true);

        assertTrue(outcome.getTransfer().hasQueued());
        assertEquals(9, outcome.getTransfer().getQueued()
                .get(ResourceType.DIAMOND).intValue());
        assertTrue(outcome.getTransfer().getDelivered().isEmpty());

        Map<ResourceType, Integer> queue = transfers.getQueue(killerId);
        assertEquals(9, queue.get(ResourceType.DIAMOND).intValue());
        assertEquals(1, queue.size());
    }

    @Test
    void laCodaPremioVieneConsegnataQuandoTornaSpazio() {
        PlayerSession session = DeathTestSupport.session(victimId);
        logout(session, DeathTestSupport.inventoryOf(ResourceType.EMERALD, 4),
                DeathTestSupport.fullInventory(), killerId, true);

        Map<ResourceType, Integer> delivered = transfers.flushQueueAdapters(
                killerId, ResourceInventory.empty());

        assertEquals(4, delivered.get(ResourceType.EMERALD).intValue());
        assertFalse(transfers.hasQueue(killerId));
    }

    // ------------------------------------------------------------------
    // Equipaggiamento
    // ------------------------------------------------------------------

    @Test
    void piccioneEdAsciaScendonoDiUnSoloTier() {
        PlayerSession session = DeathTestSupport.equippedSession(victimId);
        assertSame(ToolTier.TIER_4, session.getEquipmentState().getPickaxeTier());
        assertSame(ToolTier.TIER_3, session.getEquipmentState().getAxeTier());

        logout(session, ResourceInventory.empty(), null, null, false);

        assertSame(ToolTier.TIER_3, session.getEquipmentState().getPickaxeTier());
        assertSame(ToolTier.TIER_2, session.getEquipmentState().getAxeTier());
    }

    @Test
    void laSpadaAcquistataVieneResettata() {
        PlayerSession session = DeathTestSupport.equippedSession(victimId);
        assertSame(SwordTier.DIAMOND, session.getEquipmentState().getSwordTier());

        logout(session, ResourceInventory.empty(), null, null, false);

        assertSame(SwordTier.WOOD, session.getEquipmentState().getSwordTier());
    }

    @Test
    void armaturaECesoieRestanoPermanenti() {
        PlayerSession session = DeathTestSupport.equippedSession(victimId);

        logout(session, ResourceInventory.empty(), null, null, false);

        assertSame(ArmorTier.DIAMOND, session.getEquipmentState().getArmorTier());
        assertTrue(session.getEquipmentState().ownsShears());
    }

    // ------------------------------------------------------------------
    // Idempotenza
    // ------------------------------------------------------------------

    @Test
    void unSecondoQuitNonRipeteAlcunEffetto() {
        PlayerSession session = DeathTestSupport.equippedSession(victimId);
        DeathTestSupport.CountingInventory killerInventory =
                new DeathTestSupport.CountingInventory(
                        ResourceInventory.empty());

        DeathOutcome first = logout(session,
                DeathTestSupport.inventoryOf(ResourceType.IRON, 10),
                killerInventory, killerId, true);
        DeathOutcome second = logout(session,
                DeathTestSupport.inventoryOf(ResourceType.IRON, 10),
                killerInventory, killerId, true);

        assertTrue(first.isProcessed());
        assertFalse(second.isProcessed());
        // La sequenza non avanza: la morte definitiva chiude la sessione.
        assertEquals(first.getSequence(), second.getSequence());

        // Un solo deposito: nessun secondo trasferimento.
        assertEquals(1, killerInventory.getDepositCalls());
        assertEquals(10, killerInventory.snapshot()
                .get(ResourceType.IRON).intValue());
        // Nessun secondo downgrade.
        assertSame(ToolTier.TIER_3, session.getEquipmentState().getPickaxeTier());
        assertFalse(transfers.hasQueue(killerId));
    }

    @Test
    void unEventoMorteBukkitDopoIlQuitNonRipeteGliEffetti() {
        PlayerSession session = DeathTestSupport.equippedSession(victimId);
        logout(session, DeathTestSupport.inventoryOf(ResourceType.IRON, 8),
                ResourceInventory.empty(), killerId, true);

        DeathOutcome afterwards = processor.process(
                DeathContext.of(victimId, killerId, DeathCause.COMBAT), session,
                DeathTestSupport.inventoryOf(ResourceType.IRON, 8),
                ResourceInventory.empty(),
                new DeathTestSupport.RecordingEligibility(true));

        assertFalse(afterwards.isProcessed());
        assertSame(ToolTier.TIER_3, session.getEquipmentState().getPickaxeTier());
    }

    @Test
    void unQuitDopoLEventoMorteBukkitNonRipeteGliEffetti() {
        PlayerSession session = DeathTestSupport.equippedSession(victimId);
        DeathOutcome death = processor.process(
                DeathContext.of(victimId, killerId, DeathCause.COMBAT), session,
                DeathTestSupport.inventoryOf(ResourceType.IRON, 8),
                ResourceInventory.empty(),
                new DeathTestSupport.RecordingEligibility(true));

        DeathOutcome logout = logout(session,
                DeathTestSupport.inventoryOf(ResourceType.IRON, 8),
                ResourceInventory.empty(), killerId, true);

        assertTrue(death.isProcessed());
        assertFalse(logout.isProcessed());
        assertSame(ToolTier.TIER_3, session.getEquipmentState().getPickaxeTier());
    }

    // ------------------------------------------------------------------
    // Chiusura della morte
    // ------------------------------------------------------------------

    @Test
    void ilCombatLogoutChiudeSempreLaMorte() {
        PlayerSession session = DeathTestSupport.session(victimId);

        logout(session, ResourceInventory.empty(), null, null, false);

        assertFalse(session.isDeathInProgress());
    }

    @Test
    void laMorteNormaleRestaApertaFinoAlRespawn() {
        PlayerSession session = DeathTestSupport.session(victimId);

        processor.process(
                DeathContext.of(victimId, null, DeathCause.VOID), session,
                ResourceInventory.empty(), null, KillerEligibility.NONE);

        assertTrue(session.isDeathInProgress());
    }

    @Test
    void unEccezioneNelTrasferimentoChiudeComunqueLaMorte() {
        PlayerSession session = DeathTestSupport.session(victimId);

        assertThrows(DeathTestSupport.ExplodingInventory.TransferFailure.class,
                () -> logout(session,
                        new DeathTestSupport.ExplodingInventory(),
                        ResourceInventory.empty(), killerId, true));

        assertFalse(session.isDeathInProgress());
    }

    @Test
    void unEccezioneInUnaMorteNormaleNonBloccaLaSessione() {
        PlayerSession session = DeathTestSupport.session(victimId);

        assertThrows(DeathTestSupport.ExplodingInventory.TransferFailure.class,
                () -> processor.process(
                        DeathContext.of(victimId, killerId, DeathCause.COMBAT),
                        session, new DeathTestSupport.ExplodingInventory(),
                        ResourceInventory.empty(),
                        new DeathTestSupport.RecordingEligibility(true)));

        // Senza la chiusura la morte successiva verrebbe scambiata per duplicata.
        assertFalse(session.isDeathInProgress());
    }

    // ------------------------------------------------------------------
    // Reconnect e morte successiva
    // ------------------------------------------------------------------

    @Test
    void dopoIlReconnectUnaNuovaMorteUsaUnaNuovaSequenza() {
        PlayerSession first = DeathTestSupport.equippedSession(victimId);
        DeathOutcome logout = logout(first,
                DeathTestSupport.inventoryOf(ResourceType.IRON, 6),
                ResourceInventory.empty(), killerId, true);

        // La sessione termina: il registro per giocatore va ripulito, come fa
        // Game.leave in produzione.
        transfers.clear(victimId);

        PlayerSession reconnected = DeathTestSupport.equippedSession(victimId);
        DeathTestSupport.CountingInventory killerInventory =
                new DeathTestSupport.CountingInventory(
                        ResourceInventory.empty());
        DeathOutcome second = processor.process(
                DeathContext.of(victimId, killerId, DeathCause.COMBAT),
                reconnected,
                DeathTestSupport.inventoryOf(ResourceType.IRON, 6),
                killerInventory,
                new DeathTestSupport.RecordingEligibility(true));

        assertTrue(logout.isProcessed());
        assertTrue(second.isProcessed());
        assertEquals(1, killerInventory.getDepositCalls());
        assertEquals(6, killerInventory.snapshot()
                .get(ResourceType.IRON).intValue());
        assertSame(ToolTier.TIER_3,
                reconnected.getEquipmentState().getPickaxeTier());
    }

    @Test
    void unSecondoReconnectNonRipeteGliEffetti() {
        PlayerSession session = DeathTestSupport.equippedSession(victimId);
        logout(session, ResourceInventory.empty(), null, null, false);
        transfers.clear(victimId);

        PlayerSession reconnected = DeathTestSupport.equippedSession(victimId);
        DeathOutcome first = processor.process(
                DeathContext.of(victimId, null, DeathCause.VOID), reconnected,
                ResourceInventory.empty(), null, KillerEligibility.NONE);
        DeathOutcome duplicate = processor.process(
                DeathContext.of(victimId, null, DeathCause.VOID), reconnected,
                ResourceInventory.empty(), null, KillerEligibility.NONE);

        assertTrue(first.isProcessed());
        assertFalse(duplicate.isProcessed());
        assertSame(ToolTier.TIER_3,
                reconnected.getEquipmentState().getPickaxeTier());
    }

    @Test
    void dueMortiDistinteNellaStessaSessioneUsanoSequenzeDiverse() {
        PlayerSession session = DeathTestSupport.equippedSession(victimId);

        DeathOutcome first = processor.process(
                DeathContext.of(victimId, null, DeathCause.VOID), session,
                ResourceInventory.empty(), null, KillerEligibility.NONE);
        session.completeDeath();
        transfers.clear(victimId);
        DeathOutcome second = processor.process(
                DeathContext.of(victimId, null, DeathCause.VOID), session,
                ResourceInventory.empty(), null, KillerEligibility.NONE);

        assertEquals(1L, first.getSequence());
        assertEquals(2L, second.getSequence());
        assertTrue(second.isProcessed());
        assertSame(ToolTier.TIER_2, session.getEquipmentState().getPickaxeTier());
    }

    // ------------------------------------------------------------------
    // Assenza di esperienza
    // ------------------------------------------------------------------

    @Test
    void ilCombatLogoutNonAssegnaEsperienzaDaRaccolta() {
        PlayerSession session = DeathTestSupport.session(victimId);
        PlayerSession killerSession = DeathTestSupport.session(killerId);

        logout(session, DeathTestSupport.allCurrencies(16),
                ResourceInventory.empty(), killerId, true);

        // getResourcesCollected e' il contatore alimentato dalla raccolta
        // naturale: il trasferimento non deve toccarlo.
        assertEquals(0, killerSession.getResourcesCollected());
        assertEquals(0, session.getResourcesCollected());
    }

    @Test
    void laVittimaRegistraUnaSolaMorte() {
        PlayerSession session = DeathTestSupport.session(victimId);

        logout(session, ResourceInventory.empty(), null, null, false);
        logout(session, ResourceInventory.empty(), null, null, false);

        assertEquals(1, session.getDeaths());
    }
}
