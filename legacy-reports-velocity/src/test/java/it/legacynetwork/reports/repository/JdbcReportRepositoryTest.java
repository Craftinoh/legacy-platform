package it.legacynetwork.reports.repository;

import it.legacynetwork.reports.api.Report;
import it.legacynetwork.reports.model.ReportEvent;
import it.legacynetwork.reports.api.ReportEventType;
import it.legacynetwork.reports.api.ReportId;
import it.legacynetwork.reports.api.ReportSnapshot;
import it.legacynetwork.reports.api.ReportStatus;
import it.legacynetwork.reports.support.ReportsTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Repository JDBC contro un database reale.
 *
 * <p>Lo schema e' scritto in SQL portabile, quindi le stesse migrazioni spedite
 * nell'artefatto girano qui su SQLite: il test verifica le query vere, non una
 * loro imitazione.</p>
 */
class JdbcReportRepositoryTest {

    private static final Instant NOW = ReportsTestSupport.NOW;

    private JdbcReportRepository repository;
    private JdbcReportEventRepository events;

    @BeforeEach
    void setUp(@TempDir Path directory) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:"
                + directory.resolve("reports.db").toAbsolutePath());
        DataSource source = dataSource;
        assertEquals(1, new ReportSchemaMigrator(source).migrate());
        // Una seconda esecuzione non deve riapplicare nulla.
        assertEquals(0, new ReportSchemaMigrator(source).migrate());
        repository = new JdbcReportRepository(source, Runnable::run);
        events = new JdbcReportEventRepository(source, Runnable::run);
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
    void unReportInseritoSiRileggeIdentico() {
        UUID staffId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Report stored = Report.builder()
                .id(ReportId.random())
                .reporter(UUID.randomUUID(), "Reporter")
                .target(UUID.randomUUID(), "Target")
                .reasonId("other")
                .details("dettagli completi")
                .snapshot(new ReportSnapshot("bedwars-2", 137L, "proxy-1", NOW))
                .createdAt(NOW)
                .updatedAt(NOW.plusSeconds(30))
                .status(ReportStatus.SCREENSHARE)
                .assignedStaff(staffId, "Staff")
                .resolution("in corso")
                .punishmentId("pun-1")
                .screenshareId(sessionId)
                .revision(4L)
                .build();

        repository.insert(stored).join();
        Report read = repository.find(stored.getId()).join().orElseThrow();

        assertEquals(stored.getId(), read.getId());
        assertEquals(stored.getReporterId(), read.getReporterId());
        assertEquals("Target", read.getTargetName());
        assertEquals("other", read.getReasonId());
        assertEquals("dettagli completi", read.getDetails().orElse(null));
        assertEquals("bedwars-2", read.getSnapshot().getServerId());
        assertEquals(137L, read.getSnapshot().getTargetPingMillis());
        assertEquals("proxy-1", read.getSnapshot().getProxyId());
        assertEquals(NOW, read.getCreatedAt());
        assertEquals(NOW.plusSeconds(30), read.getUpdatedAt());
        assertEquals(ReportStatus.SCREENSHARE, read.getStatus());
        assertEquals(staffId, read.getAssignedStaffId().orElse(null));
        assertEquals("Staff", read.getAssignedStaffName().orElse(null));
        assertEquals("in corso", read.getResolution().orElse(null));
        assertEquals("pun-1", read.getPunishmentId().orElse(null));
        assertEquals(sessionId, read.getScreenshareId().orElse(null));
        assertEquals(4L, read.getRevision());
    }

    @Test
    void ilRiferimentoBreveTrovaIlReport() {
        Report stored = report(UUID.randomUUID(), UUID.randomUUID(), "Target",
                ReportStatus.OPEN, NOW);
        repository.insert(stored).join();

        assertEquals(stored.getId(), repository
                .findByReference(stored.getId().shortCode()).join()
                .orElseThrow().getId());
        assertEquals(stored.getId(), repository
                .findByReference(stored.getId().toString()).join()
                .orElseThrow().getId());
        assertFalse(repository.findByReference("zzzz").join().isPresent());
        assertFalse(repository.findByReference("").join().isPresent());
    }

    @Test
    void lElencoEuPaginato() {
        for (int index = 0; index < 5; index++) {
            repository.insert(report(UUID.randomUUID(), UUID.randomUUID(),
                    "Target" + index, ReportStatus.OPEN,
                    NOW.plusSeconds(index))).join();
        }

        ReportPage first = repository.listByStatuses(
                EnumSet.of(ReportStatus.OPEN), 1, 2).join();
        ReportPage last = repository.listByStatuses(
                EnumSet.of(ReportStatus.OPEN), 3, 2).join();

        assertEquals(2, first.getItems().size());
        assertEquals(5L, first.getTotalItems());
        assertEquals(3, first.getTotalPages());
        assertTrue(first.hasNextPage());
        assertFalse(first.hasPreviousPage());
        assertEquals(1, last.getItems().size());
        assertFalse(last.hasNextPage());
        // Ordine dal piu' recente.
        assertEquals("Target4", first.getItems().get(0).getTargetName());
    }

    @Test
    void loStoricoDelGiocatoreFiltraSoloISuoiReport() {
        UUID targetId = UUID.randomUUID();
        repository.insert(report(UUID.randomUUID(), targetId, "Target",
                ReportStatus.OPEN, NOW)).join();
        repository.insert(report(UUID.randomUUID(), targetId, "Target",
                ReportStatus.DISMISSED, NOW.plusSeconds(1))).join();
        repository.insert(report(UUID.randomUUID(), UUID.randomUUID(), "Altro",
                ReportStatus.OPEN, NOW)).join();

        ReportPage page = repository.listByTarget(targetId, 1, 10).join();

        assertEquals(2, page.getItems().size());
        assertEquals(2L, page.getTotalItems());
    }

    @Test
    void ilConteggioSiLimitaAgliStatiRichiesti() {
        UUID reporterId = UUID.randomUUID();
        repository.insert(report(reporterId, UUID.randomUUID(), "A",
                ReportStatus.OPEN, NOW)).join();
        repository.insert(report(reporterId, UUID.randomUUID(), "B",
                ReportStatus.DISMISSED, NOW)).join();

        assertEquals(1, repository.countByReporter(reporterId,
                EnumSet.of(ReportStatus.OPEN)).join());
        assertEquals(2, repository.countByReporter(reporterId,
                EnumSet.allOf(ReportStatus.class)).join());
    }

    @Test
    void ilDuplicatoRecenteVieneTrovato() {
        UUID reporterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        repository.insert(report(reporterId, targetId, "Target",
                ReportStatus.OPEN, NOW)).join();

        Optional<Report> recent = repository.findRecentDuplicate(reporterId,
                targetId, NOW.minus(Duration.ofMinutes(30))).join();
        Optional<Report> old = repository.findRecentDuplicate(reporterId,
                targetId, NOW.plus(Duration.ofMinutes(1))).join();

        assertTrue(recent.isPresent());
        assertFalse(old.isPresent());
    }

    @Test
    void lAggiornamentoEuCondizionatoAStatoERevisione() {
        Report stored = report(UUID.randomUUID(), UUID.randomUUID(), "Target",
                ReportStatus.OPEN, NOW);
        repository.insert(stored).join();

        Report claimed = stored.toBuilder()
                .status(ReportStatus.CLAIMED)
                .assignedStaff(UUID.randomUUID(), "Staff")
                .updatedAt(NOW.plusSeconds(5))
                .revision(1L)
                .build();

        assertTrue(repository.update(claimed, ReportStatus.OPEN, 0L).join());
        // Lo stesso aggiornamento, ripetuto sulla revisione ormai vecchia.
        assertFalse(repository.update(claimed, ReportStatus.OPEN, 0L).join());

        Report read = repository.find(stored.getId()).join().orElseThrow();
        assertEquals(ReportStatus.CLAIMED, read.getStatus());
        assertEquals(1L, read.getRevision());
    }

    @Test
    void unAggiornamentoRifiutatoNonLasciaTracce() {
        Report stored = report(UUID.randomUUID(), UUID.randomUUID(), "Target",
                ReportStatus.OPEN, NOW);
        repository.insert(stored).join();

        Report wrong = stored.toBuilder()
                .status(ReportStatus.DISMISSED)
                .resolution("mai applicata")
                .revision(9L)
                .build();

        assertFalse(repository.update(wrong, ReportStatus.INVESTIGATING, 7L)
                .join());
        Report read = repository.find(stored.getId()).join().orElseThrow();
        assertEquals(ReportStatus.OPEN, read.getStatus());
        assertFalse(read.getResolution().isPresent());
        assertEquals(0L, read.getRevision());
    }

    @Test
    void gliEventiSiRileggonoDalPiuRecente() {
        Report stored = report(UUID.randomUUID(), UUID.randomUUID(), "Target",
                ReportStatus.OPEN, NOW);
        repository.insert(stored).join();
        events.append(new ReportEvent(UUID.randomUUID(), stored.getId(), null,
                "system", ReportEventType.CREATED, null, ReportStatus.OPEN,
                null, "proxy", NOW)).join();
        UUID staffId = UUID.randomUUID();
        events.append(new ReportEvent(UUID.randomUUID(), stored.getId(),
                staffId, "Staff", ReportEventType.CLAIMED, ReportStatus.OPEN,
                ReportStatus.CLAIMED, "preso", "proxy",
                NOW.plusSeconds(10))).join();

        List<ReportEvent> history =
                events.findByReport(stored.getId(), 10).join();

        assertEquals(2, history.size());
        assertEquals(ReportEventType.CLAIMED, history.get(0).getType());
        assertEquals(staffId, history.get(0).getActorId().orElse(null));
        assertEquals("preso", history.get(0).getMessage().orElse(null));
        assertEquals(ReportStatus.OPEN,
                history.get(0).getPreviousStatus().orElse(null));
        assertFalse(history.get(1).getActorId().isPresent());
        assertEquals(1, events.findByReport(stored.getId(), 1).join().size());
    }

    @Test
    void unReportInesistenteNonVieneTrovato() {
        assertFalse(repository.find(ReportId.random()).join().isPresent());
        assertTrue(events.findByReport(ReportId.random(), 10).join().isEmpty());
    }
}
