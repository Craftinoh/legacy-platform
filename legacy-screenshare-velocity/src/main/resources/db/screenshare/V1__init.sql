-- LegacyScreenshare - schema iniziale.
--
-- Stesse scelte dello schema dei report: identificatori come esadecimale
-- compatto, istanti come epoch millis UTC, SQL portabile. Nessuna
-- cancellazione: una sessione sbagliata viene chiusa, mai rimossa, e lo storico
-- e' di sola aggiunta.

CREATE TABLE IF NOT EXISTS legacy_screenshare_sessions (
    id VARCHAR(32) PRIMARY KEY,
    target_uuid VARCHAR(32) NOT NULL,
    target_name VARCHAR(32) NOT NULL,
    staff_uuid VARCHAR(32) NOT NULL,
    staff_name VARCHAR(32) NOT NULL,
    report_id VARCHAR(32),
    server_id VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    started_at BIGINT,
    expires_at BIGINT NOT NULL,
    ended_at BIGINT,
    status VARCHAR(32) NOT NULL,
    outcome VARCHAR(32),
    notes VARCHAR(1024),
    proxy_id VARCHAR(64) NOT NULL,
    revision BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_legacy_screenshare_target
    ON legacy_screenshare_sessions (target_uuid, created_at);

CREATE INDEX IF NOT EXISTS idx_legacy_screenshare_staff
    ON legacy_screenshare_sessions (staff_uuid, created_at);

CREATE INDEX IF NOT EXISTS idx_legacy_screenshare_status
    ON legacy_screenshare_sessions (status, created_at);

CREATE INDEX IF NOT EXISTS idx_legacy_screenshare_report
    ON legacy_screenshare_sessions (report_id);

CREATE TABLE IF NOT EXISTS legacy_screenshare_events (
    id VARCHAR(32) PRIMARY KEY,
    session_id VARCHAR(32) NOT NULL,
    actor_uuid VARCHAR(32),
    actor_name VARCHAR(32) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32),
    message VARCHAR(512),
    proxy_id VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_legacy_screenshare_events_session
    ON legacy_screenshare_events (session_id, created_at);

CREATE INDEX IF NOT EXISTS idx_legacy_screenshare_events_created
    ON legacy_screenshare_events (created_at);
