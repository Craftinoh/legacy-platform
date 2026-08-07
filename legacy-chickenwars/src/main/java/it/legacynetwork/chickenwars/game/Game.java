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
import it.legacynetwork.chickenwars.config.ChickenWarsConfig;
import it.legacynetwork.chickenwars.generator.RuntimeGenerator;
import it.legacynetwork.chickenwars.hologram.Hologram;
import it.legacynetwork.chickenwars.model.ArenaState;
import it.legacynetwork.chickenwars.model.SimpleLocation;
import it.legacynetwork.chickenwars.player.InventorySnapshot;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.player.PlayerState;
import it.legacynetwork.chickenwars.scoreboard.GameScoreboard;
import it.legacynetwork.chickenwars.world.MapRestoreService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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
    private final List<RuntimeGenerator> generators =
            new ArrayList<RuntimeGenerator>();
    private final Map<UUID, Villager> shopNpcs = new LinkedHashMap<UUID, Villager>();
    private final Map<UUID, Location> shopAnchors = new LinkedHashMap<UUID, Location>();
    private final MapRestoreService restore = new MapRestoreService();

    private ArenaState state = ArenaState.WAITING;
    private int tickCounter;
    private int countdownSeconds;
    private int elapsedSeconds;
    private int endingSecondsLeft;
    private GameTeam winner;

    public Game(GameServices services, ArenaDefinition definition) {
        if (services == null || definition == null) {
            throw new IllegalArgumentException("Partita non inizializzabile");
        }
        this.services = services;
        this.definition = definition;
        this.countdownSeconds = services.getConfig().getStartingCountdownSeconds();
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
        Location lobby = resolve(definition.getLobby());
        if (lobby == null) {
            services.getMessages().send(player, "arena.world-not-loaded");
            return false;
        }

        PlayerSession session = new PlayerSession(player.getUniqueId(),
                player.getName(), definition.getId(),
                InventorySnapshot.capture(player));
        sessions.put(player.getUniqueId(), session);

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

        GameTeam team = getTeam(session.getTeamId());
        if (team != null) {
            team.removeMember(player.getUniqueId());
        }

        boards.remove(player.getUniqueId());
        GameScoreboard.clear(player);

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
                tickGenerators();
                if (tickCounter % TICKS_PER_SECOND == 0) {
                    tickSecond();
                }
                break;
            case ENDING:
                if (tickCounter % TICKS_PER_SECOND == 0 && --endingSecondsLeft <= 0) {
                    restart();
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
        for (int i = 0; i < generators.size(); i++) {
            generators.get(i).tick(services.getConfig().getGenerators());
        }
    }

    private void tickSecond() {
        elapsedSeconds++;
        ChickenWarsConfig config = services.getConfig();
        ChickenSettings chickenSettings = config.getChicken();

        for (GameTeam team : teams.values()) {
            RoyalChicken chicken = team.getChicken();
            if (chicken == null || !chicken.isAlive()) {
                continue;
            }
            chicken.regenerate(chickenSettings);
            services.getChickens().update(chicken, team, chickenSettings);
        }

        tickRespawns();
        updateGeneratorHolograms();
        anchorShopNpcs();

        if (elapsedSeconds >= config.getMaximumDurationSeconds()) {
            end(findLeadingTeam());
        }
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

    private void updateGeneratorHolograms() {
        for (RuntimeGenerator generator : generators) {
            if (generator.getHologram() == null) {
                continue;
            }
            List<String> lines = new ArrayList<String>();
            lines.add(generator.getType().getColor() + ChatColor.BOLD.toString()
                    + generator.getType().getItalianName());
            lines.add(services.getMessages().get(null, "generator.countdown",
                    "{seconds}", String.valueOf(generator.getSecondsUntilNext())));
            generator.updateHologram(lines);
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
        if (state == ArenaState.IN_GAME || sessions.isEmpty()) {
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
        elapsedSeconds = 0;
        applyWorldRules();
        spawnChickens();
        createGenerators();
        spawnShopNpcs();

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
        }
    }

    private void createGenerators() {
        generators.clear();
        for (GeneratorDefinition generatorDefinition : definition.getGenerators()) {
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
            RuntimeGenerator generator = new RuntimeGenerator(generatorDefinition,
                    location, services.getConfig().getGenerators());
            if (generatorDefinition.hasHologram()
                    && !generatorDefinition.isTeamGenerator()) {
                List<String> lines = new ArrayList<String>();
                lines.add(generatorDefinition.getType().getColor()
                        + ChatColor.BOLD.toString()
                        + generatorDefinition.getType().getItalianName());
                lines.add("");
                generator.setHologram(new Hologram(
                        location.clone().add(0.0D, 2.0D, 0.0D), lines));
            }
            generators.add(generator);
        }
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

    private void removeShopNpcs() {
        for (Villager villager : shopNpcs.values()) {
            if (villager != null && villager.isValid()) {
                villager.remove();
            }
        }
        shopNpcs.clear();
        shopAnchors.clear();
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
        giveKit(player, team);

        int protection = services.getConfig().getSpawnProtectionSeconds();
        session.grantInvulnerability(protection);
        session.clearDamager();

        if (!initial) {
            services.getMessages().send(player, "respawn.done");
        }
    }

    private void giveKit(Player player, GameTeam team) {
        player.getInventory().addItem(new ItemStack(Material.WOOD_SWORD));
        player.getInventory().setHelmet(colorArmor(
                new ItemStack(Material.LEATHER_HELMET), team));
        player.getInventory().setChestplate(colorArmor(
                new ItemStack(Material.LEATHER_CHESTPLATE), team));
        player.getInventory().setLeggings(colorArmor(
                new ItemStack(Material.LEATHER_LEGGINGS), team));
        player.getInventory().setBoots(colorArmor(
                new ItemStack(Material.LEATHER_BOOTS), team));
        player.updateInventory();
    }

    private ItemStack colorArmor(ItemStack stack, GameTeam team) {
        if (stack.getItemMeta() instanceof LeatherArmorMeta) {
            LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
            meta.setColor(team.getColor().getArmorColor());
            stack.setItemMeta(meta);
        }
        return stack;
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
        GameTeam team = getTeam(session.getTeamId());
        if (team == null) {
            return;
        }

        session.addDeath();
        InventorySnapshot.clear(player);

        boolean finalDeath = !team.hasChicken()
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

    private void respawn(Player player, PlayerSession session) {
        GameTeam team = getTeam(session.getTeamId());
        if (team == null || !team.hasChicken()) {
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

        if (attacker != null) {
            PlayerSession session = sessions.get(attacker.getUniqueId());
            if (session == null || !session.getState().isActive()) {
                return false;
            }
            if (owner.isMember(attacker.getUniqueId())) {
                services.getMessages().send(attacker, "chicken.own-team");
                return false;
            }
        }

        CWChickenDamageEvent event = new CWChickenDamageEvent(this, owner, chicken,
                attacker, amount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getDamage() <= 0.0D) {
            return false;
        }

        UUID attackerId = attacker == null ? null : attacker.getUniqueId();
        DamageOutcome outcome = chicken.damage(event.getDamage(), attackerId);
        if (outcome.isNoop()) {
            return false;
        }

        services.getChickens().playDamageEffect(chicken);
        ChickenSettings settings = services.getConfig().getChicken();
        services.getChickens().update(chicken, owner, settings);

        if (outcome.isFatal()) {
            handleChickenDeath(owner, chicken, attacker);
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

    private void handleChickenDeath(GameTeam owner, RoyalChicken chicken,
                                    Player killer) {
        ChickenSettings settings = services.getConfig().getChicken();
        services.getChickens().playDeath(chicken, settings);

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
        if (team.isEliminated() || team.hasChicken() || team.getAliveCount() > 0) {
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
        GameTeam survivor = null;
        int remaining = 0;
        for (GameTeam team : teams.values()) {
            if (team.getMemberCount() == 0 || team.isOut()) {
                continue;
            }
            remaining++;
            survivor = team;
        }
        if (remaining <= 1) {
            end(survivor);
        }
    }

    private GameTeam findLeadingTeam() {
        GameTeam leader = null;
        for (GameTeam team : teams.values()) {
            if (team.isOut()) {
                continue;
            }
            if (leader == null || team.getAliveCount() > leader.getAliveCount()) {
                leader = team;
            }
        }
        return leader;
    }

    /**
     * Termina la partita e avvia la fase di riepilogo.
     *
     * @param winningTeam squadra vincitrice, eventualmente nulla
     */
    public void end(GameTeam winningTeam) {
        if (state == ArenaState.ENDING || state == ArenaState.RESTARTING) {
            return;
        }
        state = ArenaState.ENDING;
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
            player.setGameMode(GameMode.SPECTATOR);
        }

        Bukkit.getPluginManager().callEvent(new CWGameEndEvent(this, winningTeam));
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
        sessions.clear();
        boards.clear();

        cleanup();
        restore.restore(definition);
        rebuildTeams();

        elapsedSeconds = 0;
        winner = null;
        countdownSeconds = services.getConfig().getStartingCountdownSeconds();
        state = definition.isEnabled() ? ArenaState.WAITING : ArenaState.DISABLED;
    }

    private void sendToReturnLobby(Player player) {
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
        for (GameTeam team : teams.values()) {
            RoyalChicken chicken = team.getChicken();
            if (chicken != null) {
                services.getChickens().remove(chicken);
                team.setChicken(null);
            }
        }
        for (RuntimeGenerator generator : generators) {
            generator.removeHologram();
        }
        generators.clear();
        removeShopNpcs();
        restore.clearEntities(definition);
    }

    // ------------------------------------------------------------------
    // Scoreboard e messaggistica
    // ------------------------------------------------------------------

    private void updateScoreboards() {
        for (Map.Entry<UUID, GameScoreboard> entry : boards.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                continue;
            }
            entry.getValue().update(
                    services.getMessages().get(player, "scoreboard.title"),
                    renderScoreboard(player));
        }
    }

    private List<String> renderScoreboard(Player player) {
        List<String> lines = new ArrayList<String>();
        PlayerSession session = sessions.get(player.getUniqueId());

        lines.add(ChatColor.GRAY + definition.getDisplayName());
        lines.add("");

        if (state == ArenaState.WAITING) {
            lines.add(services.getMessages().get(player, "scoreboard.waiting",
                    "{online}", String.valueOf(sessions.size()),
                    "{min}", String.valueOf(definition.getMinimumPlayers())));
        } else if (state == ArenaState.STARTING) {
            lines.add(services.getMessages().get(player, "scoreboard.starting",
                    "{seconds}", String.valueOf(countdownSeconds)));
        } else {
            for (GameTeam team : teams.values()) {
                if (team.getMemberCount() == 0) {
                    continue;
                }
                lines.add(renderTeamLine(player, team, session));
            }
            lines.add("");
            if (session != null) {
                lines.add(services.getMessages().get(player, "scoreboard.kills",
                        "{kills}", String.valueOf(session.getKills()),
                        "{final_kills}", String.valueOf(session.getFinalKills())));
            }
            lines.add(services.getMessages().get(player, "scoreboard.time",
                    "{time}", formatTime(elapsedSeconds)));
        }

        lines.add("");
        return lines;
    }

    private String renderTeamLine(Player player, GameTeam team,
                                  PlayerSession session) {
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
        for (GameTeam team : teams.values()) {
            RoyalChicken chicken = team.getChicken();
            if (chicken != null && chicken.getEntity() != null
                    && chicken.getEntity().getUniqueId().equals(entity.getUniqueId())) {
                return team;
            }
        }
        return null;
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
