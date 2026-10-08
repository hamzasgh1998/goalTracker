package com.milestone.app.data.sync

import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncResult {
    data object NotConfigured : SyncResult
    data class Success(val changedRows: Int) : SyncResult
    data class Failed(val message: String) : SyncResult
}

/**
 * Seam for a future backup/sync backend.
 *
 * The app is offline-first and fully functional with no implementation behind this
 * interface — [LocalOnlySyncGateway] is the one that ships. It exists so that
 * adding sync later is a new binding plus a settings toggle, rather than a rewrite
 * of every repository.
 *
 * Nothing in the UI may treat sync as required: every caller has to handle
 * [SyncResult.NotConfigured] as the ordinary, expected case.
 */
interface RemoteSyncGateway {
    val isConfigured: Boolean

    suspend fun push(): SyncResult

    suspend fun pull(): SyncResult
}

/**
 * The shipping implementation: does nothing, successfully.
 *
 * Keeps the data layer honest about the seam without pulling in any paid service.
 */
@Singleton
class LocalOnlySyncGateway @Inject constructor() : RemoteSyncGateway {
    override val isConfigured: Boolean = false

    override suspend fun push(): SyncResult = SyncResult.NotConfigured

    override suspend fun pull(): SyncResult = SyncResult.NotConfigured
}
