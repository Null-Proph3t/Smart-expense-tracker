package com.expenseanalyzer.model;

/**
 * Payment methods supported by the application.
 */
public enum PaymentMethod {

    CASH("Cash"),
    UPI("UPI"),
    CARD("Card"),
    OTHER("Other");

    private final String displayLabel;

    PaymentMethod(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }
}
