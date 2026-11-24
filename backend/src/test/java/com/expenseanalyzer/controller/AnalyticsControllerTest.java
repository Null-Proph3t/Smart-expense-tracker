package com.expenseanalyzer.controller;

import com.expenseanalyzer.dto.AnalyticsSummaryDTO;
import com.expenseanalyzer.dto.CategorySummaryDTO;
import com.expenseanalyzer.dto.HighestLowestResponseDTO;
import com.expenseanalyzer.dto.InsightsResponseDTO;
import com.expenseanalyzer.dto.MonthlyReportDTO;
import com.expenseanalyzer.exception.GlobalExceptionHandler;
import com.expenseanalyzer.model.ExpenseCategory;
import com.expenseanalyzer.service.AnalyticsService;
import com.expenseanalyzer.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private ReportService reportService;

    @InjectMocks
    private AnalyticsController analyticsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(analyticsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/analytics/summary returns 200 OK with KPIs")
    void testGetSummary() throws Exception {
        AnalyticsSummaryDTO summary = new AnalyticsSummaryDTO(
                new BigDecimal("25000.00"),
                new BigDecimal("8500.00"),
                new BigDecimal("2999.00"),
                new BigDecimal("806.45")
        );

        when(analyticsService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses").value(25000.00))
                .andExpect(jsonPath("$.thisMonthSpending").value(8500.00))
                .andExpect(jsonPath("$.highestExpense").value(2999.00))
                .andExpect(jsonPath("$.averageTransaction").value(806.45));
    }

    @Test
    @DisplayName("GET /api/analytics/categories returns 200 OK with category breakdown")
    void testGetCategories() throws Exception {
        CategorySummaryDTO cat = new CategorySummaryDTO(
                ExpenseCategory.FOOD, "Food", new BigDecimal("5000.00"), 10,
                new BigDecimal("500.00"), new BigDecimal("20.00")
        );

        when(analyticsService.getCategorySummaries()).thenReturn(List.of(cat));

        mockMvc.perform(get("/api/analytics/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category").value("FOOD"))
                .andExpect(jsonPath("$[0].categoryLabel").value("Food"))
                .andExpect(jsonPath("$[0].totalAmount").value(5000.00))
                .andExpect(jsonPath("$[0].percentage").value(20.00));
    }

    @Test
    @DisplayName("GET /api/analytics/highest-lowest returns 200 OK with extrema")
    void testGetHighestLowest() throws Exception {
        HighestLowestResponseDTO hl = new HighestLowestResponseDTO(
                null, null, new BigDecimal("450.00"), Collections.emptyList()
        );

        when(analyticsService.getHighestLowest()).thenReturn(hl);

        mockMvc.perform(get("/api/analytics/highest-lowest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(450.00));
    }

    @Test
    @DisplayName("GET /api/analytics/monthly returns 200 OK with monthly report")
    void testGetMonthlyReport() throws Exception {
        MonthlyReportDTO report = new MonthlyReportDTO(
                "2026-11", new BigDecimal("7500.00"), 8, new BigDecimal("937.50"),
                null, new BigDecimal("15.50"), new BigDecimal("312.50"),
                Collections.emptyList(), Collections.emptyList()
        );

        when(reportService.getMonthlyReport("2026-11")).thenReturn(report);

        mockMvc.perform(get("/api/analytics/monthly").param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-11"))
                .andExpect(jsonPath("$.totalExpenditure").value(7500.00))
                .andExpect(jsonPath("$.momChangePercent").value(15.50))
                .andExpect(jsonPath("$.averageDailyExpenditure").value(312.50));
    }

    @Test
    @DisplayName("GET /api/analytics/insights returns 200 OK with insights list")
    void testGetInsights() throws Exception {
        InsightsResponseDTO insights = new InsightsResponseDTO(
                List.of("Food is your highest spending category."),
                Collections.emptyList()
        );

        when(analyticsService.getInsights()).thenReturn(insights);

        mockMvc.perform(get("/api/analytics/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insights", hasSize(1)))
                .andExpect(jsonPath("$.insights[0]").value("Food is your highest spending category."));
    }

    @Test
    @DisplayName("GET /api/analytics/unusual returns 200 OK with unusual transactions list")
    void testGetUnusualExpenses() throws Exception {
        when(analyticsService.getUnusualExpenses()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/analytics/unusual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
