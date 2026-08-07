package it.legacynetwork.chickenwars.shop;

import it.legacynetwork.chickenwars.model.ResourceType;

/**
 * Prezzo di un articolo in una specifica valuta di partita.
 */
public final class ItemCost {

    private final ResourceType currency;
    private final int amount;

    public ItemCost(ResourceType currency, int amount) {
        if (currency == null) {
            throw new IllegalArgumentException("Valuta mancante");
        }
        if (amount < 0) {
            throw new IllegalArgumentException(
                    "Prezzo negativo non ammesso: " + amount);
        }
        this.currency = currency;
        this.amount = amount;
    }

    public ResourceType getCurrency() {
        return currency;
    }

    public int getAmount() {
        return amount;
    }

    /**
     * Indica se l'articolo e' gratuito.
     */
    public boolean isFree() {
        return amount == 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemCost)) {
            return false;
        }
        ItemCost that = (ItemCost) other;
        return amount == that.amount && currency == that.currency;
    }

    @Override
    public int hashCode() {
        return 31 * currency.hashCode() + amount;
    }

    @Override
    public String toString() {
        return amount + " " + currency.name();
    }
}
