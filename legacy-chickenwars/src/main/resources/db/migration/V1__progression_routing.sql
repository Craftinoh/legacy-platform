CREATE TABLE IF NOT EXISTS cw_schema_history (
  version VARCHAR(64) PRIMARY KEY,
  installed_at BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS cw_player_progress (
  player_id VARCHAR(36) PRIMARY KEY,
  total_experience BIGINT NOT NULL DEFAULT 0 CHECK (total_experience >= 0),
  coins BIGINT NOT NULL DEFAULT 0 CHECK (coins >= 0),
  updated_at BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS cw_coin_ledger (
  transaction_id VARCHAR(36) PRIMARY KEY,
  player_id VARCHAR(36) NOT NULL,
  amount BIGINT NOT NULL CHECK (amount <> 0),
  balance_after BIGINT NOT NULL CHECK (balance_after >= 0),
  idempotency_key VARCHAR(160) NOT NULL UNIQUE,
  created_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS cw_coin_ledger_player_idx ON cw_coin_ledger(player_id, created_at);
CREATE TABLE IF NOT EXISTS cw_player_statistics (
  player_id VARCHAR(36) NOT NULL,
  mode VARCHAR(16) NOT NULL,
  games BIGINT NOT NULL DEFAULT 0,
  wins BIGINT NOT NULL DEFAULT 0,
  kills BIGINT NOT NULL DEFAULT 0,
  final_kills BIGINT NOT NULL DEFAULT 0,
  deaths BIGINT NOT NULL DEFAULT 0,
  eliminations BIGINT NOT NULL DEFAULT 0,
  chicken_kills BIGINT NOT NULL DEFAULT 0,
  resources_collected BIGINT NOT NULL DEFAULT 0,
  play_seconds BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY(player_id, mode)
);
CREATE TABLE IF NOT EXISTS cw_match_results (
  match_id VARCHAR(80) PRIMARY KEY,
  mode VARCHAR(16) NOT NULL,
  winner_team_id VARCHAR(80),
  finished_at BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS cw_match_participants (
  match_id VARCHAR(80) NOT NULL,
  player_id VARCHAR(36) NOT NULL,
  team_id VARCHAR(80),
  winner BOOLEAN NOT NULL,
  experience BIGINT NOT NULL,
  coins BIGINT NOT NULL,
  PRIMARY KEY(match_id, player_id)
);
CREATE TABLE IF NOT EXISTS cw_quick_buy_presets (
  player_id VARCHAR(36) NOT NULL,
  preset_id VARCHAR(64) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  selected BOOLEAN NOT NULL,
  PRIMARY KEY(player_id, preset_id)
);
CREATE TABLE IF NOT EXISTS cw_quick_buy_slots (
  player_id VARCHAR(36) NOT NULL,
  preset_id VARCHAR(64) NOT NULL,
  slot_number INTEGER NOT NULL,
  item_id VARCHAR(80) NOT NULL,
  PRIMARY KEY(player_id, preset_id, slot_number),
  FOREIGN KEY(player_id, preset_id) REFERENCES cw_quick_buy_presets(player_id, preset_id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS cw_game_instances (
  instance_id VARCHAR(80) PRIMARY KEY,
  server_name VARCHAR(80) NOT NULL,
  mode VARCHAR(16) NOT NULL,
  arena_id VARCHAR(80) NOT NULL,
  status VARCHAR(16) NOT NULL,
  players INTEGER NOT NULL,
  capacity INTEGER NOT NULL,
  heartbeat_at BIGINT NOT NULL,
  accepting_joins BOOLEAN NOT NULL
);
CREATE INDEX IF NOT EXISTS cw_instance_route_idx ON cw_game_instances(mode, status, heartbeat_at);
CREATE TABLE IF NOT EXISTS cw_reservations (
  reservation_id VARCHAR(36) PRIMARY KEY,
  player_id VARCHAR(36) NOT NULL,
  mode VARCHAR(16) NOT NULL,
  instance_id VARCHAR(80) NOT NULL,
  status VARCHAR(16) NOT NULL,
  expires_at BIGINT NOT NULL,
  idempotency_key VARCHAR(160) NOT NULL UNIQUE
);
CREATE INDEX IF NOT EXISTS cw_reservation_instance_idx ON cw_reservations(instance_id, status, expires_at);
CREATE TABLE IF NOT EXISTS cw_reconnect_sessions (
  player_id VARCHAR(36) PRIMARY KEY,
  instance_id VARCHAR(80) NOT NULL,
  expires_at BIGINT NOT NULL,
  consumed BOOLEAN NOT NULL DEFAULT FALSE
);
