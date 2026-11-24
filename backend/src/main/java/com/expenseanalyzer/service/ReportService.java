package com.expenseanalyzer.service;

import com.expenseanalyzer.dto.CategorySummaryDTO;
import com.expenseanalyzer.dto.ExpenseHighlightDTO;
import com.expenseanalyzer.dto.MonthlyReportDTO;
import com.expenseanalyzer.dto.MonthlyTrendPointDTO;
import com.expenseanalyzer.model.Expense;
import com.expenseanalyzer.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service for computing monthly expenditure summaries, MoM trends,
 * average daily expenditures, and historical trends (spec §4.1 Module F).
 */
@Service
public class ReportService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO_SCALE_2 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final ExpenseRepository expenseRepository;
    private final AnalyticsService analyticsService;

    public ReportService(ExpenseRepository expenseRepository,
                         AnalyticsService analyticsService) {
        this.expenseRepository = expenseRepository;
        this.analyticsService = analyticsService;
    }

    /**
     * Generates a monthly spending report for the specified month (or current month if null/blank).
     *
     * @param monthStr month in "YYYY-MM" format (nullable)
     * @return MonthlyReportDTO
     */
    @Transactional(readOnly = true)
    public MonthlyReportDTO getMonthlyReport(String monthStr) {
        YearMonth requestedMonth = (monthStr != null && !monthStr.isBlank())
                ? YearMonth.parse(monthStr.trim())
                : YearMonth.now();

        LocalDate startDate = requestedMonth.atDay(1);
        LocalDate endDate = requestedMonth.atEndOfMonth();

        List<Expense> monthExpenses = expenseRepository.findByDateBetween(startDate, endDate);

        BigDecimal totalExpenditure = monthExpenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        long transactionCount = monthExpenses.size();

        BigDecimal averageTransaction = transactionCount > 0
                ? totalExpenditure.divide(BigDecimal.valueOf(transactionCount), 2, RoundingMode.HALF_UP)
                : ZERO_SCALE_2;

        Expense highestExpenseEntity = monthExpenses.stream()
                .max(Comparator.comparing(Expense::getAmount))
                .orElse(null);

        ExpenseHighlightDTO highestTransaction = ExpenseHighlightDTO.fromEntity(highestExpenseEntity);

        // 1. Month-over-Month (MoM) calculation vs immediately preceding calendar month
        YearMonth prevMonth = requestedMonth.minusMonths(1);
        List<Expense> prevExpenses = expenseRepository.findByDateBetween(
                prevMonth.atDay(1), prevMonth.atEndOfMonth());

        BigDecimal prevTotal = prevExpenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // Zero-Denominator Guard: return null if previous month's expenditure is 0
        BigDecimal momChangePercent = null;
        if (prevTotal.compareTo(BigDecimal.ZERO) > 0) {
            momChangePercent = totalExpenditure.subtract(prevTotal)
                    .multiply(HUNDRED)
                    .divide(prevTotal, 2, RoundingMode.HALF_UP);
        }

        // 2. Average Daily Expenditure
        // Ongoing current month -> divide by LocalDate.now().getDayOfMonth()
        // Past completed month -> divide by requestedMonth.lengthOfMonth()
        int divisor;
        if (requestedMonth.equals(YearMonth.now())) {
            divisor = LocalDate.now().getDayOfMonth();
        } else {
            divisor = requestedMonth.lengthOfMonth();
        }

        BigDecimal averageDailyExpenditure = totalExpenditure.divide(
                BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);

        // 3. Category breakdown for this month
        List<CategorySummaryDTO> categoryBreakdown =
                analyticsService.computeCategorySummariesForList(monthExpenses);

        // 4. Historical monthly trend (past 6 calendar months up to and including requestedMonth)
        List<MonthlyTrendPointDTO> historicalTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = requestedMonth.minusMonths(i);
            List<Expense> ymExpenses = expenseRepository.findByDateBetween(
                    ym.atDay(1), ym.atEndOfMonth());

            BigDecimal ymTotal = ymExpenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            historicalTrend.add(new MonthlyTrendPointDTO(ym.toString(), ymTotal));
        }

        return new MonthlyReportDTO(
                requestedMonth.toString(),
                totalExpenditure,
                transactionCount,
                averageTransaction,
                highestTransaction,
                momChangePercent,
                averageDailyExpenditure,
                categoryBreakdown,
                historicalTrend
        );
    }
}
