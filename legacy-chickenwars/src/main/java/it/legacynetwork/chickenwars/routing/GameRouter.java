package it.legacynetwork.chickenwars.routing;

import it.legacynetwork.chickenwars.mode.MatchMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Seleziona un'istanza e ne prenota atomicamente i posti del party. */
public final class GameRouter {
    private final InstanceRegistry instances;
    private final ReservationService reservations;
    private final long heartbeatTimeoutMillis;
    private final long reservationTtlMillis;

    public GameRouter(InstanceRegistry instances, ReservationService reservations,
                      long heartbeatTimeoutMillis, long reservationTtlMillis) {
        if (instances == null || reservations == null || heartbeatTimeoutMillis <= 0L
                || reservationTtlMillis <= 0L) throw new IllegalArgumentException("Router non valido");
        this.instances = instances; this.reservations = reservations;
        this.heartbeatTimeoutMillis = heartbeatTimeoutMillis;
        this.reservationTtlMillis = reservationTtlMillis;
    }

    public synchronized RouteResult route(UUID playerId, MatchMode mode,
            List<UUID> party, String idempotencyKey, long now) {
        List<UUID> members = party == null || party.isEmpty()
                ? Collections.singletonList(playerId) : new ArrayList<UUID>(party);
        if (!members.contains(playerId)) members.add(0, playerId);
        List<GameInstanceDescriptor> candidates = instances.list(mode);
        boolean capacityExists = false;
        for (GameInstanceDescriptor instance : candidates) {
            int reserved = reservations.reservedSeats(instance.getInstanceId(), now);
            if (instance.getCapacity() >= members.size()) capacityExists = true;
            if (!instance.isRoutable(now, heartbeatTimeoutMillis,
                    reserved + members.size() - 1)) continue;
            List<GameReservation> created = new ArrayList<GameReservation>();
            for (UUID member : members) {
                created.add(reservations.create(member, mode, instance.getInstanceId(),
                        now + reservationTtlMillis,
                        idempotencyKey + ":" + member.toString()));
            }
            return RouteResult.success(instance, created);
        }
        return RouteResult.failure(capacityExists
                ? RouteFailure.NO_INSTANCE : RouteFailure.PARTY_TOO_LARGE);
    }
}
