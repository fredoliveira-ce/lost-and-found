# Lost & Found

A Spring Boot service where an admin uploads a file of lost items, users
browse and claim them (partial quantities, multiple claimants per item), and
admins can review who claimed what.

## Running it

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8081`. It uses an in-memory H2 database
(reset on every restart) — it's private to that JVM, so there's no live
external client to point at it (no bundled H2 web console either, on
purpose — keeps the dependency footprint minimal).

Interactive API docs (Swagger UI) are at `http://localhost:8081/swagger-ui.html`
— no login needed to view them, only to call the endpoints themselves.

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
extension. Uploading is admin-only — see **Auth** below for the token.

Log in (seed accounts: `alice`/`brian`/`carla` with role `USER`, `admin` with
role `ADMIN`, all sharing the password `password123`):

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "password123"}'
```

List lost items as a user (any authenticated account):

```bash
curl http://localhost:8081/api/lost-items \
  -H "Authorization: Bearer <token>"
```

Search for items, typos and all (see **Search** below):

```bash
curl "http://localhost:8081/api/lost-items/search?q=labtop" \
  -H "Authorization: Bearer <token>"
```

Or ask for them in a sentence:

```bash
curl -G "http://localhost:8081/api/lost-items/query" \
  --data-urlencode "q=black bag lost near the cafeteria last week" \
  -H "Authorization: Bearer <token>"
```

Claim 2 of item `1` — who's claiming is taken from the token, not the request
body:

```bash
curl -X POST http://localhost:8081/api/lost-items/1/claims \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"quantity": 2}'
```

Admin view of every lost item and who has claimed it (needs an `admin`
token):

```bash
curl http://localhost:8081/api/admin/lost-items/claims \
  -H "Authorization: Bearer <admin token>"
```

## Auth

JWT auth, built on Spring Security's OAuth2 Resource Server support. Tokens
are self-issued (no external identity provider) — `POST /api/auth/login`
checks a username/password against the seeded accounts and hands back a
1-hour token signed with an RSA key. Every other endpoint needs
`Authorization: Bearer <token>`, and `/api/admin/**` also needs the token's
`role` claim to be `ADMIN`.

`Account` and `User` are kept separate on purpose: `Account` is how you log
in (username, password, role), `User` is who you are (just a name for now)
— and that's the one a `Claim` actually points to. It's a strict one-to-one
today, but keeping identity separate from credentials means claims stay
meaningful even if another way to log in shows up later.

The signing key is generated fresh every time the app starts, not saved
anywhere. That's fine here since the H2 database resets on every restart too
— old tokens just stop working, same as everything else. Accounts are
seeded on startup rather than self-registered (see `AccountSeeder`).

**Not doing:** refresh tokens, revoking tokens, logging in via an external
provider, or letting people sign themselves up. All reasonable in a real
app, just more than this one needs right now.

## Search

Two search endpoints, both local — no external API calls, no signup, no
cost, works offline.

`GET /api/lost-items/search?q=` ranks items by a small hand-rolled scoring
function: exact word matches score highest, substring matches next, and
typos are tolerated via Levenshtein distance within a small threshold.
Zero-score items are dropped. For example: with the sample data loaded,
`q=labtop` (a typo) still finds both items named "Laptop" — one lost in a
`Taxi`, one at the `Airport`.

`GET /api/lost-items/query?q=` turns a sentence like "jewels lost at the
airport" into a filter: place is matched against the actual distinct
places in the database (not guessed), dates recognize a fixed set of
phrases (today/yesterday/this or last week/month, "last N days"), and
whatever words are left over are matched against the item name. Anything
it doesn't recognize is just skipped, not guessed at. That example query
strips "lost at the" as noise, recognizes `Airport` as a real place already
in the database, and matches "jewels" against the item name — so out of
the two items at the Airport (`Laptop`, `Jewels`), only `Jewels` comes
back.

## API

| Method | Path | Who | Description |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Exchange a username/password for a JWT |
| POST | `/api/admin/lost-items/import` | Admin | Upload a `.txt` or `.csv` file, extract and store lost items |
| GET | `/api/lost-items` | Authenticated | List lost items, including quantity still available to claim |
| GET | `/api/lost-items/search` | Authenticated | Fuzzy/typo-tolerant search over item name and place |
| GET | `/api/lost-items/query` | Authenticated | Free-text sentence parsed into a place/date/keyword filter |
| POST | `/api/lost-items/{id}/claims` | Authenticated | Claim a quantity of a lost item (as the token's user) |
| GET | `/api/admin/lost-items/claims` | Admin | List every lost item with its claimants (userId + resolved name) |

Errors come back as `{"type": "...", "message": "..."}` with a matching HTTP
status: `404` (not found), `400` (bad request / validation), `401`
(missing/invalid token), `403` (authenticated but not allowed), `409`
(claiming more than what remains), `500` (unexpected).

## Architecture

```
domain/
  entity/      LostItem, Claim, Account, User - JPA entities, but framework-agnostic otherwise
  service/     business logic (claim quantity rules, import orchestration, login)
  parsing/     LostItemFileParser port (upload text -> List<LostItem>)
  search/      LostItemSearchEngine (fuzzy scoring), LostItemQueryParser (NL -> place/date/keyword filter)
  exception/   ApiException hierarchy, one type per HTTP status

data/
  repository/  LostItemRepository, ClaimRepository, AccountRepository, UserRepository -
               Spring Data JPA repositories, injected directly into domain.service

web/
  controller/  thin REST controllers, no business logic
  dto/         request/response records, decoupled from entities
  mapper/      assembles response DTOs (e.g. remaining-quantity calc)
  handler/     single @RestControllerAdvice mapping exceptions to responses

config/
  SecurityConfig   - JWT encoder/decoder, filter chain, error-response wiring
  AccountSeeder    - seeds demo accounts on startup (no self-registration)
```

## Known simplifications (and what production would add)

- **Schema managed by Hibernate (`ddl-auto=create-drop`)** against an
  in-memory H2 database. Production would use a persistent database with
  Flyway/Liquibase migrations instead.
- **No pagination** on `GET /api/lost-items` (or `/search`, `/query`) — fine
  at demo scale, would need `Pageable` for a real dataset.
- **Search is local, not AI-powered.** Fuzzy matching and query parsing
  both run in-process (Levenshtein distance, a fixed date/place
  vocabulary) — no LLM or embeddings API involved. Keeps the demo runnable
  with zero signup, config, or cost. A production version might swap in
  real NLP or a vector search for better recall.

## Possible Improvements

- **Split `AdminLostItemController`** into separate import and
  claims-reporting controllers — it currently pulls in five constructor
  dependencies to serve two endpoints that don't share much beyond both being
  "admin".
- **Interface Segregation is a bit weaker at the repository layer** — the
  repositories extend Spring Data's `JpaRepository` directly instead of a
  smaller interface of their own, so services can technically see methods
  they never use. Deliberate trade-off to keep the project simple, not an
  oversight.
