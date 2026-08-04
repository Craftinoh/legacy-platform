package it.legacynetwork.items.listener;

import it.legacynetwork.items.action.ItemActionExecutor;
import it.legacynetwork.items.cooldown.ItemCooldownService;
import it.legacynetwork.items.definition.CustomItemClickActions;
import it.legacynetwork.items.definition.CustomItemDefinition;
import it.legacynetwork.items.item.CustomItemMatcher;
import it.legacynetwork.items.item.CustomItemRegistry;
import it.legacynetwork.items.message.MessageService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class PlayerInteractListener implements Listener {
    private final ItemActionExecutor actionExecutor;
    private final ItemCooldownService cooldownService;
    private final CustomItemMatcher matcher;
    private final CustomItemRegistry registry;
    private final MessageService messageService;

    public PlayerInteractListener(ItemActionExecutor actionExecutor,
                                   ItemCooldownService cooldownService,
                                   CustomItemMatcher matcher,
                                   CustomItemRegistry registry,
                                   MessageService messageService) {
        this.actionExecutor = actionExecutor;
        this.cooldownService = cooldownService;
        this.matcher = matcher;
        this.registry = registry;
        this.messageService = messageService;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        CustomItemDefinition def = matcher.match(item);
        if (def == null) {
            return;
        }
        String actionKey = getActionKey(event);
        CustomItemClickActions actions = def.getActions().get(actionKey);
        if (actions == null) {
            return;
        }
        if (cooldownService.isOnCooldown(player.getUniqueId(), def.getId(), actionKey)) {
            long remaining = cooldownService.getRemainingMillis(
                    player.getUniqueId(), def.getId(), actionKey);
            String msg = messageService.getMessage("cooldown", player,
                    java.util.Collections.singletonMap("seconds",
                            String.valueOf((remaining + 999) / 1000)));
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            event.setCancelled(true);
            return;
        }
        if (actions.isCancelEvent()) {
            event.setCancelled(true);
        }
        cooldownService.setCooldown(player.getUniqueId(), def.getId(), actionKey,
                actions.getCooldownMillis());
        actionExecutor.executeActions(player, actions.getExecute());
    }

    private String getActionKey(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            return "right_click";
        }
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            return "left_click";
        }
        if (action == Action.PHYSICAL) {
            return "physical";
        }
        return "right_click";
    }
}
