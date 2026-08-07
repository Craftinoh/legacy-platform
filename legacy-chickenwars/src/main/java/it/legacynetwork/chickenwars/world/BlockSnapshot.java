package it.legacynetwork.chickenwars.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Stato di un blocco prima che venisse modificato durante la partita.
 */
public final class BlockSnapshot {

    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final Material material;
    private final byte data;

    @SuppressWarnings("deprecation")
    private BlockSnapshot(Block block) {
        this.world = block.getWorld().getName();
        this.x = block.getX();
        this.y = block.getY();
        this.z = block.getZ();
        this.material = block.getType();
        this.data = block.getData();
    }

    /**
     * Cattura lo stato corrente del blocco.
     *
     * @return la copia, oppure {@code null} se il blocco non e' valido
     */
    public static BlockSnapshot capture(Block block) {
        if (block == null || block.getWorld() == null) {
            return null;
        }
        return new BlockSnapshot(block);
    }

    /**
     * Chiave univoca di una posizione a blocchi, usata negli insiemi.
     */
    public static String key(String world, int x, int y, int z) {
        return world + ':' + x + ':' + y + ':' + z;
    }

    /**
     * Chiave univoca del blocco indicato.
     *
     * @return la chiave, oppure {@code null} se il blocco non e' valido
     */
    public static String key(Block block) {
        if (block == null || block.getWorld() == null) {
            return null;
        }
        return key(block.getWorld().getName(), block.getX(), block.getY(),
                block.getZ());
    }

    /**
     * Riporta il blocco allo stato registrato.
     *
     * <p>Non esegue aggiornamenti fisici sui blocchi vicini, per contenere il
     * costo del ripristino di intere arene.</p>
     *
     * @param bukkitWorld mondo su cui operare, eventualmente nullo
     */
    @SuppressWarnings("deprecation")
    public void restore(World bukkitWorld) {
        if (bukkitWorld == null) {
            return;
        }
        Block block = bukkitWorld.getBlockAt(x, y, z);
        block.setType(material, false);
        if (block.getData() != data) {
            block.setData(data, false);
        }
    }

    public String getWorld() {
        return world;
    }

    public String getKey() {
        return key(world, x, y, z);
    }

    public Material getMaterial() {
        return material;
    }
}
