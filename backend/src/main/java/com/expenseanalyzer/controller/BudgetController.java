package com.expenseanalyzer.controller;

import com.expenseanalyzer.dto.BudgetRequestDTO;
import com.expenseanalyzer.dto.BudgetStatusDTO;
import com.expenseanalyzer.service.BudgetService;
import jakarta.validation.Valid;
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
import java.util.List;

/**
 * REST controller for budget limits and utilization tracking (spec §5.2).
 */
@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /**
     * GET /api/budgets?month=YYYY-MM
     * Returns budgets and computed utilization status for the given month (defaults to current month).
     */
    @GetMapping
    public ResponseEntity<List<BudgetStatusDTO>> getBudgets(
            @RequestParam(required = false) String month) {
        List<BudgetStatusDTO> budgets = budgetService.getBudgetsForMonth(month);
        return ResponseEntity.ok(budgets);
    }

    /**
     * GET /api/budgets/{id}
     * Returns status for a single budget by id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BudgetStatusDTO> findById(@PathVariable Long id) {
        BudgetStatusDTO budget = budgetService.findById(id);
        return ResponseEntity.ok(budget);
    }

    /**
     * POST /api/budgets
     * Creates a new monthly budget. Returns HTTP 201 Created.
     */
    @PostMapping
    public ResponseEntity<BudgetStatusDTO> create(@Valid @RequestBody BudgetRequestDTO request) {
        BudgetStatusDTO created = budgetService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * PUT /api/budgets/{id}
     * Updates an existing monthly budget limit or period. Returns HTTP 200 OK.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BudgetStatusDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequestDTO request) {
        BudgetStatusDTO updated = budgetService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/budgets/{id}
     * Deletes a monthly budget. Returns HTTP 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        budgetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
