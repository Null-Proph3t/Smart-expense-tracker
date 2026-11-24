package com.expenseanalyzer.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing a comprehensive monthly spending report.
 */
public class MonthlyReportDTO {

    private String month; // YYYY-MM
    private BigDecimal totalExpenditure;
    private long transactionCount;
    private BigDecimal averageTransaction;
    private ExpenseHighlightDTO highestTransaction;
    private BigDecimal momChangePercent; // null when previous month was 0
    private BigDecimal averageDailyExpenditure;
    private List<CategorySummaryDTO> categoryBreakdown = new ArrayList<>();
    private List<MonthlyTrendPointDTO> historicalTrend = new ArrayList<>();

    public MonthlyReportDTO() {
    }

    public MonthlyReportDTO(String month,
                            BigDecimal totalExpenditure,
                            long transactionCount,
                            BigDecimal averageTransaction,
                            ExpenseHighlightDTO highestTransaction,
                            BigDecimal momChangePercent,
                            BigDecimal averageDailyExpenditure,
                            List<CategorySummaryDTO> categoryBreakdown,
                            List<MonthlyTrendPointDTO> historicalTrend) {
        this.month = month;
        this.totalExpenditure = totalExpenditure;
        this.transactionCount = transactionCount;
        this.averageTransaction = averageTransaction;
        this.highestTransaction = highestTransaction;
        this.momChangePercent = momChangePercent;
        this.averageDailyExpenditure = averageDailyExpenditure;
        this.categoryBreakdown = categoryBreakdown != null ? categoryBreakdown : new ArrayList<>();
        this.historicalTrend = historicalTrend != null ? historicalTrend : new ArrayList<>();
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public BigDecimal getTotalExpenditure() {
        return totalExpenditure;
    }

    public void setTotalExpenditure(BigDecimal totalExpenditure) {
        this.totalExpenditure = totalExpenditure;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public BigDecimal getAverageTransaction() {
        return averageTransaction;
    }

    public void setAverageTransaction(BigDecimal averageTransaction) {
        this.averageTransaction = averageTransaction;
    }

    public ExpenseHighlightDTO getHighestTransaction() {
        return highestTransaction;
    }

    public void setHighestTransaction(ExpenseHighlightDTO highestTransaction) {
        this.highestTransaction = highestTransaction;
    }

    public BigDecimal getMomChangePercent() {
        return momChangePercent;
    }

    public void setMomChangePercent(BigDecimal momChangePercent) {
        this.momChangePercent = momChangePercent;
    }

    public BigDecimal getAverageDailyExpenditure() {
        return averageDailyExpenditure;
    }

    public void setAverageDailyExpenditure(BigDecimal averageDailyExpenditure) {
        this.averageDailyExpenditure = averageDailyExpenditure;
    }

    public List<CategorySummaryDTO> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(List<CategorySummaryDTO> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    public List<MonthlyTrendPointDTO> getHistoricalTrend() {
        return historicalTrend;
    }

    public void setHistoricalTrend(List<MonthlyTrendPointDTO> historicalTrend) {
        this.historicalTrend = historicalTrend;
    }
}
