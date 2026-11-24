package com.expenseanalyzer.controller;

import com.expenseanalyzer.dto.ExpenseRequestDTO;
import com.expenseanalyzer.dto.ExpenseResponseDTO;
import com.expenseanalyzer.exception.GlobalExceptionHandler;
import com.expenseanalyzer.exception.ResourceNotFoundException;
import com.expenseanalyzer.model.ExpenseCategory;
import com.expenseanalyzer.model.PaymentMethod;
import com.expenseanalyzer.service.ExpenseService;
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
import java.time.LocalDate;
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
class ExpenseControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ExpenseService expenseService;

    @InjectMocks
    private ExpenseController expenseController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(expenseController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/expenses returns 200 OK with list of expenses")
    void testFindAll() throws Exception {
        ExpenseResponseDTO e1 = new ExpenseResponseDTO(1L, "Lunch", new BigDecimal("250.00"),
                LocalDate.of(2026, 11, 10), ExpenseCategory.FOOD, "Food",
                PaymentMethod.UPI, "UPI", "Office lunch");

        when(expenseService.findAll(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(e1));

        mockMvc.perform(get("/api/expenses")
                        .param("sortBy", "date")
                        .param("order", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Lunch"))
                .andExpect(jsonPath("$[0].amount").value(250.00))
                .andExpect(jsonPath("$[0].category").value("FOOD"))
                .andExpect(jsonPath("$[0].paymentMethod").value("UPI"));
    }

    @Test
    @DisplayName("GET /api/expenses/{id} returns 200 OK when found")
    void testFindById() throws Exception {
        ExpenseResponseDTO e1 = new ExpenseResponseDTO(1L, "Metro recharge", new BigDecimal("500.00"),
                LocalDate.of(2026, 11, 12), ExpenseCategory.TRANSPORTATION, "Transportation",
                PaymentMethod.CARD, "Card", null);

        when(expenseService.findById(1L)).thenReturn(e1);

        mockMvc.perform(get("/api/expenses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Metro recharge"))
                .andExpect(jsonPath("$.amount").value(500.00));
    }

    @Test
    @DisplayName("GET /api/expenses/{id} returns 404 Not Found when missing")
    void testFindByIdNotFound() throws Exception {
        when(expenseService.findById(999L))
                .thenThrow(new ResourceNotFoundException("Expense not found with id: 999"));

        mockMvc.perform(get("/api/expenses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Expense not found with id: 999"));
    }

    @Test
    @DisplayName("POST /api/expenses returns 201 Created with Location header")
    void testCreateExpense() throws Exception {
        ExpenseResponseDTO created = new ExpenseResponseDTO(5L, "Swiggy dinner", new BigDecimal("450.00"),
                LocalDate.of(2026, 11, 15), ExpenseCategory.FOOD, "Food",
                PaymentMethod.UPI, "UPI", null);

        when(expenseService.create(any(ExpenseRequestDTO.class))).thenReturn(created);

        String json = """
                {
                    "description": "Swiggy dinner",
                    "amount": 450.00,
                    "date": "2026-11-15",
                    "paymentMethod": "UPI"
                }
                """;

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/expenses/5")))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.category").value("FOOD"))
                .andExpect(jsonPath("$.amount").value(450.00));
    }

    @Test
    @DisplayName("POST /api/expenses returns 400 Bad Request on validation failures")
    void testCreateExpenseValidationFailure() throws Exception {
        String invalidJson = """
                {
                    "description": "",
                    "amount": 0.00,
                    "paymentMethod": null
                }
                """;

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("PUT /api/expenses/{id} returns 200 OK on update")
    void testUpdateExpense() throws Exception {
        ExpenseResponseDTO updated = new ExpenseResponseDTO(1L, "Dinner updated", new BigDecimal("550.00"),
                LocalDate.of(2026, 11, 15), ExpenseCategory.FOOD, "Food",
                PaymentMethod.CARD, "Card", "Updated notes");

        when(expenseService.update(eq(1L), any(ExpenseRequestDTO.class))).thenReturn(updated);

        String json = """
                {
                    "description": "Dinner updated",
                    "amount": 550.00,
                    "date": "2026-11-15",
                    "category": "FOOD",
                    "paymentMethod": "CARD",
                    "notes": "Updated notes"
                }
                """;

        mockMvc.perform(put("/api/expenses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(550.00))
                .andExpect(jsonPath("$.description").value("Dinner updated"));
    }

    @Test
    @DisplayName("DELETE /api/expenses/{id} returns 204 No Content")
    void testDeleteExpense() throws Exception {
        mockMvc.perform(delete("/api/expenses/1"))
                .andExpect(status().isNoContent());

        verify(expenseService).delete(1L);
    }
}
