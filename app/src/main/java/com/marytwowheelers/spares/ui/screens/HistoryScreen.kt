package com.marytwowheelers.spares.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marytwowheelers.spares.data.local.MovementType
import com.marytwowheelers.spares.data.local.SyncState
import com.marytwowheelers.spares.data.model.MovementRecord
import com.marytwowheelers.spares.sync.AppSyncStatus
import com.marytwowheelers.spares.ui.components.SyncStatusIndicator
import com.marytwowheelers.spares.ui.viewmodels.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToPartDetails: (String) -> Unit = {}
) {
    val movements by viewModel.allMovements.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.red < 0.5f

    // ─── Theme Colors ─────────────────────────────
    val pageBg = if (isDark) Color(0xFF111318) else Color(0xFFF7F8FC)
    val cardBg = if (isDark) Color(0xFF1B1E26) else Color.White
    val cardBorder = if (isDark) Color(0xFF2A2E3D) else Color(0xFFEEF0FA)
    val primaryText = if (isDark) Color(0xFFF3F4F6) else Color(0xFF1E1B4B)
    val secondaryText = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val pillBg = if (isDark) Color(0xFF232734) else Color(0xFFF1F3F9)

    // Group movements chronologically by date
    val groupedMovements = remember(movements) {
        movements.groupBy { formatRelativeDateGroup(it.timestamp) }
    }

    Scaffold(
        containerColor = pageBg,
        topBar = {
            Surface(
                color = pageBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stock History",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = primaryText
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (movements.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDark) Color(0xFF2E2A48) else Color(0xFFEFEBFA)
                            ) {
                                Text(
                                    text = "${movements.size} logs",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4338CA)
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        SyncStatusIndicator(
                            syncStatus = syncStatus,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (movements.isEmpty()) {
            // ─── Calm & Useful Empty State ──────────────
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, cardBorder),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color = if (isDark) Color(0xFF2E2A48) else Color(0xFFEFEBFA),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4338CA),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = "No Stock Movements Yet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = primaryText
                            )
                        )

                        Text(
                            text = "Your stock receipts, sales, adjustments, and returns will appear here automatically as you manage inventory.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.5.sp,
                                color = secondaryText,
                                lineHeight = 19.sp
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // ─── Chronological Grouped History List ─────
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedMovements.forEach { (dateHeader, entriesInDate) ->
                    // Date Group Header
                    item(key = "header_$dateHeader") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, top = 10.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dateHeader,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dateHeader.startsWith("Today")) {
                                        if (isDark) Color(0xFFC4B5FD) else Color(0xFF4338CA)
                                    } else {
                                        secondaryText
                                    }
                                )
                            )

                            Text(
                                text = "${entriesInDate.size} changes",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.5.sp,
                                    color = secondaryText
                                )
                            )
                        }
                    }

                    // Movement Cards in this Date Group
                    items(entriesInDate, key = { it.id }) { record ->
                        HistoryMovementCard(
                            record = record,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            pillBg = pillBg,
                            isDark = isDark,
                            onClick = { onNavigateToPartDetails(record.partId) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Clean, Compact History Movement Card
// ─────────────────────────────────────────────────────────
@Composable
fun HistoryMovementCard(
    record: MovementRecord,
    cardBg: Color,
    cardBorder: Color,
    primaryText: Color,
    secondaryText: Color,
    pillBg: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    // ─── Operation Visual Representation ─────────
    val (opTitle, opIcon, iconBg, iconTint, deltaColor, isPositive) = when (record.type) {
        MovementType.ADD -> {
            val bg = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
            val tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
            OperationVisuals("Add Stock", Icons.Outlined.AddCircleOutline, bg, tint, tint, true)
        }
        MovementType.REMOVE -> {
            val bg = if (isDark) Color(0xFF3B1D25) else Color(0xFFFEE2E2)
            val tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
            OperationVisuals("Remove / Sell", Icons.Outlined.RemoveCircleOutline, bg, tint, tint, false)
        }
        MovementType.RETURN -> {
            val bg = if (isDark) Color(0xFF2E2A48) else Color(0xFFEFEBFA)
            val tint = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4F46E5)
            OperationVisuals("Stock Return", Icons.Outlined.RotateLeft, bg, tint, tint, true)
        }
        MovementType.ADJUST -> {
            val bg = if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7)
            val tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
            val isPos = record.delta >= 0
            OperationVisuals("Stock Adjustment", Icons.Outlined.Tune, bg, tint, tint, isPos)
        }
    }

    // Previous and Resulting Stock calculation
    val previousStock = record.previousRecordedStock ?: 0
    val resultingStock = record.snapshotCount ?: (previousStock + record.delta)

    val deltaString = if (record.delta > 0) "+${record.delta}" else "${record.delta}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            // ─── 1. TOP ROW: OPERATION & QUANTITY CHANGE ─
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Operation icon + Operation Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(color = iconBg, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = opIcon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = opTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryText
                            )
                        )
                        if (record.syncState == SyncState.PENDING) {
                            Text(
                                text = "Pending sync",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                                )
                            )
                        }
                    }
                }

                // Quantity Delta & Transition (e.g. −2  12 → 10)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Delta (+ / −)
                    Text(
                        text = deltaString,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Black,
                            color = deltaColor
                        )
                    )

                    // Stock transition: 12 → 10
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = pillBg
                    ) {
                        Text(
                            text = "$previousStock → $resultingStock",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryText
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // ─── 2. MIDDLE ROW: PART NAME & PART NUMBER ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Serial Number Badge (#1, #2...)
                    if (record.serialNumber != null && record.serialNumber > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF2E2A48) else Color(0xFFEFEBFA)
                        ) {
                            Text(
                                text = "#${record.serialNumber}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4338CA)
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Part Name
                    Text(
                        text = record.partName ?: "Part #${record.partId.take(6)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryText
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Exact Timestamp
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = secondaryText,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = formatExactTime(record.timestamp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = secondaryText
                        )
                    )
                }
            }

            // ─── 3. BOTTOM ROW: PART NUMBER PILL & REASON 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PN: XXXXX
                if (!record.partNumber.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = pillBg
                    ) {
                        Text(
                            text = if (record.partNumber.startsWith("PN:", ignoreCase = true)) {
                                record.partNumber
                            } else {
                                "PN: ${record.partNumber}"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = secondaryText
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                // Reason (e.g. Reason: Physical count)
                if (!record.reason.isNullOrBlank()) {
                    Text(
                        text = "Reason: ${record.reason}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (record.type == MovementType.ADJUST) {
                                if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                            } else {
                                secondaryText
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Helper Data & Date Formatters
// ─────────────────────────────────────────────────────────
private data class OperationVisuals(
    val title: String,
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val deltaColor: Color,
    val isPositive: Boolean
)

private fun formatRelativeDateGroup(timestamp: Long): String {
    val nowCal = Calendar.getInstance()
    val entryCal = Calendar.getInstance().apply { timeInMillis = timestamp }

    val nowYear = nowCal.get(Calendar.YEAR)
    val nowDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)

    val entryYear = entryCal.get(Calendar.YEAR)
    val entryDayOfYear = entryCal.get(Calendar.DAY_OF_YEAR)

    val fullDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    val formattedDate = fullDateFormat.format(Date(timestamp))

    return when {
        nowYear == entryYear && nowDayOfYear == entryDayOfYear -> "Today · $formattedDate"
        nowYear == entryYear && (nowDayOfYear - entryDayOfYear == 1) -> "Yesterday · $formattedDate"
        else -> formattedDate
    }
}

private fun formatExactTime(timestamp: Long): String {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    return timeFormat.format(Date(timestamp)).uppercase(Locale.US)
}
