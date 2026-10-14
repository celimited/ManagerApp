package org.celimited.manager.data.mapper.attendance

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import org.celimited.manager.core.common.location.GeoLocation
import org.celimited.manager.data.remote.dto.attendance.DistributorDto
import org.celimited.manager.data.remote.dto.attendance.GeofenceConfigDto
import org.celimited.manager.data.remote.dto.attendance.LateReasonDto
import org.celimited.manager.data.remote.dto.attendance.LeaveInfoDto
import org.celimited.manager.data.remote.dto.attendance.LeaveReasonDto
import org.celimited.manager.data.remote.dto.attendance.SubordinateHierarchyDto
import org.celimited.manager.data.remote.dto.attendance.TeamSummaryDto
import org.celimited.manager.model.attendance.HierarchyLevel
import org.celimited.manager.model.attendance.SubordinateHierarchy
import org.celimited.manager.model.attendance.TeamAttendanceCounts
import org.celimited.manager.model.attendance.TeamAttendanceSummary
import org.celimited.manager.model.attendance.TeamManager
import org.celimited.manager.model.attendance.TeamMemberStatus
import org.celimited.manager.model.attendance.LeaveReason
import org.celimited.manager.data.remote.dto.attendance.MonthlyAttendanceDto
import org.celimited.manager.data.remote.dto.attendance.TodayAttendanceStatusDto
import org.celimited.manager.model.attendance.AttendanceStatus
import org.celimited.manager.model.attendance.CheckInOption
import org.celimited.manager.model.attendance.DayAttendanceStatus
import org.celimited.manager.model.attendance.MonthlyAttendance
import org.celimited.manager.model.attendance.MonthlyAttendanceSummary
import org.celimited.manager.model.attendance.Distributor
import org.celimited.manager.model.attendance.GeofenceConfig
import org.celimited.manager.model.attendance.LateReason
import org.celimited.manager.model.attendance.LeaveApprovalStatus
import org.celimited.manager.model.attendance.LeaveInfo
import org.celimited.manager.model.attendance.TodayAttendance
import kotlin.time.ExperimentalTime
import kotlin.math.roundToInt
import kotlin.time.Instant

/** [deviceNowMillis] is the device clock when the response arrived, used to measure clock skew. */
fun TodayAttendanceStatusDto.toDomain(deviceNowMillis: Long): TodayAttendance {
    // The API sends serverTime with an offset but attendanceDeadline without one; the deadline is
    // read in the server's own offset so it doesn't depend on the device's timezone setting.
    val serverOffset = serverTime.trailingUtcOffset()
    val serverMillis = serverTime.parseEpochMillis(serverOffset)
    return TodayAttendance(
        status = when (status?.trim()?.uppercase()) {
            "PRESENT" -> AttendanceStatus.PRESENT
            "ABSENT" -> AttendanceStatus.ABSENT
            "LEAVE" -> AttendanceStatus.LEAVE
            else -> AttendanceStatus.UNKNOWN
        },
        attendanceDeadlineEpochMillis = attendanceDeadline.parseEpochMillis(serverOffset),
        clockSkewMillis = serverMillis?.let { it - deviceNowMillis },
        leave = leave?.toDomain()
    )
}

fun LeaveInfoDto.toDomain(): LeaveInfo = LeaveInfo(
    fromDate = fromDate.parseLocalDate(),
    toDate = toDate.parseLocalDate(),
    leaveType = leaveType.orEmpty(),
    status = when (status?.trim()?.uppercase()) {
        "APPROVED" -> LeaveApprovalStatus.APPROVED
        "PENDING" -> LeaveApprovalStatus.PENDING
        else -> LeaveApprovalStatus.UNKNOWN
    }
)

/** Calendar rows with a missing or unreadable date are dropped. */
fun MonthlyAttendanceDto.toDomain(): MonthlyAttendance = MonthlyAttendance(
    summary = MonthlyAttendanceSummary(
        percentage = (summary?.attendancePercentage ?: 0.0).roundToInt().coerceIn(0, 100),
        present = summary?.presentDays ?: 0,
        late = summary?.lateDays ?: 0,
        leave = summary?.leaveDays ?: 0,
        absent = summary?.absentDays ?: 0
    ),
    days = calendar.orEmpty().mapNotNull { dto ->
        val date = dto.date.parseLocalDate() ?: return@mapNotNull null
        date.day to dto.status.toDayAttendanceStatus()
    }.toMap()
)

private fun String?.toDayAttendanceStatus(): DayAttendanceStatus = when (this?.trim()?.uppercase()) {
    "PRESENT" -> DayAttendanceStatus.PRESENT
    "LATE" -> DayAttendanceStatus.LATE
    "LEAVE" -> DayAttendanceStatus.LEAVE
    "ABSENT" -> DayAttendanceStatus.ABSENT
    else -> DayAttendanceStatus.NONE
}

/** Entries without an id can't be submitted, so they're dropped. */
fun List<LeaveReasonDto>.toLeaveReasons(): List<LeaveReason> = mapNotNull { dto ->
    dto.id?.let { LeaveReason(id = it, name = dto.name.orEmpty()) }
}

/** Levels without a key can't be queried further, so they're dropped. */
fun SubordinateHierarchyDto.toDomain(): SubordinateHierarchy = SubordinateHierarchy(
    currentUserRole = currentUserRole.orEmpty(),
    levels = levels.orEmpty().mapNotNull { dto ->
        val key = dto.levelKey?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        HierarchyLevel(
            key = key,
            name = dto.levelName?.takeIf { it.isNotBlank() } ?: key,
            presentCount = dto.presentCount ?: 0,
            totalCount = dto.totalCount ?: 0
        )
    }
)

/** Managers without a userId are dropped — the list needs a stable key per row. */
fun TeamSummaryDto.toDomain(): TeamAttendanceSummary = TeamAttendanceSummary(
    levelKey = levelKey.orEmpty(),
    counts = TeamAttendanceCounts(
        total = summary?.total ?: 0,
        present = summary?.present ?: 0,
        late = summary?.late ?: 0,
        leave = summary?.leave ?: 0,
        absent = summary?.absent ?: 0
    ),
    managers = managerList.orEmpty().mapNotNull { dto ->
        val userId = dto.userId ?: return@mapNotNull null
        TeamManager(
            userId = userId,
            fullName = dto.fullName?.trim().orEmpty(),
            designation = dto.designation?.trim().orEmpty(),
            locationPath = dto.locationPath?.trim().orEmpty(),
            status = dto.status.toTeamMemberStatus(),
            checkInTime = dto.checkInTime?.takeIf { it.isNotBlank() }
        )
    }
)

// The API sends "Absent" while the spec says "ABSENT", so match loosely.
private fun String?.toTeamMemberStatus(): TeamMemberStatus =
    when (this?.trim()?.uppercase()?.replace(" ", "_")) {
        "PRESENT" -> TeamMemberStatus.PRESENT
        "LATE" -> TeamMemberStatus.LATE
        "LEAVE", "ON_LEAVE" -> TeamMemberStatus.LEAVE
        "ABSENT" -> TeamMemberStatus.ABSENT
        else -> TeamMemberStatus.UNKNOWN
    }

/** Entries without an id can't be submitted, so they're dropped. */
fun List<LateReasonDto>.toLateReasons(): List<LateReason> = mapNotNull { dto ->
    dto.id?.let { LateReason(id = it, name = dto.name.orEmpty()) }
}

fun List<GeofenceConfigDto>.toGeofenceConfigs(): List<GeofenceConfig> = mapNotNull { dto ->
    val salespointId = dto.salespointId ?: return@mapNotNull null
    GeofenceConfig(
        salespointId = salespointId,
        checkInOption = when (dto.checkInOption) {
            1 -> CheckInOption.BLOCK
            2 -> CheckInOption.WARNING
            else -> CheckInOption.BYPASS
        },
        defaultRadiusMeters = dto.defaultRadiusMeters ?: 0
    )
}

fun List<DistributorDto>.toDistributors(): List<Distributor> = mapNotNull { dto ->
    val id = dto.id ?: return@mapNotNull null
    Distributor(
        id = id,
        name = dto.name.orEmpty(),
        code = dto.code.orEmpty(),
        address = dto.address.orEmpty(),
        location = parseCoordinates(dto.latitude, dto.longitude)
    )
}

internal fun parseCoordinates(latitude: String?, longitude: String?): GeoLocation? {
    val lat = latitude?.trim()?.toDoubleOrNull() ?: return null
    val lng = longitude?.trim()?.toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
    // 0,0 is what an unset location usually looks like, not a real distributor.
    if (lat == 0.0 && lng == 0.0) return null
    return GeoLocation(lat, lng)
}

private val TRAILING_OFFSET = Regex("""(Z|[+-]\d{2}:\d{2})$""")

private fun String?.trailingUtcOffset(): UtcOffset? {
    val match = this?.trim()?.let { TRAILING_OFFSET.find(it) } ?: return null
    return runCatching { UtcOffset.parse(match.value.replace("Z", "+00:00")) }.getOrNull()
}

/**
 * Parses an ISO date-time with an offset ("…+06:00" / "…Z"). One without an offset is read in
 * [fallbackOffset], or the device's timezone when that's unknown.
 */
@OptIn(ExperimentalTime::class)
private fun String?.parseEpochMillis(fallbackOffset: UtcOffset?): Long? {
    val value = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    runCatching { return Instant.parse(value).toEpochMilliseconds() }
    return runCatching {
        val local = LocalDateTime.parse(value)
        val instant = if (fallbackOffset != null) {
            local.toInstant(fallbackOffset)
        } else {
            local.toInstant(TimeZone.currentSystemDefault())
        }
        instant.toEpochMilliseconds()
    }.getOrNull()
}

private val DAY_MONTH_YEAR = Regex("""(\d{1,2})-(\d{1,2})-(\d{4})""")

/** Accepts ISO "yyyy-MM-dd" and the "dd-MM-yyyy" the monthly-summary calendar sends. */
private fun String?.parseLocalDate(): LocalDate? {
    val value = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    runCatching { return LocalDate.parse(value) }
    val (day, month, year) = DAY_MONTH_YEAR.matchEntire(value)?.destructured ?: return null
    return runCatching { LocalDate(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
}
