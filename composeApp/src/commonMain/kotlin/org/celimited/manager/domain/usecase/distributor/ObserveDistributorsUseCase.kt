package org.celimited.manager.domain.usecase.distributor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.celimited.manager.domain.repository.AttendanceRepository
import org.celimited.manager.model.attendance.Distributor

/** Distributors cached at login. Emits null while nothing has loaded (e.g. the login-time fetch failed). */
class ObserveDistributorsUseCase(
    private val repository: AttendanceRepository
) {
    operator fun invoke(): Flow<List<Distributor>?> =
        repository.observeAttendanceData()
            .map { it?.distributors }
            .distinctUntilChanged()
}
