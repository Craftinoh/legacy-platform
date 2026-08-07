package it.legacynetwork.chickenwars.economy;

import it.legacynetwork.chickenwars.model.ResourceType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * Operazioni sulle valute di partita presenti nell'inventario di un giocatore.
 *
 * <p>Le rimozioni sono atomiche: la quantita' richiesta viene verificata prima
 * di togliere qualsiasi cosa, quindi un pagamento non puo' mai lasciare il
 * giocatore parzialmente addebitato.</p>
 */
public final class ResourceWallet {

    private ResourceWallet() {
    }

    /**
     * Conta quante unita' di una valuta possiede il giocatore.
     */
    public static int count(Player player, ResourceType currency) {
        if (player == null || currency == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == currency.getMaterial()) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    /**
     * Fotografa tutte le valute possedute dal giocatore.
     *
     * @return una mappa completa, con zero per le valute assenti
     */
    public static Map<ResourceType, Integer> snapshot(Player player) {
        Map<ResourceType, Integer> totals =
                new EnumMap<ResourceType, Integer>(ResourceType.class);
        for (ResourceType type : ResourceType.values()) {
            totals.put(type, Integer.valueOf(count(player, type)));
        }
        return totals;
    }

    /**
     * Rimuove una quantita' di valuta solo se interamente disponibile.
     *
     * @return {@code true} se l'addebito e' avvenuto per intero
     */
    public static boolean withdraw(Player player, ResourceType currency,
                                   int amount) {
        if (player == null || currency == null || amount < 0) {
            return false;
        }
        if (amount == 0) {
            return true;
        }
        if (count(player, currency) < amount) {
            return false;
        }
        removeExact(player, currency, amount);
        return true;
    }

    /**
     * Rimuove tutte le valute di partita restituendo quanto e' stato tolto.
     *
     * <p>Operazione unica: viene prima calcolato l'intero contenuto e poi
     * rimosso, cosi' nessuna valuta resta a meta'.</p>
     *
     * @return le quantita' effettivamente rimosse
     */
    public static Map<ResourceType, Integer> withdrawAll(Player player) {
        Map<ResourceType, Integer> removed =
                new EnumMap<ResourceType, Integer>(ResourceType.class);
        if (player == null) {
            return removed;
        }
        Map<ResourceType, Integer> totals = snapshot(player);
        for (Map.Entry<ResourceType, Integer> entry : totals.entrySet()) {
            int amount = entry.getValue().intValue();
            if (amount <= 0) {
                continue;
            }
            removeExact(player, entry.getKey(), amount);
            removed.put(entry.getKey(), Integer.valueOf(amount));
        }
        return removed;
    }

    /**
     * Consegna una valuta, restituendo la quantita' non entrata in inventario.
     *
     * @return l'eccedenza rimasta senza spazio
     */
    public static int deposit(Player player, ResourceType currency, int amount) {
        if (player == null || currency == null || amount <= 0) {
            return Math.max(0, amount);
        }
        Inventory inventory = player.getInventory();
        Map<Integer, ItemStack> leftovers = inventory.addItem(
                new ItemStack(currency.getMaterial(), amount));

        int remaining = 0;
        for (ItemStack leftover : leftovers.values()) {
            if (leftover != null) {
                remaining += leftover.getAmount();
            }
        }
        player.updateInventory();
        return remaining;
    }

    /**
     * Verifica se l'inventario ha spazio per almeno uno slot libero.
     */
    public static boolean hasFreeSlot(Player player) {
        return player != null && player.getInventory().firstEmpty() != -1;
    }

    /**
     * Espone l'inventario di un giocatore come {@link ResourceInventory}.
     *
     * <p>E' il solo punto in cui la logica deterministica incontra Bukkit:
     * produzione e test percorrono da qui in poi lo stesso codice.</p>
     */
    public static ResourceInventory adapterFor(Player player) {
        return new PlayerInventoryAdapter(player);
    }

    static final class PlayerInventoryAdapter implements ResourceInventory {
        private final Player player;
        PlayerInventoryAdapter(Player player) { this.player = player; }
        @Override public int count(ResourceType type) { return ResourceWallet.count(player, type); }
        @Override public Map<ResourceType, Integer> withdrawAll() { return ResourceWallet.withdrawAll(player); }
        @Override public int deposit(ResourceType type, int amount) { return ResourceWallet.deposit(player, type, amount); }
        @Override public boolean hasFreeSlot() { return ResourceWallet.hasFreeSlot(player); }
    }

    private static void removeExact(Player player, ResourceType currency,
                                    int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != currency.getMaterial()) {
                continue;
            }
            int taken = Math.min(stack.getAmount(), remaining);
            remaining -= taken;
            if (stack.getAmount() - taken <= 0) {
                player.getInventory().setItem(i, null);
            } else {
                stack.setAmount(stack.getAmount() - taken);
                player.getInventory().setItem(i, stack);
            }
        }
        player.updateInventory();
    }
}
