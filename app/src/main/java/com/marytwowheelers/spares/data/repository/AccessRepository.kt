package com.marytwowheelers.spares.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.marytwowheelers.spares.data.model.AccessMember
import com.marytwowheelers.spares.data.model.AccessStatus
import com.marytwowheelers.spares.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class AccessRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("access_members_prefs_v2", Context.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _members = MutableStateFlow<List<AccessMember>>(emptyList())
    val members: StateFlow<List<AccessMember>> = _members.asStateFlow()

    private val _currentUserRole = MutableStateFlow(UserRole.STAFF)
    val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    private var invitationsListener: ListenerRegistration? = null

    init {
        loadCachedMembers()
        evaluateCurrentUserRole()
        startFirestoreListener()
    }

    private fun evaluateCurrentUserRole() {
        val user = auth.currentUser
        val email = user?.email?.lowercase()?.trim() ?: ""
        if (email == "jinsu.j2005@gmail.com") {
            _currentUserRole.value = UserRole.ADMIN
            return
        }
        if (email == "jinsukapgreen@gmail.com") {
            _currentUserRole.value = UserRole.OWNER
            return
        }
        val cached = _members.value.find { it.email.lowercase() == email }
        if (cached != null) {
            _currentUserRole.value = cached.role
        }
    }

    fun startFirestoreListener() {
        invitationsListener?.remove()
        try {
            invitationsListener = firestore.collection("invitations")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("AccessRepository", "Error listening to invitations", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val email = doc.getString("email") ?: doc.id
                                val name = doc.getString("name") ?: email.substringBefore("@")
                                val roleStr = doc.getString("role") ?: "STAFF"
                                val statusStr = doc.getString("status") ?: "ACTIVE"
                                val role = UserRole.fromString(roleStr)
                                val status = try { AccessStatus.valueOf(statusStr) } catch (e: Exception) { AccessStatus.ACTIVE }
                                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                AccessMember(
                                    id = doc.id,
                                    email = email,
                                    name = name,
                                    role = role,
                                    isOwner = (role == UserRole.OWNER || role == UserRole.ADMIN),
                                    authProvider = doc.getString("authProvider") ?: "Google",
                                    status = status,
                                    createdAt = createdAt
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _members.value = list
                        saveCachedMembers(list)
                        evaluateCurrentUserRole()
                    }
                }
        } catch (e: Exception) {
            Log.e("AccessRepository", "Failed to attach snapshot listener", e)
        }
    }

    private fun loadCachedMembers() {
        val raw = prefs.getString("members_json", null)
        if (!raw.isNullOrBlank()) {
            try {
                val array = JSONArray(raw)
                val list = mutableListOf<AccessMember>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val role = UserRole.fromString(obj.optString("role", "STAFF"))
                    list.add(
                        AccessMember(
                            id = obj.getString("id"),
                            email = obj.getString("email"),
                            name = obj.getString("name"),
                            role = role,
                            isOwner = (role == UserRole.OWNER || role == UserRole.ADMIN),
                            authProvider = obj.optString("authProvider", "Google"),
                            status = AccessStatus.valueOf(obj.optString("status", "ACTIVE")),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                _members.value = list
            } catch (e: Exception) {
                Log.e("AccessRepository", "Error parsing cached members", e)
            }
        }
    }

    private fun saveCachedMembers(list: List<AccessMember>) {
        try {
            val array = JSONArray()
            for (m in list) {
                val obj = JSONObject()
                obj.put("id", m.id)
                obj.put("email", m.email)
                obj.put("name", m.name)
                obj.put("role", m.role.name)
                obj.put("isOwner", m.isOwner)
                obj.put("authProvider", m.authProvider)
                obj.put("status", m.status.name)
                obj.put("createdAt", m.createdAt)
                array.put(obj)
            }
            prefs.edit().putString("members_json", array.toString()).apply()
        } catch (e: Exception) {
            Log.e("AccessRepository", "Error saving cached members", e)
        }
    }

    suspend fun checkInvitation(email: String): AccessMember? {
        val cleanEmail = email.lowercase().trim()
        if (cleanEmail == "jinsu.j2005@gmail.com") {
            return AccessMember(id = cleanEmail, email = cleanEmail, name = "Admin", role = UserRole.ADMIN, isOwner = true)
        }
        if (cleanEmail == "jinsukapgreen@gmail.com") {
            return AccessMember(id = cleanEmail, email = cleanEmail, name = "Owner", role = UserRole.OWNER, isOwner = true)
        }
        return try {
            val doc = firestore.collection("invitations").document(cleanEmail).get().await()
            if (doc.exists()) {
                val role = UserRole.fromString(doc.getString("role"))
                val statusStr = doc.getString("status") ?: "ACTIVE"
                val status = try { AccessStatus.valueOf(statusStr) } catch (e: Exception) { AccessStatus.ACTIVE }
                AccessMember(
                    id = doc.id,
                    email = cleanEmail,
                    name = doc.getString("name") ?: cleanEmail.substringBefore("@"),
                    role = role,
                    isOwner = (role == UserRole.OWNER || role == UserRole.ADMIN),
                    status = status
                )
            } else {
                null
            }
        } catch (e: Exception) {
            // Check local fallback
            _members.value.find { it.email.lowercase() == cleanEmail }
        }
    }

    suspend fun addMemberInvitation(email: String, name: String, role: UserRole, invitedBy: String = "Admin"): Result<Unit> {
        val cleanEmail = email.lowercase().trim()
        return try {
            val data = hashMapOf(
                "email" to cleanEmail,
                "name" to name.trim(),
                "role" to role.name,
                "status" to AccessStatus.ACTIVE.name,
                "invitedBy" to invitedBy,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("invitations").document(cleanEmail).set(data).await()
            val newMember = AccessMember(
                id = cleanEmail,
                email = cleanEmail,
                name = name,
                role = role,
                isOwner = (role == UserRole.OWNER || role == UserRole.ADMIN),
                status = AccessStatus.ACTIVE
            )
            val updated = _members.value.filter { it.email.lowercase() != cleanEmail } + newMember
            _members.value = updated
            saveCachedMembers(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeMemberInvitation(emailOrId: String): Result<Unit> {
        val cleanEmail = emailOrId.lowercase().trim()
        // Prevent deleting root Admin or root Owner
        if (cleanEmail == "jinsu.j2005@gmail.com" || cleanEmail == "jinsukapgreen@gmail.com") {
            return Result.failure(IllegalStateException("Root Admin and Root Owner accounts cannot be deleted."))
        }
        return try {
            firestore.collection("invitations").document(cleanEmail).delete().await()
            val updated = _members.value.filter { it.email.lowercase() != cleanEmail && it.id != cleanEmail }
            _members.value = updated
            saveCachedMembers(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMemberRole(emailOrId: String, newRole: UserRole): Result<Unit> {
        val cleanEmail = emailOrId.lowercase().trim()
        if (cleanEmail == "jinsu.j2005@gmail.com") {
            return Result.failure(IllegalStateException("Root Admin role is fixed."))
        }
        return try {
            firestore.collection("invitations").document(cleanEmail).update("role", newRole.name).await()
            val updated = _members.value.map {
                if (it.email.lowercase() == cleanEmail || it.id == cleanEmail) {
                    it.copy(role = newRole, isOwner = (newRole == UserRole.OWNER || newRole == UserRole.ADMIN))
                } else it
            }
            _members.value = updated
            saveCachedMembers(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun provisionUserDocument(uid: String, email: String, displayName: String, authProvider: String): Result<UserRole> {
        val cleanEmail = email.lowercase().trim()
        val invitation = checkInvitation(cleanEmail)
        val assignedRole = invitation?.role ?: if (cleanEmail == "jinsu.j2005@gmail.com") UserRole.ADMIN else if (cleanEmail == "jinsukapgreen@gmail.com") UserRole.OWNER else UserRole.STAFF

        return try {
            val userMap = hashMapOf(
                "uid" to uid,
                "email" to cleanEmail,
                "displayName" to displayName,
                "role" to assignedRole.name,
                "status" to AccessStatus.ACTIVE.name,
                "authProvider" to authProvider,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(uid).set(userMap).await()
            _currentUserRole.value = assignedRole
            Result.success(assignedRole)
        } catch (e: Exception) {
            _currentUserRole.value = assignedRole
            Result.success(assignedRole)
        }
    }
}
