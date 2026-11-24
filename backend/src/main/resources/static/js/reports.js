/**
 * reports.js — Analytics View & Monthly Reports View Controller
 * Owns rendering for both the Analytics view and the Monthly Reports view (ARCHITECTURE.md §7.3).
 */

import {
    fetchCategoryAnalytics,
    fetchHighestLowest,
    fetchMonthlyReport,
    formatINR,
    formatDate,
    getCurrentMonth,
    formatCategoryLabel,
    formatPaymentLabel
} from './api.js';
import { renderCategoryDistributionBar, renderMonthlyTrendBar } from './charts.js';

// =============================================================================
// Analytics View
// =============================================================================

export async function loadAnalyticsView() {
    try {
        const [categories, hl] = await Promise.all([
            fetchCategoryAnalytics(),
            fetchHighestLowest()
        ]);

        // 1. Category Distribution Table
        const tbody = document.getElementById('analytics-category-tbody');
        if (tbody) {
            tbody.innerHTML = '';
            categories.forEach(cat => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td><strong>${escapeHtml(cat.categoryLabel)}</strong></td>
                    <td class="text-center">${cat.transactionCount}</td>
                    <td class="text-right amount-cell">${formatINR(cat.totalAmount)}</td>
                    <td class="text-right">${formatINR(cat.averageAmount)}</td>
                    <td class="text-right"><strong>${cat.percentage.toFixed(2)}%</strong></td>
                `;
                tbody.appendChild(tr);
            });
        }

        // 2. Category Distribution Chart
        renderCategoryDistributionBar('analytics-category-chart', categories);

        // 3. Extrema KPI Cards
        const cardHighestVal = document.getElementById('analytics-highest-val');
        const cardHighestDesc = document.getElementById('analytics-highest-desc');
        const cardLowestVal = document.getElementById('analytics-lowest-val');
        const cardLowestDesc = document.getElementById('analytics-lowest-desc');
        const cardAvgVal = document.getElementById('analytics-avg-val');

        if (hl.highest && cardHighestVal) {
            cardHighestVal.textContent = formatINR(hl.highest.amount);
            if (cardHighestDesc) {
                cardHighestDesc.textContent = `${hl.highest.description} (${formatDate(hl.highest.date)})`;
            }
        } else if (cardHighestVal) {
            cardHighestVal.textContent = '₹0.00';
            if (cardHighestDesc) cardHighestDesc.textContent = 'No transactions';
        }

        if (hl.lowest && cardLowestVal) {
            cardLowestVal.textContent = formatINR(hl.lowest.amount);
            if (cardLowestDesc) {
                cardLowestDesc.textContent = `${hl.lowest.description} (${formatDate(hl.lowest.date)})`;
            }
        } else if (cardLowestVal) {
            cardLowestVal.textContent = '₹0.00';
            if (cardLowestDesc) cardLowestDesc.textContent = 'No transactions';
        }

        if (cardAvgVal) {
            cardAvgVal.textContent = formatINR(hl.average);
        }

        // 4. Top 5 Highest Expenses Table
        const top5Tbody = document.getElementById('analytics-top5-tbody');
        if (top5Tbody) {
            top5Tbody.innerHTML = '';
            if (!hl.top5 || hl.top5.length === 0) {
                top5Tbody.innerHTML = '<tr><td colspan="6" class="empty-state">No expenses available.</td></tr>';
            } else {
                hl.top5.forEach((exp, idx) => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td class="text-center" style="font-weight: 700;">#${idx + 1}</td>
                        <td><strong>${escapeHtml(exp.description)}</strong></td>
                        <td><span class="badge badge-within">${escapeHtml(formatCategoryLabel(exp.category))}</span></td>
                        <td>${formatDate(exp.date)}</td>
                        <td>${escapeHtml(formatPaymentLabel(exp.paymentMethod))}</td>
                        <td class="text-right amount-cell">${formatINR(exp.amount)}</td>
                    `;
                    top5Tbody.appendChild(tr);
                });
            }
        }

    } catch (err) {
        console.error('Failed to load analytics view:', err);
    }
}

// =============================================================================
// Monthly Reports View
// =============================================================================

export async function loadMonthlyReportsView(month) {
    const selectedMonth = month || document.getElementById('reports-month-select')?.value || getCurrentMonth();

    try {
        const report = await fetchMonthlyReport(selectedMonth);

        // 1. Monthly KPI Metrics
        const totalEl = document.getElementById('rep-total');
        const countEl = document.getElementById('rep-count');
        const avgEl = document.getElementById('rep-avg');
        const highestEl = document.getElementById('rep-highest');
        const dailyAvgEl = document.getElementById('rep-daily-avg');
        const momEl = document.getElementById('rep-mom');

        if (totalEl) totalEl.textContent = formatINR(report.totalExpenditure);
        if (countEl) countEl.textContent = report.transactionCount;
        if (avgEl) avgEl.textContent = formatINR(report.averageTransaction);
        if (highestEl) {
            highestEl.textContent = report.highestTransaction
                ? formatINR(report.highestTransaction.amount)
                : '₹0.00';
        }
        if (dailyAvgEl) dailyAvgEl.textContent = formatINR(report.averageDailyExpenditure);

        if (momEl) {
            // Null when previous month was 0 -> render literal "N/A"
            if (report.momChangePercent === null || report.momChangePercent === undefined) {
                momEl.textContent = 'N/A';
            } else {
                const val = Number(report.momChangePercent);
                const prefix = val > 0 ? '+' : '';
                momEl.textContent = `${prefix}${val.toFixed(2)}%`;
            }
        }

        // 2. Category Breakdown Table
        const tbody = document.getElementById('reports-category-tbody');
        if (tbody) {
            tbody.innerHTML = '';
            if (!report.categoryBreakdown || report.categoryBreakdown.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" class="empty-state">No spending in this month.</td></tr>';
            } else {
                report.categoryBreakdown.forEach(cat => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td><strong>${escapeHtml(cat.categoryLabel)}</strong></td>
                        <td class="text-center">${cat.transactionCount}</td>
                        <td class="text-right amount-cell">${formatINR(cat.totalAmount)}</td>
                        <td class="text-right"><strong>${cat.percentage.toFixed(2)}%</strong></td>
                    `;
                    tbody.appendChild(tr);
                });
            }
        }

        // 3. Historical Monthly Trend Chart
        if (report.historicalTrend) {
            renderMonthlyTrendBar('reports-trend-chart', report.historicalTrend);
        }

    } catch (err) {
        console.error('Failed to load monthly report:', err);
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

document.addEventListener('DOMContentLoaded', () => {
    // Month picker in Monthly Reports view
    const monthPicker = document.getElementById('reports-month-select');
    if (monthPicker) {
        monthPicker.value = getCurrentMonth();
        monthPicker.addEventListener('change', (e) => {
            loadMonthlyReportsView(e.target.value);
        });
    }
});
