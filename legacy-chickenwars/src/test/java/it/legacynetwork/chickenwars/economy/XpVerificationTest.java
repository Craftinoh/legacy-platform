package it.legacynetwork.chickenwars.economy;

import it.legacynetwork.chickenwars.death.DeathCause;
import it.legacynetwork.chickenwars.death.DeathContext;
import it.legacynetwork.chickenwars.death.KillerEligibility;
import it.legacynetwork.chickenwars.death.PlayerDeathProcessor;
import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.player.equipment.EquipmentService;
import it.legacynetwork.chickenwars.player.equipment.EquipmentSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Separazione tra raccolta naturale e trasferimento alla morte.
 *
 * <p>Il contatore alimentato dalla raccolta naturale e'
 * {@code PlayerSession.getResourcesCollected()}: e' lui che finisce nelle
 * statistiche e nella progressione. I test osservano quel contatore, non la
 * semplice assenza di eccezioni.</p>
 */
class XpVerificationTest {

    private ResourceTransferService transfers;
    private PlayerDeathProcessor processor;
    private PlayerSession victimSession;
    private PlayerSession killerSession;
    private UUID victimId;
    private UUID killerId;

    @BeforeEach
    void setUp() {
        transfers = new ResourceTransferService();
        EquipmentService equipment =
                new EquipmentService(EquipmentSettings.fromSection(null));
        processor = new PlayerDeathProcessor(transfers, equipment);
        victimId = UUID.randomUUID();
        killerId = UUID.randomUUID();
        victimSession = new PlayerSession(victimId, "Victim", "arena", null);
        killerSession = new PlayerSession(killerId, "Killer", "arena", null);
    }

    private ResourceInventory withAll(int amount) {
        Map<ResourceType, Integer> contents =
                new HashMap<ResourceType, Integer>();
        for (ResourceType type : ResourceType.values()) {
            contents.put(type, Integer.valueOf(amount));
        }
        return ResourceInventory.with(contents);
    }

    /**
     * Riferimento: la raccolta naturale alimenta davvero il contatore.
     *
     * <p>Senza questo confronto gli altri test proverebbero solo che il
     * contatore non viene mai toccato da nessuno.</p>
     */
    @Test
    void laRaccoltaNaturaleAlimentaIlContatore() {
        killerSession.addResources(9);

        assertEquals(9, killerSession.getResourcesCollected());
    }

    @Test
    void laMorteNormaleNonAssegnaEsperienzaDaRaccolta() {
        ResourceInventory killerInventory = ResourceInventory.empty();

        processor.process(
                DeathContext.of(victimId, killerId, DeathCause.COMBAT),
                victimSession, withAll(12), killerInventory,
                KillerEligibility.ALWAYS);

        // Le risorse sono realmente arrivate...
        assertEquals(12, killerInventory.count(ResourceType.IRON));
        // ...ma nessun contatore di raccolta e' stato toccato.
        assertEquals(0, killerSession.getResourcesCollected());
        assertEquals(0, victimSession.getResourcesCollected());
    }

    @Test
    void ilCombatLogoutNonAssegnaEsperienzaDaRaccolta() {
        ResourceInventory killerInventory = ResourceInventory.empty();

        processor.process(DeathContext.combatLogout(victimId, killerId),
                victimSession, withAll(20), killerInventory,
                KillerEligibility.ALWAYS);

        assertEquals(20, killerInventory.count(ResourceType.EMERALD));
        assertEquals(0, killerSession.getResourcesCollected());
        assertEquals(0, victimSession.getResourcesCollected());
    }

    @Test
    void laCodaPremioNonAssegnaEsperienzaDaRaccolta() {
        ResourceInventory.MapInventory full =
                (ResourceInventory.MapInventory) ResourceInventory.empty();
        full.setMaxSlots(0);

        processor.process(DeathContext.combatLogout(victimId, killerId),
                victimSession, withAll(6), full, KillerEligibility.ALWAYS);
        assertTrue(transfers.hasQueue(killerId));

        Map<ResourceType, Integer> delivered = transfers.flushQueueAdapters(
                killerId, ResourceInventory.empty());

        assertEquals(6, delivered.get(ResourceType.GOLD).intValue());
        assertEquals(0, killerSession.getResourcesCollected());
    }

    @Test
    void unaMorteSenzaUccisoreNonProduceAlcunGuadagno() {
        ResourceInventory victimInventory = withAll(15);

        processor.process(
                DeathContext.of(victimId, null, DeathCause.VOID),
                victimSession, victimInventory, null, KillerEligibility.NONE);

        // Le risorse spariscono senza premiare nessuno.
        assertEquals(0, victimInventory.count(ResourceType.IRON));
        assertEquals(0, victimSession.getResourcesCollected());
        assertEquals(0, killerSession.getResourcesCollected());
        assertFalse(transfers.hasQueue(killerId));
    }

    @Test
    void unEventoDuplicatoNonGeneraUnSecondoGuadagno() {
        ResourceInventory killerInventory = ResourceInventory.empty();
        DeathContext context =
                DeathContext.of(victimId, killerId, DeathCause.COMBAT);

        processor.process(context, victimSession, withAll(5), killerInventory,
                KillerEligibility.ALWAYS);
        processor.process(context, victimSession, withAll(5), killerInventory,
                KillerEligibility.ALWAYS);

        assertEquals(5, killerInventory.count(ResourceType.DIAMOND));
        assertEquals(0, killerSession.getResourcesCollected());
    }
}
