package it.legacynetwork.language.velocity.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerLanguageRepository {

    CompletableFuture<Optional<LanguageState>> find(UUID playerUuid);

    CompletableFuture<ChangeResult> changeLanguage(UUID requestId,
                                                    UUID playerUuid,
                                                    String newLanguage,
                                                    String newLocale,
                                                    String proxyId,
                                                    int cooldownSeconds,
                                                    int maxChangesPerWindow,
                                                    int windowMinutes);

    CompletableFuture<Void> upsertState(UUID playerUuid,
                                         String languageCode,
                                         String clientLocale);

    final class LanguageState {
        public final String languageCode;
        public final String clientLocale;
        public final long revision;

        public LanguageState(String languageCode, String clientLocale, long revision) {
            this.languageCode = languageCode;
            this.clientLocale = clientLocale;
            this.revision = revision;
        }
    }

    final class ChangeResult {
        public final ChangeStatus status;
        public final String languageCode;
        public final String messageCode;

        public ChangeResult(ChangeStatus status, String languageCode, String messageCode) {
            this.status = status;
            this.languageCode = languageCode;
            this.messageCode = messageCode;
        }

        public boolean isAccepted() {
            return status == ChangeStatus.SUCCESS;
        }
    }

    enum ChangeStatus {
        SUCCESS,
        ALREADY_SELECTED,
        OPEN_COOLDOWN,
        CHANGE_COOLDOWN,
        HOURLY_LIMIT,
        UNSUPPORTED_LANGUAGE,
        DATABASE_ERROR
    }
}
