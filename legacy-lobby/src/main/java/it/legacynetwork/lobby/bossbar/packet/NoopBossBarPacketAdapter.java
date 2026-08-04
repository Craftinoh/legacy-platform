package it.legacynetwork.lobby.bossbar.packet;

import com.github.retrooper.packetevents.protocol.world.Location;
import org.bukkit.entity.Player;

public final class NoopBossBarPacketAdapter implements BossBarPacketAdapter {

    @Override
    public int spawnWither(Player player, int entityId, String text, double progress) {
        return entityId;
    }

    @Override
    public void updateTextAndProgress(Player player, int entityId, String text, double progress) {
    }

    @Override
    public void updatePosition(Player player, int entityId, double x, double y, double z) {
    }

    @Override
    public Location calculatePosition(Player player) {
        return new Location(0, 0, 0, 0f, 0f);
    }

    @Override
    public void destroy(Player player, int entityId) {
    }
}
