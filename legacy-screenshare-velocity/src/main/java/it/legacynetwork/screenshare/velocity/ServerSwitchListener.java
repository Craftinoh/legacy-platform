package it.legacynetwork.screenshare.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import it.legacynetwork.language.Language;
import it.legacynetwork.screenshare.message.ScreenshareLanguageResolver;
import it.legacynetwork.screenshare.message.ScreensharePresenter;
import it.legacynetwork.screenshare.service.ScreenshareService;
import it.legacynetwork.screenshare.session.ActiveSessionRegistry;
import it.legacynetwork.screenshare.session.ServerSwitchPolicy;

import java.util.Optional;

/**
 * Tiene il giocatore sotto controllo sul server del controllo.
 *
 * <p>La decisione arriva dal registro in memoria, non dal database: questo
 * evento va risposto subito e non puo' aspettare una query. Il blocco riguarda
 * solo il cambio server — l'unica cosa che un proxy possa davvero
 * impedire.</p>
 */
public final class ServerSwitchListener {

    private final ServerSwitchPolicy policy;
    private final ActiveSessionRegistry registry;
    private final ScreenshareService service;
    private final ScreensharePresenter presenter;
    private final ScreenshareLanguageResolver languages;

    public ServerSwitchListener(ServerSwitchPolicy policy,
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
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        String requested = event.getOriginalServer().getServerInfo().getName();
        ServerSwitchPolicy.Verdict verdict =
                policy.evaluate(player.getUniqueId(), requested);
        if (verdict.isAllowed()) {
            return;
        }
        event.setResult(ServerPreConnectEvent.ServerResult.denied());
        Language language = languages.resolve(player.getUniqueId());
        verdict.getMessageKey().ifPresent(key -> player.sendMessage(
                AdventureRenderer.render(presenter.line(language, key))));
        Optional<ActiveSessionRegistry.TargetLock> lock =
                registry.lockOf(player.getUniqueId());
        lock.ifPresent(value -> service.recordBlockedSwitch(
                value.getSessionId(), player.getUniqueId(),
                player.getUsername(), requested));
    }
}
