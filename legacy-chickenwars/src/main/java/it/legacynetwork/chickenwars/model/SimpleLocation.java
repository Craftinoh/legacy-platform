package it.legacynetwork.chickenwars.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Locale;

/**
 * Posizione serializzabile indipendente dal caricamento del mondo.
 *
 * <p>Il formato testuale usato nei file YAML e' {@code mondo,x,y,z,yaw,pitch};
 * la forma ridotta {@code mondo,x,y,z} viene accettata e completata con yaw e
 * pitch pari a zero.</p>
 */
public final class SimpleLocation {

    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public SimpleLocation(String world, double x, double y, double z,
                          float yaw, float pitch) {
        if (world == null || world.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome mondo mancante");
        }
        this.world = world.trim();
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /**
     * Converte il formato testuale in una posizione.
     *
     * @param raw testo da convertire, eventualmente nullo
     * @return la posizione, oppure {@code null} se il testo non e' valido
     */
    public static SimpleLocation parse(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split(",");
        if (parts.length != 4 && parts.length != 6) {
            return null;
        }
        String worldName = parts[0].trim();
        if (worldName.isEmpty()) {
            return null;
        }
        try {
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());
            float yaw = parts.length == 6 ? Float.parseFloat(parts[4].trim()) : 0.0F;
            float pitch = parts.length == 6 ? Float.parseFloat(parts[5].trim()) : 0.0F;
            if (Double.isNaN(x) || Double.isInfinite(x)
                    || Double.isNaN(y) || Double.isInfinite(y)
                    || Double.isNaN(z) || Double.isInfinite(z)
                    || Float.isNaN(yaw) || Float.isInfinite(yaw)
                    || Float.isNaN(pitch) || Float.isInfinite(pitch)) {
                return null;
            }
            return new SimpleLocation(worldName, x, y, z, yaw, pitch);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static SimpleLocation of(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return new SimpleLocation(location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    /**
     * Serializza la posizione nel formato accettato da {@link #parse(String)}.
     */
    public String serialize() {
        return world + ","
                + format(x) + "," + format(y) + "," + format(z) + ","
                + format(yaw) + "," + format(pitch);
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /**
     * Risolve la posizione Bukkit, se il mondo indicato risulta caricato.
     *
     * @return la posizione, oppure {@code null} se il mondo non e' caricato
     */
    public Location toLocation() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return null;
        }
        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }

    /**
     * Sposta la posizione al centro del blocco mantenendo la rotazione.
     */
    public SimpleLocation centered() {
        return new SimpleLocation(world,
                Math.floor(x) + 0.5D, y, Math.floor(z) + 0.5D, yaw, pitch);
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SimpleLocation)) {
            return false;
        }
        SimpleLocation that = (SimpleLocation) other;
        return world.equals(that.world)
                && Double.compare(x, that.x) == 0
                && Double.compare(y, that.y) == 0
                && Double.compare(z, that.z) == 0
                && Float.compare(yaw, that.yaw) == 0
                && Float.compare(pitch, that.pitch) == 0;
    }

    @Override
    public int hashCode() {
        int result = world.hashCode();
        result = 31 * result + Double.valueOf(x).hashCode();
        result = 31 * result + Double.valueOf(y).hashCode();
        result = 31 * result + Double.valueOf(z).hashCode();
        result = 31 * result + Float.valueOf(yaw).hashCode();
        result = 31 * result + Float.valueOf(pitch).hashCode();
        return result;
    }

    @Override
    public String toString() {
        return serialize();
    }
}
