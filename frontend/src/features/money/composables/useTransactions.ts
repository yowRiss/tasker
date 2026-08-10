import { ref } from 'vue'
import {
  createTransaction,
  deleteTransaction,
  listTransactions,
  updateTransaction,
} from '../money.api'
import type { Transaction, TransactionFilters, TransactionInput } from '../money.types'

export function useTransactions() {
  const transactions = ref<Transaction[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function load(filters: TransactionFilters = {}) {
    loading.value = true
    error.value = null
    try {
      const res = await listTransactions(filters)
      transactions.value = res.items
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to load transactions'
    } finally {
      loading.value = false
    }
  }

  async function addTransaction(input: TransactionInput) {
    error.value = null
    try {
      const created = await createTransaction(input)
      transactions.value.unshift(created)
      return created
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to create transaction'
      throw e
    }
  }

  async function editTransaction(id: string, input: Partial<TransactionInput>) {
    error.value = null
    try {
      const updated = await updateTransaction(id, input)
      const idx = transactions.value.findIndex((t) => t.id === id)
      if (idx !== -1) {
        transactions.value[idx] = updated
      }
      return updated
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to update transaction'
      throw e
    }
  }

  async function removeTransaction(id: string) {
    error.value = null
    try {
      await deleteTransaction(id)
      transactions.value = transactions.value.filter((t) => t.id !== id)
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to delete transaction'
      throw e
    }
  }

  return {
    transactions,
    loading,
    error,
    load,
    addTransaction,
    editTransaction,
    removeTransaction,
  }
}
