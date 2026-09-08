package org.celimited.manager.model.login

data class AuthUser(
    val userId: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val profilePictureUrl: String,
    val role: String,
    val status: String
)
