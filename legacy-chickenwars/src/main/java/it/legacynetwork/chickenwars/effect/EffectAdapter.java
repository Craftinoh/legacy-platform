package it.legacynetwork.chickenwars.effect;

import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * Unico punto di contatto fra la logica degli effetti e il server.
 *
 * <p>I servizi decidono chi riceve cosa, questo adapter esegue. Separando le
 * due responsabilita' le regole restano verificabili senza un server attivo e
 * l'applicazione reale resta un solo blocco di codice.</p>
 */
public interface EffectAdapter {

    /**
     * Applica un effetto sostituendo quello dello stesso tipo gia' presente.
     *
     * @param playerId      destinatario
     * @param type          tipo di effetto
     * @param durationTicks durata in tick, deve essere positiva
     * @param amplifier     amplificatore Bukkit, dove {@code 0} e' il livello I
     * @return {@code true} se l'effetto e' stato realmente applicato
     */
    boolean apply(UUID playerId, PotionEffectType type, int durationTicks,
                  int amplifier);

    /**
     * Rimuove un effetto, se presente.
     *
     * @return {@code true} se il giocatore aveva davvero quell'effetto
     */
    boolean clear(UUID playerId, PotionEffectType type);
}
