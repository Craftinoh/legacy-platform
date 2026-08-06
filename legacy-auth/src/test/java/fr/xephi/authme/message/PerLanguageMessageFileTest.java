package fr.xephi.authme.message;

import com.google.common.io.Files;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.message.updater.MessageUpdater;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests that {@link AbstractMessageFileHandler} can serve messages in a language other than the
 * configured one, and that an incomplete translation falls back per key instead of per file.
 */
public class PerLanguageMessageFileTest {

    private static final String CONFIGURED_LANGUAGE = "test";
    private static final String TRANSLATED_KEY = "login.wrong_password";
    private static final String UNTRANSLATED_KEY = "error.unregistered_user";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File dataFolder;
    private MessagesFileHandler handler;

    @BeforeClass
    public static void setup() {
        TestHelper.setupLogger();
    }

    @Before
    public void createMessageFiles() throws IOException {
        dataFolder = temporaryFolder.newFolder();
        new File(dataFolder, MessagePathHelper.MESSAGES_FOLDER).mkdirs();

        copyResource(MessagePathHelper.createMessageFilePath(CONFIGURED_LANGUAGE));
        // An intentionally incomplete Italian file: it translates one key and omits another.
        writeFile(MessagePathHelper.createMessageFilePath("it"),
            "login:\n  wrong_password: 'Password errata!'\n");

        handler = createHandler();
    }

    @Test
    public void shouldReturnMessageInRequestedLanguage() {
        assertThat(handler.getMessage(TRANSLATED_KEY, "it"), equalTo("Password errata!"));
    }

    @Test
    public void shouldFallBackToConfiguredLanguageForUntranslatedKey() {
        // given - the Italian file has no entry for this key
        assertThat(handler.getMessageIfExists(UNTRANSLATED_KEY, "it"), nullValue());

        // when / then - the configured language is used instead of an error placeholder
        assertThat(handler.getMessage(UNTRANSLATED_KEY, "it"),
            equalTo(handler.getMessage(UNTRANSLATED_KEY)));
    }

    @Test
    public void shouldFallBackWhenLanguageHasNoFileAtAll() {
        assertThat(handler.getMessage(TRANSLATED_KEY, "klingon"),
            equalTo(handler.getMessage(TRANSLATED_KEY)));
        assertThat(handler.getMessageIfExists(TRANSLATED_KEY, "klingon"), nullValue());
    }

    @Test
    public void shouldUseConfiguredLanguageForNullLanguage() {
        assertThat(handler.getMessage(TRANSLATED_KEY, null), equalTo(handler.getMessage(TRANSLATED_KEY)));
        assertThat(handler.getMessage(TRANSLATED_KEY, CONFIGURED_LANGUAGE),
            equalTo(handler.getMessage(TRANSLATED_KEY)));
    }

    @Test
    public void shouldNotCreateAFileForALanguageWithoutTranslation() {
        // given / when
        handler.getMessage(TRANSLATED_KEY, "klingon");

        // then - a missing language must not produce a file full of default-language messages
        assertThat(new File(dataFolder, MessagePathHelper.createMessageFilePath("klingon")).exists(),
            equalTo(false));
    }

    @Test
    public void shouldReportSectionsOfOtherLanguages() {
        assertThat(handler.hasSection("login", "it"), equalTo(true));
        assertThat(handler.hasSection("login", "klingon"), equalTo(true)); // falls back to configured
        assertThat(handler.hasSection("does.not.exist", "it"), equalTo(false));
    }

    @Test
    public void shouldPickUpChangedTranslationsOnReload() throws IOException {
        // given
        assertThat(handler.getMessage(TRANSLATED_KEY, "it"), equalTo("Password errata!"));

        // when
        writeFile(MessagePathHelper.createMessageFilePath("it"),
            "login:\n  wrong_password: 'Password sbagliata!'\n");
        handler.reload();

        // then
        assertThat(handler.getMessage(TRANSLATED_KEY, "it"), equalTo("Password sbagliata!"));
    }

    @Test
    public void shouldPickUpNewlyAddedLanguageFileOnReload() throws IOException {
        // given - "zz" is not one of the languages bundled in the jar, so nothing is known about it
        assertThat(handler.getMessageIfExists(TRANSLATED_KEY, "zz"), nullValue());

        // when - a server owner drops in a translation of their own and reloads
        writeFile(MessagePathHelper.createMessageFilePath("zz"),
            "login:\n  wrong_password: 'Wrong password, zz style!'\n");
        handler.reload();

        // then
        assertThat(handler.getMessage(TRANSLATED_KEY, "zz"), equalTo("Wrong password, zz style!"));
    }

    @Test
    public void shouldUseTranslationBundledInTheJar() {
        // given / when - German is shipped with AuthMe but has no file in the data folder yet
        String message = handler.getMessage(TRANSLATED_KEY, "de");

        // then - the bundled file is extracted and used
        assertThat(message, equalTo(handler.getMessageIfExists(TRANSLATED_KEY, "de")));
        assertThat(new File(dataFolder, MessagePathHelper.createMessageFilePath("de")).exists(),
            equalTo(true));
    }

    private void writeFile(String localPath, String contents) throws IOException {
        File file = new File(dataFolder, localPath);
        if (!file.exists()) {
            FileUtils.create(file);
        }
        Files.write(contents.getBytes(StandardCharsets.UTF_8), file);
    }

    private void copyResource(String localPath) throws IOException {
        File file = new File(dataFolder, localPath);
        FileUtils.create(file);
        Files.copy(TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "message/messages_test.yml"), file);
    }

    private MessagesFileHandler createHandler() {
        Settings settings = mock(Settings.class);
        given(settings.getProperty(PluginSettings.MESSAGES_LANGUAGE)).willReturn(CONFIGURED_LANGUAGE);

        MessagesFileHandler messagesFileHandler = new MessagesFileHandler();
        ReflectionTestUtils.setField(AbstractMessageFileHandler.class, messagesFileHandler, "settings", settings);
        ReflectionTestUtils.setField(AbstractMessageFileHandler.class, messagesFileHandler, "dataFolder", dataFolder);
        ReflectionTestUtils.setField(MessagesFileHandler.class, messagesFileHandler, "messageUpdater",
            mock(MessageUpdater.class));
        ReflectionTestUtils.invokePostConstructMethods(messagesFileHandler);
        return messagesFileHandler;
    }
}
