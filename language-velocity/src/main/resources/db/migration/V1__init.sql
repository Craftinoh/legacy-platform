CREATE TABLE IF NOT EXISTS player_languages (
    player_uuid UUID PRIMARY KEY,
    language_code VARCHAR(10) NOT NULL,
    client_locale VARCHAR(32),
    revision BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS player_language_changes (
    request_id UUID PRIMARY KEY,
    player_uuid UUID NOT NULL,
    old_language VARCHAR(10),
    new_language VARCHAR(10) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    proxy_id VARCHAR(64),
    result VARCHAR(32) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_changes_player_uuid
    ON player_language_changes(player_uuid);

CREATE INDEX IF NOT EXISTS idx_changes_changed_at
    ON player_language_changes(changed_at);
