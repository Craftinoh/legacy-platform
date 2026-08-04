package it.legacynetwork.lobby.bossbar.packet;

import com.github.retrooper.packetevents.protocol.world.Location;
import org.bukkit.entity.Player;

public interface BossBarPacketAdapter {

    int spawnWither(Player player, int entityId, String text, double progress);

    void updateTextAndProgress(Player player, int entityId, String text, double progress);

    void updatePosition(Player player, int entityId, double x, double y, double z);

    Location calculatePosition(Player player);

    void destroy(Player player, int entityId);
}
