package it.legacynetwork.regions.core;

import it.legacynetwork.regions.model.CuboidRegion;
import it.legacynetwork.regions.model.FlagState;
import it.legacynetwork.regions.model.RegionDecision;
import it.legacynetwork.regions.model.RegionFlag;
import it.legacynetwork.regions.model.WorldRegionFlags;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionResolverTest {

    @Test
    void singleRegionDenyReturnsDenied() {
        CuboidRegion region = region("lobby", "world", 100,
                RegionFlag.BUILD, FlagState.DENY);
        RegionDecision decision = resolver(region).resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.BUILD);

        assertEquals(FlagState.DENY, decision.getFinalState());
        assertEquals("lobby", decision.getDecidingRegion());
    }

    @Test
    void noRegionsUsesDefaultAllow() {
        RegionDecision decision = resolver().resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.BUILD);
        assertEquals(FlagState.ALLOW, decision.getFinalState());
    }

    @Test
    void inheritFallsThroughToLowerPriorityRegion() {
        CuboidRegion inherit = region("high", "world", 100,
                RegionFlag.BUILD, FlagState.INHERIT);
        CuboidRegion deny = region("low", "world", 10,
                RegionFlag.BUILD, FlagState.DENY);

        RegionDecision decision = resolver(inherit, deny).resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.BUILD);
        assertEquals(FlagState.DENY, decision.getFinalState());
        assertEquals("low", decision.getDecidingRegion());
    }

    @Test
    void higherPriorityWins() {
        CuboidRegion low = region("low", "world", 10,
                RegionFlag.BUILD, FlagState.DENY);
        CuboidRegion high = region("high", "world", 100,
                RegionFlag.BUILD, FlagState.ALLOW);

        RegionDecision decision = resolver(low, high).resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.BUILD);
        assertEquals(FlagState.ALLOW, decision.getFinalState());
        assertEquals("high", decision.getDecidingRegion());
    }

    @Test
    void samePriorityUsesAlphabeticalId() {
        CuboidRegion alpha = region("alpha", "world", 10,
                RegionFlag.BUILD, FlagState.DENY);
        CuboidRegion beta = region("beta", "world", 10,
                RegionFlag.BUILD, FlagState.ALLOW);

        RegionDecision decision = resolver(beta, alpha).resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.BUILD);
        assertEquals(FlagState.DENY, decision.getFinalState());
        assertEquals("alpha", decision.getDecidingRegion());
    }

    @Test
    void specificFlagFallsBackToGeneralFlag() {
        CuboidRegion region = region("lobby", "world", 100,
                RegionFlag.BUILD, FlagState.DENY);

        RegionDecision decision = resolver(region).resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.BLOCK_BREAK);
        assertEquals(FlagState.DENY, decision.getFinalState());
        assertEquals("lobby", decision.getDecidingRegion());
        assertEquals(RegionFlag.BUILD, decision.getFlag());
    }

    @Test
    void explicitSpecificFlagOverridesGeneralFlag() {
        Map<RegionFlag, FlagState> flags = new HashMap<RegionFlag, FlagState>();
        flags.put(RegionFlag.BUILD, FlagState.DENY);
        flags.put(RegionFlag.BLOCK_BREAK, FlagState.ALLOW);
        CuboidRegion region = new CuboidRegion(
                "lobby", "world", "",
                0, 0, 0, 10, 10, 10, 100, flags);

        RegionDecision decision = resolver(region).resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.BLOCK_BREAK);
        assertEquals(FlagState.ALLOW, decision.getFinalState());
        assertEquals("lobby", decision.getDecidingRegion());
        assertEquals(RegionFlag.BLOCK_BREAK, decision.getFlag());
    }

    @Test
    void sameCoordinatesInOtherWorldUseDefault() {
        CuboidRegion region = region("lobby", "world", 100,
                RegionFlag.BUILD, FlagState.DENY);
        RegionDecision decision = resolver(region).resolveEffective(
                "world_nether", "", 5, 5, 5, RegionFlag.BUILD);
        assertTrue(decision.isAllowed());
    }

    @Test
    void worldFlagDenyAppliedInCorrectWorld() {
        RegionResolver resolver = resolverWithWorldFlags(
                worldFlags("world", RegionFlag.BUILD, FlagState.DENY));
        RegionDecision decision = resolver.resolveEffective(
                "world", "", 0, 0, 0, RegionFlag.BUILD);
        assertEquals(FlagState.DENY, decision.getFinalState());
        assertEquals(RegionDecision.Source.WORLD, decision.getSource());
    }

    @Test
    void samePointDifferentWorldNotAffected() {
        RegionResolver resolver = resolverWithWorldFlags(
                worldFlags("world", RegionFlag.BUILD, FlagState.DENY));
        RegionDecision decision = resolver.resolveEffective(
                "other_world", "", 0, 0, 0, RegionFlag.BUILD);
        assertEquals(FlagState.ALLOW, decision.getFinalState());
    }

    @Test
    void regionAllowOverridesWorldDeny() {
        CuboidRegion region = region("build-zone", "world", 100,
                RegionFlag.BUILD, FlagState.ALLOW);
        RegionResolver resolver = resolverWithWorldFlags(region,
                worldFlags("world", RegionFlag.BUILD, FlagState.DENY));
        RegionDecision decision = resolver.resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.BUILD);
        assertEquals(FlagState.ALLOW, decision.getFinalState());
        assertEquals(RegionDecision.Source.REGION, decision.getSource());
    }

    @Test
    void worldAllowOverriddenByRegionDeny() {
        CuboidRegion region = region("deny-zone", "world", 100,
                RegionFlag.BUILD, FlagState.DENY);
        RegionResolver resolver = resolverWithWorldFlags(region,
                worldFlags("world", RegionFlag.BUILD, FlagState.ALLOW));
        RegionDecision decision = resolver.resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.BUILD);
        assertEquals(FlagState.DENY, decision.getFinalState());
    }

    @Test
    void defaultUsedWhenWorldInherit() {
        RegionResolver resolver = resolverWithWorldFlags(
                worldFlags("world", RegionFlag.BUILD, FlagState.INHERIT));
        RegionDecision decision = resolver.resolveEffective(
                "world", "", 0, 0, 0, RegionFlag.BUILD);
        assertEquals(FlagState.ALLOW, decision.getFinalState());
    }

    @Test
    void worldFlagSpecificBlocksFallDamage() {
        RegionResolver resolver = resolverWithWorldFlags(
                worldFlags("world", RegionFlag.FALL_DAMAGE, FlagState.DENY));
        RegionDecision decision = resolver.resolveEffective(
                "world", "", 0, 0, 0, RegionFlag.FALL_DAMAGE);
        assertEquals(FlagState.DENY, decision.getFinalState());
    }

    @Test
    void worldFlagRecognizedByNameNormalized() {
        RegionResolver resolver = resolverWithWorldFlags(
                worldFlags("World", RegionFlag.BUILD, FlagState.DENY));
        RegionDecision decision = resolver.resolveEffective(
                "world", "", 0, 0, 0, RegionFlag.BUILD);
        assertEquals(FlagState.DENY, decision.getFinalState());
    }

    @Test
    void decisionSourceIndicatesCorrectOrigin() {
        CuboidRegion region = region("test-region", "world", 100,
                RegionFlag.BUILD, FlagState.ALLOW);
        RegionResolver resolver = resolverWithWorldFlags(region,
                worldFlags("world", RegionFlag.PVP, FlagState.DENY));

        RegionDecision regionDecision = resolver.resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.BUILD);
        assertEquals(RegionDecision.Source.REGION, regionDecision.getSource());

        RegionDecision worldDecision = resolver.resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.PVP);
        assertEquals(RegionDecision.Source.WORLD, worldDecision.getSource());

        RegionDecision defaultDecision = resolver.resolveEffective(
                "world", "", 5, 5, 5, RegionFlag.INTERACT);
        assertEquals(RegionDecision.Source.DEFAULT, defaultDecision.getSource());
    }

    private RegionResolver resolver(CuboidRegion... regions) {
        List<CuboidRegion> list = new ArrayList<CuboidRegion>();
        for (CuboidRegion region : regions) {
            list.add(region);
        }
        RegionIndex index = new RegionIndex();
        index.build(list);

        EnumMap<RegionFlag, FlagState> defaults =
                new EnumMap<RegionFlag, FlagState>(RegionFlag.class);
        for (RegionFlag flag : RegionFlag.values()) {
            defaults.put(flag,
                    flag.isSpecific() ? FlagState.INHERIT : FlagState.ALLOW);
        }
        return new RegionResolver(index, defaults);
    }

    private RegionResolver resolverWithWorldFlags(CuboidRegion region,
                                                    WorldRegionFlags worldFlags) {
        List<CuboidRegion> list = new ArrayList<CuboidRegion>();
        if (region != null) {
            list.add(region);
        }
        return resolverWithWorldFlagsOnly(list, worldFlags);
    }

    private RegionResolver resolverWithWorldFlags(WorldRegionFlags worldFlags) {
        return resolverWithWorldFlagsOnly(
                new ArrayList<CuboidRegion>(), worldFlags);
    }

    private RegionResolver resolverWithWorldFlagsOnly(List<CuboidRegion> regions,
                                                       WorldRegionFlags worldFlags) {
        RegionIndex index = new RegionIndex();
        index.build(regions);
        EnumMap<RegionFlag, FlagState> defaults =
                new EnumMap<RegionFlag, FlagState>(RegionFlag.class);
        for (RegionFlag flag : RegionFlag.values()) {
            defaults.put(flag,
                    flag.isSpecific() ? FlagState.INHERIT : FlagState.ALLOW);
        }
        List<WorldRegionFlags> wf = new ArrayList<WorldRegionFlags>();
        if (worldFlags != null) {
            wf.add(worldFlags);
        }
        return new RegionResolver(index, defaults, wf);
    }

    private WorldRegionFlags worldFlags(String world, RegionFlag flag,
                                         FlagState state) {
        Map<RegionFlag, FlagState> flags = new EnumMap<RegionFlag, FlagState>(
                RegionFlag.class);
        if (state != FlagState.INHERIT) {
            flags.put(flag, state);
        }
        return new WorldRegionFlags(world, "", flags);
    }

    private CuboidRegion region(String id, String world, int priority,
                                RegionFlag flag, FlagState state) {
        Map<RegionFlag, FlagState> flags = new HashMap<RegionFlag, FlagState>();
        if (state != FlagState.INHERIT) {
            flags.put(flag, state);
        }
        return new CuboidRegion(
                id, world, "",
                0, 0, 0, 10, 10, 10,
                priority, flags);
    }
}
