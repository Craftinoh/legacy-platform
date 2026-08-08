package it.legacynetwork.screenshare.config;

/**
 * Nodi di permesso, tutti configurabili.
 */
public final class ScreensharePermissions {

    private final String start;
    private final String stop;
    private final String view;
    private final String note;
    private final String admin;

    private ScreensharePermissions(String start, String stop, String view,
                                   String note, String admin) {
        this.start = start;
        this.stop = stop;
        this.view = view;
        this.note = note;
        this.admin = admin;
    }

    public static ScreensharePermissions fromSection(ConfigSection section) {
        return new ScreensharePermissions(
                section.text("start", "legacyscreenshare.staff.start"),
                section.text("stop", "legacyscreenshare.staff.stop"),
                section.text("view", "legacyscreenshare.staff.view"),
                section.text("note", "legacyscreenshare.staff.note"),
                section.text("admin", "legacyscreenshare.admin"));
    }

    public String getStart() {
        return start;
    }

    public String getStop() {
        return stop;
    }

    public String getView() {
        return view;
    }

    public String getNote() {
        return note;
    }

    public String getAdmin() {
        return admin;
    }
}
