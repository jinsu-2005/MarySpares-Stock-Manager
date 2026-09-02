package com.marytwowheelers.spares.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MovementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: MovementEntity)

    @Query("SELECT * FROM movements WHERE partId = :partId ORDER BY timestamp DESC")
    fun getMovementsForPart(partId: String): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements ORDER BY timestamp DESC")
    fun getAllMovements(): Flow<List<MovementEntity>>

    @Query("""
        SELECT m.id, m.partId, m.delta, m.type, m.reason, m.snapshotCount, m.previousRecordedStock, m.timestamp, m.syncState,
               p.name AS partName, p.partNumber AS partNumber, p.serialNumber AS serialNumber
        FROM movements m
        LEFT JOIN parts p ON m.partId = p.id
        ORDER BY m.timestamp DESC
    """)
    fun getAllMovementsWithPart(): Flow<List<com.marytwowheelers.spares.data.model.MovementRecord>>

    @Query("SELECT * FROM movements WHERE syncState = :state")
    suspend fun getMovementsBySyncState(state: SyncState): List<MovementEntity>

    @Query("UPDATE movements SET syncState = :newState WHERE id = :movementId")
    suspend fun updateSyncState(movementId: String, newState: SyncState): Int

    @Query("SELECT * FROM movements WHERE timestamp <= :cutoffTimestamp ORDER BY timestamp ASC")
    suspend fun getMovementsOlderThan(cutoffTimestamp: Long): List<MovementEntity>

    @Query("SELECT COUNT(*) FROM movements WHERE timestamp <= :cutoffTimestamp")
    suspend fun countMovementsOlderThan(cutoffTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM movements")
    suspend fun countAllMovements(): Int

    @Query("SELECT * FROM movements ORDER BY timestamp ASC")
    suspend fun getAllMovementsDirect(): List<MovementEntity>

    @Query("DELETE FROM movements WHERE id IN (:ids)")
    suspend fun deleteMovementsByIds(ids: List<String>): Int

    @Query("DELETE FROM movements WHERE timestamp <= :cutoffTimestamp")
    suspend fun deleteMovementsOlderThan(cutoffTimestamp: Long): Int

    @Query("DELETE FROM movements")
    suspend fun deleteAllMovements(): Int
}
