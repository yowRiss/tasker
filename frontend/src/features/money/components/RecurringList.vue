<template>
  <div class="recurring-grid">
    <div
      v-for="rec in items"
      :key="rec.id"
      class="card recurring-card"
      :class="{ inactive: !rec.is_active }"
    >
      <div class="card-header">
        <div class="header-main">
          <span class="cadence-badge" :class="rec.cadence">
            🔄 {{ capitalize(rec.cadence) }}
          </span>
          <h3 class="rec-title">{{ rec.description || getCategoryName(rec.category_id) }}</h3>
        </div>
        <span class="tag" :class="rec.is_active ? 'active-tag' : 'paused-tag'">
          {{ rec.is_active ? 'Active' : 'Paused' }}
        </span>
      </div>

      <div class="details-section">
        <div class="amount-row" :class="rec.transaction_type">
          <span class="sign">{{ rec.transaction_type === 'income' ? '+' : '-' }}</span>
          {{ formatIDR(rec.amount) }}
        </div>

        <div class="meta-row">
          <span>Category: <strong>{{ getCategoryName(rec.category_id) }}</strong></span>
        </div>

        <div class="dates-row">
          <span>Next Due: <strong>{{ formatDate(rec.next_due_date) }}</strong></span>
          <span v-if="rec.ends_on">Ends: {{ formatDate(rec.ends_on) }}</span>
        </div>
      </div>

      <div class="card-actions">
        <button type="button" class="button subtle" @click="$emit('edit', rec)">Edit</button>
        <button type="button" class="button subtle danger" @click="$emit('delete', rec.id)">
          Delete
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Category, RecurringTransaction } from '../money.types'

const props = defineProps<{
  items: RecurringTransaction[]
  categories: Category[]
}>()

defineEmits<{
  edit: [item: RecurringTransaction]
  delete: [id: string]
}>()

function getCategoryName(id: string): string {
  return props.categories.find((c) => c.id === id)?.name || 'Category'
}

function capitalize(val: string): string {
  return val.charAt(0).toUpperCase() + val.slice(1)
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
.recurring-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(19rem, 1fr));
  gap: var(--space-4);
}
.recurring-card {
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: var(--space-4);
}
.recurring-card.inactive {
  opacity: 0.65;
  background: var(--surface-muted);
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
.cadence-badge {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--text-muted);
}
.rec-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 750;
}
.active-tag {
  background: var(--accent-soft);
  color: var(--accent);
}
.paused-tag {
  background: var(--border);
  color: var(--text-muted);
}
.details-section {
  display: grid;
  gap: var(--space-2);
}
.amount-row {
  font-size: 1.3rem;
  font-weight: 800;
}
.amount-row.income {
  color: var(--success);
}
.amount-row.expense {
  color: var(--danger);
}
.meta-row,
.dates-row {
  display: flex;
  justify-content: space-between;
  gap: var(--space-2);
  font-size: var(--font-small);
  color: var(--text-muted);
}
.card-actions {
  display: flex;
  gap: var(--space-2);
  border-top: 1px solid var(--border);
  padding-top: var(--space-3);
}
</style>
