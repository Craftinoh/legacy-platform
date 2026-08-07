package it.legacynetwork.chickenwars.lobby;

import static org.junit.jupiter.api.Assertions.*;
import it.legacynetwork.chickenwars.mode.MatchMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class LobbySelectorHolderTest {
    @Test void holderIsAuthoritativeAndUnknownSlotsDoNothing(){Map<Integer,MatchMode> modes=new LinkedHashMap<Integer,MatchMode>();modes.put(3,MatchMode.SOLO);LobbySelectorHolder holder=new LobbySelectorHolder(modes);assertEquals(MatchMode.SOLO,holder.modeAt(3));assertNull(holder.modeAt(4));}
    @Test void inventoryCanOnlyBeAttachedOnce(){LobbySelectorHolder holder=new LobbySelectorHolder(new LinkedHashMap<Integer,MatchMode>());holder.attach(mock(Inventory.class));assertThrows(IllegalStateException.class,()->holder.attach(mock(Inventory.class)));}
}
