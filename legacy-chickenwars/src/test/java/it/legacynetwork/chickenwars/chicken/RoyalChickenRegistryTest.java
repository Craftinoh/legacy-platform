package it.legacynetwork.chickenwars.chicken;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registro autorevole delle entita' Gallina Reale.
 */
class RoyalChickenRegistryTest {

    private RoyalChickenRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new RoyalChickenRegistry();
    }

    @Test
    void unaGallinaVieneRitrovataDallEntita() {
        UUID entity = UUID.randomUUID();

        RoyalChickenRegistry.Entry entry =
                registry.register(entity, "farm", "red");

        assertNotNull(entry);
        RoyalChickenRegistry.Entry found = registry.lookup(entity);
        assertNotNull(found);
        assertEquals("farm", found.getArenaId());
        assertEquals("red", found.getTeamId());
        assertTrue(registry.isRoyalChicken(entity));
    }

    @Test
    void unEntitaSconosciutaNonEuUnaGallina() {
        registry.register(UUID.randomUUID(), "farm", "red");

        assertNull(registry.lookup(UUID.randomUUID()));
        assertFalse(registry.isRoyalChicken(UUID.randomUUID()));
        assertFalse(registry.isRoyalChicken(null));
    }

    @Test
    void ogniSquadraHaUnaSolaGallina() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        registry.register(first, "farm", "red");
        registry.register(second, "farm", "red");

        // La registrazione e' idempotente rispetto alla squadra: la vecchia
        // entita' non resta nel registro.
        assertFalse(registry.isRoyalChicken(first));
        assertTrue(registry.isRoyalChicken(second));
        assertEquals(second, registry.getEntity("farm", "red"));
        assertEquals(1, registry.size());
    }

    @Test
    void registrareDueVolteLaStessaEntitaNonDuplica() {
        UUID entity = UUID.randomUUID();

        registry.register(entity, "farm", "red");
        registry.register(entity, "farm", "red");

        assertEquals(1, registry.size());
        assertEquals(entity, registry.getEntity("farm", "red"));
    }

    @Test
    void dueSquadreConvivonoNellaStessaArena() {
        UUID red = UUID.randomUUID();
        UUID blue = UUID.randomUUID();

        registry.register(red, "farm", "red");
        registry.register(blue, "farm", "blue");

        assertEquals(2, registry.size());
        assertEquals(red, registry.getEntity("farm", "red"));
        assertEquals(blue, registry.getEntity("farm", "blue"));
    }

    @Test
    void squadreOmonimeDiAreneDiverseNonSiConfondono() {
        UUID farmRed = UUID.randomUUID();
        UUID castleRed = UUID.randomUUID();

        registry.register(farmRed, "farm", "red");
        registry.register(castleRed, "castle", "red");

        assertEquals(2, registry.size());
        assertEquals(farmRed, registry.getEntity("farm", "red"));
        assertEquals(castleRed, registry.getEntity("castle", "red"));
        assertSame("castle", registry.lookup(castleRed).getArenaId());
    }

    @Test
    void laDeregistrazioneLiberaSiaEntitaCheSquadra() {
        UUID entity = UUID.randomUUID();
        registry.register(entity, "farm", "red");

        assertTrue(registry.unregister(entity));

        assertFalse(registry.isRoyalChicken(entity));
        assertNull(registry.getEntity("farm", "red"));
        assertEquals(0, registry.size());
    }

    @Test
    void deregistrareDueVolteNonFallisce() {
        UUID entity = UUID.randomUUID();
        registry.register(entity, "farm", "red");

        assertTrue(registry.unregister(entity));
        assertFalse(registry.unregister(entity));
        assertFalse(registry.unregister(null));
    }

    @Test
    void laPuliziaDellArenaNonToccaLeAltre() {
        registry.register(UUID.randomUUID(), "farm", "red");
        registry.register(UUID.randomUUID(), "farm", "blue");
        UUID castle = UUID.randomUUID();
        registry.register(castle, "castle", "red");

        assertEquals(2, registry.clearArena("farm"));

        assertEquals(1, registry.size());
        assertTrue(registry.isRoyalChicken(castle));
        assertNull(registry.getEntity("farm", "red"));
    }

    @Test
    void unArenaRiutilizzataRipartePulita() {
        registry.register(UUID.randomUUID(), "farm", "red");
        registry.clearArena("farm");

        UUID fresh = UUID.randomUUID();
        registry.register(fresh, "farm", "red");

        assertEquals(1, registry.size());
        assertEquals(fresh, registry.getEntity("farm", "red"));
    }

    @Test
    void laPuliziaCompletaAzzeraIlRegistro() {
        registry.register(UUID.randomUUID(), "farm", "red");
        registry.register(UUID.randomUUID(), "castle", "blue");

        registry.clearAll();

        assertEquals(0, registry.size());
        assertNull(registry.getEntity("farm", "red"));
    }

    @Test
    void iDatiIncompletiNonVengonoRegistrati() {
        assertNull(registry.register(null, "farm", "red"));
        assertNull(registry.register(UUID.randomUUID(), null, "red"));
        assertNull(registry.register(UUID.randomUUID(), "farm", null));
        assertEquals(0, registry.size());
    }
}
