package it.legacynetwork.chickenwars.game;

import it.legacynetwork.chickenwars.api.event.CWChickenDamageEvent;
import it.legacynetwork.chickenwars.api.event.CWChickenDeathEvent;
import it.legacynetwork.chickenwars.api.event.CWGameEndEvent;
import it.legacynetwork.chickenwars.api.event.CWGameStartEvent;
import it.legacynetwork.chickenwars.api.event.CWTeamEliminateEvent;
import it.legacynetwork.chickenwars.arena.ArenaDefinition;
import it.legacynetwork.chickenwars.arena.GeneratorDefinition;
import it.legacynetwork.chickenwars.arena.TeamDefinition;
import it.legacynetwork.chickenwars.chicken.ChickenSettings;
import it.legacynetwork.chickenwars.chicken.DamageOutcome;
import it.legacynetwork.chickenwars.chicken.RoyalChicken;
import it.legacynetwork.chickenwars.chicken.RoyalDamageRequest;
import it.legacynetwork.chickenwars.chicken.RoyalDamageResult;
import it.legacynetwork.chickenwars.chicken.RoyalDefeat;
import it.legacynetwork.chickenwars.config.ChickenWarsConfig;
import it.legacynetwork.chickenwars.death.DeathCause;
import it.legacynetwork.chickenwars.death.DeathContext;
import it.legacynetwork.chickenwars.death.DeathOutcome;
import it.legacynetwork.chickenwars.death.KillerEligibility;
import it.legacynetwork.chickenwars.economy.ResourceTransfer;
import it.legacynetwork.chickenwars.economy.ResourceWallet;
import it.legacynetwork.chickenwars.generator.BukkitGeneratorDropSink;
import it.legacynetwork.chickenwars.generator.ConfiguredGeneratorSchedule;
import it.legacynetwork.chickenwars.generator.GeneratorService;
import it.legacynetwork.chickenwars.generator.GeneratorState;
import it.legacynetwork.chickenwars.generator.MatchPhaseDefinition;
import it.legacynetwork.chickenwars.generator.MatchTimeline;
import it.legacynetwork.chickenwars.hologram.Hologram;
import it.legacynetwork.chickenwars.model.ArenaState;
import it.legacynetwork.chickenwars.model.ResourceType;
import it.legacynetwork.chickenwars.model.SimpleLocation;
import it.legacynetwork.chickenwars.player.InventorySnapshot;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.player.PlayerState;
import it.legacynetwork.chickenwars.persistence.MatchFinalizationRequest;
import it.legacynetwork.chickenwars.persistence.MatchFinalizationResult;
import it.legacynetwork.chickenwars.persistence.MatchParticipantRecord;
import it.legacynetwork.chickenwars.persistence.PlayerProfile;
import it.legacynetwork.chickenwars.progression.ChickenWarsProgress;
import it.legacynetwork.chickenwars.progression.MatchRewards;
import it.legacynetwork.chickenwars.scoreboard.GameScoreboard;
import it.legacynetwork.chickenwars.scoreboard.RenderedScoreboard;
import it.legacynetwork.chickenwars.scoreboard.ScoreboardLayout;
import it.legacynetwork.chickenwars.scoreboard.ScoreboardPlaceholderModel;
import it.legacynetwork.chickenwars.scoreboard.ScoreboardRenderer;
import it.legacynetwork.chickenwars.scoreboard.ScoreboardSettings;
import it.legacynetwork.chickenwars.region.CuboidRegion;
import it.legacynetwork.chickenwars.upgrade.TeamUpgradeType;
import it.legacynetwork.chickenwars.world.MapRestoreService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;

/**
 * Partita in corso su una singola arena.
 *
 * <p>Governa l'intero ciclo di vita: attesa, conto alla rovescia, gioco,
 * conclusione e ripristino. Viene fatta avanzare dal ciclo centrale del plugin e
 * non registra task propri.</p>
 */
public final class Game {

    private static final int TICKS_PER_SECOND = 20;

    private final GameServices services;
    private final ArenaDefinition definition;
    private final Map<String, GameTeam> teams = new LinkedHashMap<String, GameTeam>();
    private final Map<UUID, PlayerSession> sessions =
            new LinkedHashMap<UUID, PlayerSession>();
    private final Map<UUID, GameScoreboard> boards =
            new LinkedHashMap<UUID, GameScoreboard>();
    private final Map<UUID, PlayerSession> completedSessions =
            new LinkedHashMap<UUID, PlayerSession>();
    private final Map<UUID, FinalPlayerSnapshot> finalSnapshots =
            new LinkedHashMap<UUID, FinalPlayerSnapshot>();
    private final ScoreboardRenderer scoreboardRenderer =
            new ScoreboardRenderer();
    private final MatchOutcomeResolver outcomes = new MatchOutcomeResolver();
    private GeneratorService generators;
    private final Map<String, Hologram> generatorHolograms =
            new LinkedHashMap<String, Hologram>();
    private final Map<UUID, Villager> shopNpcs = new LinkedHashMap<UUID, Villager>();
    private final Map<UUID, Location> shopAnchors = new LinkedHashMap<UUID, Location>();
    private final Map<UUID, Villager> upgradesNpcs = new LinkedHashMap<UUID, Villager>();
    private final Map<UUID, Location> upgradesAnchors = new LinkedHashMap<UUID, Location>();
    private final MapRestoreService restore = new MapRestoreService();

    private ArenaState state = ArenaState.WAITING;
    private int tickCounter;
    private int countdownSeconds;
    private int elapsedSeconds;
    private int endingSecondsLeft;
    private GameTeam winner;
    private String matchId;
    private MatchTimeline timeline;
    private boolean royalCollapse;
    private String currentPhase;
    private MatchEndingCoordinator endingCoordinator =
            new MatchEndingCoordinator();
    private boolean finalizationAnnounced;
    private boolean victoryCheckPending;

    public Game(GameServices services, ArenaDefinition definition) {
        if (services == null || definition == null) {
            throw new IllegalArgumentException("Partita non inizializzabile");
        }
        this.services = services;
        this.definition = definition;
        this.countdownSeconds = services.getConfig().getStartingCountdownSeconds();
        this.timeline = new MatchTimeline(services.getConfig().getPhases());
        rebuildTeams();
    }

    private void rebuildTeams() {
        teams.clear();
        for (TeamDefinition team : definition.getTeams()) {
            teams.put(team.getId(), new GameTeam(team));
        }
    }

    // ------------------------------------------------------------------
    // Ingresso e uscita
    // ------------------------------------------------------------------

    /**
     * Verifica se l'arena accetta nuovi giocatori.
     */
    public boolean canJoin() {
        return definition.isEnabled() && state.isJoinable()
                && sessions.size() < definition.getMaximumPlayers();
    }

    /**
     * Inserisce un giocatore nella lobby pre-partita.
     *
     * @return {@code true} se l'ingresso e' riuscito
     */
    public boolean join(Player player) {
        if (player == null || !canJoin() || sessions.containsKey(player.getUniqueId())) {
            return false;
        }
        if (services.getProgression() != null
                && definition.getModeProfile().isTracked()
                && !services.getProgression().getProfiles()
                .mayEnterTracked(player.getUniqueId())) {
            services.getMessages().send(player,
                    "persistence.profile-unavailable");
            return false;
        }
        Location lobby = resolve(definition.getLobby());
        if (lobby == null) {
            services.getMessages().send(player, "arena.world-not-loaded");
            return false;
        }

        PlayerSession session = new PlayerSession(player.getUniqueId(),
                player.getName(), definition.getId(),
                InventorySnapshot.capture(player));
        sessions.put(player.getUniqueId(), session);
        completedSessions.remove(player.getUniqueId());

        InventorySnapshot.clear(player);
        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(lobby);

        GameScoreboard board = new GameScoreboard(
                services.getMessages().get(player, "scoreboard.title"));
        boards.put(player.getUniqueId(), board);
        board.apply(player);

        broadcast("game.player-joined",
                "{player}", player.getName(),
                "{online}", String.valueOf(sessions.size()),
                "{max}", String.valueOf(definition.getMaximumPlayers()));

        if (state == ArenaState.WAITING
                && sessions.size() >= definition.getMinimumPlayers()) {
            state = ArenaState.STARTING;
            countdownSeconds = services.getConfig().getStartingCountdownSeconds();
        }
        return true;
    }

    /** Ripristina una sessione valida sulla stessa istanza durante IN_GAME. */
    public boolean rejoin(Player player) {
        if (player == null || state != ArenaState.IN_GAME
                || sessions.containsKey(player.getUniqueId())
                || !services.getReconnects().canRestore(
                player.getUniqueId(), definition.getId())) {
            return false;
        }
        PlayerSession session = completedSessions.get(player.getUniqueId());
        if (session == null) {
            session = new PlayerSession(player.getUniqueId(), player.getName(),
                    definition.getId(), InventorySnapshot.capture(player));
        }
        it.legacynetwork.chickenwars.player.ReconnectService.Snapshot restored =
                services.getReconnects().restore(session);
        if (restored == null) {
            return false;
        }
        GameTeam team = getTeam(session.getTeamId());
        if (team == null || !team.restoreMember(player.getUniqueId())) {
            return false;
        }
        sessions.put(player.getUniqueId(), session);
        completedSessions.remove(player.getUniqueId());
        if (restored.getPlayerState() == PlayerState.RESPAWNING) {
            if (!team.canRespawn()) {
                team.eliminateMember(player.getUniqueId());
                makeSpectator(player, session);
                checkTeamElimination(team);
                checkVictory();
            } else {
                prepareReconnectRespawn(player);
            }
        } else {
            session.setState(PlayerState.PLAYING);
            preparePlayer(player, session, true);
        }
        GameScoreboard board = new GameScoreboard(
                services.getMessages().get(player, "scoreboard.title"));
        boards.put(player.getUniqueId(), board);
        board.apply(player);
        services.getMessages().send(player, "reconnect.restored");
        return true;
    }

    private void prepareReconnectRespawn(Player player) {
        InventorySnapshot.clear(player);
        player.setGameMode(GameMode.SPECTATOR);
        Location spectatorPoint = resolve(definition.getSpectator());
        if (spectatorPoint != null) player.teleport(spectatorPoint);
        services.getMessages().send(player, "respawn.countdown",
                "{seconds}", String.valueOf(getSession(player.getUniqueId())
                        .getRespawnSecondsLeft()));
    }

    /**
     * Rimuove un giocatore dalla partita e ne ripristina lo stato personale.
     *
     * @param player     giocatore da rimuovere
     * @param restoreState indica se ripristinare inventario e posizione
     */
    public void leave(Player player, boolean restoreState) {
        if (player == null) {
            return;
        }
        PlayerSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (state == ArenaState.IN_GAME || state == ArenaState.ENDING) {
            completedSessions.put(session.getPlayerId(), session);
        }

        GameTeam team = getTeam(session.getTeamId());
        boolean reconnectPending = state == ArenaState.IN_GAME
                && !restoreState && services.getConfig().isAllowRejoin()
                && services.getReconnects().hasSnapshot(player.getUniqueId());
        if (team != null && !reconnectPending) {
            team.removeMember(player.getUniqueId());
        }

        boards.remove(player.getUniqueId());
        GameScoreboard.clear(player);

        services.getTransfers().clear(player.getUniqueId());
        services.getTeamEffects().forget(player.getUniqueId());
        services.getHealPool().forget(player.getUniqueId());
        services.getBaseEntryTracker().forget(player.getUniqueId());

        if (restoreState && session.getSnapshot() != null) {
            session.getSnapshot().restore(player);
        } else if (restoreState) {
            InventorySnapshot.clear(player);
            player.setGameMode(GameMode.SURVIVAL);
        }

        broadcast("game.player-left",
                "{player}", player.getName(),
                "{online}", String.valueOf(sessions.size()),
                "{max}", String.valueOf(definition.getMaximumPlayers()));

        if (state == ArenaState.STARTING
                && sessions.size() < definition.getMinimumPlayers()) {
            state = ArenaState.WAITING;
            countdownSeconds = services.getConfig().getStartingCountdownSeconds();
            broadcast("game.countdown-cancelled");
        } else if (state == ArenaState.IN_GAME) {
            checkVictory();
        }
    }

    // ------------------------------------------------------------------
    // Ciclo di gioco
    // ------------------------------------------------------------------

    /**
     * Fa avanzare la partita di un tick di server.
     *
     * <p>Chiamato dal ciclo centrale; le operazioni costose sono distribuite su
     * frequenze diverse per contenere il carico.</p>
     */
    public void tick() {
        tickCounter++;
        switch (state) {
            case STARTING:
                if (tickCounter % TICKS_PER_SECOND == 0) {
                    tickCountdown();
                }
                break;
            case IN_GAME:
                resolvePendingVictory();
                if (state != ArenaState.IN_GAME) break;
                tickGenerators();
                if (tickCounter % TICKS_PER_SECOND == 0) {
                    tickSecond();
                }
                break;
            case ENDING:
                if (tickCounter % TICKS_PER_SECOND == 0) {
                    announceFinalization();
                    if (endingCoordinator.isSettled()
                            && --endingSecondsLeft <= 0) restart();
                }
                break;
            default:
                break;
        }

        int updateTicks = services.getConfig().getScoreboardUpdateTicks();
        if (tickCounter % updateTicks == 0) {
            updateScoreboards();
        }
    }

    private void tickCountdown() {
        countdownSeconds--;
        if (countdownSeconds <= 0) {
            start();
            return;
        }
        if (countdownSeconds <= 5 || countdownSeconds % 10 == 0) {
            broadcast("game.countdown", "{seconds}", String.valueOf(countdownSeconds));
            playSound(Sound.CLICK, 1.6F);
        }
    }

    private void tickGenerators() {
        if (generators != null) {
            generators.tick(tickCounter);
        }
    }

    private void tickSecond() {
        elapsedSeconds++;
        ChickenWarsConfig config = services.getConfig();
        timeline.reload(config.getPhases());
        for (MatchPhaseDefinition phase : timeline.poll(elapsedSeconds)) {
            applyPhase(phase);
        }
        ChickenSettings chickenSettings = config.getChicken();

        for (GameTeam team : teams.values()) {
            RoyalChicken chicken = team.getChicken();
            if (chicken == null || !chicken.isAlive()) {
                continue;
            }
            chicken.regenerate(chickenSettings);
            services.getChickens().update(chicken, team, chickenSettings);
        }

        if (elapsedSeconds % 10 == 0) {
            for (GameTeam team : teams.values()) {
                List<UUID> aliveIds = new ArrayList<UUID>();
                for (UUID member : team.getAliveMembers()) {
                    Player player = Bukkit.getPlayer(member);
                    if (player != null && player.isOnline()) {
                        aliveIds.add(member);
                    }
                }
                services.getTeamEffects().refresh(
                        definition.getId(), team.getId(), aliveIds);
            }
        }

        tickRespawns();
        expireReconnects();
        updateGeneratorHolograms();
        anchorShopNpcs();
        anchorUpgradesNpcs();

        if (elapsedSeconds >= config.getMaximumDurationSeconds()) {
            end(outcomes.atTimeout(teams.values(), config.getTimeoutPolicy())
                    .getWinner());
        }
    }

    private void applyPhase(MatchPhaseDefinition phase) {
        currentPhase = phase.getId();
        if (phase.getResource() != null && generators != null) {
            generators.setTier(phase.getResource(), phase.getTier(), tickCounter);
        }
        if (phase.isRoyalCollapse()) {
            activateRoyalCollapse();
        }
        broadcast("phase.announced", "{phase}", services.getMessages().get(
                null, phase.getMessageKey()));
        playSound(Sound.valueOf(phase.getSound()), 0.8F);
    }

    private void activateRoyalCollapse() {
        if (royalCollapse) return;
        royalCollapse = true;
        broadcast("collapse.global");
        for (GameTeam team : teams.values()) {
            team.collapse();
            for (PlayerSession session : sessions.values()) {
                if (!team.getId().equals(session.getTeamId())
                        || session.getState() != PlayerState.RESPAWNING) continue;
                team.eliminateMember(session.getPlayerId());
                Player player = Bukkit.getPlayer(session.getPlayerId());
                if (player != null && player.isOnline()) makeSpectator(player, session);
                else session.setState(PlayerState.SPECTATOR);
            }
            checkTeamElimination(team);
        }
        checkVictory();
    }

    private void tickRespawns() {
        List<PlayerSession> respawning = new ArrayList<PlayerSession>();
        for (PlayerSession session : sessions.values()) {
            if (session.getState() == PlayerState.RESPAWNING) {
                respawning.add(session);
            }
        }
        for (PlayerSession session : respawning) {
            Player player = Bukkit.getPlayer(session.getPlayerId());
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (session.tickRespawn()) {
                respawn(player, session);
            } else {
                services.getMessages().send(player, "respawn.countdown",
                        "{seconds}", String.valueOf(session.getRespawnSecondsLeft()));
            }
        }
    }

    private void expireReconnects() {
        boolean changed = false;
        for (UUID playerId : services.getReconnects().expireArena(
                definition.getId(), System.currentTimeMillis())) {
            PlayerSession session = completedSessions.get(playerId);
            GameTeam team = session == null ? null : getTeam(session.getTeamId());
            if (team != null && team.eliminateMember(playerId)) {
                changed = true;
                checkTeamElimination(team);
            }
        }
        if (changed) checkVictory();
    }

    private void updateGeneratorHolograms() {
        if (generators == null) {
            return;
        }
        for (GeneratorState generator : generators.states()) {
            Hologram hologram = generatorHolograms.get(generator.getId());
            if (hologram == null) {
                continue;
            }
            List<String> lines = new ArrayList<String>();
            lines.add(generator.getType().getColor() + ChatColor.BOLD.toString()
                    + generator.getType().getItalianName());
            lines.add(services.getMessages().get(null, "generator.countdown",
                    "{seconds}", String.valueOf(Math.max(0L,
                    (generator.getNextTick() - tickCounter + 19L) / 20L))));
            hologram.update(lines);
        }
    }

    // ------------------------------------------------------------------
    // Avvio
    // ------------------------------------------------------------------

    /**
     * Avvia immediatamente la partita, saltando il conto alla rovescia.
     *
     * @return {@code true} se l'avvio e' riuscito
     */
    public boolean start() {
        if ((state != ArenaState.WAITING && state != ArenaState.STARTING)
                || sessions.isEmpty()) {
            return false;
        }

        assignTeams();
        if (!TeamAssigner.hasEnoughOccupiedTeams(teams.values())) {
            broadcast("game.not-enough-teams");
            state = ArenaState.WAITING;
            countdownSeconds = services.getConfig().getStartingCountdownSeconds();
            return false;
        }

        state = ArenaState.IN_GAME;
        matchId = definition.getId() + ":" + UUID.randomUUID().toString();
        completedSessions.clear();
        finalSnapshots.clear();
        endingCoordinator = new MatchEndingCoordinator();
        finalizationAnnounced = false;
        victoryCheckPending = false;
        timeline = new MatchTimeline(services.getConfig().getPhases());
        royalCollapse = false;
        currentPhase = null;
        elapsedSeconds = 0;
        applyWorldRules();
        spawnChickens();
        createGenerators();
        spawnShopNpcs();
        spawnUpgradesNpcs();

        for (PlayerSession session : sessions.values()) {
            Player player = Bukkit.getPlayer(session.getPlayerId());
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (!session.hasTeam()) {
                makeSpectator(player, session);
                continue;
            }
            session.setState(PlayerState.PLAYING);
            preparePlayer(player, session, true);
        }

        broadcastList("game.started");
        playSound(Sound.LEVEL_UP, 1.0F);
        Bukkit.getPluginManager().callEvent(new CWGameStartEvent(this));
        return true;
    }

    private void assignTeams() {
        List<UUID> unassigned = new ArrayList<UUID>();
        for (PlayerSession session : sessions.values()) {
            GameTeam chosen = getTeam(session.getTeamId());
            if (chosen != null && chosen.addMember(session.getPlayerId())) {
                continue;
            }
            session.setTeamId(null);
            unassigned.add(session.getPlayerId());
        }

        List<UUID> leftovers = TeamAssigner.distribute(teams.values(), unassigned);
        for (GameTeam team : teams.values()) {
            for (UUID member : team.getMembers()) {
                PlayerSession session = sessions.get(member);
                if (session != null) {
                    session.setTeamId(team.getId());
                }
            }
        }
        for (UUID leftover : leftovers) {
            PlayerSession session = sessions.get(leftover);
            if (session != null) {
                session.setTeamId(null);
            }
        }
    }

    private void applyWorldRules() {
        World world = resolveWorld();
        if (world == null) {
            return;
        }
        ChickenWarsConfig config = services.getConfig();
        world.setGameRuleValue("doDaylightCycle",
                String.valueOf(config.isDaylightCycle()));
        world.setGameRuleValue("doFireTick", String.valueOf(config.isFireSpread()));
        world.setGameRuleValue("doMobSpawning",
                String.valueOf(config.isMobSpawning()));
        world.setGameRuleValue("mobGriefing", "false");
        world.setStorm(false);
        world.setThundering(false);
    }

    private void spawnChickens() {
        ChickenSettings settings = services.getConfig().getChicken();
        for (GameTeam team : teams.values()) {
            if (team.getMemberCount() == 0) {
                continue;
            }
            RoyalChicken chicken = services.getChickens().spawn(team, settings);
            if (chicken == null) {
                continue;
            }
            chicken.releaseProtection();
            team.setChicken(chicken);

            if (chicken.getEntity() != null) {
                services.getRoyalRegistry().register(
                        chicken.getEntity().getUniqueId(),
                        definition.getId(), team.getId());
            }
            services.getRoyalApplier().applyVitality(
                    definition.getId(), team.getId(), chicken);
        }
    }

    private void createGenerators() {
        if (generators != null) {
            generators.clear();
        }
        for (Hologram hologram : generatorHolograms.values()) {
            hologram.remove();
        }
        generatorHolograms.clear();
        Map<String, Location> locations = new LinkedHashMap<String, Location>();
        List<GeneratorDefinition> active = new ArrayList<GeneratorDefinition>();
        for (GeneratorDefinition generatorDefinition : definition.getGenerators()) {
            if (!generatorDefinition.isEnabled()) {
                continue;
            }
            if (generatorDefinition.isTeamGenerator()) {
                GameTeam owner = getTeam(generatorDefinition.getTeamId());
                if (owner == null || owner.getMemberCount() == 0) {
                    continue;
                }
            }
            Location location =
                    resolve(generatorDefinition.getLocation().centered());
            if (location == null) {
                continue;
            }
            locations.put(generatorDefinition.getId(), location);
            active.add(generatorDefinition);
            if (generatorDefinition.hasHologram()
                    && !generatorDefinition.isTeamGenerator()) {
                List<String> lines = new ArrayList<String>();
                lines.add(generatorDefinition.getType().getColor()
                        + ChatColor.BOLD.toString()
                        + generatorDefinition.getType().getItalianName());
                lines.add("");
                generatorHolograms.put(generatorDefinition.getId(), new Hologram(
                        location.clone().add(0.0D, 2.0D, 0.0D), lines));
            }
        }
        generators = new GeneratorService(new ConfiguredGeneratorSchedule(
                services.getConfig().getGenerators(), definition.getModeProfile()
                .getPricingProfile()),
                new BukkitGeneratorDropSink(
                        services.getConfig().getGenerators(), locations,
                        services.getGeneratedResources()));
        for (GeneratorDefinition generator : active) {
            generators.add(new GeneratorState(matchId, generator));
        }
        generators.start(tickCounter);
    }

    /**
     * Crea un venditore per ogni squadra in gioco.
     *
     * <p>Gli NPC sono villager normali: vengono riancorati ogni secondo alla
     * posizione configurata, evitando cosi' il ricorso a NMS per bloccarne
     * l'intelligenza artificiale.</p>
     */
    private void spawnShopNpcs() {
        removeShopNpcs();
        for (GameTeam team : teams.values()) {
            if (team.getMemberCount() == 0) {
                continue;
            }
            Location location = resolve(team.getDefinition().getShop());
            if (location == null || location.getWorld() == null) {
                continue;
            }
            Villager villager = location.getWorld().spawn(location, Villager.class);
            villager.setProfession(Villager.Profession.FARMER);
            villager.setAdult();
            villager.setRemoveWhenFarAway(false);
            villager.setCanPickupItems(false);
            villager.setCustomName(services.getMessages().get(null, "shop.npc-name"));
            villager.setCustomNameVisible(true);
            shopNpcs.put(villager.getUniqueId(), villager);
            shopAnchors.put(villager.getUniqueId(), location.clone());
        }
    }

    /**
     * Riporta i venditori alla posizione configurata quando si spostano.
     */
    private void anchorShopNpcs() {
        for (Villager villager : shopNpcs.values()) {
            if (villager == null || !villager.isValid()) {
                continue;
            }
            Location anchor = shopAnchors.get(villager.getUniqueId());
            if (anchor == null) {
                continue;
            }
            if (villager.getLocation().getWorld() != anchor.getWorld()
                    || villager.getLocation().distanceSquared(anchor) > 1.0D) {
                villager.teleport(anchor);
            }
        }
    }

    private void anchorUpgradesNpcs() {
        for (Villager villager : upgradesNpcs.values()) {
            if (villager == null || !villager.isValid()) {
                continue;
            }
            Location anchor = upgradesAnchors.get(villager.getUniqueId());
            if (anchor == null) {
                continue;
            }
            if (villager.getLocation().getWorld() != anchor.getWorld()
                    || villager.getLocation().distanceSquared(anchor) > 1.0D) {
                villager.teleport(anchor);
            }
        }
    }

    private void removeShopNpcs() {
        for (Villager villager : shopNpcs.values()) {
            if (villager != null && villager.isValid()) {
                villager.remove();
            }
        }
        shopNpcs.clear();
        shopAnchors.clear();
    }

    private void spawnUpgradesNpcs() {
        removeUpgradesNpcs();
        for (GameTeam team : teams.values()) {
            if (team.getMemberCount() == 0) {
                continue;
            }
            Location location = resolve(team.getDefinition().getUpgrades());
            if (location == null || location.getWorld() == null) {
                continue;
            }
            Villager villager = location.getWorld().spawn(location, Villager.class);
            villager.setProfession(Villager.Profession.PRIEST);
            villager.setAdult();
            villager.setRemoveWhenFarAway(false);
            villager.setCanPickupItems(false);
            villager.setCustomName(services.getMessages().get(null,
                    "chicken.menu.npc-name"));
            villager.setCustomNameVisible(true);
            upgradesNpcs.put(villager.getUniqueId(), villager);
            upgradesAnchors.put(villager.getUniqueId(), location.clone());
        }
    }

    private void removeUpgradesNpcs() {
        for (Villager villager : upgradesNpcs.values()) {
            if (villager != null && villager.isValid()) {
                villager.remove();
            }
        }
        upgradesNpcs.clear();
        upgradesAnchors.clear();
    }

    public boolean isUpgradesNpc(Entity entity) {
        return entity != null && upgradesNpcs.containsKey(entity.getUniqueId());
    }

    /**
     * Indica se l'entita' e' un venditore di questa partita.
     */
    public boolean isShopNpc(Entity entity) {
        return entity != null && shopNpcs.containsKey(entity.getUniqueId());
    }

    // ------------------------------------------------------------------
    // Giocatori
    // ------------------------------------------------------------------

    /**
     * Prepara il giocatore per il gioco attivo: posizione, kit e protezione.
     *
     * @param initial indica se si tratta dell'inizio partita e non di un respawn
     */
    private void preparePlayer(Player player, PlayerSession session,
                               boolean initial) {
        GameTeam team = getTeam(session.getTeamId());
        if (team == null) {
            makeSpectator(player, session);
            return;
        }
        Location spawn = resolve(team.getDefinition().getSpawn());
        if (spawn == null) {
            makeSpectator(player, session);
            return;
        }

        InventorySnapshot.clear(player);
        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(spawn);

        services.getEquipment().applyLoadout(player, session,
                team.getColor());

        int protection = services.getConfig().getSpawnProtectionSeconds();
        session.grantInvulnerability(protection);
        session.clearDamager();
        session.completeDeath();

        deliverPendingRewards(player);

        services.getTeamEffects().apply(definition.getId(), team.getId(),
                player.getUniqueId(), true);

        if (!initial) {
            services.getMessages().send(player, "respawn.done");
        }
    }

    /**
     * Applica subito a tutti i membri vivi l'upgrade di squadra appena
     * acquistato, usando lo stato autorevole gia' aggiornato.
     */
    public void applyTeamUpgrade(GameTeam team, TeamUpgradeType type) {
        if (team == null || type == null || state != ArenaState.IN_GAME) {
            return;
        }
        String arenaId = definition.getId();
        CuboidRegion base = team.getDefinition().getBaseRegion();
        for (UUID memberId : team.getAliveMembers()) {
            Player player = Bukkit.getPlayer(memberId);
            PlayerSession session = sessions.get(memberId);
            if (player == null || !player.isOnline() || session == null
                    || !session.getState().isActive()) {
                continue;
            }
            if (type == TeamUpgradeType.PROTECTION) {
                services.getEquipment().applyArmor(player, session,
                        team.getColor());
            } else if (type == TeamUpgradeType.SHARPNESS) {
                services.getEquipment().applySword(player, session);
            } else if (type == TeamUpgradeType.HASTE) {
                services.getTeamEffects().apply(arenaId, team.getId(),
                        memberId, true);
            } else if (type == TeamUpgradeType.HEAL_POOL) {
                Location location = player.getLocation();
                boolean inside = base != null && location != null
                        && location.getWorld() != null
                        && base.contains(location.getWorld().getName(),
                        location.getX(), location.getY(), location.getZ());
                services.getHealPool().update(arenaId, team.getId(), memberId,
                        inside, inside);
            }
            player.updateInventory();
        }
    }

    /**
     * Consegna la coda premio accumulata quando l'inventario era pieno.
     */
    private void deliverPendingRewards(Player player) {
        Map<ResourceType, Integer> delivered =
                services.getTransfers().flushQueue(player);
        if (delivered.isEmpty()) {
            return;
        }
        int total = 0;
        for (Integer amount : delivered.values()) {
            total += amount.intValue();
        }
        services.getMessages().send(player, "resources.queue-delivered",
                "{amount}", String.valueOf(total));
    }

    /**
     * Gestisce la morte di un giocatore, distinguendo morte temporanea e finale.
     *
     * @param player  giocatore morto
     * @param killer  autore dell'uccisione, eventualmente nullo
     */
    public void handleDeath(Player player, Player killer) {
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session == null || state != ArenaState.IN_GAME) {
            return;
        }
        // Un giocatore gia' morto non muore una seconda volta: e' la prima
        // barriera contro gli eventi duplicati riferiti alla stessa morte.
        if (!session.getState().isActive()) {
            return;
        }
        GameTeam team = getTeam(session.getTeamId());
        if (team == null) {
            return;
        }

        // Unico orchestratore: sequenza, trasferimento risorse e downgrade
        // passano di qui esattamente come per l'abbandono in combattimento.
        DeathOutcome outcome = services.getDeaths().processPlayers(
                DeathContext.of(player.getUniqueId(),
                        killer == null ? null : killer.getUniqueId(),
                        causeOf(killer)),
                session, player, killer, killerEligibility());
        if (!outcome.isProcessed()) {
            return;
        }
        announceTransfer(player, killer, outcome);

        InventorySnapshot.clear(player);

        boolean finalDeath = !team.canRespawn()
                || !services.getConfig().isRespawnEnabled();

        if (killer != null && !killer.getUniqueId().equals(player.getUniqueId())) {
            PlayerSession killerSession = sessions.get(killer.getUniqueId());
            if (killerSession != null) {
                if (finalDeath) {
                    killerSession.addFinalKill();
                } else {
                    killerSession.addKill();
                }
            }
        }

        String killerName = killer == null
                ? services.getMessages().get(null, "death.unknown-killer")
                : killer.getName();

        if (finalDeath) {
            team.eliminateMember(player.getUniqueId());
            makeSpectator(player, session);
            broadcast("death.final",
                    "{player}", team.getColor().getChatColor() + player.getName(),
                    "{killer}", killerName);
            checkTeamElimination(team);
            checkVictory();
            return;
        }

        session.setState(PlayerState.RESPAWNING);
        session.setRespawnSecondsLeft(services.getConfig().getRespawnSeconds());
        player.setGameMode(GameMode.SPECTATOR);
        Location spectatorPoint = resolve(definition.getSpectator());
        if (spectatorPoint != null) {
            player.teleport(spectatorPoint);
        }
        broadcast("death.normal",
                "{player}", team.getColor().getChatColor() + player.getName(),
                "{killer}", killerName);
        services.getMessages().send(player, "respawn.countdown",
                "{seconds}", String.valueOf(session.getRespawnSecondsLeft()));
    }

    /**
     * Elabora l'abbandono in combattimento come una morte a tutti gli effetti.
     *
     * <p>Percorre lo stesso orchestratore della morte normale, quindi risorse,
     * downgrade e reset della spada avvengono una sola volta.</p>
     *
     * @param player giocatore che si sta disconnettendo
     * @return {@code true} se il logout e' stato convertito in morte
     */
    public boolean handleCombatLogout(Player player) {
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session == null || state != ArenaState.IN_GAME
                || !session.getState().isActive()) {
            return false;
        }

        UUID attackerId = session.getValidDamager(
                services.getConfig().getVoidKillCreditSeconds());
        if (attackerId == null) {
            // Fuori dal combattimento l'uscita non produce alcuna morte.
            return false;
        }

        Player killer = Bukkit.getPlayer(attackerId);
        DeathOutcome outcome = services.getDeaths().processPlayers(
                DeathContext.combatLogout(player.getUniqueId(), attackerId),
                session, player, killer, killerEligibility());
        if (!outcome.isProcessed()) {
            return false;
        }
        announceTransfer(player, killer, outcome);

        GameTeam team = getTeam(session.getTeamId());
        if (team != null) {
            PlayerSession killerSession = sessions.get(attackerId);
            if (killerSession != null
                    && !attackerId.equals(player.getUniqueId())) {
                killerSession.addFinalKill();
            }
            team.eliminateMember(player.getUniqueId());
            broadcast("death.combat-logout",
                    "{player}", team.getColor().getChatColor() + player.getName(),
                    "{killer}", killer == null
                            ? services.getMessages().get(null,
                            "death.unknown-killer") : killer.getName());
            checkTeamElimination(team);
        }
        return true;
    }

    /**
     * Deduce la causa da comunicare all'orchestratore.
     */
    private DeathCause causeOf(Player killer) {
        return killer == null ? DeathCause.ENVIRONMENT : DeathCause.COMBAT;
    }

    /**
     * Validita' dell'uccisore secondo lo stato reale della partita.
     */
    private KillerEligibility killerEligibility() {
        return new KillerEligibility() {
            @Override
            public boolean isEligible(UUID killerId) {
                if (killerId == null || state != ArenaState.IN_GAME) {
                    return false;
                }
                Player killer = Bukkit.getPlayer(killerId);
                if (killer == null || !killer.isOnline()) {
                    return false;
                }
                PlayerSession killerSession = sessions.get(killerId);
                return killerSession != null
                        && killerSession.getState().isActive();
            }
        };
    }

    /**
     * Comunica all'uccisore le risorse ottenute dalla vittima.
     */
    private void announceTransfer(Player victim, Player killer,
                                  DeathOutcome outcome) {
        ResourceTransfer transfer = outcome.getTransfer();
        if (transfer.isEmpty() || killer == null || !killer.isOnline()) {
            return;
        }
        services.getMessages().send(killer, "resources.stolen",
                "{victim}", victim.getName(),
                "{resources}", transfer.describe(ChatColor.GRAY + ", "));
        if (transfer.hasQueued()) {
            services.getMessages().send(killer, "resources.queued");
        }
    }

    private void respawn(Player player, PlayerSession session) {
        GameTeam team = getTeam(session.getTeamId());
        if (team == null || !team.canRespawn()) {
            makeSpectator(player, session);
            checkVictory();
            return;
        }
        session.setState(PlayerState.PLAYING);
        preparePlayer(player, session, false);
    }

    private void makeSpectator(Player player, PlayerSession session) {
        session.setState(PlayerState.SPECTATOR);
        session.clearInvulnerability();
        session.completeDeath();
        InventorySnapshot.clear(player);
        player.setGameMode(GameMode.SPECTATOR);
        Location spectatorPoint = resolve(definition.getSpectator());
        if (spectatorPoint != null) {
            player.teleport(spectatorPoint);
        }
    }

    // ------------------------------------------------------------------
    // Gallina Reale
    // ------------------------------------------------------------------

    /**
     * Applica danno alla Gallina Reale di una squadra.
     *
     * <p>Verifica squadra di appartenenza dell'aggressore, lancia l'evento
     * annullabile e, in caso di morte, avvia l'intera sequenza conseguente.</p>
     *
     * @param owner    squadra proprietaria della gallina
     * @param attacker autore del colpo, eventualmente nullo
     * @param amount   danno richiesto
     * @return {@code true} se il danno e' stato applicato
     */
    public boolean damageChicken(GameTeam owner, Player attacker, double amount) {
        if (state != ArenaState.IN_GAME || owner == null) {
            return false;
        }
        RoyalChicken chicken = owner.getChicken();
        if (chicken == null || !chicken.isAlive()) {
            return false;
        }

        UUID attackerId = attacker == null ? null : attacker.getUniqueId();
        String attackerTeamId = null;
        boolean attackerPlaying = false;
        if (attacker != null) {
            PlayerSession attackerSession = sessions.get(attacker.getUniqueId());
            if (attackerSession != null && attackerSession.getState().isActive()) {
                attackerTeamId = attackerSession.getTeamId();
                attackerPlaying = true;
            }
        }

        CWChickenDamageEvent event = new CWChickenDamageEvent(this, owner, chicken,
                attacker, amount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getDamage() <= 0.0D) {
            return false;
        }

        double reduction = services.getRoyalApplier().resolveArmorReduction(
                definition.getId(), owner.getId());
        RoyalDamageRequest request = RoyalDamageRequest.builder()
                .attacker(attackerId, attackerTeamId)
                .owner(owner.getId())
                .gameRunning(state == ArenaState.IN_GAME)
                .attackerPlaying(attackerPlaying)
                .rawDamage(event.getDamage())
                .damageReduction(reduction)
                .build();

        RoyalDamageResult result = services.getRoyalDamage().damage(chicken, request);
        if (result.isIgnored()) {
            return false;
        }

        services.getChickens().playDamageEffect(chicken);
        ChickenSettings settings = services.getConfig().getChicken();
        services.getChickens().update(chicken, owner, settings);

        if (result.isDefeated()) {
            handleChickenDefeat(owner, chicken, attacker);
            return true;
        }

        if (settings.isAlertsEnabled()
                && chicken.tryAlert(settings.getAlertCooldownMillis())) {
            notifyTeam(owner, "chicken.under-attack",
                    "{health}", String.valueOf(chicken.getVitals().getDisplayHealth()),
                    "{max_health}",
                    String.valueOf((int) chicken.getVitals().getMaxHealth()));
        }
        if (attacker != null) {
            services.getMessages().send(attacker, "chicken.damaged",
                    "{team}", owner.getColoredName(),
                    "{health}", String.valueOf(chicken.getVitals().getDisplayHealth()),
                    "{max_health}",
                    String.valueOf((int) chicken.getVitals().getMaxHealth()));
        }
        return true;
    }

    /**
     * Nutre la Gallina Reale della propria squadra.
     *
     * <p>Il mangime cura vita e scudo secondo la configurazione; il pasto viene
     * rifiutato se la gallina e' gia' al massimo oppure se il cooldown non e'
     * ancora trascorso.</p>
     *
     * @param owner  squadra proprietaria della gallina
     * @param feeder giocatore che offre il mangime
     * @return {@code true} se il mangime e' stato consumato
     */
    public boolean feedChicken(GameTeam owner, Player feeder) {
        ChickenSettings settings = services.getConfig().getChicken();
        if (!settings.isFeedingEnabled() || owner == null || feeder == null
                || state != ArenaState.IN_GAME) {
            return false;
        }
        if (!owner.isMember(feeder.getUniqueId())) {
            services.getMessages().send(feeder, "chicken.feed-other-team");
            return false;
        }

        RoyalChicken chicken = owner.getChicken();
        if (chicken == null || !chicken.isAlive()) {
            return false;
        }
        if (chicken.getVitals().isFullHealth() && chicken.getVitals().isFullShield()) {
            services.getMessages().send(feeder, "chicken.feed-full");
            return false;
        }
        if (!chicken.tryFeed(settings.getFeedCooldownMillis())) {
            return false;
        }

        double healed = chicken.heal(settings.getFeedHealAmount());
        double shielded = chicken.restoreShield(settings.getFeedShieldAmount());
        services.getChickens().update(chicken, owner, settings);

        feeder.playSound(feeder.getLocation(), Sound.CHICKEN_IDLE, 1.0F, 1.4F);
        services.getMessages().send(feeder, "chicken.fed",
                "{health}", String.valueOf((int) Math.ceil(healed)),
                "{shield}", String.valueOf((int) Math.ceil(shielded)),
                "{current}", String.valueOf(chicken.getVitals().getDisplayHealth()),
                "{max}", String.valueOf((int) chicken.getVitals().getMaxHealth()));
        return true;
    }

    private void handleChickenDefeat(GameTeam owner, RoyalChicken chicken,
                                     Player killer) {
        if (!chicken.markDefeated()) {
            return;
        }
        owner.collapse();
        broadcast("collapse.team", "{team}", owner.getColoredName());
        notifyTeam(owner, "collapse.respawn-disabled");

        // L'UUID va letto prima di playDeath: quel metodo rimuove l'entita' e
        // azzera il riferimento, quindi dopo non sarebbe piu' recuperabile ne'
        // per deregistrare la voce ne' per descrivere la sconfitta.
        UUID entityId = chicken.getEntity() == null
                ? null : chicken.getEntity().getUniqueId();

        ChickenSettings settings = services.getConfig().getChicken();
        services.getChickens().playDeath(chicken, settings);

        if (entityId != null) {
            services.getRoyalRegistry().unregister(entityId);
        }

        RoyalDefeat defeat = new RoyalDefeat(definition.getId(), owner.getId(),
                entityId, killer == null ? null : killer.getUniqueId(),
                System.currentTimeMillis());
        services.getRoyalDefeatDispatcher().dispatch(defeat);

        if (killer != null) {
            PlayerSession killerSession = sessions.get(killer.getUniqueId());
            if (killerSession != null) {
                killerSession.addChickenKill();
            }
        }

        broadcastList("chicken.destroyed",
                "{team}", owner.getColoredName(),
                "{killer}", killer == null
                        ? services.getMessages().get(null, "death.unknown-killer")
                        : killer.getName());
        playSound(Sound.ENDERDRAGON_GROWL, 1.0F);

        Bukkit.getPluginManager().callEvent(
                new CWChickenDeathEvent(this, owner, chicken, killer));

        if (settings.isLastFeatherEnabled()) {
            applyLastFeather(owner, settings);
        }

        for (PlayerSession session : sessions.values()) {
            if (!owner.getId().equals(session.getTeamId())
                    || session.getState() != PlayerState.RESPAWNING) continue;
            owner.eliminateMember(session.getPlayerId());
            Player respawning = Bukkit.getPlayer(session.getPlayerId());
            if (respawning != null && respawning.isOnline()) {
                makeSpectator(respawning, session);
            } else {
                session.setState(PlayerState.SPECTATOR);
            }
        }

        checkTeamElimination(owner);
        checkVictory();
    }

    /**
     * Concede il bonus Ultima Piuma ai superstiti della squadra colpita.
     */
    private void applyLastFeather(GameTeam owner, ChickenSettings settings) {
        List<Player> survivors = getTeamPlayers(owner);
        if (survivors.isEmpty()) {
            return;
        }
        int duration = settings.getLastFeatherDurationSeconds();
        for (Player player : survivors) {
            for (String raw : settings.getLastFeatherEffects()) {
                PotionEffect effect = parseEffect(raw, duration);
                if (effect != null) {
                    player.addPotionEffect(effect, true);
                }
            }
            services.getMessages().send(player, "chicken.last-feather",
                    "{seconds}", String.valueOf(duration));
        }
    }

    /**
     * Interpreta la forma {@code TIPO:livello} usata dagli effetti Ultima Piuma.
     *
     * @return l'effetto, oppure {@code null} se il tipo non esiste
     */
    private PotionEffect parseEffect(String raw, int seconds) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split(":");
        PotionEffectType type =
                PotionEffectType.getByName(parts[0].trim().toUpperCase(Locale.ROOT));
        if (type == null) {
            return null;
        }
        int amplifier = 0;
        if (parts.length > 1) {
            try {
                amplifier = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException exception) {
                amplifier = 0;
            }
        }
        return new PotionEffect(type, seconds * TICKS_PER_SECOND,
                Math.max(0, amplifier));
    }

    // ------------------------------------------------------------------
    // Conclusione
    // ------------------------------------------------------------------

    private void checkTeamElimination(GameTeam team) {
        if (team.isEliminated() || team.getAliveCount() > 0) {
            return;
        }
        team.setEliminated(true);
        broadcast("team.eliminated", "{team}", team.getColoredName());
        Bukkit.getPluginManager().callEvent(new CWTeamEliminateEvent(this, team));
    }

    /**
     * Conclude la partita se resta una sola squadra in gioco.
     */
    public void checkVictory() {
        if (state != ArenaState.IN_GAME) {
            return;
        }
        victoryCheckPending = true;
    }

    private void resolvePendingVictory() {
        if (!victoryCheckPending || state != ArenaState.IN_GAME) return;
        victoryCheckPending = false;
        MatchOutcomeResolver.Result result =
                outcomes.afterElimination(teams.values());
        if (result.isTerminal()) {
            end(result.getWinner());
        }
    }

    /**
     * Termina la partita e avvia la fase di riepilogo.
     *
     * @param winningTeam squadra vincitrice, eventualmente nulla
     */
    public void end(GameTeam winningTeam) {
        if (state != ArenaState.IN_GAME) {
            return;
        }
        state = ArenaState.ENDING;
        if (generators != null) generators.stop();
        this.winner = winningTeam;
        this.endingSecondsLeft = services.getConfig().getEndingSeconds();

        if (winningTeam != null) {
            broadcastList("game.victory", "{team}", winningTeam.getColoredName());
            playSound(Sound.LEVEL_UP, 1.0F);
        } else {
            broadcast("game.draw");
        }

        for (PlayerSession session : sessions.values()) {
            Player player = Bukkit.getPlayer(session.getPlayerId());
            if (player == null || !player.isOnline()) {
                continue;
            }
            services.getMessages().send(player, "game.summary",
                    "{kills}", String.valueOf(session.getKills()),
                    "{final_kills}", String.valueOf(session.getFinalKills()),
                    "{chickens}", String.valueOf(session.getChickensKilled()),
                    "{deaths}", String.valueOf(session.getDeaths()));
            player.closeInventory();
            player.setGameMode(GameMode.SPECTATOR);
            session.clearInvulnerability();
        }

        endingCoordinator.start(finalizeMatch(winningTeam));

        Bukkit.getPluginManager().callEvent(new CWGameEndEvent(this, winningTeam));
    }

    private CompletionStage<MatchFinalizationResult> finalizeMatch(
            GameTeam winningTeam) {
        if (matchId == null) {
            return CompletableFuture.completedFuture(
                    new MatchFinalizationResult(false));
        }
        Map<UUID, PlayerSession> participants =
                new LinkedHashMap<UUID, PlayerSession>(completedSessions);
        participants.putAll(sessions);
        List<MatchParticipantRecord> records =
                new ArrayList<MatchParticipantRecord>();
        finalSnapshots.clear();
        for (PlayerSession session : participants.values()) {
            boolean won = winningTeam != null
                    && winningTeam.getId().equals(session.getTeamId());
            MatchRewards rewards = definition.getModeProfile().isRewardsEnabled()
                    ? services.getProgression().getRewards().calculate(won,
                    session.getKills(), session.getFinalKills(),
                    session.getResourcesCollected()) : new MatchRewards(0L, 0L);
            finalSnapshots.put(session.getPlayerId(), snapshot(session, rewards));
            records.add(new MatchParticipantRecord(session.getPlayerId(),
                    session.getTeamId(), won, rewards.getExperience(),
                    rewards.getCoins(), session.getKills(),
                    session.getFinalKills(), session.getDeaths(),
                    session.getFinalKills(), session.getResourcesCollected(),
                    elapsedSeconds));
        }
        final String finalizedMatchId = matchId;
        MatchFinalizationRequest request = new MatchFinalizationRequest(finalizedMatchId,
                definition.getMode(), winningTeam == null ? null
                : winningTeam.getId(), records, System.currentTimeMillis(),
                services.getProgression().getExperience().getMaximumExperience());
        CompletionStage<MatchFinalizationResult> finalization =
                services.getProgression().getFinalizer().finalizeMatch(request);
        finalization.whenComplete((result, failure) -> {
                    if (failure != null) {
                        services.getPlugin().getLogger().log(Level.SEVERE,
                                "Finalizzazione partita fallita: " + finalizedMatchId,
                                failure);
                    } else {
                        services.getProgression().applyFinalized(request);
                    }
                });
        return finalization;
    }

    private void announceFinalization() {
        if (finalizationAnnounced || !endingCoordinator.isSettled()) return;
        finalizationAnnounced = true;
        if (!endingCoordinator.isSuccessful()) {
            broadcast("persistence.finalization-failed");
            return;
        }
        if (!definition.getModeProfile().isRewardsEnabled()) return;
        for (Player player : getOnlinePlayers()) {
            FinalPlayerSnapshot snapshot = finalSnapshots.get(
                    player.getUniqueId());
            if (snapshot != null) {
                services.getMessages().send(player, "progression.rewards",
                        "{experience}", String.valueOf(snapshot.getExperience()),
                        "{coins}", String.valueOf(snapshot.getCoins()));
            }
        }
    }

    private FinalPlayerSnapshot snapshot(PlayerSession session,
                                         MatchRewards rewards) {
        int level = 0;
        PlayerProfile profile = services.getProgression().getProfiles()
                .get(session.getPlayerId());
        if (profile != null) {
            ChickenWarsProgress progress = profile.getProgress().toProgress(
                    services.getProgression().getExperience());
            progress.addExperience(rewards.getExperience());
            level = progress.getLevel();
        }
        return new FinalPlayerSnapshot(session.getPlayerId(), session.getKills(),
                session.getFinalKills(), session.getChickensKilled(),
                session.getDeaths(), session.getResourcesCollected(),
                rewards.getExperience(), rewards.getCoins(), level);
    }

    /**
     * Rimuove tutti i giocatori, ripristina la mappa e riapre l'arena.
     */
    public void restart() {
        state = ArenaState.RESTARTING;

        for (PlayerSession session :
                new ArrayList<PlayerSession>(sessions.values())) {
            Player player = Bukkit.getPlayer(session.getPlayerId());
            if (player != null && player.isOnline()) {
                leave(player, true);
                sendToReturnLobby(player);
            }
        }
        cleanup();
        sessions.clear();
        completedSessions.clear();
        finalSnapshots.clear();
        boards.clear();
        restore.restore(definition);
        rebuildTeams();

        elapsedSeconds = 0;
        winner = null;
        matchId = null;
        royalCollapse = false;
        currentPhase = null;
        timeline = new MatchTimeline(services.getConfig().getPhases());
        endingCoordinator = new MatchEndingCoordinator();
        finalizationAnnounced = false;
        victoryCheckPending = false;
        countdownSeconds = services.getConfig().getStartingCountdownSeconds();
        state = definition.isEnabled() ? ArenaState.WAITING : ArenaState.DISABLED;
    }

    private void sendToReturnLobby(Player player) {
        if (services.getReturnLobby().transfer(player)) {
            services.getMessages().send(player, "ending.returning-lobby");
            return;
        }
        Location target = resolve(services.getConfig().getReturnLobby());
        if (target != null) {
            player.teleport(target);
        }
    }

    /**
     * Rimuove galline, ologrammi, generatori ed entita' residue.
     *
     * <p>Va richiamato anche allo spegnimento del plugin.</p>
     */
    public void cleanup() {
        for (UUID playerId : completedSessions.keySet()) {
            services.getReconnects().forget(playerId);
            services.getRouting().forget(playerId);
            services.getTransfers().clear(playerId);
        }
        for (GameTeam team : teams.values()) {
            RoyalChicken chicken = team.getChicken();
            if (chicken != null) {
                services.getChickens().remove(chicken);
                team.setChicken(null);
            }
        }
        if (generators != null) {
            generators.clear();
            generators = null;
        }
        for (Hologram hologram : generatorHolograms.values()) {
            hologram.remove();
        }
        generatorHolograms.clear();
        if (matchId != null) {
            services.getGeneratedResources().clearMatch(matchId);
        }
        removeShopNpcs();
        removeUpgradesNpcs();
        services.getUpgrades().clearArena(definition.getId());
        services.getRoyalRegistry().clearArena(definition.getId());
        services.getTeamEffects().clearArena(definition.getId());
        services.getHealPool().clearArena(definition.getId());
        services.getBaseEntryTracker().clearArena(definition.getId());
        restore.clearEntities(definition);
    }

    // ------------------------------------------------------------------
    // Scoreboard e messaggistica
    // ------------------------------------------------------------------

    private void updateScoreboards() {
        ScoreboardSettings settings = services.getConfig().getScoreboard();
        if (!settings.isEnabled()) {
            for (UUID playerId : boards.keySet()) {
                GameScoreboard.clear(Bukkit.getPlayer(playerId));
            }
            return;
        }
        for (Map.Entry<UUID, GameScoreboard> entry : boards.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                continue;
            }
            ScoreboardLayout layout = settings.getLayout(scoreboardLayout(player));
            if (layout == null) {
                continue;
            }
            RenderedScoreboard rendered = scoreboardRenderer.render(layout,
                    scoreboardModel(player, settings));
            entry.getValue().update(rendered.getTitle(), rendered.getLines());
        }
    }

    private String scoreboardLayout(Player player) {
        if (state == ArenaState.WAITING) return "waiting";
        if (state == ArenaState.STARTING) return "starting";
        if (state == ArenaState.ENDING) {
            return definition.getModeProfile().isRewardsEnabled()
                    ? "ending" : "ending-duel";
        }
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session != null && session.getState() == PlayerState.SPECTATOR) {
            return "spectator";
        }
        if (!definition.getModeProfile().isTracked()) return "duel";
        return definition.getModeProfile().getTeamCount() == 8
                ? "playing-eight-teams" : "playing-compact";
    }

    private ScoreboardPlaceholderModel scoreboardModel(Player player,
                                                       ScoreboardSettings settings) {
        PlayerSession session = sessions.get(player.getUniqueId());
        FinalPlayerSnapshot ended = finalSnapshots.get(player.getUniqueId());
        ScoreboardPlaceholderModel model = new ScoreboardPlaceholderModel()
                .value("date", new java.text.SimpleDateFormat("dd/MM/yyyy")
                        .format(new java.util.Date()))
                .value("map", definition.getDisplayName())
                .value("mode", definition.getMode().name())
                .value("players", sessions.size())
                .value("max_players", definition.getMaximumPlayers())
                .value("server_id", services.getConfig().getServerId())
                .value("footer", settings.getFooter())
                .value("waiting_status", message(player, "scoreboard.waiting",
                        "{online}", String.valueOf(sessions.size()), "{min}",
                        String.valueOf(definition.getMinimumPlayers())))
                .value("starting_status", message(player, "scoreboard.starting",
                        "{seconds}", String.valueOf(countdownSeconds)))
                .value("training_notice", message(player,
                        "scoreboard.training-notice"))
                .value("winner_team", winner == null
                        ? message(player, "game.draw") : winner.getColoredName())
                .value("duration", formatTime(elapsedSeconds));
        addLabels(model, player);
        addNextPhase(model, player);
        addTeamPlaceholders(model, session);

        int kills = ended == null ? session == null ? 0 : session.getKills()
                : ended.getKills();
        int finalKills = ended == null ? session == null ? 0
                : session.getFinalKills() : ended.getFinalKills();
        int chickenKills = ended == null ? session == null ? 0
                : session.getChickensKilled() : ended.getChickenKills();
        int deaths = ended == null ? session == null ? 0 : session.getDeaths()
                : ended.getDeaths();
        long resources = ended == null ? session == null ? 0L
                : session.getResourcesCollected() : ended.getResources();
        boolean rewardsVisible = definition.getModeProfile().isRewardsEnabled()
                && endingCoordinator.isSuccessful();
        model.value("kills", kills).value("final_kills", finalKills)
                .value("chicken_kills", chickenKills).value("deaths", deaths)
                .value("resources", resources)
                .value("feathers", ResourceWallet.count(player,
                        ResourceType.FEATHER))
                .value("reward_xp", rewardsVisible && ended != null
                        ? ended.getExperience() : 0L)
                .value("reward_coins", rewardsVisible && ended != null
                        ? ended.getCoins() : 0L)
                .value("level", ended == null ? 0 : ended.getLevel());
        return model;
    }

    private void addLabels(ScoreboardPlaceholderModel model, Player player) {
        String[] labels = {"map", "mode", "players", "chicken", "shield",
                "kills", "final_kills", "chickens", "feathers", "spectator",
                "winner", "deaths", "resources", "duration", "level"};
        for (String label : labels) {
            model.value("label_" + label, message(player,
                    "scoreboard.label." + label.replace('_', '-')));
        }
    }

    private void addNextPhase(ScoreboardPlaceholderModel model, Player player) {
        MatchPhaseDefinition next = timeline.next(elapsedSeconds);
        if (next == null) {
            model.value("next_event_name", message(player,
                    "scoreboard.no-next-phase")).value("next_event_time", "--:--");
            return;
        }
        model.value("next_event_name", message(player,
                        next.getMessageKey()))
                .value("next_event_time", formatTime(Math.max(0,
                        next.getAtSecond() - elapsedSeconds)));
    }

    private void addTeamPlaceholders(ScoreboardPlaceholderModel model,
                                     PlayerSession session) {
        List<String> lines = new ArrayList<String>();
        String own = "";
        String enemy = "";
        for (it.legacynetwork.chickenwars.model.TeamColor color
                : it.legacynetwork.chickenwars.model.TeamColor.values()) {
            model.value("team_" + color.name().toLowerCase(Locale.ROOT), "");
        }
        model.value("chicken_health", "-").value("chicken_max_health", "-")
                .value("chicken_shield", "-").value("chicken_max_shield", "-");
        for (GameTeam team : teams.values()) {
            if (team.getMemberCount() == 0) continue;
            String line = renderTeamLine(team, session);
            lines.add(line);
            model.value("team_" + team.getColor().name()
                    .toLowerCase(Locale.ROOT), line);
            if (session != null && team.getId().equals(session.getTeamId())) {
                own = line;
                if (team.getChicken() != null) {
                    model.value("chicken_health", team.getChicken().getVitals()
                                    .getDisplayHealth())
                            .value("chicken_max_health", (int) team.getChicken()
                                    .getVitals().getMaxHealth())
                            .value("chicken_shield", team.getChicken().getVitals()
                                    .getDisplayShield())
                            .value("chicken_max_shield", (int) team.getChicken()
                                    .getVitals().getMaxShield());
                }
            } else if (enemy.isEmpty()) {
                enemy = line;
            }
        }
        model.lines("team_lines", lines).value("own_team_line", own)
                .value("enemy_team_line", enemy);
    }

    private String renderTeamLine(GameTeam team, PlayerSession session) {
        String marker = session != null && team.getId().equals(session.getTeamId())
                ? ChatColor.GRAY + " *" : "";
        String status;
        if (team.hasChicken()) {
            status = ChatColor.GREEN.toString()
                    + team.getChicken().getVitals().getDisplayHealth();
        } else if (team.getAliveCount() > 0) {
            status = ChatColor.YELLOW.toString() + team.getAliveCount();
        } else {
            status = ChatColor.RED + "x";
        }
        return team.getColor().getChatColor() + team.getColor().getInitial()
                + ChatColor.WHITE + " " + team.getDefinition().getDisplayName()
                + ": " + status + marker;
    }

    private String message(Player player, String key, String... replacements) {
        return services.getMessages().get(player, key, replacements);
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainder = seconds % 60;
        return String.format("%02d:%02d", minutes, remainder);
    }

    /**
     * Invia un messaggio tradotto a tutti i partecipanti.
     */
    public void broadcast(String key, String... replacements) {
        services.getMessages().broadcast(getOnlinePlayers(), key, replacements);
    }

    /**
     * Invia piu' righe tradotte a tutti i partecipanti.
     */
    public void broadcastList(String key, String... replacements) {
        services.getMessages().broadcastList(getOnlinePlayers(), key, replacements);
    }

    private void notifyTeam(GameTeam team, String key, String... replacements) {
        for (Player player : getTeamPlayers(team)) {
            services.getMessages().send(player, key, replacements);
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 0.5F);
        }
    }

    private void playSound(Sound sound, float pitch) {
        for (Player player : getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, 1.0F, pitch);
        }
    }

    // ------------------------------------------------------------------
    // Accessori
    // ------------------------------------------------------------------

    /**
     * Elenca i giocatori attualmente connessi e iscritti alla partita.
     */
    public List<Player> getOnlinePlayers() {
        List<Player> players = new ArrayList<Player>();
        for (UUID playerId : sessions.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }

    /**
     * Elenca i giocatori connessi appartenenti alla squadra indicata.
     */
    public List<Player> getTeamPlayers(GameTeam team) {
        List<Player> players = new ArrayList<Player>();
        if (team == null) {
            return players;
        }
        for (UUID member : team.getMembers()) {
            Player player = Bukkit.getPlayer(member);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }

    /**
     * Individua la squadra proprietaria dell'entita' gallina indicata.
     *
     * @return la squadra, oppure {@code null} se l'entita' non e' una gallina reale
     */
    public GameTeam findChickenOwner(Entity entity) {
        if (entity == null) {
            return null;
        }
        it.legacynetwork.chickenwars.chicken.RoyalChickenRegistry.Entry entry =
                services.getRoyalRegistry().lookup(entity.getUniqueId());
        return entry != null && entry.belongsTo(definition.getId())
                ? getTeam(entry.getTeamId()) : null;
    }

    public PlayerSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    public boolean contains(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public Collection<PlayerSession> getSessions() {
        return sessions.values();
    }

    public GameTeam getTeam(String teamId) {
        return teamId == null ? null : teams.get(teamId);
    }

    public Collection<GameTeam> getTeams() {
        return teams.values();
    }

    public ArenaDefinition getDefinition() {
        return definition;
    }

    public String getMatchId() { return matchId; }

    public ArenaState getState() {
        return state;
    }

    public void setState(ArenaState state) {
        if (state != null) {
            this.state = state;
        }
    }

    public MapRestoreService getRestore() {
        return restore;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public GameTeam getWinner() {
        return winner;
    }

    public int getPlayerCount() {
        return sessions.size();
    }

    private World resolveWorld() {
        return definition.getWorld() == null
                ? null : Bukkit.getWorld(definition.getWorld());
    }

    private Location resolve(SimpleLocation location) {
        return location == null ? null : location.toLocation();
    }
}
