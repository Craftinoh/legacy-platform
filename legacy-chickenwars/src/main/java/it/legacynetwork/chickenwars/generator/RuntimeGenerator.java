package it.legacynetwork.chickenwars.generator;

import it.legacynetwork.chickenwars.arena.GeneratorDefinition;
import it.legacynetwork.chickenwars.config.GeneratorSettings;
import it.legacynetwork.chickenwars.config.GeneratorTier;
import it.legacynetwork.chickenwars.hologram.Hologram;
import it.legacynetwork.chickenwars.model.ResourceType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;

/**
 * Generatore attivo durante una partita.
 *
 * <p>Non possiede un proprio task: viene fatto avanzare dal ciclo centrale della
 * partita, come richiesto dai vincoli di prestazione.</p>
 */
public final class RuntimeGenerator {

    private static final Vector NO_VELOCITY = new Vector(0.0D, 0.0D, 0.0D);

    private final GeneratorDefinition definition;
    private final Location spawnLocation;

    private int level;
    private int ticksUntilNext;
    private Hologram hologram;

    public RuntimeGenerator(GeneratorDefinition definition, Location spawnLocation,
                            GeneratorSettings settings) {
        if (definition == null) {
            throw new IllegalArgumentException("Definizione generatore mancante");
        }
        if (spawnLocation == null) {
            throw new IllegalArgumentException("Posizione generatore non risolta");
        }
        this.definition = definition;
        this.spawnLocation = spawnLocation;
        this.level = definition.getLevel();
        this.ticksUntilNext = settings.getTier(definition.getType(), level)
                .getIntervalTicks();
    }

    /**
     * Fa avanzare il generatore di un tick, producendo risorse quando dovuto.
     *
     * @param settings parametri correnti
     * @return {@code true} se in questo tick e' stata generata una risorsa
     */
    public boolean tick(GeneratorSettings settings) {
        GeneratorTier tier = settings.getTier(definition.getType(), level);
        if (--ticksUntilNext > 0) {
            return false;
        }
        ticksUntilNext = tier.getIntervalTicks();
        spawnResource(settings, tier.getAmount());
        return true;
    }

    private void spawnResource(GeneratorSettings settings, int amount) {
        World world = spawnLocation.getWorld();
        if (world == null) {
            return;
        }
        ResourceType type = definition.getType();

        double radius = settings.getMergeRadius();
        int onGround = 0;
        Item mergeTarget = null;
        Collection<Entity> nearby =
                world.getNearbyEntities(spawnLocation, radius, radius, radius);
        for (Entity entity : nearby) {
            if (!(entity instanceof Item)) {
                continue;
            }
            Item item = (Item) entity;
            ItemStack stack = item.getItemStack();
            if (stack == null || stack.getType() != type.getMaterial()) {
                continue;
            }
            onGround += stack.getAmount();
            if (mergeTarget == null) {
                mergeTarget = item;
            }
        }

        if (onGround >= settings.getMaximumGroundItems()) {
            return;
        }

        if (settings.isItemStacking() && mergeTarget != null) {
            ItemStack stack = mergeTarget.getItemStack();
            int newAmount = Math.min(stack.getMaxStackSize(),
                    stack.getAmount() + amount);
            if (newAmount > stack.getAmount()) {
                stack.setAmount(newAmount);
                mergeTarget.setItemStack(stack);
                return;
            }
        }

        Item dropped = world.dropItem(spawnLocation,
                new ItemStack(type.getMaterial(), amount));
        dropped.setVelocity(NO_VELOCITY);
    }

    /**
     * Aggiorna le righe dell'ologramma associato, se presente.
     *
     * @param lines righe gia' tradotte
     */
    public void updateHologram(List<String> lines) {
        if (hologram != null) {
            hologram.update(lines);
        }
    }

    /**
     * Rimuove l'ologramma associato.
     */
    public void removeHologram() {
        if (hologram != null) {
            hologram.remove();
            hologram = null;
        }
    }

    /** Secondi mancanti alla prossima generazione, arrotondati per eccesso. */
    public int getSecondsUntilNext() {
        return (int) Math.ceil(ticksUntilNext / 20.0D);
    }

    public GeneratorDefinition getDefinition() {
        return definition;
    }

    public ResourceType getType() {
        return definition.getType();
    }

    public Location getSpawnLocation() {
        return spawnLocation.clone();
    }

    public int getLevel() {
        return level;
    }

    /**
     * Modifica il livello e riallinea immediatamente il conto alla rovescia.
     */
    public void setLevel(int level, GeneratorSettings settings) {
        this.level = Math.max(1, level);
        this.ticksUntilNext = Math.min(this.ticksUntilNext,
                settings.getTier(definition.getType(), this.level).getIntervalTicks());
    }

    public Hologram getHologram() {
        return hologram;
    }

    public void setHologram(Hologram hologram) {
        this.hologram = hologram;
    }
}
