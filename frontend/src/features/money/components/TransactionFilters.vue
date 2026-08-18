<template>
  <div class="card filters-card">
    <div class="filter-grid">
      <div class="search-field">
        <input
          :value="filters.q || ''"
          type="search"
          placeholder="Search description…"
          aria-label="Search transactions"
          @input="updateFilter('q', ($event.target as HTMLInputElement).value)"
        />
      </div>

      <div class="select-field">
        <select
          :value="filters.type || ''"
          aria-label="Transaction Type"
          @change="
            updateFilter(
              'type',
              ($event.target as HTMLSelectElement).value as TransactionFilters['type'],
            )
          "
        >
          <option value="">All Types</option>
          <option value="expense">Expense</option>
          <option value="income">Income</option>
        </select>
      </div>

      <div class="select-field">
        <select
          :value="filters.category_id || ''"
          aria-label="Category"
          @change="updateFilter('category_id', ($event.target as HTMLSelectElement).value)"
        >
          <option value="">All Categories</option>
          <option v-for="cat in categories" :key="cat.id" :value="cat.id">
            {{ cat.name }} ({{ cat.category_type }})
          </option>
        </select>
      </div>

      <div class="date-field">
        <input
          :value="filters.start_date || ''"
          type="date"
          aria-label="Start Date"
          placeholder="From date"
          @input="updateFilter('start_date', ($event.target as HTMLInputElement).value)"
        />
      </div>

      <div class="date-field">
        <input
          :value="filters.end_date || ''"
          type="date"
          aria-label="End Date"
          placeholder="To date"
          @input="updateFilter('end_date', ($event.target as HTMLInputElement).value)"
        />
      </div>

      <div class="amount-field">
        <input
          :value="filters.min_amount || ''"
          type="number"
          step="0.01"
          min="0"
          placeholder="Min IDR"
          aria-label="Minimum Amount"
          @input="updateFilter('min_amount', ($event.target as HTMLInputElement).value)"
        />
      </div>

      <div class="amount-field">
        <input
          :value="filters.max_amount || ''"
          type="number"
          step="0.01"
          min="0"
          placeholder="Max IDR"
          aria-label="Maximum Amount"
          @input="updateFilter('max_amount', ($event.target as HTMLInputElement).value)"
        />
      </div>
    </div>

    <div v-if="hasActiveFilters" class="filter-actions">
      <button type="button" class="button subtle" @click="resetFilters">Reset Filters</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Category, TransactionFilters } from '../money.types'

const props = defineProps<{
  filters: TransactionFilters
  categories: Category[]
}>()

const emit = defineEmits<{
  'update:filters': [filters: TransactionFilters]
  reset: []
}>()

const hasActiveFilters = computed(() => {
  const f = props.filters
  return Boolean(
    f.q || f.type || f.category_id || f.start_date || f.end_date || f.min_amount || f.max_amount,
  )
})

function updateFilter<K extends keyof TransactionFilters>(key: K, value: TransactionFilters[K]) {
  emit('update:filters', {
    ...props.filters,
    [key]: value,
  })
}

function resetFilters() {
  emit('reset')
}
</script>

<style scoped>
.filters-card {
  padding: var(--space-3) var(--space-4);
  margin-bottom: var(--space-4);
}
.filter-grid {
  display: grid;
  grid-template-columns: 2fr repeat(2, minmax(0, 1.2fr)) repeat(4, minmax(0, 1fr));
  gap: var(--space-2);
}
.filter-grid input,
.filter-grid select {
  width: 100%;
  padding: 0.45rem 0.6rem;
  border: 1px solid var(--border);
  border-radius: 7px;
  background: white;
  font-size: var(--font-small);
}
.filter-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-2);
  padding-top: var(--space-2);
  border-top: 1px dashed var(--border);
}
@media (max-width: 900px) {
  .filter-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .search-field {
    grid-column: span 2;
  }
}
@media (max-width: 520px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }
  .search-field {
    grid-column: span 1;
  }
}
</style>
