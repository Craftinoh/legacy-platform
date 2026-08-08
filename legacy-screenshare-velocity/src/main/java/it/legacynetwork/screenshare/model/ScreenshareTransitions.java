package it.legacynetwork.screenshare.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Transizioni ammesse fra gli stati di una sessione.
 *
 * <p>Una sessione non torna mai indietro: dagli stati finali non si esce, e
 * {@code ACTIVE} non ridiventa {@code TRANSFERRING}.</p>
 */
public final class ScreenshareTransitions {

    private static final Map<ScreenshareStatus, Set<ScreenshareStatus>> ALLOWED =
            buildTable();

    private ScreenshareTransitions() {
    }

    private static Map<ScreenshareStatus, Set<ScreenshareStatus>> buildTable() {
        Map<ScreenshareStatus, Set<ScreenshareStatus>> table =
                new EnumMap<>(ScreenshareStatus.class);
        table.put(ScreenshareStatus.CREATED, EnumSet.of(
                ScreenshareStatus.TRANSFERRING, ScreenshareStatus.CANCELLED,
                ScreenshareStatus.FAILED, ScreenshareStatus.VIOLATION));
        table.put(ScreenshareStatus.TRANSFERRING, EnumSet.of(
                ScreenshareStatus.ACTIVE, ScreenshareStatus.CANCELLED,
                ScreenshareStatus.FAILED, ScreenshareStatus.VIOLATION));
        table.put(ScreenshareStatus.ACTIVE, EnumSet.of(
                ScreenshareStatus.COMPLETED, ScreenshareStatus.CANCELLED,
                ScreenshareStatus.VIOLATION, ScreenshareStatus.FAILED));
        table.put(ScreenshareStatus.COMPLETED,
                EnumSet.noneOf(ScreenshareStatus.class));
        table.put(ScreenshareStatus.CANCELLED,
                EnumSet.noneOf(ScreenshareStatus.class));
        table.put(ScreenshareStatus.VIOLATION,
                EnumSet.noneOf(ScreenshareStatus.class));
        table.put(ScreenshareStatus.FAILED,
                EnumSet.noneOf(ScreenshareStatus.class));
        return Collections.unmodifiableMap(table);
    }

    public static boolean isAllowed(ScreenshareStatus from,
                                    ScreenshareStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<ScreenshareStatus> targets = ALLOWED.get(from);
        return targets != null && targets.contains(to);
    }

    public static Set<ScreenshareStatus> allowedFrom(ScreenshareStatus from) {
        Set<ScreenshareStatus> targets = from == null ? null : ALLOWED.get(from);
        return targets == null ? Collections.emptySet()
                : Collections.unmodifiableSet(targets);
    }
}
