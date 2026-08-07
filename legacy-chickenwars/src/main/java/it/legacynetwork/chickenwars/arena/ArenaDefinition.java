package it.legacynetwork.chickenwars.arena;

import it.legacynetwork.chickenwars.mode.MatchMode;
import it.legacynetwork.chickenwars.mode.ModeProfile;
import it.legacynetwork.chickenwars.mode.ModeProfileRegistry;
import it.legacynetwork.chickenwars.model.SimpleLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Configurazione completa di un'arena, come descritta nel relativo file YAML.
 *
 * <p>La definizione e' mutabile durante la fase di setup e viene consultata in
 * sola lettura durante la partita.</p>
 */
public final class ArenaDefinition {

    private final String id;
    private final Map<String, TeamDefinition> teams =
            new LinkedHashMap<String, TeamDefinition>();
    private final Map<String, GeneratorDefinition> generators =
            new LinkedHashMap<String, GeneratorDefinition>();

    private String displayName;
    private boolean enabled;
    private String world;
    private int minimumPlayers = 2;
    private int playersPerTeam = 1;
    private MatchMode mode;

    private SimpleLocation lobby;
    private SimpleLocation spectator;
    private SimpleLocation pos1;
    private SimpleLocation pos2;
    private int voidY;
    private int maximumBuildY = 256;

    public ArenaDefinition(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID arena mancante");
        }
        this.id = id.trim().toLowerCase(Locale.ROOT);
        this.displayName = this.id;
    }

    /**
     * Elenca tutto cio' che impedisce l'abilitazione dell'arena.
     *
     * <p>Il risultato alimenta sia {@code /cw admin validate} sia il controllo
     * eseguito automaticamente al caricamento.</p>
     *
     * @return descrizioni leggibili, vuote se l'arena e' completa
     */
    public List<String> findMissing() {
        List<String> missing = new ArrayList<String>();
        if (world == null || world.trim().isEmpty()) {
            missing.add("mondo dell'arena");
        }
        if (lobby == null) {
            missing.add("lobby pre-partita");
        }
        if (spectator == null) {
            missing.add("spawn spettatori");
        }
        if (pos1 == null || pos2 == null) {
            missing.add("regione principale (pos1/pos2)");
        }
        if (teams.size() < 2) {
            missing.add("almeno due squadre configurate");
        }
        for (TeamDefinition team : teams.values()) {
            missing.addAll(team.findMissing());
        }
        if (generators.isEmpty()) {
            missing.add("almeno un generatore");
        }
        return missing;
    }

    public boolean isComplete() {
        return findMissing().isEmpty();
    }

    /**
     * Indica se e' gia' stata configurata almeno una posizione.
     *
     * <p>Serve ad avvisare che un cambio di mondo rende inutilizzabili le
     * posizioni salvate, che contengono il nome del mondo precedente.</p>
     */
    public boolean hasAnyPosition() {
        if (lobby != null || spectator != null || pos1 != null || pos2 != null) {
            return true;
        }
        for (TeamDefinition team : teams.values()) {
            if (team.getSpawn() != null || team.getNest() != null
                    || team.getChicken() != null || team.getShop() != null
                    || team.getUpgrades() != null) {
                return true;
            }
        }
        return !generators.isEmpty();
    }

    /**
     * Numero massimo di giocatori, derivato dalle capienze delle squadre.
     */
    public int getMaximumPlayers() {
        int total = 0;
        for (TeamDefinition team : teams.values()) {
            total += team.getMaxPlayers();
        }
        return total;
    }

    public void addTeam(TeamDefinition team) {
        if (team == null) {
            throw new IllegalArgumentException("Squadra nulla");
        }
        teams.put(team.getId(), team);
    }

    public TeamDefinition removeTeam(String teamId) {
        if (teamId == null) {
            return null;
        }
        return teams.remove(teamId.trim().toLowerCase(Locale.ROOT));
    }

    public TeamDefinition getTeam(String teamId) {
        if (teamId == null) {
            return null;
        }
        return teams.get(teamId.trim().toLowerCase(Locale.ROOT));
    }

    public Collection<TeamDefinition> getTeams() {
        return teams.values();
    }

    public void addGenerator(GeneratorDefinition generator) {
        if (generator == null) {
            throw new IllegalArgumentException("Generatore nullo");
        }
        generators.put(generator.getId(), generator);
    }

    public GeneratorDefinition removeGenerator(String generatorId) {
        if (generatorId == null) {
            return null;
        }
        return generators.remove(generatorId.trim().toLowerCase(Locale.ROOT));
    }

    public Collection<GeneratorDefinition> getGenerators() {
        return generators.values();
    }

    /**
     * Genera un ID libero per un nuovo generatore dello stesso tipo.
     */
    public String nextGeneratorId(String prefix) {
        String base = prefix == null || prefix.trim().isEmpty()
                ? "gen" : prefix.trim().toLowerCase(Locale.ROOT);
        int index = 1;
        while (generators.containsKey(base + "_" + index)) {
            index++;
        }
        return base + "_" + index;
    }

    /**
     * Verifica se una coordinata ricade nella regione dell'arena.
     *
     * <p>Restituisce {@code false} quando la regione non e' ancora configurata,
     * cosi' che nessuna protezione venga applicata per errore.</p>
     */
    public boolean contains(String worldName, double x, double y, double z) {
        if (pos1 == null || pos2 == null || worldName == null) {
            return false;
        }
        if (!pos1.getWorld().equalsIgnoreCase(worldName)) {
            return false;
        }
        return between(x, pos1.getX(), pos2.getX())
                && between(y, pos1.getY(), pos2.getY())
                && between(z, pos1.getZ(), pos2.getZ());
    }

    private boolean between(double value, double first, double second) {
        double min = Math.min(first, second);
        double max = Math.max(first, second);
        return value >= min && value <= max + 1.0D;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        if (displayName != null && !displayName.trim().isEmpty()) {
            this.displayName = displayName.trim();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public int getMinimumPlayers() {
        return minimumPlayers;
    }

    public void setMinimumPlayers(int minimumPlayers) {
        this.minimumPlayers = Math.max(2, minimumPlayers);
    }

    public int getPlayersPerTeam() {
        return playersPerTeam;
    }

    public void setPlayersPerTeam(int playersPerTeam) {
        this.playersPerTeam = Math.max(1, playersPerTeam);
    }

    /**
     * Restituisce la modalita' esplicita oppure la deduce per le vecchie arene.
     */
    public MatchMode getMode() {
        return mode == null
                ? MatchMode.infer(teams.size(), playersPerTeam)
                : mode;
    }

    public void setMode(MatchMode mode) {
        this.mode = mode;
    }

    public ModeProfile getModeProfile() {
        return ModeProfileRegistry.defaults().get(getMode());
    }

    public SimpleLocation getLobby() {
        return lobby;
    }

    public void setLobby(SimpleLocation lobby) {
        this.lobby = lobby;
    }

    public SimpleLocation getSpectator() {
        return spectator;
    }

    public void setSpectator(SimpleLocation spectator) {
        this.spectator = spectator;
    }

    public SimpleLocation getPos1() {
        return pos1;
    }

    public void setPos1(SimpleLocation pos1) {
        this.pos1 = pos1;
    }

    public SimpleLocation getPos2() {
        return pos2;
    }

    public void setPos2(SimpleLocation pos2) {
        this.pos2 = pos2;
    }

    public int getVoidY() {
        return voidY;
    }

    public void setVoidY(int voidY) {
        this.voidY = voidY;
    }

    public int getMaximumBuildY() {
        return maximumBuildY;
    }

    public void setMaximumBuildY(int maximumBuildY) {
        this.maximumBuildY = maximumBuildY;
    }
}
