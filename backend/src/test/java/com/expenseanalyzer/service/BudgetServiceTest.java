package com.expenseanalyzer.service;

import com.expenseanalyzer.dto.BudgetRequestDTO;
import com.expenseanalyzer.dto.BudgetStatusDTO;
import com.expenseanalyzer.exception.DuplicateResourceException;
import com.expenseanalyzer.exception.ResourceNotFoundException;
import com.expenseanalyzer.model.Budget;
import com.expenseanalyzer.model.BudgetStatus;
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
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private BudgetService budgetService;

    // -------------------------------------------------------------------------
    // 1. Budget Utilization Threshold Boundaries
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Utilization thresholds: < 80.00% WITHIN_BUDGET, 80.00-100.00% APPROACHING_LIMIT, > 100.00% EXCEEDED")
    void testBudgetStatusThresholdBoundaries() {
        // Zero spending -> WITHIN_BUDGET
        assertThat(BudgetService.calculateStatus(new BigDecimal("0.00")))
                .isEqualTo(BudgetStatus.WITHIN_BUDGET);

        // Strict boundary: 79.99% -> WITHIN_BUDGET
        assertThat(BudgetService.calculateStatus(new BigDecimal("79.99")))
                .isEqualTo(BudgetStatus.WITHIN_BUDGET);

        // Boundary: 80.00% exactly -> APPROACHING_LIMIT (inclusive lower bound)
        assertThat(BudgetService.calculateStatus(new BigDecimal("80.00")))
                .isEqualTo(BudgetStatus.APPROACHING_LIMIT);

        // Within approaching limit range: 90.00% -> APPROACHING_LIMIT
        assertThat(BudgetService.calculateStatus(new BigDecimal("90.00")))
                .isEqualTo(BudgetStatus.APPROACHING_LIMIT);

        // Boundary: 99.99% -> APPROACHING_LIMIT
        assertThat(BudgetService.calculateStatus(new BigDecimal("99.99")))
                .isEqualTo(BudgetStatus.APPROACHING_LIMIT);

        // Boundary: 100.00% exactly -> APPROACHING_LIMIT (inclusive upper bound)
        assertThat(BudgetService.calculateStatus(new BigDecimal("100.00")))
                .isEqualTo(BudgetStatus.APPROACHING_LIMIT);

        // Strict boundary: 100.01% -> EXCEEDED (strictly greater than 100)
        assertThat(BudgetService.calculateStatus(new BigDecimal("100.01")))
                .isEqualTo(BudgetStatus.EXCEEDED);

        // Substantially exceeded: 175.50% -> EXCEEDED
        assertThat(BudgetService.calculateStatus(new BigDecimal("175.50")))
                .isEqualTo(BudgetStatus.EXCEEDED);
    }

    // -------------------------------------------------------------------------
    // 2. Budget Computation & Monetary Precision (Under, Exact, Exceeded)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Compute budget status: under budget retains positive remaining amount and WITHIN_BUDGET status")
    void testComputeBudgetStatusUnderBudget() {
        Budget budget = new Budget(ExpenseCategory.FOOD, new BigDecimal("10000.00"), 11, 2026);
        budget.setId(1L);

        Expense e1 = new Expense("Dinner", new BigDecimal("4500.00"), LocalDate.of(2026, 11, 5),
                ExpenseCategory.FOOD, PaymentMethod.UPI, null);

        when(budgetRepository.findByBudgetMonthAndBudgetYear(11, 2026)).thenReturn(List.of(budget));
        when(expenseRepository.findByCategoryAndDateBetween(eq(ExpenseCategory.FOOD), any(), any()))
                .thenReturn(List.of(e1));

        List<BudgetStatusDTO> results = budgetService.getBudgetsForMonth("2026-11");
        assertThat(results).hasSize(1);

        BudgetStatusDTO dto = results.get(0);
        assertThat(dto.getMonthlyLimit()).isEqualByComparingTo("10000.00");
        assertThat(dto.getActualSpent()).isEqualByComparingTo("4500.00");
        assertThat(dto.getRemainingAmount()).isEqualByComparingTo("5500.00");
        assertThat(dto.getUtilizationPercent()).isEqualByComparingTo("45.00");
        assertThat(dto.getStatus()).isEqualTo(BudgetStatus.WITHIN_BUDGET);
    }

    @Test
    @DisplayName("Compute budget status: exactly at limit has 0.00 remaining, 100.00% utilization, and APPROACHING_LIMIT")
    void testComputeBudgetStatusExactlyAtLimit() {
        Budget budget = new Budget(ExpenseCategory.GROCERIES, new BigDecimal("5000.00"), 11, 2026);
        budget.setId(2L);

        Expense e1 = new Expense("Grocery 1", new BigDecimal("3000.00"), LocalDate.of(2026, 11, 5),
                ExpenseCategory.GROCERIES, PaymentMethod.UPI, null);
        Expense e2 = new Expense("Grocery 2", new BigDecimal("2000.00"), LocalDate.of(2026, 11, 10),
                ExpenseCategory.GROCERIES, PaymentMethod.UPI, null);

        when(budgetRepository.findByBudgetMonthAndBudgetYear(11, 2026)).thenReturn(List.of(budget));
        when(expenseRepository.findByCategoryAndDateBetween(eq(ExpenseCategory.GROCERIES), any(), any()))
                .thenReturn(List.of(e1, e2));

        List<BudgetStatusDTO> results = budgetService.getBudgetsForMonth("2026-11");
        assertThat(results).hasSize(1);

        BudgetStatusDTO dto = results.get(0);
        assertThat(dto.getMonthlyLimit()).isEqualByComparingTo("5000.00");
        assertThat(dto.getActualSpent()).isEqualByComparingTo("5000.00");
        assertThat(dto.getRemainingAmount()).isEqualByComparingTo("0.00");
        assertThat(dto.getUtilizationPercent()).isEqualByComparingTo("100.00");
        assertThat(dto.getStatus()).isEqualTo(BudgetStatus.APPROACHING_LIMIT);
    }

    @Test
    @DisplayName("Compute budget status: exceeded budget has negative remaining amount and EXCEEDED status")
    void testComputeBudgetStatusExceeded() {
        Budget budget = new Budget(ExpenseCategory.SHOPPING, new BigDecimal("4000.00"), 11, 2026);
        budget.setId(3L);

        Expense e1 = new Expense("Clothes", new BigDecimal("5200.00"), LocalDate.of(2026, 11, 12),
                ExpenseCategory.SHOPPING, PaymentMethod.CARD, null);

        when(budgetRepository.findByBudgetMonthAndBudgetYear(11, 2026)).thenReturn(List.of(budget));
        when(expenseRepository.findByCategoryAndDateBetween(eq(ExpenseCategory.SHOPPING), any(), any()))
                .thenReturn(List.of(e1));

        List<BudgetStatusDTO> results = budgetService.getBudgetsForMonth("2026-11");
        assertThat(results).hasSize(1);

        BudgetStatusDTO dto = results.get(0);
        assertThat(dto.getMonthlyLimit()).isEqualByComparingTo("4000.00");
        assertThat(dto.getActualSpent()).isEqualByComparingTo("5200.00");
        assertThat(dto.getRemainingAmount()).isEqualByComparingTo("-1200.00"); // Negative when exceeded
        assertThat(dto.getUtilizationPercent()).isEqualByComparingTo("130.00");
        assertThat(dto.getStatus()).isEqualTo(BudgetStatus.EXCEEDED);
    }

    // -------------------------------------------------------------------------
    // 3. Duplicate Prevention & Validation Handling
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Duplicate budget: throws DuplicateResourceException on colliding category, month and year")
    void testDuplicateBudgetThrowsException() {
        BudgetRequestDTO request = new BudgetRequestDTO(
                ExpenseCategory.FOOD, new BigDecimal("5000.00"), 11, 2026);

        when(budgetRepository.findByCategoryAndBudgetMonthAndBudgetYear(
                ExpenseCategory.FOOD, 11, 2026))
                .thenReturn(Optional.of(new Budget()));

        assertThatThrownBy(() -> budgetService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Delete missing budget: throws ResourceNotFoundException on non-existent ID")
    void testDeleteMissingBudgetThrowsException() {
        when(budgetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Update missing budget: throws ResourceNotFoundException on non-existent ID")
    void testUpdateMissingBudgetThrowsException() {
        BudgetRequestDTO request = new BudgetRequestDTO(
                ExpenseCategory.FOOD, new BigDecimal("5000.00"), 11, 2026);
        when(budgetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.update(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
