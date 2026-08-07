package it.legacynetwork.chickenwars.generator;

import it.legacynetwork.chickenwars.model.ResourceType;

import java.util.Locale;

/** Definizione immutabile di una fase della timeline. */
public final class MatchPhaseDefinition {
    private final String id;
    private final int atSecond;
    private final ResourceType resource;
    private final int tier;
    private final boolean royalCollapse;
    private final String messageKey;
    private final String sound;

    public MatchPhaseDefinition(String id, int atSecond, ResourceType resource,
                                int tier, boolean royalCollapse) {
        this(id, atSecond, resource, tier, royalCollapse, null, "NOTE_PLING");
    }

    public MatchPhaseDefinition(String id, int atSecond, ResourceType resource,
                                int tier, boolean royalCollapse,
                                String messageKey, String sound) {
        if (id == null || id.trim().isEmpty() || atSecond < 0
                || (resource != null && tier <= 0)) {
            throw new IllegalArgumentException("Fase non valida");
        }
        this.id = id.trim().toUpperCase(Locale.ROOT);
        this.atSecond = atSecond;
        this.resource = resource;
        this.tier = tier;
        this.royalCollapse = royalCollapse;
        this.messageKey = messageKey == null || messageKey.trim().isEmpty()
                ? "phase." + this.id.toLowerCase(Locale.ROOT)
                : messageKey.trim();
        this.sound = sound == null || sound.trim().isEmpty()
                ? "NOTE_PLING" : sound.trim().toUpperCase(Locale.ROOT);
    }

    public String getId() { return id; }
    public int getAtSecond() { return atSecond; }
    public ResourceType getResource() { return resource; }
    public int getTier() { return tier; }
    public boolean isRoyalCollapse() { return royalCollapse; }
    public String getMessageKey() { return messageKey; }
    public String getSound() { return sound; }
}
