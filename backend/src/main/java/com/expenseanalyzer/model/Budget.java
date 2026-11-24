package com.expenseanalyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * JPA entity mapping to the {@code budgets} table.
 * <p>
 * A budget entry defines a monthly spending limit for a single expense category.
 * The unique constraint on {@code (category, budget_month, budget_year)} ensures
 * one budget per category per month.
 * </p>
 * <p>
 * Column names {@code budget_month} and {@code budget_year} are used deliberately
 * because {@code MONTH} and {@code YEAR} are H2 reserved words
 * (ARCHITECTURE.md §4.2).
 * </p>
 * <p>
 * No Lombok — all accessors are hand-written.
 * </p>
 */
@Entity
@Table(
    name = "budgets",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_budget_category_month_year",
            columnNames = {"category", "budget_month", "budget_year"}
        )
    }
)
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ExpenseCategory category;

    @NotNull(message = "Monthly limit is required")
    @DecimalMin(value = "0.01", message = "Monthly limit must be at least 0.01")
    @Column(name = "monthly_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyLimit;

    @NotNull(message = "Budget month is required")
    @Min(value = 1, message = "Budget month must be between 1 and 12")
    @Max(value = 12, message = "Budget month must be between 1 and 12")
    @Column(name = "budget_month", nullable = false)
    private Integer budgetMonth;

    @NotNull(message = "Budget year is required")
    @Min(value = 2000, message = "Budget year must be 2000 or later")
    @Column(name = "budget_year", nullable = false)
    private Integer budgetYear;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Budget() {
    }

    public Budget(ExpenseCategory category,
                  BigDecimal monthlyLimit,
                  Integer budgetMonth,
                  Integer budgetYear) {
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.budgetMonth = budgetMonth;
        this.budgetYear = budgetYear;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(BigDecimal monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public Integer getBudgetMonth() {
        return budgetMonth;
    }

    public void setBudgetMonth(Integer budgetMonth) {
        this.budgetMonth = budgetMonth;
    }

    public Integer getBudgetYear() {
        return budgetYear;
    }

    public void setBudgetYear(Integer budgetYear) {
        this.budgetYear = budgetYear;
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "Budget{" +
               "id=" + id +
               ", category=" + category +
               ", monthlyLimit=" + monthlyLimit +
               ", budgetMonth=" + budgetMonth +
               ", budgetYear=" + budgetYear +
               '}';
    }
}
