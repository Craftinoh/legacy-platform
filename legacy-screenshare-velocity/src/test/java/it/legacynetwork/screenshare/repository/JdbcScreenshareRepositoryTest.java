package it.legacynetwork.screenshare.repository;

import it.legacynetwork.screenshare.model.ScreenshareEvent;
import it.legacynetwork.screenshare.model.ScreenshareEventType;
import it.legacynetwork.screenshare.model.ScreenshareOutcome;
import it.legacynetwork.screenshare.model.ScreenshareSession;
import it.legacynetwork.screenshare.model.ScreenshareSessionId;
import it.legacynetwork.screenshare.model.ScreenshareStatus;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Repository JDBC contro un database reale.
 *
 * <p>Lo schema e' SQL portabile, quindi le migrazioni spedite nell'artefatto
 * girano qui su SQLite: il test verifica le query vere.</p>
 */
class JdbcScreenshareRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    private JdbcScreenshareRepository repository;
    private JdbcScreenshareEventRepository events;

    @BeforeEach
    void setUp(@TempDir Path directory) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:"
                + directory.resolve("screenshare.db").toAbsolutePath());
        DataSource source = dataSource;
        assertEquals(1, new ScreenshareSchemaMigrator(source).migrate());
        assertEquals(0, new ScreenshareSchemaMigrator(source).migrate());
        repository = new JdbcScreenshareRepository(source, Runnable::run);
        events = new JdbcScreenshareEventRepository(source, Runnable::run);
    }

    private ScreenshareSession session(UUID targetId, String targetName,
                                       UUID staffId, ScreenshareStatus status,
                                       Instant createdAt) {
        return ScreenshareSession.builder()
                .id(ScreenshareSessionId.random())
                .target(targetId, targetName)
                .staff(staffId, "Staff")
                .serverId("screenshare-1")
                .createdAt(createdAt)
                .expiresAt(createdAt.plus(Duration.ofHours(1)))
                .status(status)
                .proxyId("proxy-test")
                .revision(0L)
                .build();
    }

    @Test
    void unaSessioneInseritaSiRileggeIdentica() {
        UUID reportId = UUID.randomUUID();
        ScreenshareSession stored = ScreenshareSession.builder()
                .id(ScreenshareSessionId.random())
                .target(UUID.randomUUID(), "Target")
                .staff(UUID.randomUUID(), "Staff")
                .reportId(reportId)
                .serverId("screenshare-2")
                .createdAt(NOW)
                .startedAt(NOW.plusSeconds(10))
                .expiresAt(NOW.plusSeconds(3600))
                .endedAt(NOW.plusSeconds(900))
                .status(ScreenshareStatus.COMPLETED)
                .outcome(ScreenshareOutcome.CLEAN)
                .notes("tutto in ordine")
                .proxyId("proxy-1")
                .revision(3L)
                .build();

        repository.insert(stored).join();
        ScreenshareSession read =
                repository.find(stored.getId()).join().orElseThrow();

        assertEquals(stored.getId(), read.getId());
        assertEquals("Target", read.getTargetName());
        assertEquals("Staff", read.getStaffName());
        assertEquals(reportId, read.getReportId().orElse(null));
        assertEquals("screenshare-2", read.getServerId());
        assertEquals(NOW, read.getCreatedAt());
        assertEquals(NOW.plusSeconds(10), read.getStartedAt().orElse(null));
        assertEquals(NOW.plusSeconds(3600), read.getExpiresAt());
        assertEquals(NOW.plusSeconds(900), read.getEndedAt().orElse(null));
        assertEquals(ScreenshareStatus.COMPLETED, read.getStatus());
        assertEquals(ScreenshareOutcome.CLEAN, read.getOutcome().orElse(null));
        assertEquals("tutto in ordine", read.getNotes().orElse(null));
        assertEquals(3L, read.getRevision());
    }

    @Test
    void unaSessioneSenzaMomentiOpzionaliNonLiInventa() {
        ScreenshareSession stored = session(UUID.randomUUID(), "Target",
                UUID.randomUUID(), ScreenshareStatus.CREATED, NOW);

        repository.insert(stored).join();
        ScreenshareSession read =
                repository.find(stored.getId()).join().orElseThrow();

        assertFalse(read.getStartedAt().isPresent());
        assertFalse(read.getEndedAt().isPresent());
        assertFalse(read.getOutcome().isPresent());
        assertFalse(read.getReportId().isPresent());
    }

    @Test
    void leSessioniAperteSiTrovanoPerBersaglioEStaff() {
        UUID targetId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        repository.insert(session(targetId, "Target", staffId,
                ScreenshareStatus.ACTIVE, NOW)).join();
        repository.insert(session(UUID.randomUUID(), "Altro",
                UUID.randomUUID(), ScreenshareStatus.COMPLETED, NOW)).join();

        assertTrue(repository.findOpenByTarget(targetId).join().isPresent());
        assertEquals(1, repository.findOpenByStaff(staffId).join().size());
        assertEquals(1, repository.findOpen().join().size());
    }

    @Test
    void unaSessioneChiusaNonRisultaAperta() {
        UUID targetId = UUID.randomUUID();
        repository.insert(session(targetId, "Target", UUID.randomUUID(),
                ScreenshareStatus.VIOLATION, NOW)).join();

        assertFalse(repository.findOpenByTarget(targetId).join().isPresent());
        assertTrue(repository.findOpen().join().isEmpty());
    }

    @Test
    void lElencoEuPaginato() {
        for (int index = 0; index < 5; index++) {
            repository.insert(session(UUID.randomUUID(), "Target" + index,
                    UUID.randomUUID(), ScreenshareStatus.COMPLETED,
                    NOW.plusSeconds(index))).join();
        }

        ScreensharePage first = repository.listByStatuses(null, 1, 2).join();
        ScreensharePage last = repository.listByStatuses(null, 3, 2).join();

        assertEquals(2, first.getItems().size());
        assertEquals(5L, first.getTotalItems());
        assertEquals(3, first.getTotalPages());
        assertTrue(first.hasNextPage());
        assertFalse(first.hasPreviousPage());
        assertEquals(1, last.getItems().size());
        assertEquals("Target4", first.getItems().get(0).getTargetName());
    }

    @Test
    void lElencoSiPuoFiltrarePerStato() {
        repository.insert(session(UUID.randomUUID(), "A", UUID.randomUUID(),
                ScreenshareStatus.ACTIVE, NOW)).join();
        repository.insert(session(UUID.randomUUID(), "B", UUID.randomUUID(),
                ScreenshareStatus.CANCELLED, NOW)).join();

        assertEquals(1, repository.listByStatuses(
                EnumSet.of(ScreenshareStatus.ACTIVE), 1, 10).join()
                .getTotalItems());
    }

    @Test
    void lAggiornamentoEuCondizionatoAStatoERevisione() {
        ScreenshareSession stored = session(UUID.randomUUID(), "Target",
                UUID.randomUUID(), ScreenshareStatus.CREATED, NOW);
        repository.insert(stored).join();

        ScreenshareSession moving = stored.toBuilder()
                .status(ScreenshareStatus.TRANSFERRING)
                .revision(1L)
                .build();

        assertTrue(repository.update(moving, ScreenshareStatus.CREATED, 0L)
                .join());
        assertFalse(repository.update(moving, ScreenshareStatus.CREATED, 0L)
                .join());
        assertEquals(ScreenshareStatus.TRANSFERRING,
                repository.find(stored.getId()).join().orElseThrow()
                        .getStatus());
    }

    @Test
    void unAggiornamentoRifiutatoNonLasciaTracce() {
        ScreenshareSession stored = session(UUID.randomUUID(), "Target",
                UUID.randomUUID(), ScreenshareStatus.ACTIVE, NOW);
        repository.insert(stored).join();

        ScreenshareSession wrong = stored.toBuilder()
                .status(ScreenshareStatus.COMPLETED)
                .notes("mai applicata")
                .revision(9L)
                .build();

        assertFalse(repository.update(wrong, ScreenshareStatus.CREATED, 7L)
                .join());
        ScreenshareSession read =
                repository.find(stored.getId()).join().orElseThrow();
        assertEquals(ScreenshareStatus.ACTIVE, read.getStatus());
        assertFalse(read.getNotes().isPresent());
        assertEquals(0L, read.getRevision());
    }

    @Test
    void gliEventiSiRileggonoDalPiuRecente() {
        ScreenshareSession stored = session(UUID.randomUUID(), "Target",
                UUID.randomUUID(), ScreenshareStatus.ACTIVE, NOW);
        repository.insert(stored).join();
        events.append(new ScreenshareEvent(UUID.randomUUID(), stored.getId(),
                null, "system", ScreenshareEventType.CREATED, null,
                ScreenshareStatus.CREATED, null, "proxy-test", NOW)).join();
        UUID staffId = UUID.randomUUID();
        events.append(new ScreenshareEvent(UUID.randomUUID(), stored.getId(),
                staffId, "Staff", ScreenshareEventType.SESSION_ACTIVE,
                ScreenshareStatus.TRANSFERRING, ScreenshareStatus.ACTIVE,
                "avviato", "proxy-test", NOW.plusSeconds(5))).join();

        List<ScreenshareEvent> history =
                events.findBySession(stored.getId(), 10).join();

        assertEquals(2, history.size());
        assertEquals(ScreenshareEventType.SESSION_ACTIVE,
                history.get(0).getType());
        assertEquals(staffId, history.get(0).getActorId().orElse(null));
        assertEquals("avviato", history.get(0).getMessage().orElse(null));
        assertFalse(history.get(1).getActorId().isPresent());
        assertEquals(1, events.findBySession(stored.getId(), 1).join().size());
    }

    @Test
    void unaSessioneInesistenteNonVieneTrovata() {
        assertFalse(repository.find(ScreenshareSessionId.random()).join()
                .isPresent());
        assertTrue(events.findBySession(ScreenshareSessionId.random(), 10)
                .join().isEmpty());
    }
}
