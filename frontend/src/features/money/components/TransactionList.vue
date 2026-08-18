<template>
  <div class="transaction-list card">
    <div v-if="!transactions.length" class="empty">No transactions recorded.</div>

    <div v-else class="list-table">
      <div v-for="txn in transactions" :key="txn.id" class="txn-row" :class="txn.transaction_type">
        <div class="date-col">
          <span class="txn-date">{{ formatDate(txn.transaction_date) }}</span>
          <span class="type-pill" :class="txn.transaction_type">
            {{ txn.transaction_type }}
          </span>
        </div>

        <div class="details-col">
          <div class="main-info">
            <span class="description">{{
              txn.description || txn.category_name || 'Transaction'
            }}</span>
            <span v-if="txn.receipt" class="receipt-pill" title="Has attached receipt">
              📎 Receipt
            </span>
          </div>

          <div class="meta-info">
            <span class="cat-tag">{{ txn.category_name }}</span>
          </div>
        </div>

        <div class="amount-col" :class="txn.transaction_type">
          <span class="sign">
            {{ txn.transaction_type === 'income' ? '+' : '-' }}
          </span>
          {{ formatIDR(txn.amount) }}
        </div>

        <div class="actions-col">
          <button type="button" class="button subtle" @click="$emit('edit', txn)">Edit</button>
          <button type="button" class="button subtle danger" @click="$emit('delete', txn.id)">
            Delete
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Transaction } from '../money.types'

defineProps<{
  transactions: Transaction[]
}>()

defineEmits<{
  edit: [txn: Transaction]
  delete: [id: string]
}>()

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
.transaction-list {
  overflow: hidden;
}
.list-table {
  display: grid;
}
.txn-row {
  display: grid;
  grid-template-columns: 8rem minmax(0, 1fr) 11rem 7.5rem;
  align-items: center;
  gap: var(--space-3);
  padding: 0.85rem var(--space-4);
  border-bottom: 1px solid var(--border);
  transition: background 0.15s;
}
.txn-row:last-child {
  border-bottom: 0;
}
.txn-row:hover {
  background: var(--surface-muted);
}
.date-col {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.txn-date {
  font-size: var(--font-small);
  font-weight: 600;
}
.type-pill {
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--text-muted);
}
.type-pill.income {
  color: var(--success);
}
.type-pill.expense {
  color: var(--danger);
}
.details-col {
  display: grid;
  gap: 2px;
  min-width: 0;
}
.main-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.description {
  font-weight: 650;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.receipt-pill {
  font-size: 0.75rem;
  background: var(--accent-soft);
  color: var(--accent);
  padding: 1px 6px;
  border-radius: 4px;
}
.meta-info {
  font-size: var(--font-small);
  color: var(--text-muted);
}
.cat-tag {
  font-weight: 600;
}
.amount-col {
  font-size: 1.1rem;
  font-weight: 750;
  text-align: right;
}
.amount-col.income {
  color: var(--success);
}
.amount-col.expense {
  color: var(--danger);
}
.actions-col {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-1);
}
@media (max-width: 720px) {
  .txn-row {
    grid-template-columns: 1fr auto;
    grid-template-areas:
      'details amount'
      'date actions';
    gap: var(--space-2);
    padding: var(--space-3);
  }
  .date-col {
    grid-area: date;
    flex-direction: row;
    align-items: center;
    gap: var(--space-2);
  }
  .details-col {
    grid-area: details;
  }
  .amount-col {
    grid-area: amount;
  }
  .actions-col {
    grid-area: actions;
  }
}
@media (max-width: 480px) {
  .txn-row {
    grid-template-columns: 1fr;
    grid-template-areas:
      'details'
      'amount'
      'date'
      'actions';
  }
  .amount-col {
    text-align: left;
  }
  .actions-col {
    justify-content: flex-start;
  }
}
</style>
