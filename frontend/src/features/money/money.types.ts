export type AccountType = 'cash' | 'bank' | 'e_wallet' | 'credit_card'

export interface Account {
  id: string
  name: string
  account_type: AccountType
  currency: string
  archived_at?: string | null
  balance: string
  created_at: string
  updated_at: string
}

export interface AccountInput {
  name: string
  account_type: AccountType
}

export interface AccountPatchInput {
  name?: string
  account_type?: AccountType
  is_archived?: boolean
}

export type CategoryType = 'income' | 'expense'

export interface Category {
  id: string
  name: string
  category_type: CategoryType
  icon?: string | null
  color?: string | null
  archived_at?: string | null
  created_at: string
  updated_at: string
}

export interface CategoryInput {
  name: string
  category_type: CategoryType
  icon?: string | null
  color?: string | null
}

export interface CategoryPatchInput {
  name?: string
  icon?: string | null
  color?: string | null
  is_archived?: boolean
}

export type TransactionType = 'income' | 'expense' | 'transfer'

export interface Receipt {
  id: string
  transaction_id: string
  original_filename: string
  mime_type: string
  byte_size: number
  created_at: string
}

export interface Transaction {
  id: string
  transaction_type: TransactionType
  amount: string
  transaction_date: string
  account_id: string
  account_name: string
  transfer_account_id?: string | null
  transfer_account_name?: string | null
  category_id?: string | null
  category_name?: string | null
  description?: string | null
  receipt?: Receipt | null
  created_at: string
  updated_at: string
}

export interface TransactionInput {
  transaction_type: TransactionType
  amount: string
  transaction_date: string
  account_id: string
  transfer_account_id?: string | null
  category_id?: string | null
  description?: string | null
}

export interface TransactionFilters {
  start_date?: string
  end_date?: string
  account_id?: string
  category_id?: string
  type?: TransactionType | ''
  q?: string
  min_amount?: string
  max_amount?: string
  limit?: number
}

export interface Budget {
  id: string
  category_id: string
  category_name: string
  period_start: string
  period_end: string
  amount_limit: string
  spent: string
  remaining: string
  percent_used: string
  is_over_budget: boolean
  created_at: string
  updated_at: string
}

export interface BudgetInput {
  category_id: string
  period_start: string
  period_end: string
  amount_limit: string
}

export type RecurringCadence = 'weekly' | 'monthly' | 'yearly'

export interface RecurringTransaction {
  id: string
  transaction_type: CategoryType
  amount: string
  account_id: string
  category_id: string
  description?: string | null
  cadence: RecurringCadence
  next_due_date: string
  ends_on?: string | null
  is_active: boolean
  last_processed_on?: string | null
  created_at: string
  updated_at: string
}

export interface RecurringTransactionInput {
  transaction_type: CategoryType
  amount: string
  account_id: string
  category_id: string
  description?: string | null
  cadence: RecurringCadence
  next_due_date: string
  ends_on?: string | null
  is_active: boolean
}

export interface CategorySpend {
  category_id: string
  category_name: string
  amount: string
}

export interface MoneyTrendPoint {
  period: string
  income: string
  expense: string
}

export interface MoneyDashboard {
  total_balance: string
  income: string
  expense: string
  category_spend: CategorySpend[]
  trend: MoneyTrendPoint[]
  target_summary?: TargetSummary | null
}

export type TargetStatus = 'active' | 'achieved' | 'paused' | 'cancelled'

export interface Target {
  id: string
  name: string
  target_amount: string
  current_amount: string
  target_date?: string | null
  category_id?: string | null
  category_name?: string | null
  account_id?: string | null
  account_name?: string | null
  color?: string | null
  icon?: string | null
  status: TargetStatus
  notes?: string | null
  progress_percent: string
  remaining_amount: string
  is_achieved: boolean
  created_at: string
  updated_at: string
}

export interface TargetInput {
  name: string
  target_amount: string
  current_amount?: string
  target_date?: string | null
  category_id?: string | null
  account_id?: string | null
  color?: string | null
  icon?: string | null
  status?: TargetStatus
  notes?: string | null
}

export interface TargetPatchInput {
  name?: string
  target_amount?: string
  current_amount?: string
  target_date?: string | null
  category_id?: string | null
  account_id?: string | null
  color?: string | null
  icon?: string | null
  status?: TargetStatus
  notes?: string | null
}

export interface TargetContributeInput {
  amount: string
  is_withdraw?: boolean
}

export interface TargetSummary {
  total_targets_count: number
  active_targets_count: number
  achieved_targets_count: number
  total_target_amount: string
  total_current_amount: string
  overall_progress: string
}

