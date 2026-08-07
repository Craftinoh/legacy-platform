package it.legacynetwork.menu.lang;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.LanguageChangeResult;
import it.legacynetwork.language.PlayerLanguageChangeListener;
import it.legacynetwork.language.PlayerLanguageChangeRequestService;
import it.legacynetwork.language.PlayerLanguageChangeResultListener;
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

public class LanguageMenuService
        implements PlayerLanguageChangeListener,
        PlayerLanguageChangeResultListener {

    private static final int[] LANGUAGE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final long CLICK_COOLDOWN_MILLIS = 500L;
    private static final long REQUEST_TIMEOUT_TICKS = 100L;
    private static final int CHANGE_LIMIT = 3;
    private static final int CHANGE_WINDOW_MINUTES = 60;
    private static final int CHANGE_COOLDOWN_SECONDS = 5;

    private final LegacyMenuPlugin plugin;
    private final FlagTextureService flagTextureService;
    private final LanguageMenuMessages messages;
    private final Map<UUID, Long> clickCooldowns =
            new HashMap<UUID, Long>();
    private final Set<UUID> pendingRequests =
            new HashSet<UUID>();

    public LanguageMenuService(LegacyMenuPlugin plugin,
                               FlagTextureService flagTextureService) {
        this.plugin = plugin;
        this.flagTextureService = flagTextureService;
        this.messages = new LanguageMenuMessages(
                plugin.getDataFolder());
    }

    public void reload() {
        messages.clearCache();
        clickCooldowns.clear();
        pendingRequests.clear();
    }

    public void openMenu(Player player) {
        openMenu(player, 1);
    }

    public void openMenu(Player player, int requestedPage) {
        Language currentLanguage = getPlayerLanguage(player);
        String viewerLanguage = currentLanguage != null
                ? currentLanguage.getCode()
                : plugin.getFallbackLanguage();

        List<Language> sorted = getSortedLanguages();
        int totalPages = Math.max(1,
                (sorted.size() + LANGUAGE_SLOTS.length - 1)
                        / LANGUAGE_SLOTS.length);
        int page = Math.max(1,
                Math.min(requestedPage, totalPages));

        String title = LegacyColorTranslator.translate(
                messages.get(viewerLanguage, "menu.title"));
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }

        Inventory inventory = Bukkit.createInventory(
                new LanguageMenuHolder(page),
                54,
                title);
        fillBackground(inventory);

        int startIndex = (page - 1) * LANGUAGE_SLOTS.length;
        for (int index = 0;
             index < LANGUAGE_SLOTS.length;
             index++) {
            int languageIndex = startIndex + index;
            if (languageIndex >= sorted.size()) {
                break;
            }
            Language language = sorted.get(languageIndex);
            boolean selected = language == currentLanguage;

            ItemStack icon = flagTextureService.getBaseIcon(
                    language.getCode());
            setLanguageDisplay(
                    icon,
                    language,
                    selected,
                    viewerLanguage);
            inventory.setItem(LANGUAGE_SLOTS[index], icon);
        }

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(
                    LegacyColorTranslator.translate(
                            messages.get(
                                    viewerLanguage,
                                    "menu.back")));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(45, back);

        if (page > 1) {
            ItemStack previous = new ItemStack(Material.ARROW);
            ItemMeta previousMeta = previous.getItemMeta();
            if (previousMeta != null) {
                previousMeta.setDisplayName(
                        LegacyColorTranslator.translate(
                                messages.get(
                                        viewerLanguage,
                                        "menu.prev")));
                previous.setItemMeta(previousMeta);
            }
            inventory.setItem(48, previous);
        }

        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        if (pageMeta != null) {
            String pageText = messages.get(
                            viewerLanguage,
                            "menu.page")
                    .replace("{current}",
                            String.valueOf(page))
                    .replace("{total}",
                            String.valueOf(totalPages));
            pageMeta.setDisplayName(
                    LegacyColorTranslator.translate(pageText));
            List<String> pageLore = messages.getList(
                    viewerLanguage,
                    "menu.page-lore");
            if (pageLore != null && !pageLore.isEmpty()) {
                pageMeta.setLore(translateLines(
                        pageLore,
                        null,
                        null,
                        false));
            }
            pageInfo.setItemMeta(pageMeta);
        }
        inventory.setItem(49, pageInfo);

        if (page < totalPages) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName(
                        LegacyColorTranslator.translate(
                                messages.get(
                                        viewerLanguage,
                                        "menu.next")));
                next.setItemMeta(nextMeta);
            }
            inventory.setItem(50, next);
        }

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(
                    LegacyColorTranslator.translate(
                            messages.get(
                                    viewerLanguage,
                                    "menu.close")));
            close.setItemMeta(closeMeta);
        }
        inventory.setItem(53, close);

        player.openInventory(inventory);
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = new ItemStack(
                Material.STAINED_GLASS_PANE,
                1,
                (short) 15);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
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

        String viewerLanguage = getViewerLanguage(player);
        Language current = getPlayerLanguage(player);
        if (clicked == current) {
            sendFeedback(
                    player,
                    viewerLanguage,
                    "change.already-selected",
                    clicked);
            return;
        }

        UUID playerId = player.getUniqueId();
        if (pendingRequests.contains(playerId)) {
            sendFeedback(
                    player,
                    viewerLanguage,
                    "change.pending",
                    clicked);
            return;
        }

        long now = System.currentTimeMillis();
        Long lastClick = clickCooldowns.get(playerId);
        if (lastClick != null
                && now - lastClick < CLICK_COOLDOWN_MILLIS) {
            return;
        }
        clickCooldowns.put(playerId, now);

        PlayerLanguageChangeRequestService requestService =
                plugin.getLanguageChangeRequestService();
        if (requestService == null) {
            plugin.getLogger().warning(
                    "LanguageBackend non disponibile: "
                            + "cambio lingua non inviato.");
            sendFeedback(
                    player,
                    viewerLanguage,
                    "change.unavailable",
                    clicked);
            return;
        }

        pendingRequests.add(playerId);
        if (!requestService.requestLanguageChange(
                playerId, clicked)) {
            pendingRequests.remove(playerId);
            sendFeedback(
                    player,
                    viewerLanguage,
                    "change.error",
                    clicked);
            return;
        }

        Bukkit.getScheduler().runTaskLater(
                plugin,
                new Runnable() {
                    @Override
                    public void run() {
                        if (pendingRequests.remove(playerId)
                                && player.isOnline()) {
                            sendFeedback(
                                    player,
                                    getViewerLanguage(player),
                                    "change.timeout",
                                    clicked);
                        }
                    }
                },
                REQUEST_TIMEOUT_TICKS);
    }

    @Override
    public void onLanguageChanged(UUID playerId,
                                  Language previous,
                                  Language current) {
        pendingRequests.remove(playerId);
        refreshOpenMenu(playerId);
    }

    @Override
    public void onLanguageChangeResult(UUID playerId,
                                       Language requestedLanguage,
                                       LanguageChangeResult result) {
        pendingRequests.remove(playerId);
        clickCooldowns.remove(playerId);

        final Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }

        String key;
        switch (result) {
            case SUCCESS:
                key = "change.success";
                break;
            case ALREADY_SELECTED:
                key = "change.already-selected";
                break;
            case COOLDOWN:
                key = "change.cooldown";
                break;
            case RATE_LIMITED:
                key = "change.rate-limited";
                break;
            case ERROR:
            default:
                key = "change.error";
                break;
        }

        String viewerLanguage = result == LanguageChangeResult.SUCCESS
                ? requestedLanguage.getCode()
                : getViewerLanguage(player);
        sendFeedback(
                player,
                viewerLanguage,
                key,
                requestedLanguage);
        refreshOpenMenu(playerId);
    }

    private void refreshOpenMenu(final UUID playerId) {
        final Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        Runnable refresh = new Runnable() {
            @Override
            public void run() {
                if (player.getOpenInventory()
                        .getTopInventory()
                        .getHolder()
                        instanceof LanguageMenuHolder) {
                    LanguageMenuHolder holder =
                            (LanguageMenuHolder) player
                                    .getOpenInventory()
                                    .getTopInventory()
                                    .getHolder();
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

    private void sendFeedback(Player player,
                              String viewerLanguage,
                              String key,
                              Language requestedLanguage) {
        String raw = messages.get(viewerLanguage, key);
        String rendered = replacePlaceholders(
                raw,
                requestedLanguage,
                requestedLanguage == null
                        ? null
                        : flagTextureService.getCountryName(
                                requestedLanguage.getCode()),
                false);
        player.sendMessage(
                LegacyColorTranslator.translate(rendered));
    }

    public Language getLanguageAtSlot(int slot, int page) {
        List<Language> sorted = getSortedLanguages();
        int startIndex = (page - 1) * LANGUAGE_SLOTS.length;
        for (int index = 0;
             index < LANGUAGE_SLOTS.length;
             index++) {
            if (LANGUAGE_SLOTS[index] == slot) {
                int languageIndex = startIndex + index;
                if (languageIndex >= 0
                        && languageIndex < sorted.size()) {
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

    private void setLanguageDisplay(ItemStack item,
                                    Language language,
                                    boolean selected,
                                    String viewerLanguage) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        String nameFormat = messages.get(
                viewerLanguage,
                selected
                        ? "menu.language-name-selected"
                        : "menu.language-name");
        String country = flagTextureService.getCountryName(
                language.getCode());
        String name = replacePlaceholders(
                nameFormat,
                language,
                country,
                selected);

        if (selected) {
            meta.addEnchant(
                    Enchantment.DURABILITY,
                    1,
                    true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.setDisplayName(
                LegacyColorTranslator.translate(name));

        List<String> lore = messages.getList(
                viewerLanguage,
                selected
                        ? "menu.lore.selected"
                        : "menu.lore.unselected");
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(translateLines(
                    lore,
                    language,
                    country,
                    selected));
        }
        item.setItemMeta(meta);
    }

    private List<String> translateLines(List<String> lines,
                                        Language language,
                                        String country,
                                        boolean selected) {
        List<String> translated = new ArrayList<String>();
        for (String line : lines) {
            translated.add(
                    LegacyColorTranslator.translate(
                            replacePlaceholders(
                                    line,
                                    language,
                                    country,
                                    selected)));
        }
        return translated;
    }

    private String replacePlaceholders(String input,
                                       Language language,
                                       String country,
                                       boolean selected) {
        String value = input == null ? "" : input;
        String languageName = language == null
                ? "-"
                : language.getNativeName();
        String languageCode = language == null
                ? "-"
                : language.getCode();
        String safeCountry = country == null
                || country.trim().isEmpty()
                ? "-"
                : country;

        return value
                .replace("{language}", languageName)
                .replace("%language%", languageName)
                .replace("{code}", languageCode)
                .replace("{country}", safeCountry)
                .replace("{status}", selected
                        ? messages.get("en", "menu.status.selected")
                        : messages.get("en", "menu.status.available"))
                .replace("{limit}",
                        String.valueOf(CHANGE_LIMIT))
                .replace("{minutes}",
                        String.valueOf(CHANGE_WINDOW_MINUTES))
                .replace("{seconds}",
                        String.valueOf(CHANGE_COOLDOWN_SECONDS));
    }

    private String getViewerLanguage(Player player) {
        Language language = getPlayerLanguage(player);
        return language != null
                ? language.getCode()
                : plugin.getFallbackLanguage();
    }

    private Language getPlayerLanguage(Player player) {
        PlayerLanguageProvider provider =
                plugin.getLanguageProvider();
        if (provider == null) {
            return Language.findByInput(
                            plugin.getFallbackLanguage())
                    .orElse(Language.ENGLISH);
        }
        Language language = provider.getLanguage(
                player.getUniqueId());
        return language != null
                ? language
                : Language.ENGLISH;
    }

    private List<Language> getSortedLanguages() {
        List<Language> languages =
                new ArrayList<Language>();
        Collections.addAll(languages, Language.values());
        Collections.sort(
                languages,
                new Comparator<Language>() {
                    @Override
                    public int compare(Language first,
                                       Language second) {
                        return Integer.compare(
                                first.getMenuOrder(),
                                second.getMenuOrder());
                    }
                });
        return languages;
    }
}
