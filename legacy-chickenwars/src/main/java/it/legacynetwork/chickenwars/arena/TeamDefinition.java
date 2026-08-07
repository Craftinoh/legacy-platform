package it.legacynetwork.chickenwars.arena;

import it.legacynetwork.chickenwars.model.SimpleLocation;
import it.legacynetwork.chickenwars.model.TeamColor;
import it.legacynetwork.chickenwars.region.CuboidRegion;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Configurazione di una squadra all'interno di un'arena.
 *
 * <p>Le posizioni possono essere nulle finche' l'arena e' in configurazione:
 * {@link #findMissing()} elenca cio' che manca prima dell'abilitazione.</p>
 */
public final class TeamDefinition {

    private final String id;
    private final String displayName;
    private final TeamColor color;
    private final int maxPlayers;

    private SimpleLocation spawn;
    private SimpleLocation nest;
    private SimpleLocation chicken;
    private SimpleLocation shop;
    private SimpleLocation upgrades;
    private SimpleLocation baseMin;
    private SimpleLocation baseMax;
    private CuboidRegion baseRegion;

    public TeamDefinition(String id, String displayName, TeamColor color,
                          int maxPlayers) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID squadra mancante");
        }
        if (color == null) {
            throw new IllegalArgumentException("Colore squadra mancante");
        }
        this.id = id.trim().toLowerCase(Locale.ROOT);
        this.displayName = displayName == null || displayName.trim().isEmpty()
                ? color.getItalianName() : displayName.trim();
        this.color = color;
        this.maxPlayers = Math.max(1, maxPlayers);
    }

    /**
     * Elenca le posizioni obbligatorie non ancora configurate.
     *
     * @return descrizioni leggibili, vuote se la squadra e' completa
     */
    public List<String> findMissing() {
        List<String> missing = new ArrayList<String>();
        if (spawn == null) {
            missing.add("spawn squadra " + id);
        }
        if (nest == null) {
            missing.add("nido squadra " + id);
        }
        if (chicken == null) {
            missing.add("posizione gallina squadra " + id);
        }
        if (shop == null) {
            missing.add("shop squadra " + id);
        }
        return missing;
    }

    public boolean isComplete() {
        return findMissing().isEmpty();
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TeamColor getColor() {
        return color;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public SimpleLocation getSpawn() {
        return spawn;
    }

    public void setSpawn(SimpleLocation spawn) {
        this.spawn = spawn;
    }

    public SimpleLocation getNest() {
        return nest;
    }

    public void setNest(SimpleLocation nest) {
        this.nest = nest;
    }

    public SimpleLocation getChicken() {
        return chicken;
    }

    public void setChicken(SimpleLocation chicken) {
        this.chicken = chicken;
    }

    public SimpleLocation getShop() {
        return shop;
    }

    public void setShop(SimpleLocation shop) {
        this.shop = shop;
    }

    public SimpleLocation getUpgrades() {
        return upgrades;
    }

    public void setUpgrades(SimpleLocation upgrades) {
        this.upgrades = upgrades;
    }

    public SimpleLocation getBaseMin() {
        return baseMin;
    }

    public void setBaseMin(SimpleLocation baseMin) {
        this.baseMin = baseMin;
        this.baseRegion = null;
    }

    public SimpleLocation getBaseMax() {
        return baseMax;
    }

    public void setBaseMax(SimpleLocation baseMax) {
        this.baseMax = baseMax;
        this.baseRegion = null;
    }

    /**
     * Regione della base, ricavata dai due angoli configurati.
     *
     * <p>Il risultato viene memorizzato: e' consultato a ogni movimento dei
     * giocatori e non deve essere ricostruito ogni volta.</p>
     *
     * @return la regione, oppure {@code null} se non configurata o non valida
     */
    public CuboidRegion getBaseRegion() {
        CuboidRegion cached = baseRegion;
        if (cached != null) {
            return cached;
        }
        CuboidRegion built = CuboidRegion.between(baseMin, baseMax);
        baseRegion = built;
        return built;
    }

    /**
     * Indica se la squadra ha una regione base utilizzabile.
     */
    public boolean hasBaseRegion() {
        return getBaseRegion() != null;
    }

    /**
     * Elenca i problemi della regione base, senza renderla obbligatoria.
     *
     * <p>Heal Pool e trappole richiedono la regione: la sua assenza viene
     * segnalata come avviso, cosi' le mappe gia' configurate restano valide.</p>
     *
     * @return descrizioni leggibili, vuote se la regione e' assente o corretta
     */
    public List<String> findBaseRegionIssues() {
        List<String> issues = new ArrayList<String>();
        if (baseMin == null && baseMax == null) {
            return issues;
        }
        if (baseMin == null || baseMax == null) {
            issues.add("regione base squadra " + id + ": angolo mancante");
            return issues;
        }
        if (!baseMin.getWorld().equalsIgnoreCase(baseMax.getWorld())) {
            issues.add("regione base squadra " + id
                    + ": angoli in mondi diversi");
            return issues;
        }
        if (!isFinite(baseMin) || !isFinite(baseMax)) {
            issues.add("regione base squadra " + id
                    + ": coordinate non finite");
        }
        return issues;
    }

    private boolean isFinite(SimpleLocation location) {
        return !Double.isNaN(location.getX()) && !Double.isInfinite(location.getX())
                && !Double.isNaN(location.getY())
                && !Double.isInfinite(location.getY())
                && !Double.isNaN(location.getZ())
                && !Double.isInfinite(location.getZ());
    }

    /**
     * Nome colorato usato in chat, scoreboard e ologrammi.
     */
    public String getColoredName() {
        return color.getChatColor() + displayName;
    }
}
