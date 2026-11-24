package com.expenseanalyzer.service;

import com.expenseanalyzer.dto.BudgetRequestDTO;
import com.expenseanalyzer.dto.BudgetStatusDTO;
import com.expenseanalyzer.exception.DuplicateResourceException;
import com.expenseanalyzer.exception.ResourceNotFoundException;
import com.expenseanalyzer.model.Budget;
import com.expenseanalyzer.model.BudgetStatus;
import com.expenseanalyzer.model.Expense;
import com.expenseanalyzer.model.ExpenseCategory;
import com.expenseanalyzer.repository.BudgetRepository;
import com.expenseanalyzer.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service managing category budget CRUD and utilization calculations (spec §4.2 A).
 * <p>
 * Thresholds applied on utilization percentage rounded to 2 decimal places (HALF_UP):
 * <ul>
 *   <li>&lt; 80.00%  &rarr; WITHIN_BUDGET</li>
 *   <li>80.00% to 100.00% inclusive &rarr; APPROACHING_LIMIT</li>
 *   <li>&gt; 100.00% &rarr; EXCEEDED</li>
 * </ul>
 * </p>
 */
@Service
public class BudgetService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal EIGHTY = new BigDecimal("80.00");
    private static final BigDecimal HUNDRED_POINT_ZERO = new BigDecimal("100.00");
    private static final BigDecimal ZERO_SCALE_2 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;

    public BudgetService(BudgetRepository budgetRepository,
                         ExpenseRepository expenseRepository) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
    }

    /**
     * Creates a new monthly budget limit for a category.
     * Enforces the unique constraint on (category, budgetMonth, budgetYear).
     *
     * @throws DuplicateResourceException if a budget for that category and month already exists
     */
    @Transactional
    public BudgetStatusDTO create(BudgetRequestDTO request) {
        Optional<Budget> existing = budgetRepository.findByCategoryAndBudgetMonthAndBudgetYear(
                request.getCategory(), request.getBudgetMonth(), request.getBudgetYear());

        if (existing.isPresent()) {
            throw new DuplicateResourceException(String.format(
                    "A budget for category '%s' in %02d/%d already exists.",
                    request.getCategory().getDisplayLabel(),
                    request.getBudgetMonth(),
                    request.getBudgetYear()
            ));
        }

        BigDecimal scaledLimit = request.getMonthlyLimit().setScale(2, RoundingMode.HALF_UP);

        Budget budget = new Budget(
                request.getCategory(),
                scaledLimit,
                request.getBudgetMonth(),
                request.getBudgetYear()
        );

        Budget saved = budgetRepository.save(budget);
        return computeBudgetStatus(saved);
    }

    /**
     * Updates an existing budget's limit or period.
     * Checks for unique constraint violations if the category or period changed.
     *
     * @throws ResourceNotFoundException if no budget exists with the given id
     * @throws DuplicateResourceException if updated fields collide with another budget
     */
    @Transactional
    public BudgetStatusDTO update(Long id, BudgetRequestDTO request) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));

        boolean periodOrCategoryChanged = budget.getCategory() != request.getCategory()
                || !Objects.equals(budget.getBudgetMonth(), request.getBudgetMonth())
                || !Objects.equals(budget.getBudgetYear(), request.getBudgetYear());

        if (periodOrCategoryChanged) {
            Optional<Budget> existing = budgetRepository.findByCategoryAndBudgetMonthAndBudgetYear(
                    request.getCategory(), request.getBudgetMonth(), request.getBudgetYear());

            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new DuplicateResourceException(String.format(
                        "A budget for category '%s' in %02d/%d already exists.",
                        request.getCategory().getDisplayLabel(),
                        request.getBudgetMonth(),
                        request.getBudgetYear()
                ));
            }
        }

        budget.setCategory(request.getCategory());
        budget.setMonthlyLimit(request.getMonthlyLimit().setScale(2, RoundingMode.HALF_UP));
        budget.setBudgetMonth(request.getBudgetMonth());
        budget.setBudgetYear(request.getBudgetYear());

        Budget updated = budgetRepository.save(budget);
        return computeBudgetStatus(updated);
    }

    /**
     * Deletes a budget by its primary key id.
     * Associated expenses remain intact.
     *
     * @throws ResourceNotFoundException if no budget exists with the given id
     */
    @Transactional
    public void delete(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));
        budgetRepository.delete(budget);
    }

    /**
     * Returns budget status by primary key id.
     *
     * @throws ResourceNotFoundException if no budget exists with the given id
     */
    @Transactional(readOnly = true)
    public BudgetStatusDTO findById(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id: " + id));
        return computeBudgetStatus(budget);
    }

    /**
     * Retrieves all budgets and their computed statuses for a given month (YYYY-MM).
     * Defaults to the current calendar month if monthStr is null or blank.
     */
    @Transactional(readOnly = true)
    public List<BudgetStatusDTO> getBudgetsForMonth(String monthStr) {
        YearMonth target = (monthStr != null && !monthStr.isBlank())
                ? YearMonth.parse(monthStr.trim())
                : YearMonth.now();

        List<Budget> budgets = budgetRepository.findByBudgetMonthAndBudgetYear(
                target.getMonthValue(), target.getYear());

        return budgets.stream()
                .map(this::computeBudgetStatus)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Helper calculation
    // -------------------------------------------------------------------------

    private BudgetStatusDTO computeBudgetStatus(Budget budget) {
        YearMonth ym = YearMonth.of(budget.getBudgetYear(), budget.getBudgetMonth());
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<Expense> expenses = expenseRepository.findByCategoryAndDateBetween(
                budget.getCategory(), startDate, endDate);

        BigDecimal actualSpent = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal monthlyLimit = budget.getMonthlyLimit().setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingAmount = monthlyLimit.subtract(actualSpent).setScale(2, RoundingMode.HALF_UP);

        BigDecimal utilizationPercent = ZERO_SCALE_2;
        if (monthlyLimit.compareTo(BigDecimal.ZERO) > 0) {
            utilizationPercent = actualSpent.multiply(HUNDRED)
                    .divide(monthlyLimit, 2, RoundingMode.HALF_UP);
        }

        BudgetStatus status = calculateStatus(utilizationPercent);

        return new BudgetStatusDTO(
                budget.getId(),
                budget.getCategory(),
                budget.getCategory().getDisplayLabel(),
                monthlyLimit,
                actualSpent,
                remainingAmount,
                utilizationPercent,
                status,
                status.getDisplayLabel(),
                budget.getBudgetMonth(),
                budget.getBudgetYear()
        );
    }

    /**
     * Evaluates budget status according to master specification thresholds:
     * - &lt; 80.00% &rarr; WITHIN_BUDGET
     * - 80.00% to 100.00% inclusive &rarr; APPROACHING_LIMIT
     * - &gt; 100.00% &rarr; EXCEEDED
     */
    public static BudgetStatus calculateStatus(BigDecimal utilizationPercent) {
        if (utilizationPercent.compareTo(EIGHTY) < 0) {
            return BudgetStatus.WITHIN_BUDGET;
        } else if (utilizationPercent.compareTo(HUNDRED_POINT_ZERO) <= 0) {
            return BudgetStatus.APPROACHING_LIMIT;
        } else {
            return BudgetStatus.EXCEEDED;
        }
    }
}
