import { computed, ref } from 'vue'
import { getMoneyDashboard } from '../money.api'
import type { MoneyDashboard } from '../money.types'

export type PeriodPreset = 'this_month' | 'last_month' | 'this_quarter' | 'this_year' | 'custom'

export function useMoneyDashboard() {
  const dashboard = ref<MoneyDashboard | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const preset = ref<PeriodPreset>('this_month')
  const groupBy = ref<'day' | 'week' | 'month' | ''>('day')

  function getPresetDates(p: PeriodPreset): { start: string; end: string } {
    const d = new Date()
    switch (p) {
      case 'this_month': {
        const start = new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10)
        const end = new Date(d.getFullYear(), d.getMonth() + 1, 0).toISOString().slice(0, 10)
        return { start, end }
      }
      case 'last_month': {
        const start = new Date(d.getFullYear(), d.getMonth() - 1, 1).toISOString().slice(0, 10)
        const end = new Date(d.getFullYear(), d.getMonth(), 0).toISOString().slice(0, 10)
        return { start, end }
      }
      case 'this_quarter': {
        const currentQuarter = Math.floor(d.getMonth() / 3)
        const start = new Date(d.getFullYear(), currentQuarter * 3, 1).toISOString().slice(0, 10)
        const end = new Date(d.getFullYear(), (currentQuarter + 1) * 3, 0)
          .toISOString()
          .slice(0, 10)
        return { start, end }
      }
      case 'this_year': {
        const start = `${d.getFullYear()}-01-01`
        const end = `${d.getFullYear()}-12-31`
        return { start, end }
      }
      default: {
        const start = new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10)
        const end = new Date(d.getFullYear(), d.getMonth() + 1, 0).toISOString().slice(0, 10)
        return { start, end }
      }
    }
  }

  const initialDates = getPresetDates('this_month')
  const startDate = ref<string>(initialDates.start)
  const endDate = ref<string>(initialDates.end)

  function applyPreset(p: PeriodPreset) {
    preset.value = p
    if (p !== 'custom') {
      const dates = getPresetDates(p)
      startDate.value = dates.start
      endDate.value = dates.end
      if (p === 'this_year') {
        groupBy.value = 'month'
      } else {
        groupBy.value = 'day'
      }
    }
  }

  async function load() {
    if (!startDate.value || !endDate.value) return
    loading.value = true
    error.value = null
    try {
      dashboard.value = await getMoneyDashboard({
        start_date: startDate.value,
        end_date: endDate.value,
        group_by: groupBy.value,
      })
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to load money dashboard'
    } finally {
      loading.value = false
    }
  }

  const netSavings = computed(() => {
    if (!dashboard.value) return '0.00'
    const inc = parseFloat(dashboard.value.income || '0')
    const exp = parseFloat(dashboard.value.expense || '0')
    return (inc - exp).toFixed(2)
  })

  const totalExpense = computed(() => {
    if (!dashboard.value) return 0
    return parseFloat(dashboard.value.expense || '0')
  })

  return {
    dashboard,
    loading,
    error,
    preset,
    startDate,
    endDate,
    groupBy,
    netSavings,
    totalExpense,
    applyPreset,
    load,
  }
}
