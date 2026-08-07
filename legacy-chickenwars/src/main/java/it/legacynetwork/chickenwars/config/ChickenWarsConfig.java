package it.legacynetwork.chickenwars.config;

import it.legacynetwork.chickenwars.chicken.ChickenSettings;
import it.legacynetwork.chickenwars.model.SimpleLocation;
import it.legacynetwork.chickenwars.world.WorldTemplate;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Vista immutabile di {@code config.yml} e {@code chickens.yml}.
 *
 * <p>Viene ricostruita a ogni reload e pubblicata come singolo riferimento
 * {@code volatile}, cosi' che nessun consumatore osservi uno stato parziale.</p>
 */
public final class ChickenWarsConfig {

    private final int startingCountdownSeconds;
    private final int reducedCountdownSeconds;
    private final int maximumDurationSeconds;
    private final int endingSeconds;
    private final boolean allowRejoin;

    private final boolean respawnEnabled;
    private final int respawnSeconds;
    private final int spawnProtectionSeconds;

    private final boolean friendlyFire;
    private final int voidKillCreditSeconds;

    private final boolean blockPlaceAllowed;
    private final boolean breakPlacedOnly;
    private final boolean explosionsPlacedOnly;
    private final boolean fireSpread;
    private final boolean weatherCycle;
    private final boolean daylightCycle;
    private final boolean mobSpawning;

    private final int scoreboardUpdateTicks;
    private final SimpleLocation returnLobby;

    private final String worldNamePrefix;
    private final WorldTemplate defaultWorldTemplate;
    private final boolean autoLoadWorlds;
    private final boolean clearWorldEntities;
    private final double voidDropTolerance;

    private final String fallbackLanguage;
    private final String italianFile;
    private final String englishFile;

    private final GeneratorSettings generators;
    private final ChickenSettings chicken;

    private ChickenWarsConfig(FileConfiguration config,
                              GeneratorSettings generators,
                              ChickenSettings chicken) {
        this.startingCountdownSeconds = Math.max(3,
                config.getInt("game.starting-countdown", 30));
        this.reducedCountdownSeconds = Math.max(3,
                config.getInt("game.reduced-countdown", 10));
        this.maximumDurationSeconds = Math.max(60,
                config.getInt("game.maximum-duration", 1800));
        this.endingSeconds = Math.max(3, config.getInt("game.ending-duration", 10));
        this.allowRejoin = config.getBoolean("game.allow-rejoin", true);

        this.respawnEnabled = config.getBoolean("respawn.enabled", true);
        this.respawnSeconds = Math.max(0, config.getInt("respawn.base-time", 5));
        this.spawnProtectionSeconds = Math.max(0,
                config.getInt("respawn.spawn-protection-seconds", 3));

        this.friendlyFire = config.getBoolean("combat.friendly-fire", false);
        this.voidKillCreditSeconds = Math.max(0,
                config.getInt("combat.void-kill-credit-seconds", 10));

        this.blockPlaceAllowed = config.getBoolean("world.block-place", true);
        this.breakPlacedOnly = config.getBoolean("world.break-placed-only", true);
        this.explosionsPlacedOnly =
                config.getBoolean("world.explosions-placed-only", true);
        this.fireSpread = config.getBoolean("world.fire-spread", false);
        this.weatherCycle = config.getBoolean("world.weather-cycle", false);
        this.daylightCycle = config.getBoolean("world.daylight-cycle", false);
        this.mobSpawning = config.getBoolean("world.mob-spawning", false);

        this.scoreboardUpdateTicks = Math.max(5,
                config.getInt("scoreboard.update-ticks", 20));
        this.returnLobby = SimpleLocation.parse(config.getString("game.return-lobby"));

        this.worldNamePrefix = config.getString("world.name-prefix", "cw_");
        WorldTemplate template = WorldTemplate.fromString(
                config.getString("world.default-template", "VOID"));
        this.defaultWorldTemplate =
                template == null ? WorldTemplate.VOID : template;
        this.autoLoadWorlds = config.getBoolean("world.auto-load", true);
        this.clearWorldEntities = config.getBoolean("world.clear-entities", true);
        this.voidDropTolerance = Math.max(0.0D,
                config.getDouble("world.void-drop-tolerance", 5.0D));

        this.fallbackLanguage = normalize(config.getString("language.fallback", "it"));
        this.italianFile = config.getString("language.italian-file", "messages_it.yml");
        this.englishFile = config.getString("language.english-file", "messages_en.yml");

        this.generators = generators;
        this.chicken = chicken;
    }

    /**
     * Costruisce la configurazione a partire dai file gia' caricati.
     *
     * @param config     contenuto di config.yml
     * @param generators sezione generatori gia' interpretata
     * @param chicken    impostazioni della Gallina Reale
     * @return la configurazione risultante
     */
    public static ChickenWarsConfig create(FileConfiguration config,
                                           GeneratorSettings generators,
                                           ChickenSettings chicken) {
        if (config == null) {
            throw new IllegalArgumentException("config.yml non caricato");
        }
        if (generators == null) {
            throw new IllegalArgumentException("Impostazioni generatori mancanti");
        }
        if (chicken == null) {
            throw new IllegalArgumentException("Impostazioni gallina mancanti");
        }
        return new ChickenWarsConfig(config, generators, chicken);
    }

    private static String normalize(String language) {
        if (language == null) {
            return "it";
        }
        String trimmed = language.trim();
        return trimmed.isEmpty() ? "it" : trimmed;
    }

    public int getStartingCountdownSeconds() {
        return startingCountdownSeconds;
    }

    public int getReducedCountdownSeconds() {
        return reducedCountdownSeconds;
    }

    public int getMaximumDurationSeconds() {
        return maximumDurationSeconds;
    }

    public int getEndingSeconds() {
        return endingSeconds;
    }

    public boolean isAllowRejoin() {
        return allowRejoin;
    }

    public boolean isRespawnEnabled() {
        return respawnEnabled;
    }

    public int getRespawnSeconds() {
        return respawnSeconds;
    }

    public int getSpawnProtectionSeconds() {
        return spawnProtectionSeconds;
    }

    public boolean isFriendlyFire() {
        return friendlyFire;
    }

    public int getVoidKillCreditSeconds() {
        return voidKillCreditSeconds;
    }

    public boolean isBlockPlaceAllowed() {
        return blockPlaceAllowed;
    }

    public boolean isBreakPlacedOnly() {
        return breakPlacedOnly;
    }

    public boolean isExplosionsPlacedOnly() {
        return explosionsPlacedOnly;
    }

    public boolean isFireSpread() {
        return fireSpread;
    }

    public boolean isWeatherCycle() {
        return weatherCycle;
    }

    public boolean isDaylightCycle() {
        return daylightCycle;
    }

    public boolean isMobSpawning() {
        return mobSpawning;
    }

    public int getScoreboardUpdateTicks() {
        return scoreboardUpdateTicks;
    }

    /**
     * @return la destinazione di uscita, oppure {@code null} per usare lo spawn
     */
    public SimpleLocation getReturnLobby() {
        return returnLobby;
    }

    /** Prefisso applicato al nome del mondo creato per un'arena. */
    public String getWorldNamePrefix() {
        return worldNamePrefix;
    }

    /** Template usato quando non ne viene indicato uno esplicito. */
    public WorldTemplate getDefaultWorldTemplate() {
        return defaultWorldTemplate;
    }

    /** Indica se caricare all'avvio i mondi richiesti dalle arene. */
    public boolean isAutoLoadWorlds() {
        return autoLoadWorlds;
    }

    /**
     * Indica se rimuovere animali e mostri preesistenti dai mondi arena.
     */
    public boolean isClearWorldEntities() {
        return clearWorldEntities;
    }

    /**
     * Blocchi sopra {@code voidY} entro cui vale la protezione anti-drop.
     */
    public double getVoidDropTolerance() {
        return voidDropTolerance;
    }

    public String getFallbackLanguage() {
        return fallbackLanguage;
    }

    public String getItalianFile() {
        return italianFile;
    }

    public String getEnglishFile() {
        return englishFile;
    }

    public GeneratorSettings getGenerators() {
        return generators;
    }

    public ChickenSettings getChicken() {
        return chicken;
    }
}
