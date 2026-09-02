package com.marytwowheelers.spares.sync

sealed class AppSyncStatus {
    /**
     * All local data is fully synchronized with Firebase cloud and device is online.
     */
    object Synced : AppSyncStatus()

    /**
     * Active background push/pull sync is currently running in real-time.
     */
    data class Syncing(val pendingCount: Int = 0) : AppSyncStatus()

    /**
     * Local changes exist and device is online, waiting for or queueing sync.
     */
    data class PendingChanges(val pendingCount: Int) : AppSyncStatus()

    /**
     * Local unsynchronized changes exist while device is completely offline (no Wi-Fi or Cellular).
     */
    data class PendingChangesOffline(val pendingCount: Int) : AppSyncStatus()

    /**
     * Device is offline with zero pending changes.
     */
    object Offline : AppSyncStatus()
}
