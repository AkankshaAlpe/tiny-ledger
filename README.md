# Tiny Ledger

A production-quality single-account ledger REST API built with Java 17, Spring Boot 3.3, contract-first design (OpenAPI 3.1), TDD, and idempotent transaction safety.

---

## Quick Start

**Prerequisites:** JDK 17+, Maven 3.9+

```bash
# Run the application
mvn spring-boot:run

# Run tests + coverage report
mvn verify
```

Swagger UI: http://localhost:8080/swagger-ui.html  
OpenAPI spec: http://localhost:8080/api-docs  
Coverage report: `target/site/jacoco/index.html`

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/transactions/deposit` | Deposit funds |
| `POST` | `/api/v1/transactions/withdraw` | Withdraw funds |
| `GET` | `/api/v1/balance` | Get current balance |
| `GET` | `/api/v1/transactions` | Get full transaction history |

All `POST` endpoints require an `Idempotency-Key` header (8–64 characters, UUID recommended).

---

## cURL Examples

```bash
KEY=$(uuidgen)

# Deposit
curl -i -X POST http://localhost:8080/api/v1/transactions/deposit \
  -H "Idempotency-Key: $KEY" \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00, "description": "Salary"}'
# → 201 Created

# Retry same deposit (idempotent — no double charge)
curl -i -X POST http://localhost:8080/api/v1/transactions/deposit \
  -H "Idempotency-Key: $KEY" \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00, "description": "Salary"}'
# → 200 OK (same response, balance still 100)

# Withdraw
curl -i -X POST http://localhost:8080/api/v1/transactions/withdraw \
  -H "Idempotency-Key: $(uuidgen)" \
  -H "Content-Type: application/json" \
  -d '{"amount": 40.00, "description": "Rent"}'
# → 201 Created

# Balance
curl http://localhost:8080/api/v1/balance
# → {"balance": 60.00, "currency": "USD"}

# Transaction history (newest first)
curl http://localhost:8080/api/v1/transactions

# Conflict: same key, different payload → 409
curl -X POST http://localhost:8080/api/v1/transactions/deposit \
  -H "Idempotency-Key: $KEY" \
  -H "Content-Type: application/json" \
  -d '{"amount": 999.00}'
# → 409 Conflict
```

---

## Architecture & Design

### Methodology
```
OpenAPI Spec (contract) → Generated DTOs & interfaces
  → Failing tests (TDD Red) → Implementation (Green)
  → Refactor with SOLID/patterns → Runtime contract validation
```

### Key Design Decisions

**Contract-first (OpenAPI 3.1)**  
`src/main/resources/api/ledger-api.yaml` is the single source of truth. DTOs and API interfaces are generated via `openapi-generator-maven-plugin`. The Atlassian `swagger-request-validator` validates every response against the spec at test time.

**Idempotency (Stripe-style)**  
Every `POST` requires an `Idempotency-Key`. The service uses a cache-aside pattern:
- New key → process and store result
- Same key + same payload → return cached result (200, no side effects)
- Same key + different payload → 409 Conflict
- In-flight key → 422 Unprocessable Entity
- Failed request → key cleaned up, retryable with any payload

**SOLID Principles**

| Principle | Application |
|-----------|-------------|
| SRP | `LedgerService` (business rules), `LedgerRepository` (storage), `IdempotencyService` (replay safety), `LedgerController` (HTTP binding) |
| OCP | New operations added via new `TransactionCommand` implementations — no service changes needed |
| LSP | `LedgerRepository` interface: swap in-memory → JPA without breaking callers |
| ISP | Separate `TransactionsApi` and `AccountApi` generated interfaces |
| DIP | Service depends on `LedgerRepository` abstraction, not `InMemoryLedgerRepository` |

**Design Patterns**

| Pattern | Where |
|---------|-------|
| Command | `TransactionCommand` sealed interface → `DepositCommand`, `WithdrawCommand` |
| Repository | `LedgerRepository` interface hides storage |
| Cache-Aside | `IdempotencyService` checks store before processing |
| Factory Method | `Transaction.of(...)` static factory |
| DTO Mapper | Domain model ↔ generated API DTOs in controller |

**Error Responses**  
All errors follow [RFC 7807 Problem Details](https://datatracker.ietf.org/doc/html/rfc7807) (`application/problem+json`).

---

## Test Strategy

```
mvn verify                    # all tests + JaCoCo (≥85% line coverage enforced)
mvn pitest:mutationCoverage   # mutation testing (≥70% kill rate)
```

| Test class | Coverage |
|------------|---------|
| `LedgerServiceTest` | Unit tests — all business rules |
| `IdempotencyServiceTest` | Unit tests — all idempotency scenarios |
| `LedgerControllerTest` | Integration tests — HTTP layer via MockMvc |
| `OpenApiContractTest` | Contract tests — every response validated against the OpenAPI spec |

---

## Assumptions

- **Single account, single currency (USD)** — multi-account/multi-currency out of scope
- **In-memory storage** — data is lost on restart; this is by design per the assignment
- **`BigDecimal` for all money** — `double` is never used to avoid floating-point errors
- **Idempotency-Key is mandatory** for all `POST` endpoints; missing header returns 400
- **Idempotency TTL is 24 hours** (configurable in `InMemoryIdempotencyStore`)
- **Thread-safety** via `synchronized` on write paths + `CopyOnWriteArrayList` for reads
- **No authentication, no persistence, no logging** — per assignment scope

## Trade-offs

- **OpenAPI 3.1 generator warnings** — the generator officially supports 3.0; 3.1 features like `exclusiveMinimum: 0` (as a number rather than boolean) are not fully translated to Bean Validation annotations, so amount positivity is enforced in the service layer instead
- **In-memory idempotency store** — uses a simple `ConcurrentHashMap`; a production system would use Redis with atomic compare-and-set
- **Java 17 instead of 21** — sealed interface `switch` expressions require Java 21; downgraded to Java 17 `instanceof` pattern matching which is available and equivalent
