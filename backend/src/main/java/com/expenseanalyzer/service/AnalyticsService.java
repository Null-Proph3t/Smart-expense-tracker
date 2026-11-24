package com.expenseanalyzer.service;

import com.expenseanalyzer.dto.AnalyticsSummaryDTO;
import com.expenseanalyzer.dto.CategorySummaryDTO;
import com.expenseanalyzer.dto.ExpenseHighlightDTO;
import com.expenseanalyzer.dto.ExpenseResponseDTO;
import com.expenseanalyzer.dto.HighestLowestResponseDTO;
import com.expenseanalyzer.dto.InsightsResponseDTO;
import com.expenseanalyzer.dto.UnusualExpenseDTO;
import com.expenseanalyzer.model.Budget;
import com.expenseanalyzer.model.Expense;
import com.expenseanalyzer.model.ExpenseCategory;
import com.expenseanalyzer.model.PaymentMethod;
import com.expenseanalyzer.repository.BudgetRepository;
import com.expenseanalyzer.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for computing overall analytics, category breakdowns, extrema,
 * unusual spending detection, and automated rule-based insights (spec §4.1 Module C, D & §4.2 B, C).
 */
@Service
public class AnalyticsService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO_SCALE_2 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal TWO = new BigDecimal("2");
    private static final BigDecimal EIGHTY = new BigDecimal("80.00");

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

    public AnalyticsService(ExpenseRepository expenseRepository,
                            BudgetRepository budgetRepository) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
    }

    /**
     * Computes the 4 KPI card summary metrics for the dashboard:
     * 1. Total Expenses (all time)
     * 2. This Month's Spending
     * 3. Highest Expense (all time)
     * 4. Average Transaction (all time)
     */
    @Transactional(readOnly = true)
    public AnalyticsSummaryDTO getSummary() {
        List<Expense> allExpenses = expenseRepository.findAll();

        if (allExpenses.isEmpty()) {
            return new AnalyticsSummaryDTO(ZERO_SCALE_2, ZERO_SCALE_2, ZERO_SCALE_2, ZERO_SCALE_2);
        }

        BigDecimal totalExpenses = allExpenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        YearMonth currentMonth = YearMonth.now();
        BigDecimal thisMonthSpending = allExpenses.stream()
                .filter(e -> e.getDate() != null && YearMonth.from(e.getDate()).equals(currentMonth))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal highestExpense = allExpenses.stream()
                .map(Expense::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(ZERO_SCALE_2)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal averageTransaction = totalExpenses.divide(
                BigDecimal.valueOf(allExpenses.size()), 2, RoundingMode.HALF_UP);

        return new AnalyticsSummaryDTO(totalExpenses, thisMonthSpending, highestExpense, averageTransaction);
    }

    /**
     * Computes category-wise expenditure, transaction counts, averages, and percentage contributions.
     * Includes all canonical categories in declaration order.
     */
    @Transactional(readOnly = true)
    public List<CategorySummaryDTO> getCategorySummaries() {
        List<Expense> allExpenses = expenseRepository.findAll();
        return computeCategorySummariesForList(allExpenses);
    }

    /**
     * Identifies the highest, lowest, average, and top 5 expenses.
     * Safe for empty datasets (returns nulls / empty lists without throwing exceptions).
     */
    @Transactional(readOnly = true)
    public HighestLowestResponseDTO getHighestLowest() {
        List<Expense> allExpenses = expenseRepository.findAll();

        if (allExpenses.isEmpty()) {
            return new HighestLowestResponseDTO(null, null, ZERO_SCALE_2, Collections.emptyList());
        }

        Expense highestEntity = allExpenses.stream()
                .max(Comparator.comparing(Expense::getAmount))
                .orElse(null);

        Expense lowestEntity = allExpenses.stream()
                .min(Comparator.comparing(Expense::getAmount))
                .orElse(null);

        BigDecimal total = allExpenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = total.divide(BigDecimal.valueOf(allExpenses.size()), 2, RoundingMode.HALF_UP);

        List<ExpenseHighlightDTO> top5 = allExpenses.stream()
                .sorted(Comparator.comparing(Expense::getAmount).reversed())
                .limit(5)
                .map(ExpenseHighlightDTO::fromEntity)
                .toList();

        return new HighestLowestResponseDTO(
                ExpenseHighlightDTO.fromEntity(highestEntity),
                ExpenseHighlightDTO.fromEntity(lowestEntity),
                average,
                top5
        );
    }

    /**
     * Flags expenses whose amount is strictly greater than 2x the category average
     * for categories with at least 2 transactions.
     */
    @Transactional(readOnly = true)
    public List<UnusualExpenseDTO> getUnusualExpenses() {
        List<Expense> allExpenses = expenseRepository.findAll();
        return detectUnusualExpenses(allExpenses);
    }

    /**
     * Generates automated rule-based spending insights derived strictly from database records.
     */
    @Transactional(readOnly = true)
    public InsightsResponseDTO getInsights() {
        List<Expense> allExpenses = expenseRepository.findAll();
        List<UnusualExpenseDTO> unusualExpenses = detectUnusualExpenses(allExpenses);
        List<String> insights = new ArrayList<>();

        if (allExpenses.isEmpty()) {
            insights.add("No expenses recorded yet. Add your first transaction to unlock spending insights.");
            return new InsightsResponseDTO(insights, unusualExpenses);
        }

        // 1. Top spending category insight
        List<CategorySummaryDTO> categorySummaries = computeCategorySummariesForList(allExpenses);
        categorySummaries.stream()
                .filter(c -> c.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.comparing(CategorySummaryDTO::getTotalAmount))
                .ifPresent(topCat -> insights.add(String.format(
                        "%s is your highest spending category, accounting for %s%% (₹%s) of total expenditure.",
                        topCat.getCategoryLabel(),
                        topCat.getPercentage(),
                        topCat.getTotalAmount()
                )));

        // 2. Current month spending & daily average
        YearMonth currentMonth = YearMonth.now();
        List<Expense> currentMonthExpenses = allExpenses.stream()
                .filter(e -> e.getDate() != null && YearMonth.from(e.getDate()).equals(currentMonth))
                .toList();

        if (!currentMonthExpenses.isEmpty()) {
            BigDecimal currentMonthTotal = currentMonthExpenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            int dayOfMonth = LocalDate.now().getDayOfMonth();
            BigDecimal dailyAvg = currentMonthTotal.divide(BigDecimal.valueOf(dayOfMonth), 2, RoundingMode.HALF_UP);

            insights.add(String.format(
                    "This month (%s), total spending is ₹%s across %d transaction(s), averaging ₹%s per day.",
                    currentMonth,
                    currentMonthTotal,
                    currentMonthExpenses.size(),
                    dailyAvg
            ));
        }

        // 3. Month-over-Month category trends (spikes/drops)
        YearMonth prevMonth = currentMonth.minusMonths(1);
        List<Expense> prevMonthExpenses = allExpenses.stream()
                .filter(e -> e.getDate() != null && YearMonth.from(e.getDate()).equals(prevMonth))
                .toList();

        if (!prevMonthExpenses.isEmpty() && !currentMonthExpenses.isEmpty()) {
            Map<ExpenseCategory, BigDecimal> prevCategoryTotals = sumByCategory(prevMonthExpenses);
            Map<ExpenseCategory, BigDecimal> currentCategoryTotals = sumByCategory(currentMonthExpenses);

            for (Map.Entry<ExpenseCategory, BigDecimal> entry : currentCategoryTotals.entrySet()) {
                ExpenseCategory cat = entry.getKey();
                BigDecimal currAmt = entry.getValue();
                BigDecimal prevAmt = prevCategoryTotals.getOrDefault(cat, BigDecimal.ZERO);

                if (prevAmt.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal change = currAmt.subtract(prevAmt).multiply(HUNDRED)
                            .divide(prevAmt, 2, RoundingMode.HALF_UP);
                    if (change.compareTo(new BigDecimal("25.00")) >= 0) {
                        insights.add(String.format(
                                "Spending in %s increased by %s%% compared to last month (₹%s vs ₹%s).",
                                cat.getDisplayLabel(), change, currAmt, prevAmt
                        ));
                    } else if (change.compareTo(new BigDecimal("-25.00")) <= 0) {
                        insights.add(String.format(
                                "Spending in %s decreased by %s%% compared to last month (₹%s vs ₹%s).",
                                cat.getDisplayLabel(), change.abs(), currAmt, prevAmt
                        ));
                    }
                }
            }
        }

        // 4. Budget limit alerts & warnings for current month
        List<Budget> currentBudgets = budgetRepository.findByBudgetMonthAndBudgetYear(
                currentMonth.getMonthValue(), currentMonth.getYear());

        for (Budget budget : currentBudgets) {
            if (budget.getMonthlyLimit() == null || budget.getMonthlyLimit().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal spent = currentMonthExpenses.stream()
                    .filter(e -> e.getCategory() == budget.getCategory())
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal utilization = spent.multiply(HUNDRED)
                    .divide(budget.getMonthlyLimit(), 2, RoundingMode.HALF_UP);

            if (utilization.compareTo(HUNDRED) > 0) {
                BigDecimal overspent = spent.subtract(budget.getMonthlyLimit());
                insights.add(String.format(
                        "Budget Exceeded: %s budget has exceeded its ₹%s limit by ₹%s (%s%% utilized).",
                        budget.getCategory().getDisplayLabel(),
                        budget.getMonthlyLimit(),
                        overspent,
                        utilization
                ));
            } else if (utilization.compareTo(EIGHTY) >= 0) {
                insights.add(String.format(
                        "Budget Warning: %s budget is approaching its limit at %s%% utilization (₹%s / ₹%s).",
                        budget.getCategory().getDisplayLabel(),
                        utilization,
                        spent,
                        budget.getMonthlyLimit()
                ));
            }
        }

        // 5. Unusual spending alert
        if (!unusualExpenses.isEmpty()) {
            insights.add(String.format(
                    "Unusual Activity: %d transaction(s) were flagged for exceeding 2x their respective category average.",
                    unusualExpenses.size()
            ));
        }

        // 6. Most frequent payment method
        Map<PaymentMethod, Long> paymentMethodCounts = allExpenses.stream()
                .filter(e -> e.getPaymentMethod() != null)
                .collect(Collectors.groupingBy(Expense::getPaymentMethod, Collectors.counting()));

        paymentMethodCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(topMethod -> insights.add(String.format(
                        "Primary payment method is %s with %d transaction(s).",
                        topMethod.getKey().getDisplayLabel(),
                        topMethod.getValue()
                )));

        return new InsightsResponseDTO(insights, unusualExpenses);
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Computes CategorySummaryDTO for each category given an expense list.
     */
    public List<CategorySummaryDTO> computeCategorySummariesForList(List<Expense> expenses) {
        BigDecimal overallTotal = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Map<ExpenseCategory, List<Expense>> grouped = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory));

        List<CategorySummaryDTO> summaries = new ArrayList<>();

        for (ExpenseCategory category : ExpenseCategory.values()) {
            List<Expense> catExpenses = grouped.getOrDefault(category, Collections.emptyList());
            long count = catExpenses.size();

            BigDecimal total = catExpenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal average = count > 0
                    ? total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                    : ZERO_SCALE_2;

            BigDecimal percentage = overallTotal.compareTo(BigDecimal.ZERO) > 0
                    ? total.multiply(HUNDRED).divide(overallTotal, 2, RoundingMode.HALF_UP)
                    : ZERO_SCALE_2;

            summaries.add(new CategorySummaryDTO(
                    category,
                    category.getDisplayLabel(),
                    total,
                    count,
                    average,
                    percentage
            ));
        }

        return summaries;
    }

    private List<UnusualExpenseDTO> detectUnusualExpenses(List<Expense> allExpenses) {
        Map<ExpenseCategory, List<Expense>> byCategory = allExpenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory));

        List<UnusualExpenseDTO> unusualList = new ArrayList<>();

        for (Map.Entry<ExpenseCategory, List<Expense>> entry : byCategory.entrySet()) {
            List<Expense> catExpenses = entry.getValue();
            // Flagging requires at least 2 transactions in that category
            if (catExpenses.size() < 2) {
                continue;
            }

            BigDecimal catTotal = catExpenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal catAverage = catTotal.divide(
                    BigDecimal.valueOf(catExpenses.size()), 2, RoundingMode.HALF_UP);

            BigDecimal threshold = catAverage.multiply(TWO);

            for (Expense exp : catExpenses) {
                // Strictly greater than 2x category average
                if (exp.getAmount().compareTo(threshold) > 0) {
                    BigDecimal multiplier = catAverage.compareTo(BigDecimal.ZERO) > 0
                            ? exp.getAmount().divide(catAverage, 2, RoundingMode.HALF_UP)
                            : ZERO_SCALE_2;

                    unusualList.add(new UnusualExpenseDTO(
                            ExpenseResponseDTO.fromEntity(exp),
                            catAverage,
                            multiplier
                    ));
                }
            }
        }

        // Sort by amount descending
        unusualList.sort(Comparator.comparing((UnusualExpenseDTO u) -> u.getExpense().getAmount()).reversed());
        return unusualList;
    }

    private static Map<ExpenseCategory, BigDecimal> sumByCategory(List<Expense> expenses) {
        Map<ExpenseCategory, BigDecimal> map = new HashMap<>();
        for (Expense e : expenses) {
            map.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
        }
        return map;
    }
}
