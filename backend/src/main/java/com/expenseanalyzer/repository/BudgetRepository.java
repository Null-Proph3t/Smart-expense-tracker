package com.expenseanalyzer.repository;

import com.expenseanalyzer.model.Budget;
import com.expenseanalyzer.model.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Budget} entities.
 */
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    /**
     * Returns all budgets for a given month and year.
     *
     * @param budgetMonth 1–12
     * @param budgetYear  ≥ 2000
     * @return budgets for that month
     */
    List<Budget> findByBudgetMonthAndBudgetYear(int budgetMonth, int budgetYear);

    /**
     * Looks up the single budget for a category in a given month/year.
     * Used to enforce uniqueness checks in BudgetService (Phase 3).
     */
    Optional<Budget> findByCategoryAndBudgetMonthAndBudgetYear(
            ExpenseCategory category, int budgetMonth, int budgetYear);
}
