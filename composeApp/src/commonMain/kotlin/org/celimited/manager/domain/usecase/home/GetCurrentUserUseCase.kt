package org.celimited.manager.domain.usecase.home

import org.celimited.manager.core.common.session.UserSession
import org.celimited.manager.model.login.AuthUser

class GetCurrentUserUseCase(
    private val userSession: UserSession
) {
    suspend operator fun invoke(): AuthUser? = userSession.getUser()
}
