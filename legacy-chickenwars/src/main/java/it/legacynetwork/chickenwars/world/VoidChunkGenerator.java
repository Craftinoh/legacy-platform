package it.legacynetwork.chickenwars.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Generatore che produce chunk completamente vuoti.
 *
 * <p>E' il generatore adatto alle mappe Chicken Wars, che vengono incollate da
 * uno schematic: il mondo resta vuoto e nulla viene generato attorno all'arena.
 * Usa {@code generateExtBlockSections}, disponibile su 1.8.</p>
 */
public final class VoidChunkGenerator extends ChunkGenerator {

    private static final int SECTION_HEIGHT = 16;

    /**
     * Restituisce sezioni tutte nulle, ovvero interamente aria.
     */
    @Override
    public short[][] generateExtBlockSections(World world, Random random,
                                              int chunkX, int chunkZ,
                                              BiomeGrid biomes) {
        return new short[world.getMaxHeight() / SECTION_HEIGHT][];
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return Collections.emptyList();
    }

    /**
     * Consente lo spawn ovunque: nel vuoto non esistono posizioni sicure.
     */
    @Override
    public boolean canSpawn(World world, int x, int z) {
        return true;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0.5D, 64.0D, 0.5D);
    }
}
