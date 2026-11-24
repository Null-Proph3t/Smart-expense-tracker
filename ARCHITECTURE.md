# Architecture — Smart Expense Pattern Analyzer

**Status:** Planning only. No application source code in this stage.  
**Source of truth:** `PROJECT_SPECIFICATION.docx` (never silently overridden).  
**Companion docs:** `IMPLEMENTATION_PLAN.md`, `TECHNICAL_DECISIONS.md`, `PROJECT_STATUS.md`.

---

## 1. System context

This is an academic, single-user, locally executed full-stack web application. One Windows 11 developer runs the JVM; the browser talks only to that process. There is no identity system, no remote database, no CDN, and no cloud AI.

```text
┌─────────────┐     HTTP      ┌──────────────────────────────────┐
│  Browser    │◄─────────────►│  Spring Boot (Tomcat 11)         │
│  index.html │  :8080        │  com.expenseanalyzer             │
│  ES6 + CSS  │               │  REST /api/**  + static/**       │
│  Chart.js   │               │  JPA / Hibernate 7               │
└─────────────┘               │         │                        │
                              │         ▼                        │
                              │  H2 file DB                      │
                              │  ${user.home}/.smart-expense-    │
                              │  analyzer/db                     │
                              └──────────────────────────────────┘
```

The UI is a single-page shell served from `src/main/resources/static/`. Client-side view switching replaces a frontend framework. Chart.js is vendored as `chart.umd.min.js` so the app is fully offline after Maven caches are populated.

---

## 2. Technology stack (audited)

Exact pins, compatibility notes, and contradictions are in `TECHNICAL_DECISIONS.md`. Summary of the intended runtime:

| Layer | Specified | Planned pin |
|---|---|---|
| JDK | Java 26 (`26.0.1`), class file 70 | **26.0.1**, `<release>26</release>` — not downgraded |
| Framework | Spring Boot 4.0.x (or latest stable that supports Java 26) | **4.0.10** (latest 4.0.x; official docs list Java 17–27) |
| Build | Maven Wrapper 3.3+, `distributionType=only-script` | Wrapper **3.3.4**, Maven **3.9.16** |
| Compiler plugin | `maven-compiler-plugin` with `<release>26</release>` | Version from Boot BOM (**3.14.1**) |
| Persistence | H2 file mode + Spring Data JPA | H2 **2.4.x** (BOM-managed, currently **2.4.240**), Hibernate **7.2.25.Final** |
| JSON | implied by Spring MVC | Jackson **3.x** via Boot 4 (see starter notes) |
| Validation | `spring-boot-starter-validation` | Jakarta Validation 3.1 / Hibernate Validator 9 |
| UI | Vanilla HTML/CSS/ES6 modules | as specified |
| Charts | local Chart.js UMD | **Chart.js 4.6.1** `chart.umd.min.js` |
| Tests | JUnit 5 | Boot-managed JUnit Jupiter |
| Forbidden | Lombok, Node, Docker, MySQL/Postgres, auth, CDN, LLM APIs | none of these |

### 2.1 Layered backend

```text
controller  →  service  →  repository  →  JPA entity  →  H2
     │              │
     │              ├── CategorizationService (rules only)
     │              ├── AnalyticsService (streams + BigDecimal)
     │              ├── ReportService (monthly / MoM / daily avg)
     │              └── BudgetService (limits + status)
     └── GlobalExceptionHandler (structured JSON errors)
```

Controllers never contain classification, aggregation, or persistence queries beyond delegating to services. Services never return JPA entities to the wire; they map to DTOs.

---

## 3. Package and repository layout

The specification’s tree is preserved. The Maven project lives under `backend/`. Root holds evaluator docs only.

```text
smart-expense-analyzer/
├── PROJECT_SPECIFICATION.docx          # ultimate source of truth
├── ARCHITECTURE.md                     # this file
├── IMPLEMENTATION_PLAN.md
├── TECHNICAL_DECISIONS.md
├── PROJECT_STATUS.md
├── README.md                           # Phase 6
├── .gitignore
└── backend/
    ├── pom.xml
    ├── mvnw
    ├── mvnw.cmd
    ├── .mvn/wrapper/maven-wrapper.properties
    └── src/
        ├── main/java/com/expenseanalyzer/
        │   ├── SmartExpenseAnalyzerApplication.java
        │   ├── config/DataSeeder.java
        │   ├── controller/{Expense,Analytics,Budget}Controller.java
        │   ├── service/{Expense,Categorization,Analytics,Report,Budget}Service.java
        │   ├── repository/{Expense,Budget}Repository.java
        │   ├── model/{Expense,Budget,ExpenseCategory,PaymentMethod,BudgetStatus}.java
        │   ├── dto/                    # spec DTOs + documented extras
        │   └── exception/{ResourceNotFoundException,GlobalExceptionHandler}.java
        ├── main/resources/
        │   ├── application.properties
        │   └── static/{index.html,css/styles.css,js/...}
        └── test/java/com/expenseanalyzer/service/
            ├── CategorizationServiceTest.java
            ├── AnalyticsServiceTest.java
            ├── ReportServiceTest.java
            └── BudgetServiceTest.java
```

**Package organization convention**

| Package | Contents | Rule |
|---|---|---|
| `com.expenseanalyzer` | `@SpringBootApplication` only | No business logic in the bootstrap class |
| `...config` | `DataSeeder` (`ApplicationRunner`) | Seeding only; no REST |
| `...controller` | `@RestController` classes | HTTP mapping, `@Valid`, status codes |
| `...service` | `@Service` classes | Transactions, rules, analytics |
| `...repository` | Spring Data interfaces | Queries only |
| `...model` | Entities and enums | JPA + Bean Validation annotations |
| `...dto` | Request/response records or classes | No JPA annotations |
| `...exception` | Domain exceptions + `@RestControllerAdvice` | No business rules |

No extra top-level packages unless a later implementation instruction explicitly requires them. Supporting types (error payload, money helpers) stay inside the packages above.

---

## 4. Domain model and database relationships

Two tables. No user table (authentication is prohibited). No join table; relationship is logical, not a JPA association.

```text
┌────────────────────────────┐          ┌─────────────────────────────┐
│ expenses                   │          │ budgets                     │
├────────────────────────────┤          ├─────────────────────────────┤
│ id            PK IDENTITY  │          │ id             PK IDENTITY  │
│ description   VARCHAR(200) │          │ category       VARCHAR      │
│ amount        DECIMAL(12,2)│          │ monthly_limit  DECIMAL(12,2)│
│ date          DATE         │          │ budget_month   INT  1..12   │
│ category      VARCHAR enum │◄──logical──┤ budget_year    INT  ≥2000   │
│ payment_method VARCHAR enum│  match on  │                             │
│ notes         VARCHAR(500) │  category  │ UNIQUE (category,           │
└────────────────────────────┘  + month   │        budget_month,        │
                                          │        budget_year)         │
                                          └─────────────────────────────┘
```

There is **no FK** from `expenses` to `budgets`. Budget status is computed at read time: sum `expenses.amount` where `category` matches and `date` falls in `budgetYear-budgetMonth`.

### 4.1 Enums (canonical)

**ExpenseCategory** (order used for UI lists; classification *priority* is a different order — see §6.2):

`FOOD`, `TRANSPORTATION`, `SHOPPING`, `ENTERTAINMENT`, `HEALTHCARE`, `EDUCATION`, `GROCERIES`, `BILLS_AND_UTILITIES`, `OTHERS`

Display labels: Food, Transportation, Shopping, Entertainment, Healthcare, Education, Groceries, Bills & Utilities, Others.

**PaymentMethod:** `CASH`, `UPI`, `CARD`, `OTHER` → Cash, UPI, Card, Other.

**BudgetStatus:** `WITHIN_BUDGET` (< 80.00%), `APPROACHING_LIMIT` (80.00%–100.00% inclusive), `EXCEEDED` (> 100.00%).

### 4.2 Entity validation (must match spec)

| Field | Constraints |
|---|---|
| `Expense.description` | `@NotBlank`, max 200 |
| `Expense.amount` | `@NotNull`, `@DecimalMin("0.01")`, `precision=12, scale=2` |
| `Expense.date` | `@NotNull` `LocalDate` |
| `Expense.category` | `@NotNull` on the *entity*; optional on POST DTO (auto-categorize) |
| `Expense.paymentMethod` | `@NotNull` |
| `Expense.notes` | optional, max 500 |
| `Budget.category` | `@NotNull` |
| `Budget.monthlyLimit` | `@NotNull`, `@DecimalMin("0.01")`, `precision=12, scale=2` |
| `Budget.budgetMonth` | `@Min(1)` `@Max(12)`, column `budget_month` |
| `Budget.budgetYear` | `@Min(2000)`, column `budget_year` |

`MONTH` and `YEAR` are H2 reserved words; the specification already renames those columns. Keep the Java fields `budgetMonth` / `budgetYear`.

### 4.3 Persistence configuration (verbatim from spec)

```properties
spring.datasource.url=jdbc:h2:file:${user.home}/.smart-expense-analyzer/db;DB_CLOSE_ON_EXIT=TRUE
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.settings.web-allow-others=false
```

Additional properties required to *fulfill* the spec (H2 console reachable, dialect, JSON dates) are listed as explicit additions in `TECHNICAL_DECISIONS.md`, not as replacements of the three lines above.

### 4.4 Seeding

`DataSeeder` implements `ApplicationRunner` and runs **only** when `expenseRepository.count() == 0`. Restart must never duplicate rows or overwrite user edits.

Seed payload:

- ≥ 25 realistic INR expenses
- all 9 categories
- all 4 payment methods
- spanning the past 3 calendar months *including the current month*
- 4–5 monthly budgets for the **current** month

---

## 5. REST surface

Base URL: `http://localhost:8080`. Static UI: `/`. API prefix: `/api`.

### 5.1 Expenses — `ExpenseController`

| Method | Path | Status | Service |
|---|---|---|---|
| GET | `/api/expenses` | 200 | `ExpenseService.findAll(filters, sort)` |
| GET | `/api/expenses/{id}` | 200 / 404 | `ExpenseService.findById` |
| POST | `/api/expenses` | **201** | `ExpenseService.create` (+ `CategorizationService` if category null) |
| PUT | `/api/expenses/{id}` | **200** | `ExpenseService.update` (honor posted category) |
| DELETE | `/api/expenses/{id}` | **204** | `ExpenseService.delete` |

Query parameters (all optional, combinable): `search`, `category`, `paymentMethod`, `month` (`YYYY-MM`), `startDate` (`YYYY-MM-DD`), `endDate` (`YYYY-MM-DD`), `sortBy` (`date`\|`amount`), `order` (`asc`\|`desc`). Default sort: `date desc`.

### 5.2 Analytics & reports — `AnalyticsController`

| Method | Path | Service |
|---|---|---|
| GET | `/api/analytics/summary` | `AnalyticsService` overall + current-month KPIs |
| GET | `/api/analytics/categories` | `AnalyticsService` category totals, counts, averages, percentages |
| GET | `/api/analytics/highest-lowest` | `AnalyticsService` high/low/average/top 5 |
| GET | `/api/analytics/monthly?month=YYYY-MM` | `ReportService` monthly report, MoM, historical trend |
| GET | `/api/analytics/insights` | `AnalyticsService` insights + unusual spending |

Default `month` = current calendar month in the JVM default zone.

### 5.3 Budgets — `BudgetController`

| Method | Path | Notes |
|---|---|---|
| GET | `/api/budgets?month=YYYY-MM` | Budget vs actual for that month (default current) |
| POST | `/api/budgets` | Create; unique `(category, month, year)` |
| PUT | `/api/budgets/{id}` | Update limit and/or period |
| DELETE | `/api/budgets/{id}` | Remove budget row only; expenses stay |

HTTP statuses for budget write operations are not enumerated in the spec. Convention: POST **201**, PUT **200**, DELETE **204**, duplicate unique key **409** (see `TECHNICAL_DECISIONS.md`).

---

## 6. Service responsibilities

### 6.1 `ExpenseService`

- CRUD with 404 on missing id.
- Combined filter + sort in Java (`Comparator` + stream/query predicates), as specified.
- POST: if `category` omitted or null → `CategorizationService.categorize(description)`; if provided → honor it.
- PUT: persist the category the client sends so a description edit cannot silently reclassify. If the client omits category on PUT, keep the existing stored category (clarified in technical decisions; spec intent is “do not overwrite manual overrides”).

### 6.2 `CategorizationService`

Classification lives **only** here. Case-insensitive whole-word regex `\bkeyword\b`. Evaluate in this **priority** (spec §4.1 Module B), which is *not* enum declaration order:

1. GROCERIES  
2. FOOD  
3. TRANSPORTATION  
4. HEALTHCARE  
5. EDUCATION  
6. BILLS_AND_UTILITIES  
7. ENTERTAINMENT  
8. SHOPPING  
9. Fallback OTHERS  

Keyword lists are frozen to the specification (including `amazon` under SHOPPING after GROCERIES so `"Amazon Grocery"` hits GROCERIES first).

### 6.3 `AnalyticsService`

All money math uses `BigDecimal` and Java Streams.

- Overall total; per-category total, count, average, percentage share.
- Percentage is `0.00` when overall total is `0` (no division by zero).
- Highest / lowest individual expense, overall average, top 5 (fields: `id`, `description`, `date`, `category`, `paymentMethod`, `amount`).
- Empty dataset: no `NoSuchElementException`, no `ArithmeticException`; nullable extrema, average `0.00`, empty top-5 list.
- Unusual spending: amount **strictly greater than** `2 ×` category average, only if that category has **≥ 2** transactions; return the expense, category average, and multiplier.
- Insights: deterministic sentences from DB data only (MoM category spikes/drops, top-category share, current-month daily average, budget warnings, unusual alerts).

### 6.4 `ReportService`

For `YYYY-MM` (default current month):

- Total, category breakdown, transaction count, average, highest transaction.
- MoM % vs immediately preceding calendar month.
- If previous month total is `0`, `momChangePercent` is JSON `null` (UI shows `"N/A"`). Never `NaN` / `Infinity`.
- Average daily expenditure:
  - current ongoing month → `total / LocalDate.now().getDayOfMonth()`
  - completed past month → `total / YearMonth.lengthOfMonth()`
- Historical monthly trend series for the dashboard/reports bar chart (same endpoint, as specified).

### 6.5 `BudgetService`

- CRUD for monthly category budgets.
- Computed fields: `actualSpent`, `remainingAmount`, `utilizationPercent`, `status` using the three thresholds above.
- Remaining may be negative when exceeded; do not clamp unless a later instruction says so.

---

## 7. Requirement → code map

### 7.1 Backend

| Functional requirement | Primary type(s) |
|---|---|
| App bootstrap | `SmartExpenseAnalyzerApplication` |
| H2 + JPA config | `application.properties` |
| Sample data | `config.DataSeeder` |
| Expense entity + table | `model.Expense` |
| Budget entity + unique constraint | `model.Budget` |
| Category / payment / status vocabularies | `ExpenseCategory`, `PaymentMethod`, `BudgetStatus` |
| Persistence access | `ExpenseRepository`, `BudgetRepository` |
| CRUD, search, combined filter/sort | `ExpenseService`, `ExpenseController` |
| Auto-category on POST | `CategorizationService` via `ExpenseService` |
| Validation 400 JSON | `ExpenseRequestDTO` `@Valid` + `GlobalExceptionHandler` |
| Missing resource 404 | `ResourceNotFoundException` |
| Category analytics | `AnalyticsService`, `CategorySummaryDTO`, `GET /api/analytics/categories` |
| High/low/top 5 | `AnalyticsService`, `GET /api/analytics/highest-lowest` |
| Dashboard KPIs | `AnalyticsService`, `GET /api/analytics/summary` |
| Monthly report, daily avg, MoM, trend | `ReportService`, `MonthlyReportDTO`, `GET /api/analytics/monthly` |
| Unusual spending + insights | `AnalyticsService`, `InsightsResponseDTO`, `GET /api/analytics/insights` |
| Budget tracking | `BudgetService`, `BudgetStatusDTO`, `BudgetController` |
| Money precision | `BigDecimal` on entities, DTOs, services (`scale 2`, `HALF_UP`) |

### 7.2 Specified DTOs

| DTO | Direction | Used by |
|---|---|---|
| `ExpenseRequestDTO` | in | POST/PUT expenses |
| `ExpenseResponseDTO` | out | expense CRUD and lists |
| `CategorySummaryDTO` | out | category analytics + monthly breakdown |
| `MonthlyReportDTO` | out | monthly reports |
| `BudgetStatusDTO` | out | budget vs actual |
| `InsightsResponseDTO` | out | insights + unusual alerts |

Specified list is incomplete for a typed API. Additional DTOs (not replacements) are recorded in `TECHNICAL_DECISIONS.md`: `BudgetRequestDTO`, `AnalyticsSummaryDTO`, `HighestLowestResponseDTO`, `ExpenseHighlightDTO`, `UnusualExpenseDTO`, `MonthlyTrendPointDTO`, `ErrorResponseDTO`.

### 7.3 Frontend modules

| View / capability | File |
|---|---|
| SPA shell, 5-view navigation, sidebar | `static/index.html` |
| Monochrome design system | `static/css/styles.css` |
| `fetch` wrappers, error parsing | `static/js/api.js` |
| Chart.js constructors, monochrome palettes, destroy-on-rerender | `static/js/charts.js` |
| Dashboard KPIs, charts, insights, recent table, Add Expense trigger | `static/js/dashboard.js` |
| Ledger, search/filter/sort, edit/delete | `static/js/transactions.js` |
| Category table, distribution chart, high/low/top 5 | `static/js/reports.js` is monthly; **Analytics view** still uses `reports.js` *or* dashboard/charts — spec names `reports.js` for monthly reports. Analytics view data is loaded via `api.js` from the analytics endpoints and rendered in a section of `index.html`, with chart helpers in `charts.js`. Keep the six JS files named in the spec; do not add a seventh unless later instructed. Analytics-view DOM bindings live in `dashboard.js` **or** `reports.js` as documented in technical decisions (single owner: `reports.js` for Analytics + Monthly Reports). |
| Budget form, progress table, badges | `static/js/budget.js` |
| Chart.js library | `static/js/vendor/chart.umd.min.js` |

**Frontend module ownership (locked):**

| File | Owns |
|---|---|
| `api.js` | All `/api` calls, JSON parse, HTTP error → UI message |
| `charts.js` | Chart instances, palette, resize, teardown |
| `dashboard.js` | Dashboard view only |
| `transactions.js` | Transactions view + add/edit/delete modal shared with dashboard trigger |
| `reports.js` | Analytics view **and** Monthly Reports view |
| `budget.js` | Budget view |

Shared modal markup lives in `index.html`; `transactions.js` owns its behavior so create/edit is not duplicated.

### 7.4 Tests (Phase 6)

| Spec coverage | Class |
|---|---|
| Whole-word, priority, fallback, case | `CategorizationServiceTest` |
| Aggregations, empty safety, unusual detection | `AnalyticsServiceTest` |
| Daily averages, MoM zero-denominator | `ReportServiceTest` |
| Thresholds `<80`, `80`, `100`, `>100` | `BudgetServiceTest` |

---

## 8. Frontend architecture

- One `index.html` with five `data-view` sections. Sidebar buttons toggle `.is-active` and do not reload the page.
- Sidebar collapses on tablet/mobile (CSS only; no JS framework).
- Colour tokens exactly as specified (`#FFFFFF` … `#111111`). Chart series use only `#111111`, `#333333`, `#555555`, `#777777`, `#999999`, `#BBBBBB`, `#D8D8D8`.
- Font: `system-ui, -apple-system, "Segoe UI", sans-serif`.
- Scripts: `<script type="module" src="/js/dashboard.js">` (and siblings) after `api.js`/`charts.js` graph; Chart UMD loaded as a classic script **before** modules so `window.Chart` exists (UMD/ESM mismatch — see risks).
- Currency display: Indian grouping with `₹` (e.g. `Intl.NumberFormat('en-IN')`).
- MoM null → literal `N/A`.
- No CDN, no Google Fonts, no remote Chart.js.

### 8.1 View inventory (must all work)

1. **Dashboard** — 4 KPI cards, category chart, monthly bar chart, insights/unusual list, recent transactions, `+ Add Expense`.
2. **Transactions** — search, category, payment, month, date range, sort date/amount, edit, delete.
3. **Analytics** — category table, distribution chart, highest/lowest/top 5.
4. **Monthly Reports** — `YYYY-MM` selector, KPIs including daily average and MoM, category table, historical bar chart.
5. **Budget** — month selector, set/update form, progress table (Budget, Actual, Remaining, Utilization %, bar, badge).

---

## 9. Cross-cutting architecture

### 9.1 Error handling

All REST errors exit through `GlobalExceptionHandler`.

| Case | HTTP | Body |
|---|---|---|
| Bean Validation / `@Valid` | **400** | structured field errors |
| Malformed JSON / type mismatch | 400 | message + path |
| `ResourceNotFoundException` | 404 | message |
| Unique budget constraint | 409 | message (convention; spec silent) |
| Unhandled | 500 | generic message, log server-side |

Success payloads are **bare DTOs** (no `{ success, data }` envelope) so status codes remain the contract.

### 9.2 Monetary calculations

- Type: `java.math.BigDecimal` only. Never `double`/`float` for money or percentages that represent money.
- Persistence: `precision = 12, scale = 2`.
- Arithmetic: `RoundingMode.HALF_UP`, scale 2 for money; percentages also two decimal places.
- JSON: decimal numbers, not scientific notation (`WRITE_BIGDECIMAL_AS_PLAIN` or Jackson 3 equivalent).
- Percentages: `(part * 100) / total` with scale 2; if total is 0, `0.00`.
- Unusual multiplier: `amount / categoryAverage` at a documented scale (2) for display.

### 9.3 Transactions

- Service write methods: `@Transactional`.
- Read methods: `@Transactional(readOnly = true)` where appropriate.
- Seeder: one transaction for the initial batch.

### 9.4 Time

- Dates stored as `LocalDate` (no time component).
- “Current month” = `YearMonth.now()` in the JVM default time zone (Windows local).
- Daily-average divisor for the current month uses `LocalDate.now().getDayOfMonth()` exactly as specified.

---

## 10. Runtime topology

| Concern | Choice |
|---|---|
| Port | `8080` (Spring default; spec’s verification URL) |
| Process | single JVM via `.\mvnw.cmd spring-boot:run` from `backend/` |
| Servlet | embedded Tomcat 11 (Servlet 6.1), pulled by Web MVC starter |
| Pool | HikariCP (Boot default) |
| Schema | Hibernate `ddl-auto=update` |
| Console | `/h2-console` (local only) |
| Reset demo | delete `%USERPROFILE%\.smart-expense-analyzer\` then restart (README procedure; not an API — auth is forbidden and spec places reset in README) |

---

## 11. Quality attributes (academic scope)

| Attribute | Target |
|---|---|
| Correctness | Spec algorithms, empty-safe analytics, INR scale 2 |
| Operability | One command start; data survives restart |
| Offline | No network after Maven + Chart.js vendor |
| Simplicity | Two entities, five services, five views |
| Demonstrability | Seeded dashboard; 10 demonstration steps in the spec |
| Maintainability | Layered packages, no Lombok, explicit names |

This architecture is the blueprint for the six implementation phases. It does not authorize generating Java or frontend source in this stage.
