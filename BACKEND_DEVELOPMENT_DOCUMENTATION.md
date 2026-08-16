# PharmaTrack Backend — Complete Development Guide & Layer-by-Layer Explanation

> **Purpose of this document:** This guide explains *how* the PharmaTrack backend was developed, *what every Java annotation does*, *why each entity exists*, and *what each layer of the architecture is responsible for*. It is written as an exhaustive reference for presenting and defending the project (code review, project defense, or documentation submission).

---

## Table of Contents

1. [Project Overview & Goals](#1-project-overview--goals)
2. [Technology Stack and Why These Technologies](#2-technology-stack-and-why-these-technologies)
3. [Project Structure — The Layered Architecture](#3-project-structure--the-layered-architecture)
4. [How the Backend Was Developed (Methodology & Process)](#4-how-the-backend-was-developed-methodology--process)
5. [The Entry Point — `@SpringBootApplication`](#5-the-entry-point--springbootapplication)
6. [Build & Configuration Files (`pom.xml` and `application.properties`)](#6-build--configuration-files-pomxml-and-applicationproperties)
7. [The Entity Layer (Database Model)](#7-the-entity-layer-database-model)
   - [7.1 The Complete Annotation Reference](#71-the-complete-annotation-reference)
   - [7.2 Entity-by-Entity Deep Dive](#72-entity-by-entity-deep-dive)
   - [7.3 Entity Relationship Diagram (textual)](#73-entity-relationship-diagram-textual)
8. [The DAO / Repository Layer](#8-the-dao--repository-layer)
9. [The Service Layer (Business Logic)](#9-the-service-layer-business-logic)
10. [The Controller Layer (REST API)](#10-the-controller-layer-rest-api)
11. [The Security Layer](#11-the-security-layer)
12. [The Exception Handling Layer](#12-the-exception-handling-layer)
13. [The DTO Layer (Data Transfer Objects)](#13-the-dto-layer-data-transfer-objects)
14. [The Config Layer — DataInitializer](#14-the-config-layer--datainitializer)
15. [Complete Database Schema (Tables Produced by JPA)](#15-complete-database-schema-tables-produced-by-jpa)
16. [End-to-End Request Flow (a Full Walkthrough)](#16-end-to-end-request-flow-a-full-walkthrough)
17. [Key Business Logic Deep Dives](#17-key-business-logic-deep-dives)
18. [Design Decisions & Trade-offs](#18-design-decisions--trade-offs)
19. [How to Present This (Quick Talking Points)](#19-how-to-present-this-quick-talking-points)

---

## 1. Project Overview & Goals

**PharmaTrack** is an *Auditable Pharmaceutical Supply Chain Management REST API*. Instead of building a monolithic web application, the project focuses on a robust RESTful backend that acts as the core business layer for pharmacy operations.

The system manages:

| Module | What it covers |
|---|---|
| Identity & Access Management | Users, roles/authorities, registration, login/logout, authorization |
| Medicine Catalog | Medicines, categories, manufacturers |
| Procurement / Inventory | Suppliers, inventory batches, stock-in / stock-out movements |
| Pharmaceutical Transactions | Prescriptions, prescription items, dispensing records, payment approve/void workflow |
| Audit Management | A full audit trail of every critical operation (who, what, when, from which IP) |

**Core problems the backend solves** (from the requirements):
1. Accurate stock tracking (batch-level `quantity` vs `quantityRemaining`).
2. Preventing the sale/removal of expired medicines (expiry flags, expiring-batch queries).
3. Accountability — every mutation records *who* did it and *from which IP* (audit log).
4. Controlled access — role/authority-based authorization enforced at the API level.
5. Traceable prescription and dispensing records with a pharmacist → cashier workflow.

---

## 2. Technology Stack and Why These Technologies

| Technology | Version | Why it was chosen |
|---|---|---|
| **Java** | 17 | LTS (long-term support) version; modern syntax; records, sealed classes, pattern matching available. Set in `pom.xml` via `<java.version>17</java.version>`. |
| **Spring Boot** | 3.3.2 | Convention-over-configuration: auto-configuration, embedded server, starter dependencies. The parent POM (`spring-boot-starter-parent`) manages dependency versions so we don't have to. |
| **Spring Web** (`spring-boot-starter-web`) | — | Provides `@RestController`, embedded Tomcat, JSON (Jackson) serialization, and `ResponseEntity` — everything needed for a REST API. |
| **Spring Data JPA** (`spring-boot-starter-data-jpa`) | — | ORM with Hibernate underneath; lets us map Java classes to tables via `@Entity` and query with JPQL. |
| **Spring Security** (`spring-boot-starter-security`) | — | Authentication (form login), authorization (`@PreAuthorize`), BCrypt password hashing, and JSON error responses for 401/403. |
| **Bean Validation** (`spring-boot-starter-validation`) | — | Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Email`, …) enforced at the API boundary with `@Valid`. |
| **PostgreSQL** | — | Robust open-source relational DB with strict constraints; the runtime driver `postgresql` is included. |
| **Jackson JSR-310** (`jackson-datatype-jsr310`) | — | Enables correct JSON serialization/deserialization of `java.time.LocalDate` / `LocalDateTime` (used heavily by the entities). |
| **DevTools** | — | Auto-restart and live reload during development (runtime scope, optional). |
| **Maven** | — | Build tool; `./mvnw` wrapper included so the project builds without a globally installed Maven. |

> **Why Spring Boot rather than plain Java?** Spring Boot's auto-configuration removes almost all boilerplate: dependency injection, transaction management, database connection pool setup, security defaults, and JSON conversion all "just work" from dependencies on the classpath.

---

## 3. Project Structure — The Layered Architecture

The backend follows the classic **layered (n-tier) architecture** taught in software engineering courses. Each layer has one responsibility and only talks to the layer below it:

```
                    ╔═══════════════════════════════════════════════╗
                    ║            HTTP Request (JSON body)           ║
                    ╚═══════════════════════════════════════════════╝
                                          │
                                          ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  1. Controller Layer  (com.example.PharmaTrack.controller)              │
│  e.g. MedicineController, AuthController, PrescriptionController, ...   │
│                                                                         │
│  REQUEST processing (incoming):                                        │
│   @RestController   → registers the bean; every method return value    │
│                       is serialized to JSON for the reply              │
│   @RequestMapping   → sets the base URL (e.g. /api/medicines)          │
│   @GetMapping etc.  → routes the request VERB + PATH to the method     │
│   @RequestBody      → reads the HTTP body and deserializes it into     │
│                       a Java object (request JSON → object)            │
│   @Valid            → validates the deserialized body BEFORE the       │
│                       method runs; invalid → 400 reply with field      │
│                       errors (no service call happens)                 │
│   @PathVariable     → binds a URL segment ({id}) to a parameter        │
│   @RequestParam     → binds a query string (?name=...) to a parameter  │
│   @PreAuthorize     → checks the caller's authority BEFORE the method  │
│                       runs; missing authority → 403 reply              │
│                                                                         │
│  REPLY (outgoing):                                                     │
│   return value      → passed back up to Spring, serialized to JSON     │
│   ResponseEntity    → sets the reply HTTP status (201 Created,         │
│                       200 OK, 204 No Content)                          │
└─────────────────────────────────────────────────────────────────────────┘
     │  calls the SERVICE INTERFACE (never the impl)
     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  2a. Service Interface Layer (contract)                                 │
│  (com.example.PharmaTrack.service)                                      │
│  e.g. MedicineService, StockMovementService, PrescriptionService, ...   │
│                                                                         │
│   No annotations - a pure Java interface that declares WHAT operations  │
│   exist. It defines the API between controller and implementation;      │
│   controllers and unit tests depend on it (mock-friendly).             │
└─────────────────────────────────────────────────────────────────────────┘
     │  implemented by ▼
     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  2b. Service Implementation Layer (business logic)                      │
│  (com.example.PharmaTrack.service.impl)                                 │
│  e.g. MedicineServiceImpl, StockMovementServiceImpl, ...                │
│                                                                         │
│   @Service                  → registers the bean so the controller can  │
│                                be injected with it                      │
│   @Override                 → marks each method as fulfilling the       │
│                                interface contract                       │
│   @Transactional            → opens a DB transaction when the method    │
│                                runs; commits on success, ROLLS BACK on  │
│                                any error → the 200 reply is only sent   │
│                                after a consistent commit                │
│   @Transactional(readOnly)  → same, but read-only: faster queries       │
│   @Autowired constructor    → injects the DAO INTERFACES                │
│   (business rules inside)   → duplicate checks, stock checks, price     │
│                                computation, audit logging               │
└─────────────────────────────────────────────────────────────────────────┘
     │  calls the DAO INTERFACE (never the impl)
     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  3a. DAO Interface Layer (contract)                                     │
│  (com.example.PharmaTrack.dao)                                          │
│  e.g. MedicineDAO, UserDAO, StockMovementDAO, AuditLogDAO, ...          │
│                                                                         │
│   No annotations - declares WHAT persistence operations exist           │
│   (findById, findByCode, save, ...). Persistence can be swapped         │
│   (JPA, JDBC, in-memory mock) without touching the service layer.       │
└─────────────────────────────────────────────────────────────────────────┘
     │  implemented by ▼
     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  3b. DAO Implementation Layer (persistence)                             │
│  (com.example.PharmaTrack.repository)                                   │
│  e.g. MedicineDAOImpl, UserDAOImpl, StockMovementDAOImpl, ...           │
│                                                                         │
│   @Repository            → registers the bean; translates JPA/DB        │
│                             exceptions into Spring DataAccessException │
│   @Autowired EntityManager→ injects the persistence context (session)   │
│   TypedQuery + JPQL      → builds and executes the SQL query            │
│   persist() / merge()    → INSERT (new) / UPDATE (existing) rows       │
└─────────────────────────────────────────────────────────────────────────┘
     │  maps rows to / from
     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  4. Entity Layer (domain model)                                         │
│  (com.example.PharmaTrack.entity)                                       │
│  e.g. Medicine, User, InventoryBatch, Prescription, AuditLog, ...       │
│                                                                         │
│   @Entity / @Table      → maps the class to a database table            │
│   @Id / @GeneratedValue → marks the primary key (auto-generated)        │
│   @Column               → maps a field to a column (unique, nullable,   │
│                            precision/scale for money)                   │
│   @ManyToOne / @OneToMany / @ManyToMany + @JoinColumn / @JoinTable     │
│                         → build the foreign-key relationships           │
│   @Enumerated(STRING)   → stores enums as readable strings              │
│   @PrePersist/@PreUpdate→ set createdAt/updatedAt automatically before  │
│                            INSERT / UPDATE                               │
│   @NotBlank, @Size, @Email, @Min, @DecimalMin, @Future, ...            │
│                         → Bean Validation: checked by @Valid when the   │
│                            request body arrives (bad data → 400 reply)  │
└─────────────────────────────────────────────────────────────────────────┘
     │
     ▼
                    ╔═══════════════════════════════════════════════╗
                    ║              PostgreSQL database               ║
                    ╚═══════════════════════════════════════════════╝

REPLY path (outgoing): after the DB commits, the result object travels back UP the
same chain (DAO → Service → Controller), the controller's return value is serialized
to JSON, and ResponseEntity sets the status. Any exception thrown along the way is
caught by GlobalExceptionHandler (@RestControllerAdvice + @ExceptionHandler) and
converted into a structured JSON error reply (400/404/403...).

Dependency rule (strict top-down):
  Controller → Service interface → Service impl → DAO interface → DAO impl → Entity → DB

Each layer depends ONLY on the interface of the layer below it, never on its implementation.
```

**Cross-cutting packages** (used by any layer):

| Package | Role |
|---|---|
| `com.example.PharmaTrack.security` | `SecurityConfig` (filter chain), `AppUserDetailsService` (loads user for login), `CurrentUserProvider` (who is logged in + IP), `AppAuthorities` (configurable authority names). |
| `com.example.PharmaTrack.exception` | `GlobalExceptionHandler` (`@RestControllerAdvice`) and custom exceptions (`ResourceNotFoundException`, `BadRequestException`). |
| `com.example.PharmaTrack.dto` | `RegisterRequest`, `StockMovementRequest`, `ErrorResponse` — request/response shapes decoupled from entities. |
| `com.example.PharmaTrack.config` | `DataInitializer` — seeds roles/authorities and a default admin on startup. |

**Why layered?**
- **Separation of concerns:** each layer has a single job.
- **Testability:** you can unit-test a controller with a mock service, or a service with a mock DAO.
- **Reusability:** business logic in services can be called from controllers, scheduled jobs, or CLI tools.
- **Swappability:** the DAO interface means the persistence implementation could be swapped (e.g., JDBC) without touching services.

---

## 4. How the Backend Was Developed (Methodology & Process)

Development followed the classic software engineering lifecycle from the requirements document:

### Phase 1 — Initiation & Analysis
- Identified the pharmacy problems (inaccurate stock, expired medicine risk, no accountability).
- Turned them into concrete modules: User Management, Medicine Management, Inventory & Prescription Tracking, Audit Trail, Reporting.

### Phase 2 — Database/Entity Design (bottom-up in practice)
- Modeled the domain nouns as JPA entities first: `User`, `Role`, `Authority`, `Category`, `Manufacturer`, `Medicine`, `Supplier`, `InventoryBatch`, `StockMovement`, `Prescription`, `PrescriptionItem`, `DispensingRecord`, `AuditLog`.
- Decided the relationships (many-to-one, one-to-many, many-to-many) — see [section 7.3](#73-entity-relationship-diagram-textual).

### Phase 3 — Layered Implementation
The code was written layer by layer, **bottom-up**, so each layer could be compiled and tested as the next one was built:

1. **Entities + JPA mapping** (with Bean Validation annotations on fields so bad data is rejected at the API boundary).
2. **DAO interfaces** (pure contracts, no framework annotations) then **DAO implementations** using `EntityManager` + JPQL.
3. **Service interfaces**, then **service implementations** with `@Transactional` and business rules (duplicate checks, stock validation, audit logging).
4. **Controllers** (thin HTTP layer) wired to services.
5. **Security** — `SecurityConfig`, `AppUserDetailsService`, `CurrentUserProvider`, `AppAuthorities`.
6. **Exception handling** — custom exceptions + `GlobalExceptionHandler`.
7. **Data seeding** — `DataInitializer` so the app runs out of the box.

### Phase 4 — Continuous refinements (evident in the code)
- **DB-driven authorization upgrade:** originally the app used a single `role` enum field on the user. It was upgraded to a generic `authorities` table + many-to-many `user_authorities` join table so authority names are *not hard-coded anywhere* in security config. The old `role` field was kept for backward compatibility with the pharmacy UI (see `User.role`, `DataInitializer` back-fill logic).
- **Audit trail everywhere:** every mutating service method records an `AuditLog` entry (create/update/delete/dispense/stock-in/stock-out/login/logout).
- **Server-side pricing:** the selling price is *always computed on the server* (batch unit cost × 1.2 markup) and never trusted from the client — this is why `DispensingRecord.unitPrice/totalPrice` are set inside the service.
- **Quantity integrity:** batch `quantity` vs `quantityRemaining` split — see [section 17](#17-key-business-logic-deep-dives).

---

## 5. The Entry Point — `@SpringBootApplication`

**File:** `src/main/java/com/example/PharmaTrack/PharmaTrackApplication.java`

```java
@SpringBootApplication
public class PharmaTrackApplication {
    public static void main(String[] args) {
        SpringApplication.run(PharmaTrackApplication.class, args);
    }
}
```

### What `@SpringBootApplication` does
It is a **meta-annotation** combining three annotations:

| Combined annotation | What it enables |
|---|---|
| `@Configuration` | Marks the class as a source of Spring bean definitions. |
| `@EnableAutoConfiguration` | Turns on Spring Boot's auto-configuration: it inspects the classpath and configures beans automatically (e.g., seeing Hibernate + PostgreSQL on the classpath configures the `EntityManagerFactory` and `DataSource`; seeing Spring Security configures the security filter chain). |
| `@ComponentScan` | Scans the package `com.example.PharmaTrack` **and all sub-packages** for `@Component`, `@Service`, `@Repository`, `@Controller`, `@RestController`, `@Configuration` and registers them as Spring beans — this is why we never manually wire anything in XML. |

> **Why does the class sit in the root package?** `@ComponentScan` starts from `PharmaTrackApplication`'s package. If the class were in a different package than the controllers/services, Spring would not find them. Placing it in `com.example.PharmaTrack` ensures every sub-package is scanned.

---

## 6. Build & Configuration Files (`pom.xml` and `application.properties`)

### 6.1 `pom.xml` — Maven dependencies

| Dependency | Why it's there |
|---|---|
| `spring-boot-starter-data-jpa` | Hibernate ORM + Spring Data; lets us write `@Entity` classes and use `EntityManager`. |
| `spring-boot-starter-web` | REST controllers, embedded Tomcat, JSON (Jackson). |
| `spring-boot-starter-validation` | Jakarta Bean Validation — activates `@Valid` + `@NotBlank` etc. |
| `spring-boot-starter-security` | Authentication & authorization. |
| `jackson-datatype-jsr310` | Serializes `LocalDate`/`LocalDateTime` to ISO-8601 JSON strings. |
| `spring-boot-devtools` (runtime, optional) | Auto-restart during development. |
| `postgresql` (runtime) | PostgreSQL JDBC driver. |
| `spring-boot-starter-test` (test scope) | JUnit 5, Mockito, Spring Test for future tests. |
| `spring-boot-maven-plugin` | Packages a runnable fat JAR and supports `./mvnw spring-boot:run`. |

### 6.2 `application.properties` — configuration

```properties
spring.application.name=PharmaTrack

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/pharmatrack
spring.datasource.username=...
spring.datasource.password=...
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update        # Hibernate creates/updates tables from entities
spring.jpa.show-sql=true                     # log generated SQL (dev aid)
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# Jackson
spring.jackson.serialization.fail-on-empty-beans=false
spring.jackson.default-property-inclusion=non_null   # omit nulls in JSON responses

# Server
server.port=8080

# Configurable authority names (matched against DB authorities - nothing hard-coded)
app.security.authorities.admin=ADMIN
app.security.authorities.pharmacist=PHARMACIST
app.security.authorities.cashier=CASHIER
app.security.authorities.inventory-manager=INVENTORY_MANAGER
app.security.authorities.auditor=AUDITOR

# Seed default admin (admin / admin123) when no users exist
app.security.seed-default-admin=true

logging.level.com.example.PharmaTrack=DEBUG
```

**Key configuration decisions explained:**

| Setting | Meaning / why |
|---|---|
| `ddl-auto=update` | Hibernate compares entities to the schema and issues `ALTER TABLE`/`CREATE TABLE` automatically. Great for development; in production you'd switch to `validate` + Flyway/Liquibase migrations. |
| `default-property-inclusion=non_null` | Null fields are omitted from JSON, keeping responses clean. |
| `app.security.authorities.*` | The authority names used by `@PreAuthorize` are **configurable** (read into the `AppAuthorities` bean) rather than hard-coded strings in Java. They are matched against rows in the `authorities` table. |

---

## 7. The Entity Layer (Database Model)

The entity layer maps Java classes to database tables. There are **13 entity/enum files**:

| Entity | Table | Purpose in one sentence |
|---|---|---|
| `User` | `users` | A system user (admin, pharmacist, cashier, …) who can log in. |
| `Role` | (enum) | Business-level roles the pharmacy UI understands. |
| `Authority` | `authorities` | Generic, database-stored permission names used by Spring Security. |
| `Category` | `categories` | Groups medicines (e.g., Antibiotics, Painkillers). |
| `Manufacturer` | `manufacturers` | Company that produces medicines. |
| `Medicine` | `medicines` | A catalog product (code, name, unit, prescription flag). |
| `Supplier` | `suppliers` | Company that supplies stock to the pharmacy. |
| `InventoryBatch` | `inventory_batches` | A received batch of a medicine (batch no., cost, price, expiry). |
| `StockMovement` | `stock_movements` | Every stock-in/stock-out/adjustment event. |
| `Prescription` | `prescriptions` | A doctor's prescription for a patient. |
| `PrescriptionItem` | `prescription_items` | One medicine line inside a prescription. |
| `DispensingRecord` | `dispensing_records` | One physical dispensing of a medicine from a batch. |
| `AuditLog` | `audit_logs` | Who did what, when, from which IP. |

### 7.1 The Complete Annotation Reference

This is the **master list of every annotation used in the backend** with its meaning and why it matters.

#### A. JPA / Persistence annotations (`jakarta.persistence.*`)

| Annotation | What it does | Why we use it |
|---|---|---|
| `@Entity` | Marks the class as a JPA entity; Hibernate maps it to a table. | Turns a POJO into a persisted object. |
| `@Table(name = "users")` | Overrides the default table name. | `user` is a reserved SQL word; also matches naming conventions. |
| `@Id` | Marks the primary-key field. | Every table needs a PK. |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | The database auto-generates the PK (identity/auto-increment column in PostgreSQL). | No manual ID assignment; DB guarantees uniqueness. |
| `@Column(name, nullable, unique, precision, scale, columnDefinition)` | Maps a field to a column and adds DB-level constraints. | `unique = true` on `username`/`code` prevents duplicates at the DB; `precision=10, scale=2` makes `BigDecimal` a `NUMERIC(10,2)` for money. |
| `@ManyToOne` | Many rows point to one parent row (FK). | e.g. many `Medicine` → one `Category`. |
| `@JoinColumn(name = "category_id", nullable = false)` | Names the FK column and makes it mandatory. | Controls the actual foreign-key column name in the child table. |
| `@OneToMany(mappedBy = "category", cascade = CascadeType.ALL)` | One parent has many children; `mappedBy` says the FK lives on the child side. | Bidirectional navigation (e.g. `category.getMedicines()`). |
| `@OneToMany(..., orphanRemoval = true)` | Children deleted from the list are removed from the DB. | Used on `Prescription.items` so editing a prescription replaces its items cleanly. |
| `@ManyToMany(fetch = FetchType.LAZY)` | Many-to-many relationship (users ↔ authorities). | A user can have several authorities; an authority belongs to many users. |
| `@JoinTable(name = "user_authorities", joinColumns = @JoinColumn(name="user_id"), inverseJoinColumns = @JoinColumn(name="authority_id"))` | Creates the join table with two FKs. | Standard way to model many-to-many in relational databases. |
| `@Enumerated(EnumType.STRING)` | Stores the enum **by its name** as a VARCHAR. | Safer than ordinal: reordering the enum doesn't corrupt data, and values stay readable in the DB. |
| `@PrePersist` | Callback run by JPA **before INSERT**. | Sets `createdAt`/`updatedAt` automatically — timestamp logic lives in one place. |
| `@PreUpdate` | Callback run by JPA **before UPDATE**. | Automatically refreshes `updatedAt`. |
| `FetchType.LAZY` / `FetchType.EAGER` | When the relation is loaded. | LAZY loads on demand (fast listings); EAGER on `Prescription.items` so the items are always present in the JSON response. `User.authorities` is LAZY but loaded eagerly at login via `LEFT JOIN FETCH` (avoids `LazyInitializationException`). |

#### B. Bean Validation annotations (`jakarta.validation.constraints.*`)

These are evaluated when a `@Valid` object arrives at a controller. This implements **"validate early at the boundary"** — bad requests never reach the database.

| Annotation | Meaning | Example usage |
|---|---|---|
| `@NotBlank(message=…)` | Not null and not empty/whitespace. | `username`, `password`, `email`, `fullName` |
| `@NotNull(message=…)` | Not null (for objects/primitives wrappers and relationships). | `category`, `manufacturer`, `quantity` |
| `@NotEmpty(message=…)` | A collection is not empty. | `RegisterRequest.authorityNames` |
| `@Size(min=, max=, message=…)` | Length constraints. | `@Size(min=3, max=50)` on username |
| `@Email(message=…)` | Must be a valid e-mail shape. | `User.email`, `Supplier.email` |
| `@Min(value=0)` / `@Positive` | Numeric bounds. | batch `quantity >= 0`; movement `quantity > 0` |
| `@DecimalMin(value="0.0", inclusive=false)` | Decimal must be strictly greater than 0. | `unitCost`, `unitPrice`, `totalPrice` |
| `@PastOrPresent` / `@Future` | Date must be in the past (or present) / future. | `manufacturingDate` cannot be future; `expiryDate` must be future |
| `@Valid` | Recursively validates nested objects / triggers validation on a method parameter. | `@Valid @RequestBody Medicine medicine` |

#### C. Jackson / JSON annotations (`com.fasterxml.jackson.annotation.*`)

| Annotation | What it does | Why we use it |
|---|---|---|
| `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)` | Field is only readable *in* (deserialized from requests), never written *out* (never serialized). | `User.password` never appears in any JSON response — a critical security measure. |
| `@JsonIgnore` | Field is excluded from JSON entirely. | `Category.medicines` / `Manufacturer.medicines` / `PrescriptionItem.prescription` — prevents circular references and huge payloads. |
| `@JsonIgnoreProperties("prescription")` | When serializing a `PrescriptionItem` inside a `Prescription`, ignore the back-reference to the prescription. | Prevents infinite recursion when a prescription contains its items and each item points back to the prescription. |
| `@JsonInclude(JsonInclude.Include.NON_NULL)` | Null fields are omitted from serialized JSON. | `ErrorResponse` — clean, minimal error payloads. |

#### D. Spring Framework annotations (`org.springframework.*`)

| Annotation | What it does | Why we use it |
|---|---|---|
| `@SpringBootApplication` | Entry point; enables auto-config + component scan (see section 5). | Bootstraps the app. |
| `@Component` | Registers a plain bean in the Spring context. | `DataInitializer`, `CurrentUserProvider`, `AppAuthorities`. |
| `@Service` | Registers a bean as a *service* (business logic layer). | All `*ServiceImpl` classes. |
| `@Repository` | Registers a bean as a persistence component; also translates JPA exceptions into Spring's `DataAccessException`. | All `*DAOImpl` classes. |
| `@Configuration` | Marks a class as a source of `@Bean` definitions. | `SecurityConfig`. |
| `@Bean` | Declares a bean factory method (the bean's type is the return type). | `PasswordEncoder`, `SecurityFilterChain`. |
| `@Autowired` | Injects a dependency (field, setter, or constructor). | Constructor injection on services/DAOs. (Explicit here for clarity; Spring 4.3+ would auto-wire a single constructor.) |
| `@Value("${property:default}")` | Injects a property value from `application.properties` with a default. | `DataInitializer.seedDefaultAdmin`, `AppAuthorities.*`. |
| `@Transactional` | Wraps the method (or class) in a DB transaction; rolls back on any `RuntimeException`. | All mutating service methods — see section 9. |
| `@Transactional(readOnly = true)` | Read-only transaction: Hibernate skips dirty-checking and flushes. | All read/query service methods — faster reads. |
| `@RestController` | `@Controller` + `@ResponseBody`: every method return value is serialized to JSON. | All controllers. |
| `@RestControllerAdvice` | Global `@ControllerAdvice` + `@ResponseBody`: intercepts exceptions thrown by any controller. | `GlobalExceptionHandler`. |
| `@RequestMapping("/api/medicines")` | Base URL path for a controller. | Groups endpoints under `/api/...`. |
| `@GetMapping` / `@PostMapping` / `@PutMapping` / `@PatchMapping` / `@DeleteMapping` | Map HTTP methods to handler methods (compositions of `@RequestMapping`). | REST verb mapping. |
| `@PathVariable` | Binds a URL path segment (`/{id}`) to a method parameter. | `getMedicineById(@PathVariable Long id)`. |
| `@RequestParam` | Binds a query string parameter (`?name=...`). | `searchMedicines(@RequestParam String name)`. |
| `@RequestBody` | Deserializes the HTTP request body JSON into a Java object. | `@Valid @RequestBody Medicine medicine`. |
| `@CrossOrigin(origins = "*")` | Allows browser cross-origin requests (CORS). | The React SPA runs on a different port (e.g. 5173). Should be restricted in production. |
| `@PreAuthorize("hasAuthority(...)")` | Method-level authorization evaluated *after* authentication via SpEL. | Enforces role/authority rules per controller (see section 11). |
| `@EnableWebSecurity` | Enables Spring Security's web support. | `SecurityConfig`. |
| `@EnableMethodSecurity` | Activates `@PreAuthorize`/`@Secured` on methods. | Required for method-level security to work. |
| `@ExceptionHandler(SomeException.class)` | Declares that a method handles a specific exception type thrown in a controller. | `GlobalExceptionHandler`. |
| `@ResponseStatus(HttpStatus.NOT_FOUND)` | Sets the HTTP status for the exception when it propagates. | `ResourceNotFoundException` → 404, `BadRequestException` → 400. |
| `@DateTimeFormat(iso = ISO.DATE / DATE_TIME)` | Parses a request parameter/field into `LocalDate` / `LocalDateTime`. | `/api/inventory-batches/expiring?expiryDate=...` and audit date-range endpoints. |
| `@Override` | Declares the method implements an interface method. | Every service/DAO implementation. |
| `CommandLineRunner` (interface) | Bean whose `run(...)` executes after the app starts. | `DataInitializer`. |

#### E. Annotations summary table (quick glance for presentation)

| Category | Annotations |
|---|---|
| JPA | `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@ManyToOne`, `@OneToMany`, `@ManyToMany`, `@JoinTable`, `@JoinColumn`, `@Enumerated`, `@PrePersist`, `@PreUpdate` |
| Validation | `@Valid`, `@NotBlank`, `@NotNull`, `@NotEmpty`, `@Size`, `@Email`, `@Min`, `@Positive`, `@DecimalMin`, `@PastOrPresent`, `@Future` |
| Jackson | `@JsonProperty(WRITE_ONLY)`, `@JsonIgnore`, `@JsonIgnoreProperties`, `@JsonInclude` |
| Spring core | `@SpringBootApplication`, `@Component`, `@Service`, `@Repository`, `@Configuration`, `@Bean`, `@Autowired`, `@Value`, `@Transactional`, `@Override` |
| Web | `@RestController`, `@RestControllerAdvice`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`, `@PathVariable`, `@RequestParam`, `@RequestBody`, `@CrossOrigin`, `@DateTimeFormat` |
| Security | `@EnableWebSecurity`, `@EnableMethodSecurity`, `@PreAuthorize` |
| Errors | `@ExceptionHandler`, `@ResponseStatus` |

---

### 7.2 Entity-by-Entity Deep Dive

For each entity: **why it exists**, **what it models**, **its fields**, and **the interesting annotations on it**.

---

#### 7.2.1 `User` → table `users`

**Why it exists:** The system must authenticate staff and know *who performed each operation* (accountability). The pharmacy has different roles (admin, pharmacist, cashier, inventory manager, auditor, procurement officer) with different permissions.

**Fields:**

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | `@Id` + `@GeneratedValue(IDENTITY)` |
| `username` | `String` | `@NotBlank`, `@Size(3..50)`, `@Column(unique=true)` |
| `password` | `String` | `@NotBlank`, `@Size(min=6)`, `@JsonProperty(WRITE_ONLY)` — **never serialized to JSON**; stored as a BCrypt hash |
| `email` | `String` | `@NotBlank`, `@Email`, `@Column(unique=true)` |
| `fullName` | `String` | `@NotBlank`, `@Size(max=100)` |
| `role` | `Role` (enum) | `@Enumerated(STRING)` — business role kept for UI backward compatibility; **not** used by Spring Security |
| `authorities` | `Set<Authority>` | `@ManyToMany(fetch=LAZY)` + `@JoinTable("user_authorities")` — the **real** source of truth for security |
| `active` | `boolean` | `@Column(nullable=false)`, default `true`; inactive users are rejected at login (`enabled` flag in `UserDetails`) |
| `createdAt`, `updatedAt` | `LocalDateTime` | set by `@PrePersist` / `@PreUpdate` |

**Why the two-role design (`role` + `authorities`)?**
- The pharmacy UI was built around a single `role` field, so it is kept (`Role` enum).
- Spring Security, however, is fed from the generic `authorities` table through a many-to-many join — authority names are **database-driven and configurable**, nothing hard-coded in the security config.
- `DataInitializer` back-fills `authorities` from `role` for existing users, and `UserServiceImpl.syncAuthoritiesFromRole(...)` keeps them in sync on create/update.

---

#### 7.2.2 `Role` (enum, not a table)

```java
public enum Role { ADMIN, PHARMACIST, CASHIER, INVENTORY_MANAGER, PROCUREMENT_OFFICER, AUDITOR }
```

**Why it exists:** a closed set of business roles the pharmacy understands. Stored as a string (`@Enumerated(STRING)`) so the DB stays readable and reordering the enum never corrupts data.

---

#### 7.2.3 `Authority` → table `authorities`

**Why it exists:** generic permission names that Spring Security actually reads. Because they live in the database, adding a new authority requires no code change — only a row.

**Fields:** `id` (PK), `name` (`@NotBlank`, `@Column(unique=true)`).

**Why an `Authority` entity instead of hard-coding `ROLE_...` strings in `SecurityConfig`?** Requirement-level decision: "No authority name is hard-coded in the security configuration — everything is read dynamically from the database at runtime." It also allows *generic* (non-pharmacy) authority names.

---

#### 7.2.4 `Category` → table `categories`

**Why it exists:** medicines need grouping (antibiotics, analgesics, vitamins…) for catalog organization and reporting.

**Fields:** `id`, `name` (`@NotBlank`, `@Size(max=100)`, unique), `description` (`@Size(max=500)`), `medicines` (`@OneToMany(mappedBy="category")` + `@JsonIgnore` to prevent circular JSON), `createdAt`, `updatedAt`.

**Interesting annotations:** `@JsonIgnore` on the collection — otherwise serializing a category would serialize every medicine, and each medicine would serialize its category again → infinite recursion / huge payloads.

---

#### 7.2.5 `Manufacturer` → table `manufacturers`

**Why it exists:** tracks who *produces* the medicine (name, address, phone, email, country). Needed for recall/traceability and catalog filtering.

**Fields:** `id`, `name` (unique, `@NotBlank`, `@Size(max=200)`), `address`, `phone`, `email` (`@Email`), `country`, `medicines` (`@OneToMany` + `@JsonIgnore`), `createdAt`, `updatedAt`.

---

#### 7.2.6 `Medicine` → table `medicines`

**Why it exists:** the heart of the catalog — every product the pharmacy sells.

**Fields:**

| Field | Notes |
|---|---|
| `id` | PK |
| `code` | `@NotBlank`, `@Size(max=50)`, **unique** — a stable business identifier |
| `name` | `@NotBlank`, `@Size(max=200)` |
| `description` | `@Size(max=1000)` |
| `category` | `@NotNull` + `@ManyToOne` + `@JoinColumn(nullable=false)` — every medicine must belong to a category |
| `manufacturer` | `@NotNull` + `@ManyToOne` + `@JoinColumn(nullable=false)` |
| `unit` | `@NotBlank`, `@Size(max=50)` — e.g. "tablet", "bottle", "ml" |
| `requiresPrescription` | `boolean`, default `false` — prescription-only flag for the UI |
| `active` | `boolean`, default `true` — soft deactivation instead of hard delete |
| `createdAt`, `updatedAt` | lifecycle timestamps |

**Why `@ManyToOne` (not an embedded string)?** Keeping `category`/`manufacturer` as entities gives referential integrity (FK constraints), lets the UI filter by `categoryId`, and avoids duplicate names.

---

#### 7.2.7 `Supplier` → table `suppliers`

**Why it exists:** procurement — who supplies stock. `InventoryBatch` links each batch to a supplier for traceability.

**Fields:** `id`, `code` (unique), `name` (`@NotBlank`), `contactPerson`, `phone`, `email` (`@Email`), `address`, `city`, `country`, `active` (default `true`), `createdAt`, `updatedAt`.

---

#### 7.2.8 `InventoryBatch` → table `inventory_batches`

**Why it exists:** pharmacies receive medicine in *batches* (a batch number, manufacturing date, expiry date, unit cost, unit price). Batch-level tracking is what lets the system:
- **Never sell expired stock** (dispensing picks only non-expired batches, ordered by expiry date).
- Track which supplier delivered what.
- Record **cost** and **sell price** per batch (used for the 20% markup pricing).

**Fields:**

| Field | Notes |
|---|---|
| `id` | PK |
| `batchNumber` | `@NotBlank`, `@Size(max=50)`, **unique** |
| `medicine` | `@NotNull` + `@ManyToOne` |
| `supplier` | `@NotNull` + `@ManyToOne` |
| `quantity` | `int`, `@NotNull`, `@Min(0)` — **total ever received** (only grows on stock-in) |
| `quantityRemaining` | `int`, `@NotNull`, `@Min(0)` — **live balance** (decreases on stock-out/dispense) |
| `unitCost` | `BigDecimal`, `@DecimalMin("0.0", inclusive=false)`, `precision=10, scale=2` — cost basis for pricing |
| `unitPrice` | `BigDecimal`, same constraints — the batch's own sell price |
| `manufacturingDate` | `LocalDate`, `@PastOrPresent` |
| `expiryDate` | `LocalDate`, `@Future` — must be in the future when created |
| `expired` | `boolean`, default `false` — flag for expired batches |
| `createdAt`, `updatedAt` | lifecycle timestamps |

**Why `quantity` AND `quantityRemaining`?** `quantity` = cumulative received (audit-friendly), `quantityRemaining` = what's actually on the shelf. Stock-in adds to both; stock-out/dispense only subtracts from `quantityRemaining`. This means "how much did we ever buy" and "how much is left" are both answerable without summing movement rows.

**Why is quantity never accepted from the API?** `createInventoryBatch` force-sets `quantity = 0` and `quantityRemaining = 0` — quantity is *only* changed by stock movements. The batch PATCH endpoint even strips `quantity`/`quantityRemaining` from the request (`updates.remove("quantity")`). This protects inventory integrity.

---

#### 7.2.9 `StockMovement` → table `stock_movements`

**Why it exists:** a complete, immutable history of every stock change — the backbone of the "auditable supply chain". Every in/out/adjustment is a row with type, quantity, batch, medicine, who did it, when, and optional reference/notes.

**Fields:**

| Field | Notes |
|---|---|
| `id` | PK |
| `movementType` | `@NotNull` + `@Enumerated(STRING)` — `STOCK_IN`, `STOCK_OUT`, `ADJUSTMENT`, `RETURN`, `EXPIRED_REMOVAL` |
| `medicine` | `@NotNull` + `@ManyToOne` |
| `inventoryBatch` | `@NotNull` + `@ManyToOne` |
| `quantity` | `int`, `@NotNull`, `@Positive` (always > 0) |
| `performedBy` | `@NotNull` + `@ManyToOne` → `User` — accountability |
| `referenceNumber` | `@Size(max=100)` — e.g. the dispensing number |
| `notes` | `@Size(max=1000)` |
| `movementDate` | `LocalDateTime`, set to `now` by `@PrePersist` if null |
| `createdAt` | set by `@PrePersist` |

**Why `@Positive`?** A movement of 0 or negative quantity is meaningless; direction is encoded by the *type*, not the sign.

---

#### 7.2.10 `Prescription` → table `prescriptions`

**Why it exists:** records a doctor's order — patient, doctor, hospital, and a list of medicine items. `Prescription` drives the automatic dispensing workflow.

**Fields:**

| Field | Notes |
|---|---|
| `id` | PK |
| `prescriptionNumber` | `@NotBlank`, **unique** |
| `patientName` | `@NotBlank`, `@Size(max=200)` |
| `patientIdNumber` | `@Size(max=50)` |
| `doctorName` / `hospitalName` | `@Size(max=200)` |
| `prescriptionDetails` | `TEXT` column (`columnDefinition = "TEXT"`) — free-form notes |
| `dispensed` | `boolean` — becomes `true` after auto-dispensing |
| `voided` | `boolean` — set when a dispensing record is voided by the cashier |
| `dispensedDate`, `dispensedBy` | audit fields for *when/who* dispensed |
| `items` | `@OneToMany(mappedBy="prescription", cascade=ALL, orphanRemoval=true, fetch=EAGER)` + `@JsonIgnoreProperties("prescription")` |
| `createdAt`, `updatedAt` | lifecycle timestamps |

**Why `cascade = CascadeType.ALL` + `orphanRemoval = true`?** The items only exist as part of the prescription. Saving the prescription saves its items; removing an item from the list removes it from the DB. This makes editing a prescription a single `save` call.

**Why `fetch = EAGER` here?** The UI always displays a prescription *with* its items, so EAGER avoids a second query and any lazy-loading problems during JSON serialization. (Note: EAGER should be used sparingly — it's justified here because items are small and always needed.)

**Why `@JsonIgnoreProperties("prescription")` on the items collection?** Each `PrescriptionItem` has a `@JsonIgnore` on its own `prescription` back-reference, and this annotation additionally protects the serialization path — no infinite recursion.

---

#### 7.2.11 `PrescriptionItem` → table `prescription_items`

**Why it exists:** one medicine line within a prescription, with dosage instructions.

**Fields:** `id`, `medicine` (`@NotNull` + `@ManyToOne`), `quantity` (`@Positive` — per dose), `dosage` (`@Size(max=100)`), `timesPerDay` (`@Min(1)`, default 1), `durationDays` (`@Min(1)`, default 1), `notes` (`@Size(max=500)`), `prescription` (`@ManyToOne` + `@JsonIgnore` back-reference), `createdAt`.

**Business helper:** `getTotalQuantity()` returns `quantity × timesPerDay × durationDays` — the *total units needed for the whole course*. The dispensing service uses this to know how much stock to take from batches.

---

#### 7.2.12 `DispensingRecord` → table `dispensing_records`

**Why it exists:** the physical act of handing medicine to the patient. It ties together a prescription, medicine, specific batch, quantity, prices, the pharmacist who dispensed, and the cashier's payment workflow (`PENDING → PAID | VOIDED`).

**Fields:**

| Field | Notes |
|---|---|
| `id` | PK |
| `dispensingNumber` | `@NotBlank`, **unique** |
| `prescription` | `@NotNull` + `@ManyToOne` |
| `medicine` | `@NotNull` + `@ManyToOne` |
| `inventoryBatch` | `@NotNull` + `@ManyToOne` — which specific batch the units came from (batch-level traceability) |
| `quantityDispensed` | `int`, `@NotNull`, `@Positive` |
| `unitPrice` | `BigDecimal`, `@DecimalMin(>0)`, `precision=10, scale=2` — **computed server-side** (cost × 1.2) |
| `totalPrice` | `BigDecimal`, same — unitPrice × quantity, computed server-side |
| `dispensedBy` | `@NotNull` + `@ManyToOne` → pharmacist |
| `paymentStatus` | `@Enumerated(STRING)`, default `PENDING` — `PENDING`, `PAID`, `VOIDED` |
| `processedBy`, `processedAt` | the cashier who approved/voided + when |
| `dispensingDate`, `createdAt` | timestamps |

**Why separate `DispensingRecord` from `StockMovement`?** A stock movement is a *quantity event*; a dispensing record is a *financial + clinical transaction* with prices and payment state. Dispensing *also* writes a `STOCK_OUT` movement, but the two are kept separate so the cashier workflow (approve/void) doesn't corrupt the movement history. Voiding restores stock and writes a compensating `STOCK_IN` movement — the audit trail stays truthful.

---

#### 7.2.13 `AuditLog` → table `audit_logs`

**Why it exists:** the project's headline requirement — *complete traceability*. Every critical operation records: entity type, entity ID, action, old values, new values, who did it, from which IP, and when.

**Fields:**

| Field | Notes |
|---|---|
| `id` | PK |
| `entityType` | `@NotBlank`, `@Size(max=100)` — e.g. `"Medicine"`, `"DispensingRecord"` |
| `entityId` | `@NotNull` `Long` |
| `action` | `@Enumerated(STRING)` — `CREATE`, `UPDATE`, `DELETE`, `LOGIN`, `LOGOUT`, `DISPENSE`, `STOCK_IN`, `STOCK_OUT` |
| `oldValues` / `newValues` | `TEXT` (`columnDefinition = "TEXT"`) — human-readable before/after summaries |
| `performedBy` | `@NotNull` + `@ManyToOne` → `User` |
| `ipAddress` | `@NotBlank`, `@Size(max=50)` |
| `timestamp` | `LocalDateTime`, set by `@PrePersist` |

**Why a string JSON-ish summary instead of structured fields?** Simple, flexible, and cheap to store; the important part is *human-readable provenance*, not queryable diffs. The `TEXT` column definition supports large audit trails.

**Who writes audit logs?** Every service method (create/update/delete/dispense/stock-in/stock-out), plus `SecurityConfig` on successful **login and logout**. `AuditLogServiceImpl.logAction(...)` centralizes the write and silently skips when there's no authenticated user (e.g. system-triggered ops).

---

### 7.3 Entity Relationship Diagram (textual)

```
users  *───*  authorities            (via join table user_authorities)
 │
 │ 1
 ├────< stock_movements.performed_by_user_id
 ├────< dispensing_records.dispensed_by_user_id / processed_by_user_id
 ├────< prescriptions.dispensed_by_user_id
 └────< audit_logs.performed_by_user_id

categories 1───* medicines *───1 manufacturers
                        │
                        │ 1
                        ├────< inventory_batches (medicine_id)
                        ├────< stock_movements (medicine_id)
                        ├────< dispensing_records (medicine_id)
                        └────< prescription_items (medicine_id)

suppliers 1───* inventory_batches (supplier_id)

inventory_batches 1───* stock_movements (inventory_batch_id)
inventory_batches 1───* dispensing_records (inventory_batch_id)

prescriptions 1───* prescription_items (prescription_id)
prescriptions 1───* dispensing_records (prescription_id)
```

Cardinality summary:
- `users : authorities` = **many-to-many** (`user_authorities`).
- `category : medicine` = **one-to-many**.
- `manufacturer : medicine` = **one-to-many**.
- `medicine : inventory_batch` = **one-to-many**.
- `supplier : inventory_batch` = **one-to-many**.
- `inventory_batch : stock_movement` = **one-to-many**.
- `prescription : prescription_item` = **one-to-many** (cascade + orphan removal).
- `prescription : dispensing_record` = **one-to-many**.
- Everything referencing a `User` (performed by / dispensed by / processed by) = **many-to-one**.

---

## 8. The DAO / Repository Layer

**Files:** `dao/*.java` (interfaces) + `repository/*DAOImpl.java` (implementations).

### Why a DAO layer at all?
- It is the **contract** between services and the database.
- Services depend on the *interface*, never the implementation — so persistence could be swapped (JDBC, JPA, even an in-memory mock) without touching business logic.
- Unit tests can mock the interface (`mock(MedicineDAO.class)`).

### Why `EntityManager` + JPQL instead of Spring Data `JpaRepository`?
This is a deliberate, documented choice (visible in every `*DAOImpl` header comment): *"Uses EntityManager directly (not Spring Data JpaRepository) to give full control over JPQL queries. This matches the DAO pattern taught in class."*

| Aspect | This project | Spring Data JPA |
|---|---|---|
| Query control | Explicit JPQL in `TypedQuery` — you see exactly what runs | Derived method names / `@Query` |
| Teaching fit | Classic DAO pattern (interface + impl) | Magic proxies |
| Explicit JOIN FETCH | `findByUsernameWithAuthorities` uses `LEFT JOIN FETCH` | Needs `@Query` anyway |

### How a DAO implementation is built (example: `MedicineDAOImpl`)

```java
@Repository                              // Spring registers it + translates JPA exceptions
public class MedicineDAOImpl implements MedicineDAO {

    private final EntityManager entityManager;   // injected by Spring's JPA infrastructure

    @Autowired
    public MedicineDAOImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Medicine findByCode(String code) {
        TypedQuery<Medicine> query = entityManager.createQuery(
            "FROM Medicine WHERE code = :code", Medicine.class);
        query.setParameter("code", code);
        List<Medicine> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public Medicine save(Medicine medicine) {
        if (medicine.getId() == null) {
            entityManager.persist(medicine);   // INSERT
            return medicine;
        } else {
            return entityManager.merge(medicine); // UPDATE (or re-attach)
        }
    }
}
```

### Key JPQL patterns used across the DAOs

| Pattern | Purpose | Example |
|---|---|---|
| `FROM Entity WHERE field = :param` | Simple filter | `findByCode`, `findByMovementType` |
| `LOWER(name) LIKE LOWER(CONCAT('%', :name, '%'))` | Case-insensitive partial search | `searchByName` |
| `SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.authorities WHERE u.username = :username` | Eagerly load the lazy collection in the same query — avoids `LazyInitializationException` during login | `UserDAOImpl.findByUsernameWithAuthorities` |
| `WHERE expiryDate <= :date AND expired = false` | Expiring-batch alert | `findExpiringBatches` |
| `WHERE medicine.id = :id AND quantityRemaining > 0 AND expired = false ORDER BY expiryDate ASC` | Only sellable batches, **oldest-expiry first** (FEFO — First Expired, First Out) | `findAvailableBatchesByMedicineId` |
| `SELECT COUNT(u) FROM User u WHERE u.username = :username` | Existence checks without loading the entity | `existsByUsername`, `existsByName` |
| `WHERE timestamp BETWEEN :start AND :end` | Date-range reports | `findByTimestampBetween` |

**The `save()` convention (persist vs merge)** appears identically in every DAO impl: new entity (null id) → `persist` (INSERT); existing entity (non-null id) → `merge` (UPDATE). This one method handles both create and update.

---

## 9. The Service Layer (Business Logic)

**Files:** `service/*.java` (interfaces) + `service/impl/*ServiceImpl.java`.

### The interface-first design
- Controllers depend on interfaces (`MedicineService`, `StockMovementService`, …), not implementations.
- This enables swapping implementations and mocking in tests.
- `@Override` on every implementation method makes the contract explicit.

### The three big responsibilities of the service layer

1. **Business rules & validations** — duplicate checks (e.g. "Medicine code already exists"), state-machine rules ("Only pending dispensing records can be approved"), stock checks ("Insufficient stock. Available: X, Requested: Y").
2. **Transaction boundaries** — `@Transactional` at class level: every public method runs in a transaction; any `RuntimeException` rolls everything back.
3. **Audit logging** — every mutating method calls `auditLogService.logAction(...)` with the current user + IP from `CurrentUserProvider`.

### `@Transactional` explained (the most important service annotation)

```java
@Service
@Transactional          // <-- every method runs inside a transaction
public class MedicineServiceImpl implements MedicineService { ... }
```

| Variant | Effect |
|---|---|
| `@Transactional` (class or method) | A DB transaction is opened before the method and committed after; **if any exception propagates, the transaction is rolled back** — no partial writes. |
| `@Transactional(readOnly = true)` (on read methods) | Hibernate skips dirty-checking and doesn't acquire write locks; faster reads and signals intent. |

**Why this matters for PharmaTrack:** `DispensingRecordServiceImpl.createDispensingRecord` performs **three related writes** — (1) decrement the batch's `quantityRemaining`, (2) save a `STOCK_OUT` movement, (3) save the dispensing record. If step 2 or 3 failed *without* a transaction, the stock would be deducted but no record would exist — data corruption. With `@Transactional`, all three commit together or none do.

### Constructor injection (why `final` fields + `@Autowired`)

```java
private final MedicineDAO medicineDAO;
private final AuditLogService auditLogService;
private final CurrentUserProvider currentUserProvider;

@Autowired
public MedicineServiceImpl(MedicineDAO medicineDAO, AuditLogService auditLogService,
                           CurrentUserProvider currentUserProvider) { ... }
```

- **Constructor injection** (vs field injection): dependencies are explicit, the object is never partially constructed, and tests can pass mocks.
- **`final` fields**: guarantee the service can't be left without its dependencies.
- **`@Autowired` kept explicitly** for readability even though Spring 4.3+ auto-wires a single constructor.

### Service-by-service highlights

| Service | Notable business logic |
|---|---|
| `AuthServiceImpl` | Registration: duplicate username/email checks → **BCrypt-encode password** → resolve/create `Authority` rows → derive business `Role` if the name matches → save → audit. `getCurrentUser` loads user *with* authorities. |
| `UserServiceImpl` | Create/update/delete users; password re-encoding only when a *new* password is supplied (compares with `passwordEncoder.matches`); `syncAuthoritiesFromRole` keeps the DB authorities in sync with the business role. |
| `CategoryServiceImpl` / `ManufacturerServiceImpl` / `SupplierServiceImpl` | Standard CRUD with duplicate-name/code checks and audit logging. |
| `MedicineServiceImpl` | CRUD + duplicate `code` check; search by name; audit on create/update/delete. |
| `InventoryBatchServiceImpl` | CRUD; **force-sets quantity=0 on create** (quantity is movement-managed); expiry queries; expiring-batch listing. |
| `StockMovementServiceImpl` | `processStockIn`: validates batch + medicine + logged-in user, then `quantity += n`, `quantityRemaining += n`, saves movement, audits `STOCK_IN`. `processStockOut`: validates stock first (`quantityRemaining >= requested`), then only decrements `quantityRemaining`, saves movement, audits `STOCK_OUT`. |
| `PrescriptionServiceImpl` | **The most complex flow.** Create → validate items non-empty → set back-references → save → then **auto-dispense every item** from available batches (FEFO), creating one `DispensingRecord` per batch slice, and mark the prescription `dispensed`. Also: cannot edit/delete dispensed or voided prescriptions. |
| `DispensingRecordServiceImpl` | Create (deduct stock, log movement, compute prices server-side); `approvePayment` (PENDING→PAID, records cashier + time); `voidDispensing` (PENDING→VOIDED, **restores stock** via `STOCK_IN` movement, and marks the linked prescription voided). |
| `AuditLogServiceImpl` | Central `logAction(...)` that builds and persists `AuditLog` rows; skips when `userId == null`; query helpers (by entity, action, user, date range). |

---

## 10. The Controller Layer (REST API)

**Files:** `controller/*.java`.

### What controllers do (and don't do)
Controllers are **thin**:
- **Do:** parse the HTTP request (`@RequestBody`, `@PathVariable`, `@RequestParam`), enforce validation (`@Valid`), pick the HTTP status (`ResponseEntity`), apply authorization (`@PreAuthorize`).
- **Don't:** contain business logic, touch the database, or do persistence — all delegated to services.

### The standard controller anatomy

```java
@RestController                                  // JSON in/out
@RequestMapping("/api/medicines")                // base path
@CrossOrigin(origins = "*")                      // allow the React SPA (dev)
@PreAuthorize("hasAnyAuthority(@appAuthorities.admin, @appAuthorities.inventoryManager)")
public class MedicineController {

    private final MedicineService medicineService;

    @Autowired
    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @PostMapping
    public ResponseEntity<Medicine> createMedicine(@Valid @RequestBody Medicine medicine) {
        Medicine created = medicineService.createMedicine(medicine);
        return new ResponseEntity<>(created, HttpStatus.CREATED);   // 201
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medicine> getMedicineById(@PathVariable Long id) {
        return ResponseEntity.ok(medicineService.getMedicineById(id));  // 200
    }

    @GetMapping("/search")
    public ResponseEntity<List<Medicine>> searchMedicines(@RequestParam String name) { ... }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicine(@PathVariable Long id) {
        medicineService.deleteMedicine(id);
        return ResponseEntity.noContent().build();                     // 204
    }
}
```

### What each annotation on a controller method does

| Annotation | Role |
|---|---|
| `@PostMapping` | Handle `POST` → create → return `201 Created`. |
| `@PutMapping("/{id}")` | Handle `PUT` → full replace/update. |
| `@PatchMapping("/{id}")` | Handle `PATCH` → partial update: the controller fetches the existing entity, applies the JSON diff with `new ObjectMapper().updateValue(existing, updates)`, then delegates to the service's update method. |
| `@GetMapping` / `@GetMapping("/{id}")` | Handle `GET` for all / one. |
| `@DeleteMapping("/{id}")` | Handle `DELETE` → `204 No Content`. |
| `@PathVariable Long id` | Binds `{id}` from the URL. |
| `@RequestParam String name` | Binds `?name=...`. |
| `@RequestBody` (+ `@Valid`) | Deserializes + validates the JSON body. |
| `@DateTimeFormat(iso = …)` | Parses date/datetime query params into `LocalDate`/`LocalDateTime`. |

### The full endpoint map

| Controller | Base path | Allowed authorities (via `@PreAuthorize`) |
|---|---|---|
| `AuthController` | `/api/auth` | `/register` → ADMIN; `/me` → any authenticated |
| `UserController` | `/api/users` | ADMIN |
| `CategoryController` | `/api/categories` | ADMIN, INVENTORY_MANAGER |
| `ManufacturerController` | `/api/manufacturers` | ADMIN, INVENTORY_MANAGER |
| `SupplierController` | `/api/suppliers` | ADMIN, INVENTORY_MANAGER |
| `MedicineController` | `/api/medicines` | ADMIN, INVENTORY_MANAGER |
| `InventoryBatchController` | `/api/inventory-batches` | ADMIN, INVENTORY_MANAGER |
| `StockMovementController` | `/api/stock-movements` | ADMIN, INVENTORY_MANAGER |
| `PrescriptionController` | `/api/prescriptions` | ADMIN, PHARMACIST |
| `DispensingRecordController` | `/api/dispensing-records` | ADMIN, CASHIER |
| `AuditLogController` | `/api/audit-logs` | ADMIN, AUDITOR |
| `DemoController` | `/api/demo` | `/public` → anyone; `/authenticated` → logged in; `/admin` → ADMIN; `/pharmacist` → PHARMACIST |

**Why class-level `@PreAuthorize` on most controllers?** One rule protects every endpoint of that controller — a *single point of configuration* per resource. Exception: `AuthController.register` uses *method-level* `@PreAuthorize` because the two endpoints have different rules.

**Why a `DemoController`?** It exists purely to *demonstrate* the authentication/authorization split for the project defense: public vs authenticated vs role-protected endpoints all in one small file.

**The "never trust the client" rule in controllers:** identity-bearing fields (`performedBy`, `dispensedBy`, `processedBy`, cashier) are **never** taken from the request body — the service always resolves the currently logged-in user via `CurrentUserProvider`. Comments on the approve/void endpoints state this explicitly.

---

## 11. The Security Layer

**Files:** `security/*.java`.

### 11.1 `SecurityConfig` — the filter chain

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper,
                                                   UserDAO userDAO, AuditLogService auditLogService) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/error", "/favicon.ico").permitAll()
                .requestMatchers("/api/demo/public").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .successHandler(...)   // JSON 200 + audit LOGIN
                .failureHandler(...))  // JSON 401
            .logout(logout -> logout.logoutSuccessHandler(...))  // 204 + audit LOGOUT
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(...)  // JSON 401 for unauthenticated
                .accessDeniedHandler(...));     // JSON 403 for insufficient authority
        return http.build();
    }
}
```

**Every decision explained:**

| Decision | Why |
|---|---|
| `@EnableMethodSecurity` | Activates `@PreAuthorize` on controllers — without it the annotations are ignored. |
| `PasswordEncoder` bean = `BCryptPasswordEncoder` | BCrypt adds a **random salt per hash**, making rainbow-table attacks impractical. Every password is encoded with this bean before saving. |
| `SecurityFilterChain` (not the old `WebSecurityConfigurerAdapter`) | The modern Spring Security 6 declarative DSL; the old class is deprecated. |
| CSRF disabled | This is an API-first SPA using session cookies without CSRF tokens. Documented trade-off; re-enable for server-rendered deployments. |
| URL rules only decide "public vs authenticated" | **No role names in URL rules.** Which authority an operation needs is decided by `@PreAuthorize` referencing the configurable `AppAuthorities` bean. |
| Form login with JSON handlers | The SPA needs machine-readable responses: success → `{"message":"Login successful"}` with 200; failure → structured JSON 401. |
| Custom `authenticationEntryPoint` / `accessDeniedHandler` | Unauthenticated → **401** JSON; authenticated-but-unauthorized → **403** JSON. Consistent error contract for the frontend. |
| LOGIN/LOGOUT audit | `logAuditEvent(...)` records a `LOGIN` / `LOGOUT` row in `audit_logs` — the audit trail covers even authentication events. |

### 11.2 `AppUserDetailsService` — how login loads a user

```java
@Service
public class AppUserDetailsService implements UserDetailsService {

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userDAO.findByUsernameWithAuthorities(username);  // JOIN FETCH authorities
        if (user == null) throw new UsernameNotFoundException("User not found: " + username);

        List<GrantedAuthority> grantedAuthorities = user.getAuthorities().stream()
            .map(Authority::getName)
            .map(SimpleGrantedAuthority::new)
            .map(authority -> (GrantedAuthority) authority)
            .toList();

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(), user.getPassword(),
            user.isActive(),   // enabled — inactive users cannot log in
            true, true, true,
            grantedAuthorities);
    }
}
```

- Spring Security calls this during form login to fetch the user by username.
- The DB `Authority` rows are converted into `GrantedAuthority` objects so `hasAuthority(...)` / `hasAnyAuthority(...)` work.
- `@Transactional(readOnly = true)` + `LEFT JOIN FETCH` ensure the lazily-loaded `authorities` collection is available (otherwise `LazyInitializationException`).
- `user.isActive()` maps to the `enabled` flag → deactivated users are rejected at login.

### 11.3 `CurrentUserProvider` — "who am I right now?"

```java
@Component
public class CurrentUserProvider {
    public User getCurrentUser() { ... reads SecurityContextHolder ... }
    public Long getCurrentUserId() { ... }
    public String getClientIp() { ... reads RequestContextHolder, else "unknown" ... }
}
```

**Why this helper exists:** the service layer needs "the currently logged-in user" and "the caller's IP" for every audit entry. Without it, every service would duplicate `SecurityContextHolder` / `RequestContextHolder` boilerplate. It returns `null`/`"unknown"` gracefully when there's no authenticated user or no active HTTP request (e.g., system-triggered ops).

### 11.4 `AppAuthorities` — configurable authority names

```java
@Component
public class AppAuthorities {
    @Value("${app.security.authorities.admin:ADMIN}")  private String admin;
    @Value("${app.security.authorities.pharmacist:PHARMACIST}") private String pharmacist;
    // ...cashier, inventoryManager, auditor
}
```

**Why a bean for names that are just strings?** So `@PreAuthorize` can reference the bean in SpEL: `@PreAuthorize("hasAuthority(@appAuthorities.admin)")`. The required authority can be changed in `application.properties` **without touching Java code**, and it is matched against authorities stored in the database. Nothing security-related is hard-coded.

---

## 12. The Exception Handling Layer

**Files:** `exception/*.java` + `dto/ErrorResponse.java`.

### Custom exceptions

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
// e.g. new ResourceNotFoundException("Medicine", "id", 42L)
//   → "Medicine not found with id: '42'"
```

| Exception | HTTP status | Used for |
|---|---|---|
| `ResourceNotFoundException` | 404 | "Medicine not found with id: '42'" |
| `BadRequestException` | 400 | Duplicate codes/numbers, insufficient stock, invalid workflow states ("Only pending dispensing records can be approved"), "A dispensed prescription cannot be edited" |
| `ConstraintViolationException` (JPA-level) | 400 | DB-level validation failures |
| `MethodArgumentNotValidException` (from `@Valid`) | 400 | Request-body validation failures |
| `AccessDeniedException` (from `@PreAuthorize`) | 403 | Authenticated but missing authority |
| any other `Exception` | 400 (catch-all) | Unexpected errors without leaking stack traces |

### `GlobalExceptionHandler` — `@RestControllerAdvice`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException exc) { ... }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exc) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exc.getBindingResult().getFieldErrors().forEach(fe ->
            fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        // → { "status":400, "error":"Validation Failed", "validationErrors": {"name":"...", ...} }
    }
    // ... plus handlers for BadRequestException, ConstraintViolationException,
    //     AccessDeniedException, and a catch-all Exception handler
}
```

**Why this pattern:** services just `throw` descriptive exceptions ("fail fast"); the advice centralizes the JSON formatting so no controller needs try/catch blocks and every error has the same shape (`ErrorResponse`).

**Why handle `AccessDeniedException` here?** Without it, a `@PreAuthorize` denial could fall into the catch-all and return 400 instead of 403, breaking the API contract.

### `ErrorResponse` DTO

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;          // "Not Found", "Bad Request", "Forbidden"...
    private String message;        // human-readable detail
    private String path;           // request URI
    private Map<String, String> validationErrors;  // field → message
}
```

- Consistent shape: `timestamp`, `status`, `error`, `message`, optional `path` and `validationErrors`.
- `@JsonInclude(NON_NULL)` drops optional fields when irrelevant → clean payloads.
- Used both by `GlobalExceptionHandler` and by the security handlers in `SecurityConfig`.

---

## 13. The DTO Layer (Data Transfer Objects)

**Files:** `dto/*.java`.

### Why DTOs? (Why not just send entities?)
- **Security:** the `User` entity would serialize its `password` if returned raw — DTOs (and `@JsonProperty(WRITE_ONLY)`) prevent that.
- **Precision:** a request DTO carries *exactly* the fields the client may send; nothing else can be smuggled in.
- **Decoupling:** the API contract is independent of the database model.

### The three DTOs

**1. `RegisterRequest`** — body of `POST /api/auth/register`:
```java
@NotBlank @Size(min=3, max=50) String username;
@NotBlank @Size(min=6)         String password;
@NotBlank @Email               String email;
@NotBlank @Size(max=100)       String fullName;
@NotEmpty List<String> authorityNames;   // e.g. ["ADMIN"], ["PHARMACIST","AUDITOR"]
```
- `@NotEmpty` on `authorityNames` enforces the requirement "one or more authorities".
- Validated by `@Valid` in the controller before the service runs.

**2. `StockMovementRequest`** — body of `POST /api/stock-movements/stock-in|stock-out`:
```java
@NotNull Long medicineId;
@NotNull Long inventoryBatchId;
@NotNull @Positive int quantity;
String referenceNumber;
String notes;
```
- The movement **type** is not in the body — it's implied by which URL you call (`/stock-in` vs `/stock-out`), and the *user* is never in the body (resolved server-side).

**3. `ErrorResponse`** — see section 12.

> **Note on the rest of the API:** create/update endpoints take entity objects directly (`@Valid @RequestBody Medicine`). This is a pragmatic shortcut in this project — full DTO-per-resource would be more rigid but adds many files. The critical paths (auth, stock movement, errors) do use dedicated DTOs where it matters.

---

## 14. The Config Layer — `DataInitializer`

**File:** `config/DataInitializer.java`

```java
@Component
public class DataInitializer implements CommandLineRunner { ... }
```

`CommandLineRunner` → its `run(...)` method executes **once, right after the application context starts**.

**What it does (3 steps):**

1. **Seed authorities:** for every value in the `Role` enum, insert an `authority` row if missing (`ADMIN`, `PHARMACIST`, `CASHIER`, `INVENTORY_MANAGER`, `PROCUREMENT_OFFICER`, `AUDITOR`). Seed *data*, not hard-coded security config.
2. **Back-fill authorities for existing users:** any user that has a `role` but no authorities gets an `Authority` matching its role — so pre-upgrade users can authenticate after the DB-driven upgrade.
3. **Seed default admin:** if the `users` table is empty (and `app.security.seed-default-admin=true`), create `admin / admin123` with the `ADMIN` authority. Makes the app usable out of the box.

Key details:
- `@Transactional` on `run(...)` → all seeding happens in one transaction.
- Password is encoded with the injected `PasswordEncoder` (BCrypt), never stored in plain text.
- Property `@Value("${app.security.seed-default-admin:true}")` makes the behavior configurable.

---

## 15. Complete Database Schema (Tables Produced by JPA)

With `spring.jpa.hibernate.ddl-auto=update`, Hibernate creates these tables (names from `@Table` / `@Column` / `@JoinTable`):

```
users                    id BIGSERIAL PK, username VARCHAR(50) UNIQUE NOT NULL,
                         password VARCHAR(255) NOT NULL, email VARCHAR(255) UNIQUE NOT NULL,
                         full_name VARCHAR(100) NOT NULL, role VARCHAR(255),
                         active BOOLEAN NOT NULL, created_at TIMESTAMP NOT NULL,
                         updated_at TIMESTAMP NOT NULL

authorities              id BIGSERIAL PK, name VARCHAR(255) UNIQUE NOT NULL

user_authorities         user_id BIGINT NOT NULL → users.id,
                         authority_id BIGINT NOT NULL → authorities.id   (composite PK)

categories               id BIGSERIAL PK, name VARCHAR(100) UNIQUE NOT NULL,
                         description VARCHAR(500), created_at, updated_at

manufacturers            id BIGSERIAL PK, name VARCHAR(200) UNIQUE NOT NULL,
                         address VARCHAR(500), phone VARCHAR(50),
                         email VARCHAR(100), country VARCHAR(100), created_at, updated_at

medicines                id BIGSERIAL PK, code VARCHAR(50) UNIQUE NOT NULL,
                         name VARCHAR(200) NOT NULL, description VARCHAR(1000),
                         category_id BIGINT NOT NULL → categories.id,
                         manufacturer_id BIGINT NOT NULL → manufacturers.id,
                         unit VARCHAR(50) NOT NULL, requires_prescription BOOLEAN NOT NULL,
                         active BOOLEAN NOT NULL, created_at, updated_at

suppliers                id BIGSERIAL PK, code VARCHAR(50) UNIQUE NOT NULL,
                         name VARCHAR(200) NOT NULL, contact_person VARCHAR(200),
                         phone VARCHAR(50), email VARCHAR(100), address VARCHAR(500),
                         city VARCHAR(100), country VARCHAR(100), active BOOLEAN NOT NULL,
                         created_at, updated_at

inventory_batches        id BIGSERIAL PK, batch_number VARCHAR(50) UNIQUE NOT NULL,
                         medicine_id BIGINT NOT NULL → medicines.id,
                         supplier_id BIGINT NOT NULL → suppliers.id,
                         quantity INT NOT NULL, quantity_remaining INT NOT NULL,
                         unit_cost NUMERIC(10,2) NOT NULL, unit_price NUMERIC(10,2) NOT NULL,
                         manufacturing_date DATE NOT NULL, expiry_date DATE NOT NULL,
                         expired BOOLEAN NOT NULL, created_at, updated_at

stock_movements          id BIGSERIAL PK, movement_type VARCHAR(255) NOT NULL,
                         medicine_id BIGINT NOT NULL → medicines.id,
                         inventory_batch_id BIGINT NOT NULL → inventory_batches.id,
                         quantity INT NOT NULL, performed_by_user_id BIGINT NOT NULL → users.id,
                         reference_number VARCHAR(100), notes VARCHAR(1000),
                         movement_date TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL

prescriptions            id BIGSERIAL PK, prescription_number VARCHAR(255) UNIQUE NOT NULL,
                         patient_name VARCHAR(200) NOT NULL, patient_id_number VARCHAR(50),
                         doctor_name VARCHAR(200), hospital_name VARCHAR(200),
                         prescription_details TEXT, dispensed BOOLEAN NOT NULL,
                         voided BOOLEAN NOT NULL, dispensed_date TIMESTAMP,
                         dispensed_by_user_id BIGINT → users.id, created_at, updated_at

prescription_items       id BIGSERIAL PK, medicine_id BIGINT NOT NULL → medicines.id,
                         quantity INT NOT NULL, dosage VARCHAR(100),
                         times_per_day INT NOT NULL, duration_days INT NOT NULL,
                         notes VARCHAR(500), prescription_id BIGINT NOT NULL → prescriptions.id,
                         created_at TIMESTAMP NOT NULL

dispensing_records       id BIGSERIAL PK, dispensing_number VARCHAR(255) UNIQUE NOT NULL,
                         prescription_id BIGINT NOT NULL → prescriptions.id,
                         medicine_id BIGINT NOT NULL → medicines.id,
                         inventory_batch_id BIGINT NOT NULL → inventory_batches.id,
                         quantity_dispensed INT NOT NULL,
                         unit_price NUMERIC(10,2) NOT NULL, total_price NUMERIC(10,2) NOT NULL,
                         dispensed_by_user_id BIGINT NOT NULL → users.id,
                         payment_status VARCHAR(255) NOT NULL,
                         processed_by_user_id BIGINT → users.id, processed_at TIMESTAMP,
                         dispensing_date TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL

audit_logs               id BIGSERIAL PK, entity_type VARCHAR(100) NOT NULL,
                         entity_id BIGINT NOT NULL, action VARCHAR(255) NOT NULL,
                         old_values TEXT, new_values TEXT,
                         performed_by_user_id BIGINT NOT NULL → users.id,
                         ip_address VARCHAR(50) NOT NULL, timestamp TIMESTAMP NOT NULL
```

---

## 16. End-to-End Request Flow (a Full Walkthrough)

Let's trace two real scenarios from HTTP request to database.

### Scenario A — A pharmacist creates a prescription (the flagship flow)

1. **HTTP:** `POST /api/prescriptions` with a JSON body:
   ```json
   {
     "prescriptionNumber": "RX-1001",
     "patientName": "John Doe",
     "doctorName": "Dr. Smith",
     "items": [
       { "medicine": { "id": 5 }, "quantity": 1, "timesPerDay": 2, "durationDays": 5 }
     ]
   }
   ```
2. **Spring Security filter chain** (SecurityConfig): the request is authenticated (session cookie); no URL-level rule blocks it.
3. **`@PreAuthorize`** on `PrescriptionController` → the logged-in user must have `ADMIN` or `PHARMACIST` authority (checked against DB authorities). Otherwise → 403 JSON.
4. **`@Valid`** validates the body: `prescriptionNumber` not blank, items present, item fields valid. Otherwise → 400 with `validationErrors`.
5. **Controller** (`PrescriptionController.createPrescription`) delegates to `PrescriptionService.createPrescription(...)`.
6. **Service** (`PrescriptionServiceImpl.createPrescription`):
   - Duplicate number check → `BadRequestException` if taken.
   - Empty items check → `BadRequestException`.
   - Sets each item's back-reference to the prescription; saves → audit `CREATE`.
   - Resolves the current user (pharmacist) via `CurrentUserProvider`.
   - For each item: computes `totalQuantity = quantity × timesPerDay × durationDays`, loads available batches (FEFO, non-expired, stock > 0), and slices the required amount across batches, calling `dispensingRecordService.createDispensingRecord(...)` for each slice.
     - `createDispensingRecord`: stock check → price = `unitCost × 1.2` (server-side) → decrement `quantityRemaining` → save `STOCK_OUT` movement → save the record → audit `DISPENSE`. **All one transaction.**
   - Marks prescription `dispensed=true`, sets `dispensedDate`/`dispensedBy`, saves → audit `DISPENSE`.
   - **If anything fails** (e.g. insufficient stock), the whole transaction rolls back: no dispensing records, no stock deduction, no movement rows.
7. **DAO:** `PrescriptionDAOImpl.save` → `entityManager.persist` (INSERT) cascades to items; `InventoryBatchDAOImpl.save` → `merge` (UPDATE quantity); `StockMovementDAOImpl.save` → persist; `DispensingRecordDAOImpl.save` → persist; `AuditLogDAOImpl.save` → persist.
8. **Response:** `201 Created` with the dispensed prescription JSON (items eager-loaded, timestamps auto-set by `@PrePersist`).

### Scenario B — A cashier approves payment

1. `POST /api/dispensing-records/5/approve` (authenticated cashier/admin).
2. `@PreAuthorize` on the controller allows ADMIN/CASHIER.
3. Service `approvePayment(5)`:
   - Load record; not found → 404.
   - `paymentStatus != PENDING` → `BadRequestException` ("Only pending dispensing records can be approved").
   - Resolve cashier from the session (never from the client).
   - Set `PAID`, `processedBy = cashier`, `processedAt = now`; save → audit `UPDATE` (old `paymentStatus=PENDING` → new status).
4. Response: `200 OK` with the updated record.

### Scenario C — A void (the compensating flow)

`POST /api/dispensing-records/5/void`:
- Only `PENDING` records can be voided.
- Stock is **restored**: `quantityRemaining += quantityDispensed`, and a compensating `STOCK_IN` movement is recorded with the note `"Voided dispensing DSP-..."` — the movement history shows both the original out and the void in, so the trail is honest.
- The linked prescription is marked `voided` (cannot be dispensed again).
- Record → `VOIDED` with cashier + timestamp; audit `UPDATE`.

---

## 17. Key Business Logic Deep Dives

### 17.1 FEFO (First Expired, First Out) dispensing
`InventoryBatchDAO.findAvailableBatchesByMedicineId` returns only batches with `quantityRemaining > 0`, not `expired`, ordered by `expiryDate ASC`. `PrescriptionServiceImpl` then takes stock from the **oldest-expiring batches first** — this is the standard pharmacy practice that minimizes expiring stock. This directly addresses the requirement "prevent the sale of expired medicines".

### 17.2 Server-side pricing (never trust the client)
The dispensing price is computed in `DispensingRecordServiceImpl.createDispensingRecord`:
```java
BigDecimal unitPrice = batch.getUnitCost()
        .multiply(BigDecimal.valueOf(1.2))          // 20% markup
        .setScale(2, RoundingMode.HALF_UP);
BigDecimal totalPrice = unitPrice
        .multiply(BigDecimal.valueOf(quantityDispensed))
        .setScale(2, RoundingMode.HALF_UP);
dispensingRecord.setUnitPrice(unitPrice);
dispensingRecord.setTotalPrice(totalPrice);
```
- Price derives from the *batch's recorded cost*, guaranteeing profit.
- The client cannot send its own price (the entity annotations would also reject a null price, but the service overwrites it anyway).

### 17.3 `quantity` vs `quantityRemaining` — dual counters
| Event | `quantity` (total received) | `quantityRemaining` (live balance) |
|---|---|---|
| Create batch | 0 | 0 |
| Stock-in +N | +N | +N |
| Stock-out −N | unchanged | −N |
| Dispense −N | unchanged | −N |
| Void +N | unchanged | +N |

Why: `quantity` is a cumulative audit figure (how much of this batch did we ever receive), while `quantityRemaining` is the operational number (what's on the shelf). Because the API never lets you set either directly, inventory can only change through audited movements.

### 17.4 Workflow state machines
- **Prescription:** `created → dispensed` OR `created → voided`. A dispensed/voided prescription cannot be edited or deleted (guards in `updatePrescription`/`deletePrescription`).
- **DispensingRecord payment:** `PENDING → PAID` (cashier approves) or `PENDING → VOIDED` (cashier voids + stock restored). Transitions out of PENDING are rejected (`BadRequestException`).

### 17.5 Password security
- Encoded with `BCryptPasswordEncoder` (salted) at registration and on every user create/update.
- `@JsonProperty(WRITE_ONLY)` on `User.password` → never in any JSON response.
- Update re-encodes only when a genuinely new password is supplied (`!passwordEncoder.matches(new, existing)`).
- Deactivated users (`active=false`) are rejected at login via the `enabled` flag.

### 17.6 Audit trail completeness
Every mutating path writes an audit row with entity type, entity ID, action, old/new values, user ID, and IP:
- CRUD on users, categories, manufacturers, suppliers, medicines, batches, prescriptions.
- `STOCK_IN` / `STOCK_OUT` on movements.
- `DISPENSE` on prescriptions and dispensing records.
- `LOGIN` / `LOGOUT` on authentication events (from `SecurityConfig`).
- `AuditLogServiceImpl.logAction` centralizes it and skips gracefully when there's no authenticated user.

---

## 18. Design Decisions & Trade-offs

| Decision | Why | Trade-off / note |
|---|---|---|
| Layered architecture (controller → service → DAO) | Separation of concerns, testability, swappability | More files than a flat design |
| DAO interface + `EntityManager` impl (no Spring Data) | Full JPQL control; matches the classic DAO pattern taught in class | More boilerplate than `JpaRepository` |
| DB-driven `authorities` + configurable `AppAuthorities` | No hard-coded security names; authorities can change at runtime/config | Slightly more moving parts than `hasRole("ADMIN")` |
| Keep legacy `role` field alongside `authorities` | Backward compatibility with the pharmacy UI | Two sources of truth, kept in sync by `syncAuthoritiesFromRole` + `DataInitializer` |
| Form login + session cookie (not JWT) | Requirement asked for form login/logout; simplest correct auth for an SPA | CSRF disabled (documented trade-off); sessions don't scale horizontally without sticky sessions |
| CSRF disabled | API-first SPA, no CSRF tokens | Should be re-enabled for cookie-based server-rendered apps |
| Bean Validation on entities, `@Valid` on `@RequestBody` | "Validate early at the boundary" — bad data rejected before the service/DB | Entities carry validation concerns (pragmatic for this project) |
| Entity-as-request-body for most CRUD | Fewer files, faster development | Full DTO-per-resource would be stricter (only auth/stock/error use DTOs) |
| `ddl-auto=update` | Zero-setup dev: tables created from entities | Production should use migrations (`validate` + Flyway) |
| `@CrossOrigin(origins = "*")` | Allows the dev SPA to call the API | Must be restricted in production |
| `fetch=EAGER` on `Prescription.items` | Items are always needed in the response, avoids lazy-loading errors | EAGER is generally discouraged; justified here because items are small |
| 20% markup pricing server-side | Guarantees margin; client can't tamper with price | Markup is hard-coded (could be a config property) |
| Audit summaries as `TEXT` (old/new values) | Simple, human-readable, cheap | Not structured — can't query individual diffs |
| 401 vs 403 distinction | Correct API semantics: unauthenticated ≠ unauthorized | Requires both an entry point and an access-denied handler |

---

## 19. How to Present This (Quick Talking Points)

If you are presenting this backend, here's a suggested 5-minute story:

1. **What it is:** an auditable pharmaceutical REST API — not a monolith, but a layered backend serving a React SPA.
2. **Stack in one line:** Spring Boot 3 (Java 17) + Spring Data JPA + Spring Security + Bean Validation + PostgreSQL.
3. **The layers:** Controllers (HTTP only) → Services (`@Transactional`, business rules, audit) → DAOs (JPQL via `EntityManager`) → Entities (JPA mappings).
4. **The entities:** walk the ER diagram — `User`/`Role`/`Authority` (auth), `Category`/`Manufacturer`/`Medicine`/`Supplier` (catalog & procurement), `InventoryBatch`/`StockMovement` (inventory), `Prescription`/`PrescriptionItem`/`DispensingRecord` (transactions), `AuditLog` (audit trail).
5. **Annotations highlight:** pick 3 to explain deeply — `@SpringBootApplication` (auto-config + component scan), `@Transactional` (atomicity of multi-write ops), `@PreAuthorize` + `@appAuthorities` (configurable, DB-driven authorization).
6. **The flagship feature:** create-prescription → auto-dispense across FEFO batches → cashier approve/void, all audited.
7. **Security story:** BCrypt + `WRITE_ONLY` password, DB-stored authorities, 401/403 JSON contract, login/logout audit.
8. **Audit trail:** show `AuditLog` rows — who, what, when, from which IP, old vs new values.

---

*End of document. This guide reflects the code as it exists in the `PharmaTrack` backend module.*
