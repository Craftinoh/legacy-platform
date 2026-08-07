package it.legacynetwork.chickenwars.chicken;

import it.legacynetwork.chickenwars.game.GameTeam;
import it.legacynetwork.chickenwars.hologram.Hologram;
import it.legacynetwork.chickenwars.model.SimpleLocation;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Creazione, mantenimento e rimozione delle entita' Gallina Reale.
 *
 * <p>Usa solo API Bukkit 1.8: la gallina viene tenuta nel proprio nido tramite
 * teletrasporti correttivi anziche' modificando l'intelligenza artificiale, che
 * richiederebbe NMS.</p>
 */
public final class ChickenService {

    private final Logger logger;

    public ChickenService(Logger logger) {
        this.logger = logger;
    }

    /**
     * Genera la Gallina Reale della squadra e il relativo ologramma.
     *
     * @param team     squadra proprietaria
     * @param settings impostazioni correnti
     * @return la gallina creata, oppure {@code null} se la posizione non e' valida
     */
    public RoyalChicken spawn(GameTeam team, ChickenSettings settings) {
        SimpleLocation configured = team.getDefinition().getChicken();
        if (configured == null) {
            logger.warning("Squadra " + team.getId()
                    + ": posizione gallina non configurata.");
            return null;
        }
        Location location = configured.centered().toLocation();
        if (location == null || location.getWorld() == null) {
            logger.warning("Squadra " + team.getId()
                    + ": mondo della gallina non caricato.");
            return null;
        }

        RoyalChicken royal = new RoyalChicken(team.getId(),
                team.getDefinition().getNest(), settings);
        royal.setEntity(createEntity(location, settings));

        if (settings.isHologramEnabled()) {
            Location hologramBase =
                    location.clone().add(0.0D, settings.getHologramHeight(), 0.0D);
            royal.setHologram(new Hologram(hologramBase,
                    renderHologram(royal, team, settings)));
        }
        return royal;
    }

    /**
     * Riporta la gallina nel nido quando si allontana troppo e ne aggiorna
     * l'ologramma.
     *
     * @param royal    gallina da aggiornare
     * @param team     squadra proprietaria
     * @param settings impostazioni correnti
     */
    public void update(RoyalChicken royal, GameTeam team, ChickenSettings settings) {
        if (royal == null || !royal.isAlive()) {
            return;
        }
        LivingEntity entity = royal.getEntity();
        if (entity == null || entity.isDead() || !entity.isValid()) {
            respawnEntity(royal, team, settings);
            entity = royal.getEntity();
            if (entity == null) {
                return;
            }
        }

        entity.setFireTicks(0);
        if (entity.getHealth() < entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }

        Location anchor = anchorLocation(royal, team);
        if (anchor != null) {
            double radius = settings.canMove() ? settings.getMovementRadius() : 0.6D;
            Location current = entity.getLocation();
            boolean differentWorld = current.getWorld() != anchor.getWorld();
            if (differentWorld
                    || current.distanceSquared(anchor) > radius * radius) {
                entity.teleport(anchor);
            }
        }

        Hologram hologram = royal.getHologram();
        if (hologram != null) {
            hologram.update(renderHologram(royal, team, settings));
            if (anchor != null) {
                hologram.teleport(entity.getLocation()
                        .clone().add(0.0D, settings.getHologramHeight(), 0.0D));
            }
        }
    }

    private void respawnEntity(RoyalChicken royal, GameTeam team,
                               ChickenSettings settings) {
        Location anchor = anchorLocation(royal, team);
        if (anchor == null || anchor.getWorld() == null) {
            return;
        }
        royal.setEntity(createEntity(anchor, settings));
    }

    /**
     * Crea l'entita' gallina con le proprieta' comuni a spawn e ripristino.
     *
     * <p>La vita dell'entita' resta fissa: la salute reale e' quella gestita da
     * {@link ChickenVitals}, mentre il danno vanilla viene sempre annullato.</p>
     */
    private Chicken createEntity(Location location, ChickenSettings settings) {
        Chicken entity = location.getWorld().spawn(location, Chicken.class);
        entity.setAdult();
        entity.setRemoveWhenFarAway(false);
        entity.setCanPickupItems(false);
        entity.setCustomName(ChatColor.translateAlternateColorCodes('&',
                settings.getDisplayName()));
        entity.setCustomNameVisible(false);
        entity.setMaxHealth(20.0D);
        entity.setHealth(20.0D);
        return entity;
    }

    private Location anchorLocation(RoyalChicken royal, GameTeam team) {
        SimpleLocation configured = team.getDefinition().getChicken();
        if (configured == null) {
            configured = royal.getNest();
        }
        return configured == null ? null : configured.centered().toLocation();
    }

    /**
     * Compone le righe dell'ologramma sostituendo i segnaposto configurati.
     */
    public List<String> renderHologram(RoyalChicken royal, GameTeam team,
                                       ChickenSettings settings) {
        ChickenVitals vitals = royal.getVitals();
        List<String> lines = new ArrayList<String>();
        for (String template : settings.getHologramLines()) {
            String line = template
                    .replace("{team_color}", team.getColor().getChatColor().toString())
                    .replace("{team_name}", team.getDefinition().getDisplayName())
                    .replace("{health}", String.valueOf(vitals.getDisplayHealth()))
                    .replace("{max_health}",
                            String.valueOf((int) vitals.getMaxHealth()))
                    .replace("{shield}", String.valueOf(vitals.getDisplayShield()))
                    .replace("{max_shield}",
                            String.valueOf((int) vitals.getMaxShield()))
                    .replace("{state}", royal.getState().name());
            lines.add(line);
        }
        return lines;
    }

    /**
     * Riproduce l'effetto di danno alla gallina.
     */
    public void playDamageEffect(RoyalChicken royal) {
        LivingEntity entity = royal.getEntity();
        if (entity == null || !entity.isValid()) {
            return;
        }
        Location location = entity.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(location, Sound.CHICKEN_HURT, 1.0F, 1.0F);
        world.playEffect(location.clone().add(0.0D, 0.5D, 0.0D),
                Effect.STEP_SOUND, Material.SNOW_BLOCK.getId());
    }

    /**
     * Esegue l'animazione di morte e rimuove entita' e ologramma.
     *
     * @param royal    gallina eliminata
     * @param settings impostazioni correnti
     */
    public void playDeath(RoyalChicken royal, ChickenSettings settings) {
        LivingEntity entity = royal.getEntity();
        Location location = entity != null && entity.isValid()
                ? entity.getLocation().clone() : resolveNest(royal);

        remove(royal);

        if (location == null || location.getWorld() == null) {
            return;
        }
        World world = location.getWorld();
        world.playSound(location, Sound.ENDERDRAGON_DEATH, 1.0F, 1.0F);
        if (settings.isLightningOnDeath()) {
            world.strikeLightningEffect(location);
        }

        int particles = settings.getDeathFeatherParticles();
        for (int i = 0; i < particles; i++) {
            double offsetX = (Math.random() - 0.5D) * 2.0D;
            double offsetY = Math.random() * 1.5D;
            double offsetZ = (Math.random() - 0.5D) * 2.0D;
            world.playEffect(location.clone().add(offsetX, offsetY, offsetZ),
                    Effect.STEP_SOUND, Material.SNOW_BLOCK.getId());
        }
    }

    private Location resolveNest(RoyalChicken royal) {
        SimpleLocation nest = royal.getNest();
        return nest == null ? null : nest.centered().toLocation();
    }

    /**
     * Rimuove entita' e ologramma associati alla gallina.
     */
    public void remove(RoyalChicken royal) {
        if (royal == null) {
            return;
        }
        LivingEntity entity = royal.getEntity();
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
        royal.setEntity(null);

        Hologram hologram = royal.getHologram();
        if (hologram != null) {
            hologram.remove();
            royal.setHologram(null);
        }
    }
}
