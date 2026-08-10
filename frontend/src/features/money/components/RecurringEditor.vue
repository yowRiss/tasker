<template>
  <div class="card recurring-editor">
    <h2 class="title">{{ isEditing ? 'Edit Recurring Template' : 'New Recurring Template' }}</h2>

    <form @submit.prevent="handleSubmit">
      <div class="form-grid">
        <!-- Type -->
        <div class="field">
          <label for="rec-type">Type</label>
          <select id="rec-type" v-model="form.transaction_type" required>
            <option value="expense">Expense</option>
            <option value="income">Income</option>
          </select>
        </div>

        <!-- Amount -->
        <div class="field">
          <label for="rec-amount">Amount (IDR)</label>
          <input
            id="rec-amount"
            v-model="form.amount"
            type="number"
            step="0.01"
            min="0.01"
            placeholder="0.00"
            required
          />
        </div>

        <!-- Category -->
        <div class="field">
          <label for="rec-category">Category</label>
          <select id="rec-category" v-model="form.category_id" required>
            <option value="" disabled>Select Category</option>
            <option v-for="cat in filteredCategories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </option>
          </select>
        </div>

        <!-- Cadence -->
        <div class="field">
          <label for="rec-cadence">Repeat Cadence</label>
          <select id="rec-cadence" v-model="form.cadence" required>
            <option value="weekly">Weekly</option>
            <option value="monthly">Monthly</option>
            <option value="yearly">Yearly</option>
          </select>
        </div>

        <!-- Next Due Date -->
        <div class="field">
          <label for="rec-due">Next Due Date</label>
          <input id="rec-due" v-model="form.next_due_date" type="date" required />
        </div>

        <!-- Ends On -->
        <div class="field">
          <label for="rec-ends">Ends On (Optional)</label>
          <input id="rec-ends" v-model="form.ends_on" type="date" />
        </div>

        <!-- Is Active -->
        <div class="field checkbox-field full-width">
          <label class="checkbox-label">
            <input v-model="form.is_active" type="checkbox" />
            Active (Generates due prompts)
          </label>
        </div>

        <!-- Description -->
        <div class="field full-width">
          <label for="rec-desc">Description (Optional)</label>
          <input
            id="rec-desc"
            v-model="form.description"
            type="text"
            placeholder="e.g. Monthly rent payment, Netflix subscription"
            maxlength="1000"
          />
        </div>
      </div>

      <p v-if="localError" class="notice">{{ localError }}</p>

      <div class="actions">
        <button type="submit" class="button primary" :disabled="saving">
          {{ saving ? 'Saving…' : isEditing ? 'Update Template' : 'Create Template' }}
        </button>
        <button type="button" class="button subtle" :disabled="saving" @click="$emit('cancel')">
          Cancel
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, watch } from 'vue'
import type {
  Category,
  CategoryType,
  RecurringCadence,
  RecurringTransaction,
  RecurringTransactionInput,
} from '../money.types'

const props = defineProps<{
  recurring?: RecurringTransaction | null
  categories: Category[]
  defaultAccountId: string
}>()

const emit = defineEmits<{
  save: [input: RecurringTransactionInput]
  cancel: []
}>()

const isEditing = computed(() => Boolean(props.recurring))
const today = new Date().toISOString().slice(0, 10)

const form = reactive<{
  transaction_type: CategoryType
  amount: string
  account_id: string
  category_id: string
  description: string
  cadence: RecurringCadence
  next_due_date: string
  ends_on: string
  is_active: boolean
}>({
  transaction_type: props.recurring?.transaction_type ?? 'expense',
  amount: props.recurring?.amount ?? '',
  account_id: props.recurring?.account_id ?? props.defaultAccountId,
  category_id: props.recurring?.category_id ?? '',
  description: props.recurring?.description ?? '',
  cadence: props.recurring?.cadence ?? 'monthly',
  next_due_date: String(props.recurring?.next_due_date || today),
  ends_on: props.recurring?.ends_on ?? '',
  is_active: props.recurring?.is_active ?? true,
})

const filteredCategories = computed(() =>
  props.categories.filter(
    (c) => !c.archived_at && c.category_type === form.transaction_type,
  ),
)

watch(
  () => form.transaction_type,
  () => {
    const match = filteredCategories.value.some((c) => c.id === form.category_id)
    if (!match) {
      form.category_id = filteredCategories.value[0]?.id ?? ''
    }
  },
)

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
  if (form.ends_on && form.ends_on < form.next_due_date) {
    localError.value = 'End date cannot be before next due date.'
    return
  }

  saving.value = true
  try {
    emit('save', {
      transaction_type: form.transaction_type,
      amount: String(form.amount),
      account_id: props.defaultAccountId || form.account_id,
      category_id: form.category_id,
      description: form.description.trim() || null,
      cadence: form.cadence,
      next_due_date: form.next_due_date,
      ends_on: form.ends_on || null,
      is_active: form.is_active,
    })
  } catch (e: unknown) {
    localError.value = e instanceof Error ? e.message : 'Failed to save recurring template.'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.recurring-editor {
  padding: var(--space-5);
  margin-bottom: var(--space-5);
}
.title {
  margin-top: 0;
  margin-bottom: var(--space-4);
  font-size: 1.2rem;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
}
.full-width {
  grid-column: span 2;
}
.checkbox-field {
  display: flex;
  align-items: flex-end;
}
.checkbox-label {
  display: flex;
  align-items: center;
  gap: var(--space-2);
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
}
</style>
