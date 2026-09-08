package org.celimited.manager.core.common

sealed class AppError {
    data object NoConnection : AppError()
    data object Timeout : AppError()
    data class Server(val code: Int, val message: String? = null) : AppError()
    data class Unauthorized(val message: String? = null) : AppError()
    data class Business(val message: String) : AppError()
    data class Serialization(val message: String? = null) : AppError()
    data class Unknown(val message: String? = null) : AppError()
}
