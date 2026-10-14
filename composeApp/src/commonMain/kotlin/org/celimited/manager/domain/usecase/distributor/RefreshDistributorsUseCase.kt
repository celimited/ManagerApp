package org.celimited.manager.domain.usecase.distributor

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.domain.repository.AttendanceRepository

/** Pull-to-refresh: fetches distributors again and updates the session cache on success. */
class RefreshDistributorsUseCase(
    private val repository: AttendanceRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(): DataResult<Unit> =
        withContext(dispatcherProvider.io) {
            repository.refreshDistributors()
        }
}
