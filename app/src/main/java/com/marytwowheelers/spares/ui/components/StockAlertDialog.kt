package com.marytwowheelers.spares.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marytwowheelers.spares.data.local.StockAlertManager
import com.marytwowheelers.spares.data.model.PartWithStock
import com.marytwowheelers.spares.data.model.StockState
import com.marytwowheelers.spares.data.model.stockState

enum class StockAlertTab { ALL, OUT, LOW }

@Composable
fun StockAlertDialog(
    partsList: List<PartWithStock>,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onNavigateToPart: (partId: String) -> Unit,
    onNavigateToInventoryWithFilter: (() -> Unit)? = null,
    onQuickRestock: ((PartWithStock) -> Unit)? = null
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        StockAlertManager.init(context)
    }

    val acknowledgedKeys by StockAlertManager.acknowledgedKeys.collectAsState()

    val allAlerts = remember(partsList) { partsList.filter { it.stockState != StockState.HEALTHY } }
    val outOfStockList = remember(allAlerts) { allAlerts.filter { it.stockState == StockState.OUT } }
    val lowStockList = remember(allAlerts) { allAlerts.filter { it.stockState == StockState.LOW } }

    var selectedTab by remember { mutableStateOf(StockAlertTab.ALL) }

    val displayedAlerts = when (selectedTab) {
        StockAlertTab.ALL -> allAlerts
        StockAlertTab.OUT -> outOfStockList
        StockAlertTab.LOW -> lowStockList
    }

    val unreviewedCount = remember(allAlerts, acknowledgedKeys) {
        allAlerts.count { !acknowledgedKeys.contains(StockAlertManager.createAlertKey(it.part.id, it.currentStock)) }
    }

    val cardBg = if (isDark) Color(0xFF1B1E26) else Color.White
    val cardBorder = if (isDark) Color(0xFF2A2E3D) else Color(0xFFEEF0FA)
    val primaryText = if (isDark) Color(0xFFF3F4F6) else Color(0xFF1E1B4B)
    val secondaryText = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val pillBg = if (isDark) Color(0xFF232734) else Color(0xFFF1F3F9)
    val accentColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF5046E5)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (allAlerts.isNotEmpty()) {
                                        if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7)
                                    } else {
                                        if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (allAlerts.isNotEmpty()) Icons.Outlined.NotificationsActive else Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = if (allAlerts.isNotEmpty()) {
                                    if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                                } else {
                                    if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Stock Alerts",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = primaryText
                                )
                            )
                            Text(
                                text = if (allAlerts.isNotEmpty()) {
                                    if (unreviewedCount > 0) "$unreviewedCount unreviewed · ${allAlerts.size} total"
                                    else "All ${allAlerts.size} alerts reviewed"
                                } else "All parts are well stocked",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    color = secondaryText
                                )
                            )
                        }
                    }

                    if (unreviewedCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF2E2A48) else Color(0xFFEEF2FF),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF6366F1).copy(alpha = 0.5f) else Color(0xFFC7D2FE)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val keys = allAlerts.map { StockAlertManager.createAlertKey(it.part.id, it.currentStock) }.toSet()
                                    StockAlertManager.markAllAsReviewed(context, keys)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DoneAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4F46E5)
                                )
                                Text(
                                    text = "Clear Badge",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4F46E5)
                                )
                            }
                        }
                    } else if (allAlerts.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF142B23) else Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF1E513F) else Color(0xFFA7F3D0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFF10B981)
                                )
                                Text(
                                    text = "Reviewed",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }
        },
        text = {
            if (allAlerts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = pillBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "No Active Stock Alerts",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = primaryText
                            )
                        )
                        Text(
                            text = "All items in your catalog currently have healthy stock levels.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = secondaryText
                            )
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Filter Chips Row (All / Out / Low)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // All Chip
                        val isAllSelected = selectedTab == StockAlertTab.ALL
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isAllSelected) (if (isDark) Color(0xFF2E2A48) else Color(0xFFEEF2FF)) else pillBg,
                            border = BorderStroke(1.dp, if (isAllSelected) (if (isDark) Color(0xFF6366F1) else Color(0xFFC7D2FE)) else Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = StockAlertTab.ALL }
                        ) {
                            Text(
                                text = "All (${allAlerts.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAllSelected) (if (isDark) Color(0xFFC4B5FD) else Color(0xFF4338CA)) else secondaryText
                                ),
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        // Out of Stock Chip
                        val isOutSelected = selectedTab == StockAlertTab.OUT
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isOutSelected) (if (isDark) Color(0xFF38141B) else Color(0xFFFEE2E2)) else pillBg,
                            border = BorderStroke(1.dp, if (isOutSelected) (if (isDark) Color(0xFF5C1D2A) else Color(0xFFFECDD3)) else Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = StockAlertTab.OUT }
                        ) {
                            Text(
                                text = "Empty (${outOfStockList.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isOutSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isOutSelected) (if (isDark) Color(0xFFFB7185) else Color(0xFFDC2626)) else secondaryText
                                ),
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        // Low Stock Chip
                        val isLowSelected = selectedTab == StockAlertTab.LOW
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLowSelected) (if (isDark) Color(0xFF382A11) else Color(0xFFFEF3C7)) else pillBg,
                            border = BorderStroke(1.dp, if (isLowSelected) (if (isDark) Color(0xFF5C4419) else Color(0xFFFDE68A)) else Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = StockAlertTab.LOW }
                        ) {
                            Text(
                                text = "Low (${lowStockList.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isLowSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isLowSelected) (if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)) else secondaryText
                                ),
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // Alert Items List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(displayedAlerts) { item ->
                            val isOut = item.stockState == StockState.OUT
                            val alertKey = StockAlertManager.createAlertKey(item.part.id, item.currentStock)
                            val isItemReviewed = acknowledgedKeys.contains(alertKey)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = pillBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (isOut) {
                                        if (isDark) Color(0xFF5C1D2A) else Color(0xFFFECDD3)
                                    } else {
                                        if (isDark) Color(0xFF5C4419) else Color(0xFFFDE68A)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        StockAlertManager.markPartAsReviewed(context, alertKey)
                                        onDismiss()
                                        onNavigateToPart(item.part.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "#${item.part.serialNumber} ${item.part.name}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = primaryText
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!isItemReviewed) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(if (isDark) Color(0xFFFB7185) else Color(0xFFEF4444), CircleShape)
                                                )
                                            }
                                        }

                                        val subtitle = buildString {
                                            if (item.part.partNumber.isNotBlank()) {
                                                append(item.part.partNumber)
                                            }
                                            if (item.part.shelfLocation.isNotBlank()) {
                                                if (isNotEmpty()) append(" · ")
                                                append("Shelf ${item.part.shelfLocation}")
                                            }
                                        }
                                        if (subtitle.isNotBlank()) {
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = secondaryText
                                                )
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isOut) {
                                                if (isDark) Color(0xFF38141B) else Color(0xFFFEE2E2)
                                            } else {
                                                if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7)
                                            }
                                        ) {
                                            Text(
                                                text = if (isOut) "0 OUT" else "${item.currentStock} Left",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isOut) {
                                                        if (isDark) Color(0xFFFB7185) else Color(0xFFDC2626)
                                                    } else {
                                                        if (isDark) Color(0xFFFDE047) else Color(0xFFD97706)
                                                    }
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }

                                        if (onQuickRestock != null) {
                                            IconButton(
                                                onClick = {
                                                    StockAlertManager.markPartAsReviewed(context, alertKey)
                                                    onQuickRestock(item)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Quick Restock",
                                                    tint = accentColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (allAlerts.isNotEmpty() && onNavigateToInventoryWithFilter != null) {
                Button(
                    onClick = {
                        val keys = allAlerts.map { StockAlertManager.createAlertKey(it.part.id, it.currentStock) }.toSet()
                        StockAlertManager.markAllAsReviewed(context, keys)
                        onDismiss()
                        onNavigateToInventoryWithFilter()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF6366F1) else Color(0xFF5046E5),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("View in Inventory", fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = secondaryText)
                }
            }
        },
        dismissButton = {
            if (allAlerts.isNotEmpty() && onNavigateToInventoryWithFilter != null) {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = secondaryText)
                }
            }
        },
        containerColor = cardBg,
        shape = RoundedCornerShape(22.dp)
    )
}

