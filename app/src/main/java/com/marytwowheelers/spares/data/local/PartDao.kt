package com.marytwowheelers.spares.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.marytwowheelers.spares.data.model.PartWithStock
import kotlinx.coroutines.flow.Flow

@Dao
interface PartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPart(part: PartEntity)

    @Query("SELECT * FROM parts WHERE id = :partId")
    suspend fun getPartById(partId: String): PartEntity?

    @Query("""
        SELECT COALESCE(SUM(m.delta), 0)
        FROM movements m
        WHERE m.partId = :partId
    """)
    suspend fun calculateStockForPart(partId: String): Int

    @Query("UPDATE parts SET name = :name, partNumber = :partNumber, shelfLocation = :shelfLocation, sellingPricePaise = :sellingPrice, mrpPaise = :mrp, updatedAt = :updatedAt, syncState = :syncState WHERE id = :partId")
    suspend fun updatePartMetadata(
        partId: String,
        name: String,
        partNumber: String,
        shelfLocation: String,
        sellingPrice: Long,
        mrp: Long,
        updatedAt: Long,
        syncState: SyncState
    ): Int

    @Query("UPDATE parts SET isDeleted = 1, updatedAt = :updatedAt, syncState = :syncState WHERE id = :partId")
    suspend fun softDeletePart(
        partId: String,
        updatedAt: Long,
        syncState: SyncState
    ): Int

    @Query("SELECT MAX(serialNumber) FROM parts WHERE isDeleted = 0")
    suspend fun getMaxSerialNumber(): Long?

    @Query("SELECT * FROM parts WHERE serialNumber = 0 AND isDeleted = 0 ORDER BY updatedAt ASC")
    suspend fun getPartsWithoutSerialNumber(): List<PartEntity>

    @Query("UPDATE parts SET serialNumber = :serialNumber WHERE id = :partId")
    suspend fun updateSerialNumber(partId: String, serialNumber: Long): Int

    @Query("""
        SELECT p.*, COALESCE(SUM(m.delta), 0) as currentStock
        FROM parts p
        LEFT JOIN movements m ON p.id = m.partId
        WHERE p.isDeleted = 0
        GROUP BY p.id
        ORDER BY p.serialNumber ASC, p.name ASC
    """)
    fun getAllPartsWithStock(): Flow<List<PartWithStock>>

    @Query("""
        SELECT p.*, COALESCE(SUM(m.delta), 0) as currentStock
        FROM parts p
        LEFT JOIN movements m ON p.id = m.partId
        WHERE p.id = :partId
        GROUP BY p.id
    """)
    fun getPartWithStock(partId: String): Flow<PartWithStock?>
    
    @Query("SELECT * FROM parts WHERE syncState = :state")
    suspend fun getPartsBySyncState(state: SyncState): List<PartEntity>
    
    @Query("UPDATE parts SET syncState = :newState WHERE id = :partId")
    suspend fun updateSyncState(partId: String, newState: SyncState): Int
}
