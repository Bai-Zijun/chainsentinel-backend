SET @transactions_tx_hash_unique_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'transactions'
      AND column_name = 'tx_hash'
      AND non_unique = 0
);
SET @transactions_tx_hash_ddl = IF(
    @transactions_tx_hash_unique_exists = 0,
    'ALTER TABLE transactions ADD CONSTRAINT uk_transactions_tx_hash UNIQUE (tx_hash)',
    'SELECT 1'
);
PREPARE transactions_tx_hash_statement FROM @transactions_tx_hash_ddl;
EXECUTE transactions_tx_hash_statement;
DEALLOCATE PREPARE transactions_tx_hash_statement;

SET @features_tx_hash_unique_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'transaction_features'
      AND column_name = 'tx_hash'
      AND non_unique = 0
);
SET @features_tx_hash_ddl = IF(
    @features_tx_hash_unique_exists = 0,
    'ALTER TABLE transaction_features ADD CONSTRAINT uk_transaction_features_tx_hash UNIQUE (tx_hash)',
    'SELECT 1'
);
PREPARE features_tx_hash_statement FROM @features_tx_hash_ddl;
EXECUTE features_tx_hash_statement;
DEALLOCATE PREPARE features_tx_hash_statement;

SET @anomaly_tx_hash_unique_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'anomaly_results'
      AND column_name = 'tx_hash'
      AND non_unique = 0
);
SET @anomaly_tx_hash_ddl = IF(
    @anomaly_tx_hash_unique_exists = 0,
    'ALTER TABLE anomaly_results ADD CONSTRAINT uk_anomaly_results_tx_hash UNIQUE (tx_hash)',
    'SELECT 1'
);
PREPARE anomaly_tx_hash_statement FROM @anomaly_tx_hash_ddl;
EXECUTE anomaly_tx_hash_statement;
DEALLOCATE PREPARE anomaly_tx_hash_statement;

SET @risk_score_index_exists = (
    SELECT COUNT(*)
    FROM (
        SELECT index_name
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'anomaly_results'
        GROUP BY index_name
        HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'risk_level,anomaly_score'
    ) AS matching_indexes
);
SET @risk_score_index_ddl = IF(
    @risk_score_index_exists = 0,
    'CREATE INDEX idx_risk_level_score ON anomaly_results (risk_level, anomaly_score DESC)',
    'SELECT 1'
);
PREPARE risk_score_index_statement FROM @risk_score_index_ddl;
EXECUTE risk_score_index_statement;
DEALLOCATE PREPARE risk_score_index_statement;
