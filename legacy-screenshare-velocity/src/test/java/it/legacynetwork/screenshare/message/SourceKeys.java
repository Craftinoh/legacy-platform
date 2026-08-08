package it.legacynetwork.screenshare.message;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Lettura dei sorgenti del modulo.
 *
 * <p>Serve a due controlli che nessun test funzionale puo' fare: che ogni
 * chiave citata nel codice esista nei bundle, e che nessuna riga di chat venga
 * costruita con testo scritto in Java.</p>
 */
final class SourceKeys {

    private static final Path SOURCES = Paths.get("src/main/java");

    private static final Pattern KEY = Pattern.compile(
            "\"(screenshare\\.[A-Za-z0-9._-]+)\"");

    /** Costruttori di testo visibile: il primo argomento e' cio' che si legge. */
    private static final String[] VISIBLE_TEXT_FACTORIES = {
            "ChatLine.text(", "ChatSegment.text(", "ChatSegment.hint(",
            "ChatSegment.suggest(", "ChatSegment.run("};

    private SourceKeys() {
    }

    static Map<String, String> literalKeys() throws IOException {
        Map<String, String> keys = new LinkedHashMap<>();
        for (Path file : javaFiles()) {
            for (String line : Files.readAllLines(file,
                    StandardCharsets.UTF_8)) {
                Matcher matcher = KEY.matcher(line);
                while (matcher.find()) {
                    String key = matcher.group(1);
                    if (!key.endsWith(".")) {
                        keys.putIfAbsent(key, file.getFileName().toString());
                    }
                }
            }
        }
        return keys;
    }

    static List<String> hardcodedChatText() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : javaFiles()) {
            List<String> lines = Files.readAllLines(file,
                    StandardCharsets.UTF_8);
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                for (String factory : VISIBLE_TEXT_FACTORIES) {
                    int at = line.indexOf(factory);
                    while (at >= 0) {
                        String literal = literalAt(line,
                                at + factory.length());
                        // Uno spazio non appartiene ad alcuna lingua.
                        if (literal != null && !literal.trim().isEmpty()) {
                            offenders.add(file.getFileName() + ":"
                                    + (index + 1));
                        }
                        at = line.indexOf(factory, at + 1);
                    }
                }
            }
        }
        return offenders;
    }

    private static String literalAt(String line, int start) {
        if (start >= line.length() || line.charAt(start) != '"') {
            return null;
        }
        int end = line.indexOf('"', start + 1);
        return end < 0 ? "" : line.substring(start + 1, end);
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
