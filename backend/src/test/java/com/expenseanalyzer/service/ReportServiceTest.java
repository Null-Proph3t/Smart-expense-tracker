package com.expenseanalyzer.service;

import com.expenseanalyzer.dto.MonthlyReportDTO;
import com.expenseanalyzer.model.Expense;
import com.expenseanalyzer.model.ExpenseCategory;
import com.expenseanalyzer.model.PaymentMethod;
import com.expenseanalyzer.repository.ExpenseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private ReportService reportService;

    // -------------------------------------------------------------------------
    // 1. Zero-Denominator Guard (Month-over-Month calculation)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Zero-denominator guard: previous month total 0 yields strictly null for momChangePercent")
    void testZeroDenominatorGuardMoM() {
        // Querying month "2026-10" where prev month "2026-09" had 0 expenses
        LocalDate octStart = LocalDate.of(2026, 10, 1);
        LocalDate octEnd = LocalDate.of(2026, 10, 31);
        LocalDate sepStart = LocalDate.of(2026, 9, 1);
        LocalDate sepEnd = LocalDate.of(2026, 9, 30);

        Expense e1 = new Expense("Oct Expense", new BigDecimal("1500.00"), LocalDate.of(2026, 10, 15),
                ExpenseCategory.FOOD, PaymentMethod.UPI, null);

        when(expenseRepository.findByDateBetween(octStart, octEnd)).thenReturn(List.of(e1));
        when(expenseRepository.findByDateBetween(sepStart, sepEnd)).thenReturn(Collections.emptyList());
        when(analyticsService.computeCategorySummariesForList(any())).thenReturn(Collections.emptyList());

        MonthlyReportDTO report = reportService.getMonthlyReport("2026-10");

        assertThat(report.getTotalExpenditure()).isEqualByComparingTo("1500.00");
        assertThat(report.getTransactionCount()).isEqualTo(1);
        assertThat(report.getMomChangePercent()).isNull(); // Strictly null, not NaN or 0.00
    }

    @Test
    @DisplayName("MoM percentage calculation: positive increase and negative decrease")
    void testMoMCalculationWithPreviousMonth() {
        LocalDate octStart = LocalDate.of(2026, 10, 1);
        LocalDate octEnd = LocalDate.of(2026, 10, 31);
        LocalDate sepStart = LocalDate.of(2026, 9, 1);
        LocalDate sepEnd = LocalDate.of(2026, 9, 30);

        // Case A: September total = 1000.00, October total = 1500.00 -> Change = +50.00%
        Expense sepExp = new Expense("Sep", new BigDecimal("1000.00"), LocalDate.of(2026, 9, 15),
                ExpenseCategory.FOOD, PaymentMethod.UPI, null);
        Expense octExp = new Expense("Oct", new BigDecimal("1500.00"), LocalDate.of(2026, 10, 15),
                ExpenseCategory.FOOD, PaymentMethod.UPI, null);

        when(expenseRepository.findByDateBetween(octStart, octEnd)).thenReturn(List.of(octExp));
        when(expenseRepository.findByDateBetween(sepStart, sepEnd)).thenReturn(List.of(sepExp));
        when(analyticsService.computeCategorySummariesForList(any())).thenReturn(Collections.emptyList());

        MonthlyReportDTO report = reportService.getMonthlyReport("2026-10");
        assertThat(report.getMomChangePercent()).isEqualByComparingTo("50.00");

        // Case B: Decrease from 1000.00 to 600.00 -> Change = -40.00%
        Expense octExp2 = new Expense("Oct Low", new BigDecimal("600.00"), LocalDate.of(2026, 10, 15),
                ExpenseCategory.FOOD, PaymentMethod.UPI, null);
        when(expenseRepository.findByDateBetween(octStart, octEnd)).thenReturn(List.of(octExp2));

        MonthlyReportDTO reportDecreased = reportService.getMonthlyReport("2026-10");
        assertThat(reportDecreased.getMomChangePercent()).isEqualByComparingTo("-40.00");

        // Case C: Complete drop to 0.00 -> Change = -100.00%
        when(expenseRepository.findByDateBetween(octStart, octEnd)).thenReturn(Collections.emptyList());
        MonthlyReportDTO reportZero = reportService.getMonthlyReport("2026-10");
        assertThat(reportZero.getMomChangePercent()).isEqualByComparingTo("-100.00");
    }

    // -------------------------------------------------------------------------
    // 2. Average Daily Expenditure (Past Month vs Current Month vs Leap Year)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Daily average calculation: past month with 31 days divides total by 31")
    void testDailyAverageCalculationPastMonth31Days() {
        // October 2026 has 31 days. Total = 3100.00 -> Daily average = 3100 / 31 = 100.00
        LocalDate octStart = LocalDate.of(2026, 10, 1);
        LocalDate octEnd = LocalDate.of(2026, 10, 31);

        Expense e1 = new Expense("Oct Expense", new BigDecimal("3100.00"), LocalDate.of(2026, 10, 15),
                ExpenseCategory.FOOD, PaymentMethod.UPI, null);

        when(expenseRepository.findByDateBetween(octStart, octEnd)).thenReturn(List.of(e1));
        when(analyticsService.computeCategorySummariesForList(any())).thenReturn(Collections.emptyList());

        MonthlyReportDTO report = reportService.getMonthlyReport("2026-10");
        assertThat(report.getAverageDailyExpenditure()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Daily average calculation: February in leap year divides by 29, non-leap divides by 28")
    void testDailyAverageFebruaryLeapYear() {
        // February 2024 (Leap Year) has 29 days. Total = 2900.00 -> daily average = 100.00
        LocalDate feb2024Start = LocalDate.of(2024, 2, 1);
        LocalDate feb2024End = LocalDate.of(2024, 2, 29);

        Expense e1 = new Expense("Feb 2024", new BigDecimal("2900.00"), LocalDate.of(2024, 2, 10),
                ExpenseCategory.FOOD, PaymentMethod.UPI, null);

        when(expenseRepository.findByDateBetween(feb2024Start, feb2024End)).thenReturn(List.of(e1));
        when(analyticsService.computeCategorySummariesForList(any())).thenReturn(Collections.emptyList());

        MonthlyReportDTO leapReport = reportService.getMonthlyReport("2024-02");
        assertThat(leapReport.getAverageDailyExpenditure()).isEqualByComparingTo("100.00");

        // February 2025 (Non-Leap Year) has 28 days. Total = 2800.00 -> daily average = 100.00
        LocalDate feb2025Start = LocalDate.of(2025, 2, 1);
        LocalDate feb2025End = LocalDate.of(2025, 2, 28);

        Expense e2 = new Expense("Feb 2025", new BigDecimal("2800.00"), LocalDate.of(2025, 2, 10),
                ExpenseCategory.FOOD, PaymentMethod.UPI, null);

        when(expenseRepository.findByDateBetween(feb2025Start, feb2025End)).thenReturn(List.of(e2));

        MonthlyReportDTO nonLeapReport = reportService.getMonthlyReport("2025-02");
        assertThat(nonLeapReport.getAverageDailyExpenditure()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Daily average calculation: current ongoing month divides by current day of month")
    void testDailyAverageCurrentOngoingMonth() {
        YearMonth currentYm = YearMonth.now();
        int currentDay = LocalDate.now().getDayOfMonth();

        // Total = currentDay * 200.00. Expected daily avg = 200.00
        BigDecimal totalSpent = BigDecimal.valueOf(currentDay).multiply(new BigDecimal("200.00"));
        Expense e = new Expense("Current Month Expense", totalSpent, LocalDate.now(),
                ExpenseCategory.FOOD, PaymentMethod.UPI, null);

        when(expenseRepository.findByDateBetween(currentYm.atDay(1), currentYm.atEndOfMonth()))
                .thenReturn(List.of(e));
        when(analyticsService.computeCategorySummariesForList(any())).thenReturn(Collections.emptyList());

        MonthlyReportDTO report = reportService.getMonthlyReport(currentYm.toString());
        assertThat(report.getAverageDailyExpenditure()).isEqualByComparingTo("200.00");
    }

    // -------------------------------------------------------------------------
    // 3. Historical 6-Month Trend & Empty Month Handling
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Historical monthly trend: computes exactly 6 chronological data points")
    void testHistoricalTrendGenerates6Points() {
        when(expenseRepository.findByDateBetween(any(), any())).thenReturn(Collections.emptyList());
        when(analyticsService.computeCategorySummariesForList(any())).thenReturn(Collections.emptyList());

        MonthlyReportDTO report = reportService.getMonthlyReport("2026-11");

        assertThat(report.getHistoricalTrend()).hasSize(6);
        assertThat(report.getHistoricalTrend().get(0).getMonth()).isEqualTo("2026-06");
        assertThat(report.getHistoricalTrend().get(5).getMonth()).isEqualTo("2026-11");
        for (var point : report.getHistoricalTrend()) {
            assertThat(point.getTotal()).isEqualByComparingTo("0.00");
        }
    }

    @Test
    @DisplayName("Empty month safety: returns zero totals, null highest transaction, null MoM")
    void testEmptyMonthSafety() {
        when(expenseRepository.findByDateBetween(any(), any())).thenReturn(Collections.emptyList());
        when(analyticsService.computeCategorySummariesForList(any())).thenReturn(Collections.emptyList());

        MonthlyReportDTO report = reportService.getMonthlyReport("2026-01");

        assertThat(report.getTotalExpenditure()).isEqualByComparingTo("0.00");
        assertThat(report.getTransactionCount()).isZero();
        assertThat(report.getAverageTransaction()).isEqualByComparingTo("0.00");
        assertThat(report.getHighestTransaction()).isNull();
        assertThat(report.getMomChangePercent()).isNull();
        assertThat(report.getAverageDailyExpenditure()).isEqualByComparingTo("0.00");
        assertThat(report.getCategoryBreakdown()).isEmpty();
    }
}
