package com.tasker.android.ui.money

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
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
    val shouldAttachReceipt: Boolean = false,
    val isEditing: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val moneyRepository: MoneyRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val requestedTransactionId = savedStateHandle.get<String>("txId")?.takeIf { it.isNotBlank() }
    private val _uiState = MutableStateFlow(
        TransactionDetailUiState(isLoading = requestedTransactionId != null)
    )
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    val accounts: StateFlow<List<Account>> = moneyRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = moneyRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        requestedTransactionId?.let(::loadTransaction)
    }

    fun onTypeChange(type: String) = _uiState.update {
        it.copy(
            transactionType = type,
            transferAccountId = if (type == "transfer") it.transferAccountId else null,
            categoryId = if (type == "transfer") null else it.categoryId,
            errorMessage = null,
        )
    }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amountText = value, errorMessage = null) }
    fun onDateChange(value: String) = _uiState.update { it.copy(transactionDate = value, errorMessage = null) }
    fun onAccountChange(id: String) = _uiState.update { it.copy(accountId = id) }
    fun onTransferAccountChange(id: String) = _uiState.update { it.copy(transferAccountId = id) }
    fun onCategoryChange(id: String) = _uiState.update { it.copy(categoryId = id) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun attachReceipt(uri: Uri) = _uiState.update { it.copy(receiptUri = uri, shouldAttachReceipt = true) }

    private fun loadTransaction(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val transaction = moneyRepository.getTransaction(id)
            if (transaction == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Transaction not found") }
                return@launch
            }
            _uiState.value = TransactionDetailUiState(
                txId = transaction.id,
                transactionType = transaction.transactionType,
                amountText = transaction.amount.toString(),
                transactionDate = transaction.transactionDate,
                accountId = transaction.accountId,
                transferAccountId = transaction.transferAccountId,
                categoryId = transaction.categoryId,
                description = transaction.description.orEmpty(),
                receiptUri = transaction.receipt?.localUri?.let(Uri::parse),
                isEditing = true,
            )
        }
    }

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
        if (runCatching { LocalDate.parse(state.transactionDate) }.isFailure) {
            _uiState.update { it.copy(errorMessage = "Date must use YYYY-MM-DD format") }
            return
        }
        if (state.transactionType == "transfer" &&
            (state.transferAccountId == null || state.transferAccountId == state.accountId)
        ) {
            _uiState.update { it.copy(errorMessage = "Choose a different destination account") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val input = CreateTransactionInput(
                    transactionType = state.transactionType,
                    amount = amount,
                    transactionDate = state.transactionDate,
                    accountId = state.accountId,
                    transferAccountId = state.transferAccountId,
                    categoryId = state.categoryId,
                    description = state.description.ifBlank { null },
                )
            runCatching {
                val saved = if (state.txId == null) {
                    moneyRepository.createTransaction(input)
                } else {
                    moneyRepository.updateTransaction(state.txId, input)
                }

                if (state.shouldAttachReceipt && state.receiptUri != null) {
                    moneyRepository.attachReceipt(saved.id, state.receiptUri)
                }
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Could not save transaction")
                }
            }
        }
    }
}
