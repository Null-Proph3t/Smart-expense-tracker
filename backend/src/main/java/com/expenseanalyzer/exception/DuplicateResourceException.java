package com.expenseanalyzer.exception;

/**
 * Thrown when attempting to create a resource that violates a uniqueness constraint
 * (e.g., duplicate budget for the same category, month, and year).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
