package com.expenseanalyzer.controller;

import com.expenseanalyzer.dto.ExpenseRequestDTO;
import com.expenseanalyzer.dto.ExpenseResponseDTO;
import com.expenseanalyzer.model.ExpenseCategory;
import com.expenseanalyzer.model.PaymentMethod;
import com.expenseanalyzer.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for Expense management (spec §5.2).
 */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /**
     * GET /api/expenses
     * Retrieves expenses with optional search, category, payment method, month, date range, and sorting.
     */
    @GetMapping
    public ResponseEntity<List<ExpenseResponseDTO>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ExpenseCategory category,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "date") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String order) {

        List<ExpenseResponseDTO> results = expenseService.findAll(
                search, category, paymentMethod, month, startDate, endDate, sortBy, order);
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/expenses/{id}
     * Retrieves a single expense by id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponseDTO> findById(@PathVariable Long id) {
        ExpenseResponseDTO expense = expenseService.findById(id);
        return ResponseEntity.ok(expense);
    }

    /**
     * POST /api/expenses
     * Creates a new expense. Returns HTTP 201 Created.
     */
    @PostMapping
    public ResponseEntity<ExpenseResponseDTO> create(@Valid @RequestBody ExpenseRequestDTO request) {
        ExpenseResponseDTO created = expenseService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * PUT /api/expenses/{id}
     * Updates an existing expense. Returns HTTP 200 OK.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequestDTO request) {
        ExpenseResponseDTO updated = expenseService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/expenses/{id}
     * Deletes an expense by id. Returns HTTP 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        expenseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
