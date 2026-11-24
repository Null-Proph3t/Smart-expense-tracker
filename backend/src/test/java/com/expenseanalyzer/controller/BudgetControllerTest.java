package com.expenseanalyzer.controller;

import com.expenseanalyzer.dto.BudgetRequestDTO;
import com.expenseanalyzer.dto.BudgetStatusDTO;
import com.expenseanalyzer.exception.DuplicateResourceException;
import com.expenseanalyzer.exception.GlobalExceptionHandler;
import com.expenseanalyzer.model.BudgetStatus;
import com.expenseanalyzer.model.ExpenseCategory;
import com.expenseanalyzer.service.BudgetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BudgetControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private BudgetController budgetController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(budgetController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/budgets returns 200 OK with list of budgets")
    void testGetBudgets() throws Exception {
        BudgetStatusDTO b1 = new BudgetStatusDTO(
                1L, ExpenseCategory.FOOD, "Food", new BigDecimal("5000.00"),
                new BigDecimal("3500.00"), new BigDecimal("1500.00"), new BigDecimal("70.00"),
                BudgetStatus.WITHIN_BUDGET, "Within Budget", 11, 2026
        );

        when(budgetService.getBudgetsForMonth("2026-11")).thenReturn(List.of(b1));

        mockMvc.perform(get("/api/budgets").param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category").value("FOOD"))
                .andExpect(jsonPath("$[0].monthlyLimit").value(5000.00))
                .andExpect(jsonPath("$[0].actualSpent").value(3500.00))
                .andExpect(jsonPath("$[0].status").value("WITHIN_BUDGET"));
    }

    @Test
    @DisplayName("POST /api/budgets returns 201 Created with Location header")
    void testCreateBudget() throws Exception {
        BudgetStatusDTO created = new BudgetStatusDTO(
                2L, ExpenseCategory.GROCERIES, "Groceries", new BigDecimal("8000.00"),
                BigDecimal.ZERO, new BigDecimal("8000.00"), BigDecimal.ZERO,
                BudgetStatus.WITHIN_BUDGET, "Within Budget", 11, 2026
        );

        when(budgetService.create(any(BudgetRequestDTO.class))).thenReturn(created);

        String json = """
                {
                    "category": "GROCERIES",
                    "monthlyLimit": 8000.00,
                    "budgetMonth": 11,
                    "budgetYear": 2026
                }
                """;

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/budgets/2")))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.monthlyLimit").value(8000.00));
    }

    @Test
    @DisplayName("POST /api/budgets duplicate returns 409 Conflict")
    void testCreateDuplicateBudgetReturnsConflict() throws Exception {
        when(budgetService.create(any(BudgetRequestDTO.class)))
                .thenThrow(new DuplicateResourceException("A budget for category 'Food' in 11/2026 already exists."));

        String json = """
                {
                    "category": "FOOD",
                    "monthlyLimit": 5000.00,
                    "budgetMonth": 11,
                    "budgetYear": 2026
                }
                """;

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/budgets with invalid values returns 400 Bad Request")
    void testCreateBudgetValidationFailure() throws Exception {
        String invalidJson = """
                {
                    "category": null,
                    "monthlyLimit": 0.00,
                    "budgetMonth": 13,
                    "budgetYear": 1999
                }
                """;

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("PUT /api/budgets/{id} returns 200 OK on update")
    void testUpdateBudget() throws Exception {
        BudgetStatusDTO updated = new BudgetStatusDTO(
                1L, ExpenseCategory.FOOD, "Food", new BigDecimal("6000.00"),
                new BigDecimal("3500.00"), new BigDecimal("2500.00"), new BigDecimal("58.33"),
                BudgetStatus.WITHIN_BUDGET, "Within Budget", 11, 2026
        );

        when(budgetService.update(eq(1L), any(BudgetRequestDTO.class))).thenReturn(updated);

        String json = """
                {
                    "category": "FOOD",
                    "monthlyLimit": 6000.00,
                    "budgetMonth": 11,
                    "budgetYear": 2026
                }
                """;

        mockMvc.perform(put("/api/budgets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.monthlyLimit").value(6000.00));
    }

    @Test
    @DisplayName("DELETE /api/budgets/{id} returns 204 No Content")
    void testDeleteBudget() throws Exception {
        mockMvc.perform(delete("/api/budgets/1"))
                .andExpect(status().isNoContent());

        verify(budgetService).delete(1L);
    }
}
