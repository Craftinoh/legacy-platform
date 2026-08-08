package it.legacynetwork.screenshare.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.proxy.Player;
import it.legacynetwork.language.Language;
import it.legacynetwork.screenshare.message.ScreenshareLanguageResolver;
import it.legacynetwork.screenshare.message.ScreensharePresenter;
import it.legacynetwork.screenshare.service.ScreenshareService;
import it.legacynetwork.screenshare.session.ActiveSessionRegistry;
import it.legacynetwork.screenshare.session.TargetCommandPolicy;

import java.util.Optional;

/**
 * Limita i comandi del giocatore sotto controllo.
 *
 * <p>Riguarda solo i comandi che passano dal proxy: quelli registrati sul
 * server di destinazione non arrivano qui, e questo plugin non finge di
 * vederli.</p>
 */
public final class TargetCommandListener {

    private final TargetCommandPolicy policy;
    private final ActiveSessionRegistry registry;
    private final ScreenshareService service;
    private final ScreensharePresenter presenter;
    private final ScreenshareLanguageResolver languages;

    public TargetCommandListener(TargetCommandPolicy policy,
                                 ActiveSessionRegistry registry,
                                 ScreenshareService service,
                                 ScreensharePresenter presenter,
                                 ScreenshareLanguageResolver languages) {
        this.policy = policy;
        this.registry = registry;
        this.service = service;
        this.presenter = presenter;
        this.languages = languages;
    }

    @Subscribe
    public void onCommand(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getCommandSource();
        if (policy.isAllowed(player.getUniqueId(), event.getCommand())) {
            return;
        }
        event.setResult(CommandExecuteEvent.CommandResult.denied());
        Language language = languages.resolve(player.getUniqueId());
        player.sendMessage(AdventureRenderer.render(presenter.line(language,
                "screenshare.target.command-blocked")));
        Optional<ActiveSessionRegistry.TargetLock> lock =
                registry.lockOf(player.getUniqueId());
        lock.ifPresent(value -> service.recordBlockedCommand(
                value.getSessionId(), player.getUniqueId(),
                player.getUsername(),
                TargetCommandPolicy.label(event.getCommand())));
    }
}
