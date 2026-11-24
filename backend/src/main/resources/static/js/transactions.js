/**
 * transactions.js — Transactions View & Shared Expense Modal Controller
 * Handles ledger search, multi-filter, sorting, modal creation/editing, and deletion.
 */

import {
    fetchExpenses,
    fetchExpenseById,
    createExpense,
    updateExpense,
    deleteExpense,
    formatINR,
    formatDate,
    formatLocalDateISO
} from './api.js';
import { loadDashboard } from './dashboard.js';

let currentEditingId = null;

export async function loadTransactions() {
    const search = document.getElementById('tx-search')?.value || '';
    const category = document.getElementById('tx-filter-category')?.value || '';
    const paymentMethod = document.getElementById('tx-filter-payment')?.value || '';
    const month = document.getElementById('tx-filter-month')?.value || '';
    const startDate = document.getElementById('tx-filter-start-date')?.value || '';
    const endDate = document.getElementById('tx-filter-end-date')?.value || '';
    const sortBy = document.getElementById('tx-sort-by')?.value || 'date';
    const order = document.getElementById('tx-sort-order')?.value || 'desc';

    const params = {
        search,
        category,
        paymentMethod,
        month,
        startDate,
        endDate,
        sortBy,
        order
    };

    try {
        const expenses = await fetchExpenses(params);
        renderTransactionsTable(expenses);
    } catch (err) {
        console.error('Failed to load transactions:', err);
    }
}

function renderTransactionsTable(expenses) {
    const tbody = document.getElementById('transactions-tbody');
    const countEl = document.getElementById('tx-count-display');
    if (countEl) countEl.textContent = `${expenses.length} transaction(s)`;

    if (!tbody) return;
    tbody.innerHTML = '';

    if (expenses.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty-state">No transactions match the selected filters.</td></tr>';
        return;
    }

    expenses.forEach(exp => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${formatDate(exp.date)}</td>
            <td><strong>${escapeHtml(exp.description)}</strong></td>
            <td><span class="badge badge-within">${escapeHtml(exp.categoryLabel || exp.category)}</span></td>
            <td>${escapeHtml(exp.paymentMethodLabel || exp.paymentMethod)}</td>
            <td class="text-right amount-cell">${formatINR(exp.amount)}</td>
            <td style="color: var(--ink-secondary); font-size: 0.75rem;">${escapeHtml(exp.notes || '—')}</td>
            <td class="text-right">
                <button class="btn btn-secondary btn-sm" data-action="edit" data-id="${exp.id}">Edit</button>
                <button class="btn btn-danger btn-sm" data-action="delete" data-id="${exp.id}" style="margin-left: 0.35rem;">Delete</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// =============================================================================
// Modal Management (Add / Edit)
// =============================================================================

export function openAddExpenseModal() {
    currentEditingId = null;
    const modal = document.getElementById('expense-modal');
    const title = document.getElementById('expense-modal-title');
    const form = document.getElementById('expense-form');
    const errBanner = document.getElementById('expense-form-error');

    if (title) title.textContent = 'Add New Expense';
    if (form) form.reset();
    if (errBanner) {
        errBanner.textContent = '';
        errBanner.style.display = 'none';
    }

    // Default to today's date (local timezone)
    const dateInput = document.getElementById('exp-date');
    if (dateInput) {
        dateInput.value = formatLocalDateISO(new Date());
    }

    if (modal) modal.classList.add('is-open');
}

export async function openEditExpenseModal(id) {
    currentEditingId = id;
    const modal = document.getElementById('expense-modal');
    const title = document.getElementById('expense-modal-title');
    const errBanner = document.getElementById('expense-form-error');

    if (title) title.textContent = 'Edit Expense';
    if (errBanner) {
        errBanner.textContent = '';
        errBanner.style.display = 'none';
    }

    try {
        const exp = await fetchExpenseById(id);
        document.getElementById('exp-desc').value = exp.description;
        document.getElementById('exp-amount').value = exp.amount;
        document.getElementById('exp-date').value = exp.date;
        document.getElementById('exp-category').value = exp.category || '';
        document.getElementById('exp-payment').value = exp.paymentMethod;
        document.getElementById('exp-notes').value = exp.notes || '';

        if (modal) modal.classList.add('is-open');
    } catch (err) {
        alert('Failed to load expense details: ' + err.message);
    }
}

export function closeExpenseModal() {
    const modal = document.getElementById('expense-modal');
    if (modal) modal.classList.remove('is-open');
    currentEditingId = null;
}

async function handleExpenseSubmit(event) {
    event.preventDefault();
    const errBanner = document.getElementById('expense-form-error');
    if (errBanner) {
        errBanner.textContent = '';
        errBanner.style.display = 'none';
    }

    const description = document.getElementById('exp-desc').value.trim();
    const amount = document.getElementById('exp-amount').value;
    const date = document.getElementById('exp-date').value;
    const category = document.getElementById('exp-category').value || null;
    const paymentMethod = document.getElementById('exp-payment').value;
    const notes = document.getElementById('exp-notes').value.trim() || null;

    const payload = {
        description,
        amount: Number(amount),
        date,
        category,
        paymentMethod,
        notes
    };

    try {
        if (currentEditingId) {
            await updateExpense(currentEditingId, payload);
        } else {
            await createExpense(payload);
        }
        closeExpenseModal();
        await loadTransactions();
        await loadDashboard();
    } catch (err) {
        if (errBanner) {
            errBanner.textContent = err.message;
            errBanner.style.display = 'block';
        } else {
            alert(err.message);
        }
    }
}

async function handleDeleteExpense(id) {
    if (!confirm('Are you sure you want to delete this expense? This action cannot be undone.')) {
        return;
    }

    try {
        await deleteExpense(id);
        await loadTransactions();
        await loadDashboard();
    } catch (err) {
        alert('Failed to delete expense: ' + err.message);
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

document.addEventListener('DOMContentLoaded', () => {
    // Debounced live search on description text box
    const searchEl = document.getElementById('tx-search');
    let searchTimer = null;
    if (searchEl) {
        searchEl.addEventListener('input', () => {
            clearTimeout(searchTimer);
            searchTimer = setTimeout(() => loadTransactions(), 150);
        });
    }

    // Filter change listeners on selects and date pickers (single event per change)
    const changeFilterIds = [
        'tx-filter-category', 'tx-filter-payment',
        'tx-filter-month', 'tx-filter-start-date', 'tx-filter-end-date',
        'tx-sort-by', 'tx-sort-order'
    ];
    changeFilterIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.addEventListener('change', () => loadTransactions());
        }
    });

    const filterInputs = ['tx-search', ...changeFilterIds];

    // Reset filters
    const resetBtn = document.getElementById('tx-btn-reset');
    if (resetBtn) {
        resetBtn.addEventListener('click', () => {
            filterInputs.forEach(id => {
                const el = document.getElementById(id);
                if (el) {
                    if (id === 'tx-sort-by') el.value = 'date';
                    else if (id === 'tx-sort-order') el.value = 'desc';
                    else el.value = '';
                }
            });
            loadTransactions();
        });
    }

    // Modal submit
    const form = document.getElementById('expense-form');
    if (form) {
        form.addEventListener('submit', handleExpenseSubmit);
    }

    // Modal cancel
    const cancelBtn = document.getElementById('expense-modal-cancel');
    const closeBtn = document.getElementById('expense-modal-close');
    if (cancelBtn) cancelBtn.addEventListener('click', closeExpenseModal);
    if (closeBtn) closeBtn.addEventListener('click', closeExpenseModal);

    // Global Add Expense trigger buttons
    document.querySelectorAll('[data-action="open-add-expense"]').forEach(btn => {
        btn.addEventListener('click', openAddExpenseModal);
    });

    // Delegated edit & delete clicks on tables
    document.addEventListener('click', (e) => {
        const target = e.target.closest('button[data-action]');
        if (!target) return;
        const action = target.getAttribute('data-action');
        const id = target.getAttribute('data-id');

        if (action === 'edit' && id) {
            openEditExpenseModal(Number(id));
        } else if (action === 'delete' && id) {
            handleDeleteExpense(Number(id));
        }
    });
});
