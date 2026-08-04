package it.legacynetwork.language.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlaceholderValues;
import it.legacynetwork.language.TranslationService;
import it.legacynetwork.language.velocity.service.LanguageSynchronizationService;
import it.legacynetwork.language.velocity.service.ProxyLanguageService;
import it.legacynetwork.language.velocity.tablist.VelocityTabListService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class LanguageCommand implements SimpleCommand {
    private static final List<String> SUGGESTIONS =
            Arrays.asList("it", "en", "auto", "italiano", "inglese", "italian", "english");

    private final ProxyLanguageService languageService;
    private final LanguageSynchronizationService synchronizationService;
    private final TranslationService translations;
    private final LegacyComponentSerializer serializer;
    private final Logger logger;
    private final VelocityTabListService tabListService;

    public LanguageCommand(ProxyLanguageService languageService,
                           LanguageSynchronizationService synchronizationService,
                           TranslationService translations,
                           Logger logger,
                           VelocityTabListService tabListService) {
        this.languageService = languageService;
        this.synchronizationService = synchronizationService;
        this.translations = translations;
        this.logger = logger;
        this.tabListService = tabListService;
        this.serializer = LegacyComponentSerializer.legacyAmpersand();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("This command is only available to players."));
            return;
        }
        String[] arguments = invocation.arguments();
        if (arguments.length == 0) {
            showStatus(player);
            return;
        }
        if ("auto".equalsIgnoreCase(arguments[0])) {
            languageService.setAutomatic(player).whenComplete((state, throwable) -> {
                if (throwable != null) {
                    logger.error("Impossibile salvare la lingua di {}",
                            player.getUsername(), throwable);
                    return;
                }
                send(player, state.language(), "command.lang.automatic",
                        PlaceholderValues.empty());
                synchronizationService.synchronize(player);
                if (tabListService != null) {
                    tabListService.sendImmediately(player, true);
                }
            });
            return;
        }
        Optional<Language> language = Language.findByInput(arguments[0]);
        if (!language.isPresent()) {
            send(player, languageService.current(player).language(), "command.lang.invalid",
                    PlaceholderValues.empty());
            return;
        }

        Language selected = language.get();
        languageService.setManual(player, selected).whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                logger.error("Impossibile salvare la lingua di {}",
                        player.getUsername(), throwable);
            }
        });
        send(player, selected, "command.lang.changed",
                PlaceholderValues.builder()
                        .language(selected.getDisplayName())
                        .build());
        synchronizationService.synchronize(player);
        if (tabListService != null) {
            tabListService.sendImmediately(player, true);
        }
    }

    private void showStatus(Player player) {
        Language language = languageService.current(player).language();
        PlaceholderValues placeholders = PlaceholderValues.builder()
                .language(language.getDisplayName())
                .build();
        send(player, language, "command.lang.current", placeholders);
        send(player, language, "command.lang.available", placeholders);
        send(player, language, "command.lang.usage", placeholders);
    }

    private void send(Player player,
                      Language language,
                      String key,
                      PlaceholderValues placeholders) {
        player.sendMessage(serializer.deserialize(
                translations.translate(language, key, placeholders)));
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] arguments = invocation.arguments();
        String prefix = arguments.length == 0 ? "" :
                arguments[arguments.length - 1].toLowerCase();
        return CompletableFuture.completedFuture(SUGGESTIONS.stream()
                .filter(suggestion -> suggestion.startsWith(prefix))
                .toList());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }
}
