package com.expenseanalyzer.dto;

import java.math.BigDecimal;

/**
 * DTO representing a single data point in a monthly spending trend.
 */
public class MonthlyTrendPointDTO {

    private String month; // YYYY-MM
    private BigDecimal total;

    public MonthlyTrendPointDTO() {
    }

    public MonthlyTrendPointDTO(String month, BigDecimal total) {
        this.month = month;
        this.total = total;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
