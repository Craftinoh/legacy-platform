package it.legacynetwork.chickenwars.mode;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Registro immutabile dei profili modalita'.
 *
 * <p>I prezzi effettivi verranno letti dai file YAML usando
 * {@link ModeProfile#getPricingProfile()}.</p>
 */
public final class ModeProfileRegistry {

    private static final ModeProfileRegistry DEFAULTS = createDefaults();

    private final Map<MatchMode, ModeProfile> profiles;

    public ModeProfileRegistry(Map<MatchMode, ModeProfile> profiles) {
        EnumMap<MatchMode, ModeProfile> copy =
                new EnumMap<MatchMode, ModeProfile>(MatchMode.class);
        if (profiles != null) {
            copy.putAll(profiles);
        }
        for (MatchMode mode : MatchMode.values()) {
            if (!copy.containsKey(mode)) {
                throw new IllegalArgumentException(
                        "Profilo mancante per " + mode.name());
            }
        }
        this.profiles = Collections.unmodifiableMap(copy);
    }

    public static ModeProfileRegistry defaults() {
        return DEFAULTS;
    }

    public ModeProfile get(MatchMode mode) {
        ModeProfile profile = profiles.get(mode);
        if (profile == null) {
            throw new IllegalArgumentException("Modalita' non registrata: " + mode);
        }
        return profile;
    }

    public ModeProfile infer(int teamCount, int playersPerTeam) {
        MatchMode inferred = MatchMode.infer(teamCount, playersPerTeam);
        return get(inferred);
    }

    public Map<MatchMode, ModeProfile> asMap() {
        return profiles;
    }

    private static ModeProfileRegistry createDefaults() {
        EnumMap<MatchMode, ModeProfile> defaults =
                new EnumMap<MatchMode, ModeProfile>(MatchMode.class);
        defaults.put(MatchMode.DUEL, new ModeProfile(
                MatchMode.DUEL, "solo_duel", 2, 1, false, false));
        defaults.put(MatchMode.SOLO, new ModeProfile(
                MatchMode.SOLO, "solo_duel", 8, 1, true, true));
        defaults.put(MatchMode.DOUBLES, new ModeProfile(
                MatchMode.DOUBLES, "doubles", 8, 2, true, true));
        defaults.put(MatchMode.TRIO, new ModeProfile(
                MatchMode.TRIO, "trio", 4, 3, true, true));
        return new ModeProfileRegistry(defaults);
    }
}
