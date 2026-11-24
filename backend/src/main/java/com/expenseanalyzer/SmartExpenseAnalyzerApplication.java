package com.expenseanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Smart Expense Pattern Analyzer application.
 * <p>
 * This class contains only the bootstrap call. All business logic lives
 * in the service, repository, and controller packages.
 * </p>
 */
@SpringBootApplication
public class SmartExpenseAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartExpenseAnalyzerApplication.class, args);
    }
}
