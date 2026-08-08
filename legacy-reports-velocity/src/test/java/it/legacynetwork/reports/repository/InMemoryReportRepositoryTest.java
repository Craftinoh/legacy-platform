package it.legacynetwork.reports.repository;

import it.legacynetwork.reports.model.Report;
import it.legacynetwork.reports.model.ReportId;
import it.legacynetwork.reports.model.ReportSnapshot;
import it.legacynetwork.reports.model.ReportStatus;
import it.legacynetwork.reports.support.ReportsTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lo storage in memoria deve rispettare lo stesso contratto di quello JDBC:
 * altrimenti i test che lo usano proverebbero un'altra cosa.
 */
class InMemoryReportRepositoryTest {

    private static final Instant NOW = ReportsTestSupport.NOW;

    private InMemoryReportRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryReportRepository();
    }

    private Report report(UUID reporterId, UUID targetId, String targetName,
                          ReportStatus status, Instant createdAt) {
        return Report.builder()
                .id(ReportId.random())
                .reporter(reporterId, "Reporter")
                .target(targetId, targetName)
                .reasonId("cheating")
                .snapshot(new ReportSnapshot("lobby-1", 42L, "proxy",
                        createdAt))
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .status(status)
                .revision(0L)
                .build();
    }

    @Test
    void inserimentoELettura() {
        Report stored = report(UUID.randomUUID(), UUID.randomUUID(), "Target",
                ReportStatus.OPEN, NOW);

        repository.insert(stored).join();

        assertEquals(stored.getId(),
                repository.find(stored.getId()).join().orElseThrow().getId());
        assertFalse(repository.find(ReportId.random()).join().isPresent());
    }

    @Test
    void unPrefissoAmbiguoNonSelezionaNessuno() {
        Report stored = report(UUID.randomUUID(), UUID.randomUUID(), "Target",
                ReportStatus.OPEN, NOW);
        repository.insert(stored).join();

        // Un prefisso di un solo carattere e' condiviso da tante chiavi
        // possibili: qui ce n'e' una sola, quindi deve comunque risolvere.
        assertTrue(repository.findByReference(
                stored.getId().shortCode()).join().isPresent());
        assertFalse(repository.findByReference("nonesadecimale").join()
                .isPresent());
    }

    @Test
    void laPaginazioneSeguaLOrdineCronologicoInverso() {
        for (int index = 0; index < 3; index++) {
            repository.insert(report(UUID.randomUUID(), UUID.randomUUID(),
                    "Target" + index, ReportStatus.OPEN,
                    NOW.plusSeconds(index))).join();
        }

        ReportPage page = repository.listByStatuses(
                EnumSet.of(ReportStatus.OPEN), 1, 2).join();

        assertEquals(2, page.getItems().size());
        assertEquals(3L, page.getTotalItems());
        assertEquals("Target2", page.getItems().get(0).getTargetName());
        assertTrue(repository.listByStatuses(EnumSet.of(ReportStatus.OPEN), 9,
                2).join().isEmpty());
    }

    @Test
    void lAggiornamentoRispettaStatoERevisione() {
        Report stored = report(UUID.randomUUID(), UUID.randomUUID(), "Target",
                ReportStatus.OPEN, NOW);
        repository.insert(stored).join();
        Report claimed = stored.toBuilder()
                .status(ReportStatus.CLAIMED)
                .revision(1L)
                .build();

        assertTrue(repository.update(claimed, ReportStatus.OPEN, 0L).join());
        assertFalse(repository.update(claimed, ReportStatus.OPEN, 0L).join());
        assertEquals(ReportStatus.CLAIMED,
                repository.find(stored.getId()).join().orElseThrow()
                        .getStatus());
    }

    @Test
    void loStessoReportNonSiInseriscePiuDiUnaVolta() {
        Report stored = report(UUID.randomUUID(), UUID.randomUUID(), "Target",
                ReportStatus.OPEN, NOW);
        repository.insert(stored).join();

        assertTrue(repository.insert(stored)
                .handle((value, failure) -> failure != null).join());
    }
}
