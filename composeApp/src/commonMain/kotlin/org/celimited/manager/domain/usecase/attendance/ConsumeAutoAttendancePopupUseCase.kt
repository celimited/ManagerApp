package org.celimited.manager.domain.usecase.attendance

import org.celimited.manager.domain.repository.AttendanceRepository

/** True only the first time Home asks after a login — the popup auto-shows once. */
class ConsumeAutoAttendancePopupUseCase(
    private val repository: AttendanceRepository
) {
    operator fun invoke(): Boolean = repository.consumeAutoPopup()
}
