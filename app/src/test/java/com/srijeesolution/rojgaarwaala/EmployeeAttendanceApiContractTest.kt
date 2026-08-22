package com.srijeesolution.rojgaarwaala

import com.srijeesolution.rojgaarwaala.data.remote.model.PunchRequest
import com.srijeesolution.rojgaarwaala.network.retorfit.RetrofitApiInterface
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Pins the wire contract between the employee attendance API and the Gson models.
 *
 * The payloads below mirror what the Laravel controllers emit, so a renamed or dropped
 * backend key shows up here as a null field instead of surfacing as a blank screen on a
 * labourer's phone. The paths and query parameters each call produces are asserted too,
 * since those are just as easy to break as the response mapping.
 */
class EmployeeAttendanceApiContractTest {

    private lateinit var server: MockWebServer
    private lateinit var api: RetrofitApiInterface

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/api/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RetrofitApiInterface::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueue(body: String, code: Int = 200) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body)
        )
    }

    @Test
    fun `dashboard payload maps every field the screen reads`() = runBlocking {
        enqueue(
            """
            {
              "status": true,
              "message": "API Success",
              "data": {
                "employee": {
                  "id": 7,
                  "name": "Ramesh Kumar",
                  "employee_code": "EMP0007",
                  "joining_date": "2026-01-15",
                  "is_active": true
                },
                "greeting": "Good Morning",
                "today": {
                  "date": "2026-08-22",
                  "date_label": "22 Aug 2026",
                  "factory": {
                    "id": 3,
                    "name": "ABC Steel",
                    "code": "ABC001",
                    "address": "Industrial Area, Raipur",
                    "latitude": 21.2514,
                    "longitude": 81.6296,
                    "geofence_radius": 200,
                    "gps_accuracy_threshold": 50
                  },
                  "daily_wage": 500.0,
                  "has_active_assignment": true,
                  "attendance_marked": true,
                  "status": "present",
                  "status_label": "Present",
                  "punch_in_at": "09:12 AM",
                  "punch_out_at": null,
                  "can_punch_in": false,
                  "can_punch_out": true,
                  "earned_wage": 500.0
                },
                "month_summary": {
                  "month": "2026-08",
                  "month_label": "August 2026",
                  "present_days": 18,
                  "absent_days": 2,
                  "half_days": 1,
                  "total_earned": 9250.0,
                  "remaining_balance": 4250.0
                }
              }
            }
            """.trimIndent()
        )

        val response = api.getEmployeeDashboard()
        val data = response.body()?.data

        assertEquals("/api/employee/dashboard", server.takeRequest().path)
        assertEquals(true, response.body()?.status)
        assertEquals("EMP0007", data?.employee?.employeeCode)
        assertEquals("2026-01-15", data?.employee?.joiningDate)
        assertEquals("Good Morning", data?.greeting)
        assertEquals("ABC Steel", data?.today?.factory?.name)
        assertEquals(21.2514, data?.today?.factory?.latitude!!, 0.0)
        assertEquals(200, data.today?.factory?.geofenceRadius)
        assertEquals(50, data.today?.factory?.gpsAccuracyThreshold)
        assertEquals(500.0, data.today?.dailyWage!!, 0.0)
        assertEquals("09:12 AM", data.today?.punchInAt)
        assertNull(data.today?.punchOutAt)
        assertEquals(false, data.today?.canPunchIn)
        assertEquals(true, data.today?.canPunchOut)
        assertEquals(9250.0, data.monthSummary?.totalEarned!!, 0.0)
        assertEquals(4250.0, data.monthSummary?.remainingBalance!!, 0.0)
    }

    @Test
    fun `punch in sends the coordinates and maps the recorded attendance`() = runBlocking {
        enqueue(
            """
            {
              "status": true,
              "message": "Attendance marked successfully",
              "data": {
                "attendance": {
                  "id": 91,
                  "attendance_date": "2026-08-22",
                  "status": "present",
                  "status_label": "Present",
                  "factory_id": 3,
                  "factory_name": "ABC Steel",
                  "punch_in_at": "09:12 AM",
                  "punch_out_at": null,
                  "distance_from_factory": 34.21,
                  "daily_wage": 500.0,
                  "earned_wage": 500.0
                }
              }
            }
            """.trimIndent(),
            code = 201,
        )

        val response = api.employeePunchIn(PunchRequest(21.2514, 81.6296, 12.5))

        val request = server.takeRequest()
        assertEquals("/api/employee/attendance/punch-in", request.path)
        assertEquals("POST", request.method)

        val sent = JSONObject(request.body.readUtf8())
        assertEquals(21.2514, sent.getDouble("latitude"), 0.0)
        assertEquals(81.6296, sent.getDouble("longitude"), 0.0)
        assertEquals(12.5, sent.getDouble("accuracy"), 0.0)

        val attendance = response.body()?.data?.attendance
        assertEquals(201, response.code())
        assertEquals("present", attendance?.status)
        assertEquals("ABC Steel", attendance?.factoryName)
        assertEquals(34.21, attendance?.distanceFromFactory!!, 0.0)
        assertEquals(500.0, attendance.earnedWage!!, 0.0)
    }

    @Test
    fun `punch out posts to its own endpoint`() = runBlocking {
        enqueue(
            """
            {
              "status": true,
              "message": "Punch out recorded successfully",
              "data": {
                "attendance": {
                  "id": 91,
                  "attendance_date": "2026-08-22",
                  "status": "present",
                  "status_label": "Present",
                  "punch_in_at": "09:12 AM",
                  "punch_out_at": "06:04 PM",
                  "daily_wage": 500.0,
                  "earned_wage": 500.0
                }
              }
            }
            """.trimIndent()
        )

        val response = api.employeePunchOut(PunchRequest(21.25, 81.63, 8.0))

        assertEquals("/api/employee/attendance/punch-out", server.takeRequest().path)
        assertEquals("06:04 PM", response.body()?.data?.attendance?.punchOutAt)
    }

    @Test
    fun `a punch failure keeps the error code readable in the raw body`() = runBlocking {
        enqueue(
            """
            {
              "status": false,
              "message": "You are 480 m away from ABC Steel.",
              "data": {
                "error_code": "outside_geofence",
                "distance" : 480
              }
            }
            """.trimIndent(),
            code = 422,
        )

        val response = api.employeePunchIn(PunchRequest(21.0, 81.0, 10.0))
        val body = response.errorBody()?.string().orEmpty()

        assertEquals(422, response.code())
        assertTrue(body.contains("outside_geofence"))
        assertEquals(
            "outside_geofence",
            JSONObject(body).getJSONObject("data").getString("error_code"),
        )
    }

    @Test
    fun `attendance history sends the month and maps the day list`() = runBlocking {
        enqueue(
            """
            {
              "status": true,
              "message": "API Success",
              "data": {
                "month": "2026-07",
                "month_label": "July 2026",
                "present_days": 20,
                "absent_days": 1,
                "half_days": 2,
                "paid_leave_days": 1,
                "unpaid_leave_days": 0,
                "total_earned": 10500.0,
                "attendanceList": [
                  {
                    "id": 55,
                    "attendance_date": "2026-07-01",
                    "date_label": "01 Jul",
                    "status": "present",
                    "status_label": "Present",
                    "factory_id": 3,
                    "factory_name": "ABC Steel",
                    "punch_in_at": "09:05 AM",
                    "punch_out_at": "06:00 PM",
                    "daily_wage": 500.0,
                    "earned_wage": 500.0
                  },
                  {
                    "id": 56,
                    "attendance_date": "2026-07-02",
                    "date_label": "02 Jul",
                    "status": "half_day",
                    "status_label": "Half Day",
                    "factory_id": 3,
                    "factory_name": "ABC Steel",
                    "punch_in_at": "09:00 AM",
                    "punch_out_at": "01:00 PM",
                    "daily_wage": 500.0,
                    "earned_wage": 250.0
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val response = api.getEmployeeAttendance("2026-07")
        val data = response.body()?.data

        assertEquals("/api/employee/attendance?month=2026-07", server.takeRequest().path)
        assertEquals(1, data?.paidLeaveDays)
        assertEquals(2, data?.attendanceList?.size)
        assertEquals("half_day", data?.attendanceList?.get(1)?.status)
        assertEquals(250.0, data?.attendanceList?.get(1)?.earnedWage!!, 0.0)
        assertEquals(500.0, data.attendanceList?.get(1)?.dailyWage!!, 0.0)
    }

    @Test
    fun `monthly summary maps the full wage statement including other paid`() = runBlocking {
        enqueue(
            """
            {
              "status": true,
              "message": "API Success",
              "data": {
                "month": "2026-08",
                "month_label": "August 2026",
                "present_days": 18,
                "absent_days": 2,
                "half_days": 1,
                "paid_leave_days": 0,
                "unpaid_leave_days": 1,
                "total_working_days": 22,
                "total_earned": 9250.0,
                "advance_taken": 3000.0,
                "salary_paid": 2000.0,
                "bonus": 500.0,
                "deduction": 250.0,
                "other_paid": 150.0,
                "remaining_balance": 4350.0,
                "cumulative_balance": 7800.0,
                "factory_breakdown": [
                  {
                    "factory_id": 3,
                    "factory_name": "ABC Steel",
                    "present_days": 18,
                    "half_days": 1,
                    "absent_days": 2,
                    "daily_wage": 500.0,
                    "total_earned": 9250.0
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val response = api.getEmployeeMonthlySummary("2026-08")
        val data = response.body()?.data

        assertEquals(
            "/api/employee/attendance/monthly-summary?month=2026-08",
            server.takeRequest().path,
        )
        assertEquals(22, data?.totalWorkingDays)
        assertEquals(3000.0, data?.advanceTaken!!, 0.0)
        assertEquals(500.0, data.bonus!!, 0.0)
        assertEquals(250.0, data.deduction!!, 0.0)
        assertEquals(150.0, data.otherPaid!!, 0.0)
        assertEquals(4350.0, data.remainingBalance!!, 0.0)
        assertEquals(7800.0, data.cumulativeBalance!!, 0.0)
        assertEquals("ABC Steel", data.factoryBreakdown?.first()?.factoryName)
    }

    @Test
    fun `monthly summary balance reconciles with the parts the screen shows`() = runBlocking {
        enqueue(
            """
            {
              "status": true,
              "message": "API Success",
              "data": {
                "month": "2026-08",
                "total_earned": 1000.0,
                "advance_taken": 200.0,
                "salary_paid": 100.0,
                "bonus": 50.0,
                "deduction": 25.0,
                "other_paid": 75.0,
                "remaining_balance": 650.0
              }
            }
            """.trimIndent()
        )

        val data = api.getEmployeeMonthlySummary("2026-08").body()?.data!!

        // Every disbursement and adjustment the API reports has to account for the balance,
        // otherwise the employee sees a figure that does not add up.
        val reconciled = data.totalEarned!! + data.bonus!! -
            data.advanceTaken!! - data.salaryPaid!! - data.deduction!! - data.otherPaid!!

        assertEquals(data.remainingBalance!!, reconciled, 0.001)
    }

    @Test
    fun `payments omit the month parameter when no filter is applied`() = runBlocking {
        enqueue("""{"status": true, "message": "API Success", "data": {"paymentList": [], "totals": {}}}""")

        api.getEmployeePayments(null)

        assertEquals("/api/employee/payments", server.takeRequest().path)
    }

    @Test
    fun `payments send the month parameter when filtering`() = runBlocking {
        enqueue("""{"status": true, "message": "API Success", "data": {"paymentList": [], "totals": {}}}""")

        api.getEmployeePayments("2026-05")

        assertEquals("/api/employee/payments?month=2026-05", server.takeRequest().path)
    }

    @Test
    fun `payment list maps proofs and every total the backend reports`() = runBlocking {
        enqueue(
            """
            {
              "status": true,
              "message": "API Success",
              "data": {
                "paymentList": [
                  {
                    "id": 12,
                    "payment_type": "advance",
                    "payment_type_label": "Advance",
                    "amount": 3000.0,
                    "payment_date": "2026-08-10",
                    "date_label": "10 Aug 2026",
                    "payment_method": "Cash",
                    "transaction_reference": "TXN-1",
                    "remarks": "Festival advance",
                    "factory_name": "ABC Steel",
                    "proofList": [
                      {
                        "id": 4,
                        "file_name": "receipt.jpg",
                        "file_type": "image/jpeg",
                        "is_image": true,
                        "url": "https://example.test/proof/4"
                      }
                    ]
                  },
                  {
                    "id": 13,
                    "payment_type": "other",
                    "payment_type_label": "Other",
                    "amount": 150.0,
                    "payment_date": "2026-08-12",
                    "date_label": "12 Aug 2026",
                    "proofList": []
                  }
                ],
                "totals": {
                  "advance": 3000.0,
                  "salary_payment": 2000.0,
                  "bonus": 500.0,
                  "deduction": 250.0,
                  "other": 150.0
                }
              }
            }
            """.trimIndent()
        )

        val data = api.getEmployeePayments("2026-08").body()?.data

        assertEquals(2, data?.paymentList?.size)
        val advance = data?.paymentList?.first()
        assertEquals("advance", advance?.paymentType)
        assertEquals("Festival advance", advance?.remarks)
        assertEquals(1, advance?.proofList?.size)
        assertEquals(true, advance?.proofList?.first()?.isImage)
        assertEquals("https://example.test/proof/4", advance?.proofList?.first()?.url)

        assertEquals("other", data?.paymentList?.get(1)?.paymentType)
        assertEquals(150.0, data?.totals?.other!!, 0.0)
        assertEquals(3000.0, data.totals?.advance!!, 0.0)
        assertEquals(2000.0, data.totals?.salaryPayment!!, 0.0)
    }

    @Test
    fun `factory terms map the factory and its ordered term list`() = runBlocking {
        enqueue(
            """
            {
              "status": true,
              "message": "API Success",
              "data": {
                "factory": { "id": 3, "name": "ABC Steel" },
                "termsList": [
                  { "id": 1, "title": "Working Hours", "description": "9 AM to 6 PM" },
                  { "id": 2, "title": "Weekly Off", "description": "Sunday" }
                ]
              }
            }
            """.trimIndent()
        )

        val data = api.getEmployeeFactoryTerms().body()?.data

        assertEquals("/api/employee/factory/terms", server.takeRequest().path)
        assertEquals("ABC Steel", data?.factory?.name)
        assertEquals(2, data?.termsList?.size)
        assertEquals("Working Hours", data?.termsList?.first()?.title)
        assertEquals("Sunday", data?.termsList?.get(1)?.description)
    }

    @Test
    fun `an unauthenticated response is reported as 401 without a parsed body`() = runBlocking {
        enqueue("""{"message": "Unauthenticated."}""", code = 401)

        val response = api.getEmployeeDashboard()

        assertEquals(401, response.code())
        assertNull(response.body())
        assertTrue(response.errorBody()?.string()?.contains("Unauthenticated.") == true)
    }

    @Test
    fun `a missing factory is reported as 404`() = runBlocking {
        enqueue(
            """{"status": false, "message": "You are not assigned to any factory today.", "data": null}""",
            code = 404,
        )

        val response = api.getEmployeeFactoryTerms()

        assertEquals(404, response.code())
        assertTrue(
            response.errorBody()?.string()?.contains("not assigned to any factory") == true
        )
    }

    @Test
    fun `absent optional fields decode to null rather than failing`() = runBlocking {
        enqueue(
            """
            {
              "status": true,
              "message": "API Success",
              "data": {
                "employee": { "id": 7, "name": "Ramesh Kumar" },
                "today": { "date": "2026-08-22", "has_active_assignment": false },
                "month_summary": { "month": "2026-08" }
              }
            }
            """.trimIndent()
        )

        val data = api.getEmployeeDashboard().body()?.data

        assertEquals(7, data?.employee?.id)
        assertNull(data?.employee?.joiningDate)
        assertNull(data?.today?.factory)
        assertNull(data?.today?.dailyWage)
        assertEquals(false, data?.today?.hasActiveAssignment)
        assertNull(data?.monthSummary?.totalEarned)
    }
}
