<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="card modal-content">
      <div class="modal-header">
        <h2 class="title">Adjust Savings: {{ target.name }}</h2>
        <button type="button" class="close-btn" @click="$emit('close')">✕</button>
      </div>

      <div class="mode-toggle">
        <button
          type="button"
          class="mode-btn"
          :class="{ active: !isWithdraw }"
          @click="isWithdraw = false"
        >
          💰 Deposit (Save)
        </button>
        <button
          type="button"
          class="mode-btn"
          :class="{ active: isWithdraw }"
          @click="isWithdraw = true"
        >
          💸 Withdraw
        </button>
      </div>

      <div class="current-summary">
        <div class="stat">
          <span class="muted">Currently Saved</span>
          <strong class="val">{{ formatIDR(target.current_amount) }}</strong>
        </div>
        <div class="stat">
          <span class="muted">Target Goal</span>
          <strong class="val">{{ formatIDR(target.target_amount) }}</strong>
        </div>
      </div>

      <form @submit.prevent="handleSubmit">
        <div class="field">
          <label for="contrib-amt">
            {{ isWithdraw ? 'Amount to Withdraw (IDR)' : 'Amount to Deposit (IDR)' }}
          </label>
          <input
            id="contrib-amt"
            v-model="amount"
            type="number"
            step="0.01"
            min="0.01"
            :max="isWithdraw ? target.current_amount : undefined"
            placeholder="e.g. 250000"
            required
            autofocus
          />
        </div>

        <!-- Quick preset buttons -->
        <div class="presets">
          <button type="button" class="preset-btn" @click="setAmount('50000')">+50k</button>
          <button type="button" class="preset-btn" @click="setAmount('100000')">+100k</button>
          <button type="button" class="preset-btn" @click="setAmount('500000')">+500k</button>
          <button type="button" class="preset-btn" @click="setAmount('1000000')">+1M</button>
          <button
            v-if="isWithdraw"
            type="button"
            class="preset-btn"
            @click="setAmount(target.current_amount)"
          >
            All
          </button>
        </div>

        <!-- Projected result preview -->
        <div v-if="projectedAmount >= 0" class="projected-box">
          <span class="muted">Projected New Balance:</span>
          <strong>{{ formatIDR(String(projectedAmount)) }}</strong>
          <span class="projected-pct">({{ projectedPercent }}% of target)</span>
        </div>

        <p v-if="localError" class="notice">{{ localError }}</p>

        <div class="modal-actions">
          <button type="submit" class="button primary" :disabled="saving">
            {{ saving ? 'Saving…' : isWithdraw ? 'Confirm Withdrawal' : 'Confirm Deposit' }}
          </button>
          <button type="button" class="button subtle" :disabled="saving" @click="$emit('close')">
            Cancel
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Target } from '../money.types'

const props = defineProps<{
  target: Target
}>()

const emit = defineEmits<{
  submit: [amount: string, isWithdraw: boolean]
  close: []
}>()

const isWithdraw = ref(false)
const amount = ref('')
const saving = ref(false)
const localError = ref<string | null>(null)

function setAmount(val: string) {
  amount.value = val
}

const projectedAmount = computed(() => {
  const num = parseFloat(amount.value) || 0
  const curr = parseFloat(props.target.current_amount) || 0
  return isWithdraw.value ? Math.max(0, curr - num) : curr + num
})

const projectedPercent = computed(() => {
  const tgt = parseFloat(props.target.target_amount) || 1
  return Math.min(100, Math.round((projectedAmount.value / tgt) * 100))
})

function formatIDR(valStr: string): string {
  const val = parseFloat(valStr || '0')
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    maximumFractionDigits: 2,
  }).format(val)
}

async function handleSubmit() {
  localError.value = null
  const num = parseFloat(amount.value)
  if (!num || num <= 0) {
    localError.value = 'Please enter a positive amount.'
    return
  }
  if (isWithdraw.value && num > parseFloat(props.target.current_amount)) {
    localError.value = 'Cannot withdraw more than current saved amount.'
    return
  }

  saving.value = true
  try {
    emit('submit', amount.value, isWithdraw.value)
  } catch (e: unknown) {
    localError.value = e instanceof Error ? e.message : 'Failed to contribute.'
    saving.value = false
  }
}
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: var(--space-4);
}
.modal-content {
  width: 100%;
  max-width: 480px;
  padding: var(--space-5);
  display: grid;
  gap: var(--space-4);
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 750;
}
.close-btn {
  background: transparent;
  border: 0;
  font-size: 1.2rem;
  cursor: pointer;
  color: var(--text-muted);
}
.mode-toggle {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-2);
  background: var(--surface-muted);
  padding: 4px;
  border-radius: var(--radius-md);
}
.mode-btn {
  padding: 0.5rem;
  border: 0;
  background: transparent;
  border-radius: var(--radius-sm);
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s ease;
}
.mode-btn.active {
  background: var(--surface);
  color: var(--text-primary);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
.current-summary {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-3);
  padding: var(--space-3);
  background: var(--surface-muted);
  border-radius: var(--radius-md);
}
.stat {
  display: grid;
  gap: 2px;
}
.stat .val {
  font-size: 1rem;
  font-weight: 700;
}
.field {
  display: grid;
  gap: var(--space-1);
}
.presets {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
  margin-top: var(--space-2);
}
.preset-btn {
  padding: 4px 10px;
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: var(--font-small);
  cursor: pointer;
}
.preset-btn:hover {
  background: var(--border);
}
.projected-box {
  display: flex;
  gap: var(--space-2);
  align-items: center;
  font-size: var(--font-small);
  margin-top: var(--space-3);
  padding: var(--space-2);
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: var(--radius-sm);
}
.projected-pct {
  color: var(--success);
  font-weight: 700;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  margin-top: var(--space-4);
}
</style>
