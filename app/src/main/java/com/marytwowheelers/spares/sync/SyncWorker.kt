package com.marytwowheelers.spares.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.marytwowheelers.spares.data.local.AppDatabase
import com.marytwowheelers.spares.data.local.MovementEntity
import com.marytwowheelers.spares.data.local.MovementType
import com.marytwowheelers.spares.data.local.PartEntity
import com.marytwowheelers.spares.data.local.SyncState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        // Global in-process mutex guarantees strict mutual exclusion across all sync triggers
        private val syncMutex = Mutex()

        suspend fun performSync(context: Context): Boolean {
            return syncMutex.withLock {
                val database = AppDatabase.getDatabase(context)
                val partDao = database.partDao()
                val movementDao = database.movementDao()
                val firestore = FirebaseFirestore.getInstance()
                val auth = FirebaseAuth.getInstance()

                val user = auth.currentUser
                if (user == null) {
                    Log.w(TAG, "User not logged in, skipping sync")
                    return@withLock false
                }

                SyncManager.setSyncActive(true)
                try {
                    // 1. PUSH: Push pending local Parts to shared 'parts' collection
                    val pendingParts = partDao.getPartsBySyncState(SyncState.PENDING)
                    Log.d(TAG, "Pushing ${pendingParts.size} pending parts to cloud")
                    for (part in pendingParts) {
                        val partMap = mapOf(
                            "id" to part.id,
                            "serialNumber" to part.serialNumber,
                            "name" to part.name,
                            "partNumber" to part.partNumber,
                            "shelfLocation" to part.shelfLocation,
                            "sellingPricePaise" to part.sellingPricePaise,
                            "mrpPaise" to part.mrpPaise,
                            "isDeleted" to part.isDeleted,
                            "updatedAt" to part.updatedAt,
                            "syncState" to SyncState.SYNCED.name
                        )
                        firestore.collection("parts").document(part.id)
                            .set(partMap)
                            .await()
                        partDao.updateSyncState(part.id, SyncState.SYNCED)
                    }

                    // 2. PUSH: Push pending local Movements to shared 'movements' collection
                    val pendingMovements = movementDao.getMovementsBySyncState(SyncState.PENDING)
                    Log.d(TAG, "Pushing ${pendingMovements.size} pending movements to cloud")
                    for (movement in pendingMovements) {
                        val movementMap = mutableMapOf<String, Any?>(
                            "id" to movement.id,
                            "partId" to movement.partId,
                            "delta" to movement.delta,
                            "type" to movement.type.name,
                            "reason" to movement.reason,
                            "snapshotCount" to movement.snapshotCount,
                            "previousRecordedStock" to movement.previousRecordedStock,
                            "timestamp" to movement.timestamp,
                            "syncState" to SyncState.SYNCED.name
                        )
                        firestore.collection("movements").document(movement.id)
                            .set(movementMap)
                            .await()
                        movementDao.updateSyncState(movement.id, SyncState.SYNCED)
                    }

                    // 3. PULL: Pull remote Parts and merge with LWW (Last-Write-Wins based on updatedAt)
                    val remotePartsSnapshot = firestore.collection("parts")
                        .get()
                        .await()

                    Log.d(TAG, "Pulled ${remotePartsSnapshot.size()} parts from cloud")
                    for (doc in remotePartsSnapshot.documents) {
                        val remotePart = doc.toPartEntity() ?: continue
                        val localPart = partDao.getPartById(remotePart.id)

                        if (localPart == null) {
                            // Part created on another device / restored from cloud -> insert locally
                            partDao.insertPart(remotePart.copy(syncState = SyncState.SYNCED))
                        } else if (localPart.syncState == SyncState.SYNCED) {
                            // If local is not dirty, accept remote if newer
                            if (remotePart.updatedAt >= localPart.updatedAt) {
                                partDao.insertPart(remotePart.copy(syncState = SyncState.SYNCED))
                            }
                        } else if (localPart.syncState == SyncState.PENDING) {
                            // Local has unsynced changes: LWW resolution
                            if (remotePart.updatedAt > localPart.updatedAt) {
                                partDao.insertPart(remotePart.copy(syncState = SyncState.SYNCED))
                            }
                        }
                    }

                    // 4. PULL: Pull remote Movements and merge (Insert if not exists)
                    val remoteMovementsSnapshot = firestore.collection("movements")
                        .get()
                        .await()

                    Log.d(TAG, "Pulled ${remoteMovementsSnapshot.size()} movements from cloud")
                    for (doc in remoteMovementsSnapshot.documents) {
                        val remoteMovement = doc.toMovementEntity() ?: continue
                        // Insert movement if not already present
                        movementDao.insertMovement(remoteMovement.copy(syncState = SyncState.SYNCED))
                    }

                    SyncManager.setLastSyncTimestamp(System.currentTimeMillis())
                    Log.d(TAG, "Sync successfully completed")
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Sync failed with exception", e)
                    false
                } finally {
                    SyncManager.setSyncActive(false)
                }
            }
        }

        private fun DocumentSnapshot.toPartEntity(): PartEntity? {
            return try {
                val docId = id
                val name = getString("name") ?: return null
                val partNumber = getString("partNumber") ?: ""
                val shelfLocation = getString("shelfLocation") ?: ""
                val serialNumber = getLong("serialNumber") ?: 0L
                val sellingPricePaise = getLong("sellingPricePaise") ?: 0L
                val mrpPaise = getLong("mrpPaise") ?: 0L
                val isDeleted = getBoolean("isDeleted") ?: getBoolean("deleted") ?: false
                val updatedAt = getLong("updatedAt") ?: System.currentTimeMillis()
                val syncStateStr = getString("syncState") ?: "SYNCED"
                val syncState = try { SyncState.valueOf(syncStateStr) } catch (e: Exception) { SyncState.SYNCED }

                PartEntity(
                    id = docId,
                    serialNumber = serialNumber,
                    name = name,
                    partNumber = partNumber,
                    shelfLocation = shelfLocation,
                    sellingPricePaise = sellingPricePaise,
                    mrpPaise = mrpPaise,
                    isDeleted = isDeleted,
                    updatedAt = updatedAt,
                    syncState = syncState
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing PartEntity doc $id", e)
                null
            }
        }

        private fun DocumentSnapshot.toMovementEntity(): MovementEntity? {
            return try {
                val docId = id
                val partId = getString("partId") ?: return null
                val delta = getLong("delta")?.toInt() ?: 0
                val typeStr = getString("type") ?: "ADD"
                val type = try { MovementType.valueOf(typeStr) } catch (e: Exception) { MovementType.ADD }
                val reason = getString("reason")
                val snapshotCount = getLong("snapshotCount")?.toInt()
                val previousRecordedStock = getLong("previousRecordedStock")?.toInt()
                val timestamp = getLong("timestamp") ?: System.currentTimeMillis()
                val syncStateStr = getString("syncState") ?: "SYNCED"
                val syncState = try { SyncState.valueOf(syncStateStr) } catch (e: Exception) { SyncState.SYNCED }

                MovementEntity(
                    id = docId,
                    partId = partId,
                    delta = delta,
                    type = type,
                    reason = reason,
                    snapshotCount = snapshotCount,
                    previousRecordedStock = previousRecordedStock,
                    timestamp = timestamp,
                    syncState = syncState
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing MovementEntity doc $id", e)
                null
            }
        }
    }

    override suspend fun doWork(): Result {
        val success = performSync(applicationContext)
        return if (success) Result.success() else Result.retry()
    }
}
