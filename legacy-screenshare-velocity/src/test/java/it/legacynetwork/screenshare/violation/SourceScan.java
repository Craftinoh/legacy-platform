package it.legacynetwork.screenshare.violation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Lettura dei sorgenti per i controlli strutturali.
 *
 * <p>Alcune promesse — "non esegue comandi", "non applica punizioni" — non si
 * possono verificare con un test funzionale: o si guarda il codice, o si crede
 * a un commento.</p>
 */
final class SourceScan {

    private static final Path SOURCES = Paths.get("src/main/java");

    /**
     * Frammenti che segnalerebbero l'esecuzione di un comando o l'invenzione di
     * un sistema di punizioni.
     */
    private static final String[] FORBIDDEN = {
            "executeasync", "dispatchcommand", "commandmanager().execute",
            "/ban", "tempban", "\"ban ", "punishmentplugin", "issuepunishment"};

    private SourceScan() {
    }

    static List<String> punishmentCalls() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : javaFiles()) {
            List<String> lines = Files.readAllLines(file,
                    StandardCharsets.UTF_8);
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index).toLowerCase(Locale.ROOT);
                if (line.trim().startsWith("*") || line.trim().startsWith("//")
                        || line.trim().startsWith("/*")) {
                    continue;
                }
                for (String fragment : FORBIDDEN) {
                    if (line.contains(fragment)) {
                        offenders.add(file.getFileName() + ":" + (index + 1)
                                + " (" + fragment + ")");
                    }
                }
            }
        }
        return offenders;
    }

    private static List<Path> javaFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(SOURCES)) {
            walk.filter(path -> path.toString().endsWith(".java"))
                    .forEach(files::add);
        }
        return files;
    }
}
