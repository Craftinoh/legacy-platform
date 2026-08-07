package it.legacynetwork.chickenwars.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/** Applica in ordine le migrazioni elencate nell'indice incluso nel JAR. */
public final class MigrationRunner {
    private static final String ROOT = "db/migration/";
    private final DataSource dataSource;
    private final ClassLoader classLoader;
    public MigrationRunner(DataSource dataSource, ClassLoader classLoader) {
        this.dataSource = dataSource; this.classLoader = classLoader;
    }
    public void migrate() throws SQLException, IOException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE TABLE IF NOT EXISTS cw_schema_history "
                            + "(version VARCHAR(64) PRIMARY KEY, installed_at BIGINT NOT NULL)");
                }
                for (String migration : lines(ROOT + "index.txt")) {
                    if (!installed(connection, migration)) {
                        for (String sql : statements(read(ROOT + migration))) {
                            try (Statement statement = connection.createStatement()) {
                                statement.execute(sql);
                            }
                        }
                        try (PreparedStatement insert = connection.prepareStatement(
                                "INSERT INTO cw_schema_history(version, installed_at) VALUES (?, ?)")) {
                            insert.setString(1, migration); insert.setLong(2, System.currentTimeMillis());
                            insert.executeUpdate();
                        }
                    }
                }
                connection.commit();
            } catch (SQLException failure) {
                connection.rollback(); throw failure;
            } finally { connection.setAutoCommit(true); }
        }
    }
    private boolean installed(Connection connection, String version) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT 1 FROM cw_schema_history WHERE version = ?")) {
            query.setString(1, version);
            try (ResultSet results = query.executeQuery()) { return results.next(); }
        }
    }
    private List<String> lines(String resource) throws IOException {
        List<String> result = new ArrayList<String>();
        for (String line : read(resource).split("\\r?\\n")) {
            String value = line.trim();
            if (!value.isEmpty() && !value.startsWith("#")) result.add(value);
        }
        return result;
    }
    private String read(String resource) throws IOException {
        InputStream input = classLoader.getResourceAsStream(resource);
        if (input == null) throw new IOException("Risorsa migrazione assente: " + resource);
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line).append('\n');
        }
        return result.toString();
    }
    private List<String> statements(String script) {
        List<String> result = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        for (String line : script.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") || trimmed.isEmpty()) continue;
            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                String sql = current.toString().trim();
                result.add(sql.substring(0, sql.length() - 1)); current.setLength(0);
            }
        }
        if (current.toString().trim().length() > 0) result.add(current.toString().trim());
        return result;
    }
}
