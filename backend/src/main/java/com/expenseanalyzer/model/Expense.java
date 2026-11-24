package com.expenseanalyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA entity mapping to the {@code expenses} table.
 * <p>
 * Monetary amounts use {@code BigDecimal} with {@code precision=12, scale=2}
 * to prevent floating-point rounding errors (ARCHITECTURE.md §9.2).
 * Dates are stored as {@code LocalDate} (no time component).
 * No Lombok — all accessors are hand-written.
 * </p>
 */
@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Description must not be blank")
    @Size(max = 200, message = "Description must not exceed 200 characters")
    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Date is required")
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ExpenseCategory category;

    @NotNull(message = "Payment method is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Column(name = "notes", length = 500)
    private String notes;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Expense() {
    }

    public Expense(String description,
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

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "Expense{" +
               "id=" + id +
               ", description='" + description + '\'' +
               ", amount=" + amount +
               ", date=" + date +
               ", category=" + category +
               ", paymentMethod=" + paymentMethod +
               '}';
    }
}
