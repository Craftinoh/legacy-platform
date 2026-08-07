package it.legacynetwork.chickenwars.region;

import it.legacynetwork.chickenwars.model.SimpleLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Appartenenza a una regione base, verificata senza server.
 */
class CuboidRegionTest {

    private static final String WORLD = "cw_arena";

    private SimpleLocation at(String world, double x, double y, double z) {
        return new SimpleLocation(world, x, y, z, 0.0F, 0.0F);
    }

    private CuboidRegion region() {
        return CuboidRegion.between(at(WORLD, 10.0D, 60.0D, 10.0D),
                at(WORLD, 20.0D, 70.0D, 20.0D));
    }

    @Test
    void gliAngoliPossonoEssereIndicatiInQualunqueOrdine() {
        CuboidRegion reversed = CuboidRegion.between(
                at(WORLD, 20.0D, 70.0D, 20.0D),
                at(WORLD, 10.0D, 60.0D, 10.0D));

        assertNotNull(reversed);
        assertEquals(10.0D, reversed.getMinX(), 0.0001D);
        assertEquals(20.0D, reversed.getMaxX(), 0.0001D);
        assertTrue(reversed.contains(WORLD, 15.0D, 65.0D, 15.0D));
    }

    @Test
    void unPuntoInternoAppartieneAllaRegione() {
        assertTrue(region().contains(WORLD, 15.0D, 65.0D, 15.0D));
    }

    @Test
    void gliAngoliSonoInclusi() {
        CuboidRegion region = region();

        assertTrue(region.contains(WORLD, 10.0D, 60.0D, 10.0D));
        // Il massimo copre l'intero blocco indicato.
        assertTrue(region.contains(WORLD, 21.0D, 71.0D, 21.0D));
        assertFalse(region.contains(WORLD, 21.5D, 65.0D, 15.0D));
    }

    @Test
    void unPuntoEsternoNonAppartiene() {
        CuboidRegion region = region();

        assertFalse(region.contains(WORLD, 9.0D, 65.0D, 15.0D));
        assertFalse(region.contains(WORLD, 15.0D, 59.0D, 15.0D));
        assertFalse(region.contains(WORLD, 15.0D, 65.0D, 9.0D));
    }

    @Test
    void unMondoDiversoNonAppartieneMai() {
        assertFalse(region().contains("altro", 15.0D, 65.0D, 15.0D));
        assertFalse(region().contains(null, 15.0D, 65.0D, 15.0D));
    }

    @Test
    void ilConfrontoSulMondoIgnoraLeMaiuscole() {
        assertTrue(region().contains("CW_ARENA", 15.0D, 65.0D, 15.0D));
    }

    @Test
    void angoliInMondiDiversiNonFormanoUnaRegione() {
        assertNull(CuboidRegion.between(at(WORLD, 0.0D, 0.0D, 0.0D),
                at("altro", 10.0D, 10.0D, 10.0D)));
    }

    @Test
    void angoliMancantiNonFormanoUnaRegione() {
        assertNull(CuboidRegion.between(null, at(WORLD, 1.0D, 1.0D, 1.0D)));
        assertNull(CuboidRegion.between(at(WORLD, 1.0D, 1.0D, 1.0D), null));
    }

    @Test
    void ilVolumeContaIBlocchiInclusi() {
        CuboidRegion single = CuboidRegion.between(
                at(WORLD, 0.0D, 0.0D, 0.0D), at(WORLD, 0.0D, 0.0D, 0.0D));

        assertNotNull(single);
        assertEquals(1.0D, single.getVolume(), 0.0001D);
        assertEquals(11.0D * 11.0D * 11.0D, region().getVolume(), 0.0001D);
    }

    @Test
    void leRegioniSovrapposteVengonoRilevate() {
        CuboidRegion first = region();
        CuboidRegion overlapping = CuboidRegion.between(
                at(WORLD, 18.0D, 65.0D, 18.0D),
                at(WORLD, 30.0D, 75.0D, 30.0D));
        CuboidRegion separate = CuboidRegion.between(
                at(WORLD, 100.0D, 60.0D, 100.0D),
                at(WORLD, 110.0D, 70.0D, 110.0D));

        assertTrue(first.overlaps(overlapping));
        assertFalse(first.overlaps(separate));
        assertFalse(first.overlaps(null));
    }

    @Test
    void regioniDiMondiDiversiNonSiSovrappongono() {
        CuboidRegion elsewhere = CuboidRegion.between(
                at("altro", 10.0D, 60.0D, 10.0D),
                at("altro", 20.0D, 70.0D, 20.0D));

        assertFalse(region().overlaps(elsewhere));
    }

    @Test
    void unaRegioneValidaEuUtilizzabile() {
        assertTrue(region().isUsable());
    }
}
