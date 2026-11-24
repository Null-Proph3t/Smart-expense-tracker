package com.expenseanalyzer.controller;

import com.expenseanalyzer.dto.AnalyticsSummaryDTO;
import com.expenseanalyzer.dto.CategorySummaryDTO;
import com.expenseanalyzer.dto.HighestLowestResponseDTO;
import com.expenseanalyzer.dto.InsightsResponseDTO;
import com.expenseanalyzer.dto.MonthlyReportDTO;
import com.expenseanalyzer.service.AnalyticsService;
import com.expenseanalyzer.service.ReportService;
import com.expenseanalyzer.dto.UnusualExpenseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for analytics, metrics, insights, and monthly reports (spec §5.2).
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ReportService reportService;

    public AnalyticsController(AnalyticsService analyticsService,
                               ReportService reportService) {
        this.analyticsService = analyticsService;
        this.reportService = reportService;
    }

    /**
     * GET /api/analytics/summary
     * Returns overall & current-month KPI metrics.
     */
    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryDTO> getSummary() {
        return ResponseEntity.ok(analyticsService.getSummary());
    }

    /**
     * GET /api/analytics/categories
     * Returns category-wise totals, transaction counts, averages, and percentage shares.
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategorySummaryDTO>> getCategorySummaries() {
        return ResponseEntity.ok(analyticsService.getCategorySummaries());
    }

    /**
     * GET /api/analytics/highest-lowest
     * Returns highest individual expense, lowest, overall average, and top 5 expenses.
     */
    @GetMapping("/highest-lowest")
    public ResponseEntity<HighestLowestResponseDTO> getHighestLowest() {
        return ResponseEntity.ok(analyticsService.getHighestLowest());
    }

    /**
     * GET /api/analytics/monthly?month=YYYY-MM
     * GET /api/analytics/monthly-report?month=YYYY-MM
     * Returns monthly breakdown, MoM change, daily average, and historical trend series.
     */
    @GetMapping({"/monthly", "/monthly-report"})
    public ResponseEntity<MonthlyReportDTO> getMonthlyReport(
            @RequestParam(required = false) String month) {
        return ResponseEntity.ok(reportService.getMonthlyReport(month));
    }

    /**
     * GET /api/analytics/unusual
     * Returns expenses exceeding 2x category average (categories with >= 2 transactions).
     */
    @GetMapping("/unusual")
    public ResponseEntity<List<UnusualExpenseDTO>> getUnusualExpenses() {
        return ResponseEntity.ok(analyticsService.getUnusualExpenses());
    }

    /**
     * GET /api/analytics/insights
     * Returns automated rule-based spending insights and unusual spending alerts.
     */
    @GetMapping("/insights")
    public ResponseEntity<InsightsResponseDTO> getInsights() {
        return ResponseEntity.ok(analyticsService.getInsights());
    }
}
