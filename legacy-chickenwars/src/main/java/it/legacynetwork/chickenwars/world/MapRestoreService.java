package it.legacynetwork.chickenwars.world;

import it.legacynetwork.chickenwars.arena.ArenaDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ripristino della mappa basato sulle sole modifiche realmente avvenute.
 *
 * <p>Vengono registrati i blocchi piazzati dai giocatori e lo stato originale di
 * quelli distrutti; il ripristino tocca quindi un numero di blocchi
 * proporzionale all'attivita' della partita e non alle dimensioni dell'arena.</p>
 */
public final class MapRestoreService {

    private final Set<String> placed = new HashSet<String>();
    private final List<BlockSnapshot> broken = new ArrayList<BlockSnapshot>();

    /**
     * Registra un blocco piazzato da un giocatore.
     */
    public void recordPlace(Block block) {
        String key = BlockSnapshot.key(block);
        if (key != null) {
            placed.add(key);
        }
    }

    /**
     * Registra la distruzione di un blocco, salvandone lo stato precedente.
     *
     * <p>I blocchi piazzati durante la partita non vengono memorizzati: e'
     * sufficiente rimuoverli dall'elenco dei piazzati.</p>
     */
    public void recordBreak(Block block) {
        String key = BlockSnapshot.key(block);
        if (key == null) {
            return;
        }
        if (placed.remove(key)) {
            return;
        }
        BlockSnapshot snapshot = BlockSnapshot.capture(block);
        if (snapshot != null) {
            broken.add(snapshot);
        }
    }

    /**
     * Indica se il blocco e' stato piazzato durante la partita.
     */
    public boolean isPlaced(Block block) {
        String key = BlockSnapshot.key(block);
        return key != null && placed.contains(key);
    }

    public int getPlacedCount() {
        return placed.size();
    }

    public int getBrokenCount() {
        return broken.size();
    }

    /**
     * Riporta l'arena allo stato iniziale.
     *
     * <p>Rimuove i blocchi piazzati, ricostruisce quelli distrutti e ripulisce
     * entita' e oggetti a terra rimasti nella regione. Va eseguito sul thread
     * principale.</p>
     *
     * @param arena definizione usata per delimitare la pulizia delle entita'
     * @return il numero di blocchi ripristinati
     */
    public int restore(ArenaDefinition arena) {
        int restored = 0;

        for (String key : placed) {
            Block block = resolve(key);
            if (block != null && block.getType() != Material.AIR) {
                block.setType(Material.AIR, false);
                restored++;
            }
        }
        placed.clear();

        for (int i = broken.size() - 1; i >= 0; i--) {
            BlockSnapshot snapshot = broken.get(i);
            snapshot.restore(Bukkit.getWorld(snapshot.getWorld()));
            restored++;
        }
        broken.clear();

        if (arena != null) {
            clearEntities(arena);
        }
        return restored;
    }

    /**
     * Rimuove oggetti a terra ed entita' non giocatore dentro la regione.
     */
    public void clearEntities(ArenaDefinition arena) {
        if (arena == null || arena.getWorld() == null) {
            return;
        }
        World world = Bukkit.getWorld(arena.getWorld());
        if (world == null) {
            return;
        }
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Player) {
                continue;
            }
            if (!arena.contains(world.getName(), entity.getLocation().getX(),
                    entity.getLocation().getY(), entity.getLocation().getZ())) {
                continue;
            }
            entity.remove();
        }
    }

    /**
     * Rimuove i soli oggetti a terra della regione, lasciando le altre entita'.
     */
    public void clearGroundItems(ArenaDefinition arena) {
        if (arena == null || arena.getWorld() == null) {
            return;
        }
        World world = Bukkit.getWorld(arena.getWorld());
        if (world == null) {
            return;
        }
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Item)) {
                continue;
            }
            if (arena.contains(world.getName(), entity.getLocation().getX(),
                    entity.getLocation().getY(), entity.getLocation().getZ())) {
                entity.remove();
            }
        }
    }

    private Block resolve(String key) {
        int lastSeparator = key.lastIndexOf(':');
        int middleSeparator = key.lastIndexOf(':', lastSeparator - 1);
        int firstSeparator = key.lastIndexOf(':', middleSeparator - 1);
        if (firstSeparator <= 0) {
            return null;
        }
        World world = Bukkit.getWorld(key.substring(0, firstSeparator));
        if (world == null) {
            return null;
        }
        try {
            int x = Integer.parseInt(key.substring(firstSeparator + 1, middleSeparator));
            int y = Integer.parseInt(key.substring(middleSeparator + 1, lastSeparator));
            int z = Integer.parseInt(key.substring(lastSeparator + 1));
            return world.getBlockAt(x, y, z);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * Azzera le registrazioni senza toccare il mondo.
     */
    public void reset() {
        placed.clear();
        broken.clear();
    }
}
