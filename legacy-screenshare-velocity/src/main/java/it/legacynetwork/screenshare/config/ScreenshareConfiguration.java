package it.legacynetwork.screenshare.config;

import it.legacynetwork.language.Language;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Configurazione di LegacyScreenshare.
 *
 * <p>Nessun server, permesso o durata e' scritto nel codice.</p>
 */
public final class ScreenshareConfiguration {

    private final Language fallbackLanguage;
    private final String proxyId;
    private final String server;
    private final List<String> fallbackServers;
    private final Duration transferTimeout;
    private final Duration maximumSession;
    private final Duration staffReconnectGrace;
    private final Duration restartRecoveryGrace;
    private final StaffDisconnectPolicy staffDisconnectPolicy;
    private final boolean lockStaffServer;
    private final boolean allowMultipleSessionsPerStaff;
    private final Set<String> allowedTargetCommands;
    private final int pageSize;
    private final int historySize;
    private final int noteMaxLength;
    private final ScreensharePermissions permissions;

    private ScreenshareConfiguration(Language fallbackLanguage, String proxyId,
                                     String server,
                                     List<String> fallbackServers,
                                     Duration transferTimeout,
                                     Duration maximumSession,
                                     Duration staffReconnectGrace,
                                     Duration restartRecoveryGrace,
                                     StaffDisconnectPolicy staffDisconnectPolicy,
                                     boolean lockStaffServer,
                                     boolean allowMultipleSessionsPerStaff,
                                     Set<String> allowedTargetCommands,
                                     int pageSize, int historySize,
                                     int noteMaxLength,
                                     ScreensharePermissions permissions) {
        this.fallbackLanguage = fallbackLanguage;
        this.proxyId = proxyId;
        this.server = server;
        this.fallbackServers = fallbackServers;
        this.transferTimeout = transferTimeout;
        this.maximumSession = maximumSession;
        this.staffReconnectGrace = staffReconnectGrace;
        this.restartRecoveryGrace = restartRecoveryGrace;
        this.staffDisconnectPolicy = staffDisconnectPolicy;
        this.lockStaffServer = lockStaffServer;
        this.allowMultipleSessionsPerStaff = allowMultipleSessionsPerStaff;
        this.allowedTargetCommands = allowedTargetCommands;
        this.pageSize = pageSize;
        this.historySize = historySize;
        this.noteMaxLength = noteMaxLength;
        this.permissions = permissions;
    }

    /**
     * Legge l'intero file di configurazione.
     *
     * @throws ScreenshareConfigurationException se contiene un valore che il
     *         plugin non e' in grado di onorare
     */
    public static ScreenshareConfiguration fromRoot(ConfigSection root) {
        ConfigSection screenshare = root.section("screenshare");
        Language fallback = Language
                .findByInput(root.section("language").text("fallback", "en"))
                .orElse(Language.ENGLISH);

        Set<String> allowed = new LinkedHashSet<>();
        for (String command
                : screenshare.list("allowed-target-commands")) {
            allowed.add(command.toLowerCase(Locale.ROOT));
        }

        return new ScreenshareConfiguration(
                fallback,
                screenshare.text("proxy-id", "proxy"),
                screenshare.text("server", ""),
                Collections.unmodifiableList(
                        screenshare.list("fallback-servers")),
                Duration.ofSeconds(Math.max(1L,
                        screenshare.duration("transfer-timeout-seconds", 15L))),
                Duration.ofMinutes(Math.max(1L,
                        screenshare.duration("maximum-session-minutes", 60L))),
                Duration.ofSeconds(Math.max(0L, screenshare.duration(
                        "staff-reconnect-grace-seconds", 60L))),
                Duration.ofSeconds(Math.max(1L, screenshare.duration(
                        "restart-recovery-grace-seconds", 120L))),
                StaffDisconnectPolicy.parse(screenshare.text(
                        "staff-disconnect-policy",
                        StaffDisconnectPolicy.CANCEL.name())),
                screenshare.flag("lock-staff-server", false),
                screenshare.flag("allow-multiple-sessions-per-staff", false),
                Collections.unmodifiableSet(allowed),
                Math.max(1, screenshare.number("page-size", 8)),
                Math.max(1, screenshare.number("history-size", 10)),
                Math.max(0, screenshare.number("note-max-length", 200)),
                ScreensharePermissions.fromSection(
                        screenshare.section("permissions")));
    }

    public Language getFallbackLanguage() {
        return fallbackLanguage;
    }

    public String getProxyId() {
        return proxyId;
    }

    /**
     * Server dedicato ai controlli; vuoto significa funzione non configurata.
     */
    public String getServer() {
        return server;
    }

    /**
     * Server su cui riportare i giocatori alla fine o dopo un errore.
     */
    public List<String> getFallbackServers() {
        return fallbackServers;
    }

    public Duration getTransferTimeout() {
        return transferTimeout;
    }

    public Duration getMaximumSession() {
        return maximumSession;
    }

    public Duration getStaffReconnectGrace() {
        return staffReconnectGrace;
    }

    public Duration getRestartRecoveryGrace() {
        return restartRecoveryGrace;
    }

    public StaffDisconnectPolicy getStaffDisconnectPolicy() {
        return staffDisconnectPolicy;
    }

    /**
     * Se attivo, anche lo staffer resta legato al server di controllo.
     */
    public boolean isLockStaffServer() {
        return lockStaffServer;
    }

    public boolean isAllowMultipleSessionsPerStaff() {
        return allowMultipleSessionsPerStaff;
    }

    /**
     * Comandi proxy che il bersaglio puo' comunque usare durante il controllo.
     */
    public Set<String> getAllowedTargetCommands() {
        return allowedTargetCommands;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getHistorySize() {
        return historySize;
    }

    public int getNoteMaxLength() {
        return noteMaxLength;
    }

    public ScreensharePermissions getPermissions() {
        return permissions;
    }

    /**
     * Indica se il server di controllo e' stato configurato.
     */
    public boolean hasServer() {
        return !server.isEmpty();
    }
}
