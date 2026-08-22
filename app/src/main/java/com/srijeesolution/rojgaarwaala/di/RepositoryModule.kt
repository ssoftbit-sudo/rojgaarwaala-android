package com.srijeesolution.rojgaarwaala.di

import android.content.Context
import com.srijeesolution.rojgaarwaala.data.repository.EmployeeAttendanceRepositoryImpl
import com.srijeesolution.rojgaarwaala.data.repository.HelpDeskRepositoryImpl
import com.srijeesolution.rojgaarwaala.data.repository.HomePageRepositoryImpl
import com.srijeesolution.rojgaarwaala.data.repository.JobApplicationRepositoryImpl
import com.srijeesolution.rojgaarwaala.domain.repository.EmployeeAttendanceRepository
import com.srijeesolution.rojgaarwaala.domain.repository.HelpDeskRepository
import com.srijeesolution.rojgaarwaala.domain.repository.HomePageRepository
import com.srijeesolution.rojgaarwaala.domain.repository.JobApplicationRepository
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

    @Singleton
    @Provides
    fun provideHomePageRepository(
        homePageRepositoryImpl: HomePageRepositoryImpl
    ): HomePageRepository = homePageRepositoryImpl

    @Singleton
    @Provides
    fun provideJobApplicationRepository(
        jobApplicationRepositoryImpl: JobApplicationRepositoryImpl
    ): JobApplicationRepository = jobApplicationRepositoryImpl

    @Singleton
    @Provides
    fun provideHelpDeskRepository(
        helpDeskRepositoryImpl: HelpDeskRepositoryImpl
    ): HelpDeskRepository = helpDeskRepositoryImpl

    @Singleton
    @Provides
    fun provideEmployeeAttendanceRepository(
        employeeAttendanceRepositoryImpl: EmployeeAttendanceRepositoryImpl
    ): EmployeeAttendanceRepository = employeeAttendanceRepositoryImpl

}