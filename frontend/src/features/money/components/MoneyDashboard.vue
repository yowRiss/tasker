<template>
  <div class="money-dashboard-view">
    <!-- Period Filter Toolbar -->
    <div class="card period-bar">
      <div class="presets-row">
        <span class="bar-label">Reporting Period:</span>
        <button
          type="button"
          class="button subtle"
          :class="{ active: preset === 'this_month' }"
          @click="applyPreset('this_month')"
        >
          This Month
        </button>
        <button
          type="button"
          class="button subtle"
          :class="{ active: preset === 'last_month' }"
          @click="applyPreset('last_month')"
        >
          Last Month
        </button>
        <button
          type="button"
          class="button subtle"
          :class="{ active: preset === 'this_quarter' }"
          @click="applyPreset('this_quarter')"
        >
          This Quarter
        </button>
        <button
          type="button"
          class="button subtle"
          :class="{ active: preset === 'this_year' }"
          @click="applyPreset('this_year')"
        >
          This Year
        </button>
      </div>

      <div class="date-controls">
        <div class="field-inline">
          <label for="dash-start">From</label>
          <input id="dash-start" v-model="startDate" type="date" @change="onCustomDateChange" />
        </div>
        <div class="field-inline">
          <label for="dash-end">To</label>
          <input id="dash-end" v-model="endDate" type="date" @change="onCustomDateChange" />
        </div>
        <div class="field-inline">
          <label for="dash-group">Group By</label>
          <select id="dash-group" v-model="groupBy" @change="reload">
            <option value="day">Day</option>
            <option value="week">Week</option>
            <option value="month">Month</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Error Notice -->
    <p v-if="error" class="notice">{{ error }}</p>
    <p v-else-if="loading" class="empty">Loading financial report aggregates…</p>

    <template v-else-if="dashboard">
      <!-- Summary Metric Cards -->
      <div class="metrics-grid">
        <div class="card metric-card">
          <span class="metric-label">Total Account Balance</span>
          <div class="metric-val total">{{ formatIDR(dashboard.total_balance) }}</div>
          <span class="metric-help">Across all active accounts</span>
        </div>

        <div class="card metric-card">
          <span class="metric-label">Period Income</span>
          <div class="metric-val income">+{{ formatIDR(dashboard.income) }}</div>
          <span class="metric-help">Posted income entries</span>
        </div>

        <div class="card metric-card">
          <span class="metric-label">Period Expenses</span>
          <div class="metric-val expense">-{{ formatIDR(dashboard.expense) }}</div>
          <span class="metric-help">Posted expense entries</span>
        </div>

        <div class="card metric-card">
          <span class="metric-label">Net Savings / Cash Flow</span>
          <div class="metric-val" :class="parseFloat(netSavings) >= 0 ? 'income' : 'expense'">
            {{ parseFloat(netSavings) >= 0 ? '+' : '' }}{{ formatIDR(netSavings) }}
          </div>
          <span class="metric-help">Income minus expenses</span>
        </div>
      </div>

      <!-- Savings Targets Summary Card in Dashboard -->
      <div
        v-if="dashboard.target_summary && dashboard.target_summary.total_targets_count > 0"
        class="card target-dashboard-card"
      >
        <div class="target-dash-header">
          <div>
            <span class="metric-label">Savings & Target Goals</span>
            <div class="target-dash-amounts">
              <strong class="total-saved-text">
                {{ formatIDR(dashboard.target_summary.total_current_amount) }}
              </strong>
              <span class="muted">
                / {{ formatIDR(dashboard.target_summary.total_target_amount) }}
              </span>
            </div>
          </div>
          <div class="target-dash-stats">
            <span class="tag status-active">
              {{ dashboard.target_summary.active_targets_count }} Active
            </span>
            <span
              v-if="dashboard.target_summary.achieved_targets_count > 0"
              class="tag status-achieved"
            >
              {{ dashboard.target_summary.achieved_targets_count }} Achieved 🎉
            </span>
          </div>
        </div>

        <div class="target-dash-bar">
          <div
            class="target-dash-fill"
            :style="{
              width: `${Math.min(100, parseFloat(dashboard.target_summary.overall_progress) || 0)}%`,
            }"
          ></div>
        </div>
        <div class="target-dash-meta">
          <span>Overall Progress: {{ dashboard.target_summary.overall_progress }}%</span>
          <span>
            Remaining:
            {{
              formatIDR(
                String(
                  Math.max(
                    0,
                    parseFloat(dashboard.target_summary.total_target_amount) -
                      parseFloat(dashboard.target_summary.total_current_amount),
                  ),
                ),
              )
            }}
          </span>
        </div>
      </div>

      <!-- Reports Visualization Grid -->
      <div class="reports-grid">
        <SpendingByCategory
          :category-spend="dashboard.category_spend"
          :total-expense="totalExpense"
        />

        <TrendChart :trend="dashboard.trend" />
      </div>
    </template>
  </div>
</template>


<script setup lang="ts">
import { onMounted, watch } from 'vue'
import SpendingByCategory from './SpendingByCategory.vue'
import TrendChart from './TrendChart.vue'
import { useMoneyDashboard } from '../composables/useMoneyDashboard'

const {
  dashboard,
  loading,
  error,
  preset,
  startDate,
  endDate,
  groupBy,
  netSavings,
  totalExpense,
  applyPreset: setPreset,
  load: reload,
} = useMoneyDashboard()

function applyPreset(p: 'this_month' | 'last_month' | 'this_quarter' | 'this_year' | 'custom') {
  setPreset(p)
  void reload()
}

function onCustomDateChange() {
  preset.value = 'custom'
}

onMounted(() => void reload())

watch([startDate, endDate, groupBy], () => {
  void reload()
})

function formatIDR(valStr: string): string {
  const val = parseFloat(valStr || '0')
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    maximumFractionDigits: 2,
  }).format(val)
}
</script>

<style scoped>
.money-dashboard-view {
  display: grid;
  gap: var(--space-5);
}
.period-bar {
  padding: var(--space-3) var(--space-4);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}
.presets-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}
.bar-label {
  font-weight: 700;
  font-size: var(--font-small);
  color: var(--text-muted);
}
.presets-row button.active {
  background: var(--accent-soft);
  color: var(--accent);
  border-color: var(--accent);
}
.date-controls {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-wrap: wrap;
}
.field-inline {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-small);
  font-weight: 650;
}
.field-inline input,
.field-inline select {
  padding: 0.4rem 0.6rem;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: white;
}
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 14rem), 1fr));
  gap: var(--space-4);
}
.metric-card {
  padding: var(--space-4);
  display: grid;
  gap: var(--space-1);
}
.metric-label {
  font-size: var(--font-small);
  font-weight: 650;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.metric-val {
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -0.02em;
}
.metric-val.total {
  color: var(--accent-strong);
}
.metric-val.income {
  color: var(--success);
}
.metric-val.expense {
  color: var(--danger);
}
.metric-help {
  font-size: 0.75rem;
  color: var(--text-muted);
}
.reports-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 22rem), 1fr));
  gap: var(--space-5);
}
@media (max-width: 800px) {
  .period-bar {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-3);
  }
  .presets-row,
  .date-controls {
    width: 100%;
  }
}
@media (max-width: 500px) {
  .date-controls {
    flex-direction: column;
    align-items: stretch;
  }
  .field-inline {
    justify-content: space-between;
  }
  .field-inline input,
  .field-inline select {
    flex: 1;
  }
}
.target-dashboard-card {
  padding: var(--space-4);
  display: grid;
  gap: var(--space-3);
  background: var(--surface);
  border-left: 4px solid var(--accent);
}
.target-dash-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: var(--space-2);
}
.target-dash-amounts {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-top: 2px;
}
.total-saved-text {
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--accent);
}
.target-dash-stats {
  display: flex;
  gap: var(--space-2);
}
.target-dash-bar {
  height: 0.65rem;
  background: var(--surface-muted);
  border-radius: 999px;
  overflow: hidden;
}
.target-dash-fill {
  height: 100%;
  background: var(--accent);
  border-radius: 999px;
  transition: width 0.3s ease;
}
.target-dash-meta {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-small);
  color: var(--text-muted);
}
.tag {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 700;
}
.status-active {
  background: #e0f2fe;
  color: #0284c7;
}
.status-achieved {
  background: #dcfce7;
  color: #16a34a;
}
</style>

