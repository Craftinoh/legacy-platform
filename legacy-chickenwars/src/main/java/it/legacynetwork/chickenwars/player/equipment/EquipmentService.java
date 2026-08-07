package it.legacynetwork.chickenwars.player.equipment;

import it.legacynetwork.chickenwars.economy.ResourceWallet;
import it.legacynetwork.chickenwars.model.TeamColor;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.shop.ItemCost;
import it.legacynetwork.chickenwars.shop.PurchaseResult;
import it.legacynetwork.chickenwars.shop.ShopItemDefinition;
import it.legacynetwork.chickenwars.shop.ShopTierKind;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Unico servizio responsabile dell'equipaggiamento di partita.
 *
 * <p>Applica il loadout iniziale, quello dopo respawn e quello dopo reconnect,
 * esegue gli acquisti, sostituisce i tier inferiori e applica i downgrade.
 * Listener e menu delegano qui: le regole non sono distribuite altrove.</p>
 *
 * <p>Tutte le applicazioni del loadout sono idempotenti, perche' rimuovono
 * l'equipaggiamento gestito prima di riconsegnarlo. Chiamarle due volte non
 * produce duplicati.</p>
 */
public final class EquipmentService {

    private final Set<Material> managedSwords = new HashSet<Material>();
    private final Set<Material> managedPickaxes = new HashSet<Material>();
    private final Set<Material> managedAxes = new HashSet<Material>();

    private volatile EquipmentSettings settings;
    private volatile TeamEnchantProvider enchantProvider =
            TeamEnchantProvider.NONE;

    public EquipmentService(EquipmentSettings settings) {
        setSettings(settings);
    }

    /**
     * Sostituisce le impostazioni dopo un reload riuscito.
     */
    public void setSettings(EquipmentSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException(
                    "Impostazioni equipaggiamento mancanti");
        }
        this.settings = settings;
        rebuildManagedMaterials(settings);
    }

    /**
     * Registra la sorgente degli incantesimi di squadra.
     *
     * <p>Punto di innesto per i futuri upgrade Protection e Sharpness: la
     * gestione dell'equipaggiamento non dovra' essere riscritta.</p>
     */
    public void setEnchantProvider(TeamEnchantProvider enchantProvider) {
        this.enchantProvider = enchantProvider == null
                ? TeamEnchantProvider.NONE : enchantProvider;
    }

    public EquipmentSettings getSettings() {
        return settings;
    }

    private void rebuildManagedMaterials(EquipmentSettings source) {
        managedSwords.clear();
        managedPickaxes.clear();
        managedAxes.clear();
        for (SwordTier tier : SwordTier.values()) {
            addIfPresent(managedSwords, source.getSword(tier));
        }
        for (ToolTier tier : ToolTier.values()) {
            addIfPresent(managedPickaxes, source.getPickaxe(tier));
            addIfPresent(managedAxes, source.getAxe(tier));
        }
    }

    private void addIfPresent(Set<Material> target, Material material) {
        if (material != null) {
            target.add(material);
        }
    }

    // ------------------------------------------------------------------
    // Applicazione del loadout
    // ------------------------------------------------------------------

    /**
     * Applica l'intero equipaggiamento derivato dallo stato autorevole.
     *
     * <p>Usato per l'inizio partita, per il respawn e per il reconnect: in tutti
     * e tre i casi il risultato dipende solo dai tier posseduti, mai dagli
     * oggetti trovati nell'inventario.</p>
     *
     * @param player  destinatario
     * @param session sessione con lo stato autorevole
     * @param color   colore squadra per elmo e corazza
     */
    public void applyLoadout(Player player, PlayerSession session,
                             TeamColor color) {
        if (player == null || session == null) {
            return;
        }
        PlayerEquipmentState state = session.getEquipmentState();
        String teamId = session.getTeamId();

        applyArmor(player, state, color, teamId);
        applySword(player, state, teamId);
        applyTools(player, state);
        player.updateInventory();
    }

    /**
     * Riapplica soltanto l'armatura, dopo un acquisto o un upgrade di squadra.
     */
    public void applyArmor(Player player, PlayerEquipmentState state,
                           TeamColor color, String teamId) {
        EquipmentSettings current = settings;
        int protection = enchantProvider.getProtectionLevel(teamId);

        player.getInventory().setHelmet(buildArmorPiece(
                current.getHelmet(), color, true, protection));
        player.getInventory().setChestplate(buildArmorPiece(
                current.getChestplate(), color, true, protection));

        ArmorTier tier = state.getArmorTier();
        boolean colorLower = tier == ArmorTier.LEATHER;
        player.getInventory().setLeggings(buildArmorPiece(
                current.getLeggings(tier), color, colorLower, protection));
        player.getInventory().setBoots(buildArmorPiece(
                current.getBoots(tier), color, colorLower, protection));
    }

    /**
     * Rimuove ogni spada gestita e consegna quella del tier corrente.
     */
    public void applySword(Player player, PlayerEquipmentState state,
                           String teamId) {
        removeManaged(player, managedSwords);
        Material material = settings.getSword(state.getSwordTier());
        if (material == null) {
            return;
        }
        ItemStack sword = new ItemStack(material);
        int sharpness = enchantProvider.getSharpnessLevel(teamId);
        if (sharpness > 0) {
            sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, sharpness);
        }
        player.getInventory().addItem(sword);
    }

    /**
     * Rimuove piccone, ascia e cesoie gestiti e li riconsegna secondo lo stato.
     */
    public void applyTools(Player player, PlayerEquipmentState state) {
        EquipmentSettings current = settings;

        removeManaged(player, managedPickaxes);
        Material pickaxe = current.getPickaxe(state.getPickaxeTier());
        if (pickaxe != null) {
            player.getInventory().addItem(new ItemStack(pickaxe));
        }

        removeManaged(player, managedAxes);
        Material axe = current.getAxe(state.getAxeTier());
        if (axe != null) {
            player.getInventory().addItem(new ItemStack(axe));
        }

        Material shears = current.getShears();
        if (shears != null) {
            removeManaged(player, EnumSet.of(shears));
            if (state.ownsShears()) {
                player.getInventory().addItem(new ItemStack(shears));
            }
        }
    }

    private ItemStack buildArmorPiece(Material material, TeamColor color,
                                      boolean colored, int protection) {
        if (material == null) {
            return null;
        }
        ItemStack piece = new ItemStack(material);
        if (colored && color != null) {
            ItemMeta meta = piece.getItemMeta();
            if (meta instanceof LeatherArmorMeta) {
                ((LeatherArmorMeta) meta).setColor(color.getArmorColor());
                piece.setItemMeta(meta);
            }
        }
        if (protection > 0) {
            piece.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL,
                    protection);
        }
        return piece;
    }

    private void removeManaged(Player player, Set<Material> materials) {
        if (materials.isEmpty()) {
            return;
        }
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack != null && materials.contains(stack.getType())) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    // ------------------------------------------------------------------
    // Morte e reconnect
    // ------------------------------------------------------------------

    /**
     * Applica le conseguenze permanenti di una morte, una sola volta.
     *
     * <p>La sequenza proviene da {@link PlayerSession#beginDeath()}: eventi
     * duplicati riferiti alla stessa morte riottengono lo stesso numero e non
     * producono un secondo downgrade.</p>
     *
     * @return {@code true} se il downgrade e' stato applicato ora
     */
    public boolean handleDeath(PlayerSession session) {
        if (session == null) {
            return false;
        }
        PlayerEquipmentState state = session.getEquipmentState();
        if (!state.applyDeath(session.beginDeath())) {
            return false;
        }
        enforceMinimumToolTiers(state);
        return true;
    }

    /**
     * Prepara un rientro nella stessa partita senza secondo downgrade.
     */
    public void prepareReconnect(PlayerSession session) {
        if (session == null) {
            return;
        }
        session.getEquipmentState().prepareReconnect();
        session.completeDeath();
    }

    /**
     * Riporta i tier al minimo configurato quando il downgrade lo ha superato.
     */
    private void enforceMinimumToolTiers(PlayerEquipmentState state) {
        EquipmentSettings current = settings;
        ToolTier pickaxe = state.getPickaxeTier();
        if (pickaxe != ToolTier.NONE) {
            state.upgradePickaxe(current.clampToMinimum(pickaxe));
        }
        ToolTier axe = state.getAxeTier();
        if (axe != ToolTier.NONE) {
            state.upgradeAxe(current.clampToMinimum(axe));
        }
    }

    // ------------------------------------------------------------------
    // Acquisti
    // ------------------------------------------------------------------

    /**
     * Verifica se un articolo puo' essere acquistato, senza addebitare nulla.
     *
     * @param session sessione con lo stato autorevole
     * @param item    articolo richiesto
     * @return {@link PurchaseResult#SUCCESS} se l'acquisto sarebbe accettato
     */
    public PurchaseResult checkTier(PlayerSession session,
                                    ShopItemDefinition item) {
        if (item == null) {
            return PurchaseResult.UNAVAILABLE;
        }
        PlayerEquipmentState state = session.getEquipmentState();

        switch (item.getTierKind()) {
            case ARMOR:
                return compare(item.getArmorTier().getLevel(),
                        state.getArmorTier().getLevel());
            case SWORD:
                return compare(item.getSwordTier().getLevel(),
                        state.getSwordTier().getLevel());
            case PICKAXE:
                return compare(item.getToolTier().getLevel(),
                        state.getPickaxeTier().getLevel());
            case AXE:
                return compare(item.getToolTier().getLevel(),
                        state.getAxeTier().getLevel());
            case SHEARS:
                return state.ownsShears()
                        ? PurchaseResult.ALREADY_OWNED : PurchaseResult.SUCCESS;
            case CONSUMABLE:
            default:
                return PurchaseResult.SUCCESS;
        }
    }

    private PurchaseResult compare(int requested, int owned) {
        if (requested == owned) {
            return PurchaseResult.ALREADY_OWNED;
        }
        return requested < owned
                ? PurchaseResult.LOWER_TIER : PurchaseResult.SUCCESS;
    }

    /**
     * Esegue un acquisto completo: verifica, addebito e consegna.
     *
     * <p>L'addebito avviene solo dopo che ogni controllo e' passato, e la
     * consegna solo dopo un addebito riuscito: un acquisto non puo' lasciare il
     * giocatore addebitato senza articolo.</p>
     *
     * @param player  acquirente
     * @param session sessione con lo stato autorevole
     * @param color   colore squadra, per gli articoli colorati
     * @param item    articolo richiesto
     * @param cost    prezzo risolto dal profilo economico della partita
     * @return l'esito, mai nullo
     */
    public PurchaseResult purchase(Player player, PlayerSession session,
                                   TeamColor color, ShopItemDefinition item,
                                   ItemCost cost) {
        if (player == null || session == null || item == null || cost == null) {
            return PurchaseResult.UNAVAILABLE;
        }
        if (item.getPermission() != null
                && !player.hasPermission(item.getPermission())) {
            return PurchaseResult.NO_PERMISSION;
        }

        PurchaseResult tierCheck = checkTier(session, item);
        if (!tierCheck.isSuccess()) {
            return tierCheck;
        }

        if (!item.getTierKind().isEquipment()
                && !ResourceWallet.hasFreeSlot(player)) {
            return PurchaseResult.INVENTORY_FULL;
        }

        if (ResourceWallet.count(player, cost.getCurrency()) < cost.getAmount()) {
            return PurchaseResult.NOT_ENOUGH;
        }
        if (!ResourceWallet.withdraw(player, cost.getCurrency(),
                cost.getAmount())) {
            return PurchaseResult.NOT_ENOUGH;
        }

        deliver(player, session, color, item);
        player.updateInventory();
        return PurchaseResult.SUCCESS;
    }

    /**
     * Consegna l'articolo aggiornando lo stato autorevole quando serve.
     */
    private void deliver(Player player, PlayerSession session, TeamColor color,
                         ShopItemDefinition item) {
        PlayerEquipmentState state = session.getEquipmentState();
        String teamId = session.getTeamId();

        switch (item.getTierKind()) {
            case ARMOR:
                state.upgradeArmor(item.getArmorTier());
                applyArmor(player, state, color, teamId);
                return;
            case SWORD:
                state.upgradeSword(item.getSwordTier());
                applySword(player, state, teamId);
                return;
            case PICKAXE:
                state.upgradePickaxe(item.getToolTier());
                applyTools(player, state);
                return;
            case AXE:
                state.upgradeAxe(item.getToolTier());
                applyTools(player, state);
                return;
            case SHEARS:
                state.unlockShears();
                applyTools(player, state);
                return;
            case CONSUMABLE:
            default:
                giveConsumable(player, color, item);
        }
    }

    @SuppressWarnings("deprecation")
    private void giveConsumable(Player player, TeamColor color,
                                ShopItemDefinition item) {
        byte data = item.isTeamColored() && color != null
                ? color.getWoolData() : item.getData();
        ItemStack stack = new ItemStack(item.getMaterial(), item.getAmount(),
                (short) data);

        if (item.isTeamColored() && color != null) {
            ItemMeta meta = stack.getItemMeta();
            if (meta instanceof LeatherArmorMeta) {
                ((LeatherArmorMeta) meta).setColor(color.getArmorColor());
                stack.setItemMeta(meta);
            }
        }
        for (Map.Entry<Enchantment, Integer> entry
                : item.getEnchantments().entrySet()) {
            stack.addUnsafeEnchantment(entry.getKey(),
                    entry.getValue().intValue());
        }

        player.getInventory().addItem(stack);
        for (PotionEffect effect : item.getEffects()) {
            player.addPotionEffect(effect, true);
        }
    }

    /**
     * Indica se un articolo e' gestito dallo stato autorevole.
     */
    public boolean isEquipment(ShopItemDefinition item) {
        return item != null && item.getTierKind() != ShopTierKind.CONSUMABLE;
    }
}
