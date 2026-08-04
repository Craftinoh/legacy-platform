package it.legacynetwork.lobby.listener;

import it.legacynetwork.lobby.config.LobbyConfiguration;
import it.legacynetwork.lobby.language.BackendLanguageService;
import it.legacynetwork.lobby.message.MessageService;
import it.legacynetwork.lobby.bossbar.LegacyBossBarService;
import it.legacynetwork.lobby.scoreboard.LobbyScoreboardService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class LobbyJoinListener implements Listener {
    private final JavaPlugin plugin;
    private final LobbyConfiguration configuration;
    private final BackendLanguageService languageService;
    private final MessageService messageService;
    private final LobbyScoreboardService scoreboardService;
    private final LegacyBossBarService bossBarService;

    public LobbyJoinListener(JavaPlugin plugin,
                             LobbyConfiguration configuration,
                             BackendLanguageService languageService,
                             MessageService messageService,
                             LobbyScoreboardService scoreboardService,
                             LegacyBossBarService bossBarService) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.languageService = languageService;
        this.messageService = messageService;
        this.scoreboardService = scoreboardService;
        this.bossBarService = bossBarService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        scoreboardService.show(player);
        bossBarService.show(player);
        messageService.send(player, "welcome");
        if (configuration.isJoinSlotEnabled()) {
            scheduleSlotSelect(player);
        }
    }

    private void scheduleSlotSelect(Player player) {
        final int targetSlot = configuration.getJoinSlot() - 1;
        final boolean force = configuration.isJoinSlotForce();
        final int initialSlot = player.getInventory().getHeldItemSlot();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (!force && player.getInventory().getHeldItemSlot() != initialSlot) {
                return;
            }
            player.getInventory().setHeldItemSlot(targetSlot);
        }, configuration.getJoinSlotDelayTicks());
    }
}
