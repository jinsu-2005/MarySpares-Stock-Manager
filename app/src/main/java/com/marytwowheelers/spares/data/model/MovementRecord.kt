package com.marytwowheelers.spares.data.model

import com.marytwowheelers.spares.data.local.MovementType
import com.marytwowheelers.spares.data.local.SyncState

data class MovementRecord(
    val id: String,
    val partId: String,
    val delta: Int,
    val type: MovementType,
    val reason: String?,
    val snapshotCount: Int?,
    val previousRecordedStock: Int?,
    val timestamp: Long,
    val syncState: SyncState,
    val partName: String?,
    val partNumber: String?,
    val serialNumber: Long?,
    val isPartDeleted: Boolean? = false
)
