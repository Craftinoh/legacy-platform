package it.legacynetwork.chickenwars.velocity.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import it.legacynetwork.chickenwars.velocity.message.RejoinLanguageResolver;
import it.legacynetwork.chickenwars.velocity.message.RejoinMessages;
import it.legacynetwork.chickenwars.velocity.rejoin.RejoinCommandPolicy;
import it.legacynetwork.chickenwars.velocity.rejoin.RejoinCoordinator;
import it.legacynetwork.chickenwars.velocity.rejoin.RejoinOutcome;
import it.legacynetwork.chickenwars.velocity.rejoin.RejoinResult;
import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlaceholderValues;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Comando {@code /cw} del proxy, con il solo sottocomando {@code rejoin}.
 *
 * <p>Adapter volutamente sottile: estrae dal mittente i pochi valori primitivi
 * che servono, delega la decisione a {@link RejoinCommandPolicy} e
 * l'orchestrazione a {@link RejoinCoordinator}, poi mostra il messaggio. Non
 * contiene regole proprie.</p>
 */
public final class ChickenWarsCommand implements SimpleCommand {

    private static final String SUBCOMMAND = "rejoin";

    private final RejoinCommandPolicy policy;
    private final RejoinCoordinator coordinator;
    private final RejoinMessages messages;
    private final RejoinLanguageResolver languages;

    public ChickenWarsCommand(RejoinCommandPolicy policy,
                              RejoinCoordinator coordinator,
                              RejoinMessages messages,
                              RejoinLanguageResolver languages) {
        if (policy == null || coordinator == null || messages == null
                || languages == null) {
            throw new IllegalArgumentException("Comando rejoin incompleto");
        }
        this.policy = policy;
        this.coordinator = coordinator;
        this.messages = messages;
        this.languages = languages;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] arguments = invocation.arguments();
        if (arguments.length == 0
                || !SUBCOMMAND.equalsIgnoreCase(arguments[0])) {
            send(invocation, "rejoin.usage");
            return;
        }

        boolean player = invocation.source() instanceof Player;
        boolean permitted = invocation.source()
                .hasPermission(policy.getPermission());

        RejoinOutcome rejected = policy.reject(player, permitted);
        if (rejected != null) {
            send(invocation, rejected.getMessageKey());
            return;
        }

        Player sender = (Player) invocation.source();
        UUID playerId = sender.getUniqueId();
        send(invocation, "rejoin.searching");

        // Nessuna attesa sul thread comandi: l'esito arriva quando il
        // sistema distribuito ha risposto.
        coordinator.rejoin(playerId).whenComplete((result, failure) -> {
            RejoinResult resolved = failure != null || result == null
                    ? RejoinResult.of(RejoinOutcome.TRANSFER_FAILED) : result;
            sender.sendMessage(render(languages.resolve(playerId), resolved));
        });
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        // Il permesso viene valutato dentro execute per poter distinguere il
        // messaggio "solo giocatori" da quello "permesso assente".
        return true;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] arguments = invocation.arguments();
        if (arguments.length == 0) {
            return Collections.singletonList(SUBCOMMAND);
        }
        if (arguments.length == 1 && SUBCOMMAND.startsWith(
                arguments[0].toLowerCase(Locale.ROOT))) {
            return Collections.singletonList(SUBCOMMAND);
        }
        return Collections.emptyList();
    }

    private void send(Invocation invocation, String key) {
        UUID playerId = invocation.source() instanceof Player
                ? ((Player) invocation.source()).getUniqueId() : null;
        invocation.source().sendMessage(LegacyComponentSerializer
                .legacyAmpersand()
                .deserialize(messages.get(languages.resolve(playerId), key)));
    }

    /**
     * Compone il messaggio con i segnaposto dell'esito.
     */
    private Component render(Language language, RejoinResult result) {
        PlaceholderValues placeholders = PlaceholderValues.builder()
                .put("server", result.getServerName())
                .put("arena", result.getArenaId())
                .put("reason", reasonText(language, result))
                .build();
        return LegacyComponentSerializer.legacyAmpersand().deserialize(
                messages.get(language, result.getMessageKey(), placeholders));
    }

    /**
     * Traduce il motivo tecnico in testo leggibile.
     *
     * <p>Il backend invia un identificatore stabile, mai un dettaglio SQL o
     * un'eccezione: qui diventa una frase gia' localizzata.</p>
     */
    private String reasonText(Language language, RejoinResult result) {
        String reason = result.getReason();
        if (reason == null || reason.trim().isEmpty()) {
            return messages.get(language, "rejoin.reason.unknown");
        }
        return messages.get(language, "rejoin.reason." + reason);
    }
}
