package com.tasker.android.ui.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.android.data.model.Account
import com.tasker.android.data.model.Category
import com.tasker.android.data.model.CategorySpendItem
import com.tasker.android.data.model.MoneyDashboardData
import com.tasker.android.data.model.Transaction
import com.tasker.android.data.repository.MoneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MoneyUiState(
    val selectedAccountId: String? = null,
    val selectedType: String? = null, // income | expense | transfer
)

@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val moneyRepository: MoneyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoneyUiState())
    val uiState: StateFlow<MoneyUiState> = _uiState.asStateFlow()

    val accounts: StateFlow<List<Account>> = moneyRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = moneyRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = combine(
        _uiState,
        moneyRepository.observeTransactions()
    ) { state, allTxs ->
        allTxs.filter { tx ->
            (state.selectedAccountId == null || tx.accountId == state.selectedAccountId || tx.transferAccountId == state.selectedAccountId) &&
            (state.selectedType == null || tx.transactionType == state.selectedType)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardData: StateFlow<MoneyDashboardData> = combine(
        accounts,
        moneyRepository.observeTransactions()
    ) { accList, txList ->
        val totalBal = accList.sumOf { it.balance }
        val income = txList.filter { it.transactionType == "income" }.sumOf { it.amount }
        val expense = txList.filter { it.transactionType == "expense" }.sumOf { it.amount }

        val spendByCategory = txList.filter { it.transactionType == "expense" && it.category != null }
            .groupBy { it.category!!.name }
            .map { (catName, catTxs) ->
                CategorySpendItem(
                    categoryName = catName,
                    categoryColor = catTxs.firstOrNull()?.category?.color,
                    amount = catTxs.sumOf { it.amount }
                )
            }.sortedByDescending { it.amount }

        MoneyDashboardData(
            totalBalance = totalBal,
            totalIncome = income,
            totalExpense = expense,
            categorySpend = spendByCategory
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MoneyDashboardData())

    fun filterByAccount(accountId: String?) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    fun filterByType(type: String?) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun createAccount(name: String, type: String) {
        viewModelScope.launch {
            moneyRepository.createAccount(name, type)
        }
    }

    fun createCategory(name: String, type: String) {
        viewModelScope.launch {
            moneyRepository.createCategory(name, type)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            moneyRepository.deleteTransaction(id)
        }
    }
}
