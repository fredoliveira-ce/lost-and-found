# Lost & Found

A Spring Boot service where an admin uploads a file of lost items, users
browse and claim them (partial quantities, multiple claimants per item), and
admins can review who claimed what.

## Running it

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8081`. It uses an in-memory H2 database
(reset on every restart) — console at `http://localhost:8081/h2-console`
(JDBC URL `jdbc:h2:mem:lostandfound`, user `sa`, empty password).

```bash
./mvnw test              # unit tests
./mvnw verify             # unit + integration tests (*IT classes, via failsafe)
```

## Trying it out

Upload a sample file (see `sample-data/`) as an admin — either the plain-text
format from the assignment brief:

```bash
curl -X POST http://localhost:8081/api/admin/lost-items/import \
  -F "file=@sample-data/lost-items.txt"
```

or an equivalent CSV (header `ItemName,Quantity,Place`, one row per item):

```bash
curl -X POST http://localhost:8081/api/admin/lost-items/import \
  -F "file=@sample-data/lost-items.csv"
```

The endpoint picks the right parser from the file's content type or
extension.

List lost items as a user:

```bash
curl http://localhost:8081/api/lost-items
```

Claim 2 of item `1`:

```bash
curl -X POST http://localhost:8081/api/lost-items/1/claims \
  -H "Content-Type: application/json" \
  -d '{"userId": 1001, "quantity": 2}'
```

Admin view of every lost item and who has claimed it:

```bash
curl http://localhost:8081/api/admin/lost-items/claims
```

## API

| Method | Path | Who | Description |
|---|---|---|---|
| POST | `/api/admin/lost-items/import` | Admin | Upload a `.txt` or `.csv` file, extract and store lost items |
| GET | `/api/lost-items` | User | List lost items, including quantity still available to claim |
| POST | `/api/lost-items/{id}/claims` | User | Claim a quantity of a lost item |
| GET | `/api/admin/lost-items/claims` | Admin | List every lost item with its claimants (userId + resolved name) |

Errors come back as `{"type": "...", "message": "..."}` with a matching HTTP
status: `404` (not found), `400` (bad request / validation), `409` (claiming
more than what remains), `500` (unexpected).

## Architecture

```
domain/
  entity/      LostItem, Claim - JPA entities, but framework-agnostic otherwise
  service/     business logic (claim quantity rules, import orchestration)
  parsing/     LostItemFileParser port (upload text -> List<LostItem>)
  client/      UserServiceClient port - the "external User Service"
  exception/   ApiException hierarchy, one type per HTTP status

data/
  repository/  LostItemRepository, ClaimRepository - Spring Data JPA repositories,
               injected directly into domain.service (see "Repositories" below)
  client/      MockUserServiceClient - a stand-in for the real user service

web/
  controller/  thin REST controllers, no business logic
  dto/         request/response records, decoupled from entities
  mapper/      assembles response DTOs (e.g. remaining-quantity calc)
  handler/     single @RestControllerAdvice mapping exceptions to responses
```

## Known simplifications (and what production would add)

- **No auth.** Admin endpoints are only distinguished by the `/api/admin/**`
  path. In production, `@PreAuthorize("hasRole('ADMIN')")` (Spring Security)
  would sit on `AdminLostItemController`.
- **Schema managed by Hibernate (`ddl-auto=create-drop`)** against an
  in-memory H2 database. Production would use a persistent database with
  Flyway/Liquibase migrations instead.
- **No pagination** on `GET /api/lost-items` — fine at demo scale, would need
  `Pageable` for a real dataset.
- **No OpenAPI/Swagger UI** — kept the dependency footprint minimal; would
  add `springdoc-openapi` for interactive docs.

## Possible improvements

- **Split `AdminLostItemController`** into separate import and
  claims-reporting controllers — it currently pulls in five constructor
  dependencies to serve two endpoints that don't share much beyond both being
  "admin".
