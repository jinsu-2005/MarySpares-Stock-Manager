package com.marytwowheelers.spares.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marytwowheelers.spares.sync.AppSyncStatus

@Composable
fun SyncStatusIndicator(
    syncStatus: AppSyncStatus,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    // Rotation animation for active sync
    val isSyncing = syncStatus is AppSyncStatus.Syncing
    val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_angle"
    )

    val (bgColor, borderColor, contentColor, icon, text) = when (syncStatus) {
        is AppSyncStatus.Syncing -> Quintuple(
            if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
            if (isDark) Color(0xFF3B82F6) else Color(0xFFBFDBFE),
            if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
            Icons.Outlined.Sync,
            "Syncing..."
        )
        is AppSyncStatus.PendingChanges -> Quintuple(
            if (isDark) Color(0xFF332211) else Color(0xFFFFFBEB),
            if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A),
            if (isDark) Color(0xFFFDE047) else Color(0xFFD97706),
            Icons.Outlined.CloudUpload,
            if (syncStatus.pendingCount > 0) "${syncStatus.pendingCount} Pending" else "Pending"
        )
        is AppSyncStatus.PendingChangesOffline -> Quintuple(
            if (isDark) Color(0xFF312E3B) else Color(0xFFFEF2F2),
            if (isDark) Color(0xFF4C4556) else Color(0xFFFECACA),
            if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
            Icons.Outlined.CloudOff,
            "Offline · ${syncStatus.pendingCount} pending"
        )
        is AppSyncStatus.Offline -> Quintuple(
            if (isDark) Color(0xFF232734) else Color(0xFFF3F4F6),
            if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB),
            if (isDark) Color(0xFFD1D5DB) else Color(0xFF6B7280),
            Icons.Outlined.CloudOff,
            "Offline"
        )
        is AppSyncStatus.Synced -> Quintuple(
            if (isDark) Color(0xFF132E27) else Color(0xFFECFDF5),
            if (isDark) Color(0xFF065F46) else Color(0xFFA7F3D0),
            if (isDark) Color(0xFF6EE7B7) else Color(0xFF059669),
            Icons.Outlined.CloudDone,
            "Synced"
        )
    }

    Surface(
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier
                    .size(15.dp)
                    .then(if (isSyncing) Modifier.rotate(angle) else Modifier)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            )
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
