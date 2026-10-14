package org.celimited.manager.domain.usecase.distributor

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.core.common.device.DeviceInfoProvider
import org.celimited.manager.domain.repository.DistributorRepository
import org.celimited.manager.model.distributor.SaveIssueParams

/** Saves a new distributor issue. Returns the API's success message. */
class SaveIssueUseCase(
    private val repository: DistributorRepository,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(params: SaveIssueParams): DataResult<String> =
        withContext(dispatcherProvider.io) {
            repository.saveIssue(params, deviceInfoProvider.getDeviceInfo())
        }
}
