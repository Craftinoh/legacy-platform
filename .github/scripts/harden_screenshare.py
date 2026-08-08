from pathlib import Path
import re
R=Path(__file__).resolve().parents[2]
def f(p): return (R/p).read_text(encoding='utf-8')
def w(p,s): (R/p).write_text(s,encoding='utf-8',newline='\n')
def x(s,a,b,n):
    if a not in s: raise RuntimeError('missing '+n)
    return s.replace(a,b,1)
def q(s,a,b,n):
    z,c=re.subn(a,b,s,count=1,flags=re.S)
    if c!=1: raise RuntimeError(f'missing {n}: {c}')
    return z

p='legacy-screenshare-velocity/src/main/java/it/legacynetwork/screenshare/config/ScreenshareConfiguration.java';s=f(p)
s=x(s,'    private final Duration staffReconnectGrace;\n    private final StaffDisconnectPolicy staffDisconnectPolicy;','    private final Duration staffReconnectGrace;\n    private final Duration restartRecoveryGrace;\n    private final StaffDisconnectPolicy staffDisconnectPolicy;','cfg field')
s=x(s,'                                     Duration staffReconnectGrace,\n                                     StaffDisconnectPolicy staffDisconnectPolicy,','                                     Duration staffReconnectGrace,\n                                     Duration restartRecoveryGrace,\n                                     StaffDisconnectPolicy staffDisconnectPolicy,','cfg arg')
s=x(s,'        this.staffReconnectGrace = staffReconnectGrace;\n        this.staffDisconnectPolicy = staffDisconnectPolicy;','        this.staffReconnectGrace = staffReconnectGrace;\n        this.restartRecoveryGrace = restartRecoveryGrace;\n        this.staffDisconnectPolicy = staffDisconnectPolicy;','cfg assign')
s=x(s,'                Duration.ofSeconds(Math.max(0L, screenshare.duration(\n                        "staff-reconnect-grace-seconds", 60L))),\n                StaffDisconnectPolicy.parse(screenshare.text(','                Duration.ofSeconds(Math.max(0L, screenshare.duration(\n                        "staff-reconnect-grace-seconds", 60L))),\n                Duration.ofSeconds(Math.max(1L, screenshare.duration(\n                        "restart-recovery-grace-seconds", 120L))),\n                StaffDisconnectPolicy.parse(screenshare.text(','cfg parse')
s=x(s,'    public Duration getStaffReconnectGrace() {\n        return staffReconnectGrace;\n    }\n\n    public StaffDisconnectPolicy getStaffDisconnectPolicy() {','    public Duration getStaffReconnectGrace() {\n        return staffReconnectGrace;\n    }\n\n    public Duration getRestartRecoveryGrace() {\n        return restartRecoveryGrace;\n    }\n\n    public StaffDisconnectPolicy getStaffDisconnectPolicy() {','cfg getter');w(p,s)

p='legacy-screenshare-velocity/src/main/resources/config.yml';s=f(p)
s=x(s,'  staff-reconnect-grace-seconds: 60\n\n  # Cosa fare se lo staffer si scollega durante un controllo.','  staff-reconnect-grace-seconds: 60\n\n  # Finestra concessa alle sessioni ACTIVE dopo un riavvio del proxy.\n  restart-recovery-grace-seconds: 120\n\n  # Cosa fare se lo staffer si scollega durante un controllo.','yaml recovery')
s=s.replace("  # TRANSFER_TO_AVAILABLE_STAFF non e' implementato e viene rifiutato:\n  # richiederebbe una coda reale di staff disponibili, che non esiste.\n",'');w(p,s)

w('legacy-screenshare-velocity/src/main/java/it/legacynetwork/screenshare/config/StaffDisconnectPolicy.java','''package it.legacynetwork.screenshare.config;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
public enum StaffDisconnectPolicy {
    CANCEL,
    KEEP_ACTIVE_FOR_SECONDS;
    public static List<String> supportedNames() {
        List<String> names=new ArrayList<>();
        for (StaffDisconnectPolicy policy:values()) names.add(policy.name());
        return names;
    }
    public static StaffDisconnectPolicy parse(String raw) {
        String value=raw==null?"":raw.trim().toUpperCase(Locale.ROOT).replace('-','_');
        for (StaffDisconnectPolicy policy:values()) if(policy.name().equals(value)) return policy;
        throw new ScreenshareConfigurationException("screenshare.staff-disconnect-policy: valore sconosciuto '"+raw+"'; valori supportati "+supportedNames());
    }
}
''')

p='legacy-screenshare-velocity/src/main/java/it/legacynetwork/screenshare/model/ScreenshareEventType.java';s=f(p);s=x(s,'    TIMED_OUT,\n    RECOVERED;','    TIMED_OUT,\n    RECOVERED,\n    RECOVERY_STARTED,\n    RECOVERY_COMPLETED,\n    RECOVERY_TIMED_OUT;','events');w(p,s)
p='legacy-screenshare-velocity/src/main/java/it/legacynetwork/screenshare/violation/ScreenshareViolationType.java';s=f(p);s=x(s,'    TARGET_LEFT_DURING_TRANSFER,\n    /** Lo staffer ha chiuso il controllo dichiarando una violazione. */','    TARGET_LEFT_DURING_TRANSFER,\n    /** Il bersaglio non e\' rientrato entro la finestra dopo un restart. */\n    TARGET_MISSING_AFTER_RECOVERY,\n    /** Lo staffer ha chiuso il controllo dichiarando una violazione. */','violation');w(p,s)

p='legacy-screenshare-velocity/src/main/java/it/legacynetwork/screenshare/LegacyScreensharePlugin.java';s=f(p)
s=x(s,'    private ExecutorService databaseExecutor;\n    private ActiveSessionRegistry registry;','    private ExecutorService databaseExecutor;\n    private ActiveSessionRegistry registry;\n    private ScreenshareService service;','plugin field')
s=x(s,'        ScreenshareService service = new ScreenshareService(configuration,','        service = new ScreenshareService(configuration,','plugin assign')
s=x(s,'    public void onShutdown(ProxyShutdownEvent event) {\n        if (registry != null) {','    public void onShutdown(ProxyShutdownEvent event) {\n        if (service != null) service.beginShutdown();\n        if (registry != null) {','plugin shutdown')
s=x(s,'            dataSource = null;\n        }\n    }','            dataSource = null;\n        }\n        service = null;\n    }','plugin clear');w(p,s)

w('legacy-screenshare-velocity/src/main/java/it/legacynetwork/screenshare/velocity/ConnectionListener.java','''package it.legacynetwork.screenshare.velocity;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import it.legacynetwork.screenshare.service.ScreenshareService;
import it.legacynetwork.screenshare.session.ActiveSessionRegistry;
import java.util.UUID;
public final class ConnectionListener {
    private final ScreenshareService service;
    private final ActiveSessionRegistry registry;
    public ConnectionListener(ScreenshareService service, ActiveSessionRegistry registry) { this.service=service; this.registry=registry; }
    @Subscribe public void onDisconnect(DisconnectEvent event) {
        if(service.isShuttingDown()) return;
        UUID id=event.getPlayer().getUniqueId();
        if(registry.isLocked(id)) { service.onTargetDisconnect(id); return; }
        if(registry.sessionOfStaff(id).isPresent()) service.onStaffDisconnect(id);
    }
    @Subscribe public void onPostLogin(PostLoginEvent event) { service.onPlayerReconnect(event.getPlayer().getUniqueId()); }
    @Subscribe public void onServerPostConnect(ServerPostConnectEvent event) {
        UUID id=event.getPlayer().getUniqueId();
        service.onPlayerReconnect(id);
        if(registry.sessionOfStaff(id).isPresent()) service.onStaffReconnect(id);
    }
}
''')

p='legacy-screenshare-velocity/src/main/java/it/legacynetwork/screenshare/service/ScreenshareService.java';s=f(p)
s=x(s,'    /** Staff scollegati durante un controllo, in attesa di rientro. */\n    private final Map<UUID, Instant> staffAwaySince = new ConcurrentHashMap<>();','    /** Staff scollegati durante un controllo, in attesa di rientro. */\n    private final Map<UUID, Instant> staffAwaySince = new ConcurrentHashMap<>();\n    private final Map<ScreenshareSessionId, Instant> recoveryDeadlines = new ConcurrentHashMap<>();\n    private final Map<UUID, String> originalServers = new ConcurrentHashMap<>();\n    private final Set<ScreenshareSessionId> cleanupStarted = ConcurrentHashMap.newKeySet();\n    private volatile boolean shuttingDown;','service fields')
s=x(s,'    private CompletableFuture<ScreenshareOperationResult> create(\n            OnlinePlayer staff, OnlinePlayer target, ReportId reportId) {\n        Instant now = clock.get();','    private CompletableFuture<ScreenshareOperationResult> create(\n            OnlinePlayer staff, OnlinePlayer target, ReportId reportId) {\n        rememberOriginalServer(staff);\n        rememberOriginalServer(target);\n        Instant now = clock.get();','origins')
s=x(s,'    public CompletableFuture<ScreenshareOperationResult> onTargetDisconnect(\n            UUID targetId) {\n        return sessions.findOpenByTarget(targetId)','    public CompletableFuture<ScreenshareOperationResult> onTargetDisconnect(\n            UUID targetId) {\n        if (shuttingDown) return done(ScreenshareOperationResult.failure(ScreenshareOperationStatus.NO_SESSION));\n        return sessions.findOpenByTarget(targetId)','target shutdown')
s=x(s,'                    ScreenshareSession session = found.get();\n                    ScreenshareViolationType type =','                    ScreenshareSession session = found.get();\n                    if (recoveryDeadlines.containsKey(session.getId())) return done(ScreenshareOperationResult.unchanged(session, "screenshare.success.unchanged"));\n                    ScreenshareViolationType type =','target recovery')
s=x(s,'    public CompletableFuture<ScreenshareOperationResult> onStaffDisconnect(\n            UUID staffId) {\n        return sessions.findOpenByStaff(staffId)','    public CompletableFuture<ScreenshareOperationResult> onStaffDisconnect(\n            UUID staffId) {\n        if (shuttingDown) return done(ScreenshareOperationResult.failure(ScreenshareOperationStatus.NO_SESSION));\n        return sessions.findOpenByStaff(staffId)','staff shutdown')
s=x(s,'                    ScreenshareSession session = open.get(0);\n                    if (configuration.getStaffDisconnectPolicy()','                    ScreenshareSession session = open.get(0);\n                    if (recoveryDeadlines.containsKey(session.getId())) return done(ScreenshareOperationResult.unchanged(session, "screenshare.success.unchanged"));\n                    if (configuration.getStaffDisconnectPolicy()','staff recovery')
needle='    /**\n     * Registra il blocco di un cambio server nello storico.\n     */'
add='''    public void beginShutdown() { shuttingDown = true; }
    public boolean isShuttingDown() { return shuttingDown; }
    public CompletableFuture<Integer> onPlayerReconnect(UUID playerId) {
        if (shuttingDown) return done(0);
        return sessions.findOpenByTarget(playerId).thenCompose(target -> {
            if (target.isPresent()) return tryCompleteRecovery(target.get(), clock.get()).thenApply(done -> done ? 1 : 0);
            return sessions.findOpenByStaff(playerId).thenCompose(open -> open.isEmpty() ? done(0) : tryCompleteRecovery(open.get(0), clock.get()).thenApply(done -> done ? 1 : 0));
        }).exceptionally(failure -> 0);
    }

''';s=x(s,needle,add+needle,'reconnect api')
s=x(s,'    private CompletableFuture<Boolean> tickSession(ScreenshareSession session,\n                                                   Instant now) {\n        if (session.getStatus() != ScreenshareStatus.ACTIVE) {','    private CompletableFuture<Boolean> tickSession(ScreenshareSession session,\n                                                   Instant now) {\n        if (recoveryDeadlines.containsKey(session.getId())) return tryCompleteRecovery(session, now);\n        if (session.getStatus() != ScreenshareStatus.ACTIVE) {','tick')
pat=r'''    /\*\*\n     \* Ripristino all'avvio:.*?\n    private CompletableFuture<Boolean> recoverSession\(\n            ScreenshareSession session\) \{.*?\n    \}\n\n    // -------------------------------------------------------------- chiusure'''
rep='''    /** Ripristina senza interpretare l'offline iniziale come abbandono. */
    public CompletableFuture<Integer> recover() {
        shuttingDown=false; registry.clear(); staffAwaySince.clear(); recoveryDeadlines.clear(); originalServers.clear(); cleanupStarted.clear();
        return sessions.findOpen().thenCompose(open -> {
            CompletableFuture<Integer> chain=CompletableFuture.completedFuture(0);
            for(ScreenshareSession session:open) chain=chain.thenCompose(count -> recoverSession(session).thenApply(closed -> count+(closed?1:0)));
            return chain;
        }).exceptionally(failure -> 0);
    }
    private CompletableFuture<Boolean> recoverSession(ScreenshareSession session) {
        Instant now=clock.get();
        if(session.getStatus()!=ScreenshareStatus.ACTIVE) return apply(session,ScreenshareStatus.FAILED,b -> b.endedAt(now).outcome(ScreenshareOutcome.FAILED),ScreenshareEventType.RECOVERED,null,"system","screenshare.audit.recovered","screenshare.success.failed").thenCompose(result -> after(result,session,"screenshare.outcome.failed",ReportEventType.SCREENSHARE_FAILED,"screenshare.staff.session-failed","screenshare.target.session-ended")).thenApply(result -> true);
        registry.lock(session.getTargetId(),session.getId(),session.getServerId()); registry.assignStaff(session.getStaffId(),session.getId());
        recoveryDeadlines.put(session.getId(),now.plus(configuration.getRestartRecoveryGrace()));
        boolean both=directory.findById(session.getTargetId()).isPresent() && directory.findById(session.getStaffId()).isPresent();
        if(both) return completeRecovery(session).thenApply(v -> false);
        return events.append(event(session,ScreenshareEventType.RECOVERY_STARTED,null,"system",null,null,"screenshare.audit.recovery-started")).thenApply(v -> false);
    }
    private CompletableFuture<Boolean> tryCompleteRecovery(ScreenshareSession session, Instant now) {
        Instant deadline=recoveryDeadlines.get(session.getId()); if(deadline==null) return done(false);
        boolean target=directory.findById(session.getTargetId()).isPresent(), staff=directory.findById(session.getStaffId()).isPresent();
        if(target && staff) return completeRecovery(session).thenApply(v -> false);
        if(!now.isAfter(deadline) || !recoveryDeadlines.remove(session.getId(),deadline)) return done(false);
        return closeAfterRecoveryTimeout(session,!target).thenApply(ScreenshareOperationResult::isApplied);
    }
    private CompletableFuture<Void> completeRecovery(ScreenshareSession session) {
        Instant deadline=recoveryDeadlines.get(session.getId());
        if(deadline==null || !recoveryDeadlines.remove(session.getId(),deadline)) return CompletableFuture.completedFuture(null);
        registry.lock(session.getTargetId(),session.getId(),session.getServerId()); registry.assignStaff(session.getStaffId(),session.getId());
        return events.append(event(session,ScreenshareEventType.RECOVERY_COMPLETED,null,"system",null,null,"screenshare.audit.recovery-completed")).thenApply(v -> null);
    }
    private CompletableFuture<ScreenshareOperationResult> closeAfterRecoveryTimeout(ScreenshareSession session, boolean targetMissing) {
        Instant now=clock.get(); ScreenshareStatus status=targetMissing?ScreenshareStatus.VIOLATION:ScreenshareStatus.CANCELLED; ScreenshareOutcome outcome=targetMissing?ScreenshareOutcome.VIOLATION:ScreenshareOutcome.CANCELLED;
        return apply(session,status,b -> b.endedAt(now).outcome(outcome),ScreenshareEventType.RECOVERY_TIMED_OUT,null,"system","screenshare.audit.recovery-timed-out",targetMissing?"screenshare.success.violation":"screenshare.success.cancelled").thenCompose(result -> {
            if(!result.isApplied()) return done(result); ScreenshareSession closed=result.getSession().orElse(session);
            if(targetMissing) violations.handle(violation(closed,ScreenshareViolationType.TARGET_MISSING_AFTER_RECOVERY,now));
            return after(result,session,targetMissing?"screenshare.outcome.violation":"screenshare.outcome.cancelled",targetMissing?ReportEventType.SCREENSHARE_VIOLATION:ReportEventType.SCREENSHARE_CANCELLED,targetMissing?"screenshare.staff.target-disconnected":"screenshare.staff.session-cancelled","screenshare.target.session-ended");
        });
    }

    // -------------------------------------------------------------- chiusure''';s=q(s,pat,rep,'recover')
pat=r'''    /\*\*\n     \* Rimuove i vincoli e riporta il bersaglio su un server di rientro\.\n     \*/\n    private CompletableFuture<Void> cleanup\(ScreenshareSession session\) \{.*?\n    \}\n\n    // -------------------------------------------------------------- infrastr\.'''
rep='''    /** Cleanup idempotente e compensazione di entrambe le parti. */
    private CompletableFuture<Void> cleanup(ScreenshareSession session) {
        if(!cleanupStarted.add(session.getId())) return CompletableFuture.completedFuture(null);
        registry.allowCleanup(session.getTargetId()); registry.releaseStaff(session.getStaffId()); staffAwaySince.remove(session.getStaffId()); recoveryDeadlines.remove(session.getId());
        return returnPlayer(session.getTargetId()).thenCompose(v -> returnPlayer(session.getStaffId())).handle((v,e) -> null).thenApply(v -> { registry.unlock(session.getTargetId()); originalServers.remove(session.getTargetId()); originalServers.remove(session.getStaffId()); return null; });
    }
    private void rememberOriginalServer(OnlinePlayer player) { String server=player.serverId(); if(server!=null && !server.trim().isEmpty()) originalServers.putIfAbsent(player.uniqueId(),server.trim()); }
    private CompletableFuture<Void> returnPlayer(UUID playerId) {
        if(!directory.findById(playerId).isPresent()) return CompletableFuture.completedFuture(null);
        List<String> candidates=new ArrayList<>(); String original=originalServers.get(playerId); if(original!=null && !original.isEmpty()) candidates.add(original);
        for(String fallback:configuration.getFallbackServers()) if(!candidates.contains(fallback)) candidates.add(fallback);
        return transferToFirstAvailable(playerId,candidates,0);
    }
    private CompletableFuture<Void> transferToFirstAvailable(UUID playerId,List<String> candidates,int index) {
        if(index>=candidates.size() || !directory.findById(playerId).isPresent()) return CompletableFuture.completedFuture(null);
        String server=candidates.get(index); if(!transfers.isRegistered(server)) return transferToFirstAvailable(playerId,candidates,index+1);
        return transfers.transfer(playerId,server).handle((moved,failure) -> failure==null && Boolean.TRUE.equals(moved)).thenCompose(moved -> moved?CompletableFuture.completedFuture(null):transferToFirstAvailable(playerId,candidates,index+1));
    }

    // -------------------------------------------------------------- infrastr.''';s=q(s,pat,rep,'cleanup');w(p,s)

p='build.gradle.kts';s=f(p);s=x(s,'        ":language-common:build",\n        ":language-velocity:build",\n        ":legacy-lobby:build",','        ":language-common:build",\n        ":language-velocity:build",\n        ":language-backend:build",\n        ":chickenwars-common:build",\n        ":chickenwars-velocity:build",\n        ":legacy-reports-velocity:build",\n        ":legacy-screenshare-velocity:build",\n        ":legacy-lobby:build",','root build');w(p,s)

T={'en':('did not return before the restart recovery window expired','restart recovery window opened','restart recovery completed','restart recovery window expired'),'it':("non e' rientrato entro la finestra di recupero dopo il riavvio",'finestra di recupero dopo il riavvio aperta','recupero dopo il riavvio completato','finestra di recupero dopo il riavvio scaduta'),'es':('no regreso antes de que terminara la recuperacion tras el reinicio','ventana de recuperacion tras reinicio abierta','recuperacion tras reinicio completada','ventana de recuperacion tras reinicio agotada')}
for code,v in T.items():
 p=f'legacy-screenshare-velocity/src/main/resources/screenshare/translations/messages_{code}.properties';s=f(p)
 s=x(s,'screenshare.violation.target-left-during-transfer=',f'screenshare.violation.target-missing-after-recovery={v[0]}\nscreenshare.violation.target-left-during-transfer=',code+' violation') if 'target-missing-after-recovery' not in s else s
 if 'screenshare.audit.recovery-started=' not in s: s+=f'screenshare.audit.recovery-started={v[1]}\nscreenshare.audit.recovery-completed={v[2]}\nscreenshare.audit.recovery-timed-out={v[3]}\n'
 w(p,s)

p='legacy-screenshare-velocity/src/test/java/it/legacynetwork/screenshare/config/ScreenshareConfigurationTest.java';s=f(p)
s=x(s,'        assertEquals(60L, configuration.getStaffReconnectGrace().getSeconds());','        assertEquals(60L, configuration.getStaffReconnectGrace().getSeconds());\n        assertEquals(120L, configuration.getRestartRecoveryGrace().getSeconds());','cfg test')
s=q(s,r'''\n    @Test\n    void ilTrasferimentoAdAltroStaffVieneRifiutato\(\) \{.*?\n    \}\n''','\n','remove fake option')
s=q(s,r'''    @Test\n    void soloLePoliticheImplementateSonoDichiarate\(\) \{.*?\n    \}\n''','''    @Test
    void sonoDichiarateSoloLePoliticheSupportate() {
        assertEquals(java.util.List.of("CANCEL", "KEEP_ACTIVE_FOR_SECONDS"), StaffDisconnectPolicy.supportedNames());
        assertEquals(2, StaffDisconnectPolicy.values().length);
    }
''','policy test');w(p,s)

p='legacy-screenshare-velocity/src/test/java/it/legacynetwork/screenshare/service/ScreenshareTransferTest.java';s=f(p)
s=x(s,'        assertEquals(ScreenshareStatus.FAILED,\n                result.getSession().orElseThrow().getStatus());\n    }\n\n    @Test\n    void unFallimentoTecnicoNonProduceUnaViolazione()','        assertEquals(ScreenshareStatus.FAILED,\n                result.getSession().orElseThrow().getStatus());\n        assertTrue(world.transfers.movedTo(staff.uniqueId(), "lobby-1"));\n    }\n\n    @Test\n    void unFallimentoTecnicoNonProduceUnaViolazione()','partial transfer test')
s=x(s,'        assertTrue(world.transfers.movedTo(target.uniqueId(), "lobby-1"));\n        assertFalse(world.transfers.movedTo(target.uniqueId(), "lobby-2"),','        assertTrue(world.transfers.movedTo(target.uniqueId(), "lobby-1"));\n        assertTrue(world.transfers.movedTo(staff.uniqueId(), "lobby-1"));\n        assertFalse(world.transfers.movedTo(target.uniqueId(), "lobby-2"),','normal cleanup test');w(p,s)

p='legacy-screenshare-velocity/src/test/java/it/legacynetwork/screenshare/session/ScreenshareEventsTest.java';s=f(p)
pat=r'''    @Test\n    void ilRipristinoChiudeLeSessioniAppese\(\) \{.*?\n    \}\n\n    @Test\n    void ilRipristinoRicostruisceIVincoliDelleSessioniValide\(\) \{.*?\n    \}\n'''
rep='''    @Test
    void ilRipristinoNonAccusaSubitoChiEuAncoraOffline() {
        ScreenshareSession session=start(); world.directory.remove(target); assertEquals(0,world.recoverWithFreshRegistry());
        assertEquals(ScreenshareStatus.ACTIVE,world.service.find(session.getId()).join().orElseThrow().getStatus()); assertEquals(0,world.violations.count()); assertTrue(world.registry.isLocked(target.uniqueId()));
    }
    @Test
    void ilRientroDiEntrambeLePartiCompletaIlRecoveryUnaSolaVolta() {
        ScreenshareSession session=start(); world.directory.remove(target); world.directory.remove(staff); world.recoverWithFreshRegistry();
        world.directory.add(target); world.service.onPlayerReconnect(target.uniqueId()).join(); world.directory.add(staff); world.service.onPlayerReconnect(staff.uniqueId()).join(); world.service.onPlayerReconnect(staff.uniqueId()).join();
        assertEquals(ScreenshareStatus.ACTIVE,world.service.find(session.getId()).join().orElseThrow().getStatus());
        assertEquals(1L,world.events.all().stream().filter(e -> e.getSessionId().equals(session.getId())).filter(e -> e.getType()==it.legacynetwork.screenshare.model.ScreenshareEventType.RECOVERY_COMPLETED).count());
    }
    @Test
    void laScadenzaRecoveryDistingueLAssenzaDelBersaglio() {
        ScreenshareSession session=start(); world.directory.remove(target); world.recoverWithFreshRegistry(); world.advance(Duration.ofSeconds(121)); assertEquals(1,world.service.tick().join());
        assertEquals(ScreenshareStatus.VIOLATION,world.service.find(session.getId()).join().orElseThrow().getStatus()); assertEquals(ScreenshareViolationType.TARGET_MISSING_AFTER_RECOVERY,world.violations.last().getType());
    }
    @Test
    void loShutdownNonGeneraViolazioni() {
        ScreenshareSession session=start(); world.service.beginShutdown(); world.directory.remove(target); world.service.onTargetDisconnect(target.uniqueId()).join(); world.service.onStaffDisconnect(staff.uniqueId()).join();
        assertEquals(ScreenshareStatus.ACTIVE,world.service.find(session.getId()).join().orElseThrow().getStatus()); assertEquals(0,world.violations.count());
    }
    @Test
    void ilRipristinoRicostruisceIVincoliDelleSessioniValide() {
        ScreenshareSession session=start(); assertEquals(0,world.recoverWithFreshRegistry()); assertTrue(world.registry.isLocked(target.uniqueId())); assertEquals(ScreenshareStatus.ACTIVE,world.service.find(session.getId()).join().orElseThrow().getStatus());
    }
''';s=q(s,pat,rep,'recovery tests');w(p,s)

Path(__file__).unlink()
