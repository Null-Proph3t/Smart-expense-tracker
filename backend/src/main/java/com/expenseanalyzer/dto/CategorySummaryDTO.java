package com.expenseanalyzer.dto;

import com.expenseanalyzer.model.ExpenseCategory;

import java.math.BigDecimal;

/**
 * DTO for category-wise spending summary.
 */
public class CategorySummaryDTO {

    private ExpenseCategory category;
    private String categoryLabel;
    private BigDecimal totalAmount;
    private long transactionCount;
    private BigDecimal averageAmount;
    private BigDecimal percentage;

    public CategorySummaryDTO() {
    }

    public CategorySummaryDTO(ExpenseCategory category,
                              String categoryLabel,
                              BigDecimal totalAmount,
                              long transactionCount,
                              BigDecimal averageAmount,
                              BigDecimal percentage) {
        this.category = category;
        this.categoryLabel = categoryLabel;
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
        this.averageAmount = averageAmount;
        this.percentage = percentage;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public BigDecimal getAverageAmount() {
        return averageAmount;
    }

    public void setAverageAmount(BigDecimal averageAmount) {
        this.averageAmount = averageAmount;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }
}
