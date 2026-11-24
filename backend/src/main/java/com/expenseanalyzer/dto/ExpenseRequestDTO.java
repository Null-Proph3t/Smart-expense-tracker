package com.expenseanalyzer.dto;

import com.expenseanalyzer.model.ExpenseCategory;
import com.expenseanalyzer.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating or updating an expense.
 */
public class ExpenseRequestDTO {

    @NotBlank(message = "Description must not be blank")
    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 10 integer digits and 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Date is required")
    private LocalDate date;

    // Optional on creation (if null, CategorizationService is invoked)
    private ExpenseCategory category;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    public ExpenseRequestDTO() {
    }

    public ExpenseRequestDTO(String description,
                             BigDecimal amount,
                             LocalDate date,
                             ExpenseCategory category,
                             PaymentMethod paymentMethod,
                             String notes) {
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
