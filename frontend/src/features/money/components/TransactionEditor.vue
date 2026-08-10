<template>
  <div class="card transaction-editor">
    <div class="header">
      <h2 class="title">{{ isEditing ? 'Edit Transaction' : 'New Transaction' }}</h2>

      <!-- Type Tabs (Expense & Income) -->
      <div v-if="!isEditing" class="type-tabs">
        <button
          type="button"
          class="tab-btn"
          :class="{ active: form.transaction_type === 'expense' }"
          @click="setTransactionType('expense')"
        >
          Expense
        </button>
        <button
          type="button"
          class="tab-btn"
          :class="{ active: form.transaction_type === 'income' }"
          @click="setTransactionType('income')"
        >
          Income
        </button>
      </div>
    </div>

    <form @submit.prevent="handleSubmit">
      <div class="form-grid">
        <!-- Amount -->
        <div class="field">
          <label for="txn-amount">Amount (IDR)</label>
          <input
            id="txn-amount"
            v-model="form.amount"
            type="number"
            step="0.01"
            min="0.01"
            placeholder="0.00"
            required
          />
        </div>

        <!-- Date -->
        <div class="field">
          <label for="txn-date">Date</label>
          <input id="txn-date" v-model="form.transaction_date" type="date" required />
        </div>

        <!-- Category -->
        <div class="field full-width">
          <label for="txn-category">Category</label>
          <select id="txn-category" v-model="form.category_id" required>
            <option value="" disabled>Select Category</option>
            <option
              v-for="cat in filteredCategories"
              :key="cat.id"
              :value="cat.id"
            >
              {{ cat.name }}
            </option>
          </select>
        </div>

        <!-- Description -->
        <div class="field full-width">
          <label for="txn-desc">Description (Optional)</label>
          <input
            id="txn-desc"
            v-model="form.description"
            type="text"
            placeholder="e.g. Weekly groceries, Coffee with friends"
            maxlength="1000"
          />
        </div>
      </div>

      <!-- Receipt Uploader (For existing transactions) -->
      <div v-if="isEditing && transaction" class="receipt-section">
        <label>Receipt Image</label>
        <ReceiptUploader
          :transaction-id="transaction.id"
          :receipt="transaction.receipt"
          @uploaded="$emit('receipt-updated')"
          @deleted="$emit('receipt-updated')"
        />
      </div>

      <p v-if="localError" class="notice">{{ localError }}</p>

      <div class="actions">
        <button type="submit" class="button primary" :disabled="saving">
          {{ saving ? 'Saving…' : isEditing ? 'Update Transaction' : 'Record Transaction' }}
        </button>
        <button type="button" class="button subtle" :disabled="saving" @click="$emit('cancel')">
          Cancel
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import ReceiptUploader from './ReceiptUploader.vue'
import { useAccounts } from '../composables/useAccounts'
import type {
  Category,
  Transaction,
  TransactionInput,
  TransactionType,
} from '../money.types'

const props = defineProps<{
  transaction?: Transaction | null
  categories: Category[]
  defaultAccountId: string
}>()

const emit = defineEmits<{
  save: [input: TransactionInput]
  cancel: []
  'receipt-updated': []
}>()

const isEditing = computed(() => Boolean(props.transaction))

const today = new Date().toISOString().slice(0, 10)
const initialDate = String(props.transaction?.transaction_date || today)

const form = reactive<TransactionInput>({
  transaction_type: props.transaction?.transaction_type ?? 'expense',
  amount: props.transaction?.amount ?? '',
  transaction_date: initialDate,
  account_id: props.transaction?.account_id ?? props.defaultAccountId,
  category_id: props.transaction?.category_id ?? '',
  description: props.transaction?.description ?? '',
})

const filteredCategories = computed(() => {
  return props.categories.filter(
    (c) => !c.archived_at && c.category_type === form.transaction_type,
  )
})

function setTransactionType(type: TransactionType) {
  form.transaction_type = type
  const firstCat = filteredCategories.value[0]
  if (firstCat) {
    form.category_id = firstCat.id
  }
}

const saving = ref(false)
const localError = ref<string | null>(null)

async function handleSubmit() {
  localError.value = null
  if (!form.amount || parseFloat(form.amount) <= 0) {
    localError.value = 'Please enter a valid positive amount.'
    return
  }
  if (!form.category_id) {
    localError.value = 'Please select a category.'
    return
  }

  saving.value = true
  try {
    let accId = props.defaultAccountId || form.account_id
    if (!accId) {
      const accountStore = useAccounts()
      accId = await accountStore.ensureDefaultAccount()
    }
    if (!accId) {
      localError.value = 'Account initializing. Please try saving again.'
      saving.value = false
      return
    }

    const payload: TransactionInput = {
      transaction_type: form.transaction_type,
      amount: String(form.amount),
      transaction_date: form.transaction_date,
      account_id: accId,
      category_id: form.category_id,
      description: form.description ? form.description.trim() : null,
    }
    emit('save', payload)
  } catch (e: unknown) {
    localError.value = e instanceof Error ? e.message : 'Failed to save transaction.'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.transaction-editor {
  padding: var(--space-5);
  margin-bottom: var(--space-5);
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}
.title {
  margin: 0;
  font-size: 1.2rem;
}
.type-tabs {
  display: flex;
  background: var(--surface-muted);
  padding: 3px;
  border-radius: 8px;
}
.tab-btn {
  padding: 0.35rem 0.8rem;
  border: 0;
  background: transparent;
  border-radius: 6px;
  font-weight: 600;
  font-size: var(--font-small);
  color: var(--text-muted);
}
.tab-btn.active {
  background: var(--surface);
  color: var(--accent);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
}
.full-width {
  grid-column: span 2;
}
.receipt-section {
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--border);
  display: grid;
  gap: var(--space-2);
}
.receipt-section label {
  font-weight: 650;
  font-size: var(--font-small);
}
.actions {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-4);
}
@media (max-width: 600px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  .full-width {
    grid-column: span 1;
  }
  .header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-3);
  }
}
</style>
