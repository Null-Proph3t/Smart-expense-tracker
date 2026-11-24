# Implementation Plan — Smart Expense Pattern Analyzer

**Status:** Planning only. Do not generate application source until the next implementation instruction.  
**Source of truth:** `PROJECT_SPECIFICATION.docx`.  
**Phases:** The six phases in spec §7.3 are preserved in order. Sub-steps refine them; they do not replace them.

---

## Guiding rules

1. Implement incrementally. Do not dump empty stubs for later phases.
2. Do not silently downgrade Java 26 or introduce Lombok, Node, Docker, MySQL, auth, or CDNs.
3. Preserve the specified package tree under `backend/` and the six frontend JS filenames.
4. After each phase, meet that phase’s acceptance criteria before starting the next.
5. Fix compile/runtime errors inside the current phase before moving on.

---

## Phase 1 — Build skeleton and Boot application

**Spec:** Maven Wrapper (`only-script`), `pom.xml` (Java 26 + Spring Boot 4.0.x), `application.properties`, base Spring Boot application.

### Sequence

1. Create `backend/` Maven coordinates (`com.expenseanalyzer:smart-expense-analyzer`).
2. Generate Maven Wrapper **3.3.4** with `distributionType=only-script` and Maven **3.9.16** distribution URL. Commit `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` (no `maven-wrapper.jar`).
3. Write `pom.xml`:
   - parent `spring-boot-starter-parent` **4.0.10**
   - `<java.version>26</java.version>`
   - `maven-compiler-plugin` with `<release>26</release>` (do not use source/target instead of release)
   - starters as resolved in `TECHNICAL_DECISIONS.md` (Web MVC equivalent of `starter-web`, data-jpa, validation, H2, Jackson if not transitive, H2 console module, test starters)
   - **no Lombok**
4. `SmartExpenseAnalyzerApplication.java` in `com.expenseanalyzer`.
5. `application.properties` with the three mandated H2/JPA properties, plus the documented extras required for console, SQL logging (optional), and Jackson decimal/date format.
6. Root `.gitignore` (`target/`, `.idea/`, H2 lock files, OS junk). Do not ignore the wrapper scripts.
7. Prove the JVM: `cd backend` → `.\mvnw.cmd -v` and `.\mvnw.cmd spring-boot:run` until Tomcat listens on 8080.

### Exit criteria (measurable)

| ID | Criterion |
|---|---|
| P1-1 | `backend/.mvn/wrapper/maven-wrapper.properties` contains `distributionType=only-script` and wrapper 3.3.x; **no** `maven-wrapper.jar`. |
| P1-2 | `pom.xml` parent is Spring Boot **4.0.10**; compiler **release 26**. |
| P1-3 | `.\mvnw.cmd -v` runs on Windows without a global Maven install. |
| P1-4 | `.\mvnw.cmd spring-boot:run` starts without error; `http://localhost:8080` returns HTTP (404 on `/` is acceptable until Phase 5). |
| P1-5 | Process uses Java 26 (log or `java -version` of the configured JDK). |
| P1-6 | No Lombok dependency. No Node/Docker files. |

---

## Phase 2 — Domain, persistence, seeding

**Spec:** Entities, enums, repositories, `DataSeeder`, persistence verification.

### Sequence

1. Enums: `ExpenseCategory`, `PaymentMethod`, `BudgetStatus` with display labels.
2. `Expense` and `Budget` entities with spec validations, `@UniqueConstraint` on `(category, budget_month, budget_year)`, IDENTITY ids.
3. `ExpenseRepository`, `BudgetRepository` (`JpaRepository<Long>`).
4. `DataSeeder` (`ApplicationRunner`) guarded by `expenseRepository.count() == 0`.
5. Seed ≥ 25 expenses (all categories, all payment methods, last 3 months including current) and 4–5 current-month budgets.
6. Create `%USERPROFILE%\.smart-expense-analyzer\` via H2 URL on first start.
7. Enable `/h2-console` and confirm tables `expenses` and `budgets`.
8. Restart once: row counts must be unchanged (idempotent seed).

### Exit criteria

| ID | Criterion |
|---|---|
| P2-1 | Tables `expenses` and `budgets` exist with specified columns (`budget_month`, `budget_year`, not `MONTH`/`YEAR`). |
| P2-2 | Unique constraint present on budgets `(category, budget_month, budget_year)`. |
| P2-3 | Fresh DB seeds ≥ 25 expenses and 4–5 budgets; all 9 categories and 4 payment methods appear. |
| P2-4 | Second start does **not** increase `COUNT(*)` on `expenses`. |
| P2-5 | Inserting a second budget for the same category+month+year fails at the database/JPA layer. |
| P2-6 | `amount` / `monthly_limit` persist as scale-2 decimals (e.g. `199.50` not `199.5` binary float). |

---

## Phase 3 — Domain services (no HTTP yet)

**Spec:** `CategorizationService`, `ExpenseService` (CRUD + combined filter/sort), `AnalyticsService`, `ReportService`, `BudgetService`.

Work in this order so later services can call earlier ones:

1. **CategorizationService** — keyword maps, `\b` regex, priority order, `OTHERS` fallback. Unit-testable without Spring context if constructed directly.
2. **ExpenseService** — create/read/update/delete; POST auto-category; PUT preserves client category; filters (`search`, `category`, `paymentMethod`, `month`, `startDate`, `endDate`); sort `date`/`amount` + `asc`/`desc`; default newest first.
3. **AnalyticsService** — totals, category stats, percentages with zero-total guard, high/low/average/top 5, empty-dataset safety, unusual spending (`>` 2× category average, n ≥ 2), insight sentences.
4. **ReportService** — monthly aggregates, daily average rule (current vs past), MoM with `null` when previous total is 0, historical trend series.
5. **BudgetService** — CRUD, uniqueness handling, `actualSpent` / `remainingAmount` / `utilizationPercent` / `status` thresholds.

Use `@Transactional` on write methods. Throw `ResourceNotFoundException` for missing ids (handler comes in Phase 4; exception class may be introduced here).

### Exit criteria

| ID | Criterion |
|---|---|
| P3-1 | `"Business lunch"` → `FOOD` (not TRANSPORTATION via `bus`). |
| P3-2 | `"Amazon Grocery"` → `GROCERIES` (not SHOPPING). |
| P3-3 | Unknown text → `OTHERS`. Matching is case-insensitive. |
| P3-4 | Combined filter+sort returns a stable list for a known seed (e.g. FOOD + date desc). |
| P3-5 | Empty expense table: category percentages `0.00`, no thrown `NoSuchElementException` / `ArithmeticException`. |
| P3-6 | Previous month total `0` → `momChangePercent == null`. |
| P3-7 | Current-month daily average uses `dayOfMonth` as divisor; a past month uses `lengthOfMonth`. |
| P3-8 | Utilization `79.99%` → `WITHIN_BUDGET`; `80.00%` and `100.00%` → `APPROACHING_LIMIT`; `100.01%` → `EXCEEDED`. |
| P3-9 | Unusual flag only when category has ≥ 2 txs and amount `>` `2 * avg` (strict). |

Service methods can be exercised from temporary tests or a local runner; formal JUnit classes are Phase 6 but writing tests early is allowed if they do not block Phase 3 completion.

---

## Phase 4 — REST, DTOs, exception handler

**Spec:** REST controllers, DTOs, `GlobalExceptionHandler`.

### Sequence

1. DTOs listed in the spec, plus the extra DTOs in `TECHNICAL_DECISIONS.md`.
2. MapStruct is **not** in the spec — use explicit private mappers or small `toDto` methods on services. No Lombok builders.
3. `ExpenseController` with exact paths and status codes (201/200/204).
4. `AnalyticsController` with the five GET endpoints; monthly delegates to `ReportService`.
5. `BudgetController` with month query defaulting to current `YYYY-MM`.
6. `GlobalExceptionHandler`: `@Valid` → **400** JSON with field errors; not found → **404**; uniqueness → **409**.
7. CORS not required (same origin static + API).
8. Manual smoke with browser or `curl.exe` against each route.

### Exit criteria

| ID | Criterion |
|---|---|
| P4-1 | `POST /api/expenses` without `category` returns 201 and a categorized entity. |
| P4-2 | `POST /api/expenses` with invalid amount (`0`, negative, missing description) returns **400** with field-level JSON (not an HTML error page). |
| P4-3 | `GET /api/expenses/{unknown}` returns **404**. |
| P4-4 | `PUT` with an explicit category does not get overwritten by description keywords. |
| P4-5 | `DELETE` returns **204** and an empty body; subsequent GET is 404. |
| P4-6 | `GET /api/expenses` honors combined query params. |
| P4-7 | `GET /api/analytics/summary`, `/categories`, `/highest-lowest`, `/monthly`, `/insights` each return 200 JSON on a seeded DB. |
| P4-8 | `GET /api/budgets` returns computed `actualSpent`, `utilizationPercent`, `status`. |
| P4-9 | Duplicate budget POST returns **409** (convention) or a structured 400 — pick 409 and keep it consistent. |
| P4-10 | Money fields in JSON are decimals with two places, not scientific notation. Dates are `YYYY-MM-DD` strings. |

---

## Phase 5 — Offline monochrome UI

**Spec:** Local Chart.js, `styles.css`, `index.html`, modular ES6 across five views.

### Sequence

1. Vendor `chart.umd.min.js` **4.6.1** into `static/js/vendor/` (copy file; no npm).
2. `index.html` shell: sidebar, five views, add/edit modal, delete confirm.
3. `styles.css` — only the specified palette; no shadows, gradients, rainbow, glass, motion beyond trivial show/hide.
4. `api.js` — fetch helpers and error surfacing.
5. `charts.js` — doughnut/bar/line (or bar for trends) using stepped greys; destroy previous charts on redraw.
6. `dashboard.js` — KPIs, two charts, insights, recent table, add-expense button.
7. `transactions.js` — filters, sort, table, edit, delete.
8. `reports.js` — Analytics view + Monthly Reports view.
9. `budget.js` — month selector, form, progress table, badges (`Within Budget`, `Approaching Limit`, `Exceeded`).
10. Load Chart UMD as a non-module script, then ES modules (`type="module"`).
11. Responsive sidebar collapse (CSS breakpoints).
12. Browser pass of the ten demonstration steps (spec “DEMONSTRATION REQUIREMENTS”).

### Exit criteria

| ID | Criterion |
|---|---|
| P5-1 | `http://localhost:8080/` serves the SPA (not a Whitelabel-only page). |
| P5-2 | All five nav targets switch views without full reload. |
| P5-3 | Dashboard shows seeded KPIs and two charts with **only** greyscale fills. |
| P5-4 | Adding an expense via UI appears in Transactions and updates Dashboard after refresh/reload of data. |
| P5-5 | Auto-categorization is visible when category is left blank. |
| P5-6 | Edit and delete work; delete confirmation prevents accidental loss. |
| P5-7 | Combined sort/filter on Transactions matches API results. |
| P5-8 | Monthly Reports month picker changes KPIs, including daily average and MoM (`N/A` when null). |
| P5-9 | Budget badges match the three statuses; progress bar is monochrome. |
| P5-10 | DevTools Network: **zero** requests to CDNs or external fonts. Console: **zero** errors on the happy path. |
| P5-11 | Layout usable at ~1280px desktop and ~375px mobile (sidebar collapsed). |

---

## Phase 6 — Tests, E2E verification, README

**Spec:** JUnit 5 service tests, full verification, `README.md`, reset procedure.

### Sequence

1. `CategorizationServiceTest` — whole-word, priority, fallback, case.
2. `AnalyticsServiceTest` — aggregations, empty dataset, unusual spending.
3. `ReportServiceTest` — daily averages, MoM zero guard.
4. `BudgetServiceTest` — 79.99 / 80 / 100 / >100 boundaries.
5. `.\mvnw.cmd test` must be green.
6. `.\mvnw.cmd spring-boot:run` — walk the 10 demonstration requirements.
7. Persistence check: create/edit a row, stop JVM, start again, row still present.
8. Write evaluator `README.md` covering every heading in spec §7.2, plus **RESET DATABASE / RESET DEMO DATA** (delete `%USERPROFILE%\.smart-expense-analyzer` then restart so `DataSeeder` runs).
9. Confirm no leftover debug endpoints or sample-only hardcoded UI numbers.

### Exit criteria

| ID | Criterion |
|---|---|
| P6-1 | `.\mvnw.cmd test` exit code 0; the four test classes exist under the specified package. |
| P6-2 | Tests include the cases named in spec §7.1 (not a single trivial assert). |
| P6-3 | Single command start: `cd backend` then `.\mvnw.cmd spring-boot:run`. |
| P6-4 | All 10 demonstration requirements pass. |
| P6-5 | Data survives restart. |
| P6-6 | README contains reset procedure that restores seed data. |
| P6-7 | README includes API reference matching the live controllers. |

---

## Cross-phase demonstration checklist (execute in Phase 6, design toward from Phase 1)

| # | Requirement |
|---|---|
| 1 | Application launches with a single command |
| 2 | Dashboard loads meaningful sample data |
| 3 | All navigation elements work |
| 4 | Adding a new expense |
| 5 | Automatic categorization |
| 6 | Editing and deleting an expense |
| 7 | Sorting and filtering |
| 8 | Monthly analytics |
| 9 | Persistence after restart |
| 10 | No browser console errors |

---

## Dependency between phases

```text
Phase 1  →  Phase 2  →  Phase 3  →  Phase 4  →  Phase 5  →  Phase 6
 bootstrap   schema      services    HTTP+DTO    UI         tests+docs
```

Do not start Phase 4 until Phase 3 algorithms are correct: the UI cannot compensate for wrong MoM or category priority. Chart.js vendor file may be downloaded during Phase 5 only (not earlier, to keep Phase 1–4 backend-focused).

---

## Suggested intra-phase verification commands (Windows)

```powershell
cd backend
.\mvnw.cmd -v
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
# browser: http://localhost:8080
# H2:      http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:file:${user.home}/.smart-expense-analyzer/db
```

H2 console JDBC URL must use the expanded Windows home path, for example `jdbc:h2:file:C:/Users/Aditya/.smart-expense-analyzer/db`.

---

## Out of scope until a later instruction

- Generating Java/HTML/CSS/JS (this document only)
- Extra entities, auth, pagination, CSV import, multi-user
- Changing Java below 26
- Replacing H2 with any server database
