import { ref } from 'vue'
import { createAccount, deleteAccount, listAccounts, updateAccount } from '../money.api'
import type { Account, AccountInput, AccountPatchInput } from '../money.types'
import { ApiError } from '../../../lib/api/client'

export function useAccounts() {
  const accounts = ref<Account[]>([])
  const defaultAccountId = ref<string>('')
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function load(includeArchived = false) {
    loading.value = true
    error.value = null
    try {
      const res = await listAccounts({ include_archived: includeArchived })
      accounts.value = res.items
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to load accounts'
    } finally {
      loading.value = false
    }
  }

  async function ensureDefaultAccount(): Promise<string> {
    if (defaultAccountId.value) return defaultAccountId.value
    if (!accounts.value.length) {
      await load(true)
    }
    let active = accounts.value.find((a) => !a.archived_at)
    if (!active) {
      try {
        active = await createAccount({ name: 'Main Account', account_type: 'cash' })
        accounts.value.push(active)
      } catch (e: unknown) {
        console.error('Failed to create default account', e)
        throw e
      }
    }
    defaultAccountId.value = active.id
    return active.id
  }

  async function addAccount(input: AccountInput) {
    error.value = null
    try {
      const newAcc = await createAccount(input)
      accounts.value.push(newAcc)
      return newAcc
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to create account'
      throw e
    }
  }

  async function editAccount(id: string, input: AccountPatchInput) {
    error.value = null
    try {
      const updated = await updateAccount(id, input)
      const index = accounts.value.findIndex((a) => a.id === id)
      if (index !== -1) {
        accounts.value[index] = updated
      }
      return updated
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to update account'
      throw e
    }
  }

  async function removeAccount(id: string) {
    error.value = null
    try {
      await deleteAccount(id)
      accounts.value = accounts.value.filter((a) => a.id !== id)
    } catch (e: unknown) {
      if (e instanceof ApiError && e.status === 409) {
        throw new Error('Cannot delete account with existing transactions. Archive it instead.')
      }
      error.value = e instanceof Error ? e.message : 'Failed to delete account'
      throw e
    }
  }

  async function archiveAccount(id: string) {
    return editAccount(id, { is_archived: true })
  }

  async function unarchiveAccount(id: string) {
    return editAccount(id, { is_archived: false })
  }

  return {
    accounts,
    defaultAccountId,
    loading,
    error,
    load,
    ensureDefaultAccount,
    addAccount,
    editAccount,
    removeAccount,
    archiveAccount,
    unarchiveAccount,
  }
}
