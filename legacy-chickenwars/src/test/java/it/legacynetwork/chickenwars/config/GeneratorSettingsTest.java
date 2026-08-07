package it.legacynetwork.chickenwars.config;

import it.legacynetwork.chickenwars.model.ResourceType;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeneratorSettingsTest {

    private YamlConfiguration parse(String yaml) throws InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(yaml);
        return configuration;
    }

    @Test
    void senzaSezioneUsaIValoriPredefiniti() {
        GeneratorSettings settings = GeneratorSettings.fromSection(null);

        assertEquals(32, settings.getMaximumGroundItems());
        assertTrue(settings.isItemStacking());
        assertNotNull(settings.getTier(ResourceType.IRON, 1));
    }

    @Test
    void leggeGliIntervalliConvertendoliInTick() throws Exception {
        YamlConfiguration configuration = parse(
                "generators:\n"
                        + "  maximum-ground-items: 8\n"
                        + "  item-stacking: false\n"
                        + "  merge-radius: 3.5\n"
                        + "  types:\n"
                        + "    IRON:\n"
                        + "      levels:\n"
                        + "        1:\n"
                        + "          interval-seconds: 1.5\n"
                        + "          amount: 2\n"
                        + "        2:\n"
                        + "          interval-seconds: 1.0\n"
                        + "          amount: 3\n");

        GeneratorSettings settings = GeneratorSettings.fromSection(
                configuration.getConfigurationSection("generators"));

        assertEquals(8, settings.getMaximumGroundItems());
        assertEquals(3.5D, settings.getMergeRadius(), 0.0001D);
        assertEquals(30, settings.getTier(ResourceType.IRON, 1).getIntervalTicks());
        assertEquals(2, settings.getTier(ResourceType.IRON, 1).getAmount());
        assertEquals(20, settings.getTier(ResourceType.IRON, 2).getIntervalTicks());
        assertEquals(2, settings.getMaximumLevel(ResourceType.IRON));
    }

    @Test
    void iLivelliOltreIlMassimoRiusanoLUltimoConfigurato() throws Exception {
        YamlConfiguration configuration = parse(
                "generators:\n"
                        + "  types:\n"
                        + "    GOLD:\n"
                        + "      levels:\n"
                        + "        1:\n"
                        + "          interval-seconds: 5.0\n"
                        + "          amount: 1\n");

        GeneratorSettings settings = GeneratorSettings.fromSection(
                configuration.getConfigurationSection("generators"));

        assertEquals(100, settings.getTier(ResourceType.GOLD, 9).getIntervalTicks());
    }

    @Test
    void iLivelliNonValidiRicadonoSulPrimo() {
        GeneratorSettings settings = GeneratorSettings.fromSection(null);

        assertEquals(settings.getTier(ResourceType.DIAMOND, 1).getIntervalTicks(),
                settings.getTier(ResourceType.DIAMOND, 0).getIntervalTicks());
        assertEquals(settings.getTier(ResourceType.DIAMOND, 1).getIntervalTicks(),
                settings.getTier(ResourceType.DIAMOND, -5).getIntervalTicks());
    }

    @Test
    void profiloModalitaModificaIntervalloEQuantita() throws Exception {
        YamlConfiguration configuration = parse("generators:\n"
                + "  profiles:\n"
                + "    trio:\n"
                + "      interval-multiplier: 0.5\n"
                + "      amount-multiplier: 2.0\n");
        GeneratorSettings settings = GeneratorSettings.fromSection(
                configuration.getConfigurationSection("generators"));
        GeneratorTier base = settings.getTier(ResourceType.IRON, 1);
        GeneratorTier trio = settings.getTier(ResourceType.IRON, 1, "trio");

        assertEquals(Math.round(base.getIntervalTicks() * 0.5D),
                trio.getIntervalTicks());
        assertEquals(base.getAmount() * 2, trio.getAmount());
    }

    @Test
    void politicheEProfiliNonValidiSonoRifiutati() throws Exception {
        YamlConfiguration policy = parse("generators:\n"
                + "  catch-up-policy: BURST\n");
        assertThrows(IllegalArgumentException.class, () ->
                GeneratorSettings.fromSection(policy
                        .getConfigurationSection("generators")));

        YamlConfiguration profile = parse("generators:\n"
                + "  profiles:\n"
                + "    trio:\n"
                + "      interval-multiplier: 0\n");
        assertThrows(IllegalArgumentException.class, () ->
                GeneratorSettings.fromSection(profile
                        .getConfigurationSection("generators")));
    }
}
