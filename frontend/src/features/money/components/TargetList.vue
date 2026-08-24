<template>
  <div class="target-container">
    <!-- Targets Summary Card -->
    <div v-if="targets.length" class="card target-summary-card">
      <div class="summary-header">
        <div>
          <h2 class="summary-title">Savings & Financial Goals</h2>
          <p class="muted">
            {{ activeCount }} active goal{{ activeCount === 1 ? '' : 's' }} •
            {{ achievedCount }} achieved
          </p>
        </div>
        <div class="summary-totals">
          <span class="muted">Total Saved</span>
          <strong class="total-val">{{ formatIDR(String(totalCurrent)) }}</strong>
          <span class="muted-small">of {{ formatIDR(String(totalGoal)) }} target</span>
        </div>
      </div>

      <div class="overall-progress-bar">
        <div
          class="overall-progress-fill"
          :style="{ width: `${overallPercent}%` }"
          :class="{ complete: overallPercent >= 100 }"
        ></div>
      </div>
      <div class="summary-footer">
        <span>Overall Progress: {{ overallPercent }}%</span>
        <span>Remaining: {{ formatIDR(String(Math.max(0, totalGoal - totalCurrent))) }}</span>
      </div>
    </div>

    <!-- Filter Toolbar -->
    <div class="toolbar">
      <div class="filter-group">
        <button
          type="button"
          class="filter-pill"
          :class="{ active: filterStatus === '' }"
          @click="filterStatus = ''"
        >
          All ({{ targets.length }})
        </button>
        <button
          type="button"
          class="filter-pill"
          :class="{ active: filterStatus === 'active' }"
          @click="filterStatus = 'active'"
        >
          Active ({{ activeCount }})
        </button>
        <button
          type="button"
          class="filter-pill"
          :class="{ active: filterStatus === 'achieved' }"
          @click="filterStatus = 'achieved'"
        >
          Achieved ({{ achievedCount }})
        </button>
      </div>
    </div>

    <!-- Grid of Target Cards -->
    <div v-if="filteredTargets.length" class="target-grid">
      <div
        v-for="tgt in filteredTargets"
        :key="tgt.id"
        class="card target-card"
        :class="{ achieved: tgt.is_achieved || tgt.status === 'achieved' }"
      >
        <div class="card-top">
          <div class="target-title-row">
            <span
              class="target-color-dot"
              :style="{ backgroundColor: tgt.color || '#10B981' }"
            ></span>
            <h3 class="target-name">{{ tgt.name }}</h3>
          </div>
          <span class="tag" :class="getStatusClass(tgt)">
            {{ getStatusLabel(tgt) }}
          </span>
        </div>

        <div class="progress-section">
          <div class="amounts-row">
            <div class="saved-group">
              <span class="muted">Saved</span>
              <span class="amount-saved">{{ formatIDR(tgt.current_amount) }}</span>
            </div>
            <div class="target-group">
              <span class="muted">Target</span>
              <span class="amount-target">{{ formatIDR(tgt.target_amount) }}</span>
            </div>
          </div>

          <div class="progress-bar-track">
            <div
              class="progress-bar-fill"
              :style="{
                width: getProgressWidth(tgt),
                backgroundColor: tgt.color || '#10B981',
              }"
              :class="{ complete: tgt.is_achieved || tgt.status === 'achieved' }"
            ></div>
          </div>

          <div class="meta-row">
            <span class="percent-val">{{ tgt.progress_percent }}% complete</span>
            <span v-if="!tgt.is_achieved && tgt.status !== 'achieved'" class="remaining-val">
              {{ formatIDR(tgt.remaining_amount) }} left
            </span>
            <span v-else class="achieved-val">🎉 Goal Achieved!</span>
          </div>
        </div>

        <!-- Target details: Deadline, Account, Category, Notes -->
        <div class="target-details">
          <div v-if="tgt.target_date" class="detail-item">
            <span class="detail-icon">📅</span>
            <span class="detail-text">{{ getDeadlineText(tgt.target_date) }}</span>
          </div>
          <div v-if="tgt.category_name" class="detail-item">
            <span class="detail-icon">🏷️</span>
            <span class="detail-text">{{ tgt.category_name }}</span>
          </div>
          <div v-if="tgt.account_name" class="detail-item">
            <span class="detail-icon">💳</span>
            <span class="detail-text">{{ tgt.account_name }}</span>
          </div>
          <p v-if="tgt.notes" class="notes-text">{{ tgt.notes }}</p>
        </div>

        <!-- Card Actions: Quick Contribute, Edit, Delete -->
        <div class="card-actions">
          <button
            type="button"
            class="button subtle contribute-btn"
            @click="$emit('contribute', tgt)"
          >
            💰 Deposit / Withdraw
          </button>
          <button type="button" class="button subtle" @click="$emit('edit', tgt)">Edit</button>
          <button type="button" class="button subtle danger" @click="$emit('delete', tgt.id)">
            Delete
          </button>
        </div>
      </div>
    </div>

    <p v-else class="card empty">
      No targets found for the selected filter. Create a target to start tracking your savings
      goals.
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Target } from '../money.types'

const props = defineProps<{
  targets: Target[]
}>()

defineEmits<{
  edit: [target: Target]
  delete: [id: string]
  contribute: [target: Target]
}>()

const filterStatus = ref('')

const activeCount = computed(() => props.targets.filter((t) => t.status === 'active').length)
const achievedCount = computed(
  () => props.targets.filter((t) => t.status === 'achieved' || t.is_achieved).length,
)

const totalGoal = computed(() =>
  props.targets.reduce((sum, t) => sum + (parseFloat(t.target_amount) || 0), 0),
)
const totalCurrent = computed(() =>
  props.targets.reduce((sum, t) => sum + (parseFloat(t.current_amount) || 0), 0),
)
const overallPercent = computed(() => {
  if (totalGoal.value <= 0) return 0
  return Math.min(100, Math.round((totalCurrent.value / totalGoal.value) * 100))
})

const filteredTargets = computed(() => {
  if (!filterStatus.value) return props.targets
  if (filterStatus.value === 'active') {
    return props.targets.filter((t) => t.status === 'active')
  }
  if (filterStatus.value === 'achieved') {
    return props.targets.filter((t) => t.status === 'achieved' || t.is_achieved)
  }
  return props.targets.filter((t) => t.status === filterStatus.value)
})

function getProgressWidth(tgt: Target): string {
  const pct = parseFloat(tgt.progress_percent || '0')
  return `${Math.min(pct, 100)}%`
}

function getStatusLabel(tgt: Target): string {
  if (tgt.status === 'achieved' || tgt.is_achieved) return 'Achieved 🎉'
  if (tgt.status === 'paused') return 'Paused'
  if (tgt.status === 'cancelled') return 'Cancelled'
  return 'Active'
}

function getStatusClass(tgt: Target): string {
  if (tgt.status === 'achieved' || tgt.is_achieved) return 'status-achieved'
  if (tgt.status === 'paused') return 'status-paused'
  if (tgt.status === 'cancelled') return 'status-cancelled'
  return 'status-active'
}

function getDeadlineText(dateStr: string): string {
  const targetDate = new Date(dateStr)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  targetDate.setHours(0, 0, 0, 0)

  const diffTime = targetDate.getTime() - today.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  const formattedDate = new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(targetDate)

  if (diffDays < 0) {
    return `${formattedDate} (${Math.abs(diffDays)}d overdue)`
  }
  if (diffDays === 0) {
    return `${formattedDate} (Due today)`
  }
  return `${formattedDate} (${diffDays}d left)`
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
.target-container {
  display: grid;
  gap: var(--space-4);
}
.target-summary-card {
  padding: var(--space-4) var(--space-5);
  display: grid;
  gap: var(--space-3);
  background: var(--surface);
  border-left: 4px solid var(--accent);
}
.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-4);
  flex-wrap: wrap;
}
.summary-title {
  margin: 0 0 4px 0;
  font-size: 1.2rem;
  font-weight: 750;
}
.summary-totals {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}
.total-val {
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--accent);
}
.muted-small {
  font-size: 0.8rem;
  color: var(--text-muted);
}
.overall-progress-bar {
  height: 0.75rem;
  background: var(--surface-muted);
  border-radius: 999px;
  overflow: hidden;
}
.overall-progress-fill {
  height: 100%;
  background: var(--accent);
  border-radius: 999px;
  transition: width 0.3s ease;
}
.overall-progress-fill.complete {
  background: var(--success);
}
.summary-footer {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-small);
  color: var(--text-muted);
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.filter-group {
  display: flex;
  gap: var(--space-2);
}
.filter-pill {
  padding: 4px 12px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--surface);
  font-size: var(--font-small);
  color: var(--text-muted);
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
}
.filter-pill.active {
  background: var(--accent);
  color: white;
  border-color: var(--accent);
}
.target-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 21rem), 1fr));
  gap: var(--space-4);
}
.target-card {
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: var(--space-4);
  position: relative;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}
.target-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}
.target-card.achieved {
  border-color: var(--success);
  background: #f0fdf4;
}
.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-2);
}
.target-title-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.target-color-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  flex-shrink: 0;
}
.target-name {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 750;
}
.tag {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 700;
}
.status-active {
  background: #e0f2fe;
  color: #0284c7;
}
.status-achieved {
  background: #dcfce7;
  color: #16a34a;
}
.status-paused {
  background: #fef3c7;
  color: #d97706;
}
.status-cancelled {
  background: var(--surface-muted);
  color: var(--text-muted);
}
.progress-section {
  display: grid;
  gap: var(--space-2);
}
.amounts-row {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-small);
}
.saved-group,
.target-group {
  display: flex;
  flex-direction: column;
}
.amount-saved {
  font-size: 1.05rem;
  font-weight: 800;
  color: var(--text-primary);
}
.amount-target {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-muted);
}
.progress-bar-track {
  height: 0.65rem;
  background: var(--surface-muted);
  border-radius: 999px;
  overflow: hidden;
}
.progress-bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s ease;
}
.meta-row {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-small);
}
.percent-val {
  font-weight: 700;
  color: var(--text-primary);
}
.remaining-val {
  color: var(--text-muted);
}
.achieved-val {
  color: var(--success);
  font-weight: 700;
}
.target-details {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  font-size: var(--font-small);
  color: var(--text-muted);
}
.detail-item {
  display: flex;
  align-items: center;
  gap: 4px;
  background: var(--surface-muted);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}
.notes-text {
  margin: 4px 0 0 0;
  width: 100%;
  font-size: var(--font-small);
  color: var(--text-muted);
  font-style: italic;
}
.card-actions {
  display: flex;
  gap: var(--space-2);
  border-top: 1px solid var(--border);
  padding-top: var(--space-3);
}
.contribute-btn {
  font-weight: 700;
  color: var(--accent);
}
</style>
