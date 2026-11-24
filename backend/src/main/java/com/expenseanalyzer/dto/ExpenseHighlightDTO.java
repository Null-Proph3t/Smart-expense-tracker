package com.expenseanalyzer.dto;

import com.expenseanalyzer.model.Expense;
import com.expenseanalyzer.model.ExpenseCategory;
import com.expenseanalyzer.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lightweight DTO for displaying high-level expense highlights
 * (e.g., highest/lowest transaction, top 5 expenses).
 */
public class ExpenseHighlightDTO {

    private Long id;
    private String description;
    private LocalDate date;
    private ExpenseCategory category;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;

    public ExpenseHighlightDTO() {
    }

    public ExpenseHighlightDTO(Long id,
                              String description,
                              LocalDate date,
                              ExpenseCategory category,
                              PaymentMethod paymentMethod,
                              BigDecimal amount) {
        this.id = id;
        this.description = description;
        this.date = date;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
    }

    public static ExpenseHighlightDTO fromEntity(Expense expense) {
        if (expense == null) {
            return null;
        }
        return new ExpenseHighlightDTO(
                expense.getId(),
                expense.getDescription(),
                expense.getDate(),
                expense.getCategory(),
                expense.getPaymentMethod(),
                expense.getAmount()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
