-- LegacyReports - schema iniziale.
--
-- Lo schema e' scritto in SQL portabile: identificatori come esadecimale
-- compatto, istanti come epoch millis UTC, nessuna estensione specifica di un
-- singolo motore. Cosi' le stesse istruzioni girano sul PostgreSQL di
-- produzione e sul database di prova usato dai test.
--
-- Non esiste alcuna cancellazione: un report sbagliato viene chiuso, mai
-- rimosso, e lo storico e' di sola aggiunta.

CREATE TABLE IF NOT EXISTS legacy_reports (
    id VARCHAR(32) PRIMARY KEY,
    reporter_uuid VARCHAR(32) NOT NULL,
    reporter_name VARCHAR(32) NOT NULL,
    target_uuid VARCHAR(32) NOT NULL,
    target_name VARCHAR(32) NOT NULL,
    reason_id VARCHAR(64) NOT NULL,
    details VARCHAR(512),
    server_id VARCHAR(64) NOT NULL,
    target_ping BIGINT NOT NULL,
    proxy_id VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    assigned_staff_uuid VARCHAR(32),
    assigned_staff_name VARCHAR(32),
    resolution VARCHAR(512),
    punishment_id VARCHAR(64),
    screenshare_id VARCHAR(32),
    revision BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_legacy_reports_target
    ON legacy_reports (target_uuid, created_at);

CREATE INDEX IF NOT EXISTS idx_legacy_reports_reporter
    ON legacy_reports (reporter_uuid, created_at);

CREATE INDEX IF NOT EXISTS idx_legacy_reports_status
    ON legacy_reports (status, created_at);

CREATE INDEX IF NOT EXISTS idx_legacy_reports_created
    ON legacy_reports (created_at);

CREATE TABLE IF NOT EXISTS legacy_report_events (
    id VARCHAR(32) PRIMARY KEY,
    report_id VARCHAR(32) NOT NULL,
    actor_uuid VARCHAR(32),
    actor_name VARCHAR(32) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    previous_status VARCHAR(32),
    new_status VARCHAR(32),
    message VARCHAR(512),
    proxy_id VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_legacy_report_events_report
    ON legacy_report_events (report_id, created_at);

CREATE INDEX IF NOT EXISTS idx_legacy_report_events_created
    ON legacy_report_events (created_at);
