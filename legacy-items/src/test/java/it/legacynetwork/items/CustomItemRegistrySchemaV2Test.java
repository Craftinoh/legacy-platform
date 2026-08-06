package it.legacynetwork.items;

import it.legacynetwork.items.definition.CustomItemAction;
import it.legacynetwork.items.definition.CustomItemActionType;
import it.legacynetwork.items.definition.CustomItemClickActions;
import it.legacynetwork.items.definition.CustomItemDefinition;
import it.legacynetwork.items.definition.CustomItemTrigger;
import it.legacynetwork.items.item.CustomItemRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomItemRegistrySchemaV2Test {

    @TempDir
    Path temporaryDirectory;

    @Test
    void runtimeLoaderReadsSchemaV2TranslationsTriggersAndSimpleAction() throws Exception {
        Path itemsFile = temporaryDirectory.resolve("items.yml");
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("items.yml")) {
            assertNotNull(input);
            Files.copy(input, itemsFile, StandardCopyOption.REPLACE_EXISTING);
        }

        CustomItemRegistry registry = CustomItemRegistry.load(itemsFile.toFile());
        CustomItemDefinition selector = registry.get("server-selector");

        assertNotNull(selector);
        assertTrue(selector.isEnabled());
        assertEquals("COMPASS", selector.getMaterial());
        assertEquals(1, selector.getSlot());
        assertTrue(selector.getTriggers().contains(CustomItemTrigger.JOIN));
        assertTrue(selector.getTriggers().contains(CustomItemTrigger.RESPAWN));
        assertEquals(31, selector.getLanguages().size());
        assertFalse(selector.getLanguage("en").getName().isEmpty());
        assertFalse(selector.getLanguage("it").getLore().isEmpty());

        CustomItemClickActions rightClick =
                selector.getActions().get("right_click");
        assertNotNull(rightClick);
        assertTrue(rightClick.isCancelEvent());
        assertEquals(1, rightClick.getExecute().size());

        CustomItemAction action = rightClick.getExecute().get(0);
        assertEquals(CustomItemActionType.OPEN_MENU, action.getType());
        assertEquals("server-selector", action.getValue());
    }
}
