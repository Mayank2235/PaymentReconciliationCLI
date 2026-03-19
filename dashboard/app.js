/**
 * PayRecon Pro — Dashboard Live Engine
 * Polls backend every 5s and updates all UI sections.
 */

const API_BASE = 'http://localhost:8080/api';
const POLL_INTERVAL = 5000; // 5 seconds

// ── State ──────────────────────────────────────────────────────────
let activeTab = 'all';
let allReconciliations = [];
let activityLogEntries = [];

// ── DOMContentLoaded ───────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    startClock();
    bindTabs();
    poll(); // immediate first call
    setInterval(poll, POLL_INTERVAL);
});

// ── Clock ─────────────────────────────────────────────────────────
function startClock() {
    const clockEl = document.getElementById('liveClock');
    const dateEl = document.getElementById('liveDate');
    function tick() {
        const now = new Date();
        clockEl.textContent = now.toLocaleTimeString('en-GB', { hour12: false });
        dateEl.textContent = now.toLocaleDateString('en-GB', { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric' });
    }
    tick();
    setInterval(tick, 1000);
}

// ── Tab Binding ───────────────────────────────────────────────────
function bindTabs() {
    document.querySelectorAll('.tab').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
            btn.classList.add('active');
            activeTab = btn.dataset.tab;
            renderReconTable(allReconciliations);
        });
    });
}

// ── Main Poll Cycle ───────────────────────────────────────────────
async function poll() {
    try {
        const [dashboard, invoices, payments, reconciliations] = await Promise.all([
            fetchJSON(`${API_BASE}/dashboard`),
            fetchJSON(`${API_BASE}/invoices/recent`),
            fetchJSON(`${API_BASE}/payments/recent`),
            fetchJSON(`${API_BASE}/reconciliations/recent`),
        ]);

        setConnected(true);
        updateKPIs(dashboard);
        updateInvoiceFeed(invoices);
        updatePaymentFeed(payments);
        allReconciliations = reconciliations;
        renderReconTable(reconciliations);
        updateStatusBar(dashboard);

        // Also try to fetch latest report
        try {
            const report = await fetchJSON(`${API_BASE}/reports/latest`);
            updateReportPanel(report);
        } catch (_) { /* report might not exist yet */ }

    } catch (err) {
        setConnected(false);
        console.error('Poll error:', err);
    }
}

// ── Status Indicator ──────────────────────────────────────────────
function setConnected(connected) {
    const pill = document.getElementById('statusPill');
    const text = document.getElementById('statusText');
    pill.className = 'status-pill ' + (connected ? 'connected' : 'error');
    text.textContent = connected ? 'Live — Connected to Backend' : 'Disconnected — retrying…';
}

// ── KPI Updates ───────────────────────────────────────────────────
function updateKPIs(d) {
    animateNumber('kpiTotalInvoices', d.totalInvoices);
    animateNumber('kpiTotalPayments', d.totalPayments);
    setText('kpiInvoiceAmt', formatCurrency(d.totalInvoiceAmount));
    setText('kpiPaymentAmt', formatCurrency(d.totalPaymentReceived));
    setText('kpiMatchedAmt', formatCurrency(d.matchedAmount));
    animateNumber('kpiFullyMatched', d.fullyMatched);
    animateNumber('kpiPartial', d.partiallyMatched);
    setText('kpiOutstanding', formatCurrency(d.outstandingBalance));
    animateNumber('kpiUnmatchedInv', d.unmatchedInvoices);
}

// ── Invoice Feed ─────────────────────────────────────────────────
function updateInvoiceFeed(invoices) {
    document.getElementById('badgeInvoices').textContent = invoices.length;
    const feed = document.getElementById('invoiceFeed');
    if (!invoices.length) { feed.innerHTML = '<div class="feed-empty">No invoices yet…</div>'; return; }
    feed.innerHTML = invoices.slice(0, 20).map(inv => `
    <div class="feed-item">
      <div class="feed-num">${esc(inv.invoiceNumber)}</div>
      <div class="feed-amt">${formatCurrency(inv.amount)}</div>
      <div class="feed-meta">${esc(inv.vendor || '—')} · ${esc(inv.date || '')}</div>
    </div>
  `).join('');
}

// ── Payment Feed ──────────────────────────────────────────────────
function updatePaymentFeed(payments) {
    document.getElementById('badgePayments').textContent = payments.length;
    const feed = document.getElementById('paymentFeed');
    if (!payments.length) { feed.innerHTML = '<div class="feed-empty">No payments yet…</div>'; return; }
    feed.innerHTML = payments.slice(0, 20).map(p => `
    <div class="feed-item">
      <div class="feed-num">${esc(p.transactionId)}</div>
      <div class="feed-amt">${formatCurrency(p.amount)}</div>
      <div class="feed-meta">${esc(p.referenceNote ? p.referenceNote.substring(0, 40) : '—')} · <span class="status-badge ${badgeClass(p.status)}">${esc(p.status)}</span></div>
    </div>
  `).join('');
}

// ── Reconciliation Table ──────────────────────────────────────────
function renderReconTable(rows) {
    const tbody = document.getElementById('reconTableBody');
    let filtered = rows;
    if (activeTab === 'matched') filtered = rows.filter(r => r.matchType === 'MATCHED');
    if (activeTab === 'partial') filtered = rows.filter(r => r.matchType === 'PARTIAL' || r.matchType === 'OVERPAID');
    if (activeTab === 'unmatched') filtered = rows.filter(r => r.matchType === 'UNMATCHED' || r.matchType === 'DUPLICATE');

    if (!filtered.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty-cell">No records for this filter…</td></tr>';
        return;
    }

    tbody.innerHTML = filtered.map(r => {
        const diff = r.difference || 0;
        const diffClass = diff > 0 ? 'amber' : diff < 0 ? 'red' : 'green';
        const diffStr = diff !== 0 ? formatCurrency(Math.abs(diff)) : '—';
        const time = r.reconciledAt ? new Date(r.reconciledAt).toLocaleTimeString('en-GB', { hour12: false }) : '—';
        return `
      <tr>
        <td class="mono">${esc(r.invoiceNumber || '—')}</td>
        <td class="mono">${esc(r.transactionId || '—')}</td>
        <td>${formatCurrency(r.invoiceAmount)}</td>
        <td>${formatCurrency(r.paymentAmount)}</td>
        <td class="${diffClass}">${diffStr}</td>
        <td><span class="status-badge ${badgeClass(r.matchType)}">${esc(r.matchType)}</span></td>
        <td style="color:var(--gray-400);font-family:var(--mono);font-size:11px">${time}</td>
      </tr>
    `;
    }).join('');
}

// ── Status Bar ────────────────────────────────────────────────────
function updateStatusBar(d) {
    animateNumber('countMatched', d.fullyMatched);
    animateNumber('countPartial', d.partiallyMatched);
    animateNumber('countUnmatched', d.unmatchedInvoices);
    animateNumber('countOverpaid', d.overpaid || 0);
    animateNumber('countDuplicate', d.duplicates || 0);
}

// ── Report Panel ─────────────────────────────────────────────────
function updateReportPanel(r) {
    if (!r) return;
    setText('rptTotalInvoices', r.totalInvoices);
    setText('rptTotalPayments', r.totalPayments);
    setText('rptFullyMatched', r.fullyMatched);
    setText('rptPartial', r.partiallyMatched);
    setText('rptUnmatched', r.unmatched);
    setText('rptInvoiceTotal', formatCurrency(r.totalInvoiceAmount));
    setText('rptReceived', formatCurrency(r.totalPaymentReceived));
    setText('rptMatchedAmt', formatCurrency(r.totalMatchedAmount));
    setText('rptOutstanding', formatCurrency(r.totalOutstandingBalance));

    const time = r.generatedAt
        ? new Date(r.generatedAt).toLocaleTimeString('en-GB', { hour12: false })
        : '--';
    setText('badgeReportTime', time);

    addActivityLog(`Report generated: ${r.fullyMatched} matched, $${formatNumber(r.totalMatchedAmount)} cleared`);
}

// ── Activity Log ──────────────────────────────────────────────────
function addActivityLog(msg) {
    const log = document.getElementById('activityLog');
    const time = new Date().toLocaleTimeString('en-GB', { hour12: false });

    activityLogEntries.unshift({ time, msg });
    if (activityLogEntries.length > 30) activityLogEntries.pop();

    log.innerHTML = activityLogEntries.map(e => `
    <div class="log-entry">
      <span class="log-time">${esc(e.time)}</span>
      <span class="log-msg">${esc(e.msg)}</span>
    </div>
  `).join('');
}

// ── Helpers ───────────────────────────────────────────────────────
async function fetchJSON(url) {
    const res = await fetch(url, { cache: 'no-store' });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
}

function setText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val ?? '—';
}

function animateNumber(id, target) {
    const el = document.getElementById(id);
    if (!el) return;
    const current = parseInt(el.textContent.replace(/,/g, '')) || 0;
    const diff = (target || 0) - current;
    if (diff === 0) return;
    let start = null;
    const step = ts => {
        if (!start) start = ts;
        const prog = Math.min((ts - start) / 400, 1);
        el.textContent = Math.round(current + diff * easeOut(prog)).toLocaleString();
        if (prog < 1) requestAnimationFrame(step);
        else el.textContent = (target || 0).toLocaleString();
    };
    requestAnimationFrame(step);
}

function easeOut(t) { return 1 - Math.pow(1 - t, 3); }

function formatCurrency(val) {
    if (val == null) return '$0.00';
    return '$' + parseFloat(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatNumber(val) {
    if (val == null) return '0.00';
    return parseFloat(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function badgeClass(status) {
    const map = {
        MATCHED: 'sb-matched',
        PARTIAL: 'sb-partial',
        UNMATCHED: 'sb-unmatched',
        OVERPAID: 'sb-overpaid',
        DUPLICATE: 'sb-duplicate',
    };
    return map[status] || 'sb-unmatched';
}

function esc(val) {
    if (val == null) return '';
    return String(val)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}
