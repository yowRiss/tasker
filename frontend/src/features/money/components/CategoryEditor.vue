<template>
  <div class="card category-editor">
    <h2 class="title">{{ isEditing ? 'Edit Category' : 'New Category' }}</h2>

    <form @submit.prevent="handleSubmit">
      <div class="form-grid">
        <div class="field">
          <label for="cat-name">Category Name</label>
          <input
            id="cat-name"
            v-model="form.name"
            type="text"
            placeholder="e.g. Groceries, Subscriptions"
            required
            maxlength="80"
          />
        </div>

        <div class="field">
          <label for="cat-type">Category Type</label>
          <select id="cat-type" v-model="form.category_type" :disabled="isEditing" required>
            <option value="expense">Expense</option>
            <option value="income">Income</option>
          </select>
        </div>

        <div class="field">
          <label for="cat-icon">Icon / Identifier (Optional)</label>
          <input
            id="cat-icon"
            v-model="form.icon"
            type="text"
            placeholder="e.g. utensils, car, wallet, heart"
            maxlength="80"
          />
        </div>

        <div class="field">
          <label for="cat-color">Badge Color (Optional)</label>
          <div class="color-picker-row">
            <input id="cat-color" v-model="form.color" type="color" class="color-input" />
            <input
              v-model="form.color"
              type="text"
              placeholder="#27AE60"
              pattern="^#[0-9A-Fa-f]{6}$"
              class="color-text"
            />
          </div>
        </div>
      </div>

      <p v-if="localError" class="notice">{{ localError }}</p>

      <div class="actions">
        <button type="submit" class="button primary" :disabled="saving">
          {{ saving ? 'Saving…' : isEditing ? 'Update Category' : 'Create Category' }}
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
import type { Category, CategoryInput } from '../money.types'

const props = defineProps<{
  category?: Category | null
  defaultType?: 'income' | 'expense'
}>()

const emit = defineEmits<{
  save: [input: CategoryInput]
  cancel: []
}>()

const isEditing = computed(() => Boolean(props.category))

const form = reactive<{
  name: string
  category_type: 'income' | 'expense'
  icon: string
  color: string
}>({
  name: props.category?.name ?? '',
  category_type: props.category?.category_type ?? props.defaultType ?? 'expense',
  icon: props.category?.icon ?? '',
  color: props.category?.color ?? '#245B45',
})

const saving = ref(false)
const localError = ref<string | null>(null)

async function handleSubmit() {
  if (!form.name.trim()) {
    localError.value = 'Category name is required.'
    return
  }

  saving.value = true
  localError.value = null
  try {
    emit('save', {
      name: form.name.trim(),
      category_type: form.category_type,
      icon: form.icon.trim() || null,
      color: form.color.trim() || null,
    })
  } catch (e: unknown) {
    localError.value = e instanceof Error ? e.message : 'Failed to save category.'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.category-editor {
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
.color-picker-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.color-input {
  width: 2.5rem;
  height: 2.4rem;
  padding: 0;
  border: 1px solid var(--border);
  border-radius: 6px;
  cursor: pointer;
}
.color-text {
  flex: 1;
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
}
</style>
