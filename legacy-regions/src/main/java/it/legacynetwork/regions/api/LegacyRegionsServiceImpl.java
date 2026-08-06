package it.legacynetwork.regions.api;

import it.legacynetwork.regions.core.RegionResolver;
import it.legacynetwork.regions.model.RegionDecision;
import it.legacynetwork.regions.model.RegionFlag;
import org.bukkit.Location;

public final class LegacyRegionsServiceImpl implements LegacyRegionsService {

    private final RegionResolver resolver;

    public LegacyRegionsServiceImpl(RegionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public RegionDecision query(Location location, RegionFlag flag) {
        return resolver.resolve(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                flag);
    }

    @Override
    public boolean isAllowed(Location location, RegionFlag flag) {
        return resolver.isAllowed(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                flag);
    }
}
