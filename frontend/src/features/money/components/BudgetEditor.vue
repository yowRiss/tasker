<template>
  <div class="card budget-editor">
    <h2 class="title">{{ isEditing ? 'Edit Budget' : 'New Budget' }}</h2>

    <form @submit.prevent="handleSubmit">
      <div class="form-grid">
        <!-- Category -->
        <div class="field">
          <label for="bgt-cat">Expense Category</label>
          <select id="bgt-cat" v-model="form.category_id" required>
            <option value="" disabled>Select Expense Category</option>
            <option
              v-for="cat in expenseCategories"
              :key="cat.id"
              :value="cat.id"
            >
              {{ cat.name }}
            </option>
          </select>
        </div>

        <!-- Limit -->
        <div class="field">
          <label for="bgt-limit">Limit (IDR)</label>
          <input
            id="bgt-limit"
            v-model="form.amount_limit"
            type="number"
            step="0.01"
            min="0.01"
            placeholder="e.g. 5000000"
            required
          />
        </div>

        <!-- Quick Period Presets -->
        <div class="field full-width">
          <label>Period Presets</label>
          <div class="presets">
            <button type="button" class="button subtle" @click="setThisMonth">This Month</button>
            <button type="button" class="button subtle" @click="setNextMonth">Next Month</button>
          </div>
        </div>

        <!-- Start Date -->
        <div class="field">
          <label for="bgt-start">Start Date</label>
          <input id="bgt-start" v-model="form.period_start" type="date" required />
        </div>

        <!-- End Date -->
        <div class="field">
          <label for="bgt-end">End Date</label>
          <input id="bgt-end" v-model="form.period_end" type="date" required />
        </div>
      </div>

      <p v-if="localError" class="notice">{{ localError }}</p>

      <div class="actions">
        <button type="submit" class="button primary" :disabled="saving">
          {{ saving ? 'Saving…' : isEditing ? 'Update Budget' : 'Create Budget' }}
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
import type { Budget, BudgetInput, Category } from '../money.types'

const props = defineProps<{
  budget?: Budget | null
  categories: Category[]
}>()

const emit = defineEmits<{
  save: [input: BudgetInput]
  cancel: []
}>()

const isEditing = computed(() => Boolean(props.budget))

const now = new Date()
const firstDayThisMonth = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10)
const lastDayThisMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().slice(0, 10)

const form = reactive<BudgetInput>({
  category_id: props.budget?.category_id ?? '',
  period_start: props.budget?.period_start || firstDayThisMonth,
  period_end: props.budget?.period_end || lastDayThisMonth,
  amount_limit: props.budget?.amount_limit ?? '',
})

const expenseCategories = computed(() =>
  props.categories.filter((c) => !c.archived_at && c.category_type === 'expense'),
)

function setThisMonth() {
  const d = new Date()
  form.period_start = new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10)
  form.period_end = new Date(d.getFullYear(), d.getMonth() + 1, 0).toISOString().slice(0, 10)
}

function setNextMonth() {
  const d = new Date()
  form.period_start = new Date(d.getFullYear(), d.getMonth() + 1, 1).toISOString().slice(0, 10)
  form.period_end = new Date(d.getFullYear(), d.getMonth() + 2, 0).toISOString().slice(0, 10)
}

const saving = ref(false)
const localError = ref<string | null>(null)

async function handleSubmit() {
  localError.value = null
  if (!form.category_id) {
    localError.value = 'Expense category is required.'
    return
  }
  if (!form.amount_limit || parseFloat(form.amount_limit) <= 0) {
    localError.value = 'Please enter a valid positive budget limit.'
    return
  }
  if (form.period_end < form.period_start) {
    localError.value = 'End date cannot be before start date.'
    return
  }

  saving.value = true
  try {
    emit('save', {
      category_id: form.category_id,
      period_start: form.period_start,
      period_end: form.period_end,
      amount_limit: String(form.amount_limit),
    })
  } catch (e: unknown) {
    localError.value = e instanceof Error ? e.message : 'Failed to save budget.'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.budget-editor {
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
.presets {
  display: flex;
  gap: var(--space-2);
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
