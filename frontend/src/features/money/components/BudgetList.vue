<template>
  <div class="budget-grid">
    <div
      v-for="bgt in budgets"
      :key="bgt.id"
      class="card budget-card"
      :class="{ over: bgt.is_over_budget }"
    >
      <div class="card-header">
        <div class="header-main">
          <h3 class="category-name">{{ bgt.category_name }}</h3>
          <span class="period-dates">
            {{ formatDate(bgt.period_start) }} – {{ formatDate(bgt.period_end) }}
          </span>
        </div>
        <span v-if="bgt.is_over_budget" class="tag over-tag">⚠️ Over Budget</span>
      </div>

      <div class="progress-section">
        <div class="amounts-row">
          <span class="spent">Spent: <strong>{{ formatIDR(bgt.spent) }}</strong></span>
          <span class="limit">Limit: <strong>{{ formatIDR(bgt.amount_limit) }}</strong></span>
        </div>

        <div class="progress-bar-track">
          <div
            class="progress-bar-fill"
            :class="getProgressClass(bgt)"
            :style="{ width: getProgressWidth(bgt) }"
          ></div>
        </div>

        <div class="meta-row">
          <span class="percent">{{ bgt.percent_used }}% used</span>
          <span class="remaining" :class="{ negative: bgt.is_over_budget }">
            {{ bgt.is_over_budget ? 'Over by ' : 'Remaining: ' }}
            {{ formatIDR(bgt.is_over_budget ? Math.abs(parseFloat(bgt.remaining)).toString() : bgt.remaining) }}
          </span>
        </div>
      </div>

      <div class="card-actions">
        <button type="button" class="button subtle" @click="$emit('edit', bgt)">Edit</button>
        <button type="button" class="button subtle danger" @click="$emit('delete', bgt.id)">
          Delete
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Budget } from '../money.types'

defineProps<{
  budgets: Budget[]
}>()

defineEmits<{
  edit: [budget: Budget]
  delete: [id: string]
}>()

function getProgressWidth(bgt: Budget): string {
  const pct = parseFloat(bgt.percent_used || '0')
  return `${Math.min(pct, 100)}%`
}

function getProgressClass(bgt: Budget): string {
  if (bgt.is_over_budget) return 'red'
  const pct = parseFloat(bgt.percent_used || '0')
  if (pct >= 80) return 'orange'
  return 'green'
}

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(d)
}

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
.budget-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 20rem), 1fr));
  gap: var(--space-4);
}
.budget-card {
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: var(--space-4);
}
.budget-card.over {
  border-color: var(--danger);
  background: #fff8f8;
}
.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.header-main {
  display: grid;
  gap: 2px;
}
.category-name {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 750;
}
.period-dates {
  font-size: var(--font-small);
  color: var(--text-muted);
}
.over-tag {
  background: var(--danger-soft);
  color: var(--danger);
  font-weight: 700;
}
.progress-section {
  display: grid;
  gap: var(--space-2);
}
.amounts-row {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-small);
}
.progress-bar-track {
  height: 0.65rem;
  background: var(--surface-muted);
  border-radius: 999px;
  overflow: hidden;
}
.progress-bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s ease;
}
.progress-bar-fill.green {
  background: var(--success);
}
.progress-bar-fill.orange {
  background: #e67e22;
}
.progress-bar-fill.red {
  background: var(--danger);
}
.meta-row {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-small);
  color: var(--text-muted);
}
.remaining.negative {
  color: var(--danger);
  font-weight: 700;
}
.card-actions {
  display: flex;
  gap: var(--space-2);
  border-top: 1px solid var(--border);
  padding-top: var(--space-3);
}
</style>
