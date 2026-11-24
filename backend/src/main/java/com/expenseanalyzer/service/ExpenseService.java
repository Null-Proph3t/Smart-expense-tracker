package com.expenseanalyzer.service;

import com.expenseanalyzer.dto.ExpenseRequestDTO;
import com.expenseanalyzer.dto.ExpenseResponseDTO;
import com.expenseanalyzer.exception.ResourceNotFoundException;
import com.expenseanalyzer.model.Expense;
import com.expenseanalyzer.model.ExpenseCategory;
import com.expenseanalyzer.model.PaymentMethod;
import com.expenseanalyzer.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service managing Expense CRUD, search, filtering, and sorting (spec §4.1 Module A & E).
 */
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategorizationService categorizationService;

    public ExpenseService(ExpenseRepository expenseRepository,
                          CategorizationService categorizationService) {
        this.expenseRepository = expenseRepository;
        this.categorizationService = categorizationService;
    }

    /**
     * Creates a new expense.
     * If category is null or omitted in the request, invokes CategorizationService.
     */
    @Transactional
    public ExpenseResponseDTO create(ExpenseRequestDTO request) {
        ExpenseCategory category = request.getCategory();
        if (category == null) {
            category = categorizationService.categorize(request.getDescription());
        }

        BigDecimal scaledAmount = request.getAmount().setScale(2, RoundingMode.HALF_UP);

        Expense expense = new Expense(
                request.getDescription(),
                scaledAmount,
                request.getDate(),
                category,
                request.getPaymentMethod(),
                request.getNotes()
        );

        Expense saved = expenseRepository.save(expense);
        return ExpenseResponseDTO.fromEntity(saved);
    }

    /**
     * Retrieves an expense by its primary key id.
     *
     * @throws ResourceNotFoundException if no expense exists with the given id
     */
    @Transactional(readOnly = true)
    public ExpenseResponseDTO findById(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        return ExpenseResponseDTO.fromEntity(expense);
    }

    /**
     * Updates an existing expense.
     * If request category is non-null, updates it.
     * If request category is null, retains the existing entity category (never auto-reclassifies).
     *
     * @throws ResourceNotFoundException if no expense exists with the given id
     */
    @Transactional
    public ExpenseResponseDTO update(Long id, ExpenseRequestDTO request) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        expense.setDate(request.getDate());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setNotes(request.getNotes());

        // Preserve manual override if request category is null; update if specified
        if (request.getCategory() != null) {
            expense.setCategory(request.getCategory());
        }

        Expense updated = expenseRepository.save(expense);
        return ExpenseResponseDTO.fromEntity(updated);
    }

    /**
     * Deletes an expense by its primary key id.
     *
     * @throws ResourceNotFoundException if no expense exists with the given id
     */
    @Transactional
    public void delete(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        expenseRepository.delete(expense);
    }

    /**
     * Returns all expenses matching the combined filters and sorting criteria.
     *
     * @param search        case-insensitive substring filter on description
     * @param category      category filter
     * @param paymentMethod payment method filter
     * @param month         month in "YYYY-MM" format
     * @param startDate     inclusive start date
     * @param endDate       inclusive end date
     * @param sortBy        "date" or "amount" (default: "date")
     * @param order         "asc" or "desc" (default: "desc")
     * @return filtered and sorted list of expenses
     */
    @Transactional(readOnly = true)
    public List<ExpenseResponseDTO> findAll(String search,
                                            ExpenseCategory category,
                                            PaymentMethod paymentMethod,
                                            String month,
                                            LocalDate startDate,
                                            LocalDate endDate,
                                            String sortBy,
                                            String order) {
        List<Expense> allExpenses = expenseRepository.findAll();
        Stream<Expense> stream = allExpenses.stream();

        // 1. Filter: search (case-insensitive substring on description)
        if (search != null && !search.isBlank()) {
            String lowerSearch = search.trim().toLowerCase();
            stream = stream.filter(e -> e.getDescription() != null &&
                    e.getDescription().toLowerCase().contains(lowerSearch));
        }

        // 2. Filter: category
        if (category != null) {
            stream = stream.filter(e -> e.getCategory() == category);
        }

        // 3. Filter: payment method
        if (paymentMethod != null) {
            stream = stream.filter(e -> e.getPaymentMethod() == paymentMethod);
        }

        // 4. Filter: month (YYYY-MM)
        if (month != null && !month.isBlank()) {
            YearMonth targetMonth = YearMonth.parse(month.trim());
            stream = stream.filter(e -> e.getDate() != null &&
                    YearMonth.from(e.getDate()).equals(targetMonth));
        }

        // 5. Filter: startDate (inclusive)
        if (startDate != null) {
            stream = stream.filter(e -> e.getDate() != null && !e.getDate().isBefore(startDate));
        }

        // 6. Filter: endDate (inclusive)
        if (endDate != null) {
            stream = stream.filter(e -> e.getDate() != null && !e.getDate().isAfter(endDate));
        }

        // 7. Sort (validate allowed sortBy and order parameters)
        validateSortParameters(sortBy, order);
        Comparator<Expense> comparator = buildComparator(sortBy, order);

        return stream.sorted(comparator)
                .map(ExpenseResponseDTO::fromEntity)
                .toList();
    }

    private static void validateSortParameters(String sortBy, String order) {
        if (sortBy != null && !sortBy.isBlank()) {
            String normalizedSortBy = sortBy.trim();
            if (!"date".equalsIgnoreCase(normalizedSortBy) && !"amount".equalsIgnoreCase(normalizedSortBy)) {
                throw new IllegalArgumentException(
                        "Invalid sortBy parameter '" + sortBy + "'. Allowed values: date, amount");
            }
        }
        if (order != null && !order.isBlank()) {
            String normalizedOrder = order.trim();
            if (!"asc".equalsIgnoreCase(normalizedOrder) && !"desc".equalsIgnoreCase(normalizedOrder)) {
                throw new IllegalArgumentException(
                        "Invalid order parameter '" + order + "'. Allowed values: asc, desc");
            }
        }
    }

    private static Comparator<Expense> buildComparator(String sortBy, String order) {
        boolean isAsc = order != null && "asc".equalsIgnoreCase(order.trim());
        boolean sortByAmount = sortBy != null && "amount".equalsIgnoreCase(sortBy.trim());

        Comparator<Expense> primary;
        if (sortByAmount) {
            primary = Comparator.comparing(Expense::getAmount, Comparator.nullsLast(Comparator.naturalOrder()));
        } else {
            // Default sort by date
            primary = Comparator.comparing(Expense::getDate, Comparator.nullsLast(Comparator.naturalOrder()));
        }

        if (!isAsc) {
            primary = primary.reversed();
        }

        // Secondary tie-breaker by id for stable sorting
        Comparator<Expense> tieBreaker = Comparator.comparing(Expense::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        if (!isAsc) {
            tieBreaker = tieBreaker.reversed();
        }

        return primary.thenComparing(tieBreaker);
    }
}
