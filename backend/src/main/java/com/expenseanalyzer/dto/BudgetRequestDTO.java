package com.expenseanalyzer.dto;

import com.expenseanalyzer.model.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO for creating or updating a category budget limit.
 */
public class BudgetRequestDTO {

    @NotNull(message = "Category is required")
    private ExpenseCategory category;

    @NotNull(message = "Monthly limit is required")
    @DecimalMin(value = "0.01", message = "Monthly limit must be at least 0.01")
    @Digits(integer = 10, fraction = 2, message = "Monthly limit must have at most 10 integer digits and 2 decimal places")
    private BigDecimal monthlyLimit;

    @NotNull(message = "Budget month is required")
    @Min(value = 1, message = "Budget month must be between 1 and 12")
    @Max(value = 12, message = "Budget month must be between 1 and 12")
    private Integer budgetMonth;

    @NotNull(message = "Budget year is required")
    @Min(value = 2000, message = "Budget year must be 2000 or later")
    private Integer budgetYear;

    public BudgetRequestDTO() {
    }

    public BudgetRequestDTO(ExpenseCategory category,
                            BigDecimal monthlyLimit,
                            Integer budgetMonth,
                            Integer budgetYear) {
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.budgetMonth = budgetMonth;
        this.budgetYear = budgetYear;
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
}
