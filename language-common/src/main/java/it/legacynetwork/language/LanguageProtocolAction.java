package it.legacynetwork.language;

public enum LanguageProtocolAction {
    LANGUAGE_SYNC(1),
    LANGUAGE_CHANGE_REQUEST(2),
    LANGUAGE_CHANGE_SUCCESS(3),
    LANGUAGE_CHANGE_ALREADY_SELECTED(4),
    LANGUAGE_CHANGE_COOLDOWN(5),
    LANGUAGE_CHANGE_RATE_LIMITED(6),
    LANGUAGE_CHANGE_ERROR(7);

    private final int id;

    LanguageProtocolAction(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public boolean isLanguageChangeResult() {
        return id >= LANGUAGE_CHANGE_SUCCESS.id
                && id <= LANGUAGE_CHANGE_ERROR.id;
    }

    public static LanguageProtocolAction fromId(int id)
            throws LanguageProtocolException {
        for (LanguageProtocolAction action : values()) {
            if (action.id == id) {
                return action;
            }
        }
        throw new LanguageProtocolException(
                "Azione protocollo sconosciuta: " + id);
    }
}
