/**
 * api.js — Centralized API Communication Module
 * Handles all REST endpoints, JSON parsing, error surfacing, and money/date formatters.
 */

const BASE_URL = '/api';

export const CATEGORY_LABELS = {
    FOOD: 'Food',
    TRANSPORTATION: 'Transportation',
    SHOPPING: 'Shopping',
    ENTERTAINMENT: 'Entertainment',
    HEALTHCARE: 'Healthcare',
    EDUCATION: 'Education',
    GROCERIES: 'Groceries',
    BILLS_AND_UTILITIES: 'Bills & Utilities',
    OTHERS: 'Others'
};

export const PAYMENT_METHOD_LABELS = {
    CASH: 'Cash',
    UPI: 'UPI',
    CARD: 'Card',
    OTHER: 'Other'
};

export function formatCategoryLabel(code) {
    return CATEGORY_LABELS[code] || code || '';
}

export function formatPaymentLabel(code) {
    return PAYMENT_METHOD_LABELS[code] || code || '';
}

/**
 * Formats a Date object as YYYY-MM-DD using the user's local timezone.
 */
export function formatLocalDateISO(dateObj = new Date()) {
    const year = dateObj.getFullYear();
    const month = String(dateObj.getMonth() + 1).padStart(2, '0');
    const day = String(dateObj.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

const currencyFormatter = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
});

/**
 * Formats a number or string into Indian Rupee format: ₹1,245.50
 */
export function formatINR(val) {
    if (val === null || val === undefined || isNaN(val)) {
        return '₹0.00';
    }
    return currencyFormatter.format(Number(val));
}

/**
 * Formats ISO date string "YYYY-MM-DD" to human readable format "DD MMM YYYY"
 */
export function formatDate(dateStr) {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length !== 3) return dateStr;
    const year = parts[0];
    const monthIndex = parseInt(parts[1], 10) - 1;
    const day = parseInt(parts[2], 10);
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    return `${day} ${months[monthIndex]} ${year}`;
}

/**
 * Returns the current calendar month in "YYYY-MM" format.
 */
export function getCurrentMonth() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    return `${year}-${month}`;
}

/**
 * Core fetch wrapper with JSON parsing and structured error handling.
 */
async function request(endpoint, options = {}) {
    const url = `${BASE_URL}${endpoint}`;
    const headers = {
        'Accept': 'application/json',
        ...options.headers
    };

    if (options.body && typeof options.body === 'object') {
        headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(options.body);
    }

    const response = await fetch(url, { ...options, headers });

    if (response.status === 204) {
        return null;
    }

    let data;
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
        data = await response.json();
    } else {
        data = await response.text();
    }

    if (!response.ok) {
        let errorMsg = 'An error occurred';
        if (data && typeof data === 'object') {
            if (data.fieldErrors && Array.isArray(data.fieldErrors) && data.fieldErrors.length > 0) {
                errorMsg = data.fieldErrors.map(fe => `${fe.field}: ${fe.message}`).join(', ');
            } else if (data.message) {
                errorMsg = data.message;
            }
        }
        const error = new Error(errorMsg);
        error.status = response.status;
        error.data = data;
        throw error;
    }

    return data;
}

// =============================================================================
// Expenses API
// =============================================================================

export async function fetchExpenses(params = {}) {
    const query = new URLSearchParams();
    for (const [key, val] of Object.entries(params)) {
        if (val !== null && val !== undefined && val !== '') {
            query.append(key, val);
        }
    }
    const qs = query.toString();
    return request(`/expenses${qs ? '?' + qs : ''}`);
}

export async function fetchExpenseById(id) {
    return request(`/expenses/${id}`);
}

export async function createExpense(expenseData) {
    return request('/expenses', {
        method: 'POST',
        body: expenseData
    });
}

export async function updateExpense(id, expenseData) {
    return request(`/expenses/${id}`, {
        method: 'PUT',
        body: expenseData
    });
}

export async function deleteExpense(id) {
    return request(`/expenses/${id}`, {
        method: 'DELETE'
    });
}

// =============================================================================
// Analytics & Reports API
// =============================================================================

export async function fetchAnalyticsSummary() {
    return request('/analytics/summary');
}

export async function fetchCategoryAnalytics() {
    return request('/analytics/categories');
}

export async function fetchHighestLowest() {
    return request('/analytics/highest-lowest');
}

export async function fetchMonthlyReport(month) {
    const qs = month ? `?month=${encodeURIComponent(month)}` : '';
    return request(`/analytics/monthly${qs}`);
}

export async function fetchInsights() {
    return request('/analytics/insights');
}

// =============================================================================
// Budget API
// =============================================================================

export async function fetchBudgets(month) {
    const qs = month ? `?month=${encodeURIComponent(month)}` : '';
    return request(`/budgets${qs}`);
}

export async function createBudget(budgetData) {
    return request('/budgets', {
        method: 'POST',
        body: budgetData
    });
}

export async function updateBudget(id, budgetData) {
    return request(`/budgets/${id}`, {
        method: 'PUT',
        body: budgetData
    });
}

export async function deleteBudget(id) {
    return request(`/budgets/${id}`, {
        method: 'DELETE'
    });
}
