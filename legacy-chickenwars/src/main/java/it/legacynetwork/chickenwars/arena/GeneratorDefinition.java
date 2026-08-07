package it.legacynetwork.chickenwars.arena;

import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.model.SimpleLocation;

import java.util.Locale;

/**
 * Configurazione di un generatore di risorse.
 *
 * <p>Un generatore privo di squadra proprietaria e' considerato centrale e resta
 * accessibile a chiunque.</p>
 */
public final class GeneratorDefinition {

    private final String id;
    private final ResourceType type;
    private final SimpleLocation location;
    private final String teamId;
    private final int level;
    private final boolean hologram;

    public GeneratorDefinition(String id, ResourceType type,
                               SimpleLocation location, String teamId,
                               int level, boolean hologram) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID generatore mancante");
        }
        if (type == null) {
            throw new IllegalArgumentException("Tipo generatore mancante");
        }
        if (location == null) {
            throw new IllegalArgumentException("Posizione generatore mancante");
        }
        this.id = id.trim().toLowerCase(Locale.ROOT);
        this.type = type;
        this.location = location;
        this.teamId = teamId == null || teamId.trim().isEmpty()
                ? null : teamId.trim().toLowerCase(Locale.ROOT);
        this.level = Math.max(1, level);
        this.hologram = hologram;
    }

    public String getId() {
        return id;
    }

    public ResourceType getType() {
        return type;
    }

    public SimpleLocation getLocation() {
        return location;
    }

    /**
     * @return l'ID della squadra proprietaria, oppure {@code null} se centrale
     */
    public String getTeamId() {
        return teamId;
    }

    public boolean isTeamGenerator() {
        return teamId != null;
    }

    public int getLevel() {
        return level;
    }

    public boolean hasHologram() {
        return hologram;
    }

    /**
     * Crea una copia identica con un livello differente.
     */
    public GeneratorDefinition withLevel(int newLevel) {
        return new GeneratorDefinition(id, type, location, teamId, newLevel, hologram);
    }
}
