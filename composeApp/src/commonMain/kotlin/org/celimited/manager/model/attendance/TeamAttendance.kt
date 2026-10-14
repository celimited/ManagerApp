package org.celimited.manager.model.attendance

/** The levels below the logged-in user, shown as the Team Attendance tabs. */
data class SubordinateHierarchy(
    val currentUserRole: String,
    val levels: List<HierarchyLevel>
)

data class HierarchyLevel(
    /** Sent back as `levelKey` when fetching that level's team summary, e.g. "DIVISION". */
    val key: String,
    val name: String,
    val presentCount: Int,
    val totalCount: Int
)

/** Today's attendance for one hierarchy level. */
data class TeamAttendanceSummary(
    val levelKey: String,
    val counts: TeamAttendanceCounts,
    val managers: List<TeamManager>
)

data class TeamAttendanceCounts(
    val total: Int,
    val present: Int,
    val late: Int,
    val leave: Int,
    val absent: Int
)

data class TeamManager(
    val userId: Int,
    val fullName: String,
    val designation: String,
    val locationPath: String,
    val status: TeamMemberStatus,
    /** Server-formatted, e.g. "09:05 AM"; null when not checked in. */
    val checkInTime: String?
)

enum class TeamMemberStatus { PRESENT, LATE, LEAVE, ABSENT, UNKNOWN }
