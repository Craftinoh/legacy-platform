package it.legacynetwork.language;

/**
 * Result returned by the proxy after a backend language-change request.
 */
public enum LanguageChangeResult {
    SUCCESS,
    ALREADY_SELECTED,
    COOLDOWN,
    RATE_LIMITED,
    ERROR;

    public LanguageProtocolAction toProtocolAction() {
        switch (this) {
            case SUCCESS:
                return LanguageProtocolAction.LANGUAGE_CHANGE_SUCCESS;
            case ALREADY_SELECTED:
                return LanguageProtocolAction.LANGUAGE_CHANGE_ALREADY_SELECTED;
            case COOLDOWN:
                return LanguageProtocolAction.LANGUAGE_CHANGE_COOLDOWN;
            case RATE_LIMITED:
                return LanguageProtocolAction.LANGUAGE_CHANGE_RATE_LIMITED;
            case ERROR:
            default:
                return LanguageProtocolAction.LANGUAGE_CHANGE_ERROR;
        }
    }

    public static LanguageChangeResult fromProtocolAction(
            LanguageProtocolAction action) {
        if (action == null) {
            return null;
        }
        switch (action) {
            case LANGUAGE_CHANGE_SUCCESS:
                return SUCCESS;
            case LANGUAGE_CHANGE_ALREADY_SELECTED:
                return ALREADY_SELECTED;
            case LANGUAGE_CHANGE_COOLDOWN:
                return COOLDOWN;
            case LANGUAGE_CHANGE_RATE_LIMITED:
                return RATE_LIMITED;
            case LANGUAGE_CHANGE_ERROR:
                return ERROR;
            default:
                return null;
        }
    }
}
