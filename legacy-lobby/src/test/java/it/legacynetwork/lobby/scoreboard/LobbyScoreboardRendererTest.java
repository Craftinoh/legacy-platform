package it.legacynetwork.lobby.scoreboard;

import it.legacynetwork.language.Language;
import it.legacynetwork.lobby.config.ScoreboardConfiguration;
import it.legacynetwork.lobby.placeholder.NoopPlaceholderService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyScoreboardRendererTest {
    @Test
    void createsUniqueLegacySafeLines() {
        Map<String, ScoreboardConfiguration.LanguageSection> languages = new LinkedHashMap<>();
        List<String> lines = new ArrayList<>(Arrays.asList(
                "&7&m----------------",
                "&fName: &a%player_name%",
                "&fRank: &r%luckperms_prefix%",
                "",
                "&fLobby: &a{server}",
                "&fOnline: &a{online}",
                "",
                "&eplay.apteris.net",
                "&7&m----------------"
        ));
        languages.put("en", new ScoreboardConfiguration.LanguageSection("&6&lAPTERIS", lines));
        ScoreboardConfiguration config = new ScoreboardConfiguration(
                true, 20, false, languages);

        LobbyScoreboardRenderer renderer = new LobbyScoreboardRenderer(
                config, new NoopPlaceholderService(), "lobby-01", "play.apteris.net");
        List<ScoreboardLine> result = renderer.render(null, Language.ENGLISH, 42);
        Set<String> entries = new HashSet<>();
        for (ScoreboardLine line : result) {
            entries.add(line.getEntry());
            assertTrue(line.getPrefix().length() <= 16,
                    "Prefix too long: " + line.getPrefix().length());
            assertTrue(line.getSuffix().length() <= 16,
                    "Suffix too long: " + line.getSuffix().length());
        }
        assertEquals(result.size(), entries.size(), "Entries must be unique");
        assertEquals(9, result.size());
    }

    @Test
    void fallsBackToEnglishWhenItalianMissing() {
        Map<String, ScoreboardConfiguration.LanguageSection> languages = new LinkedHashMap<>();
        languages.put("en", new ScoreboardConfiguration.LanguageSection(
                "&6&lEN", new ArrayList<>(Arrays.asList("&fLine1", "&fLine2"))));
        ScoreboardConfiguration config = new ScoreboardConfiguration(
                true, 20, false, languages);
        LobbyScoreboardRenderer renderer = new LobbyScoreboardRenderer(
                config, new NoopPlaceholderService(), "lobby-01", "website");
        List<ScoreboardLine> result = renderer.render(null, Language.ITALIAN, 0);
        assertEquals(2, result.size());
    }

    @Test
    void handlesEmptyLines() {
        Map<String, ScoreboardConfiguration.LanguageSection> languages = new LinkedHashMap<>();
        List<String> lines = new ArrayList<>(Arrays.asList("&fLine1", "", "&fLine3"));
        languages.put("en", new ScoreboardConfiguration.LanguageSection("&6Title", lines));
        ScoreboardConfiguration config = new ScoreboardConfiguration(
                true, 20, false, languages);
        LobbyScoreboardRenderer renderer = new LobbyScoreboardRenderer(
                config, new NoopPlaceholderService(), "lobby-01", "web");
        List<ScoreboardLine> result = renderer.render(null, Language.ENGLISH, 0);
        assertEquals(3, result.size());
    }

    @Test
    void limitsTo15Lines() {
        Map<String, ScoreboardConfiguration.LanguageSection> languages = new LinkedHashMap<>();
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            lines.add("&fLine " + i);
        }
        languages.put("en", new ScoreboardConfiguration.LanguageSection("&6Title", lines));
        ScoreboardConfiguration config = new ScoreboardConfiguration(
                true, 20, false, languages);
        LobbyScoreboardRenderer renderer = new LobbyScoreboardRenderer(
                config, new NoopPlaceholderService(), "lobby-01", "web");
        List<ScoreboardLine> result = renderer.render(null, Language.ENGLISH, 0);
        assertTrue(result.size() <= 15);
    }
}
