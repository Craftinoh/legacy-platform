package it.legacynetwork.screenshare.config;
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
