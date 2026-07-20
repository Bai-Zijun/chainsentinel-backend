# Bitcoin Block Sync for Java

## Scope

The Java backend can manually backfill recent Bitcoin Core blocks into MySQL and report sync state.

```text
POST /api/sync/blocks/backfill?count=10
GET  /api/sync/status
```

The backfill count must be between 1 and 100. The current implementation stores block metadata and transaction counts, not complete transaction bodies.

## Database Migration

Flyway migration `V3__create_bitcoin_sync_tables.sql` creates:

- `bitcoin_blocks`: block metadata with unique `(network, block_hash)` identity.
- `sync_checkpoints`: the last successfully persisted block for each network.
- `sync_runs`: execution history, progress, duration timestamps, and failure messages.

The migration uses `CREATE TABLE IF NOT EXISTS` so it can coexist with tables previously created by the FastAPI prototype. Flyway still requires the existing table definitions to be compatible with V3.

## Runtime Flow

1. Read the current node tip with `getblockchaininfo`.
2. Create a `RUNNING` sync run and lock the network checkpoint.
3. Read each block with `getblockhash` and `getblock`.
4. Validate height, hash, transaction count, and previous-block continuity.
5. Persist each block in its own transaction and advance the checkpoint.
6. Mark the run `SUCCESS`, or record `FAILED` with a bounded error message.

Only one sync run per network can be active. A second request receives HTTP `409` while the checkpoint is `RUNNING`.

Repeated backfills are idempotent at the block-row level. An existing `(network, block_hash)` record is updated instead of duplicated, and only one block at a height remains canonical.

## Run and Verify

Open the SSH tunnel and set the Bitcoin RPC and database environment variables described in `BITCOIN_CORE_JAVA.md`, then start the application:

```powershell
mvn spring-boot:run
```

Backfill the latest ten blocks:

```powershell
curl.exe -X POST "http://127.0.0.1:8080/api/sync/blocks/backfill?count=10"
```

Inspect checkpoint and latest-run state:

```powershell
curl.exe "http://127.0.0.1:8080/api/sync/status"
```

Run automated tests:

```powershell
mvn test
```

The MySQL integration test uses Testcontainers and is skipped when Docker is unavailable.

## Current Boundaries

- Sync is triggered manually; there is no scheduler or ZMQ listener yet.
- A process crash can leave a checkpoint in `RUNNING`; stale-run recovery is not implemented yet.
- The service detects chain discontinuity but does not yet roll back a reorganization.
- Full transaction ingestion and real-time anomaly scoring are separate follow-up stages.
