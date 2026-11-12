CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(512) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS devices (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    installation_id VARCHAR(36) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    manufacturer VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    android_version VARCHAR(100) NOT NULL,
    battery_percent INTEGER CHECK (battery_percent BETWEEN 0 AND 100),
    connectivity VARCHAR(16) NOT NULL,
    last_seen_at BIGINT NOT NULL,
    revoked_at BIGINT
);
CREATE TABLE IF NOT EXISTS sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(36) REFERENCES devices(id) ON DELETE CASCADE,
    access_hash VARCHAR(64) NOT NULL UNIQUE,
    refresh_hash VARCHAR(64) NOT NULL UNIQUE,
    access_expires_at BIGINT NOT NULL,
    refresh_expires_at BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS pairings (
    code_hash VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    installation_id VARCHAR(36) NOT NULL,
    expires_at BIGINT NOT NULL,
    consumed_at BIGINT
);
CREATE TABLE IF NOT EXISTS audit_events (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type VARCHAR(40) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS devices_owner_idx ON devices(user_id);
CREATE INDEX IF NOT EXISTS sessions_owner_idx ON sessions(user_id);
CREATE INDEX IF NOT EXISTS pairings_owner_idx ON pairings(user_id);