package it.legacynetwork.lobby.bossbar.packet;

import com.github.retrooper.packetevents.protocol.world.Location;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NmsV1_8R3BossBarPacketAdapter implements BossBarPacketAdapter {

    private static final Logger LOGGER =
            Logger.getLogger(NmsV1_8R3BossBarPacketAdapter.class.getName());
    private static final float WITHER_MAX_HEALTH = 300.0F;
    private static final float WITHER_MIN_HEALTH = 1.0F;

    private static final String NMS_VERSION;
    private static final String NMS_PREFIX;
    private static final String CB_PREFIX;

    private static final Class<?> ENTITY_WITHER_CLASS;
    private static final Class<?> WORLD_CLASS;
    private static final Class<?> ENTITY_CLASS;
    private static final Class<?> ENTITY_LIVING_CLASS;
    private static final Class<?> PACKET_CLASS;
    private static final Class<?> SPAWN_LIVING_CLASS;
    private static final Class<?> ENTITY_METADATA_CLASS;
    private static final Class<?> ENTITY_TELEPORT_CLASS;
    private static final Class<?> ENTITY_DESTROY_CLASS;
    private static final Class<?> DATA_WATCHER_CLASS;
    private static final Class<?> ENTITY_PLAYER_CLASS;
    private static final Class<?> PLAYER_CONNECTION_CLASS;
    private static final Class<?> CRAFT_PLAYER_CLASS;
    private static final Class<?> CRAFT_WORLD_CLASS;

    private static final Constructor<?> ENTITY_WITHER_CONSTRUCTOR;
    private static final Constructor<?> SPAWN_LIVING_CONSTRUCTOR;
    private static final Constructor<?> ENTITY_METADATA_CONSTRUCTOR;
    private static final Constructor<?> ENTITY_TELEPORT_CONSTRUCTOR;
    private static final Constructor<?> ENTITY_DESTROY_CONSTRUCTOR;

    private static final Method SET_LOCATION_METHOD;
    private static final Method SET_INVISIBLE_METHOD;
    private static final Method SET_CUSTOM_NAME_METHOD;
    private static final Method SET_CUSTOM_NAME_VISIBLE_METHOD;
    private static final Method SET_HEALTH_METHOD;
    private static final Method SET_SILENT_METHOD;
    private static final Method GET_DATA_WATCHER_METHOD;
    private static final Method GET_HANDLE_METHOD;
    private static final Method SEND_PACKET_METHOD;
    private static final Method CRAFT_WORLD_GET_HANDLE_METHOD;

    private static final Field PLAYER_CONNECTION_FIELD;
    private static final Field ENTITY_ID_FIELD;

    static {
        String version = null;
        String nmsPrefix = null;
        String cbPrefix = null;
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            version = packageName.split("\\.")[3];
            nmsPrefix = "net.minecraft.server." + version + ".";
            cbPrefix = "org.bukkit.craftbukkit." + version + ".";

            ENTITY_WITHER_CLASS = Class.forName(nmsPrefix + "EntityWither");
            WORLD_CLASS = Class.forName(nmsPrefix + "World");
            ENTITY_CLASS = Class.forName(nmsPrefix + "Entity");
            ENTITY_LIVING_CLASS = Class.forName(nmsPrefix + "EntityLiving");
            PACKET_CLASS = Class.forName(nmsPrefix + "Packet");
            SPAWN_LIVING_CLASS = Class.forName(nmsPrefix + "PacketPlayOutSpawnEntityLiving");
            ENTITY_METADATA_CLASS = Class.forName(nmsPrefix + "PacketPlayOutEntityMetadata");
            ENTITY_TELEPORT_CLASS = Class.forName(nmsPrefix + "PacketPlayOutEntityTeleport");
            ENTITY_DESTROY_CLASS = Class.forName(nmsPrefix + "PacketPlayOutEntityDestroy");
            DATA_WATCHER_CLASS = Class.forName(nmsPrefix + "DataWatcher");
            ENTITY_PLAYER_CLASS = Class.forName(nmsPrefix + "EntityPlayer");
            PLAYER_CONNECTION_CLASS = Class.forName(nmsPrefix + "PlayerConnection");
            CRAFT_PLAYER_CLASS = Class.forName(cbPrefix + "entity.CraftPlayer");
            CRAFT_WORLD_CLASS = Class.forName(cbPrefix + "CraftWorld");

            ENTITY_WITHER_CONSTRUCTOR = ENTITY_WITHER_CLASS.getConstructor(WORLD_CLASS);
            SPAWN_LIVING_CONSTRUCTOR = SPAWN_LIVING_CLASS.getConstructor(ENTITY_LIVING_CLASS);
            ENTITY_METADATA_CONSTRUCTOR = ENTITY_METADATA_CLASS.getConstructor(
                    int.class, DATA_WATCHER_CLASS, boolean.class);
            ENTITY_TELEPORT_CONSTRUCTOR = ENTITY_TELEPORT_CLASS.getConstructor(
                    int.class, int.class, int.class, int.class,
                    byte.class, byte.class, boolean.class);
            ENTITY_DESTROY_CONSTRUCTOR = ENTITY_DESTROY_CLASS.getConstructor(int[].class);

            SET_LOCATION_METHOD = ENTITY_CLASS.getMethod("setLocation",
                    double.class, double.class, double.class, float.class, float.class);
            SET_INVISIBLE_METHOD = ENTITY_CLASS.getMethod("setInvisible", boolean.class);
            SET_CUSTOM_NAME_METHOD = ENTITY_CLASS.getMethod("setCustomName", String.class);
            SET_CUSTOM_NAME_VISIBLE_METHOD = ENTITY_CLASS.getMethod(
                    "setCustomNameVisible", boolean.class);
            SET_HEALTH_METHOD = ENTITY_LIVING_CLASS.getMethod("setHealth", float.class);
            GET_DATA_WATCHER_METHOD = ENTITY_CLASS.getMethod("getDataWatcher");
            GET_HANDLE_METHOD = CRAFT_PLAYER_CLASS.getMethod("getHandle");
            SEND_PACKET_METHOD = PLAYER_CONNECTION_CLASS.getMethod(
                    "sendPacket", PACKET_CLASS);
            CRAFT_WORLD_GET_HANDLE_METHOD = CRAFT_WORLD_CLASS.getMethod("getHandle");

            Method silentMethod = null;
            try {
                silentMethod = ENTITY_CLASS.getMethod("setSilent", boolean.class);
            } catch (NoSuchMethodException ignored) {
                // setSilent is optional on forks.
            }
            SET_SILENT_METHOD = silentMethod;

            PLAYER_CONNECTION_FIELD = ENTITY_PLAYER_CLASS.getField("playerConnection");
            ENTITY_ID_FIELD = ENTITY_CLASS.getDeclaredField("id");
            ENTITY_ID_FIELD.setAccessible(true);
        } catch (Exception exception) {
            NMS_VERSION = version;
            NMS_PREFIX = nmsPrefix;
            CB_PREFIX = cbPrefix;
            throw new RuntimeException(
                    "NmsV1_8R3BossBarPacketAdapter init failed for version "
                            + version, exception);
        }
        NMS_VERSION = version;
        NMS_PREFIX = nmsPrefix;
        CB_PREFIX = cbPrefix;
    }

    private boolean debugPackets;
    private boolean debugLifecycle;
    private double distance = 24.0D;
    private double verticalOffset;

    private final ConcurrentHashMap<Integer, Location> positionCache =
            new ConcurrentHashMap<Integer, Location>();
    private final ConcurrentHashMap<Integer, Object> withers =
            new ConcurrentHashMap<Integer, Object>();

    public void setDebugPackets(boolean debug) {
        this.debugPackets = debug;
    }

    public void setDebugLifecycle(boolean debug) {
        this.debugLifecycle = debug;
    }

    public void setDistance(double distance) {
        this.distance = Math.max(1.0D, distance);
    }

    public void setVerticalOffset(double verticalOffset) {
        this.verticalOffset = verticalOffset;
    }

    public static boolean validateServerVersion() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            return "v1_8_R3".equals(packageName.split("\\.")[3]);
        } catch (Exception exception) {
            return false;
        }
    }

    @Override
    public int spawnWither(Player player, int entityId, String text, double progress) {
        try {
            destroyExisting(player, entityId);

            Location location = calculatePosition(player);
            float health = healthFromProgress(progress);
            Object nmsWorld = CRAFT_WORLD_GET_HANDLE_METHOD.invoke(player.getWorld());
            Object wither = ENTITY_WITHER_CONSTRUCTOR.newInstance(nmsWorld);

            ENTITY_ID_FIELD.setInt(wither, entityId);
            SET_LOCATION_METHOD.invoke(wither, location.getX(), location.getY(),
                    location.getZ(), 0.0F, 0.0F);
            SET_INVISIBLE_METHOD.invoke(wither, true);
            SET_CUSTOM_NAME_METHOD.invoke(wither, text);
            SET_CUSTOM_NAME_VISIBLE_METHOD.invoke(wither, false);
            SET_HEALTH_METHOD.invoke(wither, health);
            if (SET_SILENT_METHOD != null) {
                SET_SILENT_METHOD.invoke(wither, true);
            }

            Object spawnPacket = SPAWN_LIVING_CONSTRUCTOR.newInstance(wither);
            sendPacket(player, spawnPacket);

            withers.put(entityId, wither);
            positionCache.put(entityId, location);

            if (debugLifecycle) {
                LOGGER.info("BossBar adapter=NmsV1_8R3BossBarPacketAdapter"
                        + " version=" + NMS_VERSION
                        + " player=" + player.getName()
                        + " entity=" + entityId
                        + " native-entity=EntityWither"
                        + " spawn-sent=true invisible=true"
                        + " custom-name-visible=false health=" + health);
            }
            if (debugPackets) {
                LOGGER.info("BossBar player=" + player.getName()
                        + " entity=" + entityId
                        + " packet=SPAWN_LIVING_ENTITY type=WITHER"
                        + " x=" + location.getX()
                        + " y=" + location.getY()
                        + " z=" + location.getZ());
            }
            return entityId;
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE,
                    "BossBar spawn fallito per " + player.getName()
                            + " entity=" + entityId, exception);
            cleanup(entityId);
            tryDestroy(player, entityId);
            return -1;
        }
    }

    @Override
    public void updateTextAndProgress(Player player, int entityId,
                                      String text, double progress) {
        Object wither = withers.get(entityId);
        if (wither == null) {
            return;
        }
        try {
            float health = healthFromProgress(progress);
            SET_CUSTOM_NAME_METHOD.invoke(wither, text);
            SET_CUSTOM_NAME_VISIBLE_METHOD.invoke(wither, false);
            SET_INVISIBLE_METHOD.invoke(wither, true);
            SET_HEALTH_METHOD.invoke(wither, health);

            Object dataWatcher = GET_DATA_WATCHER_METHOD.invoke(wither);
            Object metadataPacket = ENTITY_METADATA_CONSTRUCTOR.newInstance(
                    entityId, dataWatcher, true);
            sendPacket(player, metadataPacket);

            if (debugPackets) {
                LOGGER.info("BossBar player=" + player.getName()
                        + " entity=" + entityId
                        + " packet=ENTITY_METADATA"
                        + " native-datawatcher=true health=" + health);
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE,
                    "BossBar update fallito per " + player.getName()
                            + " entity=" + entityId, exception);
            destroy(player, entityId);
        }
    }

    @Override
    public void updatePosition(Player player, int entityId,
                               double x, double y, double z) {
        Object wither = withers.get(entityId);
        if (wither == null || !isFinite(x) || !isFinite(y) || !isFinite(z)) {
            return;
        }
        try {
            Location previous = positionCache.get(entityId);
            if (previous != null
                    && Math.abs(previous.getX() - x) < 0.5D
                    && Math.abs(previous.getY() - y) < 0.5D
                    && Math.abs(previous.getZ() - z) < 0.5D) {
                return;
            }

            SET_LOCATION_METHOD.invoke(wither, x, y, z, 0.0F, 0.0F);

            int teleX = (int) Math.floor(x * 32.0D);
            int teleY = (int) Math.floor(y * 32.0D);
            int teleZ = (int) Math.floor(z * 32.0D);
            Object teleportPacket = ENTITY_TELEPORT_CONSTRUCTOR.newInstance(
                    entityId, teleX, teleY, teleZ,
                    (byte) 0, (byte) 0, true);
            sendPacket(player, teleportPacket);

            positionCache.put(entityId, new Location(x, y, z, 0.0F, 0.0F));
            if (debugPackets) {
                LOGGER.fine("BossBar player=" + player.getName()
                        + " entity=" + entityId
                        + " packet=ENTITY_TELEPORT"
                        + " x=" + x + " y=" + y + " z=" + z);
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE,
                    "BossBar teleport fallito per " + player.getName()
                            + " entity=" + entityId, exception);
            destroy(player, entityId);
        }
    }

    @Override
    public Location calculatePosition(Player player) {
        org.bukkit.Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        if (direction.lengthSquared() == 0.0D) {
            direction = new Vector(0.0D, 0.0D, 1.0D);
        } else {
            direction.normalize();
        }
        double x = eye.getX() + direction.getX() * distance;
        double y = eye.getY() + direction.getY() * distance + verticalOffset;
        double z = eye.getZ() + direction.getZ() * distance;
        return new Location(x, Math.max(1.0D, y), z, 0.0F, 0.0F);
    }

    @Override
    public void destroy(Player player, int entityId) {
        tryDestroy(player, entityId);
        cleanup(entityId);
    }

    private void destroyExisting(Player player, int entityId) {
        if (withers.containsKey(entityId)) {
            tryDestroy(player, entityId);
            cleanup(entityId);
        }
    }

    private void tryDestroy(Player player, int entityId) {
        if (entityId < 0) {
            return;
        }
        try {
            Object destroyPacket = ENTITY_DESTROY_CONSTRUCTOR.newInstance(
                    (Object) new int[]{entityId});
            sendPacket(player, destroyPacket);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING,
                    "BossBar destroy fallito per " + player.getName()
                            + " entity=" + entityId, exception);
        }
    }

    private void cleanup(int entityId) {
        withers.remove(entityId);
        positionCache.remove(entityId);
    }

    private void sendPacket(Player player, Object packet) throws Exception {
        Object entityPlayer = GET_HANDLE_METHOD.invoke(player);
        Object connection = PLAYER_CONNECTION_FIELD.get(entityPlayer);
        SEND_PACKET_METHOD.invoke(connection, packet);
    }

    private float healthFromProgress(double progress) {
        if (Double.isNaN(progress) || Double.isInfinite(progress)) {
            progress = 1.0D;
        }
        float health = (float) (progress * WITHER_MAX_HEALTH);
        return Math.max(WITHER_MIN_HEALTH,
                Math.min(WITHER_MAX_HEALTH, health));
    }

    private boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
