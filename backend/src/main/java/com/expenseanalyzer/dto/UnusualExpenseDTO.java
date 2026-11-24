package com.expenseanalyzer.dto;

import java.math.BigDecimal;

/**
 * DTO representing an expense flagged as unusual spending
 * (amount > 2 * category average in a category with >= 2 transactions).
 */
public class UnusualExpenseDTO {

    private ExpenseResponseDTO expense;
    private BigDecimal categoryAverage;
    private BigDecimal multiplier;

    public UnusualExpenseDTO() {
    }

    public UnusualExpenseDTO(ExpenseResponseDTO expense, BigDecimal categoryAverage, BigDecimal multiplier) {
        this.expense = expense;
        this.categoryAverage = categoryAverage;
        this.multiplier = multiplier;
    }

    public ExpenseResponseDTO getExpense() {
        return expense;
    }

    public void setExpense(ExpenseResponseDTO expense) {
        this.expense = expense;
    }

    public BigDecimal getCategoryAverage() {
        return categoryAverage;
    }

    public void setCategoryAverage(BigDecimal categoryAverage) {
        this.categoryAverage = categoryAverage;
    }

    public BigDecimal getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(BigDecimal multiplier) {
        this.multiplier = multiplier;
    }
}
