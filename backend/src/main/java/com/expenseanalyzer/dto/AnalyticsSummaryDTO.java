package com.expenseanalyzer.dto;

import java.math.BigDecimal;

/**
 * DTO representing the 4 KPI cards on the dashboard summary:
 * - Total Expenses (all time)
 * - This Month's Spending
 * - Highest Expense (all time)
 * - Average Transaction (all time)
 */
public class AnalyticsSummaryDTO {

    private BigDecimal totalExpenses;
    private BigDecimal thisMonthSpending;
    private BigDecimal highestExpense;
    private BigDecimal averageTransaction;

    public AnalyticsSummaryDTO() {
    }

    public AnalyticsSummaryDTO(BigDecimal totalExpenses,
                               BigDecimal thisMonthSpending,
                               BigDecimal highestExpense,
                               BigDecimal averageTransaction) {
        this.totalExpenses = totalExpenses;
        this.thisMonthSpending = thisMonthSpending;
        this.highestExpense = highestExpense;
        this.averageTransaction = averageTransaction;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getThisMonthSpending() {
        return thisMonthSpending;
    }

    public void setThisMonthSpending(BigDecimal thisMonthSpending) {
        this.thisMonthSpending = thisMonthSpending;
    }

    public BigDecimal getHighestExpense() {
        return highestExpense;
    }

    public void setHighestExpense(BigDecimal highestExpense) {
        this.highestExpense = highestExpense;
    }

    public BigDecimal getAverageTransaction() {
        return averageTransaction;
    }

    public void setAverageTransaction(BigDecimal averageTransaction) {
        this.averageTransaction = averageTransaction;
    }
}
