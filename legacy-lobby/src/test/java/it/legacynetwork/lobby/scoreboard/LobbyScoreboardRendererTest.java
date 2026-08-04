package it.legacynetwork.lobby.scoreboard;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.TranslationBundle;
import it.legacynetwork.language.TranslationService;
import it.legacynetwork.lobby.config.LobbyConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyScoreboardRendererTest {
    @Test
    void createsUniqueLegacySafeLines() {
        Map<String, String> translations = new HashMap<String, String>();
        translations.put("scoreboard.title", "&6&lNETWORK");
        translations.put("scoreboard.rank", "&fRank: &7{rank}");
        translations.put("scoreboard.lobby", "&fLobby: &a{server}");
        translations.put("scoreboard.online", "&fOnline: &a{online}");
        translations.put("scoreboard.website", "&e{website}");
        translations.put("rank.default", "Default");
        Map<Language, TranslationBundle> bundles =
                new EnumMap<Language, TranslationBundle>(Language.class);
        bundles.put(Language.ENGLISH, new TranslationBundle(translations));

        LobbyConfiguration configuration = new LobbyConfiguration(
                "lobby-01", "NetworkLang", true, 60L, "example.net", true);
        LobbyScoreboardRenderer renderer = new LobbyScoreboardRenderer(
                new TranslationService(bundles), configuration);
        List<ScoreboardLine> lines = renderer.render(Language.ENGLISH, 42);
        Set<String> entries = new HashSet<String>();
        for (ScoreboardLine line : lines) {
            entries.add(line.getEntry());
            assertTrue(line.getPrefix().length() <= 16);
            assertTrue(line.getSuffix().length() <= 16);
        }

        assertEquals(lines.size(), entries.size());
        assertEquals(6, lines.size());
        assertEquals(new HashSet<String>(Arrays.asList(
                        "\u00A70", "\u00A71", "\u00A72",
                        "\u00A73", "\u00A74", "\u00A75")),
                entries);
    }
}
