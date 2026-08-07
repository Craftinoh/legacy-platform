package it.legacynetwork.chickenwars.persistence;

/** Un addebito non puo' portare il saldo sotto zero. */
public final class InsufficientCoinsException extends RuntimeException {
    public InsufficientCoinsException() {
        super("Saldo Chicken Coins insufficiente");
    }
}
