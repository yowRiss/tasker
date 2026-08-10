<template>
  <div class="accounts-grid">
    <div
      v-for="acc in accounts"
      :key="acc.id"
      class="card account-card"
      :class="{ archived: acc.archived_at }"
    >
      <div class="card-header">
        <div class="title-area">
          <span class="type-badge" :class="acc.account_type">
            {{ formatAccountType(acc.account_type) }}
          </span>
          <h3 class="account-name">{{ acc.name }}</h3>
        </div>
        <span v-if="acc.archived_at" class="tag archived-tag">Archived</span>
      </div>

      <div class="balance-area">
        <span class="balance-label">Current Balance</span>
        <div class="balance-amount">{{ formatIDR(acc.balance) }}</div>
      </div>

      <div class="card-actions">
        <button type="button" class="button subtle" @click="$emit('edit', acc)">Edit</button>
        <button
          v-if="!acc.archived_at"
          type="button"
          class="button subtle"
          @click="$emit('archive', acc.id)"
        >
          Archive
        </button>
        <button
          v-else
          type="button"
          class="button subtle"
          @click="$emit('unarchive', acc.id)"
        >
          Restore
        </button>
        <button type="button" class="button subtle danger" @click="$emit('delete', acc.id)">
          Delete
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Account, AccountType } from '../money.types'

defineProps<{
  accounts: Account[]
}>()

defineEmits<{
  edit: [account: Account]
  archive: [id: string]
  unarchive: [id: string]
  delete: [id: string]
}>()

function formatAccountType(type: AccountType): string {
  switch (type) {
    case 'cash':
      return '💵 Cash'
    case 'bank':
      return '🏦 Bank'
    case 'e_wallet':
      return '📱 E-Wallet'
    case 'credit_card':
      return '💳 Credit Card'
    default:
      return type
  }
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
.accounts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(18rem, 1fr));
  gap: var(--space-4);
}
.account-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: var(--space-4);
  transition: opacity 0.2s;
}
.account-card.archived {
  opacity: 0.6;
  background: var(--surface-muted);
}
.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.title-area {
  display: grid;
  gap: var(--space-1);
}
.type-badge {
  font-size: var(--font-small);
  font-weight: 600;
  color: var(--text-muted);
}
.account-name {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
}
.archived-tag {
  background: var(--border);
}
.balance-area {
  margin: var(--space-4) 0;
}
.balance-label {
  font-size: var(--font-small);
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.balance-amount {
  font-size: 1.4rem;
  font-weight: 800;
  color: var(--accent-strong);
  letter-spacing: -0.02em;
}
.card-actions {
  display: flex;
  gap: var(--space-2);
  border-top: 1px solid var(--border);
  padding-top: var(--space-3);
}
</style>
