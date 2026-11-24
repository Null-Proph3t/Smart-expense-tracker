package com.expenseanalyzer.repository;

import com.expenseanalyzer.model.Expense;
import com.expenseanalyzer.model.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Expense} entities.
 * <p>
 * Complex filtering and sorting is performed in-memory inside
 * {@code ExpenseService} (Phase 3) rather than through repository-generated
 * query methods, as specified in ARCHITECTURE.md §6.1.
 * Query methods declared here are for aggregation only.
 * </p>
 */
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /**
     * Returns all expenses whose date falls within the given month.
     * Used by BudgetService to compute actual spending against a budget.
     *
     * @param category  the expense category to filter by
     * @param startDate first day of the target month (inclusive)
     * @param endDate   last day of the target month (inclusive)
     * @return matching expenses
     */
    @Query("SELECT e FROM Expense e " +
           "WHERE e.category = :category " +
           "AND e.date >= :startDate " +
           "AND e.date <= :endDate")
    List<Expense> findByCategoryAndDateBetween(
            @Param("category") ExpenseCategory category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Returns all expenses whose date falls between the given bounds (inclusive).
     * Used by ReportService for monthly aggregations.
     */
    @Query("SELECT e FROM Expense e WHERE e.date >= :startDate AND e.date <= :endDate")
    List<Expense> findByDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
