package it.legacynetwork.regions.selection;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class WorldEditSelectionProvider implements SelectionProvider {

    private final Plugin worldEditPlugin;
    private final Method getSelectionMethod;

    public WorldEditSelectionProvider(Plugin worldEditPlugin) {
        if (worldEditPlugin == null) {
            throw new IllegalArgumentException("Plugin WorldEdit mancante");
        }
        this.worldEditPlugin = worldEditPlugin;
        try {
            this.getSelectionMethod = worldEditPlugin.getClass()
                    .getMethod("getSelection", Player.class);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException(
                    "API legacy getSelection(Player) non disponibile", exception);
        }
    }

    @Override
    public boolean isAvailable() {
        return worldEditPlugin.isEnabled() && getSelectionMethod != null;
    }

    @Override
    public RegionSelection getSelection(Player player) {
        if (!isAvailable() || player == null) {
            return null;
        }
        try {
            Object selection = getSelectionMethod.invoke(worldEditPlugin, player);
            if (selection == null) {
                return null;
            }

            Method getMinimumPoint = selection.getClass()
                    .getMethod("getMinimumPoint");
            Method getMaximumPoint = selection.getClass()
                    .getMethod("getMaximumPoint");
            Object minimum = getMinimumPoint.invoke(selection);
            Object maximum = getMaximumPoint.invoke(selection);
            if (!(minimum instanceof Location) || !(maximum instanceof Location)) {
                return null;
            }

            Location min = (Location) minimum;
            Location max = (Location) maximum;
            World world = min.getWorld() != null ? min.getWorld() : player.getWorld();
            if (world == null) {
                return null;
            }
            if (max.getWorld() != null
                    && !max.getWorld().getUID().equals(world.getUID())) {
                return null;
            }

            return new RegionSelection(
                    world.getName(),
                    world.getUID().toString(),
                    min.getBlockX(), min.getBlockY(), min.getBlockZ(),
                    max.getBlockX(), max.getBlockY(), max.getBlockZ());
        } catch (InvocationTargetException exception) {
            return null;
        } catch (ReflectiveOperationException exception) {
            return null;
        } catch (LinkageError exception) {
            return null;
        }
    }
}
