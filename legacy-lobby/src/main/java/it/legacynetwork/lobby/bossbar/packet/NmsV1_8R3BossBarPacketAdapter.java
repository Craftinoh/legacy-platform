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
    private static final Constructor<?> DATA_WATCHER_CONSTRUCTOR;

    private static final Method SET_LOCATION_METHOD;
    private static final Method SET_INVISIBLE_METHOD;
    private static final Method SET_CUSTOM_NAME_METHOD;
    private static final Method SET_CUSTOM_NAME_VISIBLE_METHOD;
    private static final Method SET_HEALTH_METHOD;
    private static final Method SET_SILENT_METHOD;
    private static final Method GET_HANDLE_METHOD;
    private static final Method DATA_WATCHER_A_METHOD;
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
                    int.class, int.class, int.class, int.class, byte.class, byte.class, boolean.class);
            ENTITY_DESTROY_CONSTRUCTOR = ENTITY_DESTROY_CLASS.getConstructor(int[].class);
            DATA_WATCHER_CONSTRUCTOR = DATA_WATCHER_CLASS.getConstructor(ENTITY_CLASS);

            SET_LOCATION_METHOD = ENTITY_CLASS.getMethod("setLocation",
                    double.class, double.class, double.class, float.class, float.class);
            SET_INVISIBLE_METHOD = ENTITY_CLASS.getMethod("setInvisible", boolean.class);
            SET_CUSTOM_NAME_METHOD = ENTITY_CLASS.getMethod("setCustomName", String.class);
            SET_CUSTOM_NAME_VISIBLE_METHOD = ENTITY_CLASS.getMethod("setCustomNameVisible", boolean.class);
            SET_HEALTH_METHOD = ENTITY_LIVING_CLASS.getMethod("setHealth", float.class);
            GET_HANDLE_METHOD = CRAFT_PLAYER_CLASS.getMethod("getHandle");
            DATA_WATCHER_A_METHOD = DATA_WATCHER_CLASS.getMethod("a", int.class, Object.class);
            SEND_PACKET_METHOD = PLAYER_CONNECTION_CLASS.getMethod("sendPacket", PACKET_CLASS);
            CRAFT_WORLD_GET_HANDLE_METHOD = CRAFT_WORLD_CLASS.getMethod("getHandle");

            Method silentMethod = null;
            try {
                silentMethod = ENTITY_CLASS.getMethod("setSilent", boolean.class);
            } catch (NoSuchMethodException ignored) {
            }
            SET_SILENT_METHOD = silentMethod;

            PLAYER_CONNECTION_FIELD = ENTITY_PLAYER_CLASS.getField("playerConnection");
            ENTITY_ID_FIELD = ENTITY_CLASS.getDeclaredField("id");
            ENTITY_ID_FIELD.setAccessible(true);

        } catch (Exception e) {
            RuntimeException re = new RuntimeException(
                    "NmsV1_8R3BossBarPacketAdapter init failed for version " + version, e);
            NMS_VERSION = version;
            NMS_PREFIX = nmsPrefix;
            CB_PREFIX = cbPrefix;
            throw re;
        }
        NMS_VERSION = version;
        NMS_PREFIX = nmsPrefix;
        CB_PREFIX = cbPrefix;
    }

    private boolean debugPackets;
    private boolean debugLifecycle;
    private double distance = 35.0;
    private double verticalOffset;
    private final ConcurrentHashMap<Integer, Location> positionCache = new ConcurrentHashMap<>();

    public void setDebugPackets(boolean debug) {
        this.debugPackets = debug;
    }

    public void setDebugLifecycle(boolean debug) {
        this.debugLifecycle = debug;
    }

    public void setDistance(double distance) {
        this.distance = Math.max(1.0, distance);
    }

    public void setVerticalOffset(double verticalOffset) {
        this.verticalOffset = verticalOffset;
    }

    public static boolean validateServerVersion() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            return "v1_8_R3".equals(packageName.split("\\.")[3]);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int spawnWither(Player player, int entityId, String text, double progress) {
        try {
            Location location = calculatePosition(player);
            float health = healthFromProgress(progress);

            if (debugLifecycle) {
                LOGGER.info("BossBar spawnWither player=" + player.getName()
                        + " entity=" + entityId
                        + " location=(" + String.format("%.2f", location.getX())
                        + "," + String.format("%.2f", location.getY())
                        + "," + String.format("%.2f", location.getZ()) + ")"
                        + " health=" + health);
            }

            Object craftWorld = player.getWorld();
            Object nmsWorld = CRAFT_WORLD_GET_HANDLE_METHOD.invoke(craftWorld);

            Object wither = ENTITY_WITHER_CONSTRUCTOR.newInstance(nmsWorld);
            ENTITY_ID_FIELD.setInt(wither, entityId);

            SET_LOCATION_METHOD.invoke(wither, location.getX(), location.getY(),
                    location.getZ(), 0f, 0f);
            SET_INVISIBLE_METHOD.invoke(wither, true);
            SET_CUSTOM_NAME_METHOD.invoke(wither, text);
            SET_CUSTOM_NAME_VISIBLE_METHOD.invoke(wither, false);
            SET_HEALTH_METHOD.invoke(wither, health);
            if (SET_SILENT_METHOD != null) {
                SET_SILENT_METHOD.invoke(wither, true);
            }

            Object spawnPacket = SPAWN_LIVING_CONSTRUCTOR.newInstance(wither);
            sendPacket(player, spawnPacket);

            if (debugLifecycle) {
                LOGGER.info("BossBar spawnSent=true"
                        + " player=" + player.getName()
                        + " entity=" + entityId);
            }

            if (debugPackets) {
                LOGGER.info("BossBar player=" + player.getName()
                        + " entity=" + entityId
                        + " packet=SPAWN_LIVING_ENTITY type=WITHER"
                        + " metadata=[0:BYTE(0x20,invisible),2:STRING(" + text + ")"
                        + ",3:BYTE(0,nameHidden),6:FLOAT(" + health + ")]");
            }

            return entityId;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "BossBar spawn fallito per " + player.getName()
                            + " entity=" + entityId + ": " + e.getMessage(), e);
            tryDestroy(player, entityId);
            return -1;
        }
    }

    @Override
    public void updateTextAndProgress(Player player, int entityId,
                                       String text, double progress) {
        try {
            float health = healthFromProgress(progress);

            Object dataWatcher = DATA_WATCHER_CONSTRUCTOR.newInstance((Object) null);
            DATA_WATCHER_A_METHOD.invoke(dataWatcher, 2, text);
            DATA_WATCHER_A_METHOD.invoke(dataWatcher, 6, health);

            Object metadataPacket = ENTITY_METADATA_CONSTRUCTOR.newInstance(
                    entityId, dataWatcher, true);
            sendPacket(player, metadataPacket);

            if (debugPackets) {
                LOGGER.info("BossBar player=" + player.getName()
                        + " entity=" + entityId
                        + " packet=ENTITY_METADATA"
                        + " metadata=[2:STRING(" + text + "),6:FLOAT(" + health + ")]");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "BossBar update fallito per " + player.getName()
                            + " entity=" + entityId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void updatePosition(Player player, int entityId,
                                double x, double y, double z) {
        try {
            if (!isFinite(x) || !isFinite(y) || !isFinite(z)) {
                return;
            }
            Location previous = positionCache.get(entityId);
            if (previous != null
                    && Math.abs(previous.getX() - x) < 0.5
                    && Math.abs(previous.getY() - y) < 0.5
                    && Math.abs(previous.getZ() - z) < 0.5) {
                return;
            }

            int teleX = (int) Math.floor(x * 32.0D);
            int teleY = (int) Math.floor(y * 32.0D);
            int teleZ = (int) Math.floor(z * 32.0D);
            byte teleYaw = 0;
            byte telePitch = 0;
            boolean onGround = true;

            Object teleportPacket = ENTITY_TELEPORT_CONSTRUCTOR.newInstance(
                    entityId, teleX, teleY, teleZ, teleYaw, telePitch, onGround);
            sendPacket(player, teleportPacket);

            positionCache.put(entityId, new Location(x, y, z, 0f, 0f));

            if (debugPackets) {
                LOGGER.fine("BossBar player=" + player.getName()
                        + " entity=" + entityId
                        + " packet=ENTITY_TELEPORT"
                        + " x=" + String.format("%.2f", x)
                        + " y=" + String.format("%.2f", y)
                        + " z=" + String.format("%.2f", z));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "BossBar teleport fallito per " + player.getName()
                            + " entity=" + entityId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Location calculatePosition(Player player) {
        org.bukkit.Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        double x = eye.getX() + direction.getX() * distance;
        double y = eye.getY() + direction.getY() * distance + verticalOffset;
        double z = eye.getZ() + direction.getZ() * distance;
        return new Location(x, Math.max(1.0, y), z, 0f, 0f);
    }

    @Override
    public void destroy(Player player, int entityId) {
        tryDestroy(player, entityId);
        positionCache.remove(entityId);
    }

    private void tryDestroy(Player player, int entityId) {
        if (entityId < 0) {
            return;
        }
        try {
            int[] entityIds = {entityId};
            Object destroyPacket = ENTITY_DESTROY_CONSTRUCTOR.newInstance((Object) entityIds);
            sendPacket(player, destroyPacket);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "BossBar destroy fallito per " + player.getName()
                            + " entity=" + entityId + ": " + e.getMessage(), e);
        }
    }

    private void sendPacket(Player player, Object packet) {
        try {
            Object entityPlayer = GET_HANDLE_METHOD.invoke(player);
            Object connection = PLAYER_CONNECTION_FIELD.get(entityPlayer);
            SEND_PACKET_METHOD.invoke(connection, packet);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "BossBar sendPacket fallito per " + player.getName()
                            + ": " + e.getMessage(), e);
        }
    }

    private float healthFromProgress(double progress) {
        float health = (float) (progress * WITHER_MAX_HEALTH);
        return Math.max(WITHER_MIN_HEALTH, Math.min(WITHER_MAX_HEALTH, health));
    }

    private boolean isFinite(double val) {
        return !Double.isNaN(val) && !Double.isInfinite(val);
    }
}
