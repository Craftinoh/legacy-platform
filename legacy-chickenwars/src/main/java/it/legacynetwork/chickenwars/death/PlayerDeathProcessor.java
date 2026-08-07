package it.legacynetwork.chickenwars.death;

import it.legacynetwork.chickenwars.economy.ResourceInventory;
import it.legacynetwork.chickenwars.economy.ResourceTransfer;
import it.legacynetwork.chickenwars.economy.ResourceTransferService;
import it.legacynetwork.chickenwars.economy.ResourceWallet;
import it.legacynetwork.chickenwars.player.PlayerSession;
import it.legacynetwork.chickenwars.player.equipment.EquipmentService;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Unico orchestratore delle conseguenze di una morte.
 *
 * <p>Morte in combattimento, caduta nel vuoto, danno ambientale e abbandono in
 * combattimento convergono qui: non esistono implementazioni separate per le
 * diverse cause.</p>
 *
 * <p>L'idempotenza si appoggia al marcatore gia' presente nello stato
 * autorevole dell'equipaggiamento: {@code PlayerEquipmentState.applyDeath} e'
 * il solo cancello. Se rifiuta la sequenza, la morte era gia' stata elaborata e
 * nessun altro effetto viene applicato, trasferimento risorse compreso.</p>
 */
public final class PlayerDeathProcessor {

    private final ResourceTransferService transfers;
    private final EquipmentService equipment;

    public PlayerDeathProcessor(ResourceTransferService transfers,
                                EquipmentService equipment) {
        if (transfers == null || equipment == null) {
            throw new IllegalArgumentException(
                    "Servizi morte incompleti");
        }
        this.transfers = transfers;
        this.equipment = equipment;
    }

    /**
     * Elabora una morte lavorando su inventari astratti.
     *
     * <p>Percorso deterministico condiviso da produzione e test: la variante
     * Bukkit si limita a costruire gli adapter.</p>
     *
     * @param context         descrizione della morte
     * @param session         sessione della vittima
     * @param victimInventory inventario della vittima
     * @param killerInventory inventario dell'uccisore, eventualmente nullo
     * @param eligibility     verifica di validita' dell'uccisore
     * @return l'esito, mai nullo
     */
    public DeathOutcome process(DeathContext context, PlayerSession session,
                                ResourceInventory victimInventory,
                                ResourceInventory killerInventory,
                                KillerEligibility eligibility) {
        if (context == null || session == null || victimInventory == null) {
            return DeathOutcome.ignored();
        }
        // Dopo una morte definitiva la sessione non ne apre altre: senza questo
        // controllo un secondo evento otterrebbe una sequenza nuova e
        // ripeterebbe downgrade e trasferimento.
        if (session.isDeathFinalised()) {
            return DeathOutcome.duplicate(session.getDeathSequence());
        }

        final long sequence = session.beginDeath();
        boolean completed = false;
        try {
            // Cancello unico: se la sequenza e' gia' stata applicata allo stato
            // autorevole, l'evento e' un duplicato e non deve produrre effetti.
            if (!equipment.handleDeath(session)) {
                completed = true;
                return DeathOutcome.duplicate(sequence);
            }
            session.addDeath();

            UUID killerId = resolveKiller(context, eligibility);
            ResourceInventory rewarded =
                    killerId == null ? null : killerInventory;

            ResourceTransfer transfer = transfers.transferAdapters(
                    context.getVictimId(), killerId, victimInventory, rewarded,
                    new ResourceTransferService.DeathSequence() {
                        @Override
                        public long nextSequence() {
                            return sequence;
                        }
                    });

            completed = true;
            return DeathOutcome.processed(sequence, transfer,
                    killerId != null && !transfer.isEmpty());
        } finally {
            // La morte va sempre chiusa quando termina la sessione, e comunque
            // se l'elaborazione si interrompe: senza questo la sessione
            // resterebbe bloccata e la morte successiva verrebbe ignorata.
            if (context.closesSession()) {
                session.finaliseDeath();
            }
            if (context.closesSession() || !completed) {
                session.completeDeath();
            }
        }
    }

    /**
     * Elabora una morte a partire dai giocatori Bukkit.
     *
     * <p>Costruisce gli adapter e delega a {@link #process}: non esiste una
     * seconda implementazione della logica.</p>
     *
     * @param context     descrizione della morte
     * @param session     sessione della vittima
     * @param victim      giocatore morto
     * @param killer      candidato uccisore, eventualmente nullo
     * @param eligibility verifica di validita' dell'uccisore
     * @return l'esito, mai nullo
     */
    public DeathOutcome processPlayers(DeathContext context,
                                       PlayerSession session, Player victim,
                                       Player killer,
                                       KillerEligibility eligibility) {
        if (victim == null) {
            return DeathOutcome.ignored();
        }
        ResourceInventory killerInventory = killer == null
                ? null : ResourceWallet.adapterFor(killer);
        return process(context, session, ResourceWallet.adapterFor(victim),
                killerInventory, eligibility);
    }

    /**
     * Applica la validita' dell'uccisore prevista dalla partita.
     *
     * @return l'uccisore da premiare, oppure {@code null}
     */
    private UUID resolveKiller(DeathContext context,
                               KillerEligibility eligibility) {
        UUID candidate = context.getKillerId();
        if (candidate == null) {
            return null;
        }
        KillerEligibility check =
                eligibility == null ? KillerEligibility.NONE : eligibility;
        return check.isEligible(candidate) ? candidate : null;
    }
}
