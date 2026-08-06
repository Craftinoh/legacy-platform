CREATE TABLE IF NOT EXISTS player_languages (
    player_uuid TEXT PRIMARY KEY,
    language_code TEXT NOT NULL,
    client_locale TEXT,
    revision INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS player_language_changes (
    request_id TEXT PRIMARY KEY,
    player_uuid TEXT NOT NULL,
    old_language TEXT,
    new_language TEXT NOT NULL,
    result TEXT NOT NULL,
    changed_at INTEGER NOT NULL,
    revision INTEGER,
    proxy_id TEXT
);

CREATE INDEX IF NOT EXISTS idx_changes_player_time
    ON player_language_changes(player_uuid, changed_at);

CREATE TABLE IF NOT EXISTS schema_migrations (
    version TEXT PRIMARY KEY,
    applied_at INTEGER NOT NULL
);

INSERT OR IGNORE INTO schema_migrations (version, applied_at)
    VALUES ('V1', CAST(strftime('%s','now') AS INTEGER));
