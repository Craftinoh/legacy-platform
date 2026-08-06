package it.legacynetwork.language;

public enum LanguageProtocolAction {
    LANGUAGE_SYNC(1),
    LANGUAGE_CHANGE_REQUEST(2);

    private final int id;

    LanguageProtocolAction(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static LanguageProtocolAction fromId(int id) throws LanguageProtocolException {
        for (LanguageProtocolAction action : values()) {
            if (action.id == id) {
                return action;
            }
        }
        throw new LanguageProtocolException("Azione protocollo sconosciuta: " + id);
    }
}
