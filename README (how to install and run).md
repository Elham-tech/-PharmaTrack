# PharmaTrack — Pharmacy Management System

A full-stack pharmacy management system that handles **medicine catalogs, suppliers, inventory batches, stock movements, prescriptions & dispensing transactions, cashier payment approval, audit logs**, and **authentication & authorization** (Spring Security 6).

- **Backend:** Spring Boot 3.3.2 · Java 17 · Spring Security 6.3.x · Spring Data JPA · PostgreSQL
- **Frontend:** React 18 · Vite 5 · Axios · React Router
- **Security:** session-based form login (no JWT needed), BCrypt password hashing, DB-driven authorities, `@PreAuthorize` method security

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Structure](#project-structure)
3. [Database Setup](#database-setup)
4. [Running the Backend](#running-the-backend)
5. [Running the Frontend](#running-the-frontend)
6. [First-Run Behavior & Seed Data](#first-run-behavior--seed-data)
7. [Security: Login, Credentials & Keys](#security-login-credentials--keys)
8. [Database Schema](#database-schema)
9. [API Reference](#api-reference)
10. [Testing the API with Postman](#testing-the-api-with-postman)
11. [curl Quick Reference](#curl-quick-reference)
12. [Manual DB Migrations](#manual-db-migrations)
13. [Configuration Properties](#configuration-properties)
14. [Troubleshooting](#troubleshooting)
15. [Group Members](#group-members)

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| **Java** | 17+ (JDK 17 or newer) | The project targets Java 17 (`java.version=17`); newer JDKs work too |
| **Maven** | 3.8+ *(optional)* | The project includes the Maven Wrapper (`mvnw` / `mvnw.cmd`), so Maven itself is not required |
| **Node.js** | 18+ | Required for the React frontend (`npm`) |
| **PostgreSQL** | 14+ (tested on 18) | Local instance required; see [Database Setup](#database-setup) |

Verify your tools:

```bash
java -version
node -v
psql --version     # optional; only needed to run SQL directly
```

---

## Project Structure

```
PharmaTrack/                 # Spring Boot backend
├── mvnw / mvnw.cmd          # Maven wrapper (no global Maven needed)
├── pom.xml                  # Build config: Boot 3.3.2, Security, Data JPA, Validation, PostgreSQL
└── src/main/
    ├── java/com/example/PharmaTrack/
    │   ├── controller/      # REST controllers (Auth, Demo, Users, Medicines, ...)
    │   ├── dao/             # DAO interfaces
    │   ├── repository/      # DAO implementations (EntityManager / JPQL)
    │   ├── entity/          # JPA entities (User, Authority, Medicine, InventoryBatch, ...)
    │   ├── dto/             # Request/response DTOs (RegisterRequest, StockMovementRequest, ErrorResponse)
    │   ├── security/        # Spring Security config (SecurityConfig, AppUserDetailsService, AppAuthorities)
    │   ├── service/         # Service interfaces + impl (business logic)
    │   ├── config/          # DataInitializer (seed data)
    │   └── exception/       # GlobalExceptionHandler + custom exceptions
    └── resources/
        ├── application.properties
        └── db/migrations/   # Manual SQL migrations (001, 002)

PharmaTrack-Frontend/        # React frontend
├── package.json
├── vite.config.js           # Dev server (port 3000) + proxy to backend :8080
└── src/
    ├── api/api.js           # Axios API layer (all resources + authApi)
    ├── components/          # Layout, Sidebar, Toast, ConfirmDialog, AuthContext
    └── pages/               # Login, Dashboard, Users, Medicines, Batches, ...
```

---

## Database Setup

1. Start your local PostgreSQL server.

2. Create the database (one time):

```bash
# Using psql (adjust path to your PostgreSQL install if needed)
psql -U postgres -c "CREATE DATABASE pharmatrack;"
```

3. Create/confirm the user, or reuse an existing one, and make sure the credentials in
   `PharmaTrack/src/main/resources/application.properties` match:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pharmatrack
spring.datasource.username=codecrashers
spring.datasource.password=Anmspring2026
```

> ⚠️ The default credentials above are what the project ships with. **Change them** (or the DB user) for anything other than local development, and update the properties file accordingly.

Tables are created/updated **automatically** by Hibernate (`spring.jpa.hibernate.ddl-auto=update`) on the first backend start — no SQL scripts needed to create the schema.

---

## Running the Backend

From the `PharmaTrack/` directory:

```bash
# macOS / Linux
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run

# Or if you have Maven installed globally
mvn spring-boot:run
```

The backend starts on **http://localhost:8080**.

You should see these log lines on a healthy start:

```
Global AuthenticationManager configured with UserDetailsService bean with name appUserDetailsService
Tomcat started on port 8080 (http) with context path '/'
Started PharmaTrackApplication
```

> 📌 No “generated security password” appears because a custom `SecurityConfig` with a BCrypt `PasswordEncoder` and a DB-backed `UserDetailsService` is configured.

---

## Running the Frontend

From the `PharmaTrack-Frontend/` directory:

```bash
npm install        # first time only
npm run dev
```

The frontend starts on **http://localhost:3000** and proxies `/api`, `/login` and `/logout` to the backend at `:8080` (see `vite.config.js`).

Open **http://localhost:3000** in a browser. You will be redirected to the login page.

**Production build** (optional):

```bash
npm run build      # outputs to dist/
```

---

## First-Run Behavior & Seed Data

On every backend startup, `DataInitializer` runs:

1. **Seeds authorities** — inserts one row per business role into the `authorities` table if missing:
   `ADMIN`, `PHARMACIST`, `CASHIER`, `INVENTORY_MANAGER`, `PROCUREMENT_OFFICER`, `AUDITOR`
2. **Back-fills existing users** — any pre-existing user whose `authorities` set is empty gets the authority matching their legacy `role` value, so they can log in immediately after upgrading.
3. **Default admin account** — if the `users` table is **completely empty**, it creates:

| Username | Password | Authorities |
|---|---|---|
| `admin` | `admin123` | ADMIN |

> Disable the default-admin seeding with `app.security.seed-default-admin=false` in `application.properties`.

### Test users

For development/testing the following accounts were registered (password `123456` for all):

| Username | Display name | Authority |
|---|---|---|
| `elham` | Elham | ADMIN |
| `hidaya` | Hidaya | PHARMACIST |
| `hilina` | Hilina | CASHIER |
| `israel` | Israel | INVENTORY_MANAGER |
| `mathewos` | Mathewos | AUDITOR |

> These five accounts exist in the shared development database. On a **fresh database** only `admin`/`admin123` is seeded automatically — create the test users yourself via `POST /api/auth/register` (public endpoint) or from the frontend Users page.

---

## Security: Login, Credentials & Keys

### How authentication works

- **Session-based form login** (Spring Security 6, `SecurityFilterChain` — no `WebSecurityConfigurerAdapter`).
- Login = `POST /login` with `username` + `password` (form-encoded). A **JSESSIONID session cookie** is set on success.
- Passwords are stored as **BCrypt hashes** (never plain text) via the shared `PasswordEncoder` bean.
- Authorities live in the **database** (`authorities` + `user_authorities` tables) and are loaded at login by `AppUserDetailsService` — nothing is hard-coded in the security configuration.
- Method-level authorization via `@EnableMethodSecurity` + `@PreAuthorize`.

### Do you need a security key / JWT secret?

**No.** This application does **not** use JWT, API keys, or a `secret-key` property. The only "keys" you manage are:

1. **The PostgreSQL credentials** in `application.properties` (`spring.datasource.username` / `.password`).
2. **The seed credentials** below (change the default `admin` password after first login).

If you later switch to JWT/stateless auth, that is where a secret key would go — it does not exist today.

### Public vs. protected endpoints

| Area | Access |
|---|---|
| `POST /login`, `GET /login`, `POST /logout` | Public (Spring Security handles these) |
| `POST /api/auth/register` | **Public** — anyone can register |
| `GET /api/demo/public` | **Public** |
| `GET /api/auth/me` | Authenticated |
| All other `/api/**` (medicines, users, batches, prescriptions, …) | **Authenticated** |
| `GET /api/demo/admin` | Authenticated **+ ADMIN** authority, else **403** |
| `GET /api/demo/pharmacist` | Authenticated **+ PHARMACIST** authority, else **403** |

### Response codes you will see

| Code | Meaning |
|---|---|
| `401` | Not authenticated — missing/invalid session (JSON body) |
| `403` | Authenticated but **without the required authority** (JSON body) |
| `400` | Validation failed (JSON body with `validationErrors`) |
| `404` | Resource not found |

### Error response shape

```json
{
  "timestamp": "2026-07-31T13:12:15.259",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication is required to access this resource",
  "path": "/api/auth/me"
}
```

### Notes / trade-offs

- **CSRF is disabled** — this is an API-first SPA using session cookies without CSRF tokens. If you serve server-rendered pages, re-enable CSRF with a token strategy.
- The required authority names for the demo endpoints are **configurable** via `app.security.authorities.*` (see [Configuration Properties](#configuration-properties)) and referenced from `@PreAuthorize("hasAuthority(@appAuthorities.admin)")` style expressions.

---

## Database Schema

Created automatically by Hibernate (`ddl-auto=update`). Main tables:

| Table | Purpose |
|---|---|
| `users` | Login accounts (username, BCrypt password, email, full_name, role, active) |
| `authorities` | Generic authority/role names (ADMIN, PHARMACIST, …) |
| `user_authorities` | Many-to-many join: users ↔ authorities |
| `categories` | Medicine categories |
| `manufacturers` | Medicine manufacturers |
| `suppliers` | Suppliers |
| `medicines` | Medicine catalog (price lives on the batch, not the medicine) |
| `inventory_batches` | Batches with `quantity` (total received), `quantity_remaining` (live balance), `unit_cost`, `unit_price`, dates |
| `stock_movements` | STOCK_IN / STOCK_OUT / ADJUSTMENT / RETURN / EXPIRED_REMOVAL movements |
| `prescriptions` | Prescriptions (+ `dispensed`, `voided` flags) |
| `prescription_items` | Line items (medicine, quantity, dosage, times_per_day, duration_days) |
| `dispensing_records` | Dispensed medicines with unit/total price, payment status (PENDING/PAID/VOIDED), processed by/at |
| `audit_logs` | Audit trail |

> Quantity rules: batch `quantity` grows only on stock-in; `quantity_remaining` is the live balance (stock-in adds, stock-out subtracts, voided dispensing restores). Dispensing prices are computed server-side as **unit cost × 1.2** (20 % margin); stock-out movements carry no price (stock-out at cost).

---

## API Reference

Base URL: `http://localhost:8080` · All responses are JSON · All `/api/**` endpoints require a session cookie except those marked **public**.

### Authentication (Spring Security + Auth)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/login` | public | Form login. Body: `application/x-www-form-urlencoded` with `username`, `password` → sets `JSESSIONID` cookie, returns 200 |
| POST | `/logout` | — | Ends the session (204) |
| POST | `/api/auth/register` | public | Register: username, password, email, fullName, authorityNames[] → 201 |
| GET | `/api/auth/me` | auth | Returns the logged-in user (with authorities) |

### Demo (authorization examples)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/demo/public` | public | No login required → 200 |
| GET | `/api/demo/authenticated` | auth | Any logged-in user → 200 |
| GET | `/api/demo/admin` | auth + ADMIN | 200 for ADMIN, **403** otherwise |
| GET | `/api/demo/pharmacist` | auth + PHARMACIST | 200 for PHARMACIST, **403** otherwise |

### Users — `/api/users`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/users` | Create user (password is BCrypt-encoded, authority synced from role) |
| PUT | `/api/users/{id}` | Update user |
| PATCH | `/api/users/{id}` | Partial update |
| GET | `/api/users` | List all |
| GET | `/api/users/{id}` | Get by id |
| GET | `/api/users/username/{username}` | Get by username |
| GET | `/api/users/role/{role}` | List by role (`ADMIN`, `PHARMACIST`, `CASHIER`, `INVENTORY_MANAGER`, `PROCUREMENT_OFFICER`, `AUDITOR`) |
| DELETE | `/api/users/{id}` | Delete |
| GET | `/api/users/check/username/{username}` | True if username exists |
| GET | `/api/users/check/email/{email}` | True if email exists |

### Medicines — `/api/medicines`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/medicines` | Create medicine |
| PUT / PATCH | `/api/medicines/{id}` | Update / partial update |
| GET | `/api/medicines` | List all |
| GET | `/api/medicines/{id}` | Get by id |
| GET | `/api/medicines/code/{code}` | Get by code |
| GET | `/api/medicines/category/{categoryId}` | Filter by category |
| GET | `/api/medicines/manufacturer/{manufacturerId}` | Filter by manufacturer |
| GET | `/api/medicines/search?name=…` | Search by name |
| DELETE | `/api/medicines/{id}` | Delete |
| GET | `/api/medicines/check/code/{code}` | True if code exists |

### Categories — `/api/categories`

POST · GET list · GET `/{id}` · GET `/name/{name}` · PUT/PATCH `/{id}` · DELETE `/{id}` · GET `/check/name/{name}`

### Manufacturers — `/api/manufacturers`

POST · GET list · GET `/{id}` · GET `/name/{name}` · PUT/PATCH `/{id}` · DELETE `/{id}` · GET `/check/name/{name}`

### Suppliers — `/api/suppliers`

POST · GET list · GET `/{id}` · GET `/code/{code}` · GET `/active` · PUT/PATCH `/{id}` · DELETE `/{id}` · GET `/check/code/{code}`

### Inventory Batches — `/api/inventory-batches`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/inventory-batches` | Create batch (**do not send quantity** — it starts at 0 and is managed by stock movements) |
| GET | `/api/inventory-batches` | List all |
| GET | `/api/inventory-batches/{id}` | Get by id |
| GET | `/api/inventory-batches/number/{batchNumber}` | Get by batch number |
| GET | `/api/inventory-batches/medicine/{medicineId}` | Batches for a medicine |
| GET | `/api/inventory-batches/supplier/{supplierId}` | Batches from a supplier |
| GET | `/api/inventory-batches/expiring?expiryDate=YYYY-MM-DD` | Batches expiring on/before a date |
| GET | `/api/inventory-batches/available/medicine/{medicineId}` | Batches with remaining stock |
| PUT / PATCH | `/api/inventory-batches/{id}` | Update (quantity fields ignored — auto-managed) |
| DELETE | `/api/inventory-batches/{id}` | Delete |

### Stock Movements — `/api/stock-movements`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/stock-movements` | Create a movement record directly |
| GET | `/api/stock-movements` | List all |
| GET | `/api/stock-movements/{id}` | Get by id |
| GET | `/api/stock-movements/type/{movementType}` | By type (STOCK_IN, STOCK_OUT, ADJUSTMENT, RETURN, EXPIRED_REMOVAL) |
| GET | `/api/stock-movements/medicine/{medicineId}` | By medicine |
| GET | `/api/stock-movements/batch/{batchId}` | By batch |
| GET | `/api/stock-movements/user/{userId}` | By performing user |
| **POST** | `/api/stock-movements/stock-in` | **Adds stock** to a batch (increases `quantity` + `quantity_remaining`) and logs a STOCK_IN movement |
| **POST** | `/api/stock-movements/stock-out` | **Deducts** `quantity_remaining` (fails if insufficient) and logs a STOCK_OUT movement |

`stock-in` / `stock-out` body (`StockMovementRequest`):

```json
{
  "medicineId": 1,
  "inventoryBatchId": 1,
  "quantity": 100,
  "performedById": 1,
  "referenceNumber": "GRN-001",
  "notes": "Initial stock"
}
```

### Prescriptions — `/api/prescriptions`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/prescriptions` | Create (with `items[]`) |
| GET | `/api/prescriptions` | List all |
| GET | `/api/prescriptions/{id}` | Get by id |
| GET | `/api/prescriptions/number/{prescriptionNumber}` | Get by prescription number |
| GET | `/api/prescriptions/patient/{patientName}` | By patient |
| GET | `/api/prescriptions/doctor/{doctorName}` | By doctor |
| GET | `/api/prescriptions/un-dispensed` | Not yet dispensed **and not voided** |
| PATCH | `/api/prescriptions/{id}/dispense?userId=…` | Mark as dispensed (rejected if already dispensed or voided) |
| PUT / PATCH | `/api/prescriptions/{id}` | Update / partial update |
| DELETE | `/api/prescriptions/{id}` | Delete |

### Dispensing Records — `/api/dispensing-records`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/dispensing-records` | Create dispensing record. **Prices are recomputed server-side** (`unitPrice = batch.unitCost × 1.2`, `totalPrice = unitPrice × qty`) — the client must still send placeholder `unitPrice`/`totalPrice` (required by `@Valid`), but the server overrides them |
| GET | `/api/dispensing-records` | List all |
| GET | `/api/dispensing-records/{id}` | Get by id |
| GET | `/api/dispensing-records/number/{dispensingNumber}` | Get by dispensing number |
| GET | `/api/dispensing-records/prescription/{prescriptionId}` | By prescription |
| GET | `/api/dispensing-records/medicine/{medicineId}` | By medicine |
| GET | `/api/dispensing-records/user/{userId}` | By dispensed-by user |
| **POST** | `/api/dispensing-records/{id}/approve?cashierId=…` | Cashier marks payment **PAID** (only from PENDING) |
| **POST** | `/api/dispensing-records/{id}/void?cashierId=…` | Cashier **voids**: restores batch stock, logs STOCK_IN, voids the prescription, marks VOIDED (only from PENDING) |

### Audit Logs — `/api/audit-logs`

POST · GET list · GET `/{id}` · GET `/entity-type/{entityType}` · GET `/entity/{entityType}/{entityId}` · GET `/action/{action}` · GET `/user/{userId}` · GET `/date-range?start=…&end=…` · POST `/log`

---

## Testing the API with Postman

### Step 0 — Prerequisites

- Backend running on `http://localhost:8080`.
- Postman desktop app (or the web version).

### Step 1 — Register a user (public)

```
POST http://localhost:8080/api/auth/register
Headers: Content-Type: application/json
Body (raw JSON):
```

```json
{
  "username": "jane",
  "password": "secret123",
  "email": "jane@example.com",
  "fullName": "Jane Doe",
  "authorityNames": ["PHARMACIST"]
}
```

**Expected:** `201 Created` and the created user (with `authorities`).

> Authority names are matched case-insensitively against the DB. If the name matches a pharmacy role (`ADMIN`, `PHARMACIST`, `CASHIER`, `INVENTORY_MANAGER`, `PROCUREMENT_OFFICER`, `AUDITOR`) the user’s `role` field is set too. Generic names work as well (role stays null).

### Step 2 — Login (form login)

```
POST http://localhost:8080/login
Headers: Content-Type: application/x-www-form-urlencoded
Body (form-urlencoded):
   username = jane
   password = secret123
```

- **Expected:** `200 OK` with `{"message":"Login successful"}`.
- Postman **automatically stores the `JSESSIONID` cookie** for `localhost` (check the **Cookies** tab under the request). All subsequent requests to `http://localhost:8080/...` will carry it.
- **Wrong password:** `401` with `{"message":"Invalid username or password"}`.

> 💡 In Postman, if the cookie isn’t picked up, enable **“Automatically follow redirects”** is not required (we return JSON, not redirects). If cookies still don’t persist, open the **Cookies** manager and confirm `localhost` is listed, or use the same request and check `Set-Cookie: JSESSIONID=...` in the response headers.

### Step 3 — Verify your session

```
GET http://localhost:8080/api/auth/me
```

**Expected:** `200` with your user object (username, email, role, authorities).

```
GET http://localhost:8080/api/auth/me     (in a fresh session / logged out)
```
**Expected:** `401` JSON — proves the endpoint is protected.

### Step 4 — Demo authorization matrix

| Request | Login as | Expected |
|---|---|---|
| `GET /api/demo/public` | anyone / none | **200** |
| `GET /api/demo/authenticated` | any user | **200** |
| `GET /api/demo/admin` | `elham` (ADMIN) | **200** |
| `GET /api/demo/admin` | `hidaya` (PHARMACIST) | **403** |
| `GET /api/demo/pharmacist` | `hidaya` (PHARMACIST) | **200** |
| `GET /api/demo/pharmacist` | `elham` (ADMIN) | **403** |
| `GET /api/demo/admin` | logged out | **401** |

### Step 5 — Logout

```
POST http://localhost:8080/logout
```
**Expected:** `204`. After this, protected requests return `401`.

### Step 6 — Full business flow (CRUD end-to-end)

Use one session (e.g. `elham` / `123456`) for all steps:

1. **Create a category**
   ```
   POST /api/categories   {"name":"Antibiotics","description":"..."}   → 201 (note its id)
   ```
2. **Create a manufacturer**
   ```
   POST /api/manufacturers {"name":"Droga","address":"Addis Ababa"}    → 201
   ```
3. **Create a medicine**
   ```json
   POST /api/medicines
   {
     "code": "AMOX500",
     "name": "Amoxicillin 500 mg tablet",
     "description": "Broad-spectrum antibiotic",
     "category": { "id": 1 },
     "manufacturer": { "id": 1 },
     "unit": "tablet",
     "requiresPrescription": true
   }
   ```
4. **Create an inventory batch** (no `quantity` — it starts at 0)
   ```json
   POST /api/inventory-batches
   {
     "batchNumber": "B-AMOX-001",
     "medicine": { "id": 1 },
     "supplier": { "id": 1 },
     "unitCost": 10.00,
     "unitPrice": 12.00,
     "manufacturingDate": "2026-01-01",
     "expiryDate": "2028-01-01"
   }
   ```
5. **Stock in 100 units**
   ```json
   POST /api/stock-movements/stock-in
   { "medicineId": 1, "inventoryBatchId": 1, "quantity": 100, "performedById": 1 }
   ```
6. **Create a prescription**
   ```json
   POST /api/prescriptions
   {
     "prescriptionNumber": "RX-001",
     "patientName": "Jane Doe",
     "doctorName": "Dr. Smith",
     "items": [
       { "medicine": { "id": 1 }, "quantity": 1, "dosage": "500mg", "timesPerDay": 2, "durationDays": 5 }
     ]
   }
   ```
7. **Dispense** — `unitPrice`/`totalPrice` are placeholders only (validation requires them); the server recomputes them at **cost × 1.2**:
   ```json
   POST /api/dispensing-records
   {
     "dispensingNumber": "D-001",
     "prescription": { "id": 1 },
     "medicine": { "id": 1 },
     "inventoryBatch": { "id": 1 },
     "quantityDispensed": 10,
     "unitPrice": 12.00,
     "totalPrice": 120.00,
     "dispensedBy": { "id": 1 }
   }
   ```
   → Response shows the server-computed `unitPrice` = 12.00 (10.00 × 1.2) and `totalPrice` = 120.00, overriding whatever you sent.

   > 💡 Substitute the real IDs returned by the earlier create steps (medicine, batch, prescription, user ids) — they won't all be `1` on a fresh database.
8. **Cashier approves payment**
   ```
   POST /api/dispensing-records/1/approve?cashierId=3
   ```
   → `paymentStatus` becomes `PAID`, `processedBy`/`processedAt` set.
9. *(Optional)* **Void** instead: `POST /api/dispensing-records/1/void?cashierId=3` — restores batch stock, logs a STOCK_IN movement, marks the prescription `voided`, and sets `paymentStatus = VOIDED`.

---

## curl Quick Reference

```bash
BASE=http://localhost:8080

# 1. Register
curl -X POST $BASE/api/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"jane","password":"secret123","email":"jane@example.com","fullName":"Jane Doe","authorityNames":["PHARMACIST"]}'

# 2. Login (saves the session cookie to /tmp/cookies.txt)
curl -c /tmp/cookies.txt -X POST $BASE/login \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data 'username=elham&password=123456'

# 3. Authenticated calls
curl -b /tmp/cookies.txt $BASE/api/auth/me
curl -b /tmp/cookies.txt $BASE/api/medicines
curl -b /tmp/cookies.txt $BASE/api/demo/admin        # 200 for ADMIN
curl -b /tmp/cookies.txt $BASE/api/demo/pharmacist   # 403 for ADMIN

# 4. Logout
curl -b /tmp/cookies.txt -X POST $BASE/logout
```

---

## Manual DB Migrations

The project uses `ddl-auto=update`, but four columns were historically added as `NOT NULL` to tables that already had rows — PostgreSQL refused, so the columns were never created and startup logged errors. Fixes live in `PharmaTrack/src/main/resources/db/migrations/`:

| File | Fix |
|---|---|
| `001_fix_prescriptions_voided.sql` | Adds `prescriptions.voided` (backfilled `false`, then NOT NULL) |
| `002_add_missing_not_null_columns.sql` | Adds `prescription_items.times_per_day`, `prescription_items.duration_days`, `inventory_batches.unit_price` (backfilled) |

**Run them manually** (they are idempotent — safe to re-run):

```bash
psql -h localhost -U codecrashers -d pharmatrack -f PharmaTrack/src/main/resources/db/migrations/001_fix_prescriptions_voided.sql
psql -h localhost -U codecrashers -d pharmatrack -f PharmaTrack/src/main/resources/db/migrations/002_add_missing_not_null_columns.sql
```

*(On Windows, use the full path to your `psql.exe`, e.g. `"/c/Program Files/PostgreSQL/18/bin/psql.exe"`, and set `PGPASSWORD` first.)*

---

## Configuration Properties

File: `PharmaTrack/src/main/resources/application.properties`

| Property | Default | Description |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/pharmatrack` | Database URL |
| `spring.datasource.username` | `codecrashers` | DB user |
| `spring.datasource.password` | `Anmspring2026` | DB password |
| `spring.jpa.hibernate.ddl-auto` | `update` | Auto create/update schema |
| `server.port` | `8080` | Backend port |
| `app.security.authorities.admin` | `ADMIN` | Authority name required by `@PreAuthorize(@appAuthorities.admin)` (demo) |
| `app.security.authorities.pharmacist` | `PHARMACIST` | Authority name required by the pharmacist demo endpoint |
| `app.security.authorities.cashier` | `CASHIER` | Cashier authority name (used by app config / frontend) |
| `app.security.authorities.auditor` | `AUDITOR` | Auditor authority name |
| `app.security.seed-default-admin` | `true` | Create `admin` / `admin123` on startup when no users exist |

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `Access denied for user 'codecrashers'` / DB connection refused | Start PostgreSQL; create the `pharmatrack` database; fix credentials in `application.properties` |
| `Port 8080 already in use` | Stop the previous backend (or change `server.port`) |
| `Port 3000 already in use` | Stop the previous Vite dev server (or change the port in `vite.config.js`) |
| Frontend calls return `401` | You are not logged in — log in via `POST /login` (or the login page) first |
| Frontend calls return `403` | Logged in, but your user lacks the required authority |
| Login returns `401 Invalid username or password` | Wrong credentials, or the user has no authorities / is inactive |
| `column "xxx" does not exist` at runtime | Run the migrations in [Manual DB Migrations](#manual-db-migrations) |
| Postman doesn't keep the session | Confirm the login response sets `Set-Cookie: JSESSIONID`, and requests target `http://localhost:8080` (same host) |
| Login succeeds but frontend redirects to login again | Clear the browser cookies for `localhost:3000` and log in again (the session cookie is host-scoped) |

---

## Group Members

- Elham Mohamod
- Helina Mogesse
- Hidaya yesuf
- Israel Kebede
- Mathewos kebede
