package com.marytwowheelers.spares.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.marytwowheelers.spares.data.local.MovementType
import com.marytwowheelers.spares.data.local.StockAlertManager
import com.marytwowheelers.spares.data.model.HistoryRetentionPeriod
import com.marytwowheelers.spares.data.model.PartWithStock
import com.marytwowheelers.spares.data.model.UserRole
import com.marytwowheelers.spares.data.repository.AccessRepository
import com.marytwowheelers.spares.data.repository.InventoryRepository
import com.marytwowheelers.spares.sync.AppSyncStatus
import com.marytwowheelers.spares.util.CsvExporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {
    init {
        repository.triggerSync()
    }

    val syncStatus = repository.syncStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSyncStatus.Synced
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val partsList: StateFlow<List<PartWithStock>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllPartsWithStock()
            } else {
                repository.searchPartsWithStock(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun triggerSync() {
        repository.triggerSync()
    }

    fun addPart(
        name: String,
        partNumber: String,
        shelfLocation: String,
        sellingPricePaise: Long,
        mrpPaise: Long,
        initialStock: Int
    ) {
        viewModelScope.launch {
            repository.addPart(name, partNumber, shelfLocation, sellingPricePaise, mrpPaise, initialStock)
        }
    }
}

class DashboardViewModel(private val repository: InventoryRepository) : ViewModel() {
    init {
        repository.triggerSync()
    }

    val partsList = repository.getAllPartsWithStock().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val syncStatus = repository.syncStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSyncStatus.Synced
    )

    fun triggerSync() {
        repository.triggerSync()
    }

    fun addPart(
        name: String,
        partNumber: String,
        shelfLocation: String,
        sellingPricePaise: Long,
        mrpPaise: Long,
        initialStock: Int
    ) {
        viewModelScope.launch {
            repository.addPart(name, partNumber, shelfLocation, sellingPricePaise, mrpPaise, initialStock)
        }
    }

    fun recordMovement(partId: String, delta: Int, type: MovementType, reason: String? = null) {
        viewModelScope.launch {
            repository.recordMovement(partId, delta, type, reason)
        }
    }
}

class PartDetailsViewModel(private val repository: InventoryRepository) : ViewModel() {
    private val _partId = MutableStateFlow<String?>(null)

    val syncStatus = repository.syncStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSyncStatus.Synced
    )

    fun triggerSync() {
        repository.triggerSync()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val partDetails = _partId
        .flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(null)
            else repository.getPartWithStock(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val movements = _partId
        .flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.getMovementsForPart(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun loadPart(id: String) {
        _partId.value = id
    }

    fun recordMovement(delta: Int, type: MovementType, reason: String? = null) {
        val id = _partId.value ?: return
        viewModelScope.launch {
            repository.recordMovement(id, delta, type, reason)
        }
    }

    fun recordAdjustment(targetPhysicalCount: Int, reason: String? = null) {
        val id = _partId.value ?: return
        viewModelScope.launch {
            repository.recordAdjustment(id, targetPhysicalCount, reason)
        }
    }

    fun updateMetadata(
        name: String,
        partNumber: String,
        shelfLocation: String,
        sellingPricePaise: Long,
        mrpPaise: Long
    ) {
        val id = _partId.value ?: return
        viewModelScope.launch {
            repository.updatePartMetadata(id, name, partNumber, shelfLocation, sellingPricePaise, mrpPaise)
        }
    }

    fun deletePart(onDeleted: () -> Unit) {
        val id = _partId.value ?: return
        viewModelScope.launch {
            repository.deletePart(id)
            onDeleted()
        }
    }
}

class HistoryViewModel(private val repository: InventoryRepository) : ViewModel() {
    val allMovements = repository.getAllMovementsWithPart().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val syncStatus = repository.syncStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSyncStatus.Synced
    )
}

class SettingsViewModel(
    private val repository: InventoryRepository,
    private val accessRepository: AccessRepository
) : ViewModel() {
    val partsList = repository.getAllPartsWithStock().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val syncStatus = repository.syncStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSyncStatus.Synced
    )

    val accessMembers = accessRepository.members.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val currentUserRole: StateFlow<UserRole> = accessRepository.currentUserRole.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserRole.STAFF
    )

    fun triggerSync() {
        repository.triggerSync()
    }

    fun addMemberInvitation(
        email: String,
        name: String,
        role: UserRole,
        invitedBy: String = "Admin",
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = accessRepository.addMemberInvitation(email, name, role, invitedBy)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Failed to add member invitation")
            }
        }
    }

    fun updateMemberRole(emailOrId: String, newRole: UserRole, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = accessRepository.updateMemberRole(emailOrId, newRole)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Failed to update member role")
            }
        }
    }

    fun removeMemberInvitation(emailOrId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = accessRepository.removeMemberInvitation(emailOrId)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Failed to remove member")
            }
        }
    }

    fun resetLocalDatabase(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.resetLocalData(autoResync = true)
            onComplete()
        }
    }

    suspend fun countHistoricalRecords(
        period: HistoryRetentionPeriod,
        customDays: Int? = null
    ): Int {
        return repository.countHistoricalRecordsForPeriod(period, customDays)
    }

    fun clearHistory(
        period: HistoryRetentionPeriod,
        customDays: Int? = null,
        onSuccess: (deletedCount: Int) -> Unit,
        onError: (message: String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.clearHistoryForPeriod(period, customDays)
            if (result.isSuccess) {
                onSuccess(result.getOrDefault(0))
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "Failed to clear history")
            }
        }
    }

    /**
     * Highly destructive Admin-only operation:
     * 1. Fetches all Firestore collections (parts, movements, users, invitations)
     * 2. Exports them to a ZIP archive or CSV backup at the user-chosen location
     * 3. Deletes all documents in Firestore upon verification
     * 4. Clears local SQLite Room database cache & resets stock alerts
     * 5. Re-bootstraps initial Admin and Owner invitations
     */
    fun deleteEntireCloudDatabase(
        context: Context,
        targetUri: Uri? = null,
        onProgress: (String) -> Unit,
        onSuccess: (backupPath: String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()

                onProgress("Fetching cloud database records for backup...")

                val partsSnap = firestore.collection("parts").get().await()
                val movementsSnap = firestore.collection("movements").get().await()
                val usersSnap = firestore.collection("users").get().await()
                val invitesSnap = firestore.collection("invitations").get().await()

                val partsList = partsSnap.documents.map { it.data ?: emptyMap() }
                val movementsList = movementsSnap.documents.map { it.data ?: emptyMap() }
                val usersList = usersSnap.documents.map { it.data ?: emptyMap() }
                val invitesList = invitesSnap.documents.map { it.data ?: emptyMap() }

                var backupPathDescription = "User Selected Directory"

                if (targetUri != null) {
                    onProgress("Writing full ZIP backup to selected location...")
                    val zipResult = CsvExporter.exportCloudBackupToZipUri(
                        context = context,
                        uri = targetUri,
                        parts = partsList,
                        movements = movementsList,
                        users = usersList,
                        invitations = invitesList
                    )
                    if (zipResult.isFailure) {
                        onError("Cloud deletion aborted: Failed to write ZIP backup (${zipResult.exceptionOrNull()?.localizedMessage})")
                        return@launch
                    }
                    backupPathDescription = "Chosen Folder"
                } else {
                    onProgress("Exporting CSV backup files to local device storage...")
                    val exportResult = CsvExporter.exportCloudBackupToDevice(
                        context = context,
                        parts = partsList,
                        movements = movementsList,
                        users = usersList,
                        invitations = invitesList
                    )

                    if (exportResult.isFailure) {
                        onError("Cloud deletion aborted: Failed to save local CSV backup (${exportResult.exceptionOrNull()?.localizedMessage})")
                        return@launch
                    }

                    val backupFiles = exportResult.getOrNull() ?: emptyList()
                    if (backupFiles.isEmpty()) {
                        onError("Cloud deletion aborted: Backup verification failed (No backup files created).")
                        return@launch
                    }
                    backupPathDescription = backupFiles.first().parent ?: "Device Documents"
                }

                onProgress("Backup verified. Deleting Firestore cloud collections...")
                kotlinx.coroutines.delay(400)

                // Delete all documents in parts
                for (doc in partsSnap.documents) {
                    doc.reference.delete().await()
                }

                // Delete all documents in movements
                for (doc in movementsSnap.documents) {
                    doc.reference.delete().await()
                }

                // Delete all documents in users
                for (doc in usersSnap.documents) {
                    doc.reference.delete().await()
                }

                // Delete all documents in invitations (except we will recreate the 2 root accounts)
                for (doc in invitesSnap.documents) {
                    doc.reference.delete().await()
                }

                onProgress("Wiping local database cache & resetting alert indices...")
                kotlinx.coroutines.delay(400)
                repository.resetLocalData(autoResync = false)
                StockAlertManager.clearAllReviewed(context)

                onProgress("Re-bootstrapping initial Admin & Owner accounts...")
                kotlinx.coroutines.delay(400)
                // Bootstrap Admin
                firestore.collection("invitations").document("jinsu.j2005@gmail.com").set(
                    hashMapOf(
                        "email" to "jinsu.j2005@gmail.com",
                        "name" to "Admin",
                        "role" to UserRole.ADMIN.name,
                        "status" to "ACTIVE",
                        "invitedBy" to "System Bootstrap",
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()

                // Bootstrap Owner
                firestore.collection("invitations").document("jinsukapgreen@gmail.com").set(
                    hashMapOf(
                        "email" to "jinsukapgreen@gmail.com",
                        "name" to "Owner",
                        "role" to UserRole.OWNER.name,
                        "status" to "ACTIVE",
                        "invitedBy" to "System Bootstrap",
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()

                kotlinx.coroutines.delay(300)
                onSuccess(backupPathDescription)
            } catch (e: Exception) {
                onError("Cloud deletion failed: ${e.localizedMessage}")
            }
        }
    }
}
