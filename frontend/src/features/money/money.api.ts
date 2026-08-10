import { api, query } from '../../lib/api/client'
import type {
  Account,
  AccountInput,
  AccountPatchInput,
  Budget,
  BudgetInput,
  Category,
  CategoryInput,
  CategoryPatchInput,
  MoneyDashboard,
  Receipt,
  RecurringTransaction,
  RecurringTransactionInput,
  Transaction,
  TransactionFilters,
  TransactionInput,
} from './money.types'

// Accounts API
export const listAccounts = (params: { include_archived?: boolean } = {}) =>
  api<{ items: Account[] }>(`/v1/accounts${query(params as Record<string, string | boolean>)}`)

export const getAccount = (id: string) => api<Account>(`/v1/accounts/${id}`)

export const createAccount = (input: AccountInput) =>
  api<Account>('/v1/accounts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

export const updateAccount = (id: string, input: AccountPatchInput) =>
  api<Account>(`/v1/accounts/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

export const deleteAccount = (id: string) => api<void>(`/v1/accounts/${id}`, { method: 'DELETE' })

// Categories API
export const listCategories = (
  params: { type?: string; include_archived?: boolean } = {},
) =>
  api<{ items: Category[] }>(`/v1/categories${query(params as Record<string, string | boolean>)}`)

export const getCategory = (id: string) => api<Category>(`/v1/categories/${id}`)

export const createCategory = (input: CategoryInput) =>
  api<Category>('/v1/categories', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

export const updateCategory = (id: string, input: CategoryPatchInput) =>
  api<Category>(`/v1/categories/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

export const deleteCategory = (id: string) =>
  api<void>(`/v1/categories/${id}`, { method: 'DELETE' })

// Transactions API
export const listTransactions = (params: TransactionFilters = {}) =>
  api<{ items: Transaction[] }>(
    `/v1/transactions${query(params as Record<string, string | number | undefined>)}`,
  )

export const getTransaction = (id: string) => api<Transaction>(`/v1/transactions/${id}`)

export const createTransaction = (input: TransactionInput) =>
  api<Transaction>('/v1/transactions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

export const updateTransaction = (id: string, input: Partial<TransactionInput>) =>
  api<Transaction>(`/v1/transactions/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

export const deleteTransaction = (id: string) =>
  api<void>(`/v1/transactions/${id}`, { method: 'DELETE' })

// Receipt API
export const uploadReceipt = (transactionId: string, file: File) => {
  const form = new FormData()
  form.set('file', file)
  return api<Receipt>(`/v1/transactions/${transactionId}/receipt`, {
    method: 'POST',
    body: form,
  })
}

export const deleteReceipt = (receiptId: string) =>
  api<void>(`/v1/transaction-receipts/${receiptId}`, { method: 'DELETE' })

export const receiptAccess = (receiptId: string) =>
  api<{ url: string; expires_in: number }>(`/v1/transaction-receipts/${receiptId}/access`)

// Budgets API
export const listBudgets = () => api<{ items: Budget[] }>('/v1/budgets')

export const getBudget = (id: string) => api<Budget>(`/v1/budgets/${id}`)

export const createBudget = (input: BudgetInput) =>
  api<Budget>('/v1/budgets', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

export const updateBudget = (id: string, input: BudgetInput) =>
  api<Budget>(`/v1/budgets/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

export const deleteBudget = (id: string) => api<void>(`/v1/budgets/${id}`, { method: 'DELETE' })

// Recurring Transactions API
export const listRecurringTransactions = () =>
  api<{ items: RecurringTransaction[] }>('/v1/recurring-transactions')

export const listDueRecurringTransactions = () =>
  api<{ items: RecurringTransaction[] }>('/v1/recurring-transactions/due')

export const getRecurringTransaction = (id: string) =>
  api<RecurringTransaction>(`/v1/recurring-transactions/${id}`)

export const createRecurringTransaction = (input: RecurringTransactionInput) =>
  api<RecurringTransaction>('/v1/recurring-transactions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

export const updateRecurringTransaction = (
  id: string,
  input: RecurringTransactionInput,
) =>
  api<RecurringTransaction>(`/v1/recurring-transactions/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

export const deleteRecurringTransaction = (id: string) =>
  api<void>(`/v1/recurring-transactions/${id}`, { method: 'DELETE' })

export const confirmRecurringTransaction = (id: string) =>
  api<Transaction>(`/v1/recurring-transactions/${id}/confirm`, { method: 'POST' })

export const skipRecurringTransaction = (id: string) =>
  api<void>(`/v1/recurring-transactions/${id}/skip`, { method: 'POST' })

// Dashboard API
export const getMoneyDashboard = (params: {
  start_date: string
  end_date: string
  group_by?: 'day' | 'week' | 'month' | ''
}) =>
  api<MoneyDashboard>(
    `/v1/money/dashboard${query(params as Record<string, string>)}`,
  )
