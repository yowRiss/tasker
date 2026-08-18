<template>
  <div class="card trend-chart-card">
    <header class="card-header">
      <h3 class="card-title">Income vs Expense Trend</h3>
      <span class="muted font-small">Server Aggregated</span>
    </header>

    <div v-if="!trend.length" class="empty">No transaction trend data for this period.</div>

    <div v-else class="content">
      <!-- uPlot Chart Canvas Mount Container -->
      <div ref="chartContainer" class="uplot-wrapper"></div>

      <!-- Accessible Data Table -->
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th scope="col">Period</th>
              <th scope="col" class="text-right">Income</th>
              <th scope="col" class="text-right">Expense</th>
              <th scope="col" class="text-right">Net Flow</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="point in trend" :key="point.period">
              <td>
                <strong>{{ point.period }}</strong>
              </td>
              <td class="text-right income-text">+{{ formatIDR(point.income) }}</td>
              <td class="text-right expense-text">-{{ formatIDR(point.expense) }}</td>
              <td class="text-right font-mono" :class="getNetClass(point.income, point.expense)">
                {{ formatIDR(getNet(point.income, point.expense)) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { MoneyTrendPoint } from '../money.types'

const props = defineProps<{
  trend: MoneyTrendPoint[]
}>()

const chartContainer = ref<HTMLDivElement | null>(null)
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let uplotInstance: any = null

async function renderChart() {
  if (!chartContainer.value || !props.trend.length) return

  // Clean up previous instance
  if (uplotInstance) {
    uplotInstance.destroy()
    uplotInstance = null
  }
  chartContainer.value.innerHTML = ''

  // Dynamically import uPlot library & CSS only when rendering reports
  const [uPlotModule] = await Promise.all([import('uplot'), import('uplot/dist/uPlot.min.css')])
  const uPlot = uPlotModule.default

  // Prepare x timestamps and y arrays
  const xVals: number[] = []
  const yIncome: number[] = []
  const yExpense: number[] = []

  props.trend.forEach((p) => {
    const timestamp = Math.floor(new Date(p.period).getTime() / 1000)
    xVals.push(isNaN(timestamp) ? 0 : timestamp)
    yIncome.push(parseFloat(p.income || '0'))
    yExpense.push(parseFloat(p.expense || '0'))
  })

  const width = chartContainer.value.clientWidth || 600

  const opts: import('uplot').Options = {
    width,
    height: 260,
    title: '',
    series: [
      {
        label: 'Date',
        value: (_u, v) => (v ? new Date(v * 1000).toISOString().slice(0, 10) : ''),
      },
      {
        label: 'Income',
        stroke: '#27AE60',
        width: 2,
        fill: 'rgba(39, 174, 96, 0.1)',
        value: (_u, v) => (v != null ? formatIDR(v.toString()) : 'Rp 0'),
      },
      {
        label: 'Expense',
        stroke: '#C0392B',
        width: 2,
        fill: 'rgba(192, 57, 43, 0.1)',
        value: (_u, v) => (v != null ? formatIDR(v.toString()) : 'Rp 0'),
      },
    ],
    axes: [
      {
        stroke: '#656b63',
        grid: { stroke: '#dfe2da', width: 1 },
        values: (_u, splits) =>
          splits.map((v) =>
            new Date(v * 1000).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }),
          ),
      },
      {
        stroke: '#656b63',
        grid: { stroke: '#dfe2da', width: 1 },
        values: (_u, splits) => splits.map((v) => `${(v / 1000).toFixed(0)}k`),
      },
    ],
    cursor: {
      drag: { setScale: false },
    },
  }

  uplotInstance = new uPlot(opts, [xVals, yIncome, yExpense], chartContainer.value)
}

onMounted(() => void renderChart())
watch(
  () => props.trend,
  () => void renderChart(),
  { deep: true },
)

onBeforeUnmount(() => {
  if (uplotInstance) {
    uplotInstance.destroy()
    uplotInstance = null
  }
})

function getNet(incStr: string, expStr: string): string {
  const inc = parseFloat(incStr || '0')
  const exp = parseFloat(expStr || '0')
  return (inc - exp).toFixed(2)
}

function getNetClass(incStr: string, expStr: string): string {
  const net = parseFloat(getNet(incStr, expStr))
  if (net > 0) return 'income-text'
  if (net < 0) return 'expense-text'
  return 'muted'
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
.trend-chart-card {
  padding: var(--space-4);
  display: grid;
  gap: var(--space-4);
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border);
  padding-bottom: var(--space-2);
}
.card-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 750;
}
.font-small {
  font-size: var(--font-small);
}
.content {
  display: grid;
  gap: var(--space-5);
}
.uplot-wrapper {
  min-height: 260px;
  overflow: hidden;
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
.income-text {
  color: var(--success);
  font-weight: 700;
}
.expense-text {
  color: var(--danger);
  font-weight: 700;
}
</style>
