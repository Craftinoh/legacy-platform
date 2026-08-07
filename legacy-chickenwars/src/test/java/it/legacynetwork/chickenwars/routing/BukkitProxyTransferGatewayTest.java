package it.legacynetwork.chickenwars.routing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BukkitProxyTransferGatewayTest {
    @Test void forwardsReservationThenConnects() throws Exception {JavaPlugin plugin=mock(JavaPlugin.class);Player player=mock(Player.class);when(player.isOnline()).thenReturn(true);UUID id=UUID.randomUUID();BukkitProxyTransferGateway gateway=new BukkitProxyTransferGateway(plugin,ignored->player);gateway.transfer(id,"game-1","reservation","arena");ArgumentCaptor<byte[]> bytes=ArgumentCaptor.forClass(byte[].class);verify(player,times(2)).sendPluginMessage(eq(plugin),eq("BungeeCord"),bytes.capture());List<byte[]> messages=bytes.getAllValues();DataInputStream forward=new DataInputStream(new ByteArrayInputStream(messages.get(0)));assertEquals("Forward",forward.readUTF());assertEquals("game-1",forward.readUTF());assertEquals("ChickenWarsReservation",forward.readUTF());int length=forward.readUnsignedShort();byte[] payload=new byte[length];forward.readFully(payload);DataInputStream data=new DataInputStream(new ByteArrayInputStream(payload));assertEquals(id.toString(),data.readUTF());assertEquals("reservation",data.readUTF());assertEquals("arena",data.readUTF());DataInputStream connect=new DataInputStream(new ByteArrayInputStream(messages.get(1)));assertEquals("Connect",connect.readUTF());assertEquals("game-1",connect.readUTF());}
    @Test void lobbyConnectSendsOnlyConnectMessage() throws Exception {JavaPlugin plugin=mock(JavaPlugin.class);Player player=mock(Player.class);when(player.isOnline()).thenReturn(true);UUID id=UUID.randomUUID();BukkitProxyTransferGateway gateway=new BukkitProxyTransferGateway(plugin,ignored->player);gateway.connect(id,"lobby-1");ArgumentCaptor<byte[]> bytes=ArgumentCaptor.forClass(byte[].class);verify(player).sendPluginMessage(eq(plugin),eq("BungeeCord"),bytes.capture());DataInputStream connect=new DataInputStream(new ByteArrayInputStream(bytes.getValue()));assertEquals("Connect",connect.readUTF());assertEquals("lobby-1",connect.readUTF());}
}
