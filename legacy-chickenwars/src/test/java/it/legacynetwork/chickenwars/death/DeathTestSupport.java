package it.legacynetwork.chickenwars.death;

import it.legacynetwork.chickenwars.economy.ResourceInventory;
import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.player.equipment.ArmorTier;
import it.legacynetwork.chickenwars.player.equipment.EquipmentService;
import it.legacynetwork.chickenwars.player.equipment.EquipmentSettings;
import it.legacynetwork.chickenwars.player.equipment.SwordTier;
import it.legacynetwork.chickenwars.player.equipment.ToolTier;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fake e utilita' condivise dai test sulla morte.
 *
 * <p>Sono adapter Java puri: nessun framework di mocking e nessuna dipendenza
 * da un server in esecuzione.</p>
 */
final class DeathTestSupport {

    private DeathTestSupport() {
    }

    static EquipmentService equipmentService() {
        return new EquipmentService(EquipmentSettings.fromSection(null));
    }

    static PlayerSession session(UUID playerId) {
        return new PlayerSession(playerId, "Tester", "arena", null);
    }

    /**
     * Sessione con equipaggiamento acquistato, per osservare i downgrade.
     */
    static PlayerSession equippedSession(UUID playerId) {
        PlayerSession session = session(playerId);
        session.getEquipmentState().upgradeArmor(ArmorTier.DIAMOND);
        session.getEquipmentState().unlockShears();
        session.getEquipmentState().upgradePickaxe(ToolTier.TIER_4);
        session.getEquipmentState().upgradeAxe(ToolTier.TIER_3);
        session.getEquipmentState().upgradeSword(SwordTier.DIAMOND);
        return session;
    }

    static ResourceInventory inventoryOf(ResourceType type, int amount) {
        Map<ResourceType, Integer> contents =
                new HashMap<ResourceType, Integer>();
        contents.put(type, Integer.valueOf(amount));
        return ResourceInventory.with(contents);
    }

    /**
     * Inventario con una quantita' identica di ogni valuta di partita.
     */
    static ResourceInventory allCurrencies(int amount) {
        Map<ResourceType, Integer> contents =
                new HashMap<ResourceType, Integer>();
        for (ResourceType type : ResourceType.values()) {
            contents.put(type, Integer.valueOf(amount));
        }
        return ResourceInventory.with(contents);
    }

    /**
     * Inventario senza spazio: ogni deposito viene rifiutato per intero.
     */
    static ResourceInventory fullInventory() {
        ResourceInventory.MapInventory inventory =
                (ResourceInventory.MapInventory) ResourceInventory.empty();
        inventory.setMaxSlots(0);
        return inventory;
    }

    /**
     * Inventario che conta le operazioni ricevute.
     */
    static final class CountingInventory implements ResourceInventory {

        private final ResourceInventory delegate;
        private int withdrawAllCalls;
        private int depositCalls;

        CountingInventory(ResourceInventory delegate) {
            this.delegate = delegate;
        }

        @Override
        public int count(ResourceType type) {
            return delegate.count(type);
        }

        @Override
        public boolean withdraw(ResourceType type, int amount) {
            return delegate.withdraw(type, amount);
        }

        @Override
        public Map<ResourceType, Integer> withdrawAll() {
            withdrawAllCalls++;
            return delegate.withdrawAll();
        }

        @Override
        public int deposit(ResourceType type, int amount) {
            depositCalls++;
            return delegate.deposit(type, amount);
        }

        @Override
        public boolean hasFreeSlot() {
            return delegate.hasFreeSlot();
        }

        int getWithdrawAllCalls() {
            return withdrawAllCalls;
        }

        int getDepositCalls() {
            return depositCalls;
        }

        Map<ResourceType, Integer> snapshot() {
            Map<ResourceType, Integer> totals =
                    new EnumMap<ResourceType, Integer>(ResourceType.class);
            for (ResourceType type : ResourceType.values()) {
                int amount = delegate.count(type);
                if (amount > 0) {
                    totals.put(type, Integer.valueOf(amount));
                }
            }
            return totals;
        }
    }

    /**
     * Inventario che fallisce durante il prelievo, per verificare il finally.
     */
    static final class ExplodingInventory implements ResourceInventory {

        static final class TransferFailure extends RuntimeException {
            TransferFailure() {
                super("prelievo fallito");
            }
        }

        @Override
        public int count(ResourceType type) {
            return 0;
        }

        @Override
        public boolean withdraw(ResourceType type, int amount) {
            throw new TransferFailure();
        }

        @Override
        public Map<ResourceType, Integer> withdrawAll() {
            throw new TransferFailure();
        }

        @Override
        public int deposit(ResourceType type, int amount) {
            return amount;
        }

        @Override
        public boolean hasFreeSlot() {
            return true;
        }
    }

    /**
     * Verifica di validita' che registra ogni interrogazione.
     */
    static final class RecordingEligibility implements KillerEligibility {

        private final boolean eligible;
        private int calls;

        RecordingEligibility(boolean eligible) {
            this.eligible = eligible;
        }

        @Override
        public boolean isEligible(UUID killerId) {
            calls++;
            return eligible && killerId != null;
        }

        int getCalls() {
            return calls;
        }
    }
}
