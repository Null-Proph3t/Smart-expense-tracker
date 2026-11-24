/**
 * dashboard.js — Dashboard View Controller
 * Coordinates KPI cards, overview charts, insights list, and recent transactions.
 */

import {
    fetchAnalyticsSummary,
    fetchCategoryAnalytics,
    fetchMonthlyReport,
    fetchInsights,
    fetchExpenses,
    formatINR,
    formatDate,
    getCurrentMonth,
    formatCategoryLabel,
    formatPaymentLabel
} from './api.js';
import { renderCategoryDoughnut, renderMonthlyTrendBar } from './charts.js';

export async function loadDashboard() {
    try {
        const [summary, categories, monthlyReport, insights, recentExpenses] = await Promise.all([
            fetchAnalyticsSummary(),
            fetchCategoryAnalytics(),
            fetchMonthlyReport(getCurrentMonth()),
            fetchInsights(),
            fetchExpenses({ sortBy: 'date', order: 'desc' })
        ]);

        // 1. Populate 4 KPI cards
        const kpiTotal = document.getElementById('kpi-total-expenses');
        const kpiMonth = document.getElementById('kpi-this-month');
        const kpiHighest = document.getElementById('kpi-highest-expense');
        const kpiAverage = document.getElementById('kpi-average-transaction');

        if (kpiTotal) kpiTotal.textContent = formatINR(summary.totalExpenses);
        if (kpiMonth) kpiMonth.textContent = formatINR(summary.thisMonthSpending);
        if (kpiHighest) kpiHighest.textContent = formatINR(summary.highestExpense);
        if (kpiAverage) kpiAverage.textContent = formatINR(summary.averageTransaction);

        // 2. Render Charts
        renderCategoryDoughnut('dashboard-category-chart', categories);
        if (monthlyReport && monthlyReport.historicalTrend) {
            renderMonthlyTrendBar('dashboard-trend-chart', monthlyReport.historicalTrend);
        }

        // 3. Automated Insights & Unusual Spending
        const insightsList = document.getElementById('dashboard-insights-list');
        if (insightsList) {
            insightsList.innerHTML = '';
            if (insights.insights && insights.insights.length > 0) {
                insights.insights.forEach(item => {
                    const li = document.createElement('li');
                    li.className = 'insight-item';
                    li.textContent = item;
                    insightsList.appendChild(li);
                });
            } else {
                insightsList.innerHTML = '<li class="empty-state">No insights generated yet.</li>';
            }
        }

        const unusualContainer = document.getElementById('dashboard-unusual-container');
        if (unusualContainer) {
            unusualContainer.innerHTML = '';
            if (insights.unusualExpenses && insights.unusualExpenses.length > 0) {
                const wrapper = document.createElement('div');
                wrapper.className = 'unusual-callout';
                wrapper.innerHTML = '<div class="unusual-heading">Flagged Unusual Transactions (&gt; 2&times; Category Average)</div>';
                insights.unusualExpenses.forEach(u => {
                    const exp = u.expense || {};
                    const div = document.createElement('div');
                    div.className = 'unusual-item';
                    div.innerHTML = `
                        <div>
                            <strong>${escapeHtml(exp.description)}</strong>
                            <div class="unusual-meta">
                                ${escapeHtml(exp.categoryLabel || formatCategoryLabel(exp.category))}
                                &middot; ${formatDate(exp.date)}
                                &middot; Cat. Avg ${formatINR(u.categoryAverage)}
                            </div>
                        </div>
                        <div class="text-right">
                            <div class="amount-cell">${formatINR(exp.amount)}</div>
                            <span class="badge badge-exceeded" style="margin-top: 2px;">${Number(u.multiplier).toFixed(2)}&times; avg</span>
                        </div>
                    `;
                    wrapper.appendChild(div);
                });
                unusualContainer.appendChild(wrapper);
            }
        }

        // 4. Recent Transactions (top 5)
        const recentTbody = document.getElementById('dashboard-recent-tbody');
        if (recentTbody) {
            recentTbody.innerHTML = '';
            const topRecent = recentExpenses.slice(0, 5);
            if (topRecent.length === 0) {
                recentTbody.innerHTML = '<tr><td colspan="5" class="empty-state">No recent expenses found.</td></tr>';
            } else {
                topRecent.forEach(exp => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td>${formatDate(exp.date)}</td>
                        <td><strong>${escapeHtml(exp.description)}</strong></td>
                        <td><span class="badge badge-within">${escapeHtml(exp.categoryLabel || formatCategoryLabel(exp.category))}</span></td>
                        <td>${escapeHtml(exp.paymentMethodLabel || formatPaymentLabel(exp.paymentMethod))}</td>
                        <td class="text-right amount-cell">${formatINR(exp.amount)}</td>
                    `;
                    recentTbody.appendChild(tr);
                });
            }
        }
    } catch (err) {
        console.error('Failed to load dashboard:', err);
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

document.addEventListener('DOMContentLoaded', () => {
    // Initial load
    loadDashboard();
});
