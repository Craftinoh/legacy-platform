package it.legacynetwork.language.velocity.repository;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresLanguageNotificationServiceTest {

    @Test
    void notificationEventHoldsCorrectData() {
        UUID id = UUID.randomUUID();
        PostgresLanguageNotificationService.NotificationEvent event =
                new PostgresLanguageNotificationService.NotificationEvent(
                        id, 42L, "it", "it_IT", "proxy-01");
        assertEquals(id, event.playerId);
        assertEquals(42L, event.revision);
        assertEquals("it", event.languageCode);
        assertEquals("it_IT", event.locale);
        assertEquals("proxy-01", event.sourceProxy);
    }

    @Test
    void handlerIgnoresOwnNotification() {
        AtomicBoolean fired = new AtomicBoolean(false);
        PostgresLanguageNotificationService service = service("proxy-01", fired);
        service.handleNotification(notifyPayload(UUID.randomUUID(), 1L, "en", "en_US", "proxy-01"));
        assertFalse(fired.get(), "Should ignore own proxy notifications");
    }

    @Test
    void handlerAcceptsForeignNotification() {
        AtomicBoolean fired = new AtomicBoolean(false);
        PostgresLanguageNotificationService service = service("proxy-02", fired);
        UUID id = UUID.randomUUID();
        service.handleNotification(notifyPayload(id, 5L, "it", "it_IT", "proxy-01"));
        assertTrue(fired.get());
    }

    @Test
    void oldRevisionIsIgnored() {
        AtomicBoolean fired = new AtomicBoolean(false);
        PostgresLanguageNotificationService service = service("proxy-01", fired);
        UUID id = UUID.randomUUID();
        service.handleNotification(notifyPayload(id, 10L, "en", "en_US", "proxy-02"));
        assertTrue(fired.getAndSet(false));
        service.handleNotification(notifyPayload(id, 5L, "it", "it_IT", "proxy-02"));
        assertFalse(fired.get(), "Old revision should be ignored");
    }

    @Test
    void duplicateNotificationIsIdempotent() {
        AtomicBoolean fired = new AtomicBoolean(false);
        PostgresLanguageNotificationService service = service("proxy-01", fired);
        UUID id = UUID.randomUUID();
        service.handleNotification(notifyPayload(id, 1L, "en", "en_US", "proxy-02"));
        assertTrue(fired.getAndSet(false));
        service.handleNotification(notifyPayload(id, 1L, "es", "es_ES", "proxy-02"));
        assertFalse(fired.get(), "Duplicate should not refire");
    }

    @Test
    void invalidPayloadIsIgnored() {
        AtomicBoolean fired = new AtomicBoolean(false);
        PostgresLanguageNotificationService service = service("proxy-01", fired);
        service.handleNotification("garbage");
        assertFalse(fired.get());
        service.handleNotification("");
        assertFalse(fired.get());
        service.handleNotification(null);
        assertFalse(fired.get());
    }

    @Test
    void payloadFormatPreservesAllFields() {
        String payload = notifyPayload(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                42L, "it", "it_IT", "proxy-01");
        assertEquals("550e8400-e29b-41d4-a716-446655440000|42|it|it_IT|proxy-01",
                payload);
    }

    @Test
    void newRevisionUpdatesCache() {
        AtomicBoolean fired = new AtomicBoolean(false);
        PostgresLanguageNotificationService service = service("proxy-01", fired);
        UUID id = UUID.randomUUID();
        service.handleNotification(notifyPayload(id, 100L, "de", "de_DE", "proxy-02"));
        assertTrue(fired.get());
        assertEquals(100L, service.getLatestRevision().get());
    }

    private PostgresLanguageNotificationService service(
            String proxyId, AtomicBoolean fired) {
        PostgresLanguageNotificationService s = new PostgresLanguageNotificationService(
                "localhost", 5432, "test", "test", "test", proxyId,
                Logger.getAnonymousLogger());
        s.setCallback(e -> fired.set(true));
        return s;
    }

    private String notifyPayload(UUID playerId, long revision,
                                  String lang, String locale, String proxy) {
        return playerId + "|" + revision + "|" + lang + "|" + locale + "|" + proxy;
    }
}
