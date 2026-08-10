import { ref } from 'vue'
import { createBudget, deleteBudget, listBudgets, updateBudget } from '../money.api'
import type { Budget, BudgetInput } from '../money.types'

export function useBudgets() {
  const budgets = ref<Budget[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function load() {
    loading.value = true
    error.value = null
    try {
      const res = await listBudgets()
      budgets.value = res.items
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to load budgets'
    } finally {
      loading.value = false
    }
  }

  async function addBudget(input: BudgetInput) {
    error.value = null
    try {
      const created = await createBudget(input)
      budgets.value.unshift(created)
      return created
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to create budget'
      throw e
    }
  }

  async function editBudget(id: string, input: BudgetInput) {
    error.value = null
    try {
      const updated = await updateBudget(id, input)
      const idx = budgets.value.findIndex((b) => b.id === id)
      if (idx !== -1) {
        budgets.value[idx] = updated
      }
      return updated
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to update budget'
      throw e
    }
  }

  async function removeBudget(id: string) {
    error.value = null
    try {
      await deleteBudget(id)
      budgets.value = budgets.value.filter((b) => b.id !== id)
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to delete budget'
      throw e
    }
  }

  return {
    budgets,
    loading,
    error,
    load,
    addBudget,
    editBudget,
    removeBudget,
  }
}
