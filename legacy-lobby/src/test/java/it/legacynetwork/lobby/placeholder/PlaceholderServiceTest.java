package it.legacynetwork.lobby.placeholder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderServiceTest {

    @Test
    void noopServiceReturnsTextUnchanged() {
        PlaceholderService service = new NoopPlaceholderService();
        String result = service.apply(null, "Hello %player_name%");
        assertEquals("Hello %player_name%", result);
    }

    @Test
    void noopServiceIsNotAvailable() {
        assertFalse(new NoopPlaceholderService().isAvailable());
    }

    @Test
    void placeholderApiServiceIsAvailable() {
        assertTrue(new PlaceholderApiService().isAvailable());
    }

    @Test
    void noopReturnsSameInstance() {
        NoopPlaceholderService a = new NoopPlaceholderService();
        NoopPlaceholderService b = new NoopPlaceholderService();
        assertEquals(a.apply(null, "test"), b.apply(null, "test"));
    }
}
