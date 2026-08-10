import { ref } from 'vue'
import {
  confirmRecurringTransaction,
  createRecurringTransaction,
  deleteRecurringTransaction,
  listDueRecurringTransactions,
  listRecurringTransactions,
  skipRecurringTransaction,
  updateRecurringTransaction,
} from '../money.api'
import type { RecurringTransaction, RecurringTransactionInput } from '../money.types'

export function useRecurringTransactions() {
  const recurring = ref<RecurringTransaction[]>([])
  const dueRecurring = ref<RecurringTransaction[]>([])
  const loading = ref(false)
  const dueLoading = ref(false)
  const error = ref<string | null>(null)

  async function loadAll() {
    loading.value = true
    error.value = null
    try {
      const res = await listRecurringTransactions()
      recurring.value = res.items
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to load recurring transactions'
    } finally {
      loading.value = false
    }
  }

  async function loadDue() {
    dueLoading.value = true
    try {
      const res = await listDueRecurringTransactions()
      dueRecurring.value = res.items
    } catch (e: unknown) {
      console.error('Failed to load due recurring transactions', e)
    } finally {
      dueLoading.value = false
    }
  }

  async function addRecurring(input: RecurringTransactionInput) {
    error.value = null
    try {
      const created = await createRecurringTransaction(input)
      recurring.value.unshift(created)
      await loadDue()
      return created
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to create recurring transaction'
      throw e
    }
  }

  async function editRecurring(id: string, input: RecurringTransactionInput) {
    error.value = null
    try {
      const updated = await updateRecurringTransaction(id, input)
      const idx = recurring.value.findIndex((r) => r.id === id)
      if (idx !== -1) {
        recurring.value[idx] = updated
      }
      await loadDue()
      return updated
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to update recurring transaction'
      throw e
    }
  }

  async function removeRecurring(id: string) {
    error.value = null
    try {
      await deleteRecurringTransaction(id)
      recurring.value = recurring.value.filter((r) => r.id !== id)
      dueRecurring.value = dueRecurring.value.filter((r) => r.id !== id)
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to delete recurring transaction'
      throw e
    }
  }

  async function confirmDue(id: string) {
    error.value = null
    try {
      const createdTxn = await confirmRecurringTransaction(id)
      dueRecurring.value = dueRecurring.value.filter((r) => r.id !== id)
      await loadAll()
      return createdTxn
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to confirm recurring transaction'
      throw e
    }
  }

  async function skipDue(id: string) {
    error.value = null
    try {
      await skipRecurringTransaction(id)
      dueRecurring.value = dueRecurring.value.filter((r) => r.id !== id)
      await loadAll()
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to skip recurring transaction'
      throw e
    }
  }

  return {
    recurring,
    dueRecurring,
    loading,
    dueLoading,
    error,
    loadAll,
    loadDue,
    addRecurring,
    editRecurring,
    removeRecurring,
    confirmDue,
    skipDue,
  }
}
