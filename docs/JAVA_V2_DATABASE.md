# ChainSentinel Java 2.0 Database Migration

## Scope

Java 2.0 uses Flyway to version the MySQL schema:

- `V1`: creates the three core tables for a new database.
- `V2`: ensures the transaction hash unique indexes and the high-risk query index exist.
- `V3`: creates Bitcoin block, sync checkpoint, and sync run tables.
- Existing non-empty databases are baselined at version `1`, so Flyway starts them from `V2` and continues through later migrations.

The migrations do not delete tables or application data.

## Checks Before First Migration

Back up the database and check for duplicate hashes before enabling Flyway on an existing database:

```sql
SELECT tx_hash, COUNT(*) AS duplicate_count
FROM transactions
GROUP BY tx_hash
HAVING COUNT(*) > 1
LIMIT 10;

SELECT tx_hash, COUNT(*) AS duplicate_count
FROM transaction_features
GROUP BY tx_hash
HAVING COUNT(*) > 1
LIMIT 10;

SELECT tx_hash, COUNT(*) AS duplicate_count
FROM anomaly_results
GROUP BY tx_hash
HAVING COUNT(*) > 1
LIMIT 10;
```

All three queries must return no rows. Building a missing index on millions of rows can take time, so run the first migration during a maintenance window.

## Run

Set the database variables, start MySQL, and run:

```powershell
$env:FLYWAY_ENABLED="true"
mvn spring-boot:run
```

Verify the result:

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

SHOW INDEX FROM transactions;
SHOW INDEX FROM transaction_features;
SHOW INDEX FROM anomaly_results;
SHOW INDEX FROM bitcoin_blocks;

SHOW CREATE TABLE sync_checkpoints;
SHOW CREATE TABLE sync_runs;
```

Do not edit an applied migration. Add a new versioned migration for every later schema change.

## Integration Test

The MySQL integration test uses Testcontainers:

```powershell
mvn test
```

When Docker is unavailable, the Testcontainers test is reported as skipped. Unit and MockMvc tests still run normally.
