package it.legacynetwork.chickenwars.player;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Copia dello stato di un giocatore prima dell'ingresso in partita.
 *
 * <p>Consente di riportarlo esattamente com'era all'uscita, compreso il punto in
 * cui si trovava, evitando che la partita alteri l'inventario personale.</p>
 */
public final class InventorySnapshot {

    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final GameMode gameMode;
    private final Location location;
    private final int level;
    private final float experience;
    private final double health;
    private final int foodLevel;
    private final boolean allowFlight;
    private final boolean flying;
    private final List<PotionEffect> effects;

    private InventorySnapshot(Player player) {
        this.contents = player.getInventory().getContents().clone();
        this.armor = player.getInventory().getArmorContents().clone();
        this.gameMode = player.getGameMode();
        this.location = player.getLocation().clone();
        this.level = player.getLevel();
        this.experience = player.getExp();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.allowFlight = player.getAllowFlight();
        this.flying = player.isFlying();
        this.effects = new ArrayList<PotionEffect>(player.getActivePotionEffects());
    }

    /**
     * Cattura lo stato corrente del giocatore.
     *
     * @return la copia, oppure {@code null} se il giocatore non e' valido
     */
    public static InventorySnapshot capture(Player player) {
        if (player == null) {
            return null;
        }
        return new InventorySnapshot(player);
    }

    /**
     * Ripristina lo stato salvato sul giocatore indicato.
     *
     * <p>Il teletrasporto viene eseguito per ultimo, dopo aver riportato
     * modalita' di gioco e volo, per evitare cadute impreviste.</p>
     */
    public void restore(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        clearEffects(player);
        player.getInventory().setContents(contents);
        player.getInventory().setArmorContents(armor);
        player.updateInventory();
        player.setGameMode(gameMode);
        player.setLevel(level);
        player.setExp(experience);
        player.setFoodLevel(foodLevel);
        player.setMaxHealth(20.0D);
        player.setHealth(Math.min(Math.max(health, 1.0D), player.getMaxHealth()));
        player.setAllowFlight(allowFlight);
        player.setFlying(allowFlight && flying);
        player.setFireTicks(0);
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect, true);
        }
        if (location != null && location.getWorld() != null) {
            player.teleport(location);
        }
    }

    /**
     * Azzera inventario, effetti e statistiche vitali del giocatore.
     *
     * <p>Usato all'ingresso in partita e a ogni respawn.</p>
     */
    public static void clear(Player player) {
        if (player == null) {
            return;
        }
        clearEffects(player);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.updateInventory();
        player.setLevel(0);
        player.setExp(0.0F);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setMaxHealth(20.0D);
        player.setHealth(20.0D);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
    }

    private static void clearEffects(Player player) {
        Collection<PotionEffect> active =
                new ArrayList<PotionEffect>(player.getActivePotionEffects());
        for (PotionEffect effect : active) {
            player.removePotionEffect(effect.getType());
        }
    }

    public Location getLocation() {
        return location == null ? null : location.clone();
    }
}
