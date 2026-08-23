package com.tasker.android.remote.api

import com.tasker.android.remote.dto.AccountCreateRequest
import com.tasker.android.remote.dto.AccountDto
import com.tasker.android.remote.dto.BudgetCreateRequest
import com.tasker.android.remote.dto.BudgetDto
import com.tasker.android.remote.dto.CategoryCreateRequest
import com.tasker.android.remote.dto.CategoryDto
import com.tasker.android.remote.dto.ItemsResponse
import com.tasker.android.remote.dto.RecurringCreateRequest
import com.tasker.android.remote.dto.RecurringDto
import com.tasker.android.remote.dto.TransactionCreateRequest
import com.tasker.android.remote.dto.TransactionDto
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface MoneyApi {

    // Accounts
    @GET("v1/accounts")
    suspend fun listAccounts(): ItemsResponse<AccountDto>

    @POST("v1/accounts")
    suspend fun createAccount(@Body body: AccountCreateRequest): AccountDto

    @DELETE("v1/accounts/{id}")
    suspend fun deleteAccount(@Path("id") id: String): Response<Unit>

    // Categories
    @GET("v1/categories")
    suspend fun listCategories(): ItemsResponse<CategoryDto>

    @POST("v1/categories")
    suspend fun createCategory(@Body body: CategoryCreateRequest): CategoryDto

    @DELETE("v1/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Response<Unit>

    // Transactions
    @GET("v1/transactions")
    suspend fun listTransactions(
        @Query("limit") limit: Int = 1000,
        @Query("account_id") accountId: String? = null,
        @Query("category_id") categoryId: String? = null,
        @Query("type") type: String? = null,
    ): ItemsResponse<TransactionDto>

    @POST("v1/transactions")
    suspend fun createTransaction(@Body body: TransactionCreateRequest): TransactionDto

    @PATCH("v1/transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: String,
        @Body body: JsonObject,
    ): TransactionDto

    @DELETE("v1/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: String): Response<Unit>

    @Multipart
    @POST("v1/transactions/{id}/receipt")
    suspend fun uploadReceipt(
        @Path("id") transactionId: String,
        @Part file: MultipartBody.Part,
    ): Response<Unit>

    // Budgets
    @GET("v1/budgets")
    suspend fun listBudgets(): ItemsResponse<BudgetDto>

    @POST("v1/budgets")
    suspend fun createBudget(@Body body: BudgetCreateRequest): BudgetDto

    @DELETE("v1/budgets/{id}")
    suspend fun deleteBudget(@Path("id") id: String): Response<Unit>

    // Recurring Transactions
    @GET("v1/recurring-transactions")
    suspend fun listRecurring(): ItemsResponse<RecurringDto>

    @POST("v1/recurring-transactions")
    suspend fun createRecurring(@Body body: RecurringCreateRequest): RecurringDto

    @DELETE("v1/recurring-transactions/{id}")
    suspend fun deleteRecurring(@Path("id") id: String): Response<Unit>
}
