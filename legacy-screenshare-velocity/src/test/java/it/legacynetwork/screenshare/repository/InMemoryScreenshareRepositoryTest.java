package it.legacynetwork.screenshare.repository;

import it.legacynetwork.screenshare.model.ScreenshareSession;
import it.legacynetwork.screenshare.model.ScreenshareSessionId;
import it.legacynetwork.screenshare.model.ScreenshareStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lo storage in memoria deve rispettare lo stesso contratto di quello JDBC.
 */
class InMemoryScreenshareRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    private InMemoryScreenshareRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryScreenshareRepository();
    }

    private ScreenshareSession session(UUID targetId, String targetName,
                                       ScreenshareStatus status,
                                       Instant createdAt) {
        return ScreenshareSession.builder()
                .id(ScreenshareSessionId.random())
                .target(targetId, targetName)
                .staff(UUID.randomUUID(), "Staff")
                .serverId("screenshare-1")
                .createdAt(createdAt)
                .expiresAt(createdAt.plus(Duration.ofHours(1)))
                .status(status)
                .proxyId("proxy-test")
                .revision(0L)
                .build();
    }

    @Test
    void inserimentoELettura() {
        ScreenshareSession stored = session(UUID.randomUUID(), "Target",
                ScreenshareStatus.ACTIVE, NOW);

        repository.insert(stored).join();

        assertEquals(stored.getId(), repository.find(stored.getId()).join()
                .orElseThrow().getId());
        assertFalse(repository.find(ScreenshareSessionId.random()).join()
                .isPresent());
    }

    @Test
    void laStessaSessioneNonSiInseriscePiuDiUnaVolta() {
        ScreenshareSession stored = session(UUID.randomUUID(), "Target",
                ScreenshareStatus.ACTIVE, NOW);
        repository.insert(stored).join();

        assertTrue(repository.insert(stored)
                .handle((value, failure) -> failure != null).join());
    }

    @Test
    void soloLeSessioniNonConcluseRisultanoAperte() {
        UUID targetId = UUID.randomUUID();
        repository.insert(session(targetId, "Target",
                ScreenshareStatus.COMPLETED, NOW)).join();

        assertFalse(repository.findOpenByTarget(targetId).join().isPresent());
        assertTrue(repository.findOpen().join().isEmpty());
    }

    @Test
    void laPaginazioneSegueLOrdineCronologicoInverso() {
        for (int index = 0; index < 3; index++) {
            repository.insert(session(UUID.randomUUID(), "Target" + index,
                    ScreenshareStatus.COMPLETED,
                    NOW.plusSeconds(index))).join();
        }

        ScreensharePage page = repository.listByStatuses(null, 1, 2).join();

        assertEquals(2, page.getItems().size());
        assertEquals(3L, page.getTotalItems());
        assertEquals("Target2", page.getItems().get(0).getTargetName());
        assertTrue(repository.listByStatuses(null, 9, 2).join().isEmpty());
    }

    @Test
    void lAggiornamentoRispettaStatoERevisione() {
        ScreenshareSession stored = session(UUID.randomUUID(), "Target",
                ScreenshareStatus.CREATED, NOW);
        repository.insert(stored).join();
        ScreenshareSession moving = stored.toBuilder()
                .status(ScreenshareStatus.TRANSFERRING)
                .revision(1L)
                .build();

        assertTrue(repository.update(moving, ScreenshareStatus.CREATED, 0L)
                .join());
        assertFalse(repository.update(moving, ScreenshareStatus.CREATED, 0L)
                .join());
    }

    @Test
    void leNoteSiAccumulanoSenzaSovrascriversi() {
        ScreenshareSession stored = session(UUID.randomUUID(), "Target",
                ScreenshareStatus.ACTIVE, NOW);

        String first = stored.appendNote("prima");
        String second = stored.toBuilder().notes(first).build()
                .appendNote("seconda");

        assertEquals("prima", first);
        assertTrue(second.contains("prima"));
        assertTrue(second.contains("seconda"));
    }
}
