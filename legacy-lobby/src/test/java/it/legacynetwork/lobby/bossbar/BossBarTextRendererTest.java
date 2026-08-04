package it.legacynetwork.lobby.bossbar;

import it.legacynetwork.lobby.placeholder.NoopPlaceholderService;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossBarTextRendererTest {

    private BossBarConfiguration config() {
        return new BossBarConfiguration(true, 5, false,
                BossBarRotationMode.SEQUENTIAL, 100, false, false, false,
                35.0, 0.0, 20,
                new LinkedHashMap<String, BossBarDefinition>());
    }

    @Test
    void staticProgress() {
        BossBarTextRenderer renderer = new BossBarTextRenderer(
                config(), new NoopPlaceholderService(), "lobby-01");
        BossBarProgress progress = new BossBarProgress(
                BossBarProgressType.STATIC, 0, 0, 0, "", "", 0.75, 1.0);
        assertEquals(0.75, renderer.calculateProgress(null, progress, 0), 0.001);
    }

    @Test
    void countdownProgress() {
        BossBarTextRenderer renderer = new BossBarTextRenderer(
                config(), new NoopPlaceholderService(), "lobby-01");
        BossBarProgress progress = new BossBarProgress(
                BossBarProgressType.COUNTDOWN, 1.0, 0.0, 100, "", "", 0, 0.5);
        assertEquals(0.5, renderer.calculateProgress(null, progress, 50), 0.01);
    }

    @Test
    void countupProgress() {
        BossBarTextRenderer renderer = new BossBarTextRenderer(
                config(), new NoopPlaceholderService(), "lobby-01");
        BossBarProgress progress = new BossBarProgress(
                BossBarProgressType.COUNTUP, 0.0, 1.0, 100, "", "", 0, 0.5);
        assertEquals(0.5, renderer.calculateProgress(null, progress, 50), 0.01);
    }

    @Test
    void countdownAtEndReturnsEndValue() {
        BossBarTextRenderer renderer = new BossBarTextRenderer(
                config(), new NoopPlaceholderService(), "lobby-01");
        BossBarProgress progress = new BossBarProgress(
                BossBarProgressType.COUNTDOWN, 1.0, 0.0, 100, "", "", 0, 0.5);
        assertEquals(0.0, renderer.calculateProgress(null, progress, 200), 0.01);
    }

    @Test
    void countupAtEndReturnsEndValue() {
        BossBarTextRenderer renderer = new BossBarTextRenderer(
                config(), new NoopPlaceholderService(), "lobby-01");
        BossBarProgress progress = new BossBarProgress(
                BossBarProgressType.COUNTUP, 0.0, 1.0, 100, "", "", 0, 0.5);
        assertEquals(1.0, renderer.calculateProgress(null, progress, 200), 0.01);
    }

    @Test
    void placeholderRatioWithNonNumericFallsBack() {
        BossBarTextRenderer renderer = new BossBarTextRenderer(
                config(), new NoopPlaceholderService(), "lobby-01");
        BossBarProgress progress = new BossBarProgress(
                BossBarProgressType.PLACEHOLDER_RATIO, 0, 0, 0,
                "not_a_number", "also_not", 0, 0.8);
        assertEquals(0.8, renderer.calculateProgress(null, progress, 0), 0.01);
    }

    @Test
    void placeholderRatioWithZeroMaxFallsBack() {
        BossBarTextRenderer renderer = new BossBarTextRenderer(
                config(), new NoopPlaceholderService(), "lobby-01");
        BossBarProgress progress = new BossBarProgress(
                BossBarProgressType.PLACEHOLDER_RATIO, 0, 0, 0,
                "10", "0", 0, 0.9);
        assertEquals(0.9, renderer.calculateProgress(null, progress, 0), 0.01);
    }

    @Test
    void placeholderRatioCalculatesCorrectly() {
        BossBarTextRenderer renderer = new BossBarTextRenderer(
                config(), new NoopPlaceholderService(), "lobby-01");
        BossBarProgress progress = new BossBarProgress(
                BossBarProgressType.PLACEHOLDER_RATIO, 0, 0, 0,
                "25", "100", 0, 0.5);
        assertEquals(0.25, renderer.calculateProgress(null, progress, 0), 0.01);
    }

    @Test
    void progressClampedBetweenZeroAndOne() {
        BossBarTextRenderer renderer = new BossBarTextRenderer(
                config(), new NoopPlaceholderService(), "lobby-01");
        BossBarProgress progress = new BossBarProgress(
                BossBarProgressType.STATIC, 0, 0, 0, "", "", 2.5, 1.0);
        assertEquals(1.0, renderer.calculateProgress(null, progress, 0), 0.01);
    }

    @Test
    void resolvesItalianText() {
        LinkedHashMap<String, BossBarDefinition> bars = new LinkedHashMap<>();
        LinkedHashMap<String, String> langTexts = new LinkedHashMap<>();
        langTexts.put("it", "&aGiocatori: &f{online}");
        langTexts.put("en", "&aPlayers: &f{online}");
        bars.put("online", new BossBarDefinition("online", true, 10, 100, langTexts,
                new BossBarProgress(BossBarProgressType.STATIC, 0, 0, 0, "", "", 1.0, 1.0)));
        BossBarConfiguration config = new BossBarConfiguration(
                true, 5, false, BossBarRotationMode.SEQUENTIAL, 100, false, false,
                false, 35.0, 0.0, 20, bars);
        BossBarTextRenderer renderer = new BossBarTextRenderer(
                config, new NoopPlaceholderService(), "lobby-01");
        String text = renderer.renderText(null, bars.get("online"), "it");
        assertTrue(text.contains("Giocatori"));
    }

    @Test
    void fallsBackToEnglish() {
        LinkedHashMap<String, BossBarDefinition> bars = new LinkedHashMap<>();
        LinkedHashMap<String, String> langTexts = new LinkedHashMap<>();
        langTexts.put("en", "&aPlayers: &f{online}");
        bars.put("online", new BossBarDefinition("online", true, 10, 100, langTexts,
                new BossBarProgress(BossBarProgressType.STATIC, 0, 0, 0, "", "", 1.0, 1.0)));
        BossBarConfiguration config = new BossBarConfiguration(
                true, 5, false, BossBarRotationMode.SEQUENTIAL, 100, false, false,
                false, 35.0, 0.0, 20, bars);
        BossBarTextRenderer renderer = new BossBarTextRenderer(
                config, new NoopPlaceholderService(), "lobby-01");
        String text = renderer.renderText(null, bars.get("online"), "it");
        assertTrue(text.contains("Players"));
    }
}
