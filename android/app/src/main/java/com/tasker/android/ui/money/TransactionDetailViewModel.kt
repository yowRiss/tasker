package com.tasker.android.ui.money

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasker.android.data.model.Account
import com.tasker.android.data.model.Category
import com.tasker.android.data.model.CreateTransactionInput
import com.tasker.android.data.repository.MoneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TransactionDetailUiState(
    val txId: String? = null,
    val transactionType: String = "expense", // income | expense | transfer
    val amountText: String = "",
    val transactionDate: String = LocalDate.now().toString(),
    val accountId: String? = null,
    val transferAccountId: String? = null,
    val categoryId: String? = null,
    val description: String = "",
    val receiptUri: Uri? = null,
    val isSaved: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val moneyRepository: MoneyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    val accounts: StateFlow<List<Account>> = moneyRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = moneyRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onTypeChange(type: String) = _uiState.update { it.copy(transactionType = type) }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amountText = value, errorMessage = null) }
    fun onDateChange(value: String) = _uiState.update { it.copy(transactionDate = value) }
    fun onAccountChange(id: String) = _uiState.update { it.copy(accountId = id) }
    fun onTransferAccountChange(id: String) = _uiState.update { it.copy(transferAccountId = id) }
    fun onCategoryChange(id: String) = _uiState.update { it.copy(categoryId = id) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun attachReceipt(uri: Uri) = _uiState.update { it.copy(receiptUri = uri) }

    fun saveTransaction() {
        val state = _uiState.value
        val amount = state.amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Valid positive amount is required") }
            return
        }
        if (state.accountId == null) {
            _uiState.update { it.copy(errorMessage = "Account is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val created = moneyRepository.createTransaction(
                CreateTransactionInput(
                    transactionType = state.transactionType,
                    amount = amount,
                    transactionDate = state.transactionDate,
                    accountId = state.accountId,
                    transferAccountId = state.transferAccountId,
                    categoryId = state.categoryId,
                    description = state.description.ifBlank { null }
                )
            )

            // Attach receipt if selected
            if (state.receiptUri != null) {
                moneyRepository.attachReceipt(created.id, state.receiptUri)
            }

            _uiState.update { it.copy(isLoading = false, isSaved = true) }
        }
    }
}
