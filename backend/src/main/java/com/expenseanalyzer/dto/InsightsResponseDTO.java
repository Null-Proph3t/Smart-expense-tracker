package com.expenseanalyzer.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO containing automated rule-based insights and unusual spending alerts.
 */
public class InsightsResponseDTO {

    private List<String> insights = new ArrayList<>();
    private List<UnusualExpenseDTO> unusualExpenses = new ArrayList<>();

    public InsightsResponseDTO() {
    }

    public InsightsResponseDTO(List<String> insights, List<UnusualExpenseDTO> unusualExpenses) {
        this.insights = insights != null ? insights : new ArrayList<>();
        this.unusualExpenses = unusualExpenses != null ? unusualExpenses : new ArrayList<>();
    }

    public List<String> getInsights() {
        return insights;
    }

    public void setInsights(List<String> insights) {
        this.insights = insights;
    }

    public List<UnusualExpenseDTO> getUnusualExpenses() {
        return unusualExpenses;
    }

    public void setUnusualExpenses(List<UnusualExpenseDTO> unusualExpenses) {
        this.unusualExpenses = unusualExpenses;
    }
}
