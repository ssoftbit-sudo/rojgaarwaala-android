package com.srijeesolution.rojgaarwaala

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.srijeesolution.rojgaarwaala.data.remote.model.AcceptTermsData
import com.srijeesolution.rojgaarwaala.data.remote.model.AcceptTermsResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.AttendanceListData
import com.srijeesolution.rojgaarwaala.data.remote.model.AttendanceListResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeeDashboardData
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeeDashboardResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeePaymentsData
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeePaymentsResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.EmployeeToday
import com.srijeesolution.rojgaarwaala.data.remote.model.FactoryTermsData
import com.srijeesolution.rojgaarwaala.data.remote.model.FactoryTermsResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.MonthlySummaryData
import com.srijeesolution.rojgaarwaala.data.remote.model.MonthlySummaryResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.PunchAttendance
import com.srijeesolution.rojgaarwaala.data.remote.model.PunchData
import com.srijeesolution.rojgaarwaala.data.remote.model.PunchRequest
import com.srijeesolution.rojgaarwaala.data.remote.model.PunchResponse
import com.srijeesolution.rojgaarwaala.data.remote.model.TermsAcceptanceState
import com.srijeesolution.rojgaarwaala.domain.repository.EmployeeAttendanceRepository
import com.srijeesolution.rojgaarwaala.network.handler.ApiError
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.EmployeeAttendanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Covers the ViewModel contract the attendance screens rely on: every load publishes Loading
 * first, the repository result is passed straight through, and the arguments the screens
 * supply (punch coordinates, selected month) reach the repository unchanged.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmployeeAttendanceViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    /**
     * Records what each screen asked for and replays a canned result, so the ViewModel is
     * tested without any network or Retrofit involvement.
     */
    private class FakeRepository : EmployeeAttendanceRepository {
        var dashboardResult: ApiResult<EmployeeDashboardResponse> =
            ApiResult.Success(EmployeeDashboardResponse(status = true))
        var punchInResult: ApiResult<PunchResponse> = ApiResult.Success(PunchResponse(status = true))
        var punchOutResult: ApiResult<PunchResponse> = ApiResult.Success(PunchResponse(status = true))
        var attendanceResult: ApiResult<AttendanceListResponse> =
            ApiResult.Success(AttendanceListResponse(status = true))
        var monthlySummaryResult: ApiResult<MonthlySummaryResponse> =
            ApiResult.Success(MonthlySummaryResponse(status = true))
        var paymentsResult: ApiResult<EmployeePaymentsResponse> =
            ApiResult.Success(EmployeePaymentsResponse(status = true))
        var factoryTermsResult: ApiResult<FactoryTermsResponse> =
            ApiResult.Success(FactoryTermsResponse(status = true))
        var acceptTermsResult: ApiResult<AcceptTermsResponse> =
            ApiResult.Success(AcceptTermsResponse(status = true))
        var acceptTermsCallCount = 0

        var punchInRequest: PunchRequest? = null
        var punchOutRequest: PunchRequest? = null
        var attendanceMonth: String? = null
        var monthlySummaryMonth: String? = null
        var paymentsMonth: String? = null
        var paymentsCallCount = 0

        override fun getDashboard(): Flow<ApiResult<EmployeeDashboardResponse>> =
            flowOf(dashboardResult)

        override fun punchIn(request: PunchRequest): Flow<ApiResult<PunchResponse>> {
            punchInRequest = request
            return flowOf(punchInResult)
        }

        override fun punchOut(request: PunchRequest): Flow<ApiResult<PunchResponse>> {
            punchOutRequest = request
            return flowOf(punchOutResult)
        }

        override fun getAttendance(month: String): Flow<ApiResult<AttendanceListResponse>> {
            attendanceMonth = month
            return flowOf(attendanceResult)
        }

        override fun getMonthlySummary(month: String): Flow<ApiResult<MonthlySummaryResponse>> {
            monthlySummaryMonth = month
            return flowOf(monthlySummaryResult)
        }

        override fun getPayments(month: String?): Flow<ApiResult<EmployeePaymentsResponse>> {
            paymentsMonth = month
            paymentsCallCount++
            return flowOf(paymentsResult)
        }

        override fun getFactoryTerms(): Flow<ApiResult<FactoryTermsResponse>> =
            flowOf(factoryTermsResult)

        override fun acceptFactoryTerms(): Flow<ApiResult<AcceptTermsResponse>> {
            acceptTermsCallCount++
            return flowOf(acceptTermsResult)
        }
    }

    private lateinit var repository: FakeRepository
    private lateinit var viewModel: EmployeeAttendanceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeRepository()
        viewModel = EmployeeAttendanceViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `dashboard emits loading before the repository result arrives`() = runTest(dispatcher) {
        viewModel.loadDashboard()

        assertTrue(viewModel.dashboardLiveData.value is ApiResult.Loading)
    }

    @Test
    fun `dashboard publishes the repository payload`() = runTest(dispatcher) {
        repository.dashboardResult = ApiResult.Success(
            EmployeeDashboardResponse(
                status = true,
                data = EmployeeDashboardData(
                    greeting = "Good morning",
                    today = EmployeeToday(canPunchIn = true, canPunchOut = false),
                ),
            )
        )

        viewModel.loadDashboard()
        dispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.dashboardLiveData.value
        assertTrue(result is ApiResult.Success)
        assertEquals("Good morning", result?.data?.data?.greeting)
        assertEquals(true, result?.data?.data?.today?.canPunchIn)
    }

    @Test
    fun `dashboard surfaces a repository error unchanged`() = runTest(dispatcher) {
        repository.dashboardResult = ApiResult.Error(
            ApiError(statusCode = 403, errorMsg = "Forbidden", errorBody = "{}")
        )

        viewModel.loadDashboard()
        dispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.dashboardLiveData.value
        assertTrue(result is ApiResult.Error)
        assertEquals(403, result?.message?.statusCode)
    }

    @Test
    fun `punch in forwards the exact coordinates it was given`() = runTest(dispatcher) {
        viewModel.punchIn(latitude = 21.2514, longitude = 81.6296, accuracy = 12.5)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(21.2514, repository.punchInRequest?.latitude!!, 0.0)
        assertEquals(81.6296, repository.punchInRequest?.longitude!!, 0.0)
        assertEquals(12.5, repository.punchInRequest?.accuracy!!, 0.0)
    }

    @Test
    fun `punch in publishes the recorded attendance`() = runTest(dispatcher) {
        repository.punchInResult = ApiResult.Success(
            PunchResponse(
                status = true,
                data = PunchData(
                    attendance = PunchAttendance(
                        status = "present",
                        earnedWage = 500.0,
                        distanceFromFactory = 34.2,
                    )
                ),
            )
        )

        viewModel.punchIn(1.0, 2.0, 3.0)
        dispatcher.scheduler.advanceUntilIdle()

        val attendance = viewModel.punchInLiveData.value?.data?.data?.attendance
        assertEquals("present", attendance?.status)
        assertEquals(500.0, attendance?.earnedWage!!, 0.0)
        assertEquals(34.2, attendance.distanceFromFactory!!, 0.0)
    }

    @Test
    fun `punch out is a separate call from punch in`() = runTest(dispatcher) {
        viewModel.punchOut(latitude = 10.0, longitude = 20.0, accuracy = 5.0)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(repository.punchInRequest)
        assertEquals(10.0, repository.punchOutRequest?.latitude!!, 0.0)
        assertTrue(viewModel.punchOutLiveData.value is ApiResult.Success)
    }

    @Test
    fun `punch out error does not leak into the punch in stream`() = runTest(dispatcher) {
        repository.punchOutResult = ApiResult.Error(
            ApiError(statusCode = 422, errorMsg = "Outside geofence", errorBody = "{}")
        )

        viewModel.punchOut(1.0, 2.0, 3.0)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.punchOutLiveData.value is ApiResult.Error)
        assertNull(viewModel.punchInLiveData.value)
    }

    @Test
    fun `attendance history requests the selected month`() = runTest(dispatcher) {
        repository.attendanceResult = ApiResult.Success(
            AttendanceListResponse(
                status = true,
                data = AttendanceListData(month = "2026-07", presentDays = 21),
            )
        )

        viewModel.loadAttendance("2026-07")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("2026-07", repository.attendanceMonth)
        assertEquals(21, viewModel.attendanceLiveData.value?.data?.data?.presentDays)
    }

    @Test
    fun `monthly summary requests the selected month`() = runTest(dispatcher) {
        repository.monthlySummaryResult = ApiResult.Success(
            MonthlySummaryResponse(
                status = true,
                data = MonthlySummaryData(month = "2026-06", remainingBalance = 4200.0),
            )
        )

        viewModel.loadMonthlySummary("2026-06")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("2026-06", repository.monthlySummaryMonth)
        assertEquals(
            4200.0,
            viewModel.monthlySummaryLiveData.value?.data?.data?.remainingBalance!!,
            0.0,
        )
    }

    @Test
    fun `payments default to no month filter`() = runTest(dispatcher) {
        viewModel.loadPayments()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.paymentsCallCount)
        assertNull(repository.paymentsMonth)
    }

    @Test
    fun `payments forward a month filter when one is supplied`() = runTest(dispatcher) {
        repository.paymentsResult = ApiResult.Success(
            EmployeePaymentsResponse(
                status = true,
                data = EmployeePaymentsData(paymentList = emptyList()),
            )
        )

        viewModel.loadPayments("2026-05")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("2026-05", repository.paymentsMonth)
    }

    @Test
    fun `factory terms publish the repository payload`() = runTest(dispatcher) {
        repository.factoryTermsResult = ApiResult.Success(
            FactoryTermsResponse(
                status = true,
                data = FactoryTermsData(termsList = emptyList()),
            )
        )

        viewModel.loadFactoryTerms()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.factoryTermsLiveData.value is ApiResult.Success)
    }

    @Test
    fun `accepting terms emits loading before the repository result arrives`() = runTest(dispatcher) {
        viewModel.acceptFactoryTerms()

        assertTrue(viewModel.acceptTermsLiveData.value is ApiResult.Loading)
    }

    @Test
    fun `accepting terms publishes the refreshed acceptance state`() = runTest(dispatcher) {
        repository.acceptTermsResult = ApiResult.Success(
            AcceptTermsResponse(
                status = true,
                data = AcceptTermsData(
                    terms = TermsAcceptanceState(acceptanceRequired = false, termsCount = 3),
                ),
            )
        )

        viewModel.acceptFactoryTerms()
        dispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.acceptTermsLiveData.value
        assertTrue(result is ApiResult.Success)
        assertEquals(
            false,
            (result as ApiResult.Success).data?.data?.terms?.acceptanceRequired,
        )
        assertEquals(1, repository.acceptTermsCallCount)
    }

    @Test
    fun `a failed acceptance is surfaced rather than swallowed`() = runTest(dispatcher) {
        repository.acceptTermsResult = ApiResult.Error(
            message = ApiError(statusCode = 422, errorMsg = "nope", errorBody = ""),
            data = null,
        )

        viewModel.acceptFactoryTerms()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.acceptTermsLiveData.value is ApiResult.Error)
    }

    @Test
    fun `each stream stays independent of the others`() = runTest(dispatcher) {
        viewModel.loadDashboard()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.dashboardLiveData.value is ApiResult.Success)
        assertNull(viewModel.attendanceLiveData.value)
        assertNull(viewModel.monthlySummaryLiveData.value)
        assertNull(viewModel.paymentsLiveData.value)
        assertNull(viewModel.factoryTermsLiveData.value)
        assertNull(viewModel.acceptTermsLiveData.value)
    }
}
