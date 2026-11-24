package com.expenseanalyzer.exception;

/**
 * Thrown when a requested resource (Expense, Budget, etc.) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
