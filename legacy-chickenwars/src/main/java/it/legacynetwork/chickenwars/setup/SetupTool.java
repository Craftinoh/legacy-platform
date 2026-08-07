package it.legacynetwork.chickenwars.setup;

import org.bukkit.Material;

/**
 * Strumenti della barra rapida dell'editor arene.
 *
 * <p>Ogni strumento occupa uno slot fisso della hotbar ed e' riconosciuto dalla
 * coppia slot piu' materiale, non dal nome visualizzato.</p>
 */
public enum SetupTool {

    /** Apre il menu delle posizioni generali dell'arena. */
    POSITIONS(0, Material.COMPASS, "setup.tool.positions"),
    /** Apre il menu di creazione e selezione squadre. */
    TEAMS(1, Material.WOOL, "setup.tool.teams"),
    /** Imposta lo spawn della squadra selezionata. */
    TEAM_SPAWN(2, Material.BED, "setup.tool.team-spawn"),
    /** Imposta il nido della squadra selezionata. */
    TEAM_NEST(3, Material.HAY_BLOCK, "setup.tool.team-nest"),
    /** Imposta la posizione della Gallina Reale. */
    TEAM_CHICKEN(4, Material.MONSTER_EGG, "setup.tool.team-chicken"),
    /** Imposta la posizione del venditore. */
    TEAM_SHOP(5, Material.EMERALD, "setup.tool.team-shop"),
    /** Apre il menu di creazione generatori. */
    GENERATORS(6, Material.IRON_INGOT, "setup.tool.generators"),
    /** Mostra il riepilogo di validazione. */
    VALIDATE(7, Material.BOOK, "setup.tool.validate"),
    /** Salva l'arena ed esce dall'editor. */
    SAVE_EXIT(8, Material.EMERALD_BLOCK, "setup.tool.save-exit");

    private final int slot;
    private final Material material;
    private final String messageKey;

    SetupTool(int slot, Material material, String messageKey) {
        this.slot = slot;
        this.material = material;
        this.messageKey = messageKey;
    }

    /**
     * Individua lo strumento corrispondente a slot e materiale.
     *
     * @return lo strumento, oppure {@code null} se non corrisponde a nessuno
     */
    public static SetupTool find(int slot, Material material) {
        if (material == null) {
            return null;
        }
        for (SetupTool tool : values()) {
            if (tool.slot == slot && tool.material == material) {
                return tool;
            }
        }
        return null;
    }

    /**
     * Indica se lo strumento agisce sulla squadra attualmente selezionata.
     */
    public boolean requiresTeam() {
        return this == TEAM_SPAWN || this == TEAM_NEST
                || this == TEAM_CHICKEN || this == TEAM_SHOP;
    }

    public int getSlot() {
        return slot;
    }

    public Material getMaterial() {
        return material;
    }

    /** Chiave del nome nei file lingua. */
    public String getNameKey() {
        return messageKey + ".name";
    }

    /** Chiave della descrizione nei file lingua. */
    public String getLoreKey() {
        return messageKey + ".lore";
    }
}
