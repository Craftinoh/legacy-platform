package it.legacynetwork.chickenwars.persistence;

/** Esito che distingue la prima scrittura da un retry idempotente. */
public final class MatchFinalizationResult {
    private final boolean applied;
    public MatchFinalizationResult(boolean applied) { this.applied = applied; }
    public boolean isApplied() { return applied; }
}
