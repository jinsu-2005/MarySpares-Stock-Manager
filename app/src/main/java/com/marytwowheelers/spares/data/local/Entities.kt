package com.marytwowheelers.spares.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "parts")
data class PartEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val serialNumber: Long = 0L,
    val name: String = "",
    val partNumber: String = "",
    val shelfLocation: String = "",
    // Storing currency as Long (paise) to avoid floating point issues
    val sellingPricePaise: Long = 0L,
    val mrpPaise: Long = 0L,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncState: SyncState = SyncState.PENDING
) {
    constructor() : this(
        id = UUID.randomUUID().toString(),
        serialNumber = 0L,
        name = "",
        partNumber = "",
        shelfLocation = "",
        sellingPricePaise = 0L,
        mrpPaise = 0L,
        isDeleted = false,
        updatedAt = System.currentTimeMillis(),
        syncState = SyncState.PENDING
    )
}

@Entity(tableName = "movements")
data class MovementEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val partId: String = "",
    val delta: Int = 0,
    val type: MovementType = MovementType.ADD,
    val reason: String? = null,
    /**
     * For ADJUST movements:
     * - [snapshotCount] stores the exact physical count observed by the user.
     * - [previousRecordedStock] stores what the local device computed at the moment of adjustment.
     * - [delta] is (snapshotCount - previousRecordedStock).
     * This provides full auditability and enables epoch-based reconciliation.
     */
    val snapshotCount: Int? = null,
    val previousRecordedStock: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val syncState: SyncState = SyncState.PENDING
) {
    constructor() : this(
        id = UUID.randomUUID().toString(),
        partId = "",
        delta = 0,
        type = MovementType.ADD,
        reason = null,
        snapshotCount = null,
        previousRecordedStock = null,
        timestamp = System.currentTimeMillis(),
        syncState = SyncState.PENDING
    )
}

enum class MovementType {
    ADD, REMOVE, RETURN, ADJUST
}

enum class SyncState {
    PENDING, SYNCED, ERROR
}
