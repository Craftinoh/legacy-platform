package it.legacynetwork.chickenwars.generator;

import it.legacynetwork.chickenwars.config.GeneratorTier;
import it.legacynetwork.chickenwars.model.ResourceType;

public interface GeneratorSchedule {
    GeneratorTier tier(ResourceType type,int level);
    CatchUpPolicy catchUpPolicy();
    int maximumCatchUpDrops();
}
