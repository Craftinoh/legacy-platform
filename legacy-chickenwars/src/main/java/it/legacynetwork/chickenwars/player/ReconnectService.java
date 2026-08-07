package it.legacynetwork.chickenwars.player;

import it.legacynetwork.chickenwars.player.equipment.ArmorTier;
import it.legacynetwork.chickenwars.player.equipment.PlayerEquipmentState;
import it.legacynetwork.chickenwars.player.equipment.ToolTier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stato conservato tra la disconnessione e il rientro nella stessa partita.
 *
 * <p>Viene salvato soltanto cio' che il regolamento dichiara permanente: tier
 * armatura, cesoie, tier piccone e ascia, preset Quick Buy selezionato. Valute,
 * spade acquistate, pozioni, blocchi e consumabili non vengono conservati, e non
 * possono quindi essere recuperati con un ciclo di logout e login.</p>
 *
 * <p>Il ripristino ricostruisce lo stato autorevole, non una copia
 * dell'inventario.</p>
 */
public final class ReconnectService {

    /**
     * Fotografia dei soli elementi permanenti di una sessione.
     */
    public static final class Snapshot {

        private final String arenaId;
        private final String teamId;
        private final ArmorTier armorTier;
        private final boolean ownsShears;
        private final ToolTier pickaxeTier;
        private final ToolTier axeTier;
        private final String quickBuyPreset;
        private final long deathSequence;
        private final long expiresAt;
        private final PlayerState playerState;
        private final int respawnSecondsLeft;

        Snapshot(PlayerSession session, long expiresAt) {
            PlayerEquipmentState state = session.getEquipmentState();
            this.arenaId = session.getArenaId();
            this.teamId = session.getTeamId();
            this.armorTier = state.getArmorTier();
            this.ownsShears = state.ownsShears();
            this.pickaxeTier = state.getPickaxeTier();
            this.axeTier = state.getAxeTier();
            this.quickBuyPreset = state.getSelectedQuickBuyPreset();
            this.deathSequence = session.getDeathSequence();
            this.expiresAt = expiresAt;
            this.playerState = session.getState();
            this.respawnSecondsLeft = session.getRespawnSecondsLeft();
        }

        public String getArenaId() {
            return arenaId;
        }

        public String getTeamId() {
            return teamId;
        }

        public ArmorTier getArmorTier() {
            return armorTier;
        }

        public boolean ownsShears() {
            return ownsShears;
        }

        public ToolTier getPickaxeTier() {
            return pickaxeTier;
        }

        public ToolTier getAxeTier() {
            return axeTier;
        }

        public String getQuickBuyPreset() {
            return quickBuyPreset;
        }

        public long getDeathSequence() {
            return deathSequence;
        }

        public long getExpiresAt() { return expiresAt; }
        public PlayerState getPlayerState() { return playerState; }
        public int getRespawnSecondsLeft() { return respawnSecondsLeft; }
    }

    private final Map<UUID, Snapshot> snapshots = new HashMap<UUID, Snapshot>();

    /**
     * Conserva gli elementi permanenti della sessione che sta terminando.
     */
    public void remember(PlayerSession session) {
        remember(session, Long.MAX_VALUE);
    }

    public void remember(PlayerSession session, long expiresAt) {
        if (session == null) {
            return;
        }
        snapshots.put(session.getPlayerId(), new Snapshot(session, expiresAt));
    }

    /**
     * Indica se esiste uno stato salvato per la stessa arena.
     */
    public boolean canRestore(UUID playerId, String arenaId) {
        Snapshot snapshot = snapshots.get(playerId);
        if (snapshot != null && snapshot.getExpiresAt()
                <= System.currentTimeMillis()) {
            snapshots.remove(playerId);
            return false;
        }
        return snapshot != null && snapshot.getArenaId() != null
                && snapshot.getArenaId().equals(arenaId);
    }

    /**
     * Riapplica lo stato permanente alla nuova sessione.
     *
     * <p>La spada torna al tier base e nessun downgrade viene applicato: il
     * rientro non e' una morte.</p>
     *
     * @return lo stato ripristinato, oppure {@code null} se assente
     */
    public Snapshot restore(PlayerSession session) {
        if (session == null) {
            return null;
        }
        Snapshot snapshot = snapshots.remove(session.getPlayerId());
        if (snapshot == null
                || snapshot.getExpiresAt() <= System.currentTimeMillis()
                || !snapshot.getArenaId().equals(session.getArenaId())) {
            return null;
        }

        PlayerEquipmentState state = session.getEquipmentState();
        state.upgradeArmor(snapshot.getArmorTier());
        if (snapshot.ownsShears()) {
            state.unlockShears();
        }
        state.upgradePickaxe(snapshot.getPickaxeTier());
        state.upgradeAxe(snapshot.getAxeTier());
        state.selectQuickBuyPreset(snapshot.getQuickBuyPreset());

        // Azzera la spada senza toccare i tier degli strumenti.
        state.prepareReconnect();
        session.setTeamId(snapshot.getTeamId());
        session.completeDeath();
        session.setState(snapshot.getPlayerState());
        session.setRespawnSecondsLeft(snapshot.getRespawnSecondsLeft());
        return snapshot;
    }

    /**
     * Dimentica lo stato di un giocatore che non rientrera'.
     */
    public void forget(UUID playerId) {
        snapshots.remove(playerId);
    }

    public boolean hasSnapshot(UUID playerId) {
        Snapshot snapshot = snapshots.get(playerId);
        if (snapshot != null && snapshot.getExpiresAt()
                <= System.currentTimeMillis()) {
            snapshots.remove(playerId);
            return false;
        }
        return snapshot != null;
    }

    /** Rimuove e restituisce le sessioni scadute della sola arena indicata. */
    public java.util.List<UUID> expireArena(String arenaId, long now) {
        java.util.List<UUID> expired = new java.util.ArrayList<UUID>();
        java.util.Iterator<Map.Entry<UUID, Snapshot>> iterator =
                snapshots.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Snapshot> entry = iterator.next();
            Snapshot snapshot = entry.getValue();
            if (snapshot.getExpiresAt() <= now
                    && snapshot.getArenaId().equals(arenaId)) {
                expired.add(entry.getKey());
                iterator.remove();
            }
        }
        return expired;
    }

    public void clear() {
        snapshots.clear();
    }
}
