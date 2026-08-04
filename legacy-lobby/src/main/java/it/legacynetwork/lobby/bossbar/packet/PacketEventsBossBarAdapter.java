package it.legacynetwork.lobby.bossbar.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PacketEventsBossBarAdapter implements BossBarPacketAdapter {
    private static final Logger LOGGER =
            Logger.getLogger(PacketEventsBossBarAdapter.class.getName());
    private static final float WITHER_MAX_HEALTH = 300.0F;
    private static final float WITHER_MIN_VISIBLE_HEALTH = 1.0F;
    private boolean debugPackets;
    private boolean debugLifecycle;
    private double distance = 35.0;
    private double verticalOffset;

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

    @Override
    public int spawnWither(Player player, int entityId, String text, double progress) {
        try {
            Location location = calculatePosition(player);
            float health = healthFromProgress(progress);

            if (debugLifecycle) {
                LOGGER.info("BossBar clientProtocol=47");
                LOGGER.info("BossBar spawnWrapper=WrapperPlayServerSpawnLivingEntity");
                LOGGER.info("BossBar requestedEntityType=WITHER");
                LOGGER.info("BossBar resolvedEntityType=WITHER");
                LOGGER.info("BossBar invisible=true");
                LOGGER.info("BossBar customNameVisible=false");
                LOGGER.info("BossBar healthType=FLOAT healthValue=" + health);
            }

            List<EntityData> metadata = buildInitialMetadata(text, health);
            WrapperPlayServerSpawnLivingEntity spawn =
                    new WrapperPlayServerSpawnLivingEntity(
                            entityId, (UUID) null, EntityTypes.WITHER,
                            location, 0f, null, metadata);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawn);

            if (debugLifecycle) {
                LOGGER.info("BossBar spawnSent=true"
                        + " player=" + player.getName()
                        + " entity=" + entityId);
            }

            if (debugPackets) {
                LOGGER.info("BossBar player=" + player.getName()
                        + " protocol=1.8 entity=" + entityId
                        + " packet=SPAWN_LIVING_ENTITY type=WITHER"
                        + " metadata=[0:BYTE(0x20,invisible),2:STRING,3:BYTE(0,nameHidden),"
                        + "6:FLOAT(" + health + ")]");
            }

            return entityId;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "BossBar spawn fallito per " + player.getName()
                            + " entity=" + entityId + ": " + e.getMessage());
            tryDestroy(player, entityId);
            return -1;
        }
    }

    @Override
    public void updateTextAndProgress(Player player, int entityId,
                                       String text, double progress) {
        try {
            List<EntityData> metadata = new ArrayList<>();
            metadata.add(new EntityData(2, EntityDataTypes.STRING, text));

            float health = healthFromProgress(progress);
            metadata.add(new EntityData(6, EntityDataTypes.FLOAT, health));

            sendMetadataInternal(player, entityId, metadata);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "BossBar update fallito per " + player.getName()
                            + " entity=" + entityId + ": " + e.getMessage());
        }
    }

    @Override
    public void updatePosition(Player player, int entityId,
                                double x, double y, double z) {
        try {
            if (!isFinite(x) || !isFinite(y) || !isFinite(z)) {
                return;
            }
            Location previous = getCachedPosition(entityId);
            if (previous != null
                    && Math.abs(previous.getX() - x) < 0.5
                    && Math.abs(previous.getY() - y) < 0.5
                    && Math.abs(previous.getZ() - z) < 0.5) {
                return;
            }
            Location location = new Location(x, y, z, 0f, 0f);
            WrapperPlayServerEntityTeleport teleport =
                    new WrapperPlayServerEntityTeleport(entityId, location, false);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, teleport);
            setCachedPosition(entityId, location);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "BossBar teleport fallito per " + player.getName()
                            + " entity=" + entityId + ": " + e.getMessage());
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

    private final ConcurrentHashMap<Integer, Location> positionCache =
            new ConcurrentHashMap<>();

    private Location getCachedPosition(int entityId) {
        return positionCache.get(entityId);
    }

    private void setCachedPosition(int entityId, Location loc) {
        positionCache.put(entityId, loc);
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
            WrapperPlayServerDestroyEntities destroy =
                    new WrapperPlayServerDestroyEntities(entityId);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroy);
        } catch (Exception ignored) {
        }
    }

    private void sendBossBarMetadata(Player player, int entityId,
                                      String text, double progress) {
        float health = healthFromProgress(progress);

        List<EntityData> metadata = new ArrayList<>();
        metadata.add(new EntityData(2, EntityDataTypes.STRING, text));
        metadata.add(new EntityData(6, EntityDataTypes.FLOAT, health));

        if (debugLifecycle) {
            LOGGER.info("BossBar healthType=FLOAT healthValue=" + health
                    + " customNameVisible=false");
        }

        sendMetadataInternal(player, entityId, metadata);
    }

    private List<EntityData> buildInitialMetadata(String text, float health) {
        List<EntityData> metadata = new ArrayList<>();
        metadata.add(new EntityData(0, EntityDataTypes.BYTE, (byte) 0x20));
        metadata.add(new EntityData(2, EntityDataTypes.STRING, text));
        metadata.add(new EntityData(3, EntityDataTypes.BYTE, (byte) 0));
        metadata.add(new EntityData(6, EntityDataTypes.FLOAT, health));
        return metadata;
    }

    private void sendMetadataInternal(Player player, int entityId,
                                       List<EntityData> metadata) {
        WrapperPlayServerEntityMetadata meta =
                new WrapperPlayServerEntityMetadata(entityId, metadata);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, meta);
    }

    private float healthFromProgress(double progress) {
        float health = (float) (progress * WITHER_MAX_HEALTH);
        return Math.max(WITHER_MIN_VISIBLE_HEALTH,
                Math.min(WITHER_MAX_HEALTH, health));
    }

    private boolean isFinite(double val) {
        return !Double.isNaN(val) && !Double.isInfinite(val);
    }
}
