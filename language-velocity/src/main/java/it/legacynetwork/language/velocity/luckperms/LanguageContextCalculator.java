package it.legacynetwork.language.velocity.luckperms;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.velocity.service.ProxyLanguageService;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.context.StaticContextCalculator;

import com.velocitypowered.api.proxy.Player;

public final class LanguageContextCalculator implements ContextCalculator<Player> {

    private final ProxyLanguageService languageService;

    public LanguageContextCalculator(ProxyLanguageService languageService) {
        this.languageService = languageService;
    }

    @Override
    public void calculate(Player target, ContextConsumer consumer) {
        ProxyLanguageService.PlayerLanguage state =
                languageService.current(target);
        String langCode = state.language().getCode();
        consumer.accept("lang", langCode);
        consumer.accept("locale", resolveLocale(target, langCode));
    }

    private String resolveLocale(Player player, String langCode) {
        try {
            String raw = player.getPlayerSettings().getLocale().toString()
                    .toLowerCase().replace('-', '_');
            return raw;
        } catch (Exception e) {
            return langCode + "_" + langCode;
        }
    }

    @Override
    public ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
        for (Language lang : Language.values()) {
            builder.add("lang", lang.getCode());
        }
        return builder.build();
    }
}
