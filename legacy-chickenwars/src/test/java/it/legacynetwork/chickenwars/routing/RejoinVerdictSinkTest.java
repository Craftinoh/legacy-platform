package it.legacynetwork.chickenwars.routing;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Invio al proxy dell'esito di un rientro.
 *
 * <p>E' cio' che permette al proxy di distinguere un rifiuto applicativo da un
 * fallimento di connessione: senza questo messaggio i due casi sarebbero
 * indistinguibili.</p>
 */
class RejoinVerdictSinkTest {

    private JavaPlugin plugin;
    private Player player;
    private UUID playerId;
    private BukkitRejoinVerdictSink sink;

    @BeforeEach
    void setUp() {
        plugin = mock(JavaPlugin.class);
        player = mock(Player.class);
        playerId = UUID.randomUUID();
        when(player.isOnline()).thenReturn(Boolean.TRUE.booleanValue());
        sink = new BukkitRejoinVerdictSink(plugin,
                new BukkitRejoinVerdictSink.OnlinePlayerLookup() {
                    @Override
                    public Player find(UUID id) {
                        return playerId.equals(id) ? player : null;
                    }
                });
    }

    private RejoinVerdictCodec.Verdict captureSent() {
        ArgumentCaptor<byte[]> payload = ArgumentCaptor.forClass(byte[].class);
        verify(player).sendPluginMessage(any(JavaPlugin.class),
                anyString(), payload.capture());
        return RejoinVerdictCodec.decode(payload.getValue());
    }

    @Test
    void unRifiutoViaggiaSulCanaleDedicato() {
        sink.report(playerId, false,
                RejoinVerdictCodec.REASON_NO_RESERVATION, "farm");

        verify(player).sendPluginMessage(plugin, RejoinVerdictCodec.CHANNEL,
                RejoinVerdictCodec.encode(playerId, false,
                        RejoinVerdictCodec.REASON_NO_RESERVATION, "farm"));
    }

    @Test
    void ilMotivoRaggiungeIlProxy() {
        sink.report(playerId, false,
                RejoinVerdictCodec.REASON_RECONNECT_REFUSED, "castle");

        RejoinVerdictCodec.Verdict sent = captureSent();
        assertNotNull(sent);
        assertEquals(playerId, sent.getPlayerId());
        assertFalse(sent.isAccepted());
        assertEquals(RejoinVerdictCodec.REASON_RECONNECT_REFUSED,
                sent.getReason());
        assertEquals("castle", sent.getArenaId());
    }

    @Test
    void unAccettazioneViaggiaSenzaMotivo() {
        sink.report(playerId, true, "", "farm");

        RejoinVerdictCodec.Verdict sent = captureSent();
        assertTrue(sent.isAccepted());
        assertEquals("", sent.getReason());
    }

    @Test
    void unGiocatoreUscitoNonRiceveNulla() {
        sink.report(UUID.randomUUID(), false,
                RejoinVerdictCodec.REASON_TIMEOUT, "farm");

        verify(player, never()).sendPluginMessage(any(JavaPlugin.class),
                anyString(), any(byte[].class));
    }

    @Test
    void unGiocatoreOfflineNonRiceveNulla() {
        when(player.isOnline()).thenReturn(Boolean.FALSE.booleanValue());

        sink.report(playerId, false,
                RejoinVerdictCodec.REASON_WRONG_MATCH, "farm");

        verify(player, never()).sendPluginMessage(any(JavaPlugin.class),
                anyString(), any(byte[].class));
    }

    @Test
    void unIdentificatoreMancanteNonProduceInvii() {
        sink.report(null, false, RejoinVerdictCodec.REASON_TIMEOUT, "farm");

        verify(player, never()).sendPluginMessage(any(JavaPlugin.class),
                anyString(), any(byte[].class));
    }

    @Test
    void ogniMotivoPrevistoSopravviveAlProtocollo() {
        for (String reason : new String[]{
                RejoinVerdictCodec.REASON_NO_RESERVATION,
                RejoinVerdictCodec.REASON_RESERVATION_CLAIMED,
                RejoinVerdictCodec.REASON_WRONG_MATCH,
                RejoinVerdictCodec.REASON_RECONNECT_REFUSED,
                RejoinVerdictCodec.REASON_PROFILE_UNAVAILABLE,
                RejoinVerdictCodec.REASON_TIMEOUT}) {
            RejoinVerdictCodec.Verdict decoded = RejoinVerdictCodec.decode(
                    RejoinVerdictCodec.encode(playerId, false, reason, "farm"));
            assertEquals(reason, decoded.getReason());
        }
    }
}
