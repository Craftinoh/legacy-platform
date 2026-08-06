package it.legacynetwork.menu.lang;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.PlayerLanguageChangeListener;
import it.legacynetwork.language.PlayerLanguageChangeRequestService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LanguageMenuService implements PlayerLanguageChangeListener {
    private static final int[] LANGUAGE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final LegacyMenuPlugin plugin;
    private final FlagTextureService flagTextureService;
    private final LanguageMenuMessages messages;
    private final Map<UUID, Long> clickCooldowns = new HashMap<UUID, Long>();
    private final Set<UUID> pendingRequests = new HashSet<UUID>();

    public LanguageMenuService(LegacyMenuPlugin plugin,
                               FlagTextureService flagTextureService) {
        this.plugin = plugin;
        this.flagTextureService = flagTextureService;
        this.messages = new LanguageMenuMessages(plugin.getDataFolder());
    }

    public void openMenu(Player player) {
        openMenu(player, 1);
    }

    public void openMenu(Player player, int requestedPage) {
        Language currentLanguage = getPlayerLanguage(player);
        String viewerLanguage = currentLanguage != null
                ? currentLanguage.getCode() : plugin.getFallbackLanguage();

        List<Language> sorted = getSortedLanguages();
        int totalPages = Math.max(1, (sorted.size() + 27) / 28);
        int page = Math.max(1, Math.min(requestedPage, totalPages));

        String title = LegacyColorTranslator.translate(
                messages.get(viewerLanguage, "menu.title"));
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }

        Inventory inventory = Bukkit.createInventory(
                new LanguageMenuHolder(page), 54, title);

        int startIndex = (page - 1) * 28;
        for (int index = 0; index < LANGUAGE_SLOTS.length; index++) {
            int languageIndex = startIndex + index;
            if (languageIndex >= sorted.size()) {
                break;
            }
            Language language = sorted.get(languageIndex);
            boolean selected = language == currentLanguage;

            ItemStack icon = flagTextureService.getBaseIcon(language.getCode());
            setLanguageDisplay(icon, language, selected, viewerLanguage);
            inventory.setItem(LANGUAGE_SLOTS[index], icon);
        }

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(LegacyColorTranslator.translate(
                    messages.get(viewerLanguage, "menu.back")));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(45, back);

        if (page > 1) {
            ItemStack previous = new ItemStack(Material.ARROW);
            ItemMeta previousMeta = previous.getItemMeta();
            if (previousMeta != null) {
                previousMeta.setDisplayName(LegacyColorTranslator.translate(
                        messages.get(viewerLanguage, "menu.prev")));
                previous.setItemMeta(previousMeta);
            }
            inventory.setItem(48, previous);
        }

        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        if (pageMeta != null) {
            String pageText = messages.get(viewerLanguage, "menu.page")
                    .replace("{current}", String.valueOf(page))
                    .replace("{total}", String.valueOf(totalPages));
            pageMeta.setDisplayName(LegacyColorTranslator.translate(pageText));
            pageInfo.setItemMeta(pageMeta);
        }
        inventory.setItem(49, pageInfo);

        if (page < totalPages) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName(LegacyColorTranslator.translate(
                        messages.get(viewerLanguage, "menu.next")));
                next.setItemMeta(nextMeta);
            }
            inventory.setItem(50, next);
        }

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(LegacyColorTranslator.translate(
                    messages.get(viewerLanguage, "menu.close")));
            close.setItemMeta(closeMeta);
        }
        inventory.setItem(53, close);

        player.openInventory(inventory);
    }

    public void handleClick(Player player, int slot, int page) {
        int navigation = getNavAction(slot);
        if (navigation == -1 || navigation == 3) {
            player.closeInventory();
            return;
        }
        if (navigation == 1) {
            openMenu(player, page - 1);
            return;
        }
        if (navigation == 2) {
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

        UUID playerId = player.getUniqueId();
        if (pendingRequests.contains(playerId)) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastClick = clickCooldowns.get(playerId);
        if (lastClick != null && now - lastClick < 500L) {
            return;
        }
        clickCooldowns.put(playerId, now);

        PlayerLanguageChangeRequestService requestService =
                plugin.getLanguageChangeRequestService();
        if (requestService == null) {
            plugin.getLogger().warning(
                    "LanguageBackend non disponibile: cambio lingua non inviato.");
            return;
        }

        pendingRequests.add(playerId);
        if (!requestService.requestLanguageChange(playerId, clicked)) {
            pendingRequests.remove(playerId);
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                pendingRequests.remove(playerId);
            }
        }, 100L);
    }

    @Override
    public void onLanguageChanged(UUID playerId, Language previous, Language current) {
        pendingRequests.remove(playerId);
        final Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        Runnable refresh = new Runnable() {
            @Override
            public void run() {
                if (player.getOpenInventory().getTopInventory().getHolder()
                        instanceof LanguageMenuHolder) {
                    LanguageMenuHolder holder = (LanguageMenuHolder) player
                            .getOpenInventory().getTopInventory().getHolder();
                    openMenu(player, holder.getPage());
                }
            }
        };
        if (Bukkit.isPrimaryThread()) {
            refresh.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, refresh);
        }
    }

    public Language getLanguageAtSlot(int slot, int page) {
        List<Language> sorted = getSortedLanguages();
        int startIndex = (page - 1) * 28;
        for (int index = 0; index < LANGUAGE_SLOTS.length; index++) {
            if (LANGUAGE_SLOTS[index] == slot) {
                int languageIndex = startIndex + index;
                if (languageIndex >= 0 && languageIndex < sorted.size()) {
                    return sorted.get(languageIndex);
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
        return plugin.getLanguageProvider();
    }

    public LanguageMenuMessages getMessages() {
        return messages;
    }

    private void setLanguageDisplay(ItemStack item, Language language,
                                    boolean selected, String viewerLanguage) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        String name = language.getNativeName();
        if (selected) {
            name = messages.get(viewerLanguage, "menu.selected") + name;
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.setDisplayName(LegacyColorTranslator.translate(name));

        List<String> lore = selected
                ? messages.getList(viewerLanguage, "menu.lore.selected")
                : messages.getList(viewerLanguage, "menu.lore.unselected");
        if (lore != null && !lore.isEmpty()) {
            List<String> translatedLore = new ArrayList<String>();
            for (String line : lore) {
                translatedLore.add(LegacyColorTranslator.translate(line));
            }
            meta.setLore(translatedLore);
        }
        item.setItemMeta(meta);
    }

    private Language getPlayerLanguage(Player player) {
        PlayerLanguageProvider provider = plugin.getLanguageProvider();
        if (provider == null) {
            return Language.findByInput(plugin.getFallbackLanguage())
                    .orElse(Language.ENGLISH);
        }
        Language language = provider.getLanguage(player.getUniqueId());
        return language != null ? language : Language.ENGLISH;
    }

    private List<Language> getSortedLanguages() {
        List<Language> languages = new ArrayList<Language>();
        Collections.addAll(languages, Language.values());
        Collections.sort(languages, new Comparator<Language>() {
            @Override
            public int compare(Language first, Language second) {
                return Integer.compare(first.getMenuOrder(), second.getMenuOrder());
            }
        });
        return languages;
    }
}
