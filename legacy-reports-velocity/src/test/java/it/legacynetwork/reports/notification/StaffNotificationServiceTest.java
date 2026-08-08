package it.legacynetwork.reports.notification;

import it.legacynetwork.language.Language;
import it.legacynetwork.reports.config.ReportsConfiguration;
import it.legacynetwork.reports.message.ReportLanguageResolver;
import it.legacynetwork.reports.message.ReportMessages;
import it.legacynetwork.reports.message.ReportPresenter;
import it.legacynetwork.reports.api.Report;
import it.legacynetwork.reports.api.ReportStatus;
import it.legacynetwork.reports.support.FakeDirectory;
import it.legacynetwork.reports.support.FakePlayer;
import it.legacynetwork.reports.support.ReportsTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le notifiche vanno solo a chi le vuole, nella lingua di ciascuno.
 */
class StaffNotificationServiceTest {

    private ReportsConfiguration configuration;
    private ReportMessages messages;
    private FakeDirectory directory;
    private StaffNotificationPreferences preferences;

    private FakePlayer italianStaff;
    private FakePlayer englishStaff;

    @BeforeEach
    void setUp() {
        configuration = ReportsTestSupport.configuration();
        messages = ReportMessages.load(configuration.getFallbackLanguage());
        directory = new FakeDirectory();
        preferences = new StaffNotificationPreferences(true);
        italianStaff = new FakePlayer("StaffIt")
                .allow("legacyreports.staff.view");
        englishStaff = new FakePlayer("StaffEn")
                .allow("legacyreports.staff.view");
        directory.add(italianStaff, englishStaff);
    }

    private StaffNotificationService service(
            java.util.function.Function<UUID, Language> perPlayer) {
        ReportPresenter presenter = new ReportPresenter(messages,
                configuration.getReasons());
        ReportLanguageResolver languages = new ReportLanguageResolver(
                perPlayer::apply, configuration.getFallbackLanguage());
        return new StaffNotificationService(directory, presenter, languages,
                preferences, configuration.getPermissions().getStaffView());
    }

    private Report report(UUID reporterId) {
        return ReportsTestSupport.report(reporterId, UUID.randomUUID(),
                ReportStatus.OPEN);
    }

    @Test
    void ogniStafferLeggeNellaPropriaLingua() {
        StaffNotificationService service = service(id ->
                id.equals(englishStaff.uniqueId()) ? Language.ENGLISH
                        : Language.ITALIAN);

        int notified = service.notifyCreated(report(UUID.randomUUID()));

        assertEquals(2, notified);
        assertTrue(italianStaff.text().contains(messages.get(Language.ITALIAN,
                "reports.button.claim")));
        assertTrue(englishStaff.text().contains(messages.get(Language.ENGLISH,
                "reports.button.claim")));
    }

    @Test
    void chiHaSpentoGliAvvisiNonRiceveNulla() {
        preferences.toggle(englishStaff.uniqueId());
        StaffNotificationService service = service(id -> Language.ITALIAN);

        int notified = service.notifyCreated(report(UUID.randomUUID()));

        assertEquals(1, notified);
        assertTrue(englishStaff.received().isEmpty());
    }

    @Test
    void chiHaScrittoIlReportNonVieneAvvisatoDelProprio() {
        StaffNotificationService service = service(id -> Language.ITALIAN);

        int notified = service.notifyCreated(
                report(italianStaff.uniqueId()));

        assertEquals(1, notified);
        assertTrue(italianStaff.received().isEmpty());
    }

    @Test
    void chiNonHaIlPermessoNonRiceveNulla() {
        FakePlayer player = new FakePlayer("Giocatore");
        directory.add(player);
        StaffNotificationService service = service(id -> Language.ITALIAN);

        service.notifyCreated(report(UUID.randomUUID()));

        assertTrue(player.received().isEmpty());
    }

    @Test
    void laNotificaPortaIPulsantiCliccabili() {
        StaffNotificationService service = service(id -> Language.ITALIAN);
        Report report = report(UUID.randomUUID());

        service.notifyCreated(report);

        boolean clickable = italianStaff.received().stream()
                .flatMap(line -> line.getSegments().stream())
                .anyMatch(segment -> segment.isClickable()
                        && segment.getHover().isPresent());
        assertTrue(clickable, "la notifica deve offrire info e claim");
    }
}
