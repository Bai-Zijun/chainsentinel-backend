# ChainSentinel Observability

## Request Tracing

Every HTTP response contains an `X-Request-Id` header. A caller may provide an identifier containing 1 to 64 letters, digits, dots, underscores, or hyphens. Invalid or missing values are replaced with a generated UUID.

The identifier is stored in SLF4J MDC while the request is processed, so controller, service, and exception logs can be correlated.

Example:

```bash
curl -i \
  -H "X-Request-Id: manual-test-001" \
  http://127.0.0.1:8080/api/transactions/risk/high?page=1\&size=10
```

The access log records:

```text
http_request method=GET path=/api/transactions/risk/high status=200 durationMs=18 outcome=completed
```

Request bodies, database passwords, RPC credentials, and authorization headers are not logged.

## Health Endpoint

Only the Actuator `health` and `info` endpoints are exposed:

```text
GET /actuator/health
GET /actuator/info
```

Health details are hidden by default. A production deployment should expose these endpoints only to the internal monitoring network.
