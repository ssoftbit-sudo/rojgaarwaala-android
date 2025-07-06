package com.srijeesolution.rojgaarwaala.di

import android.content.Context
import com.srijeesolution.rojgaarwaala.data.repository.HomePageRepositoryImpl
import com.srijeesolution.rojgaarwaala.domain.repository.HomePageRepository
import com.srijeesolution.rojgaarwaala.network.retorfit.HeaderInterceptor
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object RepositoryModule {
    @Singleton
    @Provides
    fun provideSharedPrefs(@ApplicationContext appContext: Context): SharedPrefs = SharedPrefs(appContext)


    @Provides
    @Singleton
    fun provideHeaderInterceptor(sharedPrefs: SharedPrefs): HeaderInterceptor {
        return HeaderInterceptor(sharedPrefs)
    }

    @Singleton
    @Provides
    fun provideHomePageRepository1(): HomePageRepository = HomePageRepositoryImpl()


}