package com.milestone.app.di

import com.milestone.app.core.time.AppClock
import com.milestone.app.core.time.SystemAppClock
import com.milestone.app.data.sync.LocalOnlySyncGateway
import com.milestone.app.data.sync.RemoteSyncGateway
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Scope for work that must outlive any one screen, e.g. first-run seeding. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindClock(impl: SystemAppClock): AppClock

    /**
     * Binds the no-op sync gateway.
     *
     * Swapping this one line is the whole cost of adding a real backend later;
     * nothing else in the app knows which implementation it is talking to.
     */
    @Binds
    @Singleton
    abstract fun bindSyncGateway(impl: LocalOnlySyncGateway): RemoteSyncGateway

    companion object {
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope =
            // SupervisorJob so a failure in one background task can't cancel the rest.
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
