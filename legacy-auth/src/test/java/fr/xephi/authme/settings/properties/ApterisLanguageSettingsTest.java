package fr.xephi.authme.settings.properties;

import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.migration.PlainMigrationService;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.resource.YamlFileResource;
import fr.xephi.authme.settings.Settings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests that the Apteris language settings are backwards compatible: a config.yml written by
 * upstream AuthMe has none of the new keys and must keep working with safe defaults.
 */
public class ApterisLanguageSettingsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldUseSafeDefaultsForAnOldConfigFile() throws IOException {
        // given - a minimal config.yml as an existing installation would have it
        File configFile = writeConfig(
            "settings:\n"
                + "    messagesLanguage: 'it'\n"
                + "    sessions:\n"
                + "        enabled: true\n");
        Settings settings = createSettings(configFile);

        // when / then - the old key is honoured and the new ones fall back to their defaults
        assertThat(settings.getProperty(PluginSettings.MESSAGES_LANGUAGE), equalTo("it"));
        assertThat(settings.getProperty(ApterisLanguageSettings.ENABLED), equalTo(true));
        assertThat(settings.getProperty(ApterisLanguageSettings.PER_PLAYER_LOCALE), equalTo(true));
        assertThat(settings.getProperty(ApterisLanguageSettings.USE_NETWORK_PROVIDER), equalTo(true));
        assertThat(settings.getProperty(ApterisLanguageSettings.USE_CLIENT_LOCALE), equalTo(true));
        assertThat(settings.getProperty(ApterisLanguageSettings.FALLBACK), equalTo("en"));
    }

    @Test
    public void shouldReadConfiguredValues() throws IOException {
        // given
        File configFile = writeConfig(
            "settings:\n"
                + "    messagesLanguage: 'en'\n"
                + "    perPlayerLocale: false\n"
                + "apteris-language:\n"
                + "    enabled: true\n"
                + "    use-network-provider: false\n"
                + "    use-client-locale: true\n"
                + "    fallback: 'it'\n");
        Settings settings = createSettings(configFile);

        // when / then
        assertThat(settings.getProperty(ApterisLanguageSettings.PER_PLAYER_LOCALE), equalTo(false));
        assertThat(settings.getProperty(ApterisLanguageSettings.USE_NETWORK_PROVIDER), equalTo(false));
        assertThat(settings.getProperty(ApterisLanguageSettings.USE_CLIENT_LOCALE), equalTo(true));
        assertThat(settings.getProperty(ApterisLanguageSettings.FALLBACK), equalTo("it"));
    }

    @Test
    public void shouldNotClashWithExistingPropertyPaths() {
        // given
        ConfigurationData configurationData = AuthMeSettingsRetriever.buildConfigurationData();

        // when / then - the new properties are part of the configuration data exactly once
        assertThat(countOccurrences(configurationData, ApterisLanguageSettings.PER_PLAYER_LOCALE), equalTo(1));
        assertThat(countOccurrences(configurationData, ApterisLanguageSettings.ENABLED), equalTo(1));
        assertThat(countOccurrences(configurationData, ApterisLanguageSettings.FALLBACK), equalTo(1));
        // The upstream key must be untouched
        assertThat(countOccurrences(configurationData, PluginSettings.MESSAGES_LANGUAGE), equalTo(1));
    }

    private static int countOccurrences(ConfigurationData configurationData, Property<?> property) {
        int count = 0;
        for (Property<?> knownProperty : configurationData.getProperties()) {
            if (knownProperty.getPath().equals(property.getPath())) {
                ++count;
            }
        }
        return count;
    }

    private File writeConfig(String contents) throws IOException {
        File configFile = temporaryFolder.newFile("config.yml");
        Files.write(configFile.toPath(), contents.getBytes(StandardCharsets.UTF_8));
        return configFile;
    }

    private Settings createSettings(File configFile) throws IOException {
        return new Settings(temporaryFolder.newFolder(), new YamlFileResource(configFile),
            new PlainMigrationService(), AuthMeSettingsRetriever.buildConfigurationData());
    }
}
