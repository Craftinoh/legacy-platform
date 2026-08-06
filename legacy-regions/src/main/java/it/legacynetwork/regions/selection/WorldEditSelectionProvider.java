package it.legacynetwork.regions.selection;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class WorldEditSelectionProvider implements SelectionProvider {

    private final boolean available;
    private final Object worldEditPlugin;
    private final Method getWorldEditMethod;
    private final Method getSessionMethod;
    private final Method getRegionSelectorMethod;
    private final Method getRegionMethod;
    private final Method getMinimumPointMethod;
    private final Method getMaximumPointMethod;
    private final Method getBlockXMethod;
    private final Method getBlockYMethod;
    private final Method getBlockZMethod;

    public WorldEditSelectionProvider(Object worldEditPluginInstance) {
        this.worldEditPlugin = worldEditPluginInstance;
        boolean avail = false;
        Method getWE = null;
        Method getSession = null;
        Method getRegionSelector = null;
        Method getRegion = null;
        Method getMinPoint = null;
        Method getMaxPoint = null;
        Method getBlockX = null;
        Method getBlockY = null;
        Method getBlockZ = null;

        try {
            Class<?> wePluginClass = Class.forName("com.sk89q.worldedit.bukkit.WorldEditPlugin");
            Class<?> localSessionClass = Class.forName("com.sk89q.worldedit.LocalSession");
            Class<?> worldEditClass = Class.forName("com.sk89q.worldedit.WorldEdit");
            Class<?> actorClass = Class.forName("com.sk89q.worldedit.extension.platform.Actor");
            Class<?> regionSelectorClass = Class.forName("com.sk89q.worldedit.regions.selector.RegionSelector");
            Class<?> regionClass = Class.forName("com.sk89q.worldedit.regions.Region");
            Class<?> vectorClass = Class.forName("com.sk89q.worldedit.Vector");
            Class<?> worldClass = Class.forName("com.sk89q.worldedit.LocalWorld");

            getWE = wePluginClass.getMethod("getWorldEdit");
            getSession = worldEditClass.getMethod("getSession", actorClass);
            getRegionSelector = localSessionClass.getMethod("getRegionSelector", worldClass);
            getRegion = regionSelectorClass.getMethod("getRegion");
            getMinPoint = regionClass.getMethod("getMinimumPoint");
            getMaxPoint = regionClass.getMethod("getMaximumPoint");
            getBlockX = vectorClass.getMethod("getBlockX");
            getBlockY = vectorClass.getMethod("getBlockY");
            getBlockZ = vectorClass.getMethod("getBlockZ");

            avail = true;
        } catch (Exception e) {
            avail = false;
        }

        this.available = avail;
        this.getWorldEditMethod = getWE;
        this.getSessionMethod = getSession;
        this.getRegionSelectorMethod = getRegionSelector;
        this.getRegionMethod = getRegion;
        this.getMinimumPointMethod = getMinPoint;
        this.getMaximumPointMethod = getMaxPoint;
        this.getBlockXMethod = getBlockX;
        this.getBlockYMethod = getBlockY;
        this.getBlockZMethod = getBlockZ;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public int[] getSelection(Player player) {
        if (!available) {
            return null;
        }
        try {
            Object worldEdit = getWorldEditMethod.invoke(worldEditPlugin);
            Object localSession = getSessionMethod.invoke(worldEdit, player);
            Object bukkitWorld = player.getWorld();
            Object regionSelector = getRegionSelectorMethod.invoke(localSession, bukkitWorld);
            Object region = getRegionMethod.invoke(regionSelector);
            Object minPoint = getMinimumPointMethod.invoke(region);
            Object maxPoint = getMaximumPointMethod.invoke(region);
            int minX = (Integer) getBlockXMethod.invoke(minPoint);
            int minY = (Integer) getBlockYMethod.invoke(minPoint);
            int minZ = (Integer) getBlockZMethod.invoke(minPoint);
            int maxX = (Integer) getBlockXMethod.invoke(maxPoint);
            int maxY = (Integer) getBlockYMethod.invoke(maxPoint);
            int maxZ = (Integer) getBlockZMethod.invoke(maxPoint);

            return new int[]{minX, minY, minZ, maxX, maxY, maxZ};
        } catch (Exception e) {
            return null;
        }
    }
}
