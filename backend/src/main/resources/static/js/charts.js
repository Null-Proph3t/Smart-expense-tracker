/**
 * charts.js — Monochromatic Chart.js Integration
 * Handles rendering, monochrome color palette assignment, and lifecycle teardown.
 */

// Stepped monochromatic grey palette (ARCHITECTURE.md §8)
const MONO_PALETTE = [
    '#111111', // Ink black
    '#333333',
    '#555555',
    '#777777',
    '#999999',
    '#BBBBBB',
    '#D8D8D8',
    '#666666',
    '#444444'
];

const activeCharts = {};

/**
 * Destroys any existing chart on the specified canvas element.
 */
export function destroyChart(canvasId) {
    if (activeCharts[canvasId]) {
        activeCharts[canvasId].destroy();
        delete activeCharts[canvasId];
    }
}

/**
 * Renders a monochromatic Doughnut chart for Category Spending breakdown.
 */
export function renderCategoryDoughnut(canvasId, categories) {
    destroyChart(canvasId);

    const canvas = document.getElementById(canvasId);
    if (!canvas || !window.Chart) return;

    // Filter to categories that have spend, or show all if total is zero
    const active = categories.filter(c => Number(c.totalAmount) > 0);
    const dataToRender = active.length > 0 ? active : categories;

    const labels = dataToRender.map(c => c.categoryLabel);
    const data = dataToRender.map(c => Number(c.totalAmount));
    const colors = dataToRender.map((_, i) => MONO_PALETTE[i % MONO_PALETTE.length]);

    activeCharts[canvasId] = new window.Chart(canvas, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: colors,
                borderColor: '#FFFFFF',
                borderWidth: 2,
                hoverOffset: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'right',
                    labels: {
                        boxWidth: 12,
                        padding: 12,
                        font: {
                            family: 'system-ui, -apple-system, sans-serif',
                            size: 11
                        },
                        color: '#333333'
                    }
                },
                tooltip: {
                    backgroundColor: '#111111',
                    titleColor: '#FFFFFF',
                    bodyColor: '#FFFFFF',
                    displayColors: true,
                    callbacks: {
                        label: function(context) {
                            const val = context.raw || 0;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const pct = total > 0 ? ((val / total) * 100).toFixed(1) : '0.0';
                            return ` ₹${val.toLocaleString('en-IN', { minimumFractionDigits: 2 })} (${pct}%)`;
                        }
                    }
                }
            },
            cutout: '68%'
        }
    });
}

/**
 * Renders a monochromatic Bar chart for multi-month Historical Trend series.
 */
export function renderMonthlyTrendBar(canvasId, trendPoints) {
    destroyChart(canvasId);

    const canvas = document.getElementById(canvasId);
    if (!canvas || !window.Chart) return;

    const labels = trendPoints.map(p => {
        const parts = p.month.split('-');
        if (parts.length === 2) {
            const m = parseInt(parts[1], 10) - 1;
            const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            return `${months[m]} ${parts[0]}`;
        }
        return p.month;
    });

    const data = trendPoints.map(p => Number(p.total));

    activeCharts[canvasId] = new window.Chart(canvas, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Monthly Expenditure',
                data: data,
                backgroundColor: '#111111',
                borderColor: '#111111',
                borderWidth: 1,
                borderRadius: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    grid: { display: false },
                    ticks: {
                        color: '#777777',
                        font: { size: 11 }
                    }
                },
                y: {
                    beginAtZero: true,
                    grid: {
                        color: '#EAEAEA',
                        borderDash: [3, 3]
                    },
                    ticks: {
                        color: '#777777',
                        font: { size: 11 },
                        callback: function(value) {
                            return '₹' + value.toLocaleString('en-IN');
                        }
                    }
                }
            },
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: '#111111',
                    callbacks: {
                        label: function(context) {
                            const val = context.raw || 0;
                            return ` Spending: ₹${val.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
                        }
                    }
                }
            }
        }
    });
}

/**
 * Renders a horizontal bar chart for Category Distribution.
 */
export function renderCategoryDistributionBar(canvasId, categories) {
    destroyChart(canvasId);

    const canvas = document.getElementById(canvasId);
    if (!canvas || !window.Chart) return;

    const sorted = [...categories].sort((a, b) => Number(b.totalAmount) - Number(a.totalAmount));
    const labels = sorted.map(c => c.categoryLabel);
    const data = sorted.map(c => Number(c.totalAmount));

    activeCharts[canvasId] = new window.Chart(canvas, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Amount Spent',
                data: data,
                backgroundColor: '#333333',
                borderRadius: 2
            }]
        },
        options: {
            indexAxis: 'y',
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    beginAtZero: true,
                    grid: { color: '#EAEAEA' },
                    ticks: {
                        color: '#777777',
                        callback: function(value) {
                            return '₹' + value.toLocaleString('en-IN');
                        }
                    }
                },
                y: {
                    grid: { display: false },
                    ticks: { color: '#333333', font: { size: 11 } }
                }
            },
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: '#111111',
                    callbacks: {
                        label: function(context) {
                            return ` Spent: ₹${context.raw.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
                        }
                    }
                }
            }
        }
    });
}
