package it.legacynetwork.items.item;

import it.legacynetwork.items.definition.CustomItemDefinition;
import it.legacynetwork.items.definition.CustomItemLanguage;
import it.legacynetwork.items.language.PlayerLanguageAccessor;
import it.legacynetwork.items.placeholder.ItemPlaceholderService;
import it.legacynetwork.items.util.LegacyColorTranslator;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CustomItemFactory {
    private static final ItemFlag[] EMPTY_FLAGS = {};

    private final ItemPlaceholderService placeholderService;
    private final PlayerLanguageAccessor languageAccessor;
    private final String serverId;

    public CustomItemFactory(ItemPlaceholderService placeholderService,
                              PlayerLanguageAccessor languageAccessor,
                              String serverId) {
        this.placeholderService = placeholderService;
        this.languageAccessor = languageAccessor;
        this.serverId = serverId;
    }

    public ItemStack createItem(Player player, CustomItemDefinition definition) {
        Material material = Material.matchMaterial(definition.getMaterial());
        if (material == null) {
            return null;
        }
        ItemStack item = new ItemStack(material, definition.getAmount(),
                (short) definition.getData());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String langCode = languageAccessor.getLanguageCode(player);
        CustomItemLanguage lang = definition.getLanguage(langCode);
        if (lang == null) {
            return item;
        }

        String name = resolve(player, lang.getName());
        meta.setDisplayName(LegacyColorTranslator.translate(name));

        List<String> resolvedLore = new ArrayList<>();
        for (String loreLine : lang.getLore()) {
            resolvedLore.add(LegacyColorTranslator.translate(resolve(player, loreLine)));
        }
        meta.setLore(resolvedLore);

        if (meta instanceof SkullMeta && definition.getSkullOwner() != null
                && !definition.getSkullOwner().isEmpty()) {
            SkullMeta skullMeta = (SkullMeta) meta;
            skullMeta.setOwner(resolve(player, definition.getSkullOwner()));
        }

        for (Map.Entry<String, Integer> entry : definition.getEnchantments().entrySet()) {
            Enchantment enchantment = Enchantment.getByName(entry.getKey());
            if (enchantment != null) {
                meta.addEnchant(enchantment, entry.getValue(), true);
            }
        }

        for (String flagName : definition.getItemFlags()) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(flagName));
            } catch (IllegalArgumentException ignored) {
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    private String resolve(Player player, String text) {
        if (text == null) {
            return "";
        }
        String result = text;
        result = result.replace("{player}", player != null ? player.getName() : "???");
        result = result.replace("{uuid}",
                player != null ? player.getUniqueId().toString() : "???");
        result = result.replace("{world}",
                player != null ? player.getWorld().getName() : "???");
        result = result.replace("{server}", serverId);
        result = result.replace("{online}",
                String.valueOf(org.bukkit.Bukkit.getOnlinePlayers().size()));
        result = result.replace("{language}",
                languageAccessor.getLanguageCode(player));
        result = result.replace("{language_code}",
                languageAccessor.getLanguageCode(player));
        if (placeholderService.isAvailable() && player != null) {
            result = placeholderService.apply(player, result);
        }
        return result;
    }
}
