package it.legacynetwork.regions.api;

import it.legacynetwork.regions.core.RegionResolver;
import it.legacynetwork.regions.model.RegionDecision;
import it.legacynetwork.regions.model.RegionFlag;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.function.Supplier;

public final class LegacyRegionsServiceImpl implements LegacyRegionsService {

    private final Supplier<RegionResolver> resolverSupplier;

    public LegacyRegionsServiceImpl(Supplier<RegionResolver> resolverSupplier) {
        this.resolverSupplier = resolverSupplier;
    }

    @Override
    public RegionDecision query(Location location, RegionFlag flag) {
        RegionResolver resolver = requireResolver();
        World world = requireWorld(location);
        return resolver.resolveEffective(
                world.getName(),
                world.getUID().toString(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                flag);
    }

    @Override
    public boolean isAllowed(Location location, RegionFlag flag) {
        return query(location, flag).isAllowed();
    }

    private RegionResolver requireResolver() {
        RegionResolver resolver = resolverSupplier.get();
        if (resolver == null) {
            throw new IllegalStateException("LegacyRegions non inizializzato");
        }
        return resolver;
    }

    private World requireWorld(Location location) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("Location o mondo mancante");
        }
        return location.getWorld();
    }
}
