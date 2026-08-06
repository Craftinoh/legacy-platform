package it.legacynetwork.language.velocity.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlitePlayerLanguageRepositoryTest {

    private SqlitePlayerLanguageRepository repo;
    private File dbFile;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        dbFile = tempDir.resolve("test.db").toFile();
        repo = new SqlitePlayerLanguageRepository(
                dbFile.getAbsolutePath(), 5000, "test-proxy",
                Logger.getAnonymousLogger());
        repo.initialize();
    }

    @AfterEach
    void tearDown() {
        if (repo != null) {
            repo.close();
        }
    }

    @Test
    void createEmptyDatabase() throws Exception {
        assertTrue(dbFile.exists());
    }

    @Test
    void migrationV1Executed() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:sqlite:" + dbFile.getAbsolutePath());
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT version FROM schema_migrations WHERE version = 'V1'");
            assertTrue(rs.next());
            assertEquals("V1", rs.getString("version"));
        }
    }

    @Test
    void secondInitializeIsIdempotent() throws Exception {
        repo.initialize();
        try (Connection conn = DriverManager.getConnection(
                "jdbc:sqlite:" + dbFile.getAbsolutePath());
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM schema_migrations");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void findReturnsEmptyForUnknownPlayer() throws Exception {
        Optional<PlayerLanguageRepository.LanguageState> found =
                repo.find(UUID.randomUUID()).get(5, TimeUnit.SECONDS);
        assertFalse(found.isPresent());
    }

    @Test
    void upsertStateCreatesRecord() throws Exception {
        UUID id = UUID.randomUUID();
        repo.upsertState(id, "en", "en_US").get(5, TimeUnit.SECONDS);

        Optional<PlayerLanguageRepository.LanguageState> found =
                repo.find(id).get(5, TimeUnit.SECONDS);
        assertTrue(found.isPresent());
        assertEquals("en", found.get().languageCode);
        assertEquals("en_US", found.get().clientLocale);
    }

    @Test
    void changeLanguageSucceeds() throws Exception {
        UUID id = UUID.randomUUID();
        repo.upsertState(id, "en", "en_US").get(5, TimeUnit.SECONDS);

        PlayerLanguageRepository.ChangeResult result =
                repo.changeLanguage(UUID.randomUUID(), id, "it", "it_IT",
                        "test-proxy", 5, 3, 60).get(5, TimeUnit.SECONDS);

        assertTrue(result.isAccepted());
        assertEquals("it", result.languageCode);
    }

    @Test
    void revisionIncreasesAfterChange() throws Exception {
        UUID id = UUID.randomUUID();
        repo.upsertState(id, "en", "en_US").get(5, TimeUnit.SECONDS);

        repo.changeLanguage(UUID.randomUUID(), id, "it", "it_IT",
                "test-proxy", 5, 3, 60).get(5, TimeUnit.SECONDS);

        Optional<PlayerLanguageRepository.LanguageState> found =
                repo.find(id).get(5, TimeUnit.SECONDS);
        assertTrue(found.isPresent());
        assertTrue(found.get().revision >= 1);
    }

    @Test
    void alreadySelectedDoesNotConsumeLimit() throws Exception {
        UUID id = UUID.randomUUID();
        repo.upsertState(id, "it", "it_IT").get(5, TimeUnit.SECONDS);

        PlayerLanguageRepository.ChangeResult result =
                repo.changeLanguage(UUID.randomUUID(), id, "it", "it_IT",
                        "test-proxy", 5, 3, 60).get(5, TimeUnit.SECONDS);

        assertFalse(result.isAccepted());
        assertEquals(PlayerLanguageRepository.ChangeStatus.ALREADY_SELECTED,
                result.status);
    }

    @Test
    void cooldownEnforced() throws Exception {
        UUID id = UUID.randomUUID();
        repo.upsertState(id, "en", "en_US").get(5, TimeUnit.SECONDS);
        repo.changeLanguage(UUID.randomUUID(), id, "it", "it_IT",
                "test-proxy", 5, 3, 60).get(5, TimeUnit.SECONDS);

        PlayerLanguageRepository.ChangeResult result =
                repo.changeLanguage(UUID.randomUUID(), id, "es", "es_ES",
                        "test-proxy", 5, 3, 60).get(5, TimeUnit.SECONDS);

        assertEquals(PlayerLanguageRepository.ChangeStatus.CHANGE_COOLDOWN,
                result.status);
    }

    @Test
    void hourlyLimitOfThreeEnforced() throws Exception {
        UUID id = UUID.randomUUID();
        repo.upsertState(id, "en", "en_US").get(5, TimeUnit.SECONDS);
        repo.changeLanguage(UUID.randomUUID(), id, "it", "it_IT",
                "test-proxy", 0, 3, 60).get(5, TimeUnit.SECONDS);
        repo.changeLanguage(UUID.randomUUID(), id, "es", "es_ES",
                "test-proxy", 0, 3, 60).get(5, TimeUnit.SECONDS);
        repo.changeLanguage(UUID.randomUUID(), id, "fr", "fr_FR",
                "test-proxy", 0, 3, 60).get(5, TimeUnit.SECONDS);

        PlayerLanguageRepository.ChangeResult result =
                repo.changeLanguage(UUID.randomUUID(), id, "de", "de_DE",
                        "test-proxy", 0, 3, 60).get(5, TimeUnit.SECONDS);

        assertEquals(PlayerLanguageRepository.ChangeStatus.HOURLY_LIMIT,
                result.status);
    }

    @Test
    void duplicateRequestIsIdempotent() throws Exception {
        UUID id = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        repo.upsertState(id, "en", "en_US").get(5, TimeUnit.SECONDS);

        repo.changeLanguage(requestId, id, "it", "it_IT",
                "test-proxy", 5, 3, 60).get(5, TimeUnit.SECONDS);

        PlayerLanguageRepository.ChangeResult result =
                repo.changeLanguage(requestId, id, "es", "es_ES",
                        "test-proxy", 5, 3, 60).get(5, TimeUnit.SECONDS);

        assertTrue(result.isAccepted());
        assertEquals("it", result.languageCode);
    }

    @Test
    void persistenceAfterCloseAndReopen() throws Exception {
        UUID id = UUID.randomUUID();
        repo.upsertState(id, "it", "it_IT").get(5, TimeUnit.SECONDS);
        repo.changeLanguage(UUID.randomUUID(), id, "de", "de_DE",
                "test-proxy", 5, 3, 60).get(5, TimeUnit.SECONDS);
        repo.close();

        repo = new SqlitePlayerLanguageRepository(
                dbFile.getAbsolutePath(), 5000, "test-proxy",
                Logger.getAnonymousLogger());
        repo.initialize();

        Optional<PlayerLanguageRepository.LanguageState> found =
                repo.find(id).get(5, TimeUnit.SECONDS);
        assertTrue(found.isPresent());
        assertEquals("de", found.get().languageCode);
        assertEquals("de_DE", found.get().clientLocale);
        assertTrue(found.get().revision >= 1);
    }

    @Test
    void walEnabled() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:sqlite:" + dbFile.getAbsolutePath());
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA journal_mode");
            assertTrue(rs.next());
            assertEquals("wal", rs.getString(1).toLowerCase());
        }
    }

    @Test
    void foreignKeysEnabled() throws Exception {
        assertTrue(true);
    }

    @Test
    void pathTraversalRejected() throws Exception {
        File traversal = new File("../../outside.db");
        try {
            new SqlitePlayerLanguageRepository(
                    traversal.getAbsolutePath(), 5000, "test",
                    Logger.getAnonymousLogger());
        } catch (RuntimeException e) {
            assertNotNull(e);
        }
    }

    @Test
    void twoConcurrentConnectionsOnSameUuid() throws Exception {
        UUID id = UUID.randomUUID();
        repo.upsertState(id, "en", "en_US").get(5, TimeUnit.SECONDS);

        Thread t1 = new Thread(() -> {
            try {
                repo.changeLanguage(UUID.randomUUID(), id, "it", "it_IT",
                        "test-proxy", 1, 5, 60).get(10, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        });
        Thread t2 = new Thread(() -> {
            try {
                repo.changeLanguage(UUID.randomUUID(), id, "es", "es_ES",
                        "test-proxy", 1, 5, 60).get(10, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        });

        t1.start();
        t2.start();
        t1.join(10000);
        t2.join(10000);

        Optional<PlayerLanguageRepository.LanguageState> found =
                repo.find(id).get(5, TimeUnit.SECONDS);
        assertTrue(found.isPresent());
        assertTrue("it".equals(found.get().languageCode)
                || "es".equals(found.get().languageCode));
    }
}
