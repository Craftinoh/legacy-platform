package it.legacynetwork.menu.lang;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageEventService;
import it.legacynetwork.language.PlayerLanguageProvider;
import it.legacynetwork.menu.LegacyMenuPlugin;
import it.legacynetwork.menu.util.LegacyColorTranslator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LanguageMenuService {
    private static final int[] LANGUAGE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final LegacyMenuPlugin plugin;
    private final PlayerLanguageProvider languageProvider;
    private final FlagTextureService flagTextureService;
    private final LanguageMenuMessages messages;
    private final Map<UUID, Long> cooldowns = new HashMap<UUID, Long>();

    public LanguageMenuService(LegacyMenuPlugin plugin,
                               PlayerLanguageProvider languageProvider,
                               FlagTextureService flagTextureService) {
        this.plugin = plugin;
        this.languageProvider = languageProvider;
        this.flagTextureService = flagTextureService;
        this.messages = new LanguageMenuMessages(plugin.getDataFolder());
    }

    public void openMenu(Player player) {
        openMenu(player, 1);
    }

    public void openMenu(Player player, int page) {
        Language currentLang = getPlayerLanguage(player);
        String viewerLang = currentLang != null ? currentLang.getCode() : "en";

        String title = LegacyColorTranslator.translate(messages.get(viewerLang, "menu.title"));
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }

        Inventory inv = Bukkit.createInventory(new LanguageMenuHolder(page), 54, title);

        List<Language> sorted = getSortedLanguages();
        int totalPages = (sorted.size() + 27) / 28;

        int startIdx = (page - 1) * 28;
        for (int i = 0; i < LANGUAGE_SLOTS.length; i++) {
            int langIdx = startIdx + i;
            if (langIdx >= sorted.size()) {
                break;
            }
            Language lang = sorted.get(langIdx);
            boolean isSelected = lang == currentLang;

            ItemStack baseIcon = flagTextureService.getBaseIcon(lang.getCode());
            setLanguageDisplay(baseIcon, lang, isSelected, viewerLang);
            inv.setItem(LANGUAGE_SLOTS[i], baseIcon);
        }

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(LegacyColorTranslator.translate(
                    messages.get(viewerLang, "menu.back")));
            back.setItemMeta(backMeta);
        }
        inv.setItem(45, back);

        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName(LegacyColorTranslator.translate(
                        messages.get(viewerLang, "menu.prev")));
                prev.setItemMeta(prevMeta);
            }
            inv.setItem(48, prev);
        }

        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        if (pageMeta != null) {
            String pageText = messages.get(viewerLang, "menu.page")
                    .replace("{current}", String.valueOf(page))
                    .replace("{total}", String.valueOf(totalPages));
            pageMeta.setDisplayName(LegacyColorTranslator.translate(pageText));
            pageInfo.setItemMeta(pageMeta);
        }
        inv.setItem(49, pageInfo);

        if (page < totalPages) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName(LegacyColorTranslator.translate(
                        messages.get(viewerLang, "menu.next")));
                next.setItemMeta(nextMeta);
            }
            inv.setItem(50, next);
        }

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(LegacyColorTranslator.translate(
                    messages.get(viewerLang, "menu.close")));
            close.setItemMeta(closeMeta);
        }
        inv.setItem(53, close);

        player.openInventory(inv);
    }

    public void handleClick(Player player, int slot, int page) {
        int navAction = getNavAction(slot);

        if (navAction == -1 || navAction == 3) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    player.closeInventory();
                }
            });
            return;
        }

        if (navAction == 1) {
            openMenu(player, page - 1);
            return;
        }

        if (navAction == 2) {
            openMenu(player, page + 1);
            return;
        }

        Language clicked = getLanguageAtSlot(slot, page);
        if (clicked == null) {
            return;
        }

        Language current = getPlayerLanguage(player);
        if (clicked == current) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Long lastClick = cooldowns.get(uuid);
        long now = System.currentTimeMillis();
        if (lastClick != null && (now - lastClick) < 500) {
            return;
        }
        cooldowns.put(uuid, now);

        PlayerLanguageEventService eventService = plugin.getLanguageEventService();
        if (eventService != null) {
            eventService.fireLanguageChanged(uuid, current, clicked);
        }

        openMenu(player, page);
    }

    public Language getLanguageAtSlot(int slot, int page) {
        List<Language> sorted = getSortedLanguages();
        int startIdx = (page - 1) * 28;
        for (int i = 0; i < LANGUAGE_SLOTS.length; i++) {
            if (LANGUAGE_SLOTS[i] == slot) {
                int langIdx = startIdx + i;
                if (langIdx >= 0 && langIdx < sorted.size()) {
                    return sorted.get(langIdx);
                }
                return null;
            }
        }
        return null;
    }

    public static int getNavAction(int slot) {
        if (slot == 45) {
            return -1;
        }
        if (slot == 48) {
            return 1;
        }
        if (slot == 50) {
            return 2;
        }
        if (slot == 53) {
            return 3;
        }
        return 0;
    }

    public PlayerLanguageProvider getLanguageProvider() {
        return languageProvider;
    }

    public LanguageMenuMessages getMessages() {
        return messages;
    }

    private void setLanguageDisplay(ItemStack item, Language lang, boolean isSelected, String viewerLang) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        String name = lang.getNativeName();
        if (isSelected) {
            name = messages.get(viewerLang, "menu.selected") + name;
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.setDisplayName(LegacyColorTranslator.translate(name));

        List<String> loreKeyList;
        if (isSelected) {
            loreKeyList = messages.getList(viewerLang, "menu.lore.selected");
        } else {
            loreKeyList = messages.getList(viewerLang, "menu.lore.unselected");
        }
        if (loreKeyList != null && !loreKeyList.isEmpty()) {
            List<String> translatedLore = new ArrayList<String>();
            for (String line : loreKeyList) {
                translatedLore.add(LegacyColorTranslator.translate(line));
            }
            meta.setLore(translatedLore);
        }

        item.setItemMeta(meta);
    }

    private Language getPlayerLanguage(Player player) {
        if (languageProvider == null) {
            return null;
        }
        return languageProvider.getLanguage(player.getUniqueId());
    }

    private List<Language> getSortedLanguages() {
        List<Language> all = new ArrayList<Language>();
        for (Language lang : Language.values()) {
            all.add(lang);
        }
        Collections.sort(all, new Comparator<Language>() {
            @Override
            public int compare(Language a, Language b) {
                return Integer.compare(a.getMenuOrder(), b.getMenuOrder());
            }
        });
        return all;
    }
}
