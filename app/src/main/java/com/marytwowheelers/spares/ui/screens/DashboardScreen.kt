package com.marytwowheelers.spares.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.rotate
import com.marytwowheelers.spares.data.local.MovementType
import com.marytwowheelers.spares.data.local.StockAlertManager
import com.marytwowheelers.spares.data.local.SyncState
import com.marytwowheelers.spares.data.model.PartWithStock
import com.marytwowheelers.spares.data.model.StockState
import com.marytwowheelers.spares.data.model.stockState
import com.marytwowheelers.spares.sync.SyncManager
import com.marytwowheelers.spares.sync.AppSyncStatus
import com.marytwowheelers.spares.ui.components.AddPartDialog
import com.marytwowheelers.spares.ui.components.AppSnackbarHost
import com.marytwowheelers.spares.ui.components.StockActionDialog
import com.marytwowheelers.spares.ui.components.StockAlertDialog
import com.marytwowheelers.spares.ui.components.SyncStatusIndicator
import com.marytwowheelers.spares.ui.viewmodels.DashboardViewModel
import kotlinx.coroutines.launch
import java.util.Locale

private data class SyncBannerVisuals(
    val bg: Color,
    val border: Color,
    val iconBg: Color,
    val iconTint: Color,
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val badgeBg: Color,
    val badgeColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToInventory: () -> Unit,
    onNavigateToInventorySearch: () -> Unit,
    onNavigateToPartDetails: (String) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        StockAlertManager.init(context)
        viewModel.triggerSync()
    }

    val acknowledgedKeys by StockAlertManager.acknowledgedKeys.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val partsList by viewModel.partsList.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()

    var showAddPartDialog by remember { mutableStateOf(false) }
    var showStockAlertDialog by remember { mutableStateOf(false) }
    var stockActionTarget by remember { mutableStateOf<MovementType?>(null) }
    var selectedPartForStock by remember { mutableStateOf<PartWithStock?>(null) }

    val totalParts = partsList.size
    val allAlerts = remember(partsList) { partsList.filter { it.stockState != StockState.HEALTHY } }
    val lowStockCount = partsList.count { it.stockState == StockState.LOW }
    val outOfStockCount = partsList.count { it.stockState == StockState.OUT }
    val pendingSyncCount = partsList.count { it.part.syncState == SyncState.PENDING }

    val unreviewedAlerts = remember(allAlerts, acknowledgedKeys) {
        allAlerts.filter { !acknowledgedKeys.contains(StockAlertManager.createAlertKey(it.part.id, it.currentStock)) }
    }
    val unreviewedAlertCount = unreviewedAlerts.size

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.red < 0.5f

    // Rich Dark / Light Palette
    val pageBg = if (isDark) Color(0xFF181A22) else Color(0xFFF7F8FC)
    val cardBg = if (isDark) Color(0xFF222530) else Color.White
    val cardBorder = if (isDark) Color(0xFF323748) else Color(0xFFEEF0FA)
    val primaryText = if (isDark) Color(0xFFF3F4F6) else Color(0xFF1E1B4B)
    val secondaryText = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val pillBg = if (isDark) Color(0xFF2C3140) else Color(0xFFF1F3F9)

    Scaffold(
        containerColor = pageBg,
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
        topBar = {
            // Status bar safe area with generous spacing to avoid camera cutout/notch
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
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SyncStatusIndicator(
                            syncStatus = syncStatus,
                            isDark = isDark,
                            onClick = { viewModel.triggerSync() }
                        )

                        IconButton(
                            onClick = { showStockAlertDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreviewedAlertCount > 0) {
                                        Badge(
                                            containerColor = if (isDark) Color(0xFFE11D48) else Color(0xFFDC2626),
                                            contentColor = Color.White
                                        ) {
                                            Text("$unreviewedAlertCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Stock Alerts",
                                    tint = primaryText,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ─────────────────────────────────────────────
            // 1. SEARCH INVENTORY BAR
            // ─────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigateToInventorySearch() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, cardBorder),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4F46E5),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = "Search Inventory",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = secondaryText,
                                fontSize = 16.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Go",
                            tint = if (isDark) Color(0xFFD1D5DB) else primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 2. SYNC / STATUS BANNER
            // ─────────────────────────────────────────────
            item {
                val (bannerBg, bannerBorder, bannerIconBg, bannerIconTint, bannerTitle, bannerSubtitle, badgeText, badgeBg, badgeColor) = when (val s = syncStatus) {
                    is AppSyncStatus.Offline -> SyncBannerVisuals(
                        if (isDark) Color(0xFF171A21) else Color(0xFFF9FAFB),
                        if (isDark) Color(0xFF323748) else Color(0xFFE5E7EB),
                        if (isDark) Color(0xFF232734) else Color(0xFFF3F4F6),
                        if (isDark) Color(0xFFD1D5DB) else Color(0xFF6B7280),
                        "Offline Mode",
                        "No Wi-Fi or mobile data available. Local catalog is accessible.",
                        "Offline",
                        if (isDark) Color(0xFF232734) else Color(0xFFF3F4F6),
                        if (isDark) Color(0xFFD1D5DB) else Color(0xFF6B7280)
                    )
                    is AppSyncStatus.PendingChangesOffline -> SyncBannerVisuals(
                        if (isDark) Color(0xFF261D22) else Color(0xFFFEF2F2),
                        if (isDark) Color(0xFF4C2A33) else Color(0xFFFECACA),
                        if (isDark) Color(0xFF3B1D25) else Color(0xFFFFE4E6),
                        if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                        "Pending Changes (Offline)",
                        "${s.pendingCount} update(s) saved locally. Will sync when connection is restored.",
                        "Offline · ${s.pendingCount} pending",
                        if (isDark) Color(0xFF3B1D25) else Color(0xFFFFE4E6),
                        if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                    )
                    is AppSyncStatus.Syncing -> SyncBannerVisuals(
                        if (isDark) Color(0xFF162032) else Color(0xFFEFF6FF),
                        if (isDark) Color(0xFF25426B) else Color(0xFFBFDBFE),
                        if (isDark) Color(0xFF1E293B) else Color(0xFFDBEAFE),
                        if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                        "Synchronizing...",
                        "Syncing parts and stock movements with Firebase Cloud...",
                        "Syncing",
                        if (isDark) Color(0xFF1E293B) else Color(0xFFDBEAFE),
                        if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)
                    )
                    is AppSyncStatus.PendingChanges -> SyncBannerVisuals(
                        if (isDark) Color(0xFF261F13) else Color(0xFFFFFBEB),
                        if (isDark) Color(0xFF4E3E18) else Color(0xFFFDE68A),
                        if (isDark) Color(0xFF332211) else Color(0xFFFEF3C7),
                        if (isDark) Color(0xFFFDE047) else Color(0xFFD97706),
                        "Sync Pending",
                        "${s.pendingCount} local updates ready to upload to cloud.",
                        "${s.pendingCount} Pending",
                        if (isDark) Color(0xFF332211) else Color(0xFFFEF3C7),
                        if (isDark) Color(0xFFFDE047) else Color(0xFFD97706)
                    )
                    is AppSyncStatus.Synced -> SyncBannerVisuals(
                        if (isDark) Color(0xFF12241F) else Color(0xFFF0FDF4),
                        if (isDark) Color(0xFF1D473B) else Color(0xFFBBF7D0),
                        if (isDark) Color(0xFF132E27) else Color(0xFFDCFCE7),
                        if (isDark) Color(0xFF6EE7B7) else Color(0xFF059669),
                        "Cloud Synced",
                        "All inventory and movement logs are fully up to date.",
                        "Online",
                        if (isDark) Color(0xFF132E27) else Color(0xFFDCFCE7),
                        if (isDark) Color(0xFF6EE7B7) else Color(0xFF059669)
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.triggerSync() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = bannerBg),
                    border = BorderStroke(1.dp, bannerBorder),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(color = bannerIconBg, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val isSyncing = syncStatus is AppSyncStatus.Syncing
                                val infiniteTransition = rememberInfiniteTransition(label = "banner_sync")
                                val angle by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1100, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "banner_angle"
                                )

                                Icon(
                                    imageVector = when (syncStatus) {
                                        is AppSyncStatus.Offline -> Icons.Outlined.CloudOff
                                        is AppSyncStatus.PendingChangesOffline -> Icons.Outlined.CloudOff
                                        is AppSyncStatus.Syncing -> Icons.Outlined.Sync
                                        is AppSyncStatus.PendingChanges -> Icons.Outlined.CloudUpload
                                        is AppSyncStatus.Synced -> Icons.Outlined.CloudDone
                                    },
                                    contentDescription = null,
                                    tint = bannerIconTint,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .then(if (isSyncing) Modifier.rotate(angle) else Modifier)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = bannerTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = bannerIconTint
                                    )
                                )
                                Spacer(Modifier.height(1.dp))
                                Text(
                                    text = bannerSubtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.5.sp,
                                        color = secondaryText
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = badgeBg
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = badgeColor
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 3. 4-METRIC CARDS ROW
            // ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Total Parts
                    TopMetricCard(
                        icon = Icons.Outlined.Assignment,
                        iconTint = if (isDark) Color(0xFF818CF8) else Color(0xFF1E1B4B),
                        value = String.format(Locale.US, "%,d", totalParts),
                        label = "Total Parts",
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = onNavigateToInventory,
                        modifier = Modifier.weight(1f)
                    )

                    // Low Stock
                    TopMetricCard(
                        icon = Icons.Outlined.WarningAmber,
                        iconTint = if (isDark) Color(0xFFFFB726) else Color(0xFFD97706),
                        value = "$lowStockCount",
                        label = "Low Stock",
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = { showStockAlertDialog = true },
                        modifier = Modifier.weight(1f)
                    )

                    // Out of Stock
                    TopMetricCard(
                        icon = Icons.Outlined.Archive,
                        iconTint = if (isDark) Color(0xFFFF4D6D) else Color(0xFFDC2626),
                        value = "$outOfStockCount",
                        label = "Out of Stock",
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = { showStockAlertDialog = true },
                        modifier = Modifier.weight(1f)
                    )

                    // Pending Sync
                    TopMetricCard(
                        icon = Icons.Outlined.CloudQueue,
                        iconTint = if (isDark) Color(0xFFC084FC) else Color(0xFF4F46E5),
                        value = "$pendingSyncCount",
                        label = "Pending Sync",
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = { viewModel.triggerSync() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ─────────────────────────────────────────────
            // 4. 2×2 QUICK ACTION CARDS GRID
            // ─────────────────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Row 1: Sync Now & Add Part
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // SYNC NOW BUTTON
                        ActionCard(
                            icon = Icons.Default.Sync,
                            iconBg = if (isDark) Color(0xFF2E2A48) else Color(0xFFEFEBFA),
                            iconTint = if (isDark) Color(0xFFA78BFA) else Color(0xFF4338CA),
                            title = "Sync Now",
                            subtitle = "",
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onClick = {
                                SyncManager.enqueueSync(context)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Sync triggered. Uploading and pulling changes...")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // ADD PART BUTTON (or Search Parts for Staff / Viewers)
                        if (currentUserRole.canAddParts) {
                            ActionCard(
                                icon = Icons.Default.Add,
                                iconBg = if (isDark) Color(0xFF0F3B2E) else Color(0xFFE6F9F0),
                                iconTint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                title = "Add Part",
                                subtitle = "",
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onClick = { showAddPartDialog = true },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            ActionCard(
                                icon = Icons.Default.Search,
                                iconBg = if (isDark) Color(0xFF0F3B2E) else Color(0xFFE6F9F0),
                                iconTint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                title = "Search Parts",
                                subtitle = "",
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onClick = onNavigateToInventorySearch,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Row 2: Add Stock & Remove Stock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ADD STOCK
                        ActionCard(
                            icon = if (currentUserRole.isReadOnly) Icons.Outlined.Lock else Icons.Default.Add,
                            iconBg = if (isDark) Color(0xFF1E284A) else Color(0xFFEFF4FF),
                            iconTint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                            title = "Add Stock",
                            subtitle = if (currentUserRole.isReadOnly) "Locked" else "",
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onClick = {
                                if (currentUserRole.isReadOnly) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Read-Only Access: Viewers cannot modify stock.")
                                    }
                                } else {
                                    stockActionTarget = MovementType.ADD
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // REMOVE STOCK
                        ActionCard(
                            icon = if (currentUserRole.isReadOnly) Icons.Outlined.Lock else Icons.Default.Remove,
                            iconBg = if (isDark) Color(0xFF381520) else Color(0xFFFFEEF0),
                            iconTint = if (isDark) Color(0xFFFF4D6D) else Color(0xFFF43F5E),
                            title = "Remove Stock",
                            subtitle = if (currentUserRole.isReadOnly) "Locked" else "",
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onClick = {
                                if (currentUserRole.isReadOnly) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Read-Only Access: Viewers cannot modify stock.")
                                    }
                                } else {
                                    stockActionTarget = MovementType.REMOVE
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 5. STOCK ALERT BANNER (Only shown when action is needed)
            // ─────────────────────────────────────────────
            val hasActiveAlerts = unreviewedAlertCount > 0 || outOfStockCount > 0
            if (hasActiveAlerts) {
                item {
                    val isCritical = outOfStockCount > 0

                    val alertCardBg = if (isCritical) {
                        if (isDark) Color(0xFF26151B) else Color(0xFFFFF1F2)
                    } else {
                        if (isDark) Color(0xFF261F14) else Color(0xFFFFFBEB)
                    }
                    val alertCardBorder = if (isCritical) {
                        if (isDark) Color(0xFF882336) else Color(0xFFFECDD3)
                    } else {
                        if (isDark) Color(0xFF8F6314) else Color(0xFFFDE68A)
                    }
                    val alertAccent = if (isCritical) {
                        if (isDark) Color(0xFFFF4D6D) else Color(0xFFDC2626)
                    } else {
                        if (isDark) Color(0xFFFFB726) else Color(0xFFD97706)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showStockAlertDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = alertCardBg),
                        border = BorderStroke(1.2.dp, alertCardBorder),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        color = if (isCritical) (if (isDark) Color(0xFF3E1824) else Color(0xFFFFE4E6))
                                                else (if (isDark) Color(0xFF382914) else Color(0xFFFEF3C7)),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCritical) Icons.Outlined.ErrorOutline else Icons.Outlined.WarningAmber,
                                    contentDescription = null,
                                    tint = alertAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (isCritical) "Out of Stock Alert" else "Low Stock Alert",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            color = primaryText
                                        )
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isCritical) (if (isDark) Color(0xFF3E1824) else Color(0xFFFEE2E2))
                                                else (if (isDark) Color(0xFF382914) else Color(0xFFFEF3C7))
                                    ) {
                                        Text(
                                            text = if (isCritical) "$outOfStockCount Empty" else "$unreviewedAlertCount New",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = alertAccent
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(3.dp))

                                Text(
                                    text = when {
                                        outOfStockCount > 0 && lowStockCount > 0 -> "$outOfStockCount out of stock · $lowStockCount low quantity"
                                        outOfStockCount > 0 -> "$outOfStockCount items empty · Tap to restock"
                                        else -> "$unreviewedAlertCount parts have low stock (≤5 units)"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.5.sp,
                                        color = secondaryText
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCritical) (if (isDark) Color(0xFF3E1824) else Color(0xFFFFE4E6))
                                        else (if (isDark) Color(0xFF382914) else Color(0xFFFEF3C7))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Review",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = alertAccent
                                        )
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = alertAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // DIALOGS & ACTION HANDLERS
    // ─────────────────────────────────────────────

    // 1. Add Part Dialog
    if (showAddPartDialog && currentUserRole.canAddParts) {
        AddPartDialog(
            onDismiss = { showAddPartDialog = false },
            onConfirm = { name, partNumber, shelf, sp, mrp, stock ->
                viewModel.addPart(name, partNumber, shelf, sp, mrp, stock)
                showAddPartDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("New part '$name' saved to inventory.")
                }
            }
        )
    }

    // 2. Select Part Modal for Add Stock or Remove Stock
    if (stockActionTarget != null && selectedPartForStock == null) {
        val actionType = stockActionTarget!!
        val popupBg = if (isDark) Color(0xFF2C3142) else Color.White
        val popupBorder = if (isDark) Color(0xFF424A63) else Color(0xFFEEF0FA)
        val popupPill = if (isDark) Color(0xFF373E54) else Color(0xFFF1F3F9)
        SelectPartDialog(
            actionType = actionType,
            partsList = partsList,
            isDark = isDark,
            primaryText = if (isDark) Color(0xFFF9FAFB) else primaryText,
            secondaryText = if (isDark) Color(0xFFA5B4CB) else secondaryText,
            cardBg = popupBg,
            cardBorder = popupBorder,
            pillBg = popupPill,
            onDismiss = { stockActionTarget = null },
            onPartSelected = { part ->
                selectedPartForStock = part
            }
        )
    }

    // 3. Stock Action Dialog for selected part
    if (selectedPartForStock != null && stockActionTarget != null) {
        val part = selectedPartForStock!!
        val actionType = stockActionTarget!!
        StockActionDialog(
            actionType = actionType,
            partName = "#${part.part.serialNumber} ${part.part.name}",
            currentStock = part.currentStock,
            onDismiss = {
                selectedPartForStock = null
                stockActionTarget = null
            },
            onConfirm = { delta, reason ->
                val signedDelta = if (actionType == MovementType.REMOVE) -delta else delta
                viewModel.recordMovement(part.part.id, signedDelta, actionType, reason)
                val partTitle = "#${part.part.serialNumber} ${part.part.name}"
                selectedPartForStock = null
                stockActionTarget = null
                scope.launch {
                    val actionName = when (actionType) {
                        MovementType.ADD -> "Added $delta units to"
                        MovementType.REMOVE -> "Removed $delta units from"
                        MovementType.RETURN -> "Returned $delta units to"
                        MovementType.ADJUST -> "Adjusted stock to $delta units for"
                    }
                    snackbarHostState.showSnackbar("$actionName $partTitle")
                }
            }
        )
    }

    // 4. Stock Alert Dialog (from Top Bar Bell Icon or Alert Panel)
    if (showStockAlertDialog) {
        StockAlertDialog(
            partsList = partsList,
            isDark = isDark,
            onDismiss = { showStockAlertDialog = false },
            onNavigateToPart = { partId ->
                showStockAlertDialog = false
                onNavigateToPartDetails(partId)
            },
            onNavigateToInventoryWithFilter = {
                showStockAlertDialog = false
                onNavigateToInventory()
            },
            onQuickRestock = { part ->
                selectedPartForStock = part
                stockActionTarget = MovementType.ADD
            }
        )
    }
}

// ─────────────────────────────────────────────────────────
// Metric Card (Top Row of 4)
// ─────────────────────────────────────────────────────────
@Composable
private fun TopMetricCard(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    cardBg: Color,
    cardBorder: Color,
    primaryText: Color,
    secondaryText: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .height(116.dp)
            .then(
                if (onClick != null) Modifier.clip(RoundedCornerShape(16.dp)).clickable { onClick() }
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = primaryText
                )
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    color = secondaryText
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Action Card (2x2 Grid)
// ─────────────────────────────────────────────────────────
@Composable
private fun ActionCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    cardBg: Color,
    cardBorder: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(98.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color = iconBg, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = primaryText
                    )
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        color = secondaryText,
                        lineHeight = 14.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (primaryText == Color(0xFFF3F4F6)) Color(0xFF6B7280) else Color(0xFF9CA3AF),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Select Part Dialog for Quick Stock Action
// ─────────────────────────────────────────────────────────
@Composable
private fun SelectPartDialog(
    actionType: MovementType,
    partsList: List<PartWithStock>,
    isDark: Boolean,
    primaryText: Color,
    secondaryText: Color,
    cardBg: Color,
    cardBorder: Color,
    pillBg: Color,
    onDismiss: () -> Unit,
    onPartSelected: (PartWithStock) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(partsList, searchQuery) {
        if (searchQuery.isBlank()) partsList
        else partsList.filter {
            it.part.name.contains(searchQuery, ignoreCase = true) ||
            it.part.partNumber.contains(searchQuery, ignoreCase = true) ||
            it.part.serialNumber.toString() == searchQuery.trim().removePrefix("#")
        }
    }

    val title = if (actionType == MovementType.ADD) "Select Part to Add Stock" else "Select Part to Remove / Sell"

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(22.dp)),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (actionType == MovementType.ADD) {
                                if (isDark) Color(0xFF0D382B) else Color(0xFFDCFCE7)
                            } else {
                                if (isDark) Color(0xFF381520) else Color(0xFFFEE2E2)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (actionType == MovementType.ADD) Icons.Outlined.AddCircleOutline else Icons.Outlined.RemoveCircleOutline,
                        contentDescription = null,
                        tint = if (actionType == MovementType.ADD) {
                            if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                        } else {
                            if (isDark) Color(0xFFFF4D6D) else Color(0xFFDC2626)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by serial no, name, code...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = secondaryText) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = secondaryText, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching parts found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryText
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.part.id }) { part ->
                            val isOut = part.currentStock <= 0
                            val isLow = part.currentStock in 1..5

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPartSelected(part) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = pillBg),
                                border = BorderStroke(1.dp, cardBorder),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isDark) Color(0xFF2E2A48) else Color(0xFFEFEBFA)
                                            ) {
                                                Text(
                                                    text = "#${part.part.serialNumber}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4338CA)
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Text(
                                                text = part.part.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    color = primaryText
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(Modifier.height(3.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "PN: ${part.part.partNumber}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 11.sp,
                                                    color = secondaryText
                                                )
                                            )
                                            if (part.part.shelfLocation.isNotBlank()) {
                                                Text(
                                                    text = "📍 ${part.part.shelfLocation}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.sp,
                                                        color = secondaryText
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Stock Count Tag
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when {
                                            isOut -> if (isDark) Color(0xFF3E1824) else Color(0xFFFEE2E2)
                                            isLow -> if (isDark) Color(0xFF382914) else Color(0xFFFEF3C7)
                                            else  -> if (isDark) Color(0xFF0D382B) else Color(0xFFDCFCE7)
                                        }
                                    ) {
                                        Text(
                                            text = "${part.currentStock} units",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    isOut -> if (isDark) Color(0xFFFF4D6D) else Color(0xFFDC2626)
                                                    isLow -> if (isDark) Color(0xFFFFB726) else Color(0xFFD97706)
                                                    else  -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                                }
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = secondaryText)
            }
        },
        containerColor = cardBg,
        shape = RoundedCornerShape(22.dp)
    )
}
