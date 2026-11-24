# Smart Expense Pattern Analyzer

A robust, enterprise-grade personal financial analytics platform engineered to automatically parse, categorize, analyze, and visualize daily expenditures with mathematical precision and actionable intelligence.

Built strictly according to master software engineering specifications: pure Java 26, Spring Boot 4.0.x, persistent file-based H2 database, modular ES6 JavaScript, and an offline-vendored Chart.js dashboard adhering to an ultra-clean monochromatic design system.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Problem Statement](#problem-statement)
3. [Objectives](#objectives)
4. [Technology Stack](#technology-stack)
5. [Architecture & Design System](#architecture--design-system)
6. [Package Structure](#package-structure)
7. [Database Schema & Persistence](#database-schema--persistence)
8. [Categorization Logic](#categorization-logic)
9. [Analytics & Pattern Recognition Engine](#analytics--pattern-recognition-engine)
10. [REST API Reference](#rest-api-reference)
11. [Windows Setup Instructions](#windows-setup-instructions)
12. [Execution Instructions](#execution-instructions)
13. [Testing Instructions](#testing-instructions)
14. [Reset Database & Demo Data Procedure](#reset-database--demo-data-procedure)
15. [Verification Results](#verification-results)
16. [Future Enhancements](#future-enhancements)

---

## Project Overview

The **Smart Expense Pattern Analyzer** is an end-to-end financial analytics application designed for individuals seeking complete clarity over their spending patterns. It transforms disorganized transaction records into structured, actionable financial insights without relying on third-party cloud aggregators or external telemetry.

Key highlights:
- **Zero Cloud Leaks / 100% Offline**: Operates fully offline on local file-based storage. Zero external CDN dependencies, zero npm or Node.js runtime requirements.
- **Rule-Based Natural Language Categorization**: Evaluates transaction descriptions against boundary-aware regex rules in strict precedence order to assign canonical categories automatically.
- **Strict Monetary Precision**: Uses `java.math.BigDecimal` (scale 2, `HALF_UP` rounding) for all monetary arithmetic. Prohibits floating-point representation (`double`/`float`) across entities, services, DTOs, and calculations.
- **Intelligent Financial Pattern Recognition**: Detects category-wise unusual spending anomalies (`> 2 × category average`), tracks Month-over-Month (MoM) budget changes with safe division guards, computes daily expenditure burn rates, and generates dynamic natural-language recommendations.
- **Monochromatic Financial Dashboard**: Implements a high-density, professional aesthetic (`#FFFFFF` canvas, `#F8F8F8` surfaces, `#EAEAEA` borders, `#111111` primary typography) with stepped grayscale Chart.js visualizations.

---

## Problem Statement

Modern individuals execute tens to hundreds of digital transactions every month across UPI, debit/credit cards, net banking, and cash. However:
1. **Manual Logging Friction**: Categorizing every coffee, commute, or grocery bill manually causes tracking fatigue and eventual abandonment.
2. **False Substring Matching**: Naive keyword search engines categorize a `"Business lunch"` as *Transportation* (due to `"bus"`) or `"Parental care"` as *Bills* (due to `"rent"`).
3. **Floating-Point Imprecision**: Budgeting apps using IEEE 754 `float`/`double` accumulate rounding errors, leading to balancing discrepancies and corrupted balances.
4. **Superficial Analytics**: Most tools display static pie charts without context, failing to flag anomalous spikes or compute realistic daily burn rates.
5. **Bloated Frontend Frameworks**: Simple dashboards often pull in gigabytes of `node_modules` and require complex build steps, reducing auditability and long-term maintainability.

The Smart Expense Pattern Analyzer solves these challenges by combining a high-performance Spring Boot backend with a lightweight, zero-dependency vanilla ES6 frontend.

---

## Objectives

- **Automated Ingestion & Classification**: Automatically assign one of nine canonical expense categories using regex whole-word boundary matching (`\b`), while allowing manual override when desired.
- **Rigorous Budget Management**: Allow users to allocate monthly budgets per category, enforce uniqueness per month/year, and dynamically track utilization (`WITHIN_BUDGET`, `APPROACHING_LIMIT` at 80%–100%, and `EXCEEDED` over 100%).
- **Advanced Trend Analytics**: Compute historical expenditure trends, Month-over-Month percentage shifts, and category-level percentage shares.
- **Anomaly Detection**: Flag unusual transactions where an expense exceeds twice the category average (with at least 2 historical transactions in that category).
- **Responsive Single-Page Application (SPA)**: Deliver 5 specialized views (Dashboard, Transactions, Analytics, Monthly Reports, Budget) that communicate seamlessly with REST endpoints.

---

## Technology Stack

| Layer | Technology | Specification / Version | Rationale |
| :--- | :--- | :--- | :--- |
| **Language Runtime** | Java SE | OpenJDK **26** (`<release>26</release>`) | Next-generation language features, modern record types, and garbage collection efficiency. |
| **Backend Framework** | Spring Boot | **4.0.10** | Spring Framework 7, Jakarta EE 11 baseline, high-throughput embedded Tomcat 11. |
| **Persistence / ORM** | Spring Data JPA / Hibernate | **7.2.25.Final** / Jakarta Persistence 3.2 | Strongly-typed repository abstractions and declarative transaction management. |
| **Database** | H2 Database Engine | **2.4.240** (Embedded File Mode) | Zero-install, file-persisted ACID compliance at `${user.home}/.smart-expense-analyzer/db`. |
| **Validation** | Jakarta Bean Validation | Hibernate Validator **9.0.3.Final** | Standard `@NotNull`, `@NotBlank`, `@DecimalMin("0.01")`, `@Min`, `@Max` constraints. |
| **Build & Tooling** | Apache Maven | Maven Wrapper **3.3.4** (`distributionType=only-script`) | Hermetic, reproducible builds across developer environments. |
| **Testing** | JUnit 5 & Mockito | JUnit Jupiter **5.11+** / Boot 4 Test Starters, AssertJ | Comprehensive unit, parameterized, MockMvc, and `@SpringBootTest` integration tests. |
| **Frontend UI** | Vanilla HTML5 / CSS3 | Modern CSS Flexbox & Grid | Native browser execution without bundlers, Webpack, or Babel. |
| **Frontend Scripting** | Modular ES6 JavaScript | Native Browser Modules (`type="module"`) | Encapsulated API, chart, modal, and view modules. Zero npm runtime. |
| **Data Visualization**| Chart.js (Vendored UMD) | **4.4.1** (`chart.umd.min.js`) | Offline, high-performance canvas chart rendering with monochrome stepped palettes. |

---

## Architecture & Design System

### Architectural Overview

```
+---------------------------------------------------------------------------------+
|                                CLIENT BROWSER                                   |
|  Single-Page Application (HTML5 / Vanilla CSS3 / Modular ES6 JavaScript)       |
|                                                                                 |
|  [Dashboard View]  [Transactions View]  [Analytics]  [Monthly Reports]  [Budget]|
|         |                   |                |               |              |   |
|         +-------------------+----------------+---------------+--------------+   |
|                                     |                                           |
|                     [api.js (Centralized Fetch Client)]                         |
|                     [charts.js (Monochrome Chart Engine)]                       |
+---------------------------------------------------------------------------------+
                                      | HTTP REST / JSON
                                      v
+---------------------------------------------------------------------------------+
|                           SPRING BOOT 4.0 BACKEND                               |
|                                                                                 |
|  [REST Controllers Layer]                                                       |
|    - ExpenseController      (/api/expenses/**)                                  |
|    - AnalyticsController    (/api/analytics/**)                                 |
|    - BudgetController       (/api/budgets/**)                                   |
|    - GlobalExceptionHandler (@RestControllerAdvice -> Structured Error DTO)       |
|                                                                                 |
|  [Domain Services Layer]                                                        |
|    - CategorizationService  (Regex \b whole-word matching & priority rules)     |
|    - ExpenseService         (CRUD, combined multi-predicate filter, sort)       |
|    - AnalyticsService       (KPIs, percentages, extrema, anomalies, insights)   |
|    - ReportService          (MoM calculation, daily burn, historical trend)     |
|    - BudgetService          (Threshold evaluation, utilization, remaining)      |
|                                                                                 |
|  [Data Access Layer]                                                            |
|    - ExpenseRepository      (Spring Data JPA / Derived & JPQL Queries)          |
|    - BudgetRepository       (Unique constraint queries per month/year)          |
|                                                                                 |
|  [Database Engine]                                                              |
|    - Embedded H2 File DB    (~/.smart-expense-analyzer/db.mv.db)                |
+---------------------------------------------------------------------------------+
```

### Monochromatic Design System

The user interface follows a strict high-contrast financial palette:

| Element | Hex Code | Purpose |
| :--- | :--- | :--- |
| **Canvas Background** | `#FFFFFF` | Primary viewport background. |
| **Surface / Card Background** | `#F8F8F8` | Metric tiles, ledger table container, modal surface. |
| **Hairline Borders** | `#EAEAEA` | 1px clean separators for table rows, inputs, and card borders. |
| **Muted Text / Metadata** | `#888888` | Table headers, secondary timestamps, helper captions. |
| **Secondary Typography** | `#333333` | Table values, filter labels, descriptive paragraphs. |
| **Primary Typography** | `#111111` | Header titles, numerical KPI card values, primary buttons. |
| **Chart Steps (Monochrome)** | `#111111`, `#333333`, `#555555`, `#777777`, `#999999`, `#BBBBBB`, `#D8D8D8` | Stepped grayscale shading across categories in doughnut and bar charts. |

---

## Package Structure

```
Smart-Expense-Analyzer/
|-- backend/
|   |-- pom.xml                                   # Java 26 & Spring Boot 4.0.10 build config
|   |-- mvnw.cmd / mvnw                           # Maven Wrapper scripts (distributionType=only-script)
|   |-- .mvn/wrapper/maven-wrapper.properties     # Maven 3.9.16 wrapper configuration
|   `-- src/
|       |-- main/
|       |   |-- java/com/expenseanalyzer/
|       |   |   |-- SmartExpenseAnalyzerApplication.java   # Spring Boot entry point
|       |   |   |-- config/
|       |   |   |   `-- DataSeeder.java                    # ApplicationRunner initial seed loader
|       |   |   |-- controller/
|       |   |   |   |-- AnalyticsController.java           # /api/analytics endpoints
|       |   |   |   |-- BudgetController.java              # /api/budgets endpoints
|       |   |   |   `-- ExpenseController.java             # /api/expenses endpoints
|       |   |   |-- dto/
|       |   |   |   |-- AnalyticsSummaryDTO.java
|       |   |   |   |-- BudgetRequestDTO.java
|       |   |   |   |-- BudgetStatusDTO.java
|       |   |   |   |-- CategorySummaryDTO.java
|       |   |   |   |-- ErrorResponseDTO.java
|       |   |   |   |-- ExpenseHighlightDTO.java
|       |   |   |   |-- ExpenseRequestDTO.java
|       |   |   |   |-- ExpenseResponseDTO.java
|       |   |   |   |-- FieldErrorDTO.java
|       |   |   |   |-- HighestLowestResponseDTO.java
|       |   |   |   |-- InsightsResponseDTO.java
|       |   |   |   |-- MonthlyReportDTO.java
|       |   |   |   |-- MonthlyTrendPointDTO.java
|       |   |   |   `-- UnusualExpenseDTO.java
|       |   |   |-- exception/
|       |   |   |   |-- DuplicateResourceException.java    # HTTP 409
|       |   |   |   |-- GlobalExceptionHandler.java        # Centralized @RestControllerAdvice
|       |   |   |   `-- ResourceNotFoundException.java     # HTTP 404
|       |   |   |-- model/
|       |   |   |   |-- Budget.java                        # JPA Entity for category limits
|       |   |   |   |-- BudgetStatus.java                  # Enum: WITHIN_BUDGET, APPROACHING_LIMIT, EXCEEDED
|       |   |   |   |-- Expense.java                       # JPA Entity for transactions
|       |   |   |   |-- ExpenseCategory.java               # Enum: 9 canonical categories
|       |   |   |   `-- PaymentMethod.java                 # Enum: CASH, UPI, CARD, OTHER
|       |   |   |-- repository/
|       |   |   |   |-- BudgetRepository.java
|       |   |   |   `-- ExpenseRepository.java
|       |   |   `-- service/
|       |   |       |-- AnalyticsService.java              # KPIs, percentages, anomalies, insights
|       |   |       |-- BudgetService.java                 # Budget threshold evaluation & CRUD
|       |   |       |-- CategorizationService.java         # Rule-based whole-word regex matching
|       |   |       |-- ExpenseService.java                # Transaction CRUD, filtering, sorting
|       |   |       `-- ReportService.java                 # MoM trends, daily average burn
|       |   `-- resources/
|       |       |-- application.properties                 # H2 file persistence, server config
|       |       `-- static/                                # Zero-install static web assets
|       |           |-- index.html                         # Responsive Single Page Application shell
|       |           |-- css/styles.css                     # High-density monochromatic design system
|       |           `-- js/
|       |               |-- api.js                         # Centralized fetch wrapper & INR formatters
|       |               |-- charts.js                      # Chart.js monochrome renderer
|       |               |-- dashboard.js                   # Dashboard KPIs & live widgets
|       |               |-- transactions.js                # Ledger table, filter, sort, modal CRUD
|       |               |-- reports.js                     # Analytics breakdown & monthly reports
|       |               |-- budget.js                      # Budget tracking & utilization gauges
|       |               `-- vendor/
|       |                   `-- chart.umd.min.js           # Locally vendored Chart.js 4.4.1
|       `-- test/
|           |-- java/com/expenseanalyzer/
|           |   |-- SmartExpenseAnalyzerIntegrationTest.java # SpringBootTest E2E & persistence tests
|           |   |-- controller/
|           |   |   |-- AnalyticsControllerTest.java       # Standalone MockMvc REST tests
|           |   |   |-- BudgetControllerTest.java          # Standalone MockMvc REST tests
|           |   |   `-- ExpenseControllerTest.java         # Standalone MockMvc REST tests
|           |   `-- service/
|           |       |-- AnalyticsServiceTest.java          # Edge cases, empty dataset, precision
|           |       |-- BudgetServiceTest.java             # Threshold boundaries (79.99, 80.00, 100.00, 100.01)
|           |       |-- CategorizationServiceTest.java     # Precedence, \b boundary, case insensitivity
|           |       |-- ExpenseServiceTest.java            # Multi-predicate filtering & sorting
|           |       `-- ReportServiceTest.java             # MoM zero denominator & leap year burn
|           `-- resources/
|               `-- application.properties                 # In-memory H2 config for test isolation
|-- ARCHITECTURE.md                                        # Master architecture document
|-- IMPLEMENTATION_PLAN.md                                 # Phased execution roadmap
|-- PROJECT_STATUS.md                                      # Project status and completed milestones
|-- TECHNICAL_DECISIONS.md                                 # Architectural rationale and trade-offs
`-- README.md                                              # Comprehensive project documentation
```

---

## Database Schema & Persistence

### Storage Mode
The application uses an embedded **H2 Database Engine** (v2.4.240) configured in persistent file mode.
- **Physical Path**: `${user.home}/.smart-expense-analyzer/db.mv.db`
- **URL**: `jdbc:h2:file:${user.home}/.smart-expense-analyzer/db;DB_CLOSE_ON_EXIT=TRUE`
- **Dialect**: `org.hibernate.dialect.H2Dialect`
- **DDL Strategy**: `update` (preserves existing records across server restarts)
- **H2 Web Console**: Available at `http://localhost:8080/h2-console` (restricted to `localhost`).

### Table: `expenses`
Stores individual transaction records.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Unique expense identifier. |
| `description` | `VARCHAR(200)` | `NOT NULL` | Description text used for classification. |
| `amount` | `NUMERIC(12, 2)` | `NOT NULL, >= 0.01` | Transaction value in INR (BigDecimal scale 2). |
| `date` | `DATE` | `NOT NULL` | Date of expense (`LocalDate`). |
| `category` | `VARCHAR` | `NOT NULL` | Canonical enum value (`GROCERIES`, `FOOD`, etc.). |
| `payment_method` | `VARCHAR` | `NOT NULL` | Enum: `CASH`, `UPI`, `CARD`, `OTHER`. |
| `notes` | `VARCHAR(500)` | `NULLABLE` | Optional user annotations. |

### Table: `budgets`
Stores monthly allocation limits per category.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY AUTO_INCREMENT` | Unique budget identifier. |
| `category` | `VARCHAR` | `NOT NULL` | Canonical category enum. |
| `monthly_limit` | `NUMERIC(12, 2)` | `NOT NULL, >= 0.01` | Allocated budget limit in INR. |
| `budget_month` | `INT` | `NOT NULL, 1–12` | Calendar month (1–12). |
| `budget_year` | `INT` | `NOT NULL, >= 2000` | Calendar year (e.g. 2026). |

**Unique Constraint**:
```sql
CONSTRAINT uq_budget_category_month_year UNIQUE (category, budget_month, budget_year)
```
Prevents duplicate budget allocations for the same category in the same month and year.

---

## Categorization Logic

Categorization is handled entirely inside `CategorizationService` using regular expressions compiled with case-insensitivity (`Pattern.CASE_INSENSITIVE`) and whole-word boundaries (`\b<keyword>\b`).

### Precedence Hierarchy & Keyword Rules

Rules are evaluated sequentially from top to bottom. The first matching category is returned immediately:

| Priority | Category Enum | Display Label | Keyword Regex List (`\b<keyword>\b`) |
| :---: | :--- | :--- | :--- |
| **1** | `GROCERIES` | Groceries | `vegetables`, `grocery`, `groceries`, `milk`, `fruits`, `supermarket`, `bigbasket`, `blinkit`, `zepto` |
| **2** | `FOOD` | Food | `restaurant`, `pizza`, `coffee`, `cafe`, `lunch`, `dinner`, `breakfast`, `burger`, `zomato`, `swiggy` |
| **3** | `TRANSPORTATION` | Transportation | `uber`, `ola`, `bus`, `train`, `metro`, `fuel`, `petrol`, `diesel`, `cab`, `auto` |
| **4** | `HEALTHCARE` | Healthcare | `medicine`, `hospital`, `pharmacy`, `doctor`, `clinic`, `medical`, `lab` |
| **5** | `EDUCATION` | Education | `books`, `college`, `course`, `tuition`, `udemy`, `coursera`, `exam`, `stationery` |
| **6** | `BILLS_AND_UTILITIES` | Bills & Utilities | `electricity`, `internet`, `wifi`, `rent`, `water`, `broadband`, `recharge`, `bill` |
| **7** | `ENTERTAINMENT` | Entertainment | `netflix`, `cinema`, `movies`, `movie`, `spotify`, `concert`, `gaming` |
| **8** | `SHOPPING` | Shopping | `amazon`, `flipkart`, `clothing`, `clothes`, `shoes`, `electronics`, `mall` |
| **9** | `OTHERS` | Others | *Fallback for unmatched descriptions, null, empty, or whitespace.* |

### Boundary Verification Examples
- `"Business lunch with partner"`: `"bus"` does not match because of `\b` boundaries; `"lunch"` matches `FOOD`. Result: **FOOD**.
- `"Amazon Grocery delivery"`: Both `"amazon"` and `"grocery"` are present; `GROCERIES` has higher precedence than `SHOPPING`. Result: **GROCERIES**.
- `"Parental health checkup"`: `"rent"` does not match `"parental"`. `"health"` is not a keyword, but if `"doctor"` or `"clinic"` is present, matches `HEALTHCARE`.

---

## Analytics & Pattern Recognition Engine

All statistical and analytical calculations use `BigDecimal` with `RoundingMode.HALF_UP`.

### 1. Unusual Spending Anomaly Detection
An expense is flagged as unusual if:
$$\text{amount} > 2.0 \times \overline{X}_{\text{category}}$$
**Pre-condition**: The category must have at least $N \ge 2$ historical transactions. Single transactions are never flagged.

### 2. Month-over-Month (MoM) Percentage Shift
$$\text{MoM Change \%} = \frac{\text{Total}_{\text{current}} - \text{Total}_{\text{previous}}}{\text{Total}_{\text{previous}}} \times 100$$
**Zero-Denominator Guard**: If $\text{Total}_{\text{previous}} = 0$, the calculation safely returns `null` (rendered as `"N/A"` in the UI) to avoid division by zero or misleading `+∞%` indicators.

### 3. Average Daily Burn Rate
$$\text{Daily Burn} = \frac{\text{Total Month Expenditure}}{\text{Days Considered}}$$
- **Current Calendar Month**: Divides by `LocalDate.now().getDayOfMonth()` (e.g. on the 24th of November, divides by 24).
- **Past Calendar Month**: Divides by `YearMonth.lengthOfMonth()` (e.g., 31 for October, 28/29 for February).

### 4. Budget Utilization & Status Thresholds
$$\text{Utilization \%} = \frac{\text{Actual Spent}}{\text{Monthly Limit}} \times 100$$
$$\text{Remaining Amount} = \text{Monthly Limit} - \text{Actual Spent}$$

Threshold boundaries:
- $\text{Utilization} < 80.00\% \implies$ `WITHIN_BUDGET` (Neutral status)
- $80.00\% \le \text{Utilization} \le 100.00\% \implies$ `APPROACHING_LIMIT` (Warning indicator)
- $\text{Utilization} > 100.00\% \implies$ `EXCEEDED` (Alert indicator; `Remaining Amount` is negative)

---

## REST API Reference

All requests and responses use `application/json;charset=UTF-8`.

### Expense Management (`/api/expenses`)

| Method | Endpoint | Query Parameters | Description | Status Codes |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/expenses` | `search`, `category`, `paymentMethod`, `month`, `startDate`, `endDate`, `sortBy`, `order` | Retrieve all transactions with multi-predicate filtering and sorting (`date` \| `amount`, `asc` \| `desc`). | `200 OK`, `400 Bad Request` |
| `GET` | `/api/expenses/{id}` | None | Retrieve a single transaction by ID. | `200 OK`, `404 Not Found` |
| `POST` | `/api/expenses` | None | Create a new transaction. Category is auto-assigned if omitted. | `201 Created`, `400 Bad Request` |
| `PUT` | `/api/expenses/{id}` | None | Update an existing transaction. Preserves category if omitted. | `200 OK`, `400 Bad Request`, `404 Not Found` |
| `DELETE` | `/api/expenses/{id}` | None | Delete a transaction by ID. | `204 No Content`, `404 Not Found` |

#### Sample Expense Request Body:
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

| Method | Endpoint | Query Parameters | Description | Status Codes |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/analytics/summary` | None | Overall total, current-month total, highest expense, and average transaction. | `200 OK` |
| `GET` | `/api/analytics/categories` | None | Complete 9-category breakdown with totals, counts, averages, and percentage shares. | `200 OK` |
| `GET` | `/api/analytics/highest-lowest`| None | Maximum transaction, minimum transaction, overall mean, and Top 5 highest expenses. | `200 OK` |
| `GET` | `/api/analytics/unusual` | None | List of expenses exceeding twice their category average ($N \ge 2$). | `200 OK` |
| `GET` | `/api/analytics/insights` | None | Rule-based natural language insights, budget alerts, and anomaly flags. | `200 OK` |
| `GET` | `/api/analytics/monthly` | `month` (e.g. `2026-11`) | Monthly summary, MoM shift %, daily burn rate, breakdown, and 6-month historical trend (also `/monthly-report`). | `200 OK`, `400 Bad Request` |

### Budget Management (`/api/budgets`)

| Method | Endpoint | Query Parameters | Description | Status Codes |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/budgets` | `month` (e.g. `2026-11`) | List all category budgets for the specified month with utilization progress. | `200 OK` |
| `GET` | `/api/budgets/{id}` | None | Get a specific budget by ID. | `200 OK`, `404 Not Found` |
| `POST` | `/api/budgets` | None | Create a new category budget limit. Enforces uniqueness. | `201 Created`, `400 Bad Request`, `409 Conflict` |
| `PUT` | `/api/budgets/{id}` | None | Update an existing budget's limit or period. | `200 OK`, `400 Bad Request`, `404 Not Found`, `409 Conflict` |
| `DELETE` | `/api/budgets/{id}` | None | Delete a budget limit (associated expenses are retained). | `204 No Content`, `404 Not Found` |

#### Sample Structured Error Response:
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

## Windows Setup Instructions

### Prerequisites
1. **Operating System**: Windows 10 / 11 (64-bit).
2. **Java Development Kit**: JDK 26 installed and accessible in the system path.
   - Verify by executing in PowerShell:
     ```powershell
     java -version
     ```
     *Output must indicate `openjdk version "26"` or higher.*
3. **Git**: Installed for version control.

### Clone the Repository
```powershell
git clone https://github.com/<your-username>/Smart-Expense-Analyzer.git
cd Smart-Expense-Analyzer
```

---

## Execution Instructions

The project includes an embedded Maven Wrapper (`mvnw.cmd`) configured with `distributionType=only-script`. No pre-installed Maven is required.

### 1. Launch the Application
Open PowerShell and run:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Alternatively, build and run the packaged executable JAR:
```powershell
cd backend
.\mvnw.cmd package -DskipTests
java -jar target/smart-expense-analyzer-1.0.0-SNAPSHOT.jar
```

### 2. Access the Application
Once the terminal displays `Tomcat started on port 8080 (http)`:
- **Web Dashboard**: Open [http://localhost:8080/](http://localhost:8080/) in any modern browser (Chrome, Edge, Firefox).
- **H2 Database Console**: Open [http://localhost:8080/h2-console](http://localhost:8080/h2-console).
  - **JDBC URL**: `jdbc:h2:file:C:\Users\<YourUsername>/.smart-expense-analyzer/db`
  - **User Name**: `sa`
  - **Password**: *(leave blank)*

---

## Testing Instructions

The test suite covers unit logic, edge cases, numerical precision, and MockMvc REST endpoints.

### Execute All Tests
In PowerShell, navigate to the `backend` directory and execute:
```powershell
cd backend
.\mvnw.cmd test
```

### What Is Tested:
1. **`CategorizationServiceTest` (26 tests)**:
   - Word boundaries (`\b`): verifies `"Business lunch"` is not matched as `"bus"`, `"Automobile"` is not matched as `"auto"`, etc.
   - Precedence: verifies `GROCERIES` > `SHOPPING`, `GROCERIES` > `FOOD`, `FOOD` > `TRANSPORTATION`, etc.
   - Case-insensitivity: uppercase, mixed-case, and lowercase variants.
   - Fallbacks: null, blank, whitespace, and unknown keywords map to `OTHERS`.
2. **`AnalyticsServiceTest` (6 tests)**:
   - Empty dataset safety: verifies zero values and no `ArithmeticException` or `NoSuchElementException`.
   - Monetary precision: repeating decimal averages (e.g. $\frac{50}{3} \to 16.67$) maintain exact 2-decimal scale.
   - Unusual spending: flags strictly $> 2\times$ average only when category count $N \ge 2$; verifies boundary cases.
   - Extrema and Top 5 descending ordering.
   - Rule-based insights generation and budget warning alerts.
3. **`ReportServiceTest` (7 tests)**:
   - Zero-denominator guard: verifies previous month total 0 yields strictly `null` for MoM change.
   - MoM percentage calculation for positive, negative, and 100% reduction scenarios.
   - Daily average expenditure for 31-day months, 29-day leap February, 28-day non-leap February, and current ongoing month.
   - Historical trend 6-month array generation.
4. **`BudgetServiceTest` (7 tests)**:
   - Status thresholds: `79.99%` (Within Budget), `80.00%` (Approaching Limit), `99.99%` (Approaching Limit), `100.00%` (Approaching Limit), `100.01%` (Exceeded).
   - Remaining amounts: positive, exactly zero, and negative values.
   - Duplicate prevention (`DuplicateResourceException` $\to 409$).
   - Missing entity handling (`ResourceNotFoundException` $\to 404$).
5. **`ExpenseServiceTest` (6 tests)**:
   - Automatic category assignment on null request category.
   - Preservation of manual category overrides during edits.
   - Multi-predicate filtering, sorting, invalid sort parameter validation, and BigDecimal scale normalization.
6. **Controller Tests (`ExpenseControllerTest`, `AnalyticsControllerTest`, `BudgetControllerTest`) (19 tests)**:
   - MockMvc endpoint testing, JSON payload validation, HTTP status codes, and exception handler serialization.
7. **`SmartExpenseAnalyzerIntegrationTest` (5 tests)**:
   - Full `@SpringBootTest` context verification with isolated H2 database: `DataSeeder` completeness & idempotency, 400/404/405/409 handling, and full expense CRUD lifecycle.

**Test Summary**: **76 tests executed, 0 failures, 0 errors, 0 skipped.**

---

## Reset Database & Demo Data Procedure

The application automatically seeds realistic Indian Rupee (INR) transactions across 4 months and 5 budgets on initial launch if the database is empty:
```java
if (expenseRepository.count() == 0) { ... seed ... }
```

If you wish to clear all custom transactions and restore the clean initial demo dataset:

### Step-by-Step Reset:
1. **Stop the running application**:
   In PowerShell, press `Ctrl+C` or execute:
   ```powershell
   Stop-Process -Name java -Force -ErrorAction SilentlyContinue
   ```
2. **Delete the H2 database files**:
   Execute in PowerShell:
   ```powershell
   Remove-Item -Path "$env:USERPROFILE\.smart-expense-analyzer\db.*" -Force
   ```
3. **Restart the application**:
   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```
4. **Result**:
   The `DataSeeder` detects `expenseRepository.count() == 0` and automatically populates:
   - 32 realistic INR transactions across all 9 categories and 4 payment methods spanning August through November 2026.
   - 5 active monthly budgets for the current month (`Food`, `Groceries`, `Transportation`, `Bills & Utilities`, `Entertainment`).

---

## Verification Results

| Milestone | Verified Criterion | Status |
| :--- | :--- | :---: |
| **Phase 1** | Java 26, Maven Wrapper 3.3.4, persistent file-based H2, Spring Boot 4.0.10 application startup. | **PASS** |
| **Phase 2** | `Expense` and `Budget` entities, precision 12 scale 2 `BigDecimal`, unique budget constraint, idempotent `DataSeeder`. | **PASS** |
| **Phase 3** | Rule-based categorization, multi-predicate filter/sort, MoM zero-denominator guard, budget status boundaries. | **PASS** |
| **Phase 4** | 16/16 REST endpoints, `@Valid` bean validation, `@RestControllerAdvice` RFC 7807 error format. | **PASS** |
| **Phase 5** | Responsive Single-Page Application, 5 views, Chart.js monochromatic visualizations, zero CDN dependencies. | **PASS** |
| **Phase 6** | 76/76 JUnit 5 tests passing, live server end-to-end integration verified, full documentation completed. | **PASS** |

---

## Future Enhancements

- **Export & Reporting**: Generate downloadable monthly PDF expenditure statements and CSV audit exports.
- **Multi-Currency Support**: Exchange rate integration for real-time conversion between INR, USD, EUR, and GBP.
- **Receipt OCR Parsing**: Client-side optical character recognition (via WebAssembly Tesseract) for scanning receipt photos into expense records.
- **Recurring Subscription Tracker**: Predictive detection of recurring billing cycles (e.g. Netflix, Spotify, broadband) with renewal reminders.
- **Desktop Application Packaging**: Packaging via `jpackage` into a self-contained Windows `.msi` installer.

---

## License

This project is developed under the MIT License. Designed and built strictly according to master software engineering specifications.
