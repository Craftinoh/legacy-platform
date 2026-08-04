package it.legacynetwork.lobby.bossbar;

import it.legacynetwork.lobby.bossbar.packet.BossBarPacketAdapter;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicInteger;

public final class PlayerBossBarSession {
    private static final AtomicInteger ENTITY_ID_GENERATOR = new AtomicInteger(10000);

    private final Player player;
    private final BossBarPacketAdapter adapter;
    private final int entityId;
    private String currentBarId;
    private int tickCount;
    private int displayTicksRemaining;
    private double lastProgress = -1;
    private String lastText = "";
    private boolean spawned;

    public PlayerBossBarSession(Player player, BossBarPacketAdapter adapter) {
        this.player = player;
        this.adapter = adapter;
        this.entityId = ENTITY_ID_GENERATOR.incrementAndGet();
    }

    public Player getPlayer() {
        return player;
    }

    public int getEntityId() {
        return entityId;
    }

    public String getCurrentBarId() {
        return currentBarId;
    }

    public int getTickCount() {
        return tickCount;
    }

    public int getDisplayTicksRemaining() {
        return displayTicksRemaining;
    }

    public boolean isSpawned() {
        return spawned;
    }

    public void setCurrentBarId(String currentBarId) {
        this.currentBarId = currentBarId;
    }

    public void incrementTickCount() {
        tickCount++;
    }

    public void decrementDisplayTicks() {
        if (displayTicksRemaining > 0) {
            displayTicksRemaining--;
        }
    }

    public void setDisplayTicksRemaining(int displayTicksRemaining) {
        this.displayTicksRemaining = displayTicksRemaining;
    }

    public void reset(String barId, int displayTicks) {
        this.currentBarId = barId;
        this.tickCount = 0;
        this.displayTicksRemaining = displayTicks;
        this.lastProgress = -1;
        this.lastText = "";
    }

    public void updateDisplay(String text, double progress) {
        if (!spawned) {
            spawn(text, progress);
        } else if (!text.equals(lastText) || progress != lastProgress) {
            adapter.updateTextAndProgress(player, entityId, text, progress);
        }
        if (spawned) {
            lastText = text;
            lastProgress = progress;
        }
    }

    private void spawn(String text, double progress) {
        int spawnedEntityId = adapter.spawnWither(
                player, entityId, text, progress);
        spawned = spawnedEntityId >= 0;
    }

    public void destroy() {
        if (spawned && player.isOnline()) {
            adapter.destroy(player, entityId);
        }
        spawned = false;
        currentBarId = null;
    }

    public boolean isOnline() {
        return player.isOnline();
    }
}
