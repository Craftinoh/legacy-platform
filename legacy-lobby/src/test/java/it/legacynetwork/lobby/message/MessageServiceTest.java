package it.legacynetwork.lobby.message;

import it.legacynetwork.lobby.placeholder.NoopPlaceholderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageServiceTest {

    @Test
    void loadsMessagesWithListText(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("messages_en.yml").toFile();
        String yaml = "welcome:\n" +
                "  enabled: true\n" +
                "  text:\n" +
                "    - \"&aWelcome!\"\n" +
                "    - \"&7Use /lang\"\n";
        Files.write(file.toPath(), yaml.getBytes());
        MessageService service = new MessageService(file, new NoopPlaceholderService());
        service.load();
        assertTrue(service.isEnabled("welcome"));
        assertEquals(2, service.getMessages(null, "welcome").size());
    }

    @Test
    void loadsSingleStringText(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("messages_en.yml").toFile();
        String yaml = "welcome:\n" +
                "  enabled: true\n" +
                "  text: \"&aSingle line\"\n";
        Files.write(file.toPath(), yaml.getBytes());
        MessageService service = new MessageService(file, new NoopPlaceholderService());
        service.load();
        assertTrue(service.isEnabled("welcome"));
        assertEquals(1, service.getMessages(null, "welcome").size());
    }

    @Test
    void disabledCategoryReturnsEmpty(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("messages_en.yml").toFile();
        String yaml = "language-change:\n" +
                "  enabled: false\n" +
                "  text:\n" +
                "    - \"&aLanguage updated\"\n";
        Files.write(file.toPath(), yaml.getBytes());
        MessageService service = new MessageService(file, new NoopPlaceholderService());
        service.load();
        assertFalse(service.isEnabled("language-change"));
        assertEquals(0, service.getMessages(null, "language-change").size());
    }

    @Test
    void missingCategoryReturnsEmpty(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("messages_en.yml").toFile();
        Files.write(file.toPath(), "welcome:\n  enabled: true\n  text: \"Hi\"\n".getBytes());
        MessageService service = new MessageService(file, new NoopPlaceholderService());
        service.load();
        assertFalse(service.isEnabled("nonexistent"));
        assertEquals(0, service.getMessages(null, "nonexistent").size());
    }

    @Test
    void missingFileReturnsEmpty() {
        MessageService service = new MessageService(
                new File("/nonexistent/messages.yml"), new NoopPlaceholderService());
        service.load();
        assertFalse(service.isEnabled("welcome"));
        assertEquals(0, service.getMessages(null, "welcome").size());
    }
}
