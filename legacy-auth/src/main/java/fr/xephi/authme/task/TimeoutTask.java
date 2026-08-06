package fr.xephi.authme.task;

import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import org.bukkit.entity.Player;

/**
 * Kicks a player if he hasn't logged in (scheduled to run after a configured delay).
 * <p>
 * As with {@link MessageTask}, the kick message is translated when the task actually runs and not
 * when it is scheduled, so that a language change made while the player is still in limbo is
 * honoured.
 */
public class TimeoutTask implements Runnable {

    private final Player player;
    private final Messages messages;
    private final PlayerCache playerCache;

    /**
     * Constructor for TimeoutTask.
     *
     * @param player the player to check
     * @param messages the messages service, used to translate at kick time
     * @param playerCache player cache instance
     */
    public TimeoutTask(Player player, Messages messages, PlayerCache playerCache) {
        this.player = player;
        this.messages = messages;
        this.playerCache = playerCache;
    }

    @Override
    public void run() {
        if (!playerCache.isAuthenticated(player.getName())) {
            player.kickPlayer(messages.retrieveSingle(player, MessageKey.LOGIN_TIMEOUT_ERROR));
        }
    }
}
