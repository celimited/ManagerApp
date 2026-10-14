package org.celimited.manager.domain.usecase.attendance

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.core.common.device.DeviceInfoProvider
import org.celimited.manager.domain.repository.AttendanceRepository
import org.celimited.manager.model.attendance.ApplyLeaveParams

/** Submits a leave application. Returns the API's success message. */
class ApplyLeaveUseCase(
    private val repository: AttendanceRepository,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(params: ApplyLeaveParams): DataResult<String> =
        withContext(dispatcherProvider.io) {
            repository.applyLeave(params, deviceInfoProvider.getDeviceInfo())
        }
}
