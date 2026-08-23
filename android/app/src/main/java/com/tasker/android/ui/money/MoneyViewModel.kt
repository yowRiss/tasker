package com.tasker.android.ui.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.android.data.model.Account
import com.tasker.android.data.model.Category
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

    private val allTransactions: StateFlow<List<Transaction>> = moneyRepository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = combine(_uiState, allTransactions) { state, allTxs ->
        filterMoneyTransactions(allTxs, state)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardData: StateFlow<MoneyDashboardData> = combine(
        accounts,
        allTransactions,
        _uiState,
    ) { accList, txList, state ->
        calculateMoneyDashboard(accList, txList, state)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MoneyDashboardData())

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun filterByAccount(accountId: String?) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    fun filterByType(type: String?) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun filterByCategory(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun filterByPeriod(period: MoneyPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
    }

    fun clearTransactionFilters() {
        _uiState.update {
            it.copy(query = "", selectedCategoryId = null, selectedType = null)
        }
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
