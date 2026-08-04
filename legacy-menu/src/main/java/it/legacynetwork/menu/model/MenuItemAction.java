package it.legacynetwork.menu.model;

public class MenuItemAction {
    private final String type;
    private final String value;

    public MenuItemAction(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
