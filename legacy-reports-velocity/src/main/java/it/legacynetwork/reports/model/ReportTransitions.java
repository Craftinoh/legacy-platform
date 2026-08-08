package it.legacynetwork.reports.model;

import it.legacynetwork.reports.api.ReportStatus;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Transizioni ammesse fra gli stati di un report.
 *
 * <p>Tabella unica: comandi e API non decidono nulla per conto proprio, chiedono
 * qui. {@code ACTION_TAKEN} e {@code DISMISSED} non hanno uscite, quindi un
 * report chiuso resta chiuso.</p>
 */
public final class ReportTransitions {

    private static final Map<ReportStatus, Set<ReportStatus>> ALLOWED =
            buildTable();

    private ReportTransitions() {
    }

    private static Map<ReportStatus, Set<ReportStatus>> buildTable() {
        Map<ReportStatus, Set<ReportStatus>> table =
                new EnumMap<>(ReportStatus.class);
        table.put(ReportStatus.OPEN, EnumSet.of(
                ReportStatus.CLAIMED, ReportStatus.INVESTIGATING));
        table.put(ReportStatus.CLAIMED, EnumSet.of(
                ReportStatus.INVESTIGATING, ReportStatus.OPEN));
        table.put(ReportStatus.INVESTIGATING, EnumSet.of(
                ReportStatus.SCREENSHARE, ReportStatus.ACTION_TAKEN,
                ReportStatus.DISMISSED));
        table.put(ReportStatus.SCREENSHARE, EnumSet.of(
                ReportStatus.INVESTIGATING, ReportStatus.ACTION_TAKEN,
                ReportStatus.DISMISSED));
        table.put(ReportStatus.ACTION_TAKEN, EnumSet.noneOf(ReportStatus.class));
        table.put(ReportStatus.DISMISSED, EnumSet.noneOf(ReportStatus.class));
        return Collections.unmodifiableMap(table);
    }

    /**
     * Verifica se il passaggio richiesto e' consentito.
     */
    public static boolean isAllowed(ReportStatus from, ReportStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<ReportStatus> targets = ALLOWED.get(from);
        return targets != null && targets.contains(to);
    }

    /**
     * Stati raggiungibili a partire da quello indicato.
     */
    public static Set<ReportStatus> allowedFrom(ReportStatus from) {
        Set<ReportStatus> targets = from == null ? null : ALLOWED.get(from);
        return targets == null ? Collections.emptySet()
                : Collections.unmodifiableSet(targets);
    }
}
