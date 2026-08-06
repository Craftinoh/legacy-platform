package it.legacynetwork.language.velocity.repository;

import it.legacynetwork.language.Language;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerLanguageRepositoryTest {

    @Test
    void changeResultSuccessIsAccepted() {
        PlayerLanguageRepository.ChangeResult r = new PlayerLanguageRepository
                .ChangeResult(PlayerLanguageRepository.ChangeStatus.SUCCESS,
                        "it", null);
        assertTrue(r.isAccepted());
    }

    @Test
    void changeResultDeniedIsNotAccepted() {
        PlayerLanguageRepository.ChangeResult r = new PlayerLanguageRepository
                .ChangeResult(PlayerLanguageRepository.ChangeStatus.HOURLY_LIMIT,
                        null, "limit_reached");
        assertFalse(r.isAccepted());
    }

    @Test
    void languageStateHoldsCorrectValues() {
        PlayerLanguageRepository.LanguageState s = new PlayerLanguageRepository
                .LanguageState("it", "it_IT", 42L);
        assertEquals("it", s.languageCode);
        assertEquals("it_IT", s.clientLocale);
        assertEquals(42L, s.revision);
    }

    @Test
    void changeResultAlreadySelected() {
        PlayerLanguageRepository.ChangeResult r = new PlayerLanguageRepository
                .ChangeResult(PlayerLanguageRepository.ChangeStatus.ALREADY_SELECTED,
                        "en", "already");
        assertFalse(r.isAccepted());
        assertEquals("en", r.languageCode);
    }

    @Test
    void changeResultCooldown() {
        PlayerLanguageRepository.ChangeResult r = new PlayerLanguageRepository
                .ChangeResult(PlayerLanguageRepository.ChangeStatus.CHANGE_COOLDOWN,
                        null, "cooldown");
        assertEquals(PlayerLanguageRepository.ChangeStatus.CHANGE_COOLDOWN,
                r.status);
    }

    @Test
    void allChangeStatusesAreDistinct() {
        PlayerLanguageRepository.ChangeStatus[] values =
                PlayerLanguageRepository.ChangeStatus.values();
        assertEquals(7, values.length);
    }

    @Test
    void inMemoryRepositoryCachesLanguage() throws Exception {
        MockRepository repo = new MockRepository();
        UUID id = UUID.randomUUID();
        repo.upsertState(id, "it", "it_IT").get(1, TimeUnit.SECONDS);

        Optional<PlayerLanguageRepository.LanguageState> found =
                repo.find(id).get(1, TimeUnit.SECONDS);
        assertTrue(found.isPresent());
        assertEquals("it", found.get().languageCode);
        assertEquals("it_IT", found.get().clientLocale);
    }

    @Test
    void inMemoryRepositoryReturnsEmptyForUnknown() throws Exception {
        MockRepository repo = new MockRepository();
        Optional<PlayerLanguageRepository.LanguageState> found =
                repo.find(UUID.randomUUID()).get(1, TimeUnit.SECONDS);
        assertFalse(found.isPresent());
    }

    @Test
    void inMemoryRepositoryChangeLanguageSucceeds() throws Exception {
        MockRepository repo = new MockRepository();
        UUID id = UUID.randomUUID();
        repo.upsertState(id, "en", "en_US").get(1, TimeUnit.SECONDS);

        PlayerLanguageRepository.ChangeResult result = repo.changeLanguage(
                UUID.randomUUID(), id, "it", "it_IT", "proxy-01",
                5, 3, 60).get(1, TimeUnit.SECONDS);

        assertTrue(result.isAccepted());
        assertEquals("it", result.languageCode);
    }

    @Test
    void inMemoryRepositoryEnforcesCooldown() throws Exception {
        MockRepository repo = new MockRepository();
        UUID id = UUID.randomUUID();
        repo.upsertState(id, "en", "en_US").get(1, TimeUnit.SECONDS);
        repo.changeLanguage(UUID.randomUUID(), id, "it", "it_IT",
                "proxy-01", 5, 3, 60).get(1, TimeUnit.SECONDS);

        PlayerLanguageRepository.ChangeResult result = repo.changeLanguage(
                UUID.randomUUID(), id, "es", "es_ES",
                "proxy-01", 5, 3, 60).get(1, TimeUnit.SECONDS);

        assertEquals(PlayerLanguageRepository.ChangeStatus.CHANGE_COOLDOWN,
                result.status);
    }

    @Test
    void inMemoryRepositoryEnforcesHourlyLimit() throws Exception {
        MockRepository repo = new MockRepository();
        UUID id = UUID.randomUUID();
        repo.upsertState(id, "en", "en_US").get(1, TimeUnit.SECONDS);

        repo.changeLanguage(UUID.randomUUID(), id, "it", "it_IT",
                "proxy-01", 1, 2, 60).get(1, TimeUnit.SECONDS);
        repo.changeLanguage(UUID.randomUUID(), id, "es", "es_ES",
                "proxy-01", 0, 2, 60).get(1, TimeUnit.SECONDS);

        PlayerLanguageRepository.ChangeResult result = repo.changeLanguage(
                UUID.randomUUID(), id, "de", "de_DE",
                "proxy-01", 0, 2, 60).get(1, TimeUnit.SECONDS);

        assertEquals(PlayerLanguageRepository.ChangeStatus.HOURLY_LIMIT,
                result.status);
    }

    @Test
    void inMemoryRepositorySkipsAlreadySelected() throws Exception {
        MockRepository repo = new MockRepository();
        UUID id = UUID.randomUUID();
        repo.upsertState(id, "it", "it_IT").get(1, TimeUnit.SECONDS);

        PlayerLanguageRepository.ChangeResult result = repo.changeLanguage(
                UUID.randomUUID(), id, "it", "it_IT",
                "proxy-01", 5, 3, 60).get(1, TimeUnit.SECONDS);

        assertEquals(PlayerLanguageRepository.ChangeStatus.ALREADY_SELECTED,
                result.status);
    }

    @Test
    void inMemoryRepositoryRequestIdIsIdempotent() throws Exception {
        MockRepository repo = new MockRepository();
        UUID playerId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        repo.upsertState(playerId, "en", "en_US").get(1, TimeUnit.SECONDS);

        PlayerLanguageRepository.ChangeResult r1 = repo.changeLanguage(
                requestId, playerId, "it", "it_IT",
                "proxy-01", 5, 3, 60).get(1, TimeUnit.SECONDS);

        PlayerLanguageRepository.ChangeResult r2 = repo.changeLanguage(
                requestId, playerId, "es", "es_ES",
                "proxy-02", 5, 3, 60).get(1, TimeUnit.SECONDS);

        assertTrue(r1.isAccepted());
        assertEquals("it", r2.languageCode);
    }

    static class MockRepository implements PlayerLanguageRepository {
        private final ConcurrentHashMap<UUID, LanguageState> states =
                new ConcurrentHashMap<>();
        private final ConcurrentHashMap<UUID, Long> lastChanged =
                new ConcurrentHashMap<>();
        private final ConcurrentHashMap<UUID, Integer> changeCounts =
                new ConcurrentHashMap<>();
        private final ConcurrentHashMap<UUID, String> seenRequestIds =
                new ConcurrentHashMap<>();

        @Override
        public CompletableFuture<Optional<LanguageState>> find(UUID playerUuid) {
            return CompletableFuture.completedFuture(
                    Optional.ofNullable(states.get(playerUuid)));
        }

        @Override
        public CompletableFuture<ChangeResult> changeLanguage(UUID requestId,
                UUID playerUuid, String newLanguage, String newLocale,
                String proxyId, int cooldownSeconds,
                int maxChangesPerWindow, int windowMinutes) {
            return CompletableFuture.supplyAsync(() -> {
                String seen = seenRequestIds.putIfAbsent(requestId, newLanguage);
                if (seen != null) {
                    return new ChangeResult(ChangeStatus.SUCCESS, seen, null);
                }
                LanguageState current = states.get(playerUuid);
                if (current != null && current.languageCode.equals(newLanguage)) {
                    return new ChangeResult(ChangeStatus.ALREADY_SELECTED,
                            newLanguage, null);
                }
                Long last = lastChanged.get(playerUuid);
                if (last != null
                        && (System.currentTimeMillis() - last) < cooldownSeconds * 1000L) {
                    return new ChangeResult(ChangeStatus.CHANGE_COOLDOWN,
                            null, "cooldown");
                }
                Integer count = changeCounts.getOrDefault(playerUuid, 0);
                if (count >= maxChangesPerWindow) {
                    return new ChangeResult(ChangeStatus.HOURLY_LIMIT,
                            null, "limit");
                }
                long revision = current != null ? current.revision + 1 : 1;
                states.put(playerUuid, new LanguageState(
                        newLanguage, newLocale, revision));
                lastChanged.put(playerUuid, System.currentTimeMillis());
                changeCounts.put(playerUuid, count + 1);
                return new ChangeResult(ChangeStatus.SUCCESS,
                        newLanguage, null);
            });
        }

        @Override
        public CompletableFuture<Void> upsertState(UUID playerUuid,
                String languageCode, String clientLocale) {
            return CompletableFuture.runAsync(() -> {
                LanguageState existing = states.get(playerUuid);
                long revision = existing != null ? existing.revision + 1 : 1;
                states.put(playerUuid, new LanguageState(
                        languageCode, clientLocale, revision));
            });
        }
    }
}
