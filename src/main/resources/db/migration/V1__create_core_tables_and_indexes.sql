CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tx_hash CHAR(64) NOT NULL,
    block_id BIGINT NULL,
    tx_time DATETIME NULL,
    size INT NULL,
    weight INT NULL,
    input_count INT NULL,
    output_count INT NULL,
    input_total DECIMAL(20, 8) NULL,
    output_total DECIMAL(20, 8) NULL,
    fee DECIMAL(20, 8) NULL,
    fee_rate DECIMAL(20, 8) NULL,
    is_coinbase BOOLEAN NULL,
    has_witness BOOLEAN NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_transactions_tx_hash (tx_hash)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS transaction_features (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tx_hash CHAR(64) NOT NULL,
    input_output_ratio DECIMAL(20, 8) NULL,
    amount_entropy DECIMAL(20, 8) NULL,
    round_amount_ratio DECIMAL(20, 8) NULL,
    dust_output_ratio DECIMAL(20, 8) NULL,
    feature_version VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_transaction_features_tx_hash (tx_hash)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS anomaly_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tx_hash CHAR(64) NOT NULL,
    anomaly_score DECIMAL(10, 8) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    model_name VARCHAR(64) NOT NULL,
    model_version VARCHAR(32) NOT NULL,
    reason VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_anomaly_results_tx_hash (tx_hash),
    KEY idx_risk_level_score (risk_level, anomaly_score DESC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
