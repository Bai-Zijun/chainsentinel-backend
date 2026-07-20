# Bitcoin Core testnet4 Integration for Java

## Scope

The Java backend exposes four read-only Bitcoin Core JSON-RPC endpoints:

```text
GET /api/node/blockchain
GET /api/node/network
GET /api/node/mempool
GET /api/node/blocks/{height}?txLimit=20
```

Manual block persistence and sync status are documented in `BITCOIN_SYNC_JAVA.md`.

The backend verifies that `getblockchaininfo.chain` matches the configured network. The default expected network is `testnet4`.

## SSH Tunnel

Bitcoin Core RPC remains bound to `127.0.0.1:48332` on the remote server. Open a local tunnel before starting the Java backend:

```bash
ssh -N -L 18443:127.0.0.1:48332 root@<remote-host>
```

Keep that terminal open. Do not expose the Bitcoin Core RPC port directly to the Internet.

## Environment

Set these variables in the terminal or local `set-env.ps1`. Never commit their values:

```powershell
$env:BITCOIN_RPC_URL="http://127.0.0.1:18443"
$env:BITCOIN_RPC_USER="your_rpc_user"
$env:BITCOIN_RPC_PASSWORD="your_rpc_password"
$env:BITCOIN_RPC_TIMEOUT="10"
$env:BITCOIN_NETWORK="testnet4"
```

Start the backend and query the node:

```powershell
mvn spring-boot:run

curl.exe http://127.0.0.1:8080/api/node/blockchain
curl.exe http://127.0.0.1:8080/api/node/network
curl.exe http://127.0.0.1:8080/api/node/mempool
curl.exe "http://127.0.0.1:8080/api/node/blocks/143838?txLimit=20"
```

## Error Mapping

- `502`: authentication failure, JSON-RPC error, invalid result, or network mismatch.
- `503`: credentials are missing or the RPC service is unavailable.
- `504`: the RPC request timed out.
- `404`: the requested block height does not exist.

RPC credentials and authorization headers are never written to application logs.
