package it.legacynetwork.chickenwars.generator;

import it.legacynetwork.chickenwars.config.GeneratorSettings;
import it.legacynetwork.chickenwars.config.GeneratorTier;
import it.legacynetwork.chickenwars.model.ResourceType;

public final class ConfiguredGeneratorSchedule implements GeneratorSchedule {
    private final GeneratorSettings settings;
    private final String profileId;
    public ConfiguredGeneratorSchedule(GeneratorSettings settings,String profileId){this.settings=settings;this.profileId=profileId;}
    @Override public GeneratorTier tier(ResourceType type,int level){return settings.getTier(type,level,profileId);}
    @Override public CatchUpPolicy catchUpPolicy(){return settings.getCatchUpPolicy();}
    @Override public int maximumCatchUpDrops(){return settings.getMaximumCatchUpDrops();}
}
