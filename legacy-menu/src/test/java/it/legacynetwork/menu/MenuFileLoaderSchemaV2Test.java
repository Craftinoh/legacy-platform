package it.legacynetwork.menu;

import it.legacynetwork.menu.loader.MenuFileLoader;
import it.legacynetwork.menu.model.MenuDefinition;
import it.legacynetwork.menu.model.MenuItem;
import it.legacynetwork.menu.model.MenuItemAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuFileLoaderSchemaV2Test {

    @TempDir
    Path temporaryDirectory;

    @Test
    void runtimeLoaderReadsSchemaV2TitlesTranslationsAndActions() throws Exception {
        Path menuFile = temporaryDirectory.resolve("server-selector.yml");
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("menus/server-selector.yml")) {
            assertNotNull(input);
            Files.copy(input, menuFile, StandardCopyOption.REPLACE_EXISTING);
        }

        MenuDefinition menu = MenuFileLoader.load(menuFile.toFile());

        assertEquals("server-selector", menu.getId());
        assertTrue(menu.isEnabled(), "enabled must default to true in schema v2");
        assertEquals(27, menu.getSize());
        assertEquals("&8Server Selector", menu.getTitle().get("en"));
        assertEquals("&8Selettore Server", menu.getTitle().get("it"));
        assertEquals(3, menu.getItems().size());

        MenuItem bedWars = menu.getItems().get(Integer.valueOf(12));
        assertNotNull(bedWars);
        assertEquals("&cBedWars", bedWars.getName().get("en"));
        assertTrue(bedWars.getLore().get("it").get(0).contains("BedWars"));

        List<MenuItemAction> clickActions = bedWars.getActions().get("CLICK");
        assertNotNull(clickActions);
        assertEquals(1, clickActions.size());
        assertEquals("CONNECT_SERVER", clickActions.get(0).getType());
        assertEquals("bedwars-lobby", clickActions.get(0).getValue());

        MenuItem close = menu.getItems().get(Integer.valueOf(23));
        assertNotNull(close);
        assertEquals("CLOSE", close.getActions().get("CLICK").get(0).getType());
    }
}
