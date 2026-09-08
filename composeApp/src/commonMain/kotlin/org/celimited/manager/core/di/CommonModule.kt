package org.celimited.manager.core.di

import org.celimited.manager.core.common.DefaultDispatcherProvider
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.core.common.token.InMemoryTokenStorage
import org.celimited.manager.core.common.token.TokenStorage
import org.koin.dsl.module

val commonModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<TokenStorage> { InMemoryTokenStorage() }
}
