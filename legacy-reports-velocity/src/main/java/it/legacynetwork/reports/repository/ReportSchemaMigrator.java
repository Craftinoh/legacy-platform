package it.legacynetwork.reports.repository;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Migrazioni versionate dello schema dei report.
 *
 * <p>Ogni versione e' un file SQL incluso nell'artefatto e viene applicata una
 * sola volta: la tabella {@code legacy_reports_schema_history} registra cosa e'
 * gia' passato. Non esiste alcun downgrade — si aggiunge una versione, non si
 * riscrive quella vecchia.</p>
 */
public final class ReportSchemaMigrator {

    /** Versioni note, in ordine di applicazione. */
    public static final List<String> VERSIONS =
            java.util.Collections.unmodifiableList(Arrays.asList("V1__init"));

    private static final String HISTORY_TABLE =
            "CREATE TABLE IF NOT EXISTS legacy_reports_schema_history ("
                    + "version VARCHAR(64) PRIMARY KEY, "
                    + "applied_at BIGINT NOT NULL)";

    private static final String SELECT_VERSION =
            "SELECT version FROM legacy_reports_schema_history WHERE version = ?";

    private static final String INSERT_VERSION =
            "INSERT INTO legacy_reports_schema_history (version, applied_at) "
                    + "VALUES (?, ?)";

    private final DataSource dataSource;

    public ReportSchemaMigrator(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("DataSource mancante");
        }
        this.dataSource = dataSource;
    }

    /**
     * Applica le migrazioni mancanti.
     *
     * @return il numero di versioni applicate in questa esecuzione
     */
    public int migrate() {
        int applied = 0;
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(HISTORY_TABLE);
            }
            for (String version : VERSIONS) {
                if (isApplied(connection, version)) {
                    continue;
                }
                apply(connection, version);
                applied++;
            }
        } catch (SQLException failure) {
            throw new ReportRepositoryException(
                    "Migrazione dello schema report fallita", failure);
        }
        return applied;
    }

    private boolean isApplied(Connection connection, String version)
            throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement(SELECT_VERSION)) {
            statement.setString(1, version);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    private void apply(Connection connection, String version)
            throws SQLException {
        List<String> statements = readStatements(version);
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (Statement statement = connection.createStatement()) {
                for (String sql : statements) {
                    statement.executeUpdate(sql);
                }
            }
            try (PreparedStatement statement =
                         connection.prepareStatement(INSERT_VERSION)) {
                statement.setString(1, version);
                statement.setLong(2, System.currentTimeMillis());
                statement.executeUpdate();
            }
            connection.commit();
        } catch (SQLException failure) {
            connection.rollback();
            throw failure;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private List<String> readStatements(String version) {
        String resource = "/db/reports/" + version + ".sql";
        try (InputStream stream =
                     ReportSchemaMigrator.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new ReportRepositoryException(
                        "Migrazione mancante nell'artefatto: " + resource);
            }
            return split(readAll(stream));
        } catch (IOException unreadable) {
            throw new ReportRepositoryException(
                    "Migrazione illeggibile: " + resource, unreadable);
        }
    }

    private static String readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = stream.read(chunk)) >= 0) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Divide il file in istruzioni, ignorando i commenti di riga.
     */
    private static List<String> split(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : script.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            current.append(trimmed).append('\n');
            if (trimmed.endsWith(";")) {
                String sql = current.toString().trim();
                statements.add(sql.substring(0, sql.length() - 1).trim());
                current.setLength(0);
            }
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            statements.add(tail);
        }
        return statements;
    }
}
