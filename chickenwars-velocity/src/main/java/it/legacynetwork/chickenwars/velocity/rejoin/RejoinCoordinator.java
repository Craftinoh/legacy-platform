package it.legacynetwork.chickenwars.velocity.rejoin;

import it.legacynetwork.chickenwars.routing.GameInstanceDescriptor;
import it.legacynetwork.chickenwars.routing.GameReservation;
import it.legacynetwork.chickenwars.routing.ReconnectCoordinator;
import it.legacynetwork.chickenwars.routing.ReconnectDiagnosis;
import it.legacynetwork.chickenwars.routing.ReconnectSessionInspector;
import it.legacynetwork.chickenwars.routing.RouteResult;
import it.legacynetwork.chickenwars.routing.RoutingCoordinator;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.LongSupplier;

/**
 * Orchestrazione proxy-side di {@code /cw rejoin}.
 *
 * <p>Non contiene alcuna regola di routing: la scelta dell'istanza, il
 * controllo dell'heartbeat, il consumo della sessione e la creazione della
 * prenotazione avvengono tutti dentro
 * {@link ReconnectCoordinator#reconnect}, nella stessa transazione del
 * backend. Qui restano soltanto la protezione dalle richieste concorrenti, la
 * risoluzione del server sul proxy, il trasferimento e il ripristino dello
 * stato dopo un fallimento.</p>
 *
 * <p>Nessun metodo blocca il thread chiamante: ogni passo e' incatenato sul
 * {@link CompletionStage} restituito dal backend.</p>
 */
public final class RejoinCoordinator {

    private final ReconnectCoordinator reconnect;
    private final RoutingCoordinator routing;
    private final ReconnectSessionInspector inspector;
    private final ProxyTransferGateway transfers;
    private final RejoinAttemptRegistry attempts;
    private final BackendVerdictRegistry verdicts;
    private final LongSupplier clock;
    private final long reconnectTtlMillis;

    public RejoinCoordinator(ReconnectCoordinator reconnect,
                             RoutingCoordinator routing,
                             ReconnectSessionInspector inspector,
                             ProxyTransferGateway transfers,
                             RejoinAttemptRegistry attempts,
                             BackendVerdictRegistry verdicts,
                             LongSupplier clock,
                             long reconnectTtlMillis) {
        if (reconnect == null || routing == null || inspector == null
                || transfers == null || attempts == null || verdicts == null
                || clock == null) {
            throw new IllegalArgumentException("Coordinatore rejoin incompleto");
        }
        this.reconnect = reconnect;
        this.routing = routing;
        this.inspector = inspector;
        this.transfers = transfers;
        this.attempts = attempts;
        this.verdicts = verdicts;
        this.clock = clock;
        this.reconnectTtlMillis = Math.max(1L, reconnectTtlMillis);
    }

    /**
     * Riporta il giocatore alla propria istanza, se la sessione lo consente.
     *
     * @param playerId giocatore richiedente
     * @return l'esito, mai nullo e mai eccezionale
     */
    public CompletionStage<RejoinResult> rejoin(final UUID playerId) {
        if (playerId == null) {
            return CompletableFuture.completedFuture(
                    RejoinResult.of(RejoinOutcome.PLAYER_ONLY));
        }
        final long now = clock.getAsLong();
        if (!attempts.begin(playerId, now)) {
            // Doppio comando: la prima richiesta e' ancora viva.
            return CompletableFuture.completedFuture(
                    RejoinResult.of(RejoinOutcome.ALREADY_IN_PROGRESS));
        }

        CompletionStage<RejoinResult> pipeline;
        try {
            pipeline = reconnect
                    .reconnect(playerId, idempotencyKey(playerId), now)
                    .thenCompose(result -> onRouted(playerId, result))
                    .exceptionally(failure ->
                            RejoinResult.of(RejoinOutcome.RESERVATION_FAILED));
        } catch (RuntimeException immediate) {
            attempts.finish(playerId);
            return CompletableFuture.completedFuture(
                    RejoinResult.of(RejoinOutcome.RESERVATION_FAILED));
        }

        // Il tentativo va chiuso in ogni caso: successo, errore o eccezione.
        return pipeline.whenComplete((outcome, failure) -> {
            attempts.finish(playerId);
            verdicts.cancel(playerId);
        });
    }

    /**
     * Chiave stabile per giocatore: due invii ravvicinati non possono produrre
     * due prenotazioni distinte.
     */
    private String idempotencyKey(UUID playerId) {
        return "rejoin:" + playerId;
    }

    private CompletionStage<RejoinResult> onRouted(UUID playerId,
                                                   RouteResult result) {
        if (result == null || !result.isSuccessful()) {
            // La richiesta e' stata rifiutata: la diagnosi serve solo a
            // scegliere il messaggio, non a instradare.
            return explain(playerId);
        }

        GameInstanceDescriptor instance = result.getInstance();
        List<GameReservation> reservations = result.getReservations();
        if (instance == null || reservations.isEmpty()) {
            return CompletableFuture.completedFuture(
                    RejoinResult.of(RejoinOutcome.RESERVATION_FAILED));
        }
        GameReservation reservation = reservations.get(0);
        String serverName = instance.getServerName();

        if (transfers.isAlreadyConnectedTo(playerId, serverName)) {
            // Il giocatore e' gia' dove voleva tornare: la prenotazione appena
            // creata va rilasciata, altrimenti occuperebbe un posto.
            return release(playerId, instance, reservation,
                    RejoinOutcome.TRANSFER_STARTED);
        }
        if (!transfers.isServerRegistered(serverName)) {
            return release(playerId, instance, reservation,
                    RejoinOutcome.SERVER_NOT_REGISTERED);
        }

        // L'attesa va aperta prima del trasferimento: il backend potrebbe
        // rispondere non appena il giocatore arriva.
        final CompletableFuture<BackendVerdict> verdict =
                verdicts.await(playerId);
        try {
            return transfers.transfer(playerId, serverName,
                            reservation.getReservationId(), instance.getArenaId())
                    .thenCompose(connected -> Boolean.TRUE.equals(connected)
                            ? awaitBackend(playerId, instance, reservation, verdict)
                            : cancelVerdict(playerId, release(playerId, instance,
                                    reservation, RejoinOutcome.TRANSFER_FAILED)))
                    .exceptionallyCompose(failure -> cancelVerdict(playerId,
                            release(playerId, instance, reservation,
                                    RejoinOutcome.TRANSFER_FAILED)));
        } catch (RuntimeException immediate) {
            return cancelVerdict(playerId, release(playerId, instance,
                    reservation, RejoinOutcome.TRANSFER_FAILED));
        }
    }

    /**
     * Il server e' stato raggiunto: ora decide la validazione ChickenWars.
     *
     * <p>Un rifiuto qui non e' un errore di connessione, quindi non deve
     * confondersi con {@link RejoinOutcome#TRANSFER_FAILED}.</p>
     */
    private CompletionStage<RejoinResult> awaitBackend(
            UUID playerId, GameInstanceDescriptor instance,
            GameReservation reservation,
            CompletableFuture<BackendVerdict> verdict) {
        return verdict
                .exceptionally(failure -> BackendVerdict.timedOut())
                .thenCompose(value -> {
                    if (value != null && value.isAccepted()) {
                        return CompletableFuture.completedFuture(
                                RejoinResult.of(RejoinOutcome.TRANSFER_STARTED,
                                        instance.getServerName(),
                                        instance.getArenaId()));
                    }
                    return onRejected(playerId, instance, reservation, value);
                });
    }

    /**
     * Il server ha rifiutato: il giocatore non deve restarci dentro.
     *
     * <p>La prenotazione viene annullata perche' non sara' mai reclamata, la
     * finestra di reconnect ripristinata per consentire un nuovo tentativo e il
     * giocatore riportato su un server di ripiego. L'allontanamento e' chiesto
     * una volta sola, quindi non puo' generare un ciclo di trasferimenti.</p>
     */
    private CompletionStage<RejoinResult> onRejected(UUID playerId,
                                                     GameInstanceDescriptor instance,
                                                     GameReservation reservation,
                                                     BackendVerdict verdict) {
        final RejoinResult rejected = RejoinResult.rejected(
                instance.getServerName(), arenaOf(instance, verdict),
                verdict == null ? "" : verdict.getReason());

        CompletionStage<Boolean> cancelled;
        try {
            cancelled = routing.cancel(reservation.getReservationId());
        } catch (RuntimeException failure) {
            cancelled = CompletableFuture.completedFuture(Boolean.FALSE);
        }
        return cancelled
                .exceptionally(failure -> Boolean.FALSE)
                .thenCompose(ignored -> remember(playerId, instance))
                .thenCompose(ignored -> evacuate(playerId, instance))
                .thenApply(ignored -> rejected)
                .exceptionally(failure -> rejected);
    }

    private CompletionStage<Boolean> evacuate(UUID playerId,
                                              GameInstanceDescriptor instance) {
        try {
            return transfers.evacuate(playerId, instance.getServerName())
                    .exceptionally(failure -> Boolean.FALSE);
        } catch (RuntimeException failure) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
    }

    private String arenaOf(GameInstanceDescriptor instance,
                           BackendVerdict verdict) {
        if (verdict == null || verdict.getArenaId().isEmpty()) {
            return instance.getArenaId();
        }
        return verdict.getArenaId();
    }

    /**
     * Chiude l'attesa quando il trasferimento non e' nemmeno riuscito.
     */
    private CompletionStage<RejoinResult> cancelVerdict(UUID playerId,
                                                        CompletionStage<RejoinResult> stage) {
        verdicts.cancel(playerId);
        return stage;
    }

    /**
     * Annulla la prenotazione e riapre la finestra di reconnect.
     *
     * <p>Senza il ripristino della sessione un errore di trasferimento
     * impedirebbe per sempre un nuovo tentativo: la sessione risulta gia'
     * consumata dalla transazione precedente.</p>
     */
    private CompletionStage<RejoinResult> release(UUID playerId,
                                                  GameInstanceDescriptor instance,
                                                  GameReservation reservation,
                                                  RejoinOutcome outcome) {
        final RejoinResult result = RejoinResult.of(outcome,
                instance.getServerName(), instance.getArenaId());
        CompletionStage<Boolean> cancelled;
        try {
            cancelled = routing.cancel(reservation.getReservationId());
        } catch (RuntimeException failure) {
            cancelled = CompletableFuture.completedFuture(Boolean.FALSE);
        }
        return cancelled
                .exceptionally(failure -> Boolean.FALSE)
                .thenCompose(ignored -> remember(playerId, instance))
                .thenApply(ignored -> result)
                .exceptionally(failure -> result);
    }

    private CompletionStage<Void> remember(UUID playerId,
                                           GameInstanceDescriptor instance) {
        try {
            return reconnect.remember(playerId, instance.getInstanceId(),
                            clock.getAsLong() + reconnectTtlMillis)
                    .exceptionally(failure -> null);
        } catch (RuntimeException failure) {
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Traduce il rifiuto del backend nel messaggio corretto.
     */
    private CompletionStage<RejoinResult> explain(UUID playerId) {
        try {
            return inspector.inspect(playerId, clock.getAsLong())
                    .thenApply(diagnosis -> RejoinResult.of(describe(diagnosis)))
                    .exceptionally(failure -> RejoinResult.of(
                            RejoinOutcome.INSTANCE_UNAVAILABLE));
        } catch (RuntimeException failure) {
            return CompletableFuture.completedFuture(
                    RejoinResult.of(RejoinOutcome.INSTANCE_UNAVAILABLE));
        }
    }

    static RejoinOutcome describe(ReconnectDiagnosis diagnosis) {
        if (diagnosis == null) {
            return RejoinOutcome.INSTANCE_UNAVAILABLE;
        }
        switch (diagnosis) {
            case NONE:
                return RejoinOutcome.NO_SESSION;
            case EXPIRED:
                return RejoinOutcome.EXPIRED;
            case CONSUMED:
                return RejoinOutcome.ELIMINATED;
            case MATCH_ENDED:
                return RejoinOutcome.MATCH_ENDED;
            case INSTANCE_MISSING:
            case INSTANCE_OFFLINE:
                return RejoinOutcome.INSTANCE_OFFLINE;
            case READY:
            default:
                return RejoinOutcome.INSTANCE_UNAVAILABLE;
        }
    }
}
