package it.legacynetwork.screenshare.session;

import it.legacynetwork.screenshare.config.ScreenshareConfiguration;

import java.util.Locale;
import java.util.UUID;

/**
 * Decide se un comando del bersaglio puo' passare durante un controllo.
 *
 * <p>Riguarda soltanto i comandi che il proxy vede davvero. I comandi
 * registrati sul server di destinazione non passano da qui e questa classe non
 * finge di conoscerli: bloccarli richiederebbe un protocollo verso il backend
 * che oggi non esiste.</p>
 */
public final class TargetCommandPolicy {

    private final ActiveSessionRegistry registry;
    private final ScreenshareConfiguration configuration;

    public TargetCommandPolicy(ActiveSessionRegistry registry,
                               ScreenshareConfiguration configuration) {
        if (registry == null || configuration == null) {
            throw new IllegalArgumentException(
                    "Politica sui comandi incompleta");
        }
        this.registry = registry;
        this.configuration = configuration;
    }

    /**
     * Indica se il comando puo' essere eseguito.
     *
     * @param playerId chi lo esegue
     * @param commandLine riga digitata, con o senza barra iniziale
     */
    public boolean isAllowed(UUID playerId, String commandLine) {
        if (!registry.isLocked(playerId)) {
            return true;
        }
        String label = label(commandLine);
        return !label.isEmpty()
                && configuration.getAllowedTargetCommands().contains(label);
    }

    /**
     * Etichetta del comando: prima parola, senza barra e senza namespace.
     */
    public static String label(String commandLine) {
        if (commandLine == null) {
            return "";
        }
        String trimmed = commandLine.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        int space = trimmed.indexOf(' ');
        if (space >= 0) {
            trimmed = trimmed.substring(0, space);
        }
        int colon = trimmed.indexOf(':');
        if (colon >= 0) {
            trimmed = trimmed.substring(colon + 1);
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
