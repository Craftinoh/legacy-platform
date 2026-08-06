package it.legacynetwork.regions.api;

import it.legacynetwork.regions.model.RegionDecision;
import it.legacynetwork.regions.model.RegionFlag;
import org.bukkit.Location;

public interface LegacyRegionsService {
    RegionDecision query(Location location, RegionFlag flag);
    boolean isAllowed(Location location, RegionFlag flag);
}
