/**
 * budget.js — Budget Tracking View Controller
 * Handles monthly budget display, utilization thresholds, form creation, updates, and deletion.
 */

import {
    fetchBudgets,
    createBudget,
    updateBudget,
    deleteBudget,
    formatINR,
    getCurrentMonth
} from './api.js';

let editingBudgetId = null;

export async function loadBudgetView() {
    const monthSelect = document.getElementById('budget-month-select');
    const selectedMonth = monthSelect ? monthSelect.value || getCurrentMonth() : getCurrentMonth();

    try {
        const budgets = await fetchBudgets(selectedMonth);
        renderBudgetTable(budgets);
    } catch (err) {
        console.error('Failed to load budgets:', err);
    }
}

function renderBudgetTable(budgets) {
    const tbody = document.getElementById('budget-tbody');
    if (!tbody) return;
    tbody.innerHTML = '';

    if (budgets.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty-state">No budgets configured for this month. Use the form above to add one.</td></tr>';
        return;
    }

    budgets.forEach(b => {
        const util = Number(b.utilizationPercent || 0);
        const progressWidth = Math.min(Math.max(util, 0), 100);

        let badgeClass = 'badge-within';
        if (b.status === 'APPROACHING_LIMIT') {
            badgeClass = 'badge-approaching';
        } else if (b.status === 'EXCEEDED') {
            badgeClass = 'badge-exceeded';
        }

        const remainingVal = Number(b.remainingAmount);
        const remainingDisplay = remainingVal < 0
            ? `-${formatINR(Math.abs(remainingVal))}`
            : formatINR(remainingVal);

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><strong>${escapeHtml(b.categoryLabel || b.category)}</strong></td>
            <td class="text-right">${formatINR(b.monthlyLimit)}</td>
            <td class="text-right amount-cell">${formatINR(b.actualSpent)}</td>
            <td class="text-right" style="font-weight: 600; color: ${remainingVal < 0 ? '#111111' : 'inherit'};">${remainingDisplay}</td>
            <td style="width: 180px;">
                <div style="display: flex; justify-content: space-between; font-size: 0.75rem; margin-bottom: 2px;">
                    <span>${util.toFixed(1)}%</span>
                </div>
                <div class="progress-bar-container">
                    <div class="progress-bar-fill" style="width: ${progressWidth}%;"></div>
                </div>
            </td>
            <td><span class="badge ${badgeClass}">${escapeHtml(b.statusLabel || b.status)}</span></td>
            <td class="text-right">
                <button class="btn btn-secondary btn-sm" data-budget-action="edit" data-id="${b.id}" data-category="${b.category}" data-limit="${b.monthlyLimit}">Edit</button>
                <button class="btn btn-danger btn-sm" data-budget-action="delete" data-id="${b.id}" style="margin-left: 0.35rem;">Delete</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

async function handleBudgetSubmit(event) {
    event.preventDefault();
    const errBanner = document.getElementById('budget-form-error');
    if (errBanner) {
        errBanner.textContent = '';
        errBanner.style.display = 'none';
    }

    const category = document.getElementById('budget-form-category').value;
    const monthlyLimit = document.getElementById('budget-form-limit').value;
    const monthSelect = document.getElementById('budget-month-select').value || getCurrentMonth();
    const parts = monthSelect.split('-');
    const budgetYear = parseInt(parts[0], 10);
    const budgetMonth = parseInt(parts[1], 10);

    const payload = {
        category,
        monthlyLimit: Number(monthlyLimit),
        budgetMonth,
        budgetYear
    };

    try {
        if (editingBudgetId) {
            await updateBudget(editingBudgetId, payload);
        } else {
            await createBudget(payload);
        }
        resetBudgetForm();
        await loadBudgetView();
    } catch (err) {
        if (errBanner) {
            errBanner.textContent = err.message;
            errBanner.style.display = 'block';
        } else {
            alert(err.message);
        }
    }
}

function resetBudgetForm() {
    editingBudgetId = null;
    const form = document.getElementById('budget-form');
    if (form) form.reset();
    const submitBtn = document.getElementById('budget-form-submit-btn');
    if (submitBtn) submitBtn.textContent = 'Save Budget';
    const heading = document.getElementById('budget-form-heading');
    if (heading) heading.textContent = 'Set Category Budget';
    const cancelBtn = document.getElementById('budget-form-cancel-btn');
    if (cancelBtn) cancelBtn.style.display = 'none';
}

function startEditBudget(id, category, limit) {
    editingBudgetId = id;
    document.getElementById('budget-form-category').value = category;
    document.getElementById('budget-form-limit').value = limit;

    const submitBtn = document.getElementById('budget-form-submit-btn');
    if (submitBtn) submitBtn.textContent = 'Update Budget';
    const heading = document.getElementById('budget-form-heading');
    if (heading) heading.textContent = 'Update Category Budget';
    const cancelBtn = document.getElementById('budget-form-cancel-btn');
    if (cancelBtn) cancelBtn.style.display = 'inline-flex';
}

async function handleDeleteBudget(id) {
    if (!confirm('Are you sure you want to delete this budget limit? Recorded expenses will not be affected.')) {
        return;
    }

    try {
        await deleteBudget(id);
        await loadBudgetView();
    } catch (err) {
        alert('Failed to delete budget: ' + err.message);
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

document.addEventListener('DOMContentLoaded', () => {
    // Month picker in Budget view
    const monthPicker = document.getElementById('budget-month-select');
    if (monthPicker) {
        monthPicker.value = getCurrentMonth();
        monthPicker.addEventListener('change', () => {
            resetBudgetForm();
            loadBudgetView();
        });
    }

    // Budget form submit
    const form = document.getElementById('budget-form');
    if (form) {
        form.addEventListener('submit', handleBudgetSubmit);
    }

    // Cancel edit
    const cancelBtn = document.getElementById('budget-form-cancel-btn');
    if (cancelBtn) {
        cancelBtn.addEventListener('click', resetBudgetForm);
    }

    // Delegated edit & delete clicks
    document.addEventListener('click', (e) => {
        const target = e.target.closest('button[data-budget-action]');
        if (!target) return;
        const action = target.getAttribute('data-budget-action');
        const id = target.getAttribute('data-id');

        if (action === 'edit') {
            const category = target.getAttribute('data-category');
            const limit = target.getAttribute('data-limit');
            startEditBudget(Number(id), category, limit);
        } else if (action === 'delete') {
            handleDeleteBudget(Number(id));
        }
    });
});
