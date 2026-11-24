package com.expenseanalyzer.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for highest, lowest, average, and top 5 expenses.
 */
public class HighestLowestResponseDTO {

    private ExpenseHighlightDTO highest;
    private ExpenseHighlightDTO lowest;
    private BigDecimal average;
    private List<ExpenseHighlightDTO> top5 = new ArrayList<>();

    public HighestLowestResponseDTO() {
    }

    public HighestLowestResponseDTO(ExpenseHighlightDTO highest,
                                    ExpenseHighlightDTO lowest,
                                    BigDecimal average,
                                    List<ExpenseHighlightDTO> top5) {
        this.highest = highest;
        this.lowest = lowest;
        this.average = average;
        this.top5 = top5 != null ? top5 : new ArrayList<>();
    }

    public ExpenseHighlightDTO getHighest() {
        return highest;
    }

    public void setHighest(ExpenseHighlightDTO highest) {
        this.highest = highest;
    }

    public ExpenseHighlightDTO getLowest() {
        return lowest;
    }

    public void setLowest(ExpenseHighlightDTO lowest) {
        this.lowest = lowest;
    }

    public BigDecimal getAverage() {
        return average;
    }

    public void setAverage(BigDecimal average) {
        this.average = average;
    }

    public List<ExpenseHighlightDTO> getTop5() {
        return top5;
    }

    public void setTop5(List<ExpenseHighlightDTO> top5) {
        this.top5 = top5;
    }
}
