package it.legacynetwork.chickenwars.mode;

/**
 * Regole strutturali ed economiche associate a una modalita'.
 */
public final class ModeProfile {

    private final MatchMode mode;
    private final String pricingProfile;
    private final int teamCount;
    private final int playersPerTeam;
    private final boolean tracked;
    private final boolean rewardsEnabled;

    public ModeProfile(MatchMode mode, String pricingProfile, int teamCount,
                       int playersPerTeam, boolean tracked,
                       boolean rewardsEnabled) {
        if (mode == null) {
            throw new IllegalArgumentException("Modalita' mancante");
        }
        if (pricingProfile == null || pricingProfile.trim().isEmpty()) {
            throw new IllegalArgumentException("Profilo prezzi mancante");
        }
        this.mode = mode;
        this.pricingProfile = pricingProfile.trim().toLowerCase();
        this.teamCount = Math.max(2, teamCount);
        this.playersPerTeam = Math.max(1, playersPerTeam);
        this.tracked = tracked;
        this.rewardsEnabled = rewardsEnabled;
    }

    public boolean matches(int configuredTeamCount,
                           int configuredPlayersPerTeam) {
        return teamCount == configuredTeamCount
                && playersPerTeam == configuredPlayersPerTeam;
    }

    public int getMaximumPlayers() {
        return teamCount * playersPerTeam;
    }

    public MatchMode getMode() {
        return mode;
    }

    public String getPricingProfile() {
        return pricingProfile;
    }

    public int getTeamCount() {
        return teamCount;
    }

    public int getPlayersPerTeam() {
        return playersPerTeam;
    }

    public boolean isTracked() {
        return tracked;
    }

    public boolean isRewardsEnabled() {
        return rewardsEnabled;
    }
}
