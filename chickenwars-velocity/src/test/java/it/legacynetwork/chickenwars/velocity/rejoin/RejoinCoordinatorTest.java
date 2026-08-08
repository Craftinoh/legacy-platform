package it.legacynetwork.chickenwars.velocity.rejoin;

import it.legacynetwork.chickenwars.routing.InstanceStatus;
import it.legacynetwork.chickenwars.routing.ReconnectDiagnosis;
import it.legacynetwork.chickenwars.routing.RejoinVerdictCodec;
import it.legacynetwork.chickenwars.routing.RouteFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;

import static it.legacynetwork.chickenwars.velocity.rejoin.RejoinTestSupport.FakeInspector;
import static it.legacynetwork.chickenwars.velocity.rejoin.RejoinTestSupport.FakeReconnect;
import static it.legacynetwork.chickenwars.velocity.rejoin.RejoinTestSupport.FakeRouting;
import static it.legacynetwork.chickenwars.velocity.rejoin.RejoinTestSupport.FakeTransfers;
import static it.legacynetwork.chickenwars.velocity.rejoin.RejoinTestSupport.INSTANCE;
import static it.legacynetwork.chickenwars.velocity.rejoin.RejoinTestSupport.SERVER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comportamento osservabile di {@code /cw rejoin} lato proxy.
 *
 * <p>Ogni test verifica quante prenotazioni, quante cancellazioni e quanti
 * trasferimenti sono stati realmente richiesti: l'esito da solo non
 * distinguerebbe un percorso corretto da uno che duplica il lavoro.</p>
 */
class RejoinCoordinatorTest {

    private UUID playerId;
    private FakeReconnect reconnect;
    private FakeRouting routing;
    private FakeInspector inspector;
    private FakeTransfers transfers;
    private RejoinAttemptRegistry attempts;
    private BackendVerdictRegistry verdicts;
    private RejoinTestSupport.ManualDelayer delayer;
    private AtomicLong clock;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        reconnect = new FakeReconnect();
        routing = new FakeRouting();
        inspector = new FakeInspector();
        delayer = new RejoinTestSupport.ManualDelayer();
        verdicts = new BackendVerdictRegistry(delayer, 5000L);
        transfers = new FakeTransfers().answering(verdicts);
        attempts = new RejoinAttemptRegistry(5000L);
        clock = new AtomicLong(10_000L);
    }

    private RejoinCoordinator coordinator() {
        return new RejoinCoordinator(reconnect, routing, inspector, transfers,
                attempts, verdicts, clock::get, 120_000L);
    }

    private RejoinResult result() {
        CompletionStage<RejoinResult> stage = coordinator().rejoin(playerId);
        return stage.toCompletableFuture().join();
    }

    private RejoinOutcome run() {
        return result().getOutcome();
    }

    // ------------------------------------------------------------------
    // Sessione
    // ------------------------------------------------------------------

    @Test
    void sessioneValidaAvviaIlTrasferimento() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);

        assertSame(RejoinOutcome.TRANSFER_STARTED, run());

        assertEquals(1, reconnect.reconnectCalls());
        assertEquals(1, transfers.transfers().size());
        assertTrue(transfers.transfers().get(0).startsWith(SERVER + "/"));
        // Successo: nessuna cancellazione e nessun ripristino di sessione.
        assertEquals(0, routing.cancelled().size());
        assertEquals(0, reconnect.remembered().size());
    }

    @Test
    void nessunaSessioneProduceIlMessaggioDedicato() {
        reconnect.failing(RouteFailure.STALE_INSTANCE);
        inspector.saying(ReconnectDiagnosis.NONE);

        assertSame(RejoinOutcome.NO_SESSION, run());
        assertEquals(0, transfers.transfers().size());
    }

    @Test
    void sessioneScadutaProduceIlMessaggioDedicato() {
        reconnect.failing(RouteFailure.STALE_INSTANCE);
        inspector.saying(ReconnectDiagnosis.EXPIRED);

        assertSame(RejoinOutcome.EXPIRED, run());
    }

    @Test
    void partitaTerminataProduceIlMessaggioDedicato() {
        reconnect.failing(RouteFailure.STALE_INSTANCE);
        inspector.saying(ReconnectDiagnosis.MATCH_ENDED);

        assertSame(RejoinOutcome.MATCH_ENDED, run());
    }

    @Test
    void sessioneGiaConsumataSegnalaIlRientroNonPiuPossibile() {
        reconnect.failing(RouteFailure.STALE_INSTANCE);
        inspector.saying(ReconnectDiagnosis.CONSUMED);

        assertSame(RejoinOutcome.ELIMINATED, run());
    }

    @Test
    void laFinestraDiReconnectValidaNonVieneRipristinataSuSuccesso() {
        reconnect.succeeding(playerId, InstanceStatus.WAITING);

        run();

        assertEquals(0, reconnect.remembered().size());
    }

    // ------------------------------------------------------------------
    // Istanza
    // ------------------------------------------------------------------

    @Test
    void istanzaInGiocoEuValidaPerIlRientro() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);

        assertSame(RejoinOutcome.TRANSFER_STARTED, run());
    }

    @Test
    void istanzaInAttesaEuValidaPerIlRientro() {
        reconnect.succeeding(playerId, InstanceStatus.WAITING);

        assertSame(RejoinOutcome.TRANSFER_STARTED, run());
    }

    @Test
    void heartbeatScadutoRiportaIstanzaOffline() {
        reconnect.failing(RouteFailure.STALE_INSTANCE);
        inspector.saying(ReconnectDiagnosis.INSTANCE_OFFLINE);

        assertSame(RejoinOutcome.INSTANCE_OFFLINE, run());
        assertEquals(0, transfers.transfers().size());
    }

    @Test
    void istanzaSparitaDalRegistryRiportaOffline() {
        reconnect.failing(RouteFailure.STALE_INSTANCE);
        inspector.saying(ReconnectDiagnosis.INSTANCE_MISSING);

        assertSame(RejoinOutcome.INSTANCE_OFFLINE, run());
    }

    @Test
    void serverNonRegistratoSulProxyAnnullaLaPrenotazione() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.unregistered();

        assertSame(RejoinOutcome.SERVER_NOT_REGISTERED, run());

        assertEquals(0, transfers.transfers().size());
        // La prenotazione appena creata non deve restare appesa.
        assertEquals(1, routing.cancelled().size());
        assertEquals(INSTANCE, reconnect.remembered().get(0));
    }

    @Test
    void istanzaVivaMaNonInstradabileRiportaIndisponibilita() {
        reconnect.failing(RouteFailure.NO_INSTANCE);
        inspector.saying(ReconnectDiagnosis.READY);

        assertSame(RejoinOutcome.INSTANCE_UNAVAILABLE, run());
    }

    // ------------------------------------------------------------------
    // Prenotazione
    // ------------------------------------------------------------------

    @Test
    void laPrenotazioneUsaUnaChiaveIdempotentePerGiocatore() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);

        run();
        attempts.finish(playerId);
        run();

        assertEquals(2, reconnect.keys().size());
        assertEquals(reconnect.keys().get(0), reconnect.keys().get(1));
        assertTrue(reconnect.keys().get(0).contains(playerId.toString()));
    }

    @Test
    void unErroreDelBackendNonLasciaPrenotazioniAppese() {
        reconnect.throwing(new IllegalStateException("database irraggiungibile"));

        assertSame(RejoinOutcome.RESERVATION_FAILED, run());
        assertEquals(0, transfers.transfers().size());
        assertEquals(0, attempts.size());
    }

    @Test
    void unaDiagnosiFallitaNonPropagaLEccezione() {
        reconnect.failing(RouteFailure.STALE_INSTANCE);
        inspector.throwing(new IllegalStateException("diagnosi non disponibile"));

        assertSame(RejoinOutcome.INSTANCE_UNAVAILABLE, run());
    }

    // ------------------------------------------------------------------
    // Trasferimento
    // ------------------------------------------------------------------

    @Test
    void ilTrasferimentoPortaLaPrenotazioneEuLArena() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);

        run();

        String recorded = transfers.transfers().get(0);
        assertEquals(SERVER + "/res-" + playerId + "/"
                + RejoinTestSupport.ARENA, recorded);
    }

    @Test
    void unTrasferimentoRifiutatoAnnullaERipristina() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.refusing();

        assertSame(RejoinOutcome.TRANSFER_FAILED, run());

        assertEquals(1, transfers.transfers().size());
        assertEquals(1, routing.cancelled().size());
        // Senza il ripristino il giocatore non potrebbe piu' riprovare.
        assertEquals(INSTANCE, reconnect.remembered().get(0));
    }

    @Test
    void unEccezioneDelProxyNonPropagaAlChiamante() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.throwing(new IllegalStateException("proxy in errore"));

        assertSame(RejoinOutcome.TRANSFER_FAILED, run());
        assertEquals(1, routing.cancelled().size());
    }

    @Test
    void giocatoreGiaSulServerNonVieneTrasferitoDueVolte() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.alreadyConnected();

        assertSame(RejoinOutcome.TRANSFER_STARTED, run());

        assertEquals(0, transfers.transfers().size());
        // Il posto prenotato inutilmente viene rilasciato.
        assertEquals(1, routing.cancelled().size());
    }

    @Test
    void dopoOgniEsitoIlTentativoVieneChiuso() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        run();
        assertEquals(0, attempts.size());

        transfers.refusing();
        run();
        assertEquals(0, attempts.size());
    }

    // ------------------------------------------------------------------
    // Concorrenza
    // ------------------------------------------------------------------

    @Test
    void unaSecondaRichiestaContemporaneaVieneRifiutata() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        // Il tentativo resta aperto: simula la prima richiesta ancora in volo.
        assertTrue(attempts.begin(playerId, clock.get()));

        assertSame(RejoinOutcome.ALREADY_IN_PROGRESS, run());

        assertEquals(0, reconnect.reconnectCalls());
        assertEquals(0, transfers.transfers().size());
    }

    @Test
    void dueComandiRavvicinatiProduconoUnSoloTrasferimento() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        RejoinCoordinator coordinator = coordinator();

        RejoinOutcome first = coordinator.rejoin(playerId)
                .toCompletableFuture().join().getOutcome();
        // Il primo tentativo si e' gia' chiuso: il secondo e' un retry lecito.
        assertSame(RejoinOutcome.TRANSFER_STARTED, first);

        assertTrue(attempts.begin(playerId, clock.get()));
        RejoinOutcome second = coordinator.rejoin(playerId)
                .toCompletableFuture().join().getOutcome();

        assertSame(RejoinOutcome.ALREADY_IN_PROGRESS, second);
        assertEquals(1, transfers.transfers().size());
    }

    @Test
    void dopoUnErroreUnNuovoTentativoEuConsentito() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.refusing();
        assertSame(RejoinOutcome.TRANSFER_FAILED, run());

        // Nessun cooldown lungo: il retry parte subito.
        assertEquals(0, attempts.size());
        assertSame(RejoinOutcome.TRANSFER_FAILED, run());
        assertEquals(2, reconnect.reconnectCalls());
    }

    @Test
    void unGiocatoreNulloNonAvviaNulla() {
        assertSame(RejoinOutcome.PLAYER_ONLY, coordinator().rejoin(null)
                .toCompletableFuture().join().getOutcome());
        assertEquals(0, reconnect.reconnectCalls());
    }

    // ------------------------------------------------------------------
    // Esito del backend
    // ------------------------------------------------------------------

    @Test
    void ilBackendAccettaEdIlRientroRiesce() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);

        RejoinResult result = result();

        assertSame(RejoinOutcome.TRANSFER_STARTED, result.getOutcome());
        assertEquals(SERVER, result.getServerName());
        assertEquals(RejoinTestSupport.ARENA, result.getArenaId());
    }

    @Test
    void prenotazioneAssenteProduceBackendRejected() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.backendRejects(RejoinVerdictCodec.REASON_NO_RESERVATION);

        RejoinResult result = result();

        // Il server e' stato raggiunto: non e' un fallimento di trasferimento.
        assertSame(RejoinOutcome.BACKEND_REJECTED, result.getOutcome());
        assertEquals(RejoinVerdictCodec.REASON_NO_RESERVATION,
                result.getReason());
        assertEquals(1, transfers.transfers().size());
    }

    @Test
    void prenotazioneGiaReclamataProduceBackendRejected() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.backendRejects(RejoinVerdictCodec.REASON_RESERVATION_CLAIMED);

        assertEquals(RejoinVerdictCodec.REASON_RESERVATION_CLAIMED,
                result().getReason());
    }

    @Test
    void matchErratoProduceBackendRejected() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.backendRejects(RejoinVerdictCodec.REASON_WRONG_MATCH);

        RejoinResult result = result();

        assertSame(RejoinOutcome.BACKEND_REJECTED, result.getOutcome());
        assertEquals(RejoinVerdictCodec.REASON_WRONG_MATCH, result.getReason());
    }

    @Test
    void giocatoreEliminatoProduceBackendRejected() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.backendRejects(RejoinVerdictCodec.REASON_RECONNECT_REFUSED);

        assertEquals(RejoinVerdictCodec.REASON_RECONNECT_REFUSED,
                result().getReason());
    }

    @Test
    void profiloNonDisponibileProduceBackendRejected() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.backendRejects(RejoinVerdictCodec.REASON_PROFILE_UNAVAILABLE);

        assertSame(RejoinOutcome.BACKEND_REJECTED, result().getOutcome());
    }

    @Test
    void ilSilenzioDelBackendScadeInBackendRejected() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.backendSilent();

        CompletionStage<RejoinResult> stage = coordinator().rejoin(playerId);
        // Nessuna risposta: scatta il timeout previsto dal protocollo.
        delayer.fire();
        RejoinResult result = stage.toCompletableFuture().join();

        assertSame(RejoinOutcome.BACKEND_REJECTED, result.getOutcome());
        assertEquals(RejoinVerdictCodec.REASON_TIMEOUT, result.getReason());
    }

    @Test
    void unRifiutoDelBackendNonEuUnTransferFailed() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.backendRejects(RejoinVerdictCodec.REASON_NO_RESERVATION);

        assertSame(RejoinOutcome.BACKEND_REJECTED, run());

        // Simmetrico: la connessione fallita resta transfer-failed.
        setUp();
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.answering(verdicts).refusing();
        assertSame(RejoinOutcome.TRANSFER_FAILED, run());
    }

    @Test
    void dopoUnRifiutoIlTentativoEuChiusoEuLAttesaLiberata() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.backendRejects(RejoinVerdictCodec.REASON_NO_RESERVATION);

        run();

        assertEquals(0, attempts.size());
        assertEquals(0, verdicts.size());
    }

    @Test
    void dopoUnRifiutoUnNuovoTentativoEuConsentito() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.backendRejects(RejoinVerdictCodec.REASON_NO_RESERVATION);
        assertSame(RejoinOutcome.BACKEND_REJECTED, run());

        transfers.backendAccepts();
        assertSame(RejoinOutcome.TRANSFER_STARTED, run());
        assertEquals(2, reconnect.reconnectCalls());
    }

    @Test
    void unaConnessioneFallitaNonLasciaAttesePendenti() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.refusing();

        assertSame(RejoinOutcome.TRANSFER_FAILED, run());
        assertEquals(0, verdicts.size());
    }

    // ------------------------------------------------------------------
    // Uscita sicura dall'istanza che ha rifiutato
    // ------------------------------------------------------------------

    @Test
    void dopoUnRifiutoIlGiocatoreVieneAllontanato() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.backendRejects(RejoinVerdictCodec.REASON_NO_RESERVATION);

        assertSame(RejoinOutcome.BACKEND_REJECTED, run());

        // Restare sull'istanza che lo ha rifiutato non e' uno stato valido.
        assertEquals(1, transfers.evacuations().size());
        assertEquals(SERVER, transfers.evacuations().get(0));
    }

    @Test
    void ilRipiegoVieneChiestoUnaVoltaSola() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.backendRejects(RejoinVerdictCodec.REASON_WRONG_MATCH);

        run();

        assertEquals(1, transfers.evacuations().size());
        // Un solo trasferimento verso l'istanza: nessun ciclo.
        assertEquals(1, transfers.transfers().size());
    }

    @Test
    void ilRifiutoAnnullaLaPrenotazionePrimaDiAllontanare() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.backendRejects(RejoinVerdictCodec.REASON_RECONNECT_REFUSED);

        run();

        // La prenotazione non sara' mai reclamata: va rilasciata.
        assertEquals(1, routing.cancelled().size());
        assertEquals(0, routing.claimed().size());
        assertEquals(INSTANCE, reconnect.remembered().get(0));
    }

    @Test
    void ancheIlTimeoutAllontanaIlGiocatore() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.backendSilent();

        CompletionStage<RejoinResult> stage = coordinator().rejoin(playerId);
        delayer.fire();
        stage.toCompletableFuture().join();

        assertEquals(1, transfers.evacuations().size());
        assertEquals(1, routing.cancelled().size());
    }

    @Test
    void unRipiegoNonRiuscitoNonImpedisceIlMessaggio() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.backendRejects(RejoinVerdictCodec.REASON_NO_RESERVATION)
                .evacuationRefused();

        RejoinResult result = result();

        assertSame(RejoinOutcome.BACKEND_REJECTED, result.getOutcome());
        assertEquals(RejoinVerdictCodec.REASON_NO_RESERVATION,
                result.getReason());
        assertEquals(0, attempts.size());
    }

    @Test
    void unEccezioneDelRipiegoNonPropagaAlChiamante() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.backendRejects(RejoinVerdictCodec.REASON_WRONG_MATCH)
                .evacuationThrowing(new IllegalStateException("proxy in errore"));

        assertSame(RejoinOutcome.BACKEND_REJECTED, run());
        assertEquals(0, attempts.size());
    }

    @Test
    void dopoUnRifiutoEuAllontanamentoIlRetryEuConsentito() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.backendRejects(RejoinVerdictCodec.REASON_NO_RESERVATION);
        assertSame(RejoinOutcome.BACKEND_REJECTED, run());

        transfers.backendAccepts();
        assertSame(RejoinOutcome.TRANSFER_STARTED, run());

        assertEquals(2, reconnect.reconnectCalls());
        // Il secondo tentativo non riusa la prenotazione annullata.
        assertEquals(1, routing.cancelled().size());
        assertEquals(1, transfers.evacuations().size());
    }

    @Test
    void unTrasferimentoRiuscitoNonAllontanaNessuno() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);

        assertSame(RejoinOutcome.TRANSFER_STARTED, run());

        assertEquals(0, transfers.evacuations().size());
        assertEquals(0, routing.cancelled().size());
    }

    @Test
    void unaConnessioneFallitaNonAllontanaNessuno() {
        reconnect.succeeding(playerId, InstanceStatus.INGAME);
        transfers.refusing();

        assertSame(RejoinOutcome.TRANSFER_FAILED, run());

        // Il giocatore non e' mai arrivato: non c'e' nulla da cui uscire.
        assertEquals(0, transfers.evacuations().size());
    }
}
