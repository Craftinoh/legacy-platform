package it.legacynetwork.lobby.listener;

import it.legacynetwork.lobby.bossbar.LegacyBossBarService;
import it.legacynetwork.lobby.config.LobbyConfiguration;
import it.legacynetwork.lobby.language.BackendLanguageService;
import it.legacynetwork.lobby.message.MessageService;
import it.legacynetwork.lobby.scoreboard.LobbyScoreboardService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Supplier;

public final class LobbyJoinListener implements Listener {
    private final JavaPlugin plugin;
    private final Supplier<LobbyConfiguration> configurationSupplier;
    private final BackendLanguageService languageService;
    private final Supplier<MessageService> messageServiceSupplier;
    private final LobbyScoreboardService scoreboardService;
    private final LegacyBossBarService bossBarService;

    public LobbyJoinListener(JavaPlugin plugin,
                             Supplier<LobbyConfiguration> configurationSupplier,
                             BackendLanguageService languageService,
                             Supplier<MessageService> messageServiceSupplier,
                             LobbyScoreboardService scoreboardService,
                             LegacyBossBarService bossBarService) {
        this.plugin = plugin;
        this.configurationSupplier = configurationSupplier;
        this.languageService = languageService;
        this.messageServiceSupplier = messageServiceSupplier;
        this.scoreboardService = scoreboardService;
        this.bossBarService = bossBarService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        scoreboardService.show(player);
        bossBarService.show(player);
        MessageService messageService = messageServiceSupplier.get();
        if (messageService != null) {
            messageService.send(player, "welcome");
        }

        LobbyConfiguration configuration = configurationSupplier.get();
        if (configuration != null && configuration.isJoinSlotEnabled()) {
            scheduleSlotSelect(player, configuration);
        }
    }

    private void scheduleSlotSelect(Player player,
                                    LobbyConfiguration configuration) {
        final int targetSlot = configuration.getJoinSlot() - 1;
        final boolean force = configuration.isJoinSlotForce();
        final int initialSlot = player.getInventory().getHeldItemSlot();
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                if (!force
                        && player.getInventory().getHeldItemSlot() != initialSlot) {
                    return;
                }
                player.getInventory().setHeldItemSlot(targetSlot);
            }
        }, configuration.getJoinSlotDelayTicks());
    }
}
