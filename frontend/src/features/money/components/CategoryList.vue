<template>
  <div class="category-list-container">
    <div class="section-group">
      <h3 class="group-title">Expense Categories</h3>
      <div v-if="!expenseCategories.length" class="empty card">No expense categories.</div>
      <div v-else class="grid">
        <div
          v-for="cat in expenseCategories"
          :key="cat.id"
          class="card cat-card"
          :class="{ archived: cat.archived_at }"
        >
          <div class="cat-header">
            <span
              class="color-dot"
              :style="{ backgroundColor: cat.color || 'var(--accent)' }"
            ></span>
            <span class="cat-name">{{ cat.name }}</span>
            <span v-if="cat.icon" class="cat-icon">{{ cat.icon }}</span>
          </div>

          <span v-if="cat.archived_at" class="tag archived-tag">Archived</span>

          <div class="cat-actions">
            <button type="button" class="button subtle" @click="$emit('edit', cat)">Edit</button>
            <button
              v-if="!cat.archived_at"
              type="button"
              class="button subtle"
              @click="$emit('archive', cat.id)"
            >
              Archive
            </button>
            <button
              v-else
              type="button"
              class="button subtle"
              @click="$emit('unarchive', cat.id)"
            >
              Restore
            </button>
            <button type="button" class="button subtle danger" @click="$emit('delete', cat.id)">
              Delete
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="section-group">
      <h3 class="group-title">Income Categories</h3>
      <div v-if="!incomeCategories.length" class="empty card">No income categories.</div>
      <div v-else class="grid">
        <div
          v-for="cat in incomeCategories"
          :key="cat.id"
          class="card cat-card"
          :class="{ archived: cat.archived_at }"
        >
          <div class="cat-header">
            <span
              class="color-dot"
              :style="{ backgroundColor: cat.color || 'var(--success)' }"
            ></span>
            <span class="cat-name">{{ cat.name }}</span>
            <span v-if="cat.icon" class="cat-icon">{{ cat.icon }}</span>
          </div>

          <span v-if="cat.archived_at" class="tag archived-tag">Archived</span>

          <div class="cat-actions">
            <button type="button" class="button subtle" @click="$emit('edit', cat)">Edit</button>
            <button
              v-if="!cat.archived_at"
              type="button"
              class="button subtle"
              @click="$emit('archive', cat.id)"
            >
              Archive
            </button>
            <button
              v-else
              type="button"
              class="button subtle"
              @click="$emit('unarchive', cat.id)"
            >
              Restore
            </button>
            <button type="button" class="button subtle danger" @click="$emit('delete', cat.id)">
              Delete
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Category } from '../money.types'

const props = defineProps<{
  categories: Category[]
}>()

defineEmits<{
  edit: [cat: Category]
  archive: [id: string]
  unarchive: [id: string]
  delete: [id: string]
}>()

const expenseCategories = computed(() =>
  props.categories.filter((c) => c.category_type === 'expense'),
)

const incomeCategories = computed(() =>
  props.categories.filter((c) => c.category_type === 'income'),
)
</script>

<style scoped>
.category-list-container {
  display: grid;
  gap: var(--space-6);
}
.group-title {
  margin: 0 0 var(--space-3) 0;
  font-size: 1.1rem;
  font-weight: 700;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(14rem, 1fr));
  gap: var(--space-3);
}
.cat-card {
  padding: var(--space-3) var(--space-4);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: var(--space-3);
}
.cat-card.archived {
  opacity: 0.6;
  background: var(--surface-muted);
}
.cat-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.color-dot {
  width: 0.85rem;
  height: 0.85rem;
  border-radius: 50%;
  flex-shrink: 0;
}
.cat-name {
  font-weight: 700;
  font-size: 1rem;
  flex: 1;
}
.cat-icon {
  font-size: 0.85rem;
  color: var(--text-muted);
  background: var(--surface-muted);
  padding: 1px 6px;
  border-radius: 4px;
}
.archived-tag {
  align-self: flex-start;
  font-size: 0.7rem;
}
.cat-actions {
  display: flex;
  gap: var(--space-1);
  border-top: 1px solid var(--border);
  padding-top: var(--space-2);
}
</style>
