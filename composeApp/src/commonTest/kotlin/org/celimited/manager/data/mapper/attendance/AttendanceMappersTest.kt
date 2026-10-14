package org.celimited.manager.data.mapper.attendance

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.celimited.manager.core.common.location.GeoLocation
import org.celimited.manager.core.network.ApiEnvelope
import org.celimited.manager.data.remote.dto.attendance.ApplyLeaveRequestDto
import org.celimited.manager.data.remote.dto.attendance.DistributorDto
import org.celimited.manager.data.remote.dto.attendance.GeofenceConfigDto
import org.celimited.manager.data.remote.dto.attendance.LateReasonDto
import org.celimited.manager.data.remote.dto.attendance.LeaveReasonDto
import org.celimited.manager.data.remote.dto.attendance.MonthlyAttendanceDto
import org.celimited.manager.data.remote.dto.attendance.MonthlySummaryDto
import org.celimited.manager.data.remote.dto.attendance.SaveAttendanceRequestDto
import org.celimited.manager.data.remote.dto.attendance.TodayAttendanceStatusDto
import org.celimited.manager.data.remote.dto.login.DeviceInfoDto
import org.celimited.manager.domain.usecase.attendance.decideAttendancePopup
import org.celimited.manager.model.attendance.AttendancePopupDecision
import org.celimited.manager.model.attendance.AttendanceStatus
import org.celimited.manager.model.attendance.CheckInOption
import org.celimited.manager.model.attendance.DayAttendanceStatus
import org.celimited.manager.model.attendance.MonthlyAttendanceSummary
import org.celimited.manager.model.attendance.LeaveApprovalStatus
import org.celimited.manager.model.attendance.LeaveReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AttendanceMappersTest {

    // Same configuration as NetworkModule.
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun todayStatusParsesAndMeasuresClockSkew() {
        val body = """
            {"success":true,"statusCode":200,"message":"ok","data":{
              "serverTime":"2026-09-20T09:05:12+06:00",
              "attendanceDeadline":"2026-09-20T09:30:00+06:00",
              "status":"Absent",
              "leave":{"fromDate":"2026-02-15","toDate":"2026-02-18","leaveType":"Casual Leave","status":"APPROVED"}
            }}
        """.trimIndent()
        val dto = json.decodeFromString<ApiEnvelope<TodayAttendanceStatusDto>>(body).data!!

        // 2026-09-20T03:05:12Z, i.e. the same instant as serverTime.
        val serverMillis = 1_789_873_512_000L
        val today = dto.toDomain(deviceNowMillis = serverMillis - 30_000)

        assertEquals(AttendanceStatus.ABSENT, today.status)
        assertEquals(30_000L, today.clockSkewMillis)
        assertEquals(serverMillis + (24 * 60 + 48) * 1000L, today.attendanceDeadlineEpochMillis)
        assertEquals(LocalDate(2026, 2, 15), today.leave?.fromDate)
        assertEquals(LeaveApprovalStatus.APPROVED, today.leave?.status)
    }

    @Test
    fun deadlineWithoutOffsetIsReadInServerOffsetAndIsLate() {
        // Real response: serverTime has an offset and a 7-digit fraction, the deadline has neither.
        val body = """
            {"data":{"serverTime":"2026-09-28T14:19:22.4453138+06:00",
              "attendanceDeadline":"2026-09-28T09:30:00","status":"Absent","leave":null},
             "success":true,"statusCode":200,"message":"Today's attendance status retrieved successfully."}
        """.trimIndent()
        val dto = json.decodeFromString<ApiEnvelope<TodayAttendanceStatusDto>>(body).data!!

        val serverMillis = 1_790_583_562_445L // 2026-09-28T08:19:22.445Z
        val today = dto.toDomain(deviceNowMillis = serverMillis)

        assertEquals(AttendanceStatus.ABSENT, today.status)
        assertEquals(1_790_566_200_000L, today.attendanceDeadlineEpochMillis) // 2026-09-28T03:30:00Z
        assertEquals(0L, today.clockSkewMillis)
        assertEquals(AttendancePopupDecision.Late, decideAttendancePopup(today, nowMillis = serverMillis))
    }

    @Test
    fun deadlineWithoutAnyOffsetFallsBackToDeviceZone() {
        val today = TodayAttendanceStatusDto(
            serverTime = "2026-09-28T14:19:22",
            attendanceDeadline = "2026-09-28T09:30:00",
            status = "ABSENT"
        ).toDomain(0)
        assertNotNull(today.attendanceDeadlineEpochMillis)
        assertNotNull(today.clockSkewMillis)
    }

    @Test
    fun statusIsCaseInsensitive() {
        assertEquals(AttendanceStatus.PRESENT, TodayAttendanceStatusDto(status = "Present").toDomain(0).status)
        assertEquals(AttendanceStatus.LEAVE, TodayAttendanceStatusDto(status = "LEAVE").toDomain(0).status)
        assertEquals(AttendanceStatus.UNKNOWN, TodayAttendanceStatusDto(status = "NOT_MARKED").toDomain(0).status)
    }

    @Test
    fun unparseableTimesBecomeNull() {
        val today = TodayAttendanceStatusDto(serverTime = "bad", attendanceDeadline = null).toDomain(0)
        assertNull(today.clockSkewMillis)
        assertNull(today.attendanceDeadlineEpochMillis)
    }

    @Test
    fun monthlySummaryParses() {
        val body = """
            {"success":true,"statusCode":200,"message":"Monthly attendance report retrieved successfully.",
             "data":{"summary":{"attendancePercentage":75,"presentDays":12,"lateDays":2,"leaveDays":1,"absentDays":1},
                     "calendar":[{"date":"2026-01-01","status":"PRESENT","statusId":1},
                                 {"date":"2026-01-02","status":"late","statusId":2},
                                 {"date":"2026-01-05","status":"Leave"},
                                 {"date":"2026-01-06","status":"ABSENT"},
                                 {"date":"2026-01-07","status":"HOLIDAY"},
                                 {"date":"bad","status":"PRESENT"},
                                 {"status":"PRESENT"}]}}
        """.trimIndent()
        val monthly = json.decodeFromString<ApiEnvelope<MonthlyAttendanceDto>>(body).data!!.toDomain()

        assertEquals(MonthlyAttendanceSummary(75, 12, 2, 1, 1), monthly.summary)
        assertEquals(
            mapOf(
                1 to DayAttendanceStatus.PRESENT,
                2 to DayAttendanceStatus.LATE,
                5 to DayAttendanceStatus.LEAVE,
                6 to DayAttendanceStatus.ABSENT,
                7 to DayAttendanceStatus.NONE
            ),
            monthly.days
        )
    }

    @Test
    fun monthlySummaryDefaultsAndClampsPercentage() {
        val empty = MonthlyAttendanceDto().toDomain()
        assertEquals(MonthlyAttendanceSummary(0, 0, 0, 0, 0), empty.summary)
        assertTrue(empty.days.isEmpty())

        val over = MonthlyAttendanceDto(summary = MonthlySummaryDto(attendancePercentage = 140.0)).toDomain()
        assertEquals(100, over.summary.percentage)
        val fractional = MonthlyAttendanceDto(summary = MonthlySummaryDto(attendancePercentage = 66.6)).toDomain()
        assertEquals(67, fractional.summary.percentage)
    }

    @Test
    fun leaveReasonsParseAndDropRowsWithoutId() {
        val reasons = json.decodeFromString<ApiEnvelope<List<LeaveReasonDto>>>(
            """{"success":true,"statusCode":200,"message":"Leave reason list retrieved successfully.",
                "data":[{"id":1,"name":"Casual Leave"},{"name":"no id"}]}"""
        ).data!!.toLeaveReasons()
        assertEquals(listOf(LeaveReason(1, "Casual Leave")), reasons)
    }

    @Test
    fun applyLeaveRequestEncodesIsoDatesAndOmitsNullRemarks() {
        val request = ApplyLeaveRequestDto(
            fromDate = LocalDate(2026, 10, 1).toString(),
            toDate = LocalDate(2026, 10, 3).toString(),
            leaveReasonId = 2,
            latitude = "23.780636",
            longitude = "90.279541",
            deviceInfo = DeviceInfoDto("id", "mobile", "Android 14", "1.0")
        )
        val encoded = json.encodeToString(ApplyLeaveRequestDto.serializer(), request)
        assertTrue("\"fromDate\":\"2026-10-01\"" in encoded)
        assertTrue("\"toDate\":\"2026-10-03\"" in encoded)
        assertTrue("\"leaveReasonId\":2" in encoded)
        assertTrue("\"latitude\":\"23.780636\"" in encoded)
        assertTrue("\"longitude\":\"90.279541\"" in encoded)
        assertFalse("remarks" in encoded)
    }

    @Test
    fun referenceListsParse() {
        val reasons = json.decodeFromString<ApiEnvelope<List<LateReasonDto>>>(
            """{"success":true,"data":[{"id":3,"name":"Illness"},{"name":"no id"}]}"""
        ).data!!.toLateReasons()
        assertEquals(1, reasons.size)
        assertEquals("Illness", reasons.single().name)

        val geofence = json.decodeFromString<ApiEnvelope<List<GeofenceConfigDto>>>(
            """{"data":[{"salespointId":7645,"checkInOption":1,"defaultRadiusMeters":50}],"success":true}"""
        ).data!!.toGeofenceConfigs().single()
        assertEquals(7645, geofence.salespointId)
        assertEquals(CheckInOption.BLOCK, geofence.checkInOption)
        assertEquals(50, geofence.defaultRadiusMeters)

        val distributor = json.decodeFromString<ApiEnvelope<List<DistributorDto>>>(
            """{"data":[{"id":7645,"name":"M/S. SHATHI ENTERPRISE","latitude":"","longitude":"",
                "code":"0021137541","address":"KOLAROA, SATKHIRA"}],"success":true}"""
        ).data!!.toDistributors().single()
        assertEquals(7645, distributor.id)
        assertNull(distributor.location)
    }

    @Test
    fun coordinatesMustBeValid() {
        assertEquals(GeoLocation(23.78, 90.27), parseCoordinates(" 23.78 ", "90.27"))
        assertNull(parseCoordinates("", "90.27"))
        assertNull(parseCoordinates("abc", "90.27"))
        assertNull(parseCoordinates("95", "90.27"))
        assertNull(parseCoordinates("0", "0"))
    }

    @Test
    fun saveRequestOmitsOptionalFieldsWhenNull() {
        val request = SaveAttendanceRequestDto(
            salespointId = 7645,
            latitude = "23.78",
            longitude = "90.27",
            deviceInfo = DeviceInfoDto("id", "mobile", "Android 14", "1.0")
        )
        val encoded = json.encodeToString(SaveAttendanceRequestDto.serializer(), request)
        assertFalse("lateReasonId" in encoded)
        assertFalse("image" in encoded)
        assertTrue("\"salespointId\":7645" in encoded)
    }
}
