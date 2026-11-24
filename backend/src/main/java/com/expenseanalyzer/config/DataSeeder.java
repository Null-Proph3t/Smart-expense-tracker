package com.expenseanalyzer.config;

import com.expenseanalyzer.model.Budget;
import com.expenseanalyzer.model.Expense;
import com.expenseanalyzer.model.ExpenseCategory;
import com.expenseanalyzer.model.PaymentMethod;
import com.expenseanalyzer.repository.BudgetRepository;
import com.expenseanalyzer.repository.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the database with realistic Indian INR sample data on the very first
 * application start (when {@code expenseRepository.count() == 0}).
 *
 * <p>Guard: runs only when the {@code expenses} table is empty.
 * Re-starts with existing data are completely idempotent.</p>
 *
 * <p>Seed content (IMPLEMENTATION_PLAN.md §Phase 2):
 * <ul>
 *   <li>≥ 25 realistic INR expenses</li>
 *   <li>All 9 {@link ExpenseCategory} values</li>
 *   <li>All 4 {@link PaymentMethod} values</li>
 *   <li>Spanning the previous 3 calendar months including the current month</li>
 *   <li>4–5 budgets for the current month</li>
 * </ul>
 * </p>
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

    public DataSeeder(ExpenseRepository expenseRepository,
                      BudgetRepository budgetRepository) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (expenseRepository.count() == 0) {
            log.info("DataSeeder: expenses table is empty — seeding sample data.");
            seedExpenses();
            seedBudgets();
            log.info("DataSeeder: seeding complete — {} expenses, {} budgets.",
                    expenseRepository.count(), budgetRepository.count());
        } else {
            log.info("DataSeeder: expenses table has {} rows — skipping seed.",
                    expenseRepository.count());
        }
    }

    // -------------------------------------------------------------------------
    // Expenses
    // -------------------------------------------------------------------------

    private void seedExpenses() {
        // Derive month boundaries dynamically so the seed is always relative to
        // today (TECHNICAL_DECISIONS.md §2.9 / IMPLEMENTATION_PLAN.md §Phase 2).
        YearMonth current  = YearMonth.now();                    // e.g. 2026-11
        YearMonth prev1    = current.minusMonths(1);             // 2026-10
        YearMonth prev2    = current.minusMonths(2);             // 2026-09
        YearMonth prev3    = current.minusMonths(3);             // 2026-08

        List<Expense> expenses = new ArrayList<>();

        // -----------------------------------------------------------------
        // Current month (≥ 7 entries, mix of all categories / methods)
        // -----------------------------------------------------------------
        expenses.add(expense("Swiggy dinner order",          "349.00",  date(current, 3),  ExpenseCategory.FOOD,              PaymentMethod.UPI,  "Butter chicken + naan"));
        expenses.add(expense("BigBasket grocery order",      "1245.50", date(current, 5),  ExpenseCategory.GROCERIES,         PaymentMethod.CARD, "Monthly essentials — oils, pulses, rice"));
        expenses.add(expense("Ola cab to airport",           "680.00",  date(current, 7),  ExpenseCategory.TRANSPORTATION,    PaymentMethod.UPI,  null));
        expenses.add(expense("Amazon — wireless earbuds",    "1899.00", date(current, 9),  ExpenseCategory.SHOPPING,          PaymentMethod.CARD, "Boat Airdopes 141"));
        expenses.add(expense("Netflix subscription",         "649.00",  date(current, 11), ExpenseCategory.ENTERTAINMENT,     PaymentMethod.CARD, "Monthly plan"));
        expenses.add(expense("Apollo Pharmacy medicines",    "520.00",  date(current, 13), ExpenseCategory.HEALTHCARE,        PaymentMethod.CASH, "Vitamins and cold medicine"));
        expenses.add(expense("Coursera Python course",       "2999.00", date(current, 14), ExpenseCategory.EDUCATION,         PaymentMethod.CARD, "7-day free trial cancelled"));
        expenses.add(expense("Jio postpaid bill",            "999.00",  date(current, 15), ExpenseCategory.BILLS_AND_UTILITIES, PaymentMethod.UPI, "November bill"));
        expenses.add(expense("Morning chai & poha",          "85.00",   date(current, 16), ExpenseCategory.FOOD,              PaymentMethod.CASH,  "Local tapri breakfast"));
        expenses.add(expense("BEST bus monthly pass",        "380.00",  date(current, 17), ExpenseCategory.TRANSPORTATION,    PaymentMethod.UPI,   "Office commute pass"));
        expenses.add(expense("NetBanking — passport fee",    "1500.00", date(current, 4),  ExpenseCategory.OTHERS,            PaymentMethod.OTHER, "Government portal renewal via net banking"));

        // -----------------------------------------------------------------
        // Previous month 1 (≥ 7 entries)
        // -----------------------------------------------------------------
        expenses.add(expense("Zomato lunch — burger & fries", "420.00",  date(prev1, 2),  ExpenseCategory.FOOD,              PaymentMethod.UPI,  null));
        expenses.add(expense("DMart weekly groceries",        "1875.00", date(prev1, 6),  ExpenseCategory.GROCERIES,         PaymentMethod.CASH, "Weekly staples"));
        expenses.add(expense("Rapido bike ride",              "95.00",   date(prev1, 8),  ExpenseCategory.TRANSPORTATION,    PaymentMethod.UPI,  null));
        expenses.add(expense("Myntra kurta set",              "1299.00", date(prev1, 10), ExpenseCategory.SHOPPING,          PaymentMethod.CARD, "Festive shopping"));
        expenses.add(expense("BookMyShow — 2 movie tickets",  "580.00",  date(prev1, 12), ExpenseCategory.ENTERTAINMENT,     PaymentMethod.UPI,  "Weekend multiplex show"));
        expenses.add(expense("Dental checkup — Dr Sharma",   "800.00",  date(prev1, 14), ExpenseCategory.HEALTHCARE,        PaymentMethod.CASH, "Routine cleaning"));
        expenses.add(expense("Udemy web dev bootcamp",        "499.00",  date(prev1, 15), ExpenseCategory.EDUCATION,         PaymentMethod.CARD, "On sale — 90% off"));
        expenses.add(expense("BSES electricity bill",         "1420.00", date(prev1, 18), ExpenseCategory.BILLS_AND_UTILITIES, PaymentMethod.UPI, "October bill"));
        expenses.add(expense("Parking fine — MCD",           "250.00",  date(prev1, 20), ExpenseCategory.OTHERS,            PaymentMethod.CASH, "Overstayed paid zone"));
        expenses.add(expense("Haldiram's snacks box",         "375.00",  date(prev1, 22), ExpenseCategory.GROCERIES,         PaymentMethod.CARD, "Diwali snacks for family"));

        // -----------------------------------------------------------------
        // Previous month 2 (≥ 6 entries)
        // -----------------------------------------------------------------
        expenses.add(expense("Chai sutta — evening snack",  "60.00",   date(prev2, 4),  ExpenseCategory.FOOD,              PaymentMethod.CASH, null));
        expenses.add(expense("Metro card top-up",           "500.00",  date(prev2, 7),  ExpenseCategory.TRANSPORTATION,    PaymentMethod.UPI,  null));
        expenses.add(expense("Flipkart — running shoes",    "2499.00", date(prev2, 11), ExpenseCategory.SHOPPING,          PaymentMethod.CARD, "Puma Softride"));
        expenses.add(expense("Spotify premium — 3 months",  "489.00",  date(prev2, 13), ExpenseCategory.ENTERTAINMENT,     PaymentMethod.UPI,  "Quarterly plan"));
        expenses.add(expense("AIIMS OPD consultation",      "300.00",  date(prev2, 16), ExpenseCategory.HEALTHCARE,        PaymentMethod.CASH, "Follow-up visit"));
        expenses.add(expense("Internet broadband — ACT",    "799.00",  date(prev2, 20), ExpenseCategory.BILLS_AND_UTILITIES, PaymentMethod.UPI, "September bill"));

        // -----------------------------------------------------------------
        // Previous month 3 (≥ 5 entries)
        // -----------------------------------------------------------------
        expenses.add(expense("Vegetable sabzi from vendor",  "180.00",  date(prev3, 3),  ExpenseCategory.GROCERIES,         PaymentMethod.CASH, "Weekly fresh veggies"));
        expenses.add(expense("Uber ride to railway station", "320.00",  date(prev3, 9),  ExpenseCategory.TRANSPORTATION,    PaymentMethod.UPI,  null));
        expenses.add(expense("Programming books — Amazon",   "1150.00", date(prev3, 14), ExpenseCategory.EDUCATION,         PaymentMethod.CARD, "Clean Code + Java concurrency"));
        expenses.add(expense("Cooking gas cylinder LPG",    "910.00",  date(prev3, 17), ExpenseCategory.BILLS_AND_UTILITIES, PaymentMethod.CASH, "Indane cylinder refill"));
        expenses.add(expense("Charity donation — NGO",       "500.00",  date(prev3, 22), ExpenseCategory.OTHERS,            PaymentMethod.UPI,  "CRY — Child Rights & You"));

        expenseRepository.saveAll(expenses);
        log.info("DataSeeder: saved {} expenses.", expenses.size());
    }

    // -------------------------------------------------------------------------
    // Budgets — current month only (IMPLEMENTATION_PLAN.md §Phase 2)
    // Guard: skip a budget if the unique key already exists to avoid crashing
    // on a scenario where user has cleared expenses but kept budgets
    // (TECHNICAL_DECISIONS.md §2.10).
    // -------------------------------------------------------------------------

    private void seedBudgets() {
        YearMonth current = YearMonth.now();
        int month = current.getMonthValue();
        int year  = current.getYear();

        seedBudgetIfAbsent(ExpenseCategory.FOOD,              "5000.00", month, year);
        seedBudgetIfAbsent(ExpenseCategory.GROCERIES,         "8000.00", month, year);
        seedBudgetIfAbsent(ExpenseCategory.TRANSPORTATION,    "3000.00", month, year);
        seedBudgetIfAbsent(ExpenseCategory.ENTERTAINMENT,     "800.00",  month, year);
        seedBudgetIfAbsent(ExpenseCategory.BILLS_AND_UTILITIES, "4000.00", month, year);
    }

    /**
     * Inserts a budget only if no row already exists for the given
     * {@code (category, budgetMonth, budgetYear)} triple.
     */
    private void seedBudgetIfAbsent(ExpenseCategory category,
                                    String limit,
                                    int budgetMonth,
                                    int budgetYear) {
        boolean exists = budgetRepository
                .findByCategoryAndBudgetMonthAndBudgetYear(category, budgetMonth, budgetYear)
                .isPresent();
        if (!exists) {
            budgetRepository.save(
                    new Budget(category, new BigDecimal(limit), budgetMonth, budgetYear));
            log.debug("DataSeeder: inserted budget {} {}/{}", category, budgetMonth, budgetYear);
        } else {
            log.debug("DataSeeder: budget {}/{}/{} already exists — skipped.",
                    category, budgetMonth, budgetYear);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Constructs an Expense with scale-2 BigDecimal amount.
     */
    private static Expense expense(String description,
                                   String amount,
                                   LocalDate date,
                                   ExpenseCategory category,
                                   PaymentMethod paymentMethod,
                                   String notes) {
        return new Expense(description, new BigDecimal(amount), date, category, paymentMethod, notes);
    }

    /**
     * Returns a date in the given {@link YearMonth} clamped to the last valid
     * day of that month (e.g. day 31 in February becomes the 28th/29th).
     */
    private static LocalDate date(YearMonth ym, int day) {
        int clamped = Math.min(day, ym.lengthOfMonth());
        return ym.atDay(clamped);
    }
}
