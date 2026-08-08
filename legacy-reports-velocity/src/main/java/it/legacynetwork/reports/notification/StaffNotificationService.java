package it.legacynetwork.reports.notification;

import it.legacynetwork.language.Language;
import it.legacynetwork.reports.message.ChatLine;
import it.legacynetwork.reports.message.ReportLanguageResolver;
import it.legacynetwork.reports.message.ReportPresenter;
import it.legacynetwork.reports.model.Report;
import it.legacynetwork.reports.platform.OnlinePlayer;
import it.legacynetwork.reports.platform.PlayerDirectory;

/**
 * Avvisa lo staff collegato quando nasce un report.
 *
 * <p>Ogni destinatario riceve il messaggio nella propria lingua, risolta una
 * volta per persona: non esiste un testo unico tradotto a caso. Chi ha spento le
 * notifiche non riceve nulla, e chi ha scritto il report non viene avvisato del
 * proprio.</p>
 */
public final class StaffNotificationService {

    private final PlayerDirectory directory;
    private final ReportPresenter presenter;
    private final ReportLanguageResolver languages;
    private final StaffNotificationPreferences preferences;
    private final String viewPermission;

    public StaffNotificationService(PlayerDirectory directory,
                                    ReportPresenter presenter,
                                    ReportLanguageResolver languages,
                                    StaffNotificationPreferences preferences,
                                    String viewPermission) {
        if (directory == null || presenter == null || languages == null
                || preferences == null || viewPermission == null) {
            throw new IllegalArgumentException("Notifiche staff incomplete");
        }
        this.directory = directory;
        this.presenter = presenter;
        this.languages = languages;
        this.preferences = preferences;
        this.viewPermission = viewPermission;
    }

    /**
     * Notifica la creazione di un report.
     *
     * @return il numero di staffer avvisati
     */
    public int notifyCreated(Report report) {
        int notified = 0;
        for (OnlinePlayer staff : directory.withPermission(viewPermission)) {
            if (staff.uniqueId() == null
                    || staff.uniqueId().equals(report.getReporterId())
                    || !preferences.isEnabled(staff.uniqueId())) {
                continue;
            }
            Language language = languages.resolve(staff.uniqueId());
            for (ChatLine line : presenter.notification(language, report)) {
                staff.send(line);
            }
            notified++;
        }
        return notified;
    }
}
