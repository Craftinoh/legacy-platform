package it.legacynetwork.chickenwars.velocity.rejoin;

import it.legacynetwork.chickenwars.mode.MatchMode;
import it.legacynetwork.chickenwars.routing.GameInstanceDescriptor;
import it.legacynetwork.chickenwars.routing.GameReservation;
import it.legacynetwork.chickenwars.routing.InstanceStatus;
import it.legacynetwork.chickenwars.routing.ReconnectCoordinator;
import it.legacynetwork.chickenwars.routing.ReconnectDiagnosis;
import it.legacynetwork.chickenwars.routing.ReconnectSessionInspector;
import it.legacynetwork.chickenwars.routing.RejoinVerdictCodec;
import it.legacynetwork.chickenwars.routing.ReservationStatus;
import it.legacynetwork.chickenwars.routing.RouteFailure;
import it.legacynetwork.chickenwars.routing.RouteResult;
import it.legacynetwork.chickenwars.routing.RoutingCoordinator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Doppioni deterministici dei contratti condivisi.
 *
 * <p>Registrano ogni invocazione: i test possono cosi' verificare quante
 * prenotazioni e quanti trasferimenti sono stati richiesti, non soltanto
 * l'esito finale.</p>
 */
final class RejoinTestSupport {

    static final String ARENA = "farm";
    static final String INSTANCE = "cw-1:farm";
    static final String SERVER = "chickenwars-1";

    private RejoinTestSupport() {
    }

    static GameInstanceDescriptor instance(InstanceStatus status, long heartbeat) {
        return new GameInstanceDescriptor(INSTANCE, SERVER, MatchMode.SOLO,
                ARENA, status, 4, 8, heartbeat, true);
    }

    static GameReservation reservation(UUID playerId) {
        return new GameReservation("res-" + playerId, playerId, MatchMode.SOLO,
                INSTANCE, Long.MAX_VALUE, "rejoin:" + playerId);
    }

    /** Coordinatore reconnect programmabile, con conteggio delle chiamate. */
    static final class FakeReconnect implements ReconnectCoordinator {

        private final List<String> keys = new ArrayList<String>();
        private final List<String> remembered = new ArrayList<String>();
        private RouteResult result;
        private RuntimeException failure;
        private int reconnectCalls;

        FakeReconnect succeeding(UUID playerId, InstanceStatus status) {
            this.result = RouteResult.success(instance(status, 1_000L),
                    Collections.singletonList(reservation(playerId)));
            return this;
        }

        FakeReconnect failing(RouteFailure reason) {
            this.result = RouteResult.failure(reason);
            return this;
        }

        FakeReconnect throwing(RuntimeException failure) {
            this.failure = failure;
            return this;
        }

        @Override
        public CompletionStage<Void> remember(UUID playerId, String instanceId,
                                              long expiresAt) {
            remembered.add(instanceId);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<RouteResult> reconnect(UUID playerId, String key,
                                                      long now) {
            reconnectCalls++;
            keys.add(key);
            if (failure != null) {
                CompletableFuture<RouteResult> broken = new CompletableFuture<RouteResult>();
                broken.completeExceptionally(failure);
                return broken;
            }
            return CompletableFuture.completedFuture(result);
        }

        @Override
        public CompletionStage<Void> forget(UUID playerId) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Integer> cleanup(long now) {
            return CompletableFuture.completedFuture(Integer.valueOf(0));
        }

        int reconnectCalls() {
            return reconnectCalls;
        }

        List<String> keys() {
            return keys;
        }

        List<String> remembered() {
            return remembered;
        }
    }

    /** Routing usato solo per annullare le prenotazioni fallite. */
    static final class FakeRouting implements RoutingCoordinator {

        private final List<String> cancelled = new ArrayList<String>();
        private final List<String> claimed = new ArrayList<String>();

        @Override
        public CompletionStage<RouteResult> route(UUID playerId, MatchMode mode,
                                                  List<UUID> party, String key,
                                                  long now) {
            throw new UnsupportedOperationException(
                    "Il rejoin non instrada: usa reconnect()");
        }

        @Override
        public CompletionStage<Boolean> claim(String reservationId, UUID playerId,
                                              long now) {
            claimed.add(reservationId);
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        @Override
        public CompletionStage<Boolean> cancel(String reservationId) {
            cancelled.add(reservationId);
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        @Override
        public CompletionStage<Integer> cleanup(long now) {
            return CompletableFuture.completedFuture(Integer.valueOf(0));
        }

        List<String> cancelled() {
            return cancelled;
        }

        List<String> claimed() {
            return claimed;
        }
    }

    /** Diagnosi programmabile. */
    static final class FakeInspector implements ReconnectSessionInspector {

        private ReconnectDiagnosis diagnosis = ReconnectDiagnosis.NONE;
        private RuntimeException failure;

        FakeInspector saying(ReconnectDiagnosis diagnosis) {
            this.diagnosis = diagnosis;
            return this;
        }

        FakeInspector throwing(RuntimeException failure) {
            this.failure = failure;
            return this;
        }

        @Override
        public CompletionStage<ReconnectDiagnosis> inspect(UUID playerId, long now) {
            if (failure != null) {
                CompletableFuture<ReconnectDiagnosis> broken =
                        new CompletableFuture<ReconnectDiagnosis>();
                broken.completeExceptionally(failure);
                return broken;
            }
            return CompletableFuture.completedFuture(diagnosis);
        }
    }

    /** Ritardi programmati a mano: nessuna attesa reale nei test. */
    static final class ManualDelayer implements BackendVerdictRegistry.Delayer {

        private final List<Runnable> scheduled = new ArrayList<Runnable>();

        @Override
        public void schedule(Runnable action, long delayMillis) {
            scheduled.add(action);
        }

        /** Fa scattare tutti i timeout programmati. */
        void fire() {
            List<Runnable> pending = new ArrayList<Runnable>(scheduled);
            scheduled.clear();
            for (Runnable action : pending) {
                action.run();
            }
        }

        int pending() {
            return scheduled.size();
        }
    }

    /**
     * Gateway che registra i trasferimenti richiesti.
     *
     * <p>Puo' rispondere al posto del backend: e' cosi' che i test
     * distinguono un rifiuto applicativo da un errore di connessione.</p>
     */
    static final class FakeTransfers implements ProxyTransferGateway {

        private final List<String> transfers = new ArrayList<String>();
        private boolean registered = true;
        private boolean alreadyConnected;
        private boolean connects = true;
        private RuntimeException failure;
        private BackendVerdictRegistry verdicts;
        private Boolean backendAccepts = Boolean.TRUE;
        private String backendReason = "";
        private final List<String> evacuations = new ArrayList<String>();
        private boolean evacuates = true;
        private RuntimeException evacuationFailure;

        FakeTransfers answering(BackendVerdictRegistry verdicts) {
            this.verdicts = verdicts;
            return this;
        }

        FakeTransfers backendRejects(String reason) {
            this.backendAccepts = Boolean.FALSE;
            this.backendReason = reason;
            return this;
        }

        /** Il backend non risponde: restera' solo il timeout. */
        FakeTransfers backendSilent() {
            this.backendAccepts = null;
            return this;
        }

        FakeTransfers backendAccepts() {
            this.backendAccepts = Boolean.TRUE;
            this.backendReason = "";
            return this;
        }

        FakeTransfers unregistered() {
            this.registered = false;
            return this;
        }

        FakeTransfers alreadyConnected() {
            this.alreadyConnected = true;
            return this;
        }

        FakeTransfers refusing() {
            this.connects = false;
            return this;
        }

        FakeTransfers throwing(RuntimeException failure) {
            this.failure = failure;
            return this;
        }

        @Override
        public boolean isServerRegistered(String serverName) {
            return registered;
        }

        @Override
        public boolean isAlreadyConnectedTo(UUID playerId, String serverName) {
            return alreadyConnected;
        }

        @Override
        public CompletionStage<Boolean> transfer(UUID playerId, String serverName,
                                                 String reservationId,
                                                 String arenaId) {
            transfers.add(serverName + '/' + reservationId + '/' + arenaId);
            if (failure != null) {
                throw failure;
            }
            if (connects && verdicts != null && backendAccepts != null) {
                // Il backend risponde appena il giocatore arriva.
                verdicts.complete(new RejoinVerdictCodec.Verdict(playerId,
                        backendAccepts.booleanValue(), backendReason, arenaId));
            }
            return CompletableFuture.completedFuture(
                    Boolean.valueOf(connects));
        }

        @Override
        public CompletionStage<Boolean> evacuate(UUID playerId,
                                                 String rejectedServerName) {
            evacuations.add(rejectedServerName);
            if (evacuationFailure != null) {
                throw evacuationFailure;
            }
            return CompletableFuture.completedFuture(
                    Boolean.valueOf(evacuates));
        }

        FakeTransfers evacuationRefused() {
            this.evacuates = false;
            return this;
        }

        FakeTransfers evacuationThrowing(RuntimeException failure) {
            this.evacuationFailure = failure;
            return this;
        }

        List<String> transfers() {
            return transfers;
        }

        List<String> evacuations() {
            return evacuations;
        }
    }

    static ReservationStatus statusOf(GameReservation reservation) {
        return reservation.getStatus();
    }
}
