package it.legacynetwork.chickenwars.shop;

import it.legacynetwork.chickenwars.mode.ModeProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Catalogo completo dello shop: categorie, articoli e profili prezzi.
 *
 * <p>Immutabile: viene sostituita interamente a ogni reload, cosi' che nessun
 * menu aperto osservi uno stato parziale.</p>
 */
public final class ShopConfiguration {

    private final List<ShopCategoryDefinition> categories;
    private final Map<String, ShopItemDefinition> itemsById;
    private final Map<String, PricingProfile> profiles;
    private final List<String> warnings;

    ShopConfiguration(List<ShopCategoryDefinition> categories,
                      Map<String, ShopItemDefinition> itemsById,
                      Map<String, PricingProfile> profiles,
                      List<String> warnings) {
        this.categories = Collections.unmodifiableList(
                new ArrayList<ShopCategoryDefinition>(categories));
        this.itemsById = Collections.unmodifiableMap(
                new LinkedHashMap<String, ShopItemDefinition>(itemsById));
        this.profiles = Collections.unmodifiableMap(
                new LinkedHashMap<String, PricingProfile>(profiles));
        this.warnings = Collections.unmodifiableList(
                new ArrayList<String>(warnings));
    }

    /**
     * Catalogo vuoto, usato quando la configurazione non e' leggibile.
     */
    public static ShopConfiguration empty() {
        return new ShopConfiguration(
                Collections.<ShopCategoryDefinition>emptyList(),
                Collections.<String, ShopItemDefinition>emptyMap(),
                Collections.<String, PricingProfile>emptyMap(),
                Collections.<String>emptyList());
    }

    /**
     * Risolve il prezzo di un articolo per la modalita' della partita.
     *
     * <p>Il profilo viene scelto dal {@link ModeProfile}, quindi non esiste
     * alcun controllo di modalita' sparso nel resto del codice.</p>
     *
     * @param itemId  articolo richiesto
     * @param profile profilo modalita' della partita
     * @return il prezzo, oppure {@code null} se non listato
     */
    public ItemCost resolveCost(String itemId, ModeProfile profile) {
        if (profile == null) {
            return null;
        }
        return resolveCost(itemId, profile.getPricingProfile());
    }

    /**
     * Risolve il prezzo di un articolo in un profilo indicato per nome.
     *
     * @return il prezzo, oppure {@code null} se non listato
     */
    public ItemCost resolveCost(String itemId, String profileId) {
        PricingProfile pricing = getProfile(profileId);
        return pricing == null ? null : pricing.getCost(itemId);
    }

    public PricingProfile getProfile(String profileId) {
        return profileId == null
                ? null : profiles.get(profileId.trim().toLowerCase(Locale.ROOT));
    }

    public ShopItemDefinition getItem(String itemId) {
        return itemId == null
                ? null : itemsById.get(itemId.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Categoria richiesta, oppure la prima disponibile.
     *
     * @return la categoria, oppure {@code null} se il catalogo e' vuoto
     */
    public ShopCategoryDefinition getCategoryOrFirst(String categoryId) {
        if (categoryId != null) {
            for (ShopCategoryDefinition category : categories) {
                if (category.getId().equalsIgnoreCase(categoryId)) {
                    return category;
                }
            }
        }
        return categories.isEmpty() ? null : categories.get(0);
    }

    public List<ShopCategoryDefinition> getCategories() {
        return categories;
    }

    public Map<String, ShopItemDefinition> getItems() {
        return itemsById;
    }

    public Map<String, PricingProfile> getProfiles() {
        return profiles;
    }

    /**
     * Anomalie non fatali rilevate al caricamento, da mostrare agli
     * amministratori.
     */
    public List<String> getWarnings() {
        return warnings;
    }

    public boolean isEmpty() {
        return categories.isEmpty() || itemsById.isEmpty();
    }
}
