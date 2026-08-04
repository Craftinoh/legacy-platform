package it.legacynetwork.lobby.scoreboard;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlaceholderValues;
import it.legacynetwork.language.TranslationService;
import it.legacynetwork.lobby.config.LobbyConfiguration;
import it.legacynetwork.lobby.util.LegacyColorTranslator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LobbyScoreboardRenderer {
    private static final String[] ENTRIES = {
            "\u00A70", "\u00A71", "\u00A72", "\u00A73", "\u00A74", "\u00A75"
    };

    private final TranslationService translations;
    private final LobbyConfiguration configuration;

    public LobbyScoreboardRenderer(TranslationService translations,
                                   LobbyConfiguration configuration) {
        this.translations = translations;
        this.configuration = configuration;
    }

    public String title(Language language) {
        return LegacyColorTranslator.translate(
                translations.translate(language, "scoreboard.title"));
    }

    public List<ScoreboardLine> render(Language language, int onlinePlayers) {
        PlaceholderValues placeholders = PlaceholderValues.builder()
                .server(configuration.getServerId())
                .online(onlinePlayers)
                .website(configuration.getWebsite())
                .rank(translations.translate(language, "rank.default"))
                .build();
        List<String> rawLines = new ArrayList<String>();
        rawLines.add("");
        rawLines.add(translations.translate(
                language, "scoreboard.rank", placeholders));
        rawLines.add(translations.translate(
                language, "scoreboard.lobby", placeholders));
        rawLines.add(translations.translate(
                language, "scoreboard.online", placeholders));
        rawLines.add("");
        rawLines.add(translations.translate(
                language, "scoreboard.website", placeholders));

        List<ScoreboardLine> lines = new ArrayList<ScoreboardLine>();
        for (int index = 0; index < rawLines.size(); index++) {
            lines.add(split(ENTRIES[index],
                    LegacyColorTranslator.translate(rawLines.get(index))));
        }
        return Collections.unmodifiableList(lines);
    }

    private ScoreboardLine split(String entry, String text) {
        if (text.length() <= 16) {
            return new ScoreboardLine(entry, text, "");
        }
        int splitIndex = 16;
        if (text.charAt(splitIndex - 1) == '\u00A7') {
            splitIndex--;
        }
        String prefix = text.substring(0, splitIndex);
        String remaining = text.substring(splitIndex);
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
        for (int index = 0; index < text.length() - 1; index++) {
            if (text.charAt(index) != '\u00A7') {
                continue;
            }
            char code = Character.toLowerCase(text.charAt(index + 1));
            if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                color = "\u00A7" + code;
                formats = "";
            } else if (code >= 'k' && code <= 'o') {
                formats += "\u00A7" + code;
            } else if (code == 'r') {
                color = "";
                formats = "";
            }
            index++;
        }
        return color + formats;
    }
}
