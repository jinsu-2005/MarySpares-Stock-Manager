package com.marytwowheelers.spares.data.model

enum class UserRole(val displayName: String) {
    ADMIN("Admin"),
    OWNER("Owner"),
    STAFF("Staff"),
    RELATIVE("Relative"),
    FRIEND("Friend");

    val canManageUsers: Boolean get() = this == ADMIN || this == OWNER
    val canClearHistory: Boolean get() = this == ADMIN || this == OWNER
    val canResetLocalDb: Boolean get() = this == ADMIN || this == OWNER
    val canDeleteCloudDb: Boolean get() = this == ADMIN

    companion object {
        fun fromString(value: String?): UserRole {
            return when (value?.uppercase()?.trim()) {
                "ADMIN" -> ADMIN
                "OWNER" -> OWNER
                "STAFF" -> STAFF
                "RELATIVE" -> RELATIVE
                "FRIEND" -> FRIEND
                else -> STAFF
            }
        }
    }
}

enum class AccessStatus {
    ACTIVE,
    PENDING,
    REVOKED
}

data class AccessMember(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: UserRole = UserRole.STAFF,
    val isOwner: Boolean = (role == UserRole.OWNER || role == UserRole.ADMIN),
    val authProvider: String = "Google", // "Google" or "Email"
    val status: AccessStatus = AccessStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis()
)
