package it.legacynetwork.chickenwars.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimpleLocationTest {

    private static final double DELTA = 0.0001D;

    @Test
    void interpretaIlFormatoCompleto() {
        SimpleLocation location =
                SimpleLocation.parse("cw_farm,-80.5,65,0.5,90,-10");

        assertNotNull(location);
        assertEquals("cw_farm", location.getWorld());
        assertEquals(-80.5D, location.getX(), DELTA);
        assertEquals(65.0D, location.getY(), DELTA);
        assertEquals(0.5D, location.getZ(), DELTA);
        assertEquals(90.0F, location.getYaw(), DELTA);
        assertEquals(-10.0F, location.getPitch(), DELTA);
    }

    @Test
    void interpretaIlFormatoRidottoAzzerandoLaRotazione() {
        SimpleLocation location = SimpleLocation.parse("cw_farm,10,64,20");

        assertNotNull(location);
        assertEquals(0.0F, location.getYaw(), DELTA);
        assertEquals(0.0F, location.getPitch(), DELTA);
    }

    @Test
    void ignoraGliSpaziAttornoAiValori() {
        SimpleLocation location = SimpleLocation.parse(" cw_farm , 10 , 64 , 20 ");

        assertNotNull(location);
        assertEquals("cw_farm", location.getWorld());
        assertEquals(10.0D, location.getX(), DELTA);
    }

    @Test
    void rifiutaIFormatiNonValidi() {
        assertNull(SimpleLocation.parse(null));
        assertNull(SimpleLocation.parse(""));
        assertNull(SimpleLocation.parse("cw_farm,10,64"));
        assertNull(SimpleLocation.parse("cw_farm,10,64,20,0"));
        assertNull(SimpleLocation.parse("cw_farm,dieci,64,20"));
        assertNull(SimpleLocation.parse(",10,64,20"));
    }

    @Test
    void laSerializzazioneEReversibile() {
        SimpleLocation original =
                new SimpleLocation("cw_farm", -80.5D, 65.0D, 0.5D, 90.0F, 0.0F);

        SimpleLocation parsed = SimpleLocation.parse(original.serialize());

        assertEquals(original, parsed);
        assertEquals(original.hashCode(), parsed.hashCode());
    }

    @Test
    void centraLaPosizioneSulBlocco() {
        SimpleLocation centered =
                new SimpleLocation("cw_farm", 10.2D, 65.0D, -3.8D, 0.0F, 0.0F)
                        .centered();

        assertEquals(10.5D, centered.getX(), DELTA);
        assertEquals(-3.5D, centered.getZ(), DELTA);
        assertEquals(65.0D, centered.getY(), DELTA);
    }

    @Test
    void ilNomeDelMondoEObbligatorio() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimpleLocation("  ", 0.0D, 0.0D, 0.0D, 0.0F, 0.0F));
    }
}
