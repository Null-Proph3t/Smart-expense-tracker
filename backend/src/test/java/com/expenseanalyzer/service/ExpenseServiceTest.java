package com.expenseanalyzer.service;

import com.expenseanalyzer.dto.ExpenseRequestDTO;
import com.expenseanalyzer.dto.ExpenseResponseDTO;
import com.expenseanalyzer.exception.ResourceNotFoundException;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CategorizationService categorizationService;

    @InjectMocks
    private ExpenseService expenseService;

    @Test
    @DisplayName("Create expense with null category invokes CategorizationService")
    void testCreateAutoCategorize() {
        ExpenseRequestDTO request = new ExpenseRequestDTO(
                "Swiggy lunch", new BigDecimal("350.00"), LocalDate.now(), null, PaymentMethod.UPI, "notes"
        );

        when(categorizationService.categorize("Swiggy lunch")).thenReturn(ExpenseCategory.FOOD);
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
            Expense e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });

        ExpenseResponseDTO response = expenseService.create(request);
        assertThat(response.getCategory()).isEqualTo(ExpenseCategory.FOOD);
        verify(categorizationService).categorize("Swiggy lunch");
    }

    @Test
    @DisplayName("Update expense preserves category when request category is null")
    void testUpdatePreservesManualCategory() {
        Expense existing = new Expense("Uber cab", new BigDecimal("450.00"), LocalDate.now(),
                ExpenseCategory.TRANSPORTATION, PaymentMethod.UPI, "initial");
        existing.setId(1L);

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        // Update description to something containing food keywords, but category is null
        ExpenseRequestDTO updateRequest = new ExpenseRequestDTO(
                "Uber to restaurant for lunch", new BigDecimal("500.00"), LocalDate.now(),
                null, // Category null -> must preserve TRANSPORTATION
                PaymentMethod.CARD, "updated notes"
        );

        ExpenseResponseDTO updated = expenseService.update(1L, updateRequest);
        assertThat(updated.getCategory()).isEqualTo(ExpenseCategory.TRANSPORTATION);
        assertThat(updated.getDescription()).isEqualTo("Uber to restaurant for lunch");
    }

    @Test
    @DisplayName("P3-4: Combined filter and sort returns correct filtered/sorted subset")
    void testCombinedFilterAndSort() {
        Expense e1 = new Expense("Lunch", new BigDecimal("200.00"), LocalDate.of(2026, 11, 1), ExpenseCategory.FOOD, PaymentMethod.UPI, null);
        Expense e2 = new Expense("Dinner", new BigDecimal("500.00"), LocalDate.of(2026, 11, 2), ExpenseCategory.FOOD, PaymentMethod.CARD, null);
        Expense e3 = new Expense("Metro", new BigDecimal("50.00"), LocalDate.of(2026, 11, 3), ExpenseCategory.TRANSPORTATION, PaymentMethod.UPI, null);
        e1.setId(1L);
        e2.setId(2L);
        e3.setId(3L);

        when(expenseRepository.findAll()).thenReturn(List.of(e1, e2, e3));

        // Filter: Category FOOD, sort: amount desc
        List<ExpenseResponseDTO> result = expenseService.findAll(
                null, ExpenseCategory.FOOD, null, null, null, null, "amount", "desc"
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo("500.00");
        assertThat(result.get(1).getAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Missing expense throws ResourceNotFoundException")
    void testMissingExpenseThrowsException() {
        when(expenseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Invalid sortBy or order parameter throws IllegalArgumentException")
    void testInvalidSortParametersThrowIllegalArgumentException() {
        when(expenseRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> expenseService.findAll(null, null, null, null, null, null, "description", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sortBy");

        assertThatThrownBy(() -> expenseService.findAll(null, null, null, null, null, null, "date", "sideways"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("order");
    }

    @Test
    @DisplayName("Create expense normalizes BigDecimal scale to 2 decimal places")
    void testCreateNormalizesScale2() {
        ExpenseRequestDTO request = new ExpenseRequestDTO(
                "Coffee snack", new BigDecimal("123.4"), LocalDate.now(), null, PaymentMethod.CASH, null
        );

        when(categorizationService.categorize("Coffee snack")).thenReturn(ExpenseCategory.FOOD);
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
            Expense e = inv.getArgument(0);
            e.setId(20L);
            return e;
        });

        ExpenseResponseDTO response = expenseService.create(request);
        assertThat(response.getAmount()).isEqualByComparingTo("123.40");
        assertThat(response.getAmount().scale()).isEqualTo(2);
    }
}
