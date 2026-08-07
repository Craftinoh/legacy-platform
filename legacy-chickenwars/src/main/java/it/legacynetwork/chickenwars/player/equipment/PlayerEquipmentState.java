package it.legacynetwork.chickenwars.player.equipment;

/**
 * Stato autorevole dell'equipaggiamento acquistato durante una partita.
 *
 * <p>Non viene ricostruito dall'inventario: shop, respawn e reconnect devono
 * consultare questa classe per evitare duplicazioni e perdite di tier.</p>
 */
public final class PlayerEquipmentState {

    private ArmorTier armorTier = ArmorTier.LEATHER;
    private boolean ownsShears;
    private ToolTier pickaxeTier = ToolTier.NONE;
    private ToolTier axeTier = ToolTier.NONE;
    private SwordTier swordTier = SwordTier.WOOD;
    private String selectedQuickBuyPreset = "default";
    private long lastProcessedDeathSequence = Long.MIN_VALUE;

    public synchronized boolean upgradeArmor(ArmorTier requested) {
        if (requested == null || !requested.isHigherThan(armorTier)) {
            return false;
        }
        armorTier = requested;
        return true;
    }

    public synchronized boolean unlockShears() {
        if (ownsShears) {
            return false;
        }
        ownsShears = true;
        return true;
    }

    public synchronized boolean upgradePickaxe(ToolTier requested) {
        if (requested == null || requested == ToolTier.NONE
                || !requested.isHigherThan(pickaxeTier)) {
            return false;
        }
        pickaxeTier = requested;
        return true;
    }

    public synchronized boolean upgradeAxe(ToolTier requested) {
        if (requested == null || requested == ToolTier.NONE
                || !requested.isHigherThan(axeTier)) {
            return false;
        }
        axeTier = requested;
        return true;
    }

    public synchronized boolean upgradeSword(SwordTier requested) {
        if (requested == null || !requested.isHigherThan(swordTier)) {
            return false;
        }
        swordTier = requested;
        return true;
    }

    /**
     * Applica esattamente una volta le conseguenze permanenti di una morte.
     *
     * @param deathSequence contatore crescente della morte nella sessione
     * @return true se la morte e' stata applicata, false se era gia' processata
     */
    public synchronized boolean applyDeath(long deathSequence) {
        if (deathSequence <= lastProcessedDeathSequence) {
            return false;
        }
        lastProcessedDeathSequence = deathSequence;
        pickaxeTier = pickaxeTier.downgradeAfterDeath();
        axeTier = axeTier.downgradeAfterDeath();
        swordTier = SwordTier.WOOD;
        return true;
    }

    /**
     * Prepara un reconnect senza infliggere un secondo downgrade agli strumenti.
     */
    public synchronized void prepareReconnect() {
        swordTier = SwordTier.WOOD;
    }

    public synchronized void selectQuickBuyPreset(String presetId) {
        if (presetId == null || presetId.trim().isEmpty()) {
            selectedQuickBuyPreset = "default";
            return;
        }
        selectedQuickBuyPreset = presetId.trim();
    }

    public synchronized ArmorTier getArmorTier() {
        return armorTier;
    }

    public synchronized boolean ownsShears() {
        return ownsShears;
    }

    public synchronized ToolTier getPickaxeTier() {
        return pickaxeTier;
    }

    public synchronized ToolTier getAxeTier() {
        return axeTier;
    }

    public synchronized SwordTier getSwordTier() {
        return swordTier;
    }

    public synchronized String getSelectedQuickBuyPreset() {
        return selectedQuickBuyPreset;
    }

    public synchronized long getLastProcessedDeathSequence() {
        return lastProcessedDeathSequence;
    }
}
