package it.legacynetwork.chickenwars.persistence;

public final class ProfileLoadResult {
    public enum Status { LOADED, OFFLINE, TIMED_OUT }
    private final Status status; private final PlayerProfile profile;
    private ProfileLoadResult(Status status,PlayerProfile profile){this.status=status;this.profile=profile;}
    public static ProfileLoadResult loaded(PlayerProfile p){return new ProfileLoadResult(Status.LOADED,p);}
    public static ProfileLoadResult failed(Status s){return new ProfileLoadResult(s,null);}
    public Status getStatus(){return status;}
    public PlayerProfile getProfile(){return profile;}
    public boolean isLoaded(){return status==Status.LOADED;}
}
