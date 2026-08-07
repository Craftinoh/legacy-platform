package it.legacynetwork.chickenwars.shop;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Listino prezzi di una singola modalita' economica.
 *
 * <p>I profili previsti sono {@code solo_duel}, {@code doubles} e {@code trio};
 * {@code DUEL} e {@code SOLO} condividono lo stesso profilo.</p>
 */
public final class PricingProfile {

    private final String id;
    private final Map<String, ItemCost> costs;

    public PricingProfile(String id, Map<String, ItemCost> costs) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID profilo prezzi mancante");
        }
        this.id = id.trim().toLowerCase(Locale.ROOT);
        this.costs = Collections.unmodifiableMap(
                new LinkedHashMap<String, ItemCost>(
                        costs == null
                                ? Collections.<String, ItemCost>emptyMap()
                                : costs));
    }

    /**
     * Prezzo di un articolo in questo profilo.
     *
     * @return il prezzo, oppure {@code null} se l'articolo non e' listato
     */
    public ItemCost getCost(String itemId) {
        return itemId == null
                ? null : costs.get(itemId.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Indica se il profilo contiene un prezzo per l'articolo indicato.
     */
    public boolean contains(String itemId) {
        return getCost(itemId) != null;
    }

    public String getId() {
        return id;
    }

    public Map<String, ItemCost> getCosts() {
        return costs;
    }

    public int size() {
        return costs.size();
    }
}
