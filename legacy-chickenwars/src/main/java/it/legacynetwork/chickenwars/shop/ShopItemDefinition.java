package it.legacynetwork.chickenwars.shop;

import it.legacynetwork.chickenwars.player.equipment.ArmorTier;
import it.legacynetwork.chickenwars.player.equipment.SwordTier;
import it.legacynetwork.chickenwars.player.equipment.ToolTier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Definizione strutturale di un articolo dello shop.
 *
 * <p>Non contiene prezzi: quelli vivono nei profili economici, perche' lo stesso
 * articolo costa diversamente in {@code solo_duel}, {@code doubles} e
 * {@code trio}.</p>
 */
public final class ShopItemDefinition {

    private final String id;
    private final String categoryId;
    private final Material material;
    private final byte data;
    private final int amount;
    private final boolean teamColored;
    private final boolean repeatable;
    private final int slot;
    private final ShopTierKind tierKind;
    private final ArmorTier armorTier;
    private final SwordTier swordTier;
    private final ToolTier toolTier;
    private final Map<Enchantment, Integer> enchantments;
    private final List<PotionEffect> effects;
    private final String permission;

    ShopItemDefinition(Builder builder) {
        this.id = builder.id;
        this.categoryId = builder.categoryId;
        this.material = builder.material;
        this.data = builder.data;
        this.amount = Math.max(1, builder.amount);
        this.teamColored = builder.teamColored;
        this.repeatable = builder.repeatable;
        this.slot = builder.slot;
        this.tierKind = builder.tierKind;
        this.armorTier = builder.armorTier;
        this.swordTier = builder.swordTier;
        this.toolTier = builder.toolTier;
        this.enchantments = Collections.unmodifiableMap(
                new LinkedHashMap<Enchantment, Integer>(builder.enchantments));
        this.effects = Collections.unmodifiableList(
                new ArrayList<PotionEffect>(builder.effects));
        this.permission = builder.permission;
    }

    /**
     * Livello numerico della progressione, usato per confronti generici.
     *
     * @return il livello, oppure {@code 0} per gli articoli senza tier
     */
    public int getTierLevel() {
        if (armorTier != null) {
            return armorTier.getLevel();
        }
        if (swordTier != null) {
            return swordTier.getLevel();
        }
        if (toolTier != null) {
            return toolTier.getLevel();
        }
        return 0;
    }

    /**
     * Chiave del nome localizzato nei file lingua.
     */
    public String getNameKey() {
        return "shop.items." + id + ".name";
    }

    /**
     * Chiave della descrizione localizzata nei file lingua.
     */
    public String getLoreKey() {
        return "shop.items." + id + ".lore";
    }

    public String getId() {
        return id;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public Material getMaterial() {
        return material;
    }

    public byte getData() {
        return data;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isTeamColored() {
        return teamColored;
    }

    /**
     * Indica se l'articolo puo' essere acquistato piu' volte.
     */
    public boolean isRepeatable() {
        return repeatable;
    }

    public int getSlot() {
        return slot;
    }

    public ShopTierKind getTierKind() {
        return tierKind;
    }

    public ArmorTier getArmorTier() {
        return armorTier;
    }

    public SwordTier getSwordTier() {
        return swordTier;
    }

    public ToolTier getToolTier() {
        return toolTier;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    public List<PotionEffect> getEffects() {
        return effects;
    }

    /**
     * @return il permesso richiesto, oppure {@code null} se libero
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Costruttore incrementale usato dal loader di configurazione.
     */
    static final class Builder {

        private final String id;
        private final String categoryId;

        private Material material;
        private byte data;
        private int amount = 1;
        private boolean teamColored;
        private boolean repeatable = true;
        private int slot = -1;
        private ShopTierKind tierKind = ShopTierKind.CONSUMABLE;
        private ArmorTier armorTier;
        private SwordTier swordTier;
        private ToolTier toolTier;
        private Map<Enchantment, Integer> enchantments =
                new LinkedHashMap<Enchantment, Integer>();
        private List<PotionEffect> effects = new ArrayList<PotionEffect>();
        private String permission;

        Builder(String id, String categoryId) {
            this.id = id;
            this.categoryId = categoryId;
        }

        Builder material(Material material) {
            this.material = material;
            return this;
        }

        Builder data(byte data) {
            this.data = data;
            return this;
        }

        Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        Builder teamColored(boolean teamColored) {
            this.teamColored = teamColored;
            return this;
        }

        Builder repeatable(boolean repeatable) {
            this.repeatable = repeatable;
            return this;
        }

        Builder slot(int slot) {
            this.slot = slot;
            return this;
        }

        Builder tierKind(ShopTierKind tierKind) {
            this.tierKind = tierKind;
            return this;
        }

        Builder armorTier(ArmorTier armorTier) {
            this.armorTier = armorTier;
            return this;
        }

        Builder swordTier(SwordTier swordTier) {
            this.swordTier = swordTier;
            return this;
        }

        Builder toolTier(ToolTier toolTier) {
            this.toolTier = toolTier;
            return this;
        }

        Builder enchantments(Map<Enchantment, Integer> enchantments) {
            this.enchantments = enchantments;
            return this;
        }

        Builder effects(List<PotionEffect> effects) {
            this.effects = effects;
            return this;
        }

        Builder permission(String permission) {
            this.permission = permission == null || permission.trim().isEmpty()
                    ? null : permission.trim();
            return this;
        }

        ShopItemDefinition build() {
            return new ShopItemDefinition(this);
        }
    }
}
