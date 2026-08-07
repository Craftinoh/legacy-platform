package it.legacynetwork.chickenwars.shop;

import it.legacynetwork.chickenwars.persistence.InMemoryQuickBuyRepository;
import it.legacynetwork.chickenwars.persistence.QuickBuyPresetRecord;
import it.legacynetwork.chickenwars.persistence.QuickBuyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Creazione, selezione ed eliminazione dei preset Quick Buy.
 *
 * <p>Ogni verifica osserva lo stato del servizio e del repository, non solo il
 * valore restituito.</p>
 */
class QuickBuyPresetManagementTest {

    private static final int LIMIT = 3;
    private static final int UNLIMITED = Integer.MAX_VALUE;

    private QuickBuyRepository repository;
    private QuickBuyService service;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        repository = new InMemoryQuickBuyRepository();
        service = new QuickBuyService(repository, Logger.getAnonymousLogger());
        playerId = UUID.randomUUID();
        service.load(playerId);
    }

    private List<String> presetIds(UUID owner) {
        List<String> ids = new ArrayList<String>();
        for (QuickBuyPresetRecord preset : service.list(owner)) {
            ids.add(preset.getPresetId());
        }
        return ids;
    }

    /**
     * Legge direttamente dal repository, ignorando la cache del servizio.
     */
    private List<String> storedIds(UUID owner) {
        CompletionStage<List<QuickBuyPresetRecord>> stage =
                repository.loadPresets(owner);
        final List<String> ids = new ArrayList<String>();
        stage.thenAccept(records -> {
            for (QuickBuyPresetRecord record : records) {
                ids.add(record.getPresetId());
            }
        });
        return ids;
    }

    private int selectedCount(UUID owner) {
        int selected = 0;
        for (QuickBuyPresetRecord preset : service.list(owner)) {
            if (preset.isSelected()) {
                selected++;
            }
        }
        return selected;
    }

    // ------------------------------------------------------------------
    // Creazione
    // ------------------------------------------------------------------

    @Test
    void siPossonoCreareFinoAlLimite() {
        assertTrue(service.create(playerId, "rush", LIMIT));
        assertTrue(service.create(playerId, "difesa", LIMIT));

        assertEquals(3, service.list(playerId).size());
        assertTrue(storedIds(playerId).contains("rush"));
    }

    @Test
    void ilQuartoPresetVieneRifiutato() {
        service.create(playerId, "rush", LIMIT);
        service.create(playerId, "difesa", LIMIT);

        assertFalse(service.create(playerId, "extra", LIMIT));
        assertEquals(3, service.list(playerId).size());
        assertFalse(storedIds(playerId).contains("extra"));
    }

    @Test
    void ilPermessoIllimitatoSuperaIlLimite() {
        service.create(playerId, "rush", LIMIT);
        service.create(playerId, "difesa", LIMIT);

        assertTrue(service.create(playerId, "extra", UNLIMITED));
        assertEquals(4, service.list(playerId).size());
    }

    @Test
    void creareDueVolteLoStessoPresetNonDuplica() {
        assertTrue(service.create(playerId, "rush", LIMIT));
        assertFalse(service.create(playerId, "rush", LIMIT));

        assertEquals(2, service.list(playerId).size());
    }

    @Test
    void ilPresetPredefinitoNonVieneDuplicato() {
        assertFalse(service.create(playerId,
                QuickBuyService.DEFAULT_PRESET, LIMIT));

        assertEquals(1, service.list(playerId).size());
    }

    @Test
    void gliIdGeneratiSonoProgressiviELiberi() {
        assertEquals("preset_2", service.nextPresetId(playerId));
        service.create(playerId, service.nextPresetId(playerId), LIMIT);
        assertEquals("preset_3", service.nextPresetId(playerId));
    }

    // ------------------------------------------------------------------
    // Selezione
    // ------------------------------------------------------------------

    @Test
    void laSelezioneLasciaUnSoloPresetAttivo() {
        service.create(playerId, "rush", LIMIT);

        assertTrue(service.select(playerId, "rush"));

        assertEquals("rush", service.getSelected(playerId).getPresetId());
        assertEquals(1, selectedCount(playerId));
    }

    @Test
    void selezionareIlPresetGiaAttivoEuIdempotente() {
        service.create(playerId, "rush", LIMIT);
        service.select(playerId, "rush");

        assertTrue(service.select(playerId, "rush"));

        assertEquals("rush", service.getSelected(playerId).getPresetId());
        assertEquals(1, selectedCount(playerId));
    }

    @Test
    void laSelezioneNonAlteraGliArticoliAssegnati() {
        service.create(playerId, "rush", LIMIT);
        service.select(playerId, "rush");
        service.assign(playerId, 19, "wool");

        service.select(playerId, QuickBuyService.DEFAULT_PRESET);
        service.select(playerId, "rush");

        assertEquals("wool",
                service.getSelected(playerId).getSlots().get(Integer.valueOf(19)));
    }

    @Test
    void unPresetInesistenteNonPuoEssereSelezionato() {
        assertFalse(service.select(playerId, "inesistente"));

        assertEquals(QuickBuyService.DEFAULT_PRESET,
                service.getSelected(playerId).getPresetId());
    }

    // ------------------------------------------------------------------
    // Eliminazione
    // ------------------------------------------------------------------

    @Test
    void eliminareUnPresetInattivoNonCambiaLAttivo() {
        service.create(playerId, "rush", LIMIT);
        service.create(playerId, "difesa", LIMIT);
        service.select(playerId, "rush");

        assertSame(QuickBuyService.DeleteResult.DELETED,
                service.delete(playerId, "difesa"));

        assertEquals("rush", service.getSelected(playerId).getPresetId());
        assertEquals(2, service.list(playerId).size());
        assertFalse(presetIds(playerId).contains("difesa"));
        assertFalse(storedIds(playerId).contains("difesa"));
    }

    @Test
    void eliminareIlPresetAttivoNeSelezionaUnAltro() {
        service.create(playerId, "rush", LIMIT);
        service.create(playerId, "difesa", LIMIT);
        service.select(playerId, "rush");

        assertSame(QuickBuyService.DeleteResult.DELETED,
                service.delete(playerId, "rush"));

        // Il primo rimasto nell'ordine di creazione diventa attivo.
        assertEquals(QuickBuyService.DEFAULT_PRESET,
                service.getSelected(playerId).getPresetId());
        assertEquals(1, selectedCount(playerId));
        assertEquals(2, service.list(playerId).size());
    }

    @Test
    void lUnicoPresetNonPuoEssereEliminato() {
        assertSame(QuickBuyService.DeleteResult.LAST_PRESET,
                service.delete(playerId, QuickBuyService.DEFAULT_PRESET));

        assertEquals(1, service.list(playerId).size());
        assertEquals(1, selectedCount(playerId));
    }

    @Test
    void eliminareUnPresetInesistenteNonAlteraNulla() {
        service.create(playerId, "rush", LIMIT);

        assertSame(QuickBuyService.DeleteResult.NOT_FOUND,
                service.delete(playerId, "inesistente"));

        assertEquals(2, service.list(playerId).size());
    }

    @Test
    void unaSecondaEliminazioneNonTrovaPiuIlPreset() {
        service.create(playerId, "rush", LIMIT);

        assertSame(QuickBuyService.DeleteResult.DELETED,
                service.delete(playerId, "rush"));
        assertSame(QuickBuyService.DeleteResult.NOT_FOUND,
                service.delete(playerId, "rush"));

        assertEquals(1, service.list(playerId).size());
    }

    @Test
    void ilPresetEliminatoNonRicompareDopoUnReload() {
        service.create(playerId, "rush", LIMIT);
        service.delete(playerId, "rush");

        service.unload(playerId);
        service.load(playerId);

        assertFalse(presetIds(playerId).contains("rush"));
        assertFalse(storedIds(playerId).contains("rush"));
        assertEquals(1, selectedCount(playerId));
    }

    @Test
    void dopoUnEliminazioneSiPuoCreareDiNuovo() {
        service.create(playerId, "rush", LIMIT);
        service.create(playerId, "difesa", LIMIT);
        assertFalse(service.create(playerId, "extra", LIMIT));

        service.delete(playerId, "difesa");

        assertTrue(service.create(playerId, "extra", LIMIT));
        assertEquals(3, service.list(playerId).size());
    }

    @Test
    void nessunaOperazioneProduceDuePresetAttivi() {
        service.create(playerId, "rush", LIMIT);
        service.create(playerId, "difesa", LIMIT);
        service.select(playerId, "difesa");
        service.delete(playerId, "difesa");
        service.create(playerId, "nuovo", LIMIT);
        service.select(playerId, "nuovo");

        assertEquals(1, selectedCount(playerId));
    }

    // ------------------------------------------------------------------
    // Isolamento e slot
    // ------------------------------------------------------------------

    @Test
    void iPresetDiUnGiocatoreNonToccanoQuelliDiUnAltro() {
        UUID other = UUID.randomUUID();
        service.load(other);
        service.create(playerId, "rush", LIMIT);
        service.create(other, "rush", LIMIT);
        service.assign(other, 19, "wool");

        service.delete(playerId, "rush");

        assertFalse(presetIds(playerId).contains("rush"));
        assertTrue(presetIds(other).contains("rush"));
        assertEquals(2, service.list(other).size());
        // Anche gli slot dell'altro giocatore restano intatti.
        assertEquals("wool", service.getSelected(other)
                .getSlots().get(Integer.valueOf(19)));
    }

    @Test
    void laRimozioneDiUnoSlotNonToccaGliAltri() {
        service.assign(playerId, 19, "wool");
        service.assign(playerId, 20, "tnt");

        assertTrue(service.assign(playerId, 19, null));

        assertEquals(1, service.getSelected(playerId).getSlots().size());
        assertEquals("tnt", service.getSelected(playerId)
                .getSlots().get(Integer.valueOf(20)));
    }

    @Test
    void rimuovereUnoSlotGiaVuotoEuIdempotente() {
        assertFalse(service.assign(playerId, 19, null));

        assertTrue(service.getSelected(playerId).getSlots().isEmpty());
    }

    @Test
    void ilLayoutRestaLegatoAlPresetNonAllaModalita() {
        service.assign(playerId, 19, "wool");
        service.create(playerId, "rush", LIMIT);
        service.select(playerId, "rush");

        // Il nuovo preset parte vuoto...
        assertTrue(service.getSelected(playerId).getSlots().isEmpty());

        service.select(playerId, QuickBuyService.DEFAULT_PRESET);

        // ...e quello precedente conserva la propria disposizione.
        assertEquals("wool", service.getSelected(playerId)
                .getSlots().get(Integer.valueOf(19)));
    }
}
