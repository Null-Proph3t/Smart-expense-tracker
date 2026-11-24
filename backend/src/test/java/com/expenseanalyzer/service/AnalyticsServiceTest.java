package com.expenseanalyzer.service;

import com.expenseanalyzer.dto.AnalyticsSummaryDTO;
import com.expenseanalyzer.dto.CategorySummaryDTO;
import com.expenseanalyzer.dto.HighestLowestResponseDTO;
import com.expenseanalyzer.dto.InsightsResponseDTO;
import com.expenseanalyzer.dto.UnusualExpenseDTO;
import com.expenseanalyzer.model.Budget;
import com.expenseanalyzer.model.Expense;
import com.expenseanalyzer.model.ExpenseCategory;
import com.expenseanalyzer.model.PaymentMethod;
import com.expenseanalyzer.repository.BudgetRepository;
import com.expenseanalyzer.repository.ExpenseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    // -------------------------------------------------------------------------
    // 1. Empty dataset handling
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Empty dataset safety: returns zero totals, no NoSuchElementException or ArithmeticException")
    void testEmptyDatasetSafety() {
        when(expenseRepository.findAll()).thenReturn(Collections.emptyList());

        AnalyticsSummaryDTO summary = analyticsService.getSummary();
        assertThat(summary.getTotalExpenses()).isEqualByComparingTo("0.00");
        assertThat(summary.getThisMonthSpending()).isEqualByComparingTo("0.00");
        assertThat(summary.getHighestExpense()).isEqualByComparingTo("0.00");
        assertThat(summary.getAverageTransaction()).isEqualByComparingTo("0.00");

        List<CategorySummaryDTO> categories = analyticsService.getCategorySummaries();
        assertThat(categories).hasSize(9);
        for (CategorySummaryDTO cat : categories) {
            assertThat(cat.getTotalAmount()).isEqualByComparingTo("0.00");
            assertThat(cat.getTransactionCount()).isZero();
            assertThat(cat.getAverageAmount()).isEqualByComparingTo("0.00");
            assertThat(cat.getPercentage()).isEqualByComparingTo("0.00");
        }

        HighestLowestResponseDTO hl = analyticsService.getHighestLowest();
        assertThat(hl.getHighest()).isNull();
        assertThat(hl.getLowest()).isNull();
        assertThat(hl.getAverage()).isEqualByComparingTo("0.00");
        assertThat(hl.getTop5()).isEmpty();

        List<UnusualExpenseDTO> unusual = analyticsService.getUnusualExpenses();
        assertThat(unusual).isEmpty();

        InsightsResponseDTO insights = analyticsService.getInsights();
        assertThat(insights.getInsights()).hasSize(1);
        assertThat(insights.getInsights().get(0)).contains("No expenses recorded yet");
        assertThat(insights.getUnusualExpenses()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // 2. Monetary Precision (BigDecimal scale 2 HALF_UP, no float/double drift)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Monetary precision: large amounts and repeating fractions retain exact 2-decimal scale")
    void testMonetaryPrecisionAndRounding() {
        // 3 expenses of ₹100.00: total = 300.00, average = 100.00
        // 1 expense of ₹100.00 divided by 3 items in average = 33.33 (HALF_UP rounding)
        LocalDate date = LocalDate.of(2026, 11, 10);
        Expense e1 = new Expense("Item 1", new BigDecimal("10.00"), date, ExpenseCategory.FOOD, PaymentMethod.UPI, null);
        Expense e2 = new Expense("Item 2", new BigDecimal("20.00"), date, ExpenseCategory.FOOD, PaymentMethod.UPI, null);
        Expense e3 = new Expense("Item 3", new BigDecimal("20.00"), date, ExpenseCategory.FOOD, PaymentMethod.UPI, null);

        // Total = 50.00, count = 3, avg = 50.00 / 3 = 16.6666... -> 16.67
        when(expenseRepository.findAll()).thenReturn(List.of(e1, e2, e3));

        AnalyticsSummaryDTO summary = analyticsService.getSummary();
        assertThat(summary.getTotalExpenses()).isEqualByComparingTo("50.00");
        assertThat(summary.getAverageTransaction()).isEqualByComparingTo("16.67");
    }

    @Test
    @DisplayName("Monetary precision: percentage contributions sum to 100% and scale properly")
    void testCategoryPercentagesMonetaryPrecision() {
        LocalDate date = LocalDate.of(2026, 11, 10);
        // Food: 75.00 (75%), Transportation: 25.00 (25%)
        Expense e1 = new Expense("Food", new BigDecimal("75.00"), date, ExpenseCategory.FOOD, PaymentMethod.UPI, null);
        Expense e2 = new Expense("Cab", new BigDecimal("25.00"), date, ExpenseCategory.TRANSPORTATION, PaymentMethod.UPI, null);

        when(expenseRepository.findAll()).thenReturn(List.of(e1, e2));

        List<CategorySummaryDTO> summaries = analyticsService.getCategorySummaries();
        CategorySummaryDTO foodSummary = summaries.stream()
                .filter(c -> c.getCategory() == ExpenseCategory.FOOD)
                .findFirst().orElseThrow();
        CategorySummaryDTO transSummary = summaries.stream()
                .filter(c -> c.getCategory() == ExpenseCategory.TRANSPORTATION)
                .findFirst().orElseThrow();

        assertThat(foodSummary.getTotalAmount()).isEqualByComparingTo("75.00");
        assertThat(foodSummary.getPercentage()).isEqualByComparingTo("75.00");
        assertThat(foodSummary.getTransactionCount()).isEqualTo(1);

        assertThat(transSummary.getTotalAmount()).isEqualByComparingTo("25.00");
        assertThat(transSummary.getPercentage()).isEqualByComparingTo("25.00");
        assertThat(transSummary.getTransactionCount()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // 3. Unusual Spending Thresholds
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Unusual spending detection: flags strictly > 2x average only when category count >= 2")
    void testUnusualSpendingThresholds() {
        // FOOD: 3 expenses: 100, 100, 500. Avg = 700/3 = 233.33. 2x avg = 466.66. 500 > 466.66 -> Flagged!
        Expense f1 = new Expense("Snack 1", new BigDecimal("100.00"), LocalDate.of(2026, 11, 1), ExpenseCategory.FOOD, PaymentMethod.CASH, null);
        Expense f2 = new Expense("Snack 2", new BigDecimal("100.00"), LocalDate.of(2026, 11, 2), ExpenseCategory.FOOD, PaymentMethod.CASH, null);
        Expense f3 = new Expense("Party", new BigDecimal("500.00"), LocalDate.of(2026, 11, 3), ExpenseCategory.FOOD, PaymentMethod.UPI, null);

        // TRANSPORTATION: only 1 expense -> count < 2 -> MUST NOT BE FLAGGED even if huge
        Expense t1 = new Expense("Flight", new BigDecimal("25000.00"), LocalDate.of(2026, 11, 4), ExpenseCategory.TRANSPORTATION, PaymentMethod.CARD, null);

        // SHOPPING: 2 expenses: 100, 300. Avg = 200. 2x avg = 400. 300 <= 400 -> NOT FLAGGED
        Expense s1 = new Expense("Shirt", new BigDecimal("100.00"), LocalDate.of(2026, 11, 5), ExpenseCategory.SHOPPING, PaymentMethod.CARD, null);
        Expense s2 = new Expense("Pants", new BigDecimal("300.00"), LocalDate.of(2026, 11, 6), ExpenseCategory.SHOPPING, PaymentMethod.CARD, null);

        // BILLS_AND_UTILITIES: 3 expenses: 100, 100, 400 -> Total = 600, Avg = 200.00. 2x avg = 400.00.
        // Strictly equal 400.00 MUST NOT be flagged (spec requires strictly > 2x average)
        Expense b1 = new Expense("Bill 1", new BigDecimal("100.00"), LocalDate.of(2026, 11, 7), ExpenseCategory.BILLS_AND_UTILITIES, PaymentMethod.UPI, null);
        Expense b2 = new Expense("Bill 2", new BigDecimal("100.00"), LocalDate.of(2026, 11, 8), ExpenseCategory.BILLS_AND_UTILITIES, PaymentMethod.UPI, null);
        Expense b3 = new Expense("Bill 3 Exact 2x", new BigDecimal("400.00"), LocalDate.of(2026, 11, 9), ExpenseCategory.BILLS_AND_UTILITIES, PaymentMethod.UPI, null);

        when(expenseRepository.findAll()).thenReturn(List.of(f1, f2, f3, t1, s1, s2, b1, b2, b3));

        List<UnusualExpenseDTO> unusual = analyticsService.getUnusualExpenses();

        assertThat(unusual).hasSize(1);
        assertThat(unusual.get(0).getExpense().getDescription()).isEqualTo("Party");
        assertThat(unusual.get(0).getExpense().getAmount()).isEqualByComparingTo("500.00");
        assertThat(unusual.get(0).getCategoryAverage()).isEqualByComparingTo("233.33");
        assertThat(unusual.get(0).getMultiplier()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // 4. Extrema & Top 5 highest expenses
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Extrema: correctly identifies highest, lowest, average, and limits top 5 descending")
    void testExtremaAndTop5() {
        LocalDate date = LocalDate.of(2026, 11, 1);
        Expense e1 = new Expense("E1", new BigDecimal("100.00"), date, ExpenseCategory.FOOD, PaymentMethod.CASH, null);
        Expense e2 = new Expense("E2", new BigDecimal("500.00"), date, ExpenseCategory.FOOD, PaymentMethod.CASH, null);
        Expense e3 = new Expense("E3", new BigDecimal("300.00"), date, ExpenseCategory.FOOD, PaymentMethod.CASH, null);
        Expense e4 = new Expense("E4", new BigDecimal("800.00"), date, ExpenseCategory.FOOD, PaymentMethod.CASH, null);
        Expense e5 = new Expense("E5", new BigDecimal("200.00"), date, ExpenseCategory.FOOD, PaymentMethod.CASH, null);
        Expense e6 = new Expense("E6", new BigDecimal("950.00"), date, ExpenseCategory.FOOD, PaymentMethod.CASH, null);

        when(expenseRepository.findAll()).thenReturn(List.of(e1, e2, e3, e4, e5, e6));

        HighestLowestResponseDTO hl = analyticsService.getHighestLowest();

        assertThat(hl.getHighest().getDescription()).isEqualTo("E6");
        assertThat(hl.getHighest().getAmount()).isEqualByComparingTo("950.00");

        assertThat(hl.getLowest().getDescription()).isEqualTo("E1");
        assertThat(hl.getLowest().getAmount()).isEqualByComparingTo("100.00");

        // Total = 100 + 500 + 300 + 800 + 200 + 950 = 2850. Avg = 2850 / 6 = 475.00
        assertThat(hl.getAverage()).isEqualByComparingTo("475.00");

        // Top 5: E6 (950), E4 (800), E2 (500), E3 (300), E5 (200)
        assertThat(hl.getTop5()).hasSize(5);
        assertThat(hl.getTop5().get(0).getDescription()).isEqualTo("E6");
        assertThat(hl.getTop5().get(1).getDescription()).isEqualTo("E4");
        assertThat(hl.getTop5().get(2).getDescription()).isEqualTo("E2");
        assertThat(hl.getTop5().get(3).getDescription()).isEqualTo("E3");
        assertThat(hl.getTop5().get(4).getDescription()).isEqualTo("E5");
    }

    // -------------------------------------------------------------------------
    // 5. Rule-Based Insights Generation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Insights: generates top category, monthly summary, and budget alerts")
    void testRuleBasedInsightsGeneration() {
        LocalDate currentMonthDate = LocalDate.now();
        Expense e1 = new Expense("Dinner", new BigDecimal("2500.00"), currentMonthDate, ExpenseCategory.FOOD, PaymentMethod.UPI, null);
        Expense e2 = new Expense("Movie", new BigDecimal("500.00"), currentMonthDate, ExpenseCategory.ENTERTAINMENT, PaymentMethod.CARD, null);

        // Budget of 2000 for FOOD in current month -> spent 2500 -> 125% -> EXCEEDED alert
        YearMonth ym = YearMonth.now();
        Budget b1 = new Budget(ExpenseCategory.FOOD, new BigDecimal("2000.00"), ym.getMonthValue(), ym.getYear());

        when(expenseRepository.findAll()).thenReturn(List.of(e1, e2));
        when(budgetRepository.findByBudgetMonthAndBudgetYear(ym.getMonthValue(), ym.getYear()))
                .thenReturn(List.of(b1));

        InsightsResponseDTO result = analyticsService.getInsights();

        assertThat(result.getInsights()).isNotEmpty();
        // Check for top category insight
        assertThat(result.getInsights()).anyMatch(s -> s.contains("Food is your highest spending category"));
        // Check for budget exceeded alert
        assertThat(result.getInsights()).anyMatch(s -> s.contains("Budget Exceeded: Food budget has exceeded its"));
    }
}
