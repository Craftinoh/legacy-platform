package it.legacynetwork.chickenwars.chicken;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RoyalDefeatDispatcherTest {

    @Test
    void dispatchesToRegisteredListeners() {
        RoyalDefeatDispatcher dispatcher = new RoyalDefeatDispatcher();
        AtomicInteger called = new AtomicInteger();

        dispatcher.register(defeat -> called.incrementAndGet());
        int notified = dispatcher.dispatch(
                new RoyalDefeat("a1", "red", UUID.randomUUID(), null,
                        System.currentTimeMillis()));

        assertEquals(1, notified);
        assertEquals(1, called.get());
    }

    @Test
    void ignoraNullDefeat() {
        RoyalDefeatDispatcher dispatcher = new RoyalDefeatDispatcher();
        int notified = dispatcher.dispatch(null);
        assertEquals(0, notified);
    }

    @Test
    void exceptionNonFermaAltri() {
        RoyalDefeatDispatcher dispatcher = new RoyalDefeatDispatcher();
        AtomicInteger called = new AtomicInteger();

        dispatcher.register(defeat -> { throw new RuntimeException("bad"); });
        dispatcher.register(defeat -> called.incrementAndGet());

        int notified = dispatcher.dispatch(
                new RoyalDefeat("a1", "red", UUID.randomUUID(), null,
                        System.currentTimeMillis()));

        assertEquals(1, notified);
        assertEquals(1, called.get());
    }

    @Test
    void ignoreDuplicateRegistration() {
        RoyalDefeatDispatcher dispatcher = new RoyalDefeatDispatcher();
        RoyalDefeatListener listener = defeat -> {};

        dispatcher.register(listener);
        dispatcher.register(listener);

        assertEquals(1, dispatcher.size());
    }

    @Test
    void unregisterRimuove() {
        RoyalDefeatDispatcher dispatcher = new RoyalDefeatDispatcher();
        RoyalDefeatListener listener = defeat -> {};

        dispatcher.register(listener);
        assertTrue(dispatcher.size() > 0);

        dispatcher.unregister(listener);
        assertEquals(0, dispatcher.size());
    }

    @Test
    void clearRimuoveTutti() {
        RoyalDefeatDispatcher dispatcher = new RoyalDefeatDispatcher();
        dispatcher.register(defeat -> {});
        dispatcher.register(defeat -> {});

        dispatcher.clear();
        assertEquals(0, dispatcher.size());
    }

    @Test
    void dispatchToMultipleListeners() {
        RoyalDefeatDispatcher dispatcher = new RoyalDefeatDispatcher();
        List<String> received = new ArrayList<String>();

        dispatcher.register(defeat -> received.add("first"));
        dispatcher.register(defeat -> received.add("second"));

        dispatcher.dispatch(
                new RoyalDefeat("a1", "red", UUID.randomUUID(), null,
                        System.currentTimeMillis()));

        assertEquals(2, received.size());
    }
}
