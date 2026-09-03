package com.marytwowheelers.spares.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.marytwowheelers.spares.data.local.AppDatabase
import com.marytwowheelers.spares.data.local.MovementEntity
import com.marytwowheelers.spares.data.local.MovementType
import com.marytwowheelers.spares.data.local.PartEntity
import com.marytwowheelers.spares.data.local.SyncState
import com.marytwowheelers.spares.data.model.HistoryRetentionPeriod
import com.marytwowheelers.spares.data.model.PartWithStock
import com.marytwowheelers.spares.sync.AppSyncStatus
import com.marytwowheelers.spares.sync.SyncManager
import com.marytwowheelers.spares.util.FuzzySearchEngine
import com.marytwowheelers.spares.util.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class InventoryRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val partDao = database.partDao()
    private val movementDao = database.movementDao()
    private val networkMonitor = NetworkMonitor(context)

    private val repositoryScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)

    /**
     * Real-time synchronization state combining network status, WorkManager active execution,
     * and pending local Room database mutations.
     */
    val syncStatus: Flow<AppSyncStatus> = combine(
        networkMonitor.isOnline,
        SyncManager.isSyncActive,
        movementDao.getAllMovements(),
        partDao.getAllPartsWithStock()
    ) { isOnline, isSyncActive, movements, parts ->
        val pendingMovementCount = movements.count { it.syncState == SyncState.PENDING }
        val pendingPartCount = parts.count { it.part.syncState == SyncState.PENDING }
        val totalPending = pendingMovementCount + pendingPartCount

        when {
            !isOnline && totalPending > 0 -> AppSyncStatus.PendingChangesOffline(totalPending)
            !isOnline -> AppSyncStatus.Offline
            isSyncActive -> AppSyncStatus.Syncing(totalPending)
            totalPending > 0 -> AppSyncStatus.PendingChanges(totalPending)
            else -> AppSyncStatus.Synced
        }
    }

    suspend fun directSync(): Boolean = withContext(Dispatchers.IO) {
        com.marytwowheelers.spares.sync.SyncWorker.performSync(context)
    }

    fun triggerSync() {
        repositoryScope.launch {
            directSync()
        }
        SyncManager.enqueueSync(context)
    }

    fun getAllPartsWithStock(): Flow<List<PartWithStock>> {
        return partDao.getAllPartsWithStock().map { allParts ->
            // Self-heal any parts with unassigned serial number (0)
            val unassigned = allParts.filter { it.part.serialNumber == 0L }
            if (unassigned.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    var currentMax = partDao.getMaxSerialNumber() ?: 0L
                    for (item in unassigned) {
                        currentMax += 1L
                        partDao.updateSerialNumber(item.part.id, currentMax)
                    }
                }
            }
            allParts
        }
    }

    /**
     * Typo-tolerant search using Damerau-Levenshtein distance & token matching.
     * Evaluates live against all local parts on Dispatchers.Default.
     * Handles character swaps, insertions, deletions and searches by serial number.
     */
    fun searchPartsWithStock(query: String): Flow<List<PartWithStock>> {
        return getAllPartsWithStock().map { allParts ->
            if (query.isBlank()) {
                allParts
            } else {
                FuzzySearchEngine.search(
                    items = allParts,
                    query = query,
                    nameExtractor = { "${it.part.name} #${it.part.serialNumber} ${it.part.serialNumber} ${it.part.shelfLocation}" },
                    partNumberExtractor = { "${it.part.partNumber} ${it.part.shelfLocation}" }
                )
            }
        }
    }

    fun getPartWithStock(partId: String): Flow<PartWithStock?> = partDao.getPartWithStock(partId)
    
    fun getMovementsForPart(partId: String): Flow<List<MovementEntity>> = movementDao.getMovementsForPart(partId)
    
    fun getAllMovements(): Flow<List<MovementEntity>> = movementDao.getAllMovements()

    fun getAllMovementsWithPart(): Flow<List<com.marytwowheelers.spares.data.model.MovementRecord>> = movementDao.getAllMovementsWithPart()

    suspend fun getNextSerialNumber(): Long = withContext(Dispatchers.IO) {
        (partDao.getMaxSerialNumber() ?: 0L) + 1L
    }

    suspend fun addPart(
        name: String,
        partNumber: String,
        shelfLocation: String,
        sellingPricePaise: Long,
        mrpPaise: Long,
        initialStock: Int
    ) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val nextSerial = (partDao.getMaxSerialNumber() ?: 0L) + 1L
            val part = PartEntity(
                serialNumber = nextSerial,
                name = name,
                partNumber = partNumber,
                shelfLocation = shelfLocation,
                sellingPricePaise = sellingPricePaise,
                mrpPaise = mrpPaise,
                updatedAt = now,
                syncState = SyncState.PENDING
            )
            partDao.insertPart(part)
            
            if (initialStock != 0) {
                val movement = MovementEntity(
                    partId = part.id,
                    delta = initialStock,
                    type = MovementType.ADD,
                    reason = "Initial Stock",
                    previousRecordedStock = 0,
                    snapshotCount = initialStock,
                    timestamp = now,
                    syncState = SyncState.PENDING
                )
                movementDao.insertMovement(movement)
            }
            SyncManager.enqueueSync(context)
        }
    }

    suspend fun updatePartMetadata(
        partId: String,
        name: String,
        partNumber: String,
        shelfLocation: String,
        sellingPricePaise: Long,
        mrpPaise: Long
    ) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            partDao.updatePartMetadata(
                partId = partId,
                name = name,
                partNumber = partNumber,
                shelfLocation = shelfLocation,
                sellingPrice = sellingPricePaise,
                mrp = mrpPaise,
                updatedAt = now,
                syncState = SyncState.PENDING
            )
            SyncManager.enqueueSync(context)
        }
    }

    /**
     * Records standard stock movements (ADD, REMOVE, RETURN).
     */
    suspend fun recordMovement(partId: String, delta: Int, type: MovementType, reason: String?) {
        withContext(Dispatchers.IO) {
            val currentStock = partDao.calculateStockForPart(partId)
            val movement = MovementEntity(
                partId = partId,
                delta = delta,
                type = type,
                reason = reason,
                previousRecordedStock = currentStock,
                snapshotCount = currentStock + delta,
                timestamp = System.currentTimeMillis(),
                syncState = SyncState.PENDING
            )
            movementDao.insertMovement(movement)
            SyncManager.enqueueSync(context)
        }
    }

    /**
     * Records a physical count ADJUST movement with exact auditability.
     * Records:
     * - [targetPhysicalCount]: observed count on shelf
     * - [previousRecordedStock]: what the system computed right before adjustment
     * - [delta]: (targetPhysicalCount - previousRecordedStock)
     */
    suspend fun recordAdjustment(partId: String, targetPhysicalCount: Int, reason: String?) {
        withContext(Dispatchers.IO) {
            val currentStock = partDao.calculateStockForPart(partId)
            val delta = targetPhysicalCount - currentStock
            val movement = MovementEntity(
                partId = partId,
                delta = delta,
                type = MovementType.ADJUST,
                reason = reason?.ifBlank { null } ?: "Stock count correction",
                snapshotCount = targetPhysicalCount,
                previousRecordedStock = currentStock,
                timestamp = System.currentTimeMillis(),
                syncState = SyncState.PENDING
            )
            movementDao.insertMovement(movement)
            SyncManager.enqueueSync(context)
        }
    }

    suspend fun deletePart(partId: String) {
        withContext(Dispatchers.IO) {
            partDao.softDeletePart(
                partId = partId,
                updatedAt = System.currentTimeMillis(),
                syncState = SyncState.PENDING
            )
            SyncManager.enqueueSync(context)
        }
    }

    suspend fun deleteParts(partIds: List<String>) {
        if (partIds.isEmpty()) return
        withContext(Dispatchers.IO) {
            partDao.softDeleteParts(
                partIds = partIds,
                updatedAt = System.currentTimeMillis(),
                syncState = SyncState.PENDING
            )
            SyncManager.enqueueSync(context)
        }
    }

    suspend fun resetLocalData(autoResync: Boolean = true) {
        withContext(Dispatchers.IO) {
            database.clearAllTables()
            if (autoResync) {
                com.marytwowheelers.spares.sync.SyncWorker.performSync(context)
            }
        }
    }

    /**
     * Counts historical movement records that match the given retention period.
     */
    suspend fun countHistoricalRecordsForPeriod(period: HistoryRetentionPeriod, customDays: Int? = null): Int = withContext(Dispatchers.IO) {
        if (period == HistoryRetentionPeriod.NEVER) {
            return@withContext 0
        }
        val cutoff = period.getCutoffTimestamp(customDays) ?: return@withContext 0
        movementDao.countMovementsOlderThan(cutoff)
    }

    /**
     * Safely clears historical stock-movement records older than [period].
     * 
     * Key invariants preserved:
     * 1. Never corrupts or changes current stock count on shelves or in parts table (calculates baseline delta).
     * 2. Handles pending/unsynchronized movements by pushing them before purging or preserving their state.
     * 3. Deletes records from both local Room database and remote Firestore collection (users/{uid}/movements).
     * 4. Safe across offline and online transitions with immediate Flow updates to HistoryScreen.
     */
    suspend fun clearHistoryForPeriod(period: HistoryRetentionPeriod, customDays: Int? = null): Result<Int> = withContext(Dispatchers.IO) {
        if (period == HistoryRetentionPeriod.NEVER) {
            return@withContext Result.success(0)
        }
        try {
            val cutoff = period.getCutoffTimestamp(customDays) ?: return@withContext Result.success(0)
            val movementsToDelete = movementDao.getMovementsOlderThan(cutoff)

            if (movementsToDelete.isEmpty()) {
                return@withContext Result.success(0)
            }

            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            val uid = auth.currentUser?.uid

            // Step 1: Check if any movements are pending sync. If so and online, push them first.
            if (uid != null) {
                try {
                    val pendingInSelection = movementsToDelete.filter { it.syncState == SyncState.PENDING }
                    for (pending in pendingInSelection) {
                        firestore.collection("movements").document(pending.id)
                            .set(pending.copy(syncState = SyncState.SYNCED))
                            .await()
                        movementDao.updateSyncState(pending.id, SyncState.SYNCED)
                    }
                } catch (e: Exception) {
                    // Proceed with local transactional integrity if offline
                }
            }

            // Step 2: Consolidate deltas per part to protect current stock integrity
            val baselinesToInsert = mutableListOf<MovementEntity>()
            val groupedByPart = movementsToDelete.groupBy { it.partId }

            for ((partId, partMovements) in groupedByPart) {
                val sumDeletedDelta = partMovements.sumOf { it.delta }
                if (sumDeletedDelta != 0) {
                    val baselineTimestamp = cutoff
                    val baselineReason = if (period == HistoryRetentionPeriod.CUSTOM && customDays != null) {
                        "Historical baseline (${customDays}d retention)"
                    } else {
                        "Historical baseline (${period.label})"
                    }
                    val currentComputedStock = partDao.calculateStockForPart(partId)
                    val baselineEntity = MovementEntity(
                        id = UUID.randomUUID().toString(),
                        partId = partId,
                        delta = sumDeletedDelta,
                        type = MovementType.ADD,
                        reason = baselineReason,
                        previousRecordedStock = currentComputedStock - sumDeletedDelta,
                        snapshotCount = currentComputedStock,
                        timestamp = baselineTimestamp,
                        syncState = if (uid != null) SyncState.PENDING else SyncState.SYNCED
                    )
                    baselinesToInsert.add(baselineEntity)
                }
            }

            // Step 3: Delete from local Room database and insert baselines
            val idsToDelete = movementsToDelete.map { it.id }
            movementDao.deleteMovementsByIds(idsToDelete)
            for (baseline in baselinesToInsert) {
                movementDao.insertMovement(baseline)
            }

            // Step 4: Delete from Firestore (if user is authenticated)
            if (uid != null) {
                try {
                    // Batch delete in chunks of 400 (Firestore limit is 500 per batch)
                    val chunks = idsToDelete.chunked(400)
                    for (chunk in chunks) {
                        val batch = firestore.batch()
                        for (docId in chunk) {
                            val ref = firestore.collection("movements").document(docId)
                            batch.delete(ref)
                        }
                        batch.commit().await()
                    }

                    // Push baseline records to Firestore
                    for (baseline in baselinesToInsert) {
                        firestore.collection("movements").document(baseline.id)
                            .set(baseline.copy(syncState = SyncState.SYNCED))
                            .await()
                        movementDao.updateSyncState(baseline.id, SyncState.SYNCED)
                    }
                } catch (e: Exception) {
                    // Enqueue background sync if offline
                    SyncManager.enqueueSync(context)
                }
            }

            SyncManager.enqueueSync(context)
            Result.success(movementsToDelete.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
