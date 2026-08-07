package it.legacynetwork.chickenwars.hologram;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ologramma multiriga realizzato con armor stand invisibili.
 *
 * <p>Usa esclusivamente API Bukkit disponibili su 1.8.8, senza NMS ne' plugin
 * esterni. Ogni riga corrisponde a un armor stand marker, quindi privo di
 * hitbox e di interazioni con il mondo.</p>
 */
public final class Hologram {

    private static final double LINE_SPACING = 0.25D;

    private final List<ArmorStand> stands = new ArrayList<ArmorStand>();
    private final List<String> renderedLines = new ArrayList<String>();
    private Location base;

    /**
     * Crea l'ologramma alla posizione indicata.
     *
     * @param base   posizione della riga piu' bassa
     * @param lines  righe gia' tradotte, senza codici colore alternativi
     */
    public Hologram(Location base, List<String> lines) {
        this.base = base.clone();
        update(lines);
    }

    /**
     * Aggiorna il testo, ricreando gli armor stand solo se cambia il numero di
     * righe. Le modifiche di solo testo riusano gli stand esistenti.
     *
     * @param lines righe desiderate
     */
    public void update(List<String> lines) {
        List<String> safeLines = sanitize(lines);
        if (safeLines.size() != stands.size()) {
            rebuild(safeLines);
            return;
        }
        for (int i = 0; i < safeLines.size(); i++) {
            String line = safeLines.get(i);
            if (line.equals(renderedLines.get(i))) {
                continue;
            }
            ArmorStand stand = stands.get(i);
            if (stand != null && stand.isValid()) {
                stand.setCustomName(line);
            }
            renderedLines.set(i, line);
        }
    }

    /**
     * Sposta l'ologramma mantenendo testo e ordine delle righe.
     */
    public void teleport(Location target) {
        if (target == null || target.getWorld() == null) {
            return;
        }
        this.base = target.clone();
        for (int i = 0; i < stands.size(); i++) {
            ArmorStand stand = stands.get(i);
            if (stand != null && stand.isValid()) {
                stand.teleport(lineLocation(stands.size(), i));
            }
        }
    }

    /**
     * Rimuove tutti gli armor stand associati.
     */
    public void remove() {
        for (ArmorStand stand : stands) {
            if (stand != null && stand.isValid()) {
                stand.remove();
            }
        }
        stands.clear();
        renderedLines.clear();
    }

    private void rebuild(List<String> lines) {
        remove();
        World world = base.getWorld();
        if (world == null) {
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            ArmorStand stand = spawnStand(lineLocation(lines.size(), i), lines.get(i));
            stands.add(stand);
            renderedLines.add(lines.get(i));
        }
    }

    /**
     * La riga di indice zero e' la piu' in alto: gli armor stand vengono quindi
     * distribuiti verso il basso a partire dalla cima.
     */
    private Location lineLocation(int total, int index) {
        double offset = (total - 1 - index) * LINE_SPACING;
        return base.clone().add(0.0D, offset, 0.0D);
    }

    private ArmorStand spawnStand(Location location, String text) {
        World world = location.getWorld();
        ArmorStand stand = world.spawn(location, ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setMarker(true);
        stand.setCanPickupItems(false);
        stand.setRemoveWhenFarAway(false);
        stand.setCustomName(text);
        stand.setCustomNameVisible(!text.isEmpty());
        return stand;
    }

    private List<String> sanitize(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>(lines.size());
        for (String line : lines) {
            String value = line == null ? "" : line;
            result.add(ChatColor.translateAlternateColorCodes('&', value));
        }
        return result;
    }
}
