package it.legacynetwork.chickenwars.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RouteResult {
    private final RouteFailure failure;
    private final GameInstanceDescriptor instance;
    private final List<GameReservation> reservations;
    private RouteResult(RouteFailure failure, GameInstanceDescriptor instance,
                        List<GameReservation> reservations) {
        this.failure = failure; this.instance = instance;
        this.reservations = Collections.unmodifiableList(
                new ArrayList<GameReservation>(reservations));
    }
    public static RouteResult failure(RouteFailure failure) {
        return new RouteResult(failure, null, Collections.<GameReservation>emptyList());
    }
    public static RouteResult success(GameInstanceDescriptor instance,
                                      List<GameReservation> reservations) {
        return new RouteResult(RouteFailure.NONE, instance, reservations);
    }
    public boolean isSuccessful() { return failure == RouteFailure.NONE; }
    public RouteFailure getFailure() { return failure; }
    public GameInstanceDescriptor getInstance() { return instance; }
    public List<GameReservation> getReservations() { return reservations; }
}
