package it.legacynetwork.language.velocity.repository;

import it.legacynetwork.language.Language;
import it.legacynetwork.language.LanguagePreference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class FileLanguageRepository implements AutoCloseable {
    private final Path storageFile;
    private final ExecutorService executor;
    private final Map<UUID, StoredPreference> preferences = new HashMap<>();

    public FileLanguageRepository(Path dataDirectory) {
        this.storageFile = dataDirectory.resolve("player-languages.properties");
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "network-language-storage");
            thread.setDaemon(true);
            return thread;
        });
    }

    public CompletableFuture<Void> load() {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(storageFile.getParent());
                if (!Files.exists(storageFile)) {
                    return;
                }
                Properties properties = new Properties();
                try (InputStream input = Files.newInputStream(storageFile)) {
                    properties.load(input);
                }
                Map<UUID, StoredPreference> loaded = new HashMap<>();
                for (String name : properties.stringPropertyNames()) {
                    if (!name.endsWith(".language")) {
                        continue;
                    }
                    String uuidText = name.substring(0, name.length() - ".language".length());
                    try {
                        UUID uuid = UUID.fromString(uuidText);
                        Optional<Language> language =
                                Language.findByInput(properties.getProperty(name));
                        String preferenceText =
                                properties.getProperty(uuidText + ".preference", "AUTOMATIC");
                        LanguagePreference preference =
                                LanguagePreference.valueOf(preferenceText);
                        if (language.isPresent()) {
                            loaded.put(uuid, new StoredPreference(language.get(), preference));
                        }
                    } catch (IllegalArgumentException ignored) {
                        // Una voce corrotta non impedisce il caricamento delle altre.
                    }
                }
                synchronized (preferences) {
                    preferences.clear();
                    preferences.putAll(loaded);
                }
            } catch (IOException exception) {
                throw new RepositoryException("Impossibile caricare le preferenze", exception);
            }
        }, executor);
    }

    public Optional<StoredPreference> find(UUID uuid) {
        synchronized (preferences) {
            return Optional.ofNullable(preferences.get(uuid));
        }
    }

    public CompletableFuture<Void> save(UUID uuid,
                                        Language language,
                                        LanguagePreference preference) {
        synchronized (preferences) {
            preferences.put(uuid, new StoredPreference(language, preference));
        }
        return persistSnapshot();
    }

    public CompletableFuture<Void> persistSnapshot() {
        final Map<UUID, StoredPreference> snapshot;
        synchronized (preferences) {
            snapshot = new HashMap<>(preferences);
        }
        return CompletableFuture.runAsync(() -> write(snapshot), executor);
    }

    private void write(Map<UUID, StoredPreference> snapshot) {
        try {
            Files.createDirectories(storageFile.getParent());
            Properties properties = new Properties();
            for (Map.Entry<UUID, StoredPreference> entry : snapshot.entrySet()) {
                String prefix = entry.getKey().toString();
                properties.setProperty(prefix + ".language",
                        entry.getValue().language().getCode());
                properties.setProperty(prefix + ".preference",
                        entry.getValue().preference().name());
            }
            Path temporaryFile = storageFile.resolveSibling(storageFile.getFileName() + ".tmp");
            try (OutputStream output = Files.newOutputStream(temporaryFile)) {
                properties.store(output, "NetworkLanguage player preferences");
            }
            try {
                Files.move(temporaryFile, storageFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, storageFile,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new RepositoryException("Impossibile salvare le preferenze", exception);
        }
    }

    @Override
    public void close() {
        try {
            persistSnapshot().join();
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }
    }

    public record StoredPreference(Language language, LanguagePreference preference) {
    }

    private static final class RepositoryException extends RuntimeException {
        private RepositoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
