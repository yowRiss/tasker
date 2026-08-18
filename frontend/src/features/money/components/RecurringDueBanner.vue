<template>
  <div v-if="dueItems.length" class="due-banner card">
    <div class="banner-header">
      <div class="title-row">
        <span class="bell-icon">🔔</span>
        <h3 class="banner-title">Due Recurring Transactions ({{ dueItems.length }})</h3>
      </div>
      <p class="banner-subtitle">
        Review pending occurrences below. Confirm to post the entry or skip to advance the schedule
        without posting.
      </p>
    </div>

    <div class="due-list">
      <div v-for="item in dueItems" :key="item.id" class="due-item">
        <div class="item-info">
          <span class="due-date">Due {{ formatDate(item.next_due_date) }}</span>
          <span class="item-title">
            {{ item.description || getCategoryName(item.category_id) }}
          </span>
          <span class="item-meta"> Category: {{ getCategoryName(item.category_id) }} </span>
        </div>

        <div class="item-amount" :class="item.transaction_type">
          {{ item.transaction_type === 'income' ? '+' : '-' }}{{ formatIDR(item.amount) }}
        </div>

        <div class="item-actions">
          <button
            type="button"
            class="button primary"
            :disabled="processingId === item.id"
            @click="handleConfirm(item.id)"
          >
            {{ processingId === item.id ? 'Posting…' : 'Confirm & Post' }}
          </button>
          <button
            type="button"
            class="button subtle"
            :disabled="processingId === item.id"
            @click="handleSkip(item.id)"
          >
            Skip
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { Category, RecurringTransaction } from '../money.types'

const props = defineProps<{
  dueItems: RecurringTransaction[]
  categories: Category[]
}>()

const emit = defineEmits<{
  confirm: [id: string]
  skip: [id: string]
}>()

const processingId = ref<string | null>(null)

function getCategoryName(id: string): string {
  return props.categories.find((c) => c.id === id)?.name || 'Category'
}

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
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

async function handleConfirm(id: string) {
  processingId.value = id
  try {
    emit('confirm', id)
  } finally {
    processingId.value = null
  }
}

async function handleSkip(id: string) {
  processingId.value = id
  try {
    emit('skip', id)
  } finally {
    processingId.value = null
  }
}
</script>

<style scoped>
.due-banner {
  border-left: 4px solid var(--accent);
  background: var(--accent-soft);
  padding: var(--space-4);
  margin-bottom: var(--space-5);
  display: grid;
  gap: var(--space-3);
}
.banner-header {
  display: grid;
  gap: 2px;
}
.title-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.bell-icon {
  font-size: 1.2rem;
}
.banner-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 750;
  color: var(--accent-strong);
}
.banner-subtitle {
  margin: 0;
  font-size: var(--font-small);
  color: var(--text-muted);
}
.due-list {
  display: grid;
  gap: var(--space-2);
}
.due-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3);
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.item-info {
  display: grid;
  gap: 2px;
}
.due-date {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--accent);
  text-transform: uppercase;
}
.item-title {
  font-weight: 700;
}
.item-meta {
  font-size: var(--font-small);
  color: var(--text-muted);
}
.item-amount {
  font-size: 1.2rem;
  font-weight: 800;
  margin: 0 var(--space-4);
}
.item-amount.income {
  color: var(--success);
}
.item-amount.expense {
  color: var(--danger);
}
.item-actions {
  display: flex;
  gap: var(--space-2);
}
@media (max-width: 600px) {
  .due-item {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-3);
  }
  .item-amount {
    margin: 0;
  }
  .item-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
