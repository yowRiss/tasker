<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">Money Management</h1>
        <p class="muted">Track income, expenses, categories, budgets, and cash flow.</p>
      </div>

      <div class="header-actions">
        <button
          v-if="activeTab === 'transactions'"
          class="button primary"
          @click="editingTransaction = null; creatingTransaction = !creatingTransaction"
        >
          {{ creatingTransaction ? 'Close' : 'New Transaction' }}
        </button>

        <button
          v-if="activeTab === 'categories'"
          class="button primary"
          @click="editingCategory = null; creatingCategory = !creatingCategory"
        >
          {{ creatingCategory ? 'Close' : 'New Category' }}
        </button>

        <button
          v-if="activeTab === 'budgets'"
          class="button primary"
          @click="editingBudget = null; creatingBudget = !creatingBudget"
        >
          {{ creatingBudget ? 'Close' : 'New Budget' }}
        </button>

        <button
          v-if="activeTab === 'recurring'"
          class="button primary"
          @click="editingRecurring = null; creatingRecurring = !creatingRecurring"
        >
          {{ creatingRecurring ? 'Close' : 'New Recurring' }}
        </button>
      </div>
    </header>

    <!-- Recurring Due Banner (Always visible if pending due occurrences exist) -->
    <RecurringDueBanner
      :due-items="dueRecurring"
      :categories="categories"
      @confirm="handleConfirmRecurring"
      @skip="handleSkipRecurring"
    />

    <!-- Navigation Tabs -->
    <nav class="money-tabs" aria-label="Money sections">
      <button
        type="button"
        class="tab-link"
        :class="{ active: activeTab === 'dashboard' }"
        @click="activeTab = 'dashboard'"
      >
        Dashboard & Reports
      </button>
      <button
        type="button"
        class="tab-link"
        :class="{ active: activeTab === 'transactions' }"
        @click="activeTab = 'transactions'"
      >
        Transactions
      </button>
      <button
        type="button"
        class="tab-link"
        :class="{ active: activeTab === 'categories' }"
        @click="activeTab = 'categories'"
      >
        Categories ({{ categories.length }})
      </button>
      <button
        type="button"
        class="tab-link"
        :class="{ active: activeTab === 'budgets' }"
        @click="activeTab = 'budgets'"
      >
        Budgets ({{ budgets.length }})
      </button>
      <button
        type="button"
        class="tab-link"
        :class="{ active: activeTab === 'recurring' }"
        @click="activeTab = 'recurring'"
      >
        Recurring ({{ recurringTemplates.length }})
        <span v-if="dueRecurring.length" class="due-badge">{{ dueRecurring.length }}</span>
      </button>
    </nav>

    <!-- Global Notice / Error -->
    <p v-if="globalError" class="notice">{{ globalError }}</p>

    <!-- TAB 0: DASHBOARD & REPORTS -->
    <section v-if="activeTab === 'dashboard'" class="tab-content">
      <MoneyDashboard />
    </section>

    <!-- TAB 1: TRANSACTIONS -->
    <section v-if="activeTab === 'transactions'" class="tab-content">
      <TransactionEditor
        v-if="creatingTransaction || editingTransaction"
        :transaction="editingTransaction"
        :categories="categories"
        :default-account-id="defaultAccountId"
        @save="handleSaveTransaction"
        @cancel="creatingTransaction = false; editingTransaction = null"
        @receipt-updated="reloadTransactions"
      />

      <TransactionFiltersComponent
        v-model:filters="filters"
        :categories="categories"
        @reset="resetFilters"
      />

      <p v-if="transactionStore.loading.value" class="empty">Loading transactions…</p>
      <TransactionList
        v-else-if="transactions.length"
        :transactions="transactions"
        @edit="handleEditTransaction"
        @delete="handleDeleteTransaction"
      />
      <p v-else class="card empty">No transactions match the selected filters.</p>
    </section>

    <!-- TAB 2: CATEGORIES -->
    <section v-if="activeTab === 'categories'" class="tab-content">
      <CategoryEditor
        v-if="creatingCategory || editingCategory"
        :category="editingCategory"
        @save="handleSaveCategory"
        @cancel="creatingCategory = false; editingCategory = null"
      />

      <div class="toolbar">
        <label class="checkbox-label">
          <input
            v-model="includeArchivedCategories"
            type="checkbox"
            @change="reloadCategories"
          />
          Show archived categories
        </label>
      </div>

      <p v-if="categoryStore.loading.value" class="empty">Loading categories…</p>
      <CategoryList
        v-else-if="categories.length"
        :categories="categories"
        @edit="handleEditCategory"
        @archive="handleArchiveCategory"
        @unarchive="handleUnarchiveCategory"
        @delete="handleDeleteCategory"
      />
      <p v-else class="card empty">No categories found.</p>
    </section>

    <!-- TAB 3: BUDGETS -->
    <section v-if="activeTab === 'budgets'" class="tab-content">
      <BudgetEditor
        v-if="creatingBudget || editingBudget"
        :budget="editingBudget"
        :categories="categories"
        @save="handleSaveBudget"
        @cancel="creatingBudget = false; editingBudget = null"
      />

      <p v-if="budgetStore.loading.value" class="empty">Loading budgets…</p>
      <BudgetList
        v-else-if="budgets.length"
        :budgets="budgets"
        @edit="handleEditBudget"
        @delete="handleDeleteBudget"
      />
      <p v-else class="card empty">No budgets set. Create a budget to track expense limits per category.</p>
    </section>

    <!-- TAB 4: RECURRING -->
    <section v-if="activeTab === 'recurring'" class="tab-content">
      <RecurringEditor
        v-if="creatingRecurring || editingRecurring"
        :recurring="editingRecurring"
        :categories="categories"
        :default-account-id="defaultAccountId"
        @save="handleSaveRecurring"
        @cancel="creatingRecurring = false; editingRecurring = null"
      />

      <p v-if="recurringStore.loading.value" class="empty">Loading recurring templates…</p>
      <RecurringList
        v-else-if="recurringTemplates.length"
        :items="recurringTemplates"
        :categories="categories"
        @edit="handleEditRecurring"
        @delete="handleDeleteRecurring"
      />
      <p v-else class="card empty">No recurring transactions set up. Create templates for rent, subscriptions, or salary.</p>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import BudgetEditor from '../features/money/components/BudgetEditor.vue'
import BudgetList from '../features/money/components/BudgetList.vue'
import CategoryEditor from '../features/money/components/CategoryEditor.vue'
import CategoryList from '../features/money/components/CategoryList.vue'
import MoneyDashboard from '../features/money/components/MoneyDashboard.vue'
import RecurringDueBanner from '../features/money/components/RecurringDueBanner.vue'
import RecurringEditor from '../features/money/components/RecurringEditor.vue'
import RecurringList from '../features/money/components/RecurringList.vue'
import TransactionEditor from '../features/money/components/TransactionEditor.vue'
import TransactionFiltersComponent from '../features/money/components/TransactionFilters.vue'
import TransactionList from '../features/money/components/TransactionList.vue'
import { useAccounts } from '../features/money/composables/useAccounts'
import { useBudgets } from '../features/money/composables/useBudgets'
import { useCategories } from '../features/money/composables/useCategories'
import { useRecurringTransactions } from '../features/money/composables/useRecurringTransactions'
import { useTransactions } from '../features/money/composables/useTransactions'
import type {
  Budget,
  BudgetInput,
  Category,
  CategoryInput,
  RecurringTransaction,
  RecurringTransactionInput,
  Transaction,
  TransactionFilters,
  TransactionInput,
} from '../features/money/money.types'

const activeTab = ref<'dashboard' | 'transactions' | 'categories' | 'budgets' | 'recurring'>('dashboard')

// Stores
const accountStore = useAccounts()
const categoryStore = useCategories()
const transactionStore = useTransactions()
const budgetStore = useBudgets()
const recurringStore = useRecurringTransactions()

const { defaultAccountId } = accountStore
const { categories } = categoryStore
const { transactions } = transactionStore
const { budgets } = budgetStore
const { recurring: recurringTemplates, dueRecurring } = recurringStore

const globalError = computed(() => {
  return (
    accountStore.error.value ||
    categoryStore.error.value ||
    transactionStore.error.value ||
    budgetStore.error.value ||
    recurringStore.error.value ||
    null
  )
})

// Transaction state
const creatingTransaction = ref(false)
const editingTransaction = ref<Transaction | null>(null)

// Category state
const creatingCategory = ref(false)
const editingCategory = ref<Category | null>(null)
const includeArchivedCategories = ref(false)

// Budget state
const creatingBudget = ref(false)
const editingBudget = ref<Budget | null>(null)

// Recurring state
const creatingRecurring = ref(false)
const editingRecurring = ref<RecurringTransaction | null>(null)

const filters = reactive<TransactionFilters>({
  start_date: '',
  end_date: '',
  account_id: '',
  category_id: '',
  type: '',
  q: '',
  min_amount: '',
  max_amount: '',
  limit: 50,
})

let filterTimer: number | undefined

async function reloadCategories() {
  await categoryStore.load('', includeArchivedCategories.value)
}

async function reloadTransactions() {
  await transactionStore.load(filters)
}

async function reloadBudgets() {
  await budgetStore.load()
}

async function reloadRecurring() {
  await recurringStore.loadAll()
  await recurringStore.loadDue()
}

function resetFilters() {
  filters.start_date = ''
  filters.end_date = ''
  filters.account_id = ''
  filters.category_id = ''
  filters.type = ''
  filters.q = ''
  filters.min_amount = ''
  filters.max_amount = ''
  void reloadTransactions()
}

watch(
  () => [
    filters.start_date,
    filters.end_date,
    filters.category_id,
    filters.type,
    filters.q,
    filters.min_amount,
    filters.max_amount,
  ],
  () => {
    window.clearTimeout(filterTimer)
    filterTimer = window.setTimeout(() => void reloadTransactions(), 250)
  },
)

onMounted(async () => {
  await accountStore.ensureDefaultAccount()
  await reloadCategories()
  await reloadTransactions()
  await reloadBudgets()
  await reloadRecurring()
})

// Transaction Actions
async function handleSaveTransaction(input: TransactionInput) {
  try {
    if (!input.account_id) {
      input.account_id = await accountStore.ensureDefaultAccount()
    }
    if (editingTransaction.value) {
      await transactionStore.editTransaction(editingTransaction.value.id, input)
      editingTransaction.value = null
    } else {
      await transactionStore.addTransaction(input)
      creatingTransaction.value = false
    }
    await reloadTransactions()
    await reloadBudgets()
  } catch (e: unknown) {
    console.error('Save transaction error:', e)
  }
}
function handleEditTransaction(txn: Transaction) {
  creatingTransaction.value = false
  editingTransaction.value = txn
}
async function handleDeleteTransaction(id: string) {
  if (!confirm('Are you sure you want to delete this transaction?')) return
  try {
    await transactionStore.removeTransaction(id)
    await reloadBudgets()
  } catch (e: unknown) {
    console.error('Delete transaction error:', e)
  }
}

// Category Actions
async function handleSaveCategory(input: CategoryInput) {
  try {
    if (editingCategory.value) {
      await categoryStore.editCategory(editingCategory.value.id, input)
      editingCategory.value = null
    } else {
      await categoryStore.addCategory(input)
      creatingCategory.value = false
    }
    await reloadCategories()
  } catch (e: unknown) {
    console.error('Save category error:', e)
  }
}
function handleEditCategory(cat: Category) {
  creatingCategory.value = false
  editingCategory.value = cat
}
async function handleArchiveCategory(id: string) {
  try {
    await categoryStore.editCategory(id, { is_archived: true })
  } catch (e: unknown) {
    console.error('Archive category error:', e)
  }
}
async function handleUnarchiveCategory(id: string) {
  try {
    await categoryStore.editCategory(id, { is_archived: false })
  } catch (e: unknown) {
    console.error('Unarchive category error:', e)
  }
}
async function handleDeleteCategory(id: string) {
  if (!confirm('Are you sure you want to delete this category?')) return
  try {
    await categoryStore.removeCategory(id)
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : 'Unable to delete category.')
  }
}

// Budget Actions
async function handleSaveBudget(input: BudgetInput) {
  try {
    if (editingBudget.value) {
      await budgetStore.editBudget(editingBudget.value.id, input)
      editingBudget.value = null
    } else {
      await budgetStore.addBudget(input)
      creatingBudget.value = false
    }
    await reloadBudgets()
  } catch (e: unknown) {
    console.error('Save budget error:', e)
  }
}
function handleEditBudget(bgt: Budget) {
  creatingBudget.value = false
  editingBudget.value = bgt
}
async function handleDeleteBudget(id: string) {
  if (!confirm('Are you sure you want to delete this budget?')) return
  try {
    await budgetStore.removeBudget(id)
  } catch (e: unknown) {
    console.error('Delete budget error:', e)
  }
}

// Recurring Actions
async function handleSaveRecurring(input: RecurringTransactionInput) {
  try {
    if (!input.account_id) {
      input.account_id = await accountStore.ensureDefaultAccount()
    }
    if (editingRecurring.value) {
      await recurringStore.editRecurring(editingRecurring.value.id, input)
      editingRecurring.value = null
    } else {
      await recurringStore.addRecurring(input)
      creatingRecurring.value = false
    }
    await reloadRecurring()
  } catch (e: unknown) {
    console.error('Save recurring error:', e)
  }
}
function handleEditRecurring(item: RecurringTransaction) {
  creatingRecurring.value = false
  editingRecurring.value = item
}
async function handleDeleteRecurring(id: string) {
  if (!confirm('Are you sure you want to delete this recurring template?')) return
  try {
    await recurringStore.removeRecurring(id)
  } catch (e: unknown) {
    console.error('Delete recurring error:', e)
  }
}
async function handleConfirmRecurring(id: string) {
  try {
    await recurringStore.confirmDue(id)
    await reloadTransactions()
    await reloadBudgets()
  } catch (e: unknown) {
    console.error('Confirm recurring error:', e)
  }
}
async function handleSkipRecurring(id: string) {
  try {
    await recurringStore.skipDue(id)
  } catch (e: unknown) {
    console.error('Skip recurring error:', e)
  }
}
</script>

<style scoped>
.money-tabs {
  display: flex;
  gap: var(--space-2);
  border-bottom: 1px solid var(--border);
  margin-bottom: var(--space-5);
  overflow-x: auto;
}
.tab-link {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: 0.6rem 1rem;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  font-weight: 650;
  color: var(--text-muted);
  white-space: nowrap;
}
.tab-link.active {
  border-bottom-color: var(--accent);
  color: var(--accent);
}
.due-badge {
  background: var(--danger);
  color: white;
  font-size: 0.7rem;
  padding: 1px 6px;
  border-radius: 999px;
  font-weight: 800;
}
.toolbar {
  margin-bottom: var(--space-4);
  display: flex;
  justify-content: flex-end;
}
.checkbox-label {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-small);
  color: var(--text-muted);
}
.header-actions {
  display: flex;
  gap: var(--space-2);
}
</style>
