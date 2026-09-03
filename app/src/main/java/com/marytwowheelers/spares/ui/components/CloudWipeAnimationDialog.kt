package com.marytwowheelers.spares.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class WipePhase(val progress: Float, val label: String) {
    SAVING_BACKUP(0.25f, "Writing & Verifying Backup Archive"),
    PURGING_CLOUD(0.60f, "Purging Cloud Firestore Collections"),
    WIPING_LOCAL(0.85f, "Wiping Local SQLite Database Cache"),
    BOOTSTRAPPING(0.95f, "Re-securing Root Administrator Accounts"),
    COMPLETED(1.0f, "Cloud & Local Database Successfully Wiped!")
}

@Composable
fun CloudWipeAnimationDialog(
    statusText: String?,
    isComplete: Boolean,
    errorMessage: String?,
    backupDestination: String?,
    onDismiss: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Calculate current phase from status text
    val currentPhase = remember(statusText, isComplete) {
        when {
            isComplete -> WipePhase.COMPLETED
            statusText?.contains("backup", ignoreCase = true) == true && !statusText.contains("verified", ignoreCase = true) -> WipePhase.SAVING_BACKUP
            statusText?.contains("firestore", ignoreCase = true) == true || statusText?.contains("cloud", ignoreCase = true) == true -> WipePhase.PURGING_CLOUD
            statusText?.contains("local", ignoreCase = true) == true || statusText?.contains("sqlite", ignoreCase = true) == true -> WipePhase.WIPING_LOCAL
            statusText?.contains("bootstrap", ignoreCase = true) == true -> WipePhase.BOOTSTRAPPING
            else -> WipePhase.SAVING_BACKUP
        }
    }

    // Infinite rotations and pulsations
    val infiniteTransition = rememberInfiniteTransition(label = "WipeAnim")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    // Animated Progress
    val animatedProgress by animateFloatAsState(
        targetValue = currentPhase.progress,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "ProgressBar"
    )

    // Dynamic Colors based on phase
    val accentColor = when (currentPhase) {
        WipePhase.SAVING_BACKUP -> Color(0xFF6366F1)
        WipePhase.PURGING_CLOUD -> Color(0xFFEF4444)
        WipePhase.WIPING_LOCAL  -> Color(0xFFF59E0B)
        WipePhase.BOOTSTRAPPING -> Color(0xFF8B5CF6)
        WipePhase.COMPLETED     -> Color(0xFF10B981)
    }

    val dialogBg = if (isDark) Color(0xFF14161F) else Color.White
    val cardBorder = if (isDark) Color(0xFF2C3246) else Color(0xFFE2E8F0)
    val primaryText = if (isDark) Color(0xFFF3F4F6) else Color(0xFF1E1B4B)
    val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Dialog(
        onDismissRequest = { if (isComplete || errorMessage != null) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = isComplete || errorMessage != null, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            border = BorderStroke(1.2.dp, cardBorder),
            tonalElevation = 10.dp,
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ─── 1. CENTRAL ANIMATED GLOWING ORB ───
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isComplete && errorMessage == null) {
                        // Outer Rotating Gradient Aura
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .rotate(rotationAngle)
                                .background(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            accentColor.copy(alpha = 0.05f),
                                            accentColor.copy(alpha = 0.35f),
                                            accentColor.copy(alpha = 0.85f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }

                    // Inner Core Glow
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(if (!isComplete && errorMessage == null) pulseScale else 1f)
                            .background(
                                color = accentColor.copy(alpha = if (isDark) 0.22f else 0.14f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = currentPhase,
                            transitionSpec = {
                                (fadeIn(tween(400)) + scaleIn(tween(400))).togetherWith(fadeOut(tween(200)) + scaleOut(tween(200)))
                            },
                            label = "IconAnim"
                        ) { phase ->
                            Icon(
                                imageVector = when {
                                    errorMessage != null -> Icons.Outlined.ErrorOutline
                                    phase == WipePhase.SAVING_BACKUP -> Icons.Outlined.Archive
                                    phase == WipePhase.PURGING_CLOUD -> Icons.Outlined.CloudOff
                                    phase == WipePhase.WIPING_LOCAL  -> Icons.Outlined.CleaningServices
                                    phase == WipePhase.BOOTSTRAPPING -> Icons.Outlined.AdminPanelSettings
                                    else -> Icons.Filled.Check
                                },
                                contentDescription = null,
                                tint = if (errorMessage != null) Color(0xFFEF4444) else accentColor,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                // ─── 2. TITLE & STATUS SUBTITLE ───
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when {
                            errorMessage != null -> "Wipe Operation Aborted"
                            isComplete -> "Database Wiped Successfully"
                            else -> "Wiping Cloud Database"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = if (errorMessage != null) Color(0xFFEF4444) else primaryText
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = errorMessage ?: (statusText ?: currentPhase.label),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = if (errorMessage != null) Color(0xFFEF4444) else secondaryText,
                            lineHeight = 16.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                // ─── 3. SMOOTH LINEAR PROGRESS BAR ───
                if (errorMessage == null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = accentColor,
                            trackColor = if (isDark) Color(0xFF232735) else Color(0xFFF1F5F9)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Safety Backup Protocol",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = secondaryText)
                            )
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            )
                        }
                    }
                }

                // ─── 4. STEP-BY-STEP LIVE AUDIT CHECKLIST ───
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF1C1F2B) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF2E3446) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StepItem(
                            stepNumber = "1",
                            title = "Export & Verify Full Backup ZIP",
                            isDone = currentPhase.ordinal >= WipePhase.PURGING_CLOUD.ordinal,
                            isActive = currentPhase == WipePhase.SAVING_BACKUP,
                            isDark = isDark
                        )
                        StepItem(
                            stepNumber = "2",
                            title = "Purge Cloud Firestore Collections",
                            isDone = currentPhase.ordinal >= WipePhase.WIPING_LOCAL.ordinal,
                            isActive = currentPhase == WipePhase.PURGING_CLOUD,
                            isDark = isDark
                        )
                        StepItem(
                            stepNumber = "3",
                            title = "Wipe Local SQLite Tables (0 Items)",
                            isDone = currentPhase.ordinal >= WipePhase.BOOTSTRAPPING.ordinal,
                            isActive = currentPhase == WipePhase.WIPING_LOCAL,
                            isDark = isDark
                        )
                        StepItem(
                            stepNumber = "4",
                            title = "Re-secure Admin & Owner Root Accounts",
                            isDone = currentPhase == WipePhase.COMPLETED,
                            isActive = currentPhase == WipePhase.BOOTSTRAPPING,
                            isDark = isDark
                        )
                    }
                }

                // ─── 5. BACKUP DESTINATION BANNER (IF COMPLETED) ───
                if (isComplete && backupDestination != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF142B23) else Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E513F) else Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderZip,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Backup saved to: $backupDestination",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                                )
                            )
                        }
                    }
                }

                // ─── 6. ACTION BUTTONS ───
                if (isComplete || errorMessage != null) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (errorMessage != null) Color(0xFFEF4444) else Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (errorMessage != null) "Close" else "Done • Return to Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    stepNumber: String,
    title: String,
    isDone: Boolean,
    isActive: Boolean,
    isDark: Boolean
) {
    val primaryText = if (isDark) Color(0xFFF3F4F6) else Color(0xFF1E1B4B)
    val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = when {
                        isDone -> Color(0xFF10B981)
                        isActive -> Color(0xFF6366F1)
                        else -> if (isDark) Color(0xFF2C3246) else Color(0xFFE2E8F0)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            } else if (isActive) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = Color.White,
                    strokeWidth = 1.8.dp
                )
            } else {
                Text(
                    text = stepNumber,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = secondaryText
                    )
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                fontWeight = if (isActive || isDone) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isDone -> Color(0xFF10B981)
                    isActive -> primaryText
                    else -> secondaryText
                }
            ),
            modifier = Modifier.weight(1f)
        )
    }
}
