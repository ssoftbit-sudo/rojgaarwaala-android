package com.srijeesolution.rojgaarwaala.data.remote.model

import com.google.gson.annotations.SerializedName

data class EmployeeDashboardResponse(
    @SerializedName("status") val status: Boolean? = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: EmployeeDashboardData? = null,
)

data class EmployeeDashboardData(
    @SerializedName("terms") val terms: TermsAcceptanceState? = null,
    @SerializedName("employee") val employee: EmployeeProfile? = null,
    @SerializedName("greeting") val greeting: String? = null,
    @SerializedName("today") val today: EmployeeToday? = null,
    @SerializedName("month_summary") val monthSummary: EmployeeMonthSummary? = null,
)

data class TermsAcceptanceState(
    @SerializedName("acceptance_required") val acceptanceRequired: Boolean? = false,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("accepted_at") val acceptedAt: String? = null,
    @SerializedName("terms_count") val termsCount: Int? = 0,
)

data class EmployeeProfile(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("employee_code") val employeeCode: String? = null,
    @SerializedName("joining_date") val joiningDate: String? = null,
    @SerializedName("is_active") val isActive: Boolean? = false,
)

data class EmployeeFactory(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("geofence_radius") val geofenceRadius: Int? = null,
    @SerializedName("gps_accuracy_threshold") val gpsAccuracyThreshold: Int? = null,
)

data class EmployeeToday(
    @SerializedName("date") val date: String? = null,
    @SerializedName("date_label") val dateLabel: String? = null,
    @SerializedName("factory") val factory: EmployeeFactory? = null,
    @SerializedName("daily_wage") val dailyWage: Double? = null,
    @SerializedName("has_active_assignment") val hasActiveAssignment: Boolean? = false,
    @SerializedName("attendance_marked") val attendanceMarked: Boolean? = false,
    @SerializedName("status") val status: String? = null,
    @SerializedName("status_label") val statusLabel: String? = null,
    @SerializedName("punch_in_at") val punchInAt: String? = null,
    @SerializedName("punch_out_at") val punchOutAt: String? = null,
    @SerializedName("can_punch_in") val canPunchIn: Boolean? = false,
    @SerializedName("can_punch_out") val canPunchOut: Boolean? = false,
    @SerializedName("earned_wage") val earnedWage: Double? = null,
)

data class EmployeeMonthSummary(
    @SerializedName("month") val month: String? = null,
    @SerializedName("month_label") val monthLabel: String? = null,
    @SerializedName("present_days") val presentDays: Int? = null,
    @SerializedName("absent_days") val absentDays: Int? = null,
    @SerializedName("half_days") val halfDays: Int? = null,
    @SerializedName("total_earned") val totalEarned: Double? = null,
    @SerializedName("remaining_balance") val remainingBalance: Double? = null,
)

data class PunchRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Double,
)

data class PunchResponse(
    @SerializedName("status") val status: Boolean? = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: PunchData? = null,
)

data class PunchData(
    @SerializedName("attendance") val attendance: PunchAttendance? = null,
)

data class PunchAttendance(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("attendance_date") val attendanceDate: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("status_label") val statusLabel: String? = null,
    @SerializedName("factory_id") val factoryId: Int? = null,
    @SerializedName("factory_name") val factoryName: String? = null,
    @SerializedName("punch_in_at") val punchInAt: String? = null,
    @SerializedName("punch_out_at") val punchOutAt: String? = null,
    @SerializedName("distance_from_factory") val distanceFromFactory: Double? = null,
    @SerializedName("daily_wage") val dailyWage: Double? = null,
    @SerializedName("earned_wage") val earnedWage: Double? = null,
)

data class AttendanceListResponse(
    @SerializedName("status") val status: Boolean? = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: AttendanceListData? = null,
)

data class AttendanceListData(
    @SerializedName("month") val month: String? = null,
    @SerializedName("month_label") val monthLabel: String? = null,
    @SerializedName("present_days") val presentDays: Int? = null,
    @SerializedName("absent_days") val absentDays: Int? = null,
    @SerializedName("half_days") val halfDays: Int? = null,
    @SerializedName("paid_leave_days") val paidLeaveDays: Int? = null,
    @SerializedName("unpaid_leave_days") val unpaidLeaveDays: Int? = null,
    @SerializedName("total_earned") val totalEarned: Double? = null,
    @SerializedName("attendanceList") val attendanceList: List<AttendanceItem>? = emptyList(),
)

data class AttendanceItem(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("attendance_date") val attendanceDate: String? = null,
    @SerializedName("date_label") val dateLabel: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("status_label") val statusLabel: String? = null,
    @SerializedName("factory_id") val factoryId: Int? = null,
    @SerializedName("factory_name") val factoryName: String? = null,
    @SerializedName("punch_in_at") val punchInAt: String? = null,
    @SerializedName("punch_out_at") val punchOutAt: String? = null,
    @SerializedName("daily_wage") val dailyWage: Double? = null,
    @SerializedName("earned_wage") val earnedWage: Double? = null,
)

data class MonthlySummaryResponse(
    @SerializedName("status") val status: Boolean? = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: MonthlySummaryData? = null,
)

data class MonthlySummaryData(
    @SerializedName("month") val month: String? = null,
    @SerializedName("month_label") val monthLabel: String? = null,
    @SerializedName("present_days") val presentDays: Int? = null,
    @SerializedName("absent_days") val absentDays: Int? = null,
    @SerializedName("half_days") val halfDays: Int? = null,
    @SerializedName("paid_leave_days") val paidLeaveDays: Int? = null,
    @SerializedName("unpaid_leave_days") val unpaidLeaveDays: Int? = null,
    @SerializedName("total_working_days") val totalWorkingDays: Int? = null,
    @SerializedName("total_earned") val totalEarned: Double? = null,
    @SerializedName("advance_taken") val advanceTaken: Double? = null,
    @SerializedName("salary_paid") val salaryPaid: Double? = null,
    @SerializedName("bonus") val bonus: Double? = null,
    @SerializedName("deduction") val deduction: Double? = null,
    @SerializedName("other_paid") val otherPaid: Double? = null,
    @SerializedName("remaining_balance") val remainingBalance: Double? = null,
    @SerializedName("cumulative_balance") val cumulativeBalance: Double? = null,
    @SerializedName("factory_breakdown") val factoryBreakdown: List<FactoryBreakdownItem>? = emptyList(),
)

data class FactoryBreakdownItem(
    @SerializedName("factory_id") val factoryId: Int? = null,
    @SerializedName("factory_name") val factoryName: String? = null,
    @SerializedName("present_days") val presentDays: Int? = null,
    @SerializedName("half_days") val halfDays: Int? = null,
    @SerializedName("absent_days") val absentDays: Int? = null,
    @SerializedName("daily_wage") val dailyWage: Double? = null,
    @SerializedName("total_earned") val totalEarned: Double? = null,
)

data class EmployeePaymentsResponse(
    @SerializedName("status") val status: Boolean? = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: EmployeePaymentsData? = null,
)

data class EmployeePaymentsData(
    @SerializedName("paymentList") val paymentList: List<EmployeePaymentItem>? = emptyList(),
    @SerializedName("totals") val totals: EmployeePaymentTotals? = null,
)

data class EmployeePaymentItem(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("payment_type") val paymentType: String? = null,
    @SerializedName("payment_type_label") val paymentTypeLabel: String? = null,
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("payment_date") val paymentDate: String? = null,
    @SerializedName("date_label") val dateLabel: String? = null,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("transaction_reference") val transactionReference: String? = null,
    @SerializedName("remarks") val remarks: String? = null,
    @SerializedName("factory_name") val factoryName: String? = null,
    @SerializedName("proofList") val proofList: List<EmployeePaymentProof>? = emptyList(),
)

data class EmployeePaymentProof(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("file_type") val fileType: String? = null,
    @SerializedName("is_image") val isImage: Boolean? = false,
    @SerializedName("url") val url: String? = null,
)

data class EmployeePaymentTotals(
    @SerializedName("advance") val advance: Double? = null,
    @SerializedName("salary_payment") val salaryPayment: Double? = null,
    @SerializedName("bonus") val bonus: Double? = null,
    @SerializedName("deduction") val deduction: Double? = null,
    @SerializedName("other") val other: Double? = null,
)

data class FactoryTermsResponse(
    @SerializedName("status") val status: Boolean? = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: FactoryTermsData? = null,
)

data class FactoryTermsData(
    @SerializedName("factory") val factory: FactoryTermsFactory? = null,
    @SerializedName("termsList") val termsList: List<FactoryTermItem>? = emptyList(),
    @SerializedName("terms") val terms: TermsAcceptanceState? = null,
)

data class AcceptTermsResponse(
    @SerializedName("status") val status: Boolean? = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: AcceptTermsData? = null,
)

data class AcceptTermsData(
    @SerializedName("terms") val terms: TermsAcceptanceState? = null,
)

data class FactoryTermsFactory(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name") val name: String? = null,
)

data class FactoryTermItem(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
)