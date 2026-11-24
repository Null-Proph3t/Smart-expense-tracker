package com.expenseanalyzer.service;

import com.expenseanalyzer.model.ExpenseCategory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Rule-based expense categorization service (spec §4.1 Module B).
 * <p>
 * Evaluates descriptions against keyword lists in strict priority order.
 * Keyword matching is case-insensitive with whole-word regex boundaries (\b).
 * Fallback category is OTHERS.
 * </p>
 */
@Service
public class CategorizationService {

    private record CategoryRule(ExpenseCategory category, List<Pattern> patterns) {}

    private final List<CategoryRule> rules;

    public CategorizationService() {
        List<CategoryRule> ruleList = new ArrayList<>();

        // 1. GROCERIES
        ruleList.add(createRule(ExpenseCategory.GROCERIES, Arrays.asList(
                "vegetables", "grocery", "groceries", "milk", "fruits",
                "supermarket", "bigbasket", "blinkit", "zepto"
        )));

        // 2. FOOD
        ruleList.add(createRule(ExpenseCategory.FOOD, Arrays.asList(
                "restaurant", "pizza", "coffee", "cafe", "lunch",
                "dinner", "breakfast", "burger", "zomato", "swiggy"
        )));

        // 3. TRANSPORTATION
        ruleList.add(createRule(ExpenseCategory.TRANSPORTATION, Arrays.asList(
                "uber", "ola", "bus", "train", "metro",
                "fuel", "petrol", "diesel", "cab", "auto"
        )));

        // 4. HEALTHCARE
        ruleList.add(createRule(ExpenseCategory.HEALTHCARE, Arrays.asList(
                "medicine", "hospital", "pharmacy", "doctor",
                "clinic", "medical", "lab"
        )));

        // 5. EDUCATION
        ruleList.add(createRule(ExpenseCategory.EDUCATION, Arrays.asList(
                "books", "college", "course", "tuition",
                "udemy", "coursera", "exam", "stationery"
        )));

        // 6. BILLS_AND_UTILITIES
        ruleList.add(createRule(ExpenseCategory.BILLS_AND_UTILITIES, Arrays.asList(
                "electricity", "internet", "wifi", "rent",
                "water", "broadband", "recharge", "bill"
        )));

        // 7. ENTERTAINMENT
        ruleList.add(createRule(ExpenseCategory.ENTERTAINMENT, Arrays.asList(
                "netflix", "cinema", "movies", "movie",
                "spotify", "concert", "gaming"
        )));

        // 8. SHOPPING
        ruleList.add(createRule(ExpenseCategory.SHOPPING, Arrays.asList(
                "amazon", "flipkart", "clothing", "clothes",
                "shoes", "electronics", "mall"
        )));

        this.rules = Collections.unmodifiableList(ruleList);
    }

    /**
     * Categorizes an expense based on its description text.
     *
     * @param description raw transaction description
     * @return detected ExpenseCategory, or OTHERS if no keywords match or description is empty
     */
    public ExpenseCategory categorize(String description) {
        if (description == null || description.isBlank()) {
            return ExpenseCategory.OTHERS;
        }

        for (CategoryRule rule : rules) {
            for (Pattern pattern : rule.patterns()) {
                if (pattern.matcher(description).find()) {
                    return rule.category();
                }
            }
        }

        return ExpenseCategory.OTHERS;
    }

    private static CategoryRule createRule(ExpenseCategory category, List<String> keywords) {
        List<Pattern> patterns = new ArrayList<>();
        for (String kw : keywords) {
            // Whole-word boundary, case-insensitive
            patterns.add(Pattern.compile("\\b" + Pattern.quote(kw) + "\\b", Pattern.CASE_INSENSITIVE));
        }
        return new CategoryRule(category, Collections.unmodifiableList(patterns));
    }
}
