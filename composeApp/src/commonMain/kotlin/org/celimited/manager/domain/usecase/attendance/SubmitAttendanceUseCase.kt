package org.celimited.manager.domain.usecase.attendance

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.core.common.device.DeviceInfoProvider
import org.celimited.manager.core.common.session.UserSession
import org.celimited.manager.domain.repository.AttendanceRepository
import org.celimited.manager.model.attendance.SaveAttendanceParams

/**
 * Saves today's check-in, then re-fetches today-status so the Home card reflects it. Returns the
 * API's success message.
 */
class SubmitAttendanceUseCase(
    private val repository: AttendanceRepository,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val userSession: UserSession,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(params: SaveAttendanceParams): DataResult<String> =
        withContext(dispatcherProvider.io) {
            val result = repository.saveAttendance(params, deviceInfoProvider.getDeviceInfo())
            if (result is DataResult.Success) {
                // Best effort: the save already succeeded and the cache is marked present.
                userSession.getUser()?.userId?.takeIf { it.isNotBlank() }?.let {
                    repository.refreshTodayStatus(it)
                }
            }
            result
        }
}
