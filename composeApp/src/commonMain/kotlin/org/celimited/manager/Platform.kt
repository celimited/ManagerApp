package org.celimited.manager

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform