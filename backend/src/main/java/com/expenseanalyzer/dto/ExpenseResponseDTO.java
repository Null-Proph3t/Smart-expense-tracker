package com.expenseanalyzer.dto;

import com.expenseanalyzer.model.Expense;
import com.expenseanalyzer.model.ExpenseCategory;
import com.expenseanalyzer.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO representing an expense response.
 */
public class ExpenseResponseDTO {

    private Long id;
    private String description;
    private BigDecimal amount;
    private LocalDate date;
    private ExpenseCategory category;
    private String categoryLabel;
    private PaymentMethod paymentMethod;
    private String paymentMethodLabel;
    private String notes;

    public ExpenseResponseDTO() {
    }

    public ExpenseResponseDTO(Long id,
                              String description,
                              BigDecimal amount,
                              LocalDate date,
                              ExpenseCategory category,
                              String categoryLabel,
                              PaymentMethod paymentMethod,
                              String paymentMethodLabel,
                              String notes) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.categoryLabel = categoryLabel;
        this.paymentMethod = paymentMethod;
        this.paymentMethodLabel = paymentMethodLabel;
        this.notes = notes;
    }

    public static ExpenseResponseDTO fromEntity(Expense expense) {
        if (expense == null) {
            return null;
        }
        return new ExpenseResponseDTO(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getDate(),
                expense.getCategory(),
                expense.getCategory() != null ? expense.getCategory().getDisplayLabel() : null,
                expense.getPaymentMethod(),
                expense.getPaymentMethod() != null ? expense.getPaymentMethod().getDisplayLabel() : null,
                expense.getNotes()
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

    public String getCategoryLabel() {
        return categoryLabel;
    }

    public void setCategoryLabel(String categoryLabel) {
        this.categoryLabel = categoryLabel;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentMethodLabel() {
        return paymentMethodLabel;
    }

    public void setPaymentMethodLabel(String paymentMethodLabel) {
        this.paymentMethodLabel = paymentMethodLabel;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
