package it.legacynetwork.lobby.scoreboard;

public final class ScoreboardLine {
    private final String entry;
    private final String prefix;
    private final String suffix;

    public ScoreboardLine(String entry, String prefix, String suffix) {
        this.entry = entry;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public String getEntry() {
        return entry;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }
}
