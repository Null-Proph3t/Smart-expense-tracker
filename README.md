# Smart Expense Pattern Analyzer

**A zero-cloud, high-precision personal financial analytics platform built with Java 26, Spring Boot 4.0, an embedded file-based H2 database, and a lightweight modular ES6 frontend.**

Smart Expense Pattern Analyzer turns raw daily transactions into structured budgets, spending anomaly alerts, and Month-over-Month (MoM) financial trends—running 100% locally with zero external cloud dependencies, zero Node.js/npm build overhead, and exact decimal arithmetic.

---

## Table of Contents

1. [Why This Project Exists (Problem & Solution)](#why-this-project-exists-problem--solution)
2. [Core Capabilities](#core-capabilities)
3. [Quick Start (Windows)](#quick-start-windows)
4. [System Architecture & Data Flow](#system-architecture--data-flow)
5. [Technology Stack](#technology-stack)
6. [Project Structure](#project-structure)
7. [Database Schema & Persistence](#database-schema--persistence)
8. [Rule-Based Categorization Engine](#rule-based-categorization-engine)
9. [Analytics & Financial Math Engine](#analytics--financial-math-engine)
10. [Monochromatic Design System](#monochromatic-design-system)
11. [REST API Reference](#rest-api-reference)
12. [Testing & Verification](#testing--verification)
13. [Resetting the Database & Demo Data](#resetting-the-database--demo-data)
14. [Future Enhancements](#future-enhancements)
15. [License](#license)

---

## Why This Project Exists (Problem & Solution)

Most lightweight expense trackers break down in real-world usage due to five common engineering shortcuts. This project is designed specifically to eliminate each one:

| Common Failure Point | Impact on Users | How Smart Expense Pattern Analyzer Solves It |
| :--- | :--- | :--- |
| **Manual Categorization Fatigue** | Logging every coffee, UPI transfer, or grocery bill manually leads to abandonment. | **Automated Regex Classification**: Evaluates transaction descriptions against priority-ordered rules on creation. |
| **Naive Substring Matching** | Simple `.contains()` checks misclassify `"Business lunch"` as *Transportation* (`"bus"`) or `"Parental care"` as *Bills* (`"rent"`). | **Whole-Word Boundary Matching (`\b`)**: Enforces strict word boundaries and deterministic category precedence. |
| **Floating-Point Rounding Drift** | Using `double` or `float` (IEEE 754) introduces binary representation errors in sums and percentages. | **Strict `BigDecimal` Arithmetic**: Uses `NUMERIC(12, 2)` and `RoundingMode.HALF_UP` end-to-end across entities, DTOs, and services. |
| **Static, Context-Free Charts** | Basic pie charts show *where* money went without flagging spikes or pacing. | **Pattern Recognition Engine**: Flags anomalies (`> 2x` category average), computes daily burn rates, and tracks MoM shifts. |
| **Bloated Frontend Toolchains** | Heavy SPA frameworks pull in gigabytes of `node_modules` and external CDNs. | **Zero-Build Vanilla ES6 + Vendored Chart.js**: Native browser modules (`type="module"`) with offline-vendored Chart.js `4.4.1`. |

---

## Core Capabilities

- **100% Local & Offline-First**: Persists data to a local H2 file database (`~/.smart-expense-analyzer/db.mv.db`). No external APIs, telemetry, or CDN requests.
- **Deterministic Auto-Categorization**: Classifies transactions into 9 canonical categories using whole-word regular expressions, while preserving manual category overrides during creation or edits.
- **Monthly Budget Guardrails**: Enforces one budget limit per category per month/year and evaluates live utilization across three states: `WITHIN_BUDGET`, `APPROACHING_LIMIT` (`80%–100%`), and `EXCEEDED` (`> 100%`).
- **Statistical & Behavioral Insights**:
  - Identifies single-transaction anomalies exceeding twice the category average (once a category has at least 2 transactions).
  - Computes Month-over-Month (MoM) spending changes with safe zero-denominator handling.
  - Calculates adaptive daily burn rates (elapsed days for the active month vs. total calendar days for completed months).
- **5-View Single-Page Application (SPA)**: Seamless client-side navigation across **Dashboard**, **Transactions**, **Analytics**, **Monthly Reports**, and **Budget** views.

---

## Quick Start (Windows)

### Prerequisites
- **OS**: Windows 10 or Windows 11 (64-bit)
- **JDK**: OpenJDK **26** or higher available on `PATH`
- **Git**: For cloning the repository *(Apache Maven is bundled via `mvnw.cmd`—no separate Maven install required)*

### 1. Verify Java & Clone
```powershell
java -version
# Expected: openjdk version "26" (or higher)

git clone [https://github.com/](https://github.com/)<your-username>/Smart-Expense-Analyzer.git
cd Smart-Expense-Analyzer\backend
```

### 2. Run the Application
```powershell
.\mvnw.cmd spring-boot:run
```
*(Alternatively, build an executable JAR with `.\mvnw.cmd package -DskipTests` and run `java -jar target/smart-expense-analyzer-1.0.0-SNAPSHOT.jar`.)*

### 3. Open in Browser
Once the console prints `Tomcat started on port 8080 (http)`:
- **Application UI**: [http://localhost:8080/](http://localhost:8080/)
- **H2 Database Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - **JDBC URL**: `jdbc:h2:file:C:\Users\<YourUsername>/.smart-expense-analyzer/db`
  - **User Name**: `sa`
  - **Password**: *(leave blank)*

On first startup against an empty database, `DataSeeder` automatically populates **32 sample INR transactions** (spanning 4 months) and **5 active monthly budgets** so all charts and reports work immediately.

---

## System Architecture & Data Flow

The application follows a clean three-tier Spring Boot architecture paired with a modular vanilla ES6 frontend served directly from Spring Boot's static resources.

```
+-----------------------------------------------------------------------------------+
|                                  CLIENT BROWSER                                   |
|  Single-Page Application (HTML5 / CSS3 / Modular ES6 JavaScript - No Build Step)  |
|                                                                                   |
|  [Dashboard]    [Transactions]    [Analytics]    [Monthly Reports]    [Budget]    |
|       |               |                |                 |               |        |
|       +---------------+----------------+-----------------+---------------+        |
|                                        |                                          |
|                      [api.js — Centralized Fetch & INR Formatter]                 |
|                      [charts.js — Monochrome Chart.js Renderer]                   |
+-----------------------------------------------------------------------------------+
                                         | HTTP REST / JSON
                                         v
+-----------------------------------------------------------------------------------+
|                            SPRING BOOT 4.0.10 BACKEND                             |
|                                                                                   |
|  [REST Controllers]                                                               |
|    ├── ExpenseController      (/api/expenses/**)                                  |
|    ├── AnalyticsController    (/api/analytics/**)                                 |
|    ├── BudgetController       (/api/budgets/**)                                   |
|    └── GlobalExceptionHandler (@RestControllerAdvice -> Structured JSON Errors)   |
|                                                                                   |
|  [Domain Services]                                                                |
|    ├── CategorizationService  (Boundary-aware \b regex & precedence engine)       |
|    ├── ExpenseService         (CRUD, multi-predicate filtering, sorting)          |
|    ├── AnalyticsService       (KPIs, category shares, extrema, anomalies)         |
|    ├── ReportService          (MoM shifts, daily burn rate, 6-month trend)        |
|    └── BudgetService          (Utilization %, remaining balance, status rules)    |
|                                                                                   |
|  [Persistence Layer]                                                              |
|    ├── ExpenseRepository      (Spring Data JPA derived & custom JPQL queries)     |
|    └── BudgetRepository       (Unique month/year/category lookups)                |
+-----------------------------------------------------------------------------------+
                                         | JDBC / Hibernate 7.2
                                         v
+-----------------------------------------------------------------------------------+
|                     EMBEDDED H2 FILE DATABASE (v2.4.240)                          |
|                   ${user.home}/.smart-expense-analyzer/db.mv.db                   |
+-----------------------------------------------------------------------------------+
```

### How a Transaction Flows Through the System
1. **Input**: A user submits `"Swiggy dinner order"` for `₹450.00` via `UPI` in the Transactions modal (`transactions.js`).
2. **Validation**: `ExpenseController` validates the payload via Jakarta Bean Validation (`@NotBlank`, `@DecimalMin("0.01")`, `@NotNull`).
3. **Classification**: Because `category` is omitted/`null`, `ExpenseService` delegates to `CategorizationService`, which matches `\bswiggy\b` and `\bdinner\b` and assigns `FOOD`.
4. **Persistence**: `ExpenseRepository` saves the normalized `BigDecimal` (`450.00`) to `db.mv.db`.
5. **Recomputation**: Subsequent dashboard or budget queries dynamically recalculate category shares, daily burn rates, anomaly thresholds, and `FOOD` budget utilization.

---

## Technology Stack

| Layer | Technology | Version / Spec | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Java SE | OpenJDK **26** (`<release>26</release>`) | Modern Java runtime with records and improved GC performance. |
| **Framework** | Spring Boot | **4.0.10** | Spring Framework 7, Jakarta EE 11 baseline, embedded Tomcat 11. |
| **ORM / JPA** | Spring Data JPA / Hibernate | **7.2.25.Final** (Jakarta Persistence 3.2) | Repository abstractions and transactional persistence. |
| **Database** | H2 Database Engine | **2.4.240** (Embedded File Mode) | Zero-setup, ACID-compliant local file persistence. |
| **Validation** | Hibernate Validator | **9.0.3.Final** | Declarative request DTO validation (`@Valid`). |
| **Build Tool** | Apache Maven Wrapper | **3.3.4** (`only-script`, Maven `3.9.16`) | Reproducible builds and test execution without global Maven setup. |
| **Testing** | JUnit 5, Mockito, AssertJ | JUnit Jupiter **5.11+** / Boot 4 Test | Unit, parameterized, MockMvc, and `@SpringBootTest` integration tests. |
| **Frontend** | HTML5, CSS3, ES6 Modules | Native Browser Standards | Zero-bundler SPA using Flexbox, Grid, and `type="module"`. |
| **Charts** | Chart.js (Vendored UMD) | **4.4.1** (`chart.umd.min.js`) | Locally bundled canvas charting with stepped grayscale palettes. |

---

## Project Structure

```text
Smart-Expense-Analyzer/
├── backend/
│   ├── pom.xml                                        # Java 26 & Spring Boot 4.0.10 configuration
│   ├── mvnw.cmd / mvnw                                # Maven Wrapper execution scripts
│   ├── .mvn/wrapper/maven-wrapper.properties          # Wrapper distribution settings
│   └── src/
│       ├── main/
│       │   ├── java/com/expenseanalyzer/
│       │   │   ├── SmartExpenseAnalyzerApplication.java   # Application entry point
│       │   │   ├── config/
│       │   │   │   └── DataSeeder.java                    # Idempotent initial demo data loader
│       │   │   ├── controller/
│       │   │   │   ├── AnalyticsController.java           # /api/analytics endpoints
│       │   │   │   ├── BudgetController.java              # /api/budgets endpoints
│       │   │   │   └── ExpenseController.java             # /api/expenses endpoints
│       │   │   ├── dto/                                   # Immutable request/response records & DTOs
│       │   │   ├── exception/
│       │   │   │   ├── DuplicateResourceException.java    # Maps to HTTP 409 Conflict
│       │   │   │   ├── GlobalExceptionHandler.java        # Centralized @RestControllerAdvice
│       │   │   │   └── ResourceNotFoundException.java     # Maps to HTTP 404 Not Found
│       │   │   ├── model/
│       │   │   │   ├── Budget.java                        # JPA entity for monthly category limits
│       │   │   │   ├── BudgetStatus.java                  # WITHIN_BUDGET, APPROACHING_LIMIT, EXCEEDED
│       │   │   │   ├── Expense.java                       # JPA entity for expense transactions
│       │   │   │   ├── ExpenseCategory.java               # 9 canonical expense categories
│       │   │   │   └── PaymentMethod.java                 # CASH, UPI, CARD, OTHER
│       │   │   ├── repository/
│       │   │   │   ├── BudgetRepository.java
│       │   │   │   └── ExpenseRepository.java
│       │   │   └── service/
│       │   │       ├── AnalyticsService.java              # KPIs, shares, extrema, anomalies, insights
│       │   │       ├── BudgetService.java                 # Threshold evaluation & CRUD
│       │   │       ├── CategorizationService.java         # Whole-word regex classification engine
│       │   │       ├── ExpenseService.java                # Transaction CRUD, filtering & sorting
│       │   │       └── ReportService.java                 # MoM trends & daily burn calculations
│       │   └── resources/
│       │       ├── application.properties                 # H2 file config & server properties
│       │       └── static/
│       │           ├── index.html                         # 5-view SPA shell
│       │           ├── css/styles.css                     # High-contrast monochromatic stylesheet
│       │           └── js/
│       │               ├── api.js                         # REST fetch wrapper & INR currency helpers
│       │               ├── charts.js                      # Monochrome Chart.js configuration
│       │               ├── dashboard.js                   # KPI cards & recent activity view
│       │               ├── transactions.js                # Filterable ledger & add/edit modal
│       │               ├── reports.js                     # Category analytics & monthly reports
│       │               ├── budget.js                      # Budget creation & utilization bars
│       │               └── vendor/chart.umd.min.js        # Offline Chart.js 4.4.1 bundle
│       └── test/
│           ├── java/com/expenseanalyzer/
│           │   ├── SmartExpenseAnalyzerIntegrationTest.java # End-to-end @SpringBootTest suite
│           │   ├── controller/                            # Standalone MockMvc REST tests
│           │   └── service/                               # Unit & edge-case service tests
│           └── resources/
│               └── application.properties                 # Isolated in-memory H2 test config
├── ARCHITECTURE.md                                    # Master architectural blueprint
├── IMPLEMENTATION_PLAN.md                             # Phased build roadmap
├── PROJECT_STATUS.md                                  # Milestone completion log
├── TECHNICAL_DECISIONS.md                             # Engineering trade-offs & rationale
└── README.md                                          # Project documentation
```

---

## Database Schema & Persistence

### Storage Configuration
- **Database File**: `${user.home}/.smart-expense-analyzer/db.mv.db`
- **JDBC URL**: `jdbc:h2:file:${user.home}/.smart-expense-analyzer/db;DB_CLOSE_ON_EXIT=TRUE`
- **Hibernate DDL Mode**: `update` (retains all transactions and budgets across restarts)

### `expenses` Table
| Column | SQL Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Surrogate primary key. |
| `description` | `VARCHAR(200)` | `NOT NULL` | Merchant or item note used for auto-categorization. |
| `amount` | `NUMERIC(12, 2)` | `NOT NULL, >= 0.01` | Exact monetary value in INR (`BigDecimal`, scale 2). |
| `date` | `DATE` | `NOT NULL` | Transaction date (`java.time.LocalDate`). |
| `category` | `VARCHAR` | `NOT NULL` | Canonical `ExpenseCategory` enum string. |
| `payment_method` | `VARCHAR` | `NOT NULL` | `CASH`, `UPI`, `CARD`, or `OTHER`. |
| `notes` | `VARCHAR(500)` | `NULLABLE` | Optional user remarks. |

### `budgets` Table
| Column | SQL Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Surrogate primary key. |
| `category` | `VARCHAR` | `NOT NULL` | Canonical `ExpenseCategory` enum string. |
| `monthly_limit` | `NUMERIC(12, 2)` | `NOT NULL, >= 0.01` | Target spending cap in INR (`BigDecimal`, scale 2). |
| `budget_month` | `INT` | `NOT NULL, 1–12` | Calendar month (`1` = January, `12` = December). |
| `budget_year` | `INT` | `NOT NULL, >= 2000` | Four-digit calendar year (e.g., `2026`). |

**Composite Unique Constraint**:
```sql
CONSTRAINT uq_budget_category_month_year UNIQUE (category, budget_month, budget_year)
```
Guarantees that only one budget limit can exist for a given category within the same month and year. Attempting to create a duplicate returns `HTTP 409 Conflict`.

---

## Rule-Based Categorization Engine

`CategorizationService` evaluates descriptions top-to-bottom using precompiled, case-insensitive regular expressions wrapped in word boundaries (`\b<keyword>\b`). The first matching rule wins.

| Priority | Category Enum | UI Display Name | Matched Whole-Word Keywords (`\b...\b`) |
| :---: | :--- | :--- | :--- |
| **1** | `GROCERIES` | Groceries | `vegetables`, `grocery`, `groceries`, `milk`, `fruits`, `supermarket`, `bigbasket`, `blinkit`, `zepto` |
| **2** | `FOOD` | Food | `restaurant`, `pizza`, `coffee`, `cafe`, `lunch`, `dinner`, `breakfast`, `burger`, `zomato`, `swiggy` |
| **3** | `TRANSPORTATION` | Transportation | `uber`, `ola`, `bus`, `train`, `metro`, `fuel`, `petrol`, `diesel`, `cab`, `auto` |
| **4** | `HEALTHCARE` | Healthcare | `medicine`, `hospital`, `pharmacy`, `doctor`, `clinic`, `medical`, `lab` |
| **5** | `EDUCATION` | Education | `books`, `college`, `course`, `tuition`, `udemy`, `coursera`, `exam`, `stationery` |
| **6** | `BILLS_AND_UTILITIES` | Bills & Utilities | `electricity`, `internet`, `wifi`, `rent`, `water`, `broadband`, `recharge`, `bill` |
| **7** | `ENTERTAINMENT` | Entertainment | `netflix`, `cinema`, `movies`, `movie`, `spotify`, `concert`, `gaming` |
| **8** | `SHOPPING` | Shopping | `amazon`, `flipkart`, `clothing`, `clothes`, `shoes`, `electronics`, `mall` |
| **9** | `OTHERS` | Others | *Default fallback when no keywords match, or input is null/blank.* |

### Why Precedence & Word Boundaries Matter
- **`"Business lunch with partner"` $\rightarrow$ `FOOD`**: Because `\bbus\b` enforces word boundaries, `"Business"` does not falsely trigger `TRANSPORTATION`; `\blunch\b` matches `FOOD`.
- **`"Amazon Grocery delivery"` $\rightarrow$ `GROCERIES`**: Both `"amazon"` and `"grocery"` are present, but `GROCERIES` (Priority 1) is evaluated before `SHOPPING` (Priority 8).
- **`"Automobile service"` $\rightarrow$ `OTHERS`**: `\bauto\b` does not match `"Automobile"`, preventing false positives.

---

## Analytics & Financial Math Engine

Every calculation in `AnalyticsService`, `ReportService`, and `BudgetService` uses `java.math.BigDecimal` with `RoundingMode.HALF_UP` to preserve exact 2-decimal precision.

### 1. Unusual Spending Anomaly Detection
An expense is flagged as an anomaly when:

$$\text{amount} > 2.0 \times \overline{X}_{\text{category}}$$

- **Minimum Sample Guard ($N \ge 2$)**: A category must contain at least 2 recorded expenses before any transaction can be flagged. A single isolated transaction is never falsely marked as an anomaly against itself.

### 2. Month-over-Month (MoM) Percentage Shift

$$\text{MoM Change \%} = \frac{\text{Total}_{\text{current}} - \text{Total}_{\text{previous}}}{\text{Total}_{\text{previous}}} \times 100$$

- **Zero-Denominator Guard**: When $\text{Total}_{\text{previous}} = 0$, the API returns `null` (displayed as `N/A` in the UI) rather than throwing an `ArithmeticException` or displaying a misleading infinite percentage.

### 3. Adaptive Daily Burn Rate

$$\text{Daily Burn} = \frac{\text{Total Month Expenditure}}{\text{Days Elapsed}}$$

- **Active Current Month**: Divides by `LocalDate.now().getDayOfMonth()` so the burn rate reflects actual daily pacing so far this month.
- **Completed Past Months**: Divides by `YearMonth.lengthOfMonth()` (accounting accurately for 28-, 29-, 30-, and 31-day months).

### 4. Budget Utilization & Status Thresholds

$$\text{Utilization \%} = \frac{\text{Actual Spent}}{\text{Monthly Limit}} \times 100 \qquad \text{Remaining} = \text{Monthly Limit} - \text{Actual Spent}$$

| Utilization Range | Status Enum | Meaning |
| :--- | :--- | :--- |
| `< 80.00%` | `WITHIN_BUDGET` | Spending is comfortably within the allocated cap. |
| `80.00% – 100.00%` | `APPROACHING_LIMIT` | Warning zone; budget is nearly or fully utilized. |
| `> 100.00%` | `EXCEEDED` | Over budget; `Remaining` becomes a negative deficit value. |

---

## Monochromatic Design System

The frontend avoids visual clutter in favor of a high-contrast, Swiss-inspired financial ledger aesthetic:

| Token Role | Hex Value | Usage |
| :--- | :--- | :--- |
| **Canvas** | `#FFFFFF` | Primary page background. |
| **Elevated Surface** | `#F8F8F8` | Metric tiles, table wrappers, and modal dialogs. |
| **Hairline Border** | `#EAEAEA` | 1px structural dividers, form inputs, and card outlines. |
| **Muted Text** | `#888888` | Table column headers, timestamps, and secondary captions. |
| **Body Text** | `#333333` | Ledger rows, filter labels, and descriptive copy. |
| **Primary Ink** | `#111111` | Headings, KPI figures, and primary action buttons. |
| **Chart Grayscale Ramp** | `#111111` $\rightarrow$ `#D8D8D8` | 7-step monochrome palette (`#111111`, `#333333`, `#555555`, `#777777`, `#999999`, `#BBBBBB`, `#D8D8D8`) for doughnut and bar charts. |

---

## REST API Reference

All endpoints accept and return `application/json;charset=UTF-8`.

### Expenses (`/api/expenses`)
| Method | Endpoint | Query Parameters | Description | Responses |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/expenses` | `search`, `category`, `paymentMethod`, `month`, `startDate`, `endDate`, `sortBy` (`date`\|`amount`), `order` (`asc`\|`desc`) | List transactions with combined filtering and sorting. | `200`, `400` |
| `GET` | `/api/expenses/{id}` | — | Fetch a single transaction by ID. | `200`, `404` |
| `POST` | `/api/expenses` | — | Create a transaction (auto-categorizes if `category` is `null`). | `201`, `400` |
| `PUT` | `/api/expenses/{id}` | — | Update a transaction (retains existing category if omitted). | `200`, `400`, `404` |
| `DELETE` | `/api/expenses/{id}` | — | Delete a transaction by ID. | `204`, `404` |

**Example `POST /api/expenses` Request**:
```json
{
  "description": "Starbucks coffee iced latte",
  "amount": 320.00,
  "date": "2026-11-24",
  "category": null,
  "paymentMethod": "UPI",
  "notes": "Afternoon coffee"
}
```

### Analytics & Reports (`/api/analytics`)
| Method | Endpoint | Query Parameters | Description | Responses |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/analytics/summary` | — | Overall total, current-month total, highest expense, and average spend. | `200` |
| `GET` | `/api/analytics/categories` | — | Breakdown across all 9 categories (totals, counts, averages, `%` share). | `200` |
| `GET` | `/api/analytics/highest-lowest` | — | Max expense, min expense, overall mean, and Top 5 expenses. | `200` |
| `GET` | `/api/analytics/unusual` | — | Transactions exceeding `2x` their category average ($N \ge 2$). | `200` |
| `GET` | `/api/analytics/insights` | — | Dynamic natural-language spending insights and budget alerts. | `200` |
| `GET` | `/api/analytics/monthly` | `month` (e.g., `2026-11`) | Monthly totals, MoM `%` change, daily burn rate, and 6-month trend *(alias: `/monthly-report`)*. | `200`, `400` |

### Budgets (`/api/budgets`)
| Method | Endpoint | Query Parameters | Description | Responses |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/budgets` | `month` (e.g., `2026-11`) | List category budgets for the given month with live utilization metrics. | `200` |
| `GET` | `/api/budgets/{id}` | — | Fetch a single budget record by ID. | `200`, `404` |
| `POST` | `/api/budgets` | — | Create a monthly category budget (enforces uniqueness). | `201`, `400`, `409` |
| `PUT` | `/api/budgets/{id}` | — | Update an existing budget limit or period. | `200`, `400`, `404`, `409` |
| `DELETE` | `/api/budgets/{id}` | — | Remove a budget cap (underlying expenses are preserved). | `204`, `404` |

**Example Validation Error Response (`HTTP 400 Bad Request`)**:
```json
{
  "timestamp": "2026-11-24T20:55:22.9057793",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/expenses",
  "fieldErrors": [
    {
      "field": "amount",
      "message": "Amount must be at least 0.01",
      "rejectedValue": -50.0
    },
    {
      "field": "description",
      "message": "Description must not be blank",
      "rejectedValue": ""
    }
  ]
}
```

---

## Testing & Verification

The project includes **76 automated tests** covering business logic, edge-case boundaries, REST serialization, and full Spring Boot integration using an isolated in-memory H2 profile.

### Run the Test Suite
```powershell
cd backend
.\mvnw.cmd test
```

### Test Coverage Breakdown
- **`CategorizationServiceTest` (26 tests)**: Validates `\b` word boundaries, top-down category precedence, case insensitivity, and fallback handling for blank/unknown inputs.
- **`AnalyticsServiceTest` (6 tests)**: Verifies empty-dataset safety, `BigDecimal` repeating-decimal rounding (e.g., $50 / 3 \rightarrow 16.67$), anomaly thresholds ($N \ge 2$), Top 5 ordering, and insight generation.
- **`ReportServiceTest` (7 tests)**: Tests MoM zero-denominator `null` safety, positive/negative shifts, and daily burn calculations across 28-, 29-, 30-, and 31-day months.
- **`BudgetServiceTest` (7 tests)**: Verifies exact threshold transitions (`79.99%` $\rightarrow$ `80.00%` $\rightarrow$ `100.00%` $\rightarrow$ `100.01%`), negative remaining balances, and `409`/`404` exceptions.
- **`ExpenseServiceTest` (6 tests)**: Tests auto-categorization, manual override preservation, multi-predicate filtering, sorting validation, and scale normalization.
- **Controller MockMvc Tests (19 tests)**: `ExpenseControllerTest`, `AnalyticsControllerTest`, and `BudgetControllerTest` verify HTTP status codes, JSON contracts, and `@RestControllerAdvice` error payloads.
- **`SmartExpenseAnalyzerIntegrationTest` (5 tests)**: End-to-end `@SpringBootTest` verification of `DataSeeder` idempotency, error status codes (`400`, `404`, `405`, `409`), and full transaction CRUD persistence.

**Latest Result**: `76 tests run, 0 failures, 0 errors, 0 skipped.`

---

## Resetting the Database & Demo Data

To wipe custom entries and restore the clean initial dataset (32 INR transactions across 4 months + 5 active budgets):

1. **Stop the running backend server** (`Ctrl + C` in the active terminal, or via PowerShell):
   ```powershell
   Stop-Process -Name java -Force -ErrorAction SilentlyContinue
   ```
2. **Delete the local H2 database files**:
   ```powershell
   Remove-Item -Path "$env:USERPROFILE\.smart-expense-analyzer\db.*" -Force
   ```
3. **Restart the application**:
   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```
   `DataSeeder` will detect an empty `expenses` table and automatically re-seed the initial demo records.

---

## Future Enhancements

- **Statement Exports**: Generate downloadable monthly PDF expenditure statements and CSV audit logs.
- **Multi-Currency Conversion**: Support real-time conversion across INR, USD, EUR, and GBP.
- **Client-Side Receipt OCR**: Extract merchant descriptions and totals from receipt photos using WebAssembly Tesseract.
- **Recurring Subscription Detection**: Identify monthly billing cycles (e.g., streaming, broadband) and surface upcoming renewal alerts.
- **Native Desktop Packaging**: Bundle a standalone Windows `.msi` installer using `jpackage`.

---

## License

Released under the **MIT License**.
