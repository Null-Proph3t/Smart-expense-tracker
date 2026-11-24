package com.expenseanalyzer.model;

/**
 * Budget utilization status thresholds (ARCHITECTURE.md §4.1).
 * <p>
 * Status is computed on the rounded utilization percentage (2 decimal places, HALF_UP):
 * <ul>
 *   <li>WITHIN_BUDGET    — utilization &lt; 80.00 %</li>
 *   <li>APPROACHING_LIMIT — utilization 80.00 % – 100.00 % inclusive</li>
 *   <li>EXCEEDED         — utilization &gt; 100.00 %</li>
 * </ul>
 */
public enum BudgetStatus {

    WITHIN_BUDGET("Within Budget"),
    APPROACHING_LIMIT("Approaching Limit"),
    EXCEEDED("Exceeded");

    private final String displayLabel;

    BudgetStatus(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }
}
