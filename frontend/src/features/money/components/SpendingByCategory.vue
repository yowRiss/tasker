<template>
  <div class="card category-spend-card">
    <header class="card-header">
      <h3 class="card-title">Spending by Category</h3>
    </header>

    <div v-if="!categorySpend.length" class="empty">No expense records found for this period.</div>

    <div v-else class="content">
      <!-- SVG / Bar-list Graphic -->
      <div class="bars-container" aria-label="Category spending bar chart" role="img">
        <div v-for="item in processedItems" :key="item.category_id" class="bar-row">
          <div class="bar-label">
            <span class="name">{{ item.category_name }}</span>
            <span class="pct">{{ item.percentage }}%</span>
          </div>

          <div class="bar-track">
            <div
              class="bar-fill"
              :style="{ width: item.percentage + '%', backgroundColor: item.color }"
            ></div>
          </div>
        </div>
      </div>

      <!-- Accessible Data Table -->
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th scope="col">Category</th>
              <th scope="col" class="text-right">Amount</th>
              <th scope="col" class="text-right">Share</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in processedItems" :key="item.category_id">
              <td>
                <span class="color-indicator" :style="{ backgroundColor: item.color }"></span>
                <strong>{{ item.category_name }}</strong>
              </td>
              <td class="text-right font-mono">{{ formatIDR(item.amount) }}</td>
              <td class="text-right muted">{{ item.percentage }}%</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CategorySpend } from '../money.types'

const props = defineProps<{
  categorySpend: CategorySpend[]
  totalExpense: number
}>()

const palette = [
  '#E67E22',
  '#2980B9',
  '#8E44AD',
  '#16A085',
  '#C0392B',
  '#27AE60',
  '#F39C12',
  '#D35400',
  '#2C3E50',
  '#7F8C8D',
]

const processedItems = computed(() => {
  const total = props.totalExpense || 1
  return props.categorySpend.map((item, index) => {
    const amt = parseFloat(item.amount || '0')
    const pct = ((amt / total) * 100).toFixed(1)
    return {
      ...item,
      percentage: pct,
      color: palette[index % palette.length],
    }
  })
})

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
.category-spend-card {
  padding: var(--space-4);
  display: grid;
  gap: var(--space-4);
}
.card-header {
  border-bottom: 1px solid var(--border);
  padding-bottom: var(--space-2);
}
.card-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 750;
}
.content {
  display: grid;
  gap: var(--space-5);
}
.bars-container {
  display: grid;
  gap: var(--space-3);
}
.bar-row {
  display: grid;
  gap: 4px;
}
.bar-label {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-small);
  font-weight: 650;
}
.pct {
  color: var(--text-muted);
}
.bar-track {
  height: 0.6rem;
  background: var(--surface-muted);
  border-radius: 999px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.4s ease;
}
.table-container {
  overflow-x: auto;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-small);
}
.data-table th,
.data-table td {
  padding: 0.5rem 0.4rem;
  border-bottom: 1px solid var(--border);
}
.data-table th {
  text-align: left;
  color: var(--text-muted);
  font-weight: 650;
}
.text-right {
  text-align: right;
}
.font-mono {
  font-weight: 700;
}
.color-indicator {
  display: inline-block;
  width: 0.75rem;
  height: 0.75rem;
  border-radius: 50%;
  margin-right: 0.5rem;
  vertical-align: middle;
}
</style>
