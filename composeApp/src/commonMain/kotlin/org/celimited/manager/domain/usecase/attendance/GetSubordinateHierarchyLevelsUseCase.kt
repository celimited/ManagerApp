package org.celimited.manager.domain.usecase.attendance

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.domain.repository.AttendanceRepository
import org.celimited.manager.model.attendance.SubordinateHierarchy

class GetSubordinateHierarchyLevelsUseCase(
    private val repository: AttendanceRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(): DataResult<SubordinateHierarchy> =
        withContext(dispatcherProvider.io) { repository.getSubordinateHierarchyLevels() }
}
