package com.expenseanalyzer;

import com.expenseanalyzer.config.DataSeeder;
import com.expenseanalyzer.model.Expense;
import com.expenseanalyzer.model.ExpenseCategory;
import com.expenseanalyzer.model.PaymentMethod;
import com.expenseanalyzer.repository.BudgetRepository;
import com.expenseanalyzer.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SmartExpenseAnalyzerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private DataSeeder dataSeeder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("DataSeeder populates >=25 INR expenses across all 9 categories, all 4 payment methods, and is idempotent")
    void testDataSeederCompletenessAndIdempotency() {
        List<Expense> expenses = expenseRepository.findAll();
        assertThat(expenses.size()).isGreaterThanOrEqualTo(25);

        Set<ExpenseCategory> categories = expenses.stream()
                .map(Expense::getCategory)
                .collect(Collectors.toSet());
        assertThat(categories).containsExactlyInAnyOrder(ExpenseCategory.values());

        Set<PaymentMethod> methods = expenses.stream()
                .map(Expense::getPaymentMethod)
                .collect(Collectors.toSet());
        assertThat(methods).containsExactlyInAnyOrder(PaymentMethod.values());

        YearMonth current = YearMonth.now();
        assertThat(budgetRepository.findByBudgetMonthAndBudgetYear(
                current.getMonthValue(), current.getYear()).size()).isBetween(4, 5);

        long beforeExpenses = expenseRepository.count();
        long beforeBudgets = budgetRepository.count();

        // Running seeder again must be a strict no-op
        dataSeeder.run(new DefaultApplicationArguments());
        assertThat(expenseRepository.count()).isEqualTo(beforeExpenses);
        assertThat(budgetRepository.count()).isEqualTo(beforeBudgets);
    }

    @Test
    @DisplayName("Invalid month query parameters and invalid sortBy return HTTP 400 Bad Request")
    void testInvalidQueryParametersReturn400() throws Exception {
        mockMvc.perform(get("/api/expenses").param("month", "2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/analytics/monthly").param("month", "2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/budgets").param("month", "invalid-month"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/expenses").param("sortBy", "description"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("sortBy")));

        mockMvc.perform(get("/api/expenses").param("order", "random"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Missing routes return 404 Not Found and wrong HTTP methods return 405 Method Not Allowed")
    void testNotFoundAndMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/api/nonexistent-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(delete("/api/analytics/summary"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    @DisplayName("Budget request with null or missing month/year returns structured 400 fieldErrors and duplicate returns 409")
    void testBudgetNullFieldsAndDuplicateConflict() throws Exception {
        String missingPeriod = """
                {
                    "category": "HEALTHCARE",
                    "monthlyLimit": 3000.00,
                    "budgetMonth": null,
                    "budgetYear": null
                }
                """;

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingPeriod))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("budgetMonth")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("budgetYear")));

        // Seeded current month has FOOD budget; creating another FOOD budget for current month must return 409
        YearMonth current = YearMonth.now();
        String duplicateFood = """
                {
                    "category": "FOOD",
                    "monthlyLimit": 6000.00,
                    "budgetMonth": %d,
                    "budgetYear": %d
                }
                """.formatted(current.getMonthValue(), current.getYear());

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateFood))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Full expense lifecycle: POST auto-categorizes & scales to 2 decimals, PUT preserves category, DELETE cleans up")
    void testExpenseEndToEndLifecycle() throws Exception {
        String createPayload = """
                {
                    "description": "Amazon Grocery staples",
                    "amount": 123.4,
                    "date": "2026-11-24",
                    "paymentMethod": "CARD"
                }
                """;

        String responseJson = mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("GROCERIES"))
                .andExpect(jsonPath("$.amount").value(123.40))
                .andReturn().getResponse().getContentAsString();

        // Extract created ID
        long id = Long.parseLong(responseJson.replaceAll(".*\"id\":(\\d+).*", "$1"));

        // Edit description to include "uber", omit category -> must preserve GROCERIES
        String updatePayload = """
                {
                    "description": "Uber ride receipt update",
                    "amount": 250.5,
                    "date": "2026-11-24",
                    "category": null,
                    "paymentMethod": "CARD"
                }
                """;

        mockMvc.perform(put("/api/expenses/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("GROCERIES"))
                .andExpect(jsonPath("$.amount").value(250.50));

        mockMvc.perform(delete("/api/expenses/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/expenses/" + id))
                .andExpect(status().isNotFound());
    }
}
