package it.legacynetwork.lobby.scoreboard;

import it.legacynetwork.language.Language;
import it.legacynetwork.lobby.config.ScoreboardConfiguration;
import it.legacynetwork.lobby.placeholder.PlaceholderService;
import it.legacynetwork.lobby.util.LegacyColorTranslator;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LobbyScoreboardRenderer {
    private static final String[] ENTRIES = {
            "\u00A70", "\u00A71", "\u00A72", "\u00A73", "\u00A74", "\u00A75",
            "\u00A76", "\u00A77", "\u00A78", "\u00A79", "\u00A7a", "\u00A7b",
            "\u00A7c", "\u00A7d", "\u00A7e"
    };

    private final ScoreboardConfiguration config;
    private final PlaceholderService placeholderService;
    private final String serverId;
    private final String website;

    public LobbyScoreboardRenderer(ScoreboardConfiguration config,
                                   PlaceholderService placeholderService,
                                   String serverId,
                                   String website) {
        this.config = config;
        this.placeholderService = placeholderService;
        this.serverId = serverId;
        this.website = website;
    }

    public String title(Player player, Language language) {
        ScoreboardConfiguration.LanguageSection langSection =
                config.getLanguage(language.getCode());
        if (langSection == null) {
            return "&6&lAPTERIS";
        }
        return renderText(player, langSection.getTitle());
    }

    public List<ScoreboardLine> render(Player player, Language language, int onlinePlayers) {
        ScoreboardConfiguration.LanguageSection langSection =
                config.getLanguage(language.getCode());
        if (langSection == null) {
            return Collections.emptyList();
        }
        List<String> rawLines = langSection.getLines();
        List<ScoreboardLine> lines = new ArrayList<>();
        for (int i = 0; i < rawLines.size() && i < ENTRIES.length; i++) {
            String raw = rawLines.get(i);
            if (raw == null || raw.isEmpty()) {
                raw = " ";
            }
            String resolved = resolvePlaceholders(player, raw, onlinePlayers);
            lines.add(split(ENTRIES[i], resolved));
        }
        return Collections.unmodifiableList(lines);
    }

    private String resolvePlaceholders(Player player, String text, int online) {
        String result = text;
        String playerName = player != null ? player.getName() : "???";
        String locale = player != null ? player.spigot().getLocale() : "en";
        result = result.replace("{player}", playerName);
        result = result.replace("{server}", serverId);
        result = result.replace("{online}", String.valueOf(online));
        result = result.replace("{language}", locale);
        result = result.replace("{language_code}", locale);
        result = result.replace("{website}", website);

        if (placeholderService.isAvailable() && config.isPlaceholderApiEnabled()
                && player != null) {
            result = placeholderService.apply(player, result);
        }
        return LegacyColorTranslator.translate(result);
    }

    private String renderText(Player player, String raw) {
        String result = raw;
        String playerName = player != null ? player.getName() : "???";
        String locale = player != null ? player.spigot().getLocale() : "en";
        result = result.replace("{player}", playerName);
        result = result.replace("{server}", serverId);
        result = result.replace("{online}", String.valueOf(0));
        result = result.replace("{language}", locale);
        result = result.replace("{language_code}", locale);
        result = result.replace("{website}", website);
        if (placeholderService.isAvailable() && config.isPlaceholderApiEnabled()
                && player != null) {
            result = placeholderService.apply(player, result);
        }
        return LegacyColorTranslator.translate(result);
    }

    ScoreboardLine split(String entry, String text) {
        if (text.length() <= 16) {
            return new ScoreboardLine(entry, text, "");
        }
        int splitPoint = 16;
        if (text.charAt(splitPoint - 1) == '\u00A7') {
            splitPoint--;
        }
        String prefix = text.substring(0, splitPoint);
        String remaining = text.substring(splitPoint);
        String activeColors = activeColors(prefix);
        String suffix = activeColors + remaining;
        if (suffix.length() > 16) {
            suffix = suffix.substring(0, 16);
            if (suffix.charAt(suffix.length() - 1) == '\u00A7') {
                suffix = suffix.substring(0, suffix.length() - 1);
            }
        }
        return new ScoreboardLine(entry, prefix, suffix);
    }

    private String activeColors(String text) {
        String color = "";
        String formats = "";
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) != '\u00A7') {
                continue;
            }
            char code = Character.toLowerCase(text.charAt(i + 1));
            if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                color = "\u00A7" + code;
                formats = "";
            } else if (code >= 'k' && code <= 'o') {
                formats += "\u00A7" + code;
            } else if (code == 'r') {
                color = "";
                formats = "";
            }
            i++;
        }
        return color + formats;
    }
}
