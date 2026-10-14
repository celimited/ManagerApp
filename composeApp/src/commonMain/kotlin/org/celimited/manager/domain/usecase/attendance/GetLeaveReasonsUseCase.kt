package org.celimited.manager.domain.usecase.attendance

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.domain.repository.AttendanceRepository
import org.celimited.manager.model.attendance.LeaveReason

class GetLeaveReasonsUseCase(
    private val repository: AttendanceRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(): DataResult<List<LeaveReason>> =
        withContext(dispatcherProvider.io) { repository.getLeaveReasons() }
}
