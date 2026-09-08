package org.celimited.manager.core.common

sealed class DataResult<out T> {
    data class Success<out T>(val data: T) : DataResult<T>()
    data class Error(val error: AppError) : DataResult<Nothing>()
    data object Loading : DataResult<Nothing>()
}
