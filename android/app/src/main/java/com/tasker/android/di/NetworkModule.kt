package com.tasker.android.di

import com.tasker.android.BuildConfig
import com.tasker.android.remote.AuthEventBus
import com.tasker.android.remote.AuthInterceptor
import com.tasker.android.remote.BaseUrlInterceptor
import com.tasker.android.remote.api.AuthApi
import com.tasker.android.remote.api.MoneyApi
import com.tasker.android.remote.api.NoteApi
import com.tasker.android.remote.api.TaskApi
import com.tasker.android.remote.buildOkHttpClient
import com.tasker.android.remote.buildRetrofit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthEventBus(): AuthEventBus = AuthEventBus()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        baseUrlInterceptor: BaseUrlInterceptor,
    ): OkHttpClient = buildOkHttpClient(
        authInterceptor    = authInterceptor,
        baseUrlInterceptor = baseUrlInterceptor,
        debug              = BuildConfig.DEBUG,
    )

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        buildRetrofit(
            baseUrl      = BuildConfig.API_BASE_URL,
            okHttpClient = okHttpClient,
        )

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create()

    @Provides
    @Singleton
    fun provideTaskApi(retrofit: Retrofit): TaskApi = retrofit.create()

    @Provides
    @Singleton
    fun provideNoteApi(retrofit: Retrofit): NoteApi = retrofit.create()

    @Provides
    @Singleton
    fun provideMoneyApi(retrofit: Retrofit): MoneyApi = retrofit.create()
}
