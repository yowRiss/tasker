<template>
  <div class="card target-editor">
    <h2 class="title">{{ isEditing ? 'Edit Savings Target' : 'New Savings Target' }}</h2>

    <form @submit.prevent="handleSubmit">
      <div class="form-grid">
        <!-- Target Name -->
        <div class="field full-width">
          <label for="tgt-name">Target / Goal Name</label>
          <input
            id="tgt-name"
            v-model="form.name"
            type="text"
            placeholder="e.g. Emergency Fund, New Laptop, Holiday Trip"
            required
            maxlength="80"
          />
        </div>

        <!-- Target Amount -->
        <div class="field">
          <label for="tgt-amount">Target Amount (IDR)</label>
          <input
            id="tgt-amount"
            v-model="form.target_amount"
            type="number"
            step="0.01"
            min="0.01"
            placeholder="e.g. 15000000"
            required
          />
        </div>

        <!-- Current Saved Amount -->
        <div class="field">
          <label for="tgt-curr">Currently Saved Amount (IDR)</label>
          <input
            id="tgt-curr"
            v-model="form.current_amount"
            type="number"
            step="0.01"
            min="0"
            placeholder="e.g. 2000000"
          />
        </div>

        <!-- Target Date -->
        <div class="field">
          <label for="tgt-date">Target Deadline (Optional)</label>
          <input id="tgt-date" v-model="form.target_date" type="date" />
        </div>

        <!-- Status -->
        <div class="field">
          <label for="tgt-status">Status</label>
          <select id="tgt-status" v-model="form.status">
            <option value="active">Active</option>
            <option value="achieved">Achieved 🎉</option>
            <option value="paused">Paused</option>
            <option value="cancelled">Cancelled</option>
          </select>
        </div>

        <!-- Linked Category -->
        <div class="field">
          <label for="tgt-cat">Linked Category (Optional)</label>
          <select id="tgt-cat" v-model="form.category_id">
            <option value="">None</option>
            <option v-for="cat in activeCategories" :key="cat.id" :value="cat.id">
              {{ cat.name }} ({{ cat.category_type }})
            </option>
          </select>
        </div>

        <!-- Linked Account -->
        <div class="field">
          <label for="tgt-acc">Linked Account (Optional)</label>
          <select id="tgt-acc" v-model="form.account_id">
            <option value="">None</option>
            <option v-for="acc in activeAccounts" :key="acc.id" :value="acc.id">
              {{ acc.name }} ({{ acc.account_type }})
            </option>
          </select>
        </div>

        <!-- Color Preset -->
        <div class="field full-width">
          <label>Badge Color</label>
          <div class="color-options">
            <button
              v-for="c in colorPresets"
              :key="c"
              type="button"
              class="color-btn"
              :style="{ background: c }"
              :class="{ selected: form.color === c }"
              @click="form.color = c"
            ></button>
          </div>
        </div>

        <!-- Notes -->
        <div class="field full-width">
          <label for="tgt-notes">Notes / Milestones (Optional)</label>
          <textarea
            id="tgt-notes"
            v-model="form.notes"
            rows="2"
            maxlength="1000"
            placeholder="Additional details, plan, or savings notes…"
          ></textarea>
        </div>
      </div>

      <p v-if="localError" class="notice">{{ localError }}</p>

      <div class="actions">
        <button type="submit" class="button primary" :disabled="saving">
          {{ saving ? 'Saving…' : isEditing ? 'Update Target' : 'Create Target' }}
        </button>
        <button type="button" class="button subtle" :disabled="saving" @click="$emit('cancel')">
          Cancel
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { Account, Category, Target, TargetInput, TargetStatus } from '../money.types'

const props = defineProps<{
  target?: Target | null
  categories: Category[]
  accounts: Account[]
}>()

const emit = defineEmits<{
  save: [input: TargetInput]
  cancel: []
}>()

const isEditing = computed(() => Boolean(props.target))

const colorPresets = [
  '#10B981', // emerald
  '#3B82F6', // blue
  '#8B5CF6', // purple
  '#F59E0B', // amber
  '#EC4899', // pink
  '#14B8A6', // teal
  '#EF4444', // red
]

const form = reactive<TargetInput>({
  name: props.target?.name ?? '',
  target_amount: props.target?.target_amount ?? '',
  current_amount: props.target?.current_amount ?? '0',
  target_date: props.target?.target_date ?? '',
  category_id: props.target?.category_id ?? '',
  account_id: props.target?.account_id ?? '',
  color: props.target?.color || colorPresets[0],
  status: (props.target?.status as TargetStatus) || 'active',
  notes: props.target?.notes ?? '',
})

const activeCategories = computed(() => props.categories.filter((c) => !c.archived_at))
const activeAccounts = computed(() => props.accounts.filter((a) => !a.archived_at))

const saving = ref(false)
const localError = ref<string | null>(null)

async function handleSubmit() {
  localError.value = null
  if (!form.name.trim()) {
    localError.value = 'Target name is required.'
    return
  }
  if (!form.target_amount || parseFloat(form.target_amount) <= 0) {
    localError.value = 'Please enter a valid positive target amount.'
    return
  }

  saving.value = true
  try {
    emit('save', {
      name: form.name.trim(),
      target_amount: String(form.target_amount),
      current_amount: String(form.current_amount || '0'),
      target_date: form.target_date || null,
      category_id: form.category_id || null,
      account_id: form.account_id || null,
      color: form.color || null,
      status: form.status || 'active',
      notes: form.notes?.trim() || null,
    })
  } catch (e: unknown) {
    localError.value = e instanceof Error ? e.message : 'Failed to save target.'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.target-editor {
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
.field {
  display: grid;
  gap: var(--space-1);
}
.color-options {
  display: flex;
  gap: var(--space-2);
  margin-top: 4px;
}
.color-btn {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  border: 2px solid transparent;
  cursor: pointer;
  transition: transform 0.15s ease;
}
.color-btn:hover {
  transform: scale(1.1);
}
.color-btn.selected {
  border-color: var(--text-primary);
  box-shadow: 0 0 0 2px var(--surface);
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
