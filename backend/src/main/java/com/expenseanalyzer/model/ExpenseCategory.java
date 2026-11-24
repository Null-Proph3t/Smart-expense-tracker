package com.expenseanalyzer.model;

/**
 * Canonical expense categories.
 * Declaration order matches the UI list (ARCHITECTURE.md §4.1).
 * Classification priority order is different — see CategorizationService (Phase 3).
 */
public enum ExpenseCategory {

    FOOD("Food"),
    TRANSPORTATION("Transportation"),
    SHOPPING("Shopping"),
    ENTERTAINMENT("Entertainment"),
    HEALTHCARE("Healthcare"),
    EDUCATION("Education"),
    GROCERIES("Groceries"),
    BILLS_AND_UTILITIES("Bills & Utilities"),
    OTHERS("Others");

    private final String displayLabel;

    ExpenseCategory(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }
}
