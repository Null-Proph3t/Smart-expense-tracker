package com.expenseanalyzer.dto;

import com.expenseanalyzer.model.BudgetStatus;
import com.expenseanalyzer.model.ExpenseCategory;

import java.math.BigDecimal;

/**
 * DTO representing budget utilization vs limit.
 */
public class BudgetStatusDTO {

    private Long id;
    private ExpenseCategory category;
    private String categoryLabel;
    private BigDecimal monthlyLimit;
    private BigDecimal actualSpent;
    private BigDecimal remainingAmount;
    private BigDecimal utilizationPercent;
    private BudgetStatus status;
    private String statusLabel;
    private int budgetMonth;
    private int budgetYear;

    public BudgetStatusDTO() {
    }

    public BudgetStatusDTO(Long id,
                           ExpenseCategory category,
                           String categoryLabel,
                           BigDecimal monthlyLimit,
                           BigDecimal actualSpent,
                           BigDecimal remainingAmount,
                           BigDecimal utilizationPercent,
                           BudgetStatus status,
                           String statusLabel,
                           int budgetMonth,
                           int budgetYear) {
        this.id = id;
        this.category = category;
        this.categoryLabel = categoryLabel;
        this.monthlyLimit = monthlyLimit;
        this.actualSpent = actualSpent;
        this.remainingAmount = remainingAmount;
        this.utilizationPercent = utilizationPercent;
        this.status = status;
        this.statusLabel = statusLabel;
        this.budgetMonth = budgetMonth;
        this.budgetYear = budgetYear;
    }

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

    public String getCategoryLabel() {
        return categoryLabel;
    }

    public void setCategoryLabel(String categoryLabel) {
        this.categoryLabel = categoryLabel;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(BigDecimal monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public BigDecimal getActualSpent() {
        return actualSpent;
    }

    public void setActualSpent(BigDecimal actualSpent) {
        this.actualSpent = actualSpent;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public BigDecimal getUtilizationPercent() {
        return utilizationPercent;
    }

    public void setUtilizationPercent(BigDecimal utilizationPercent) {
        this.utilizationPercent = utilizationPercent;
    }

    public BudgetStatus getStatus() {
        return status;
    }

    public void setStatus(BudgetStatus status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public int getBudgetMonth() {
        return budgetMonth;
    }

    public void setBudgetMonth(int budgetMonth) {
        this.budgetMonth = budgetMonth;
    }

    public int getBudgetYear() {
        return budgetYear;
    }

    public void setBudgetYear(int budgetYear) {
        this.budgetYear = budgetYear;
    }
}
