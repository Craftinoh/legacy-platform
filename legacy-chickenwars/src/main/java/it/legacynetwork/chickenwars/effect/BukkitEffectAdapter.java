package it.legacynetwork.chickenwars.effect;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * Applicazione reale degli effetti tramite API Bukkit 1.8.
 *
 * <p>Ogni applicazione rimuove prima l'effetto dello stesso tipo: un
 * potenziamento non puo' quindi accumularsi con la propria versione precedente,
 * ne' sopravvivere con un amplificatore piu' alto a un downgrade.</p>
 *
 * <p>Un giocatore non piu' connesso non produce errori: l'adapter riferisce
 * semplicemente che non ha applicato nulla.</p>
 */
public final class BukkitEffectAdapter implements EffectAdapter {

    @Override
    public boolean apply(UUID playerId, PotionEffectType type,
                         int durationTicks, int amplifier) {
        if (type == null || durationTicks <= 0 || amplifier < 0) {
            return false;
        }
        Player player = resolve(playerId);
        if (player == null) {
            return false;
        }
        player.removePotionEffect(type);
        return player.addPotionEffect(
                new PotionEffect(type, durationTicks, amplifier), true);
    }

    @Override
    public boolean clear(UUID playerId, PotionEffectType type) {
        if (type == null) {
            return false;
        }
        Player player = resolve(playerId);
        if (player == null || !player.hasPotionEffect(type)) {
            return false;
        }
        player.removePotionEffect(type);
        return true;
    }

    private Player resolve(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        Player player = Bukkit.getPlayer(playerId);
        return player != null && player.isOnline() ? player : null;
    }
}
