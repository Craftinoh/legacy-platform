package it.legacynetwork.chickenwars.region;

import it.legacynetwork.chickenwars.model.SimpleLocation;

import java.util.Locale;

/**
 * Regione cuboidale di una base, usata da trappole e Heal Pool.
 *
 * <p>Il controllo di appartenenza e' puro e non dipende da Bukkit: puo' quindi
 * essere verificato con test unitari e valutato senza scansioni costose.</p>
 */
public final class CuboidRegion {

    private final String world;
    private final double minX;
    private final double minY;
    private final double minZ;
    private final double maxX;
    private final double maxY;
    private final double maxZ;

    private CuboidRegion(String world, double firstX, double firstY,
                         double firstZ, double secondX, double secondY,
                         double secondZ) {
        this.world = world;
        this.minX = Math.min(firstX, secondX);
        this.minY = Math.min(firstY, secondY);
        this.minZ = Math.min(firstZ, secondZ);
        this.maxX = Math.max(firstX, secondX);
        this.maxY = Math.max(firstY, secondY);
        this.maxZ = Math.max(firstZ, secondZ);
    }

    /**
     * Costruisce la regione da due angoli, in qualunque ordine.
     *
     * @param first  primo angolo
     * @param second secondo angolo
     * @return la regione, oppure {@code null} se gli angoli non sono validi
     */
    public static CuboidRegion between(SimpleLocation first,
                                       SimpleLocation second) {
        if (first == null || second == null) {
            return null;
        }
        if (!first.getWorld().equalsIgnoreCase(second.getWorld())) {
            return null;
        }
        return new CuboidRegion(first.getWorld(),
                first.getX(), first.getY(), first.getZ(),
                second.getX(), second.getY(), second.getZ());
    }

    /**
     * Verifica se una posizione ricade nella regione.
     *
     * <p>Il massimo e' inclusivo di un blocco intero, cosi' che un angolo
     * indicato con coordinate di blocco copra anche il blocco stesso.</p>
     */
    public boolean contains(String worldName, double x, double y, double z) {
        if (worldName == null || !world.equalsIgnoreCase(worldName)) {
            return false;
        }
        return x >= minX && x <= maxX + 1.0D
                && y >= minY && y <= maxY + 1.0D
                && z >= minZ && z <= maxZ + 1.0D;
    }

    /**
     * Indica se la regione ha un volume non nullo.
     */
    public boolean isUsable() {
        return world != null && !world.trim().isEmpty();
    }

    public String getWorld() {
        return world;
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMinZ() {
        return minZ;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    public double getMaxZ() {
        return maxZ;
    }

    /**
     * Volume in blocchi, utile per diagnosticare regioni assurde.
     */
    public double getVolume() {
        return (maxX - minX + 1.0D) * (maxY - minY + 1.0D)
                * (maxZ - minZ + 1.0D);
    }

    /**
     * Indica se due regioni si sovrappongono, anche parzialmente.
     */
    public boolean overlaps(CuboidRegion other) {
        if (other == null
                || !world.equalsIgnoreCase(other.world)) {
            return false;
        }
        return minX <= other.maxX && maxX >= other.minX
                && minY <= other.maxY && maxY >= other.minY
                && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT,
                "CuboidRegion{%s, %.1f..%.1f, %.1f..%.1f, %.1f..%.1f}",
                world, minX, maxX, minY, maxY, minZ, maxZ);
    }
}
