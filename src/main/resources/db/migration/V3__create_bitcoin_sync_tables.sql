CREATE TABLE IF NOT EXISTS bitcoin_blocks (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    network VARCHAR(16) NOT NULL,
    height BIGINT UNSIGNED NOT NULL,
    block_hash CHAR(64) NOT NULL,
    previous_block_hash CHAR(64) NULL,
    merkle_root CHAR(64) NOT NULL,
    block_time DATETIME NOT NULL,
    transaction_count INT UNSIGNED NOT NULL,
    size INT UNSIGNED NOT NULL,
    weight INT UNSIGNED NOT NULL,
    is_canonical TINYINT(1) NOT NULL DEFAULT 1,
    synced_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_blocks_network_hash (network, block_hash),
    KEY idx_blocks_network_height_canonical (network, height, is_canonical)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sync_checkpoints (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    network VARCHAR(16) NOT NULL,
    last_height BIGINT UNSIGNED NULL,
    last_block_hash CHAR(64) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'IDLE',
    last_synced_at DATETIME NULL,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_checkpoints_network (network)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sync_runs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    network VARCHAR(16) NOT NULL,
    run_type VARCHAR(16) NOT NULL,
    start_height BIGINT UNSIGNED NOT NULL,
    target_height BIGINT UNSIGNED NOT NULL,
    last_success_height BIGINT UNSIGNED NULL,
    blocks_processed INT UNSIGNED NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL,
    error_message TEXT NULL,
    started_at DATETIME NOT NULL,
    finished_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_sync_runs_network_started (network, started_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
