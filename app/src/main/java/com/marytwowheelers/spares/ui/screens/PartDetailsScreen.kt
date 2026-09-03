package com.marytwowheelers.spares.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marytwowheelers.spares.data.local.MovementEntity
import com.marytwowheelers.spares.data.local.MovementType
import com.marytwowheelers.spares.data.local.SyncState
import com.marytwowheelers.spares.sync.AppSyncStatus
import com.marytwowheelers.spares.ui.components.AddPartDialog
import com.marytwowheelers.spares.ui.components.AppSnackbarHost
import com.marytwowheelers.spares.ui.components.StockActionDialog
import com.marytwowheelers.spares.ui.components.SyncStatusIndicator
import com.marytwowheelers.spares.ui.viewmodels.PartDetailsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartDetailsScreen(
    partId: String,
    viewModel: PartDetailsViewModel,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(partId) {
        viewModel.loadPart(partId)
    }

    val partWithStock by viewModel.partDetails.collectAsState()
    val movements     by viewModel.movements.collectAsState()
    val syncStatus    by viewModel.syncStatus.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeActionType  by remember { mutableStateOf<MovementType?>(null) }
    var showEditDialog    by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.red < 0.5f

    // ─── Theme Colors ─────────────────────────────
    val pageBg = if (isDark) Color(0xFF111318) else Color(0xFFF7F8FC)
    val cardBg = if (isDark) Color(0xFF1B1E26) else Color.White
    val cardBorder = if (isDark) Color(0xFF2A2E3D) else Color(0xFFEEF0FA)
    val primaryText = if (isDark) Color(0xFFF3F4F6) else Color(0xFF1E1B4B)
    val secondaryText = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val pillBg = if (isDark) Color(0xFF232734) else Color(0xFFF1F3F9)

    Scaffold(
        containerColor = pageBg,
        snackbarHost = { AppSnackbarHost(snackbarHostState, bottomPadding = 16.dp) },
        topBar = {
            Surface(
                color = pageBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 8.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = primaryText
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = partWithStock?.part?.name ?: "Part Details",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = primaryText
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Sync Status Indicator
                        SyncStatusIndicator(
                            syncStatus = syncStatus,
                            isDark = isDark,
                            onClick = { viewModel.triggerSync() }
                        )

                        // Edit Part Button (Only Owner / Admin)
                        if (currentUserRole.canEditParts) {
                            IconButton(onClick = { showEditDialog = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit Part",
                                    tint = if (isDark) Color(0xFFC4B5FD) else Color(0xFF5046E5)
                                )
                            }
                        }

                        // Delete / Archive Button (Only Owner / Admin)
                        if (currentUserRole.canDeleteParts) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete Part",
                                    tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (partWithStock == null) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = if (isDark) Color(0xFFC4B5FD) else cs.primary)
            }
        } else {
            val p = partWithStock!!
            val stock = p.currentStock
            val isOut = stock <= 0
            val isLow = stock in 1..5

            // Status Colors & Badges
            val (statusText, statusBadgeBg, statusBadgeTint, statusIcon) = when {
                isOut -> Quadruple(
                    "OUT OF STOCK",
                    if (isDark) Color(0xFF3B1D25) else Color(0xFFFEE2E2),
                    if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                    Icons.Default.ErrorOutline
                )
                isLow -> Quadruple(
                    "LOW STOCK",
                    if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7),
                    if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                    Icons.Outlined.WarningAmber
                )
                else -> Quadruple(
                    "IN STOCK",
                    if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7),
                    if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                    Icons.Outlined.CheckCircle
                )
            }

            // Pulsing animation for Low and Empty stock
            val infiniteTransition = rememberInfiniteTransition(label = "stockPulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = if (isLow || isOut) 1.35f else 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = if (isLow || isOut) 0.55f else 0.0f,
                targetValue = if (isLow || isOut) 0.12f else 0.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )

            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ─────────────────────────────────────────────
                // 1. HERO OPERATIONAL SUMMARY CARD
                // ─────────────────────────────────────────────
                item {
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
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Top Row: Serial Badge + Part Name & Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isDark) Color(0xFF2E2A48) else Color(0xFFEFEBFA)
                                        ) {
                                            Text(
                                                text = "SL. NO: #${p.part.serialNumber}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4338CA)
                                                ),
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }

                                        if (p.part.syncState == SyncState.PENDING) {
                                            val isPendingOffline = syncStatus is AppSyncStatus.Offline || syncStatus is AppSyncStatus.PendingChangesOffline
                                            val isActivelySyncing = syncStatus is AppSyncStatus.Syncing
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = when {
                                                    isPendingOffline -> if (isDark) Color(0xFF3B1D25) else Color(0xFFFEE2E2)
                                                    isActivelySyncing -> if (isDark) Color(0xFF1E293B) else Color(0xFFDBEAFE)
                                                    else -> if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7)
                                                }
                                            ) {
                                                Text(
                                                    text = when {
                                                        isPendingOffline -> "Pending (Offline)"
                                                        isActivelySyncing -> "Syncing..."
                                                        else -> "Pending sync"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = when {
                                                            isPendingOffline -> if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                                                            isActivelySyncing -> if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)
                                                            else -> if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                                                        }
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = p.part.name,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryText
                                        )
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    // Pills: PN + Shelf Location (Render only if present)
                                    if (p.part.partNumber.isNotBlank() || p.part.shelfLocation.isNotBlank()) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (p.part.partNumber.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = pillBg
                                                ) {
                                                    Text(
                                                        text = if (p.part.partNumber.startsWith("PN:", ignoreCase = true)) {
                                                            p.part.partNumber
                                                        } else {
                                                            "PN: ${p.part.partNumber}"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = secondaryText
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            if (p.part.shelfLocation.isNotBlank()) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.LocationOn,
                                                        contentDescription = null,
                                                        tint = secondaryText,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = p.part.shelfLocation,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 11.5.sp,
                                                            color = secondaryText
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Status Badge with Pulsing Halo
                                Box(
                                    modifier = Modifier.size(46.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLow || isOut) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .graphicsLayer {
                                                    scaleX = pulseScale
                                                    scaleY = pulseScale
                                                    alpha = pulseAlpha
                                                }
                                                .background(color = statusBadgeTint, shape = CircleShape)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(color = statusBadgeBg, shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = statusIcon,
                                            contentDescription = null,
                                            tint = statusBadgeTint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = cardBorder, thickness = 1.dp)

                            // Bottom Row: Current Stock Metric + Pricing
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Stock Quantity
                                Column {
                                    Text(
                                        text = "CURRENT QUANTITY",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = secondaryText,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "$stock",
                                            style = MaterialTheme.typography.displaySmall.copy(
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Black,
                                                color = statusBadgeTint
                                            )
                                        )
                                        Text(
                                            text = "units ($statusText)",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = statusBadgeTint
                                            )
                                        )
                                    }
                                }

                                // Pricing Info
                                if (p.part.sellingPricePaise > 0 || p.part.mrpPaise > 0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "PRICE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = secondaryText,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "₹${p.part.sellingPricePaise / 100}",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFFC4B5FD) else cs.primary
                                            )
                                        )
                                        if (p.part.mrpPaise > 0 && p.part.mrpPaise != p.part.sellingPricePaise) {
                                            Text(
                                                text = "MRP ₹${p.part.mrpPaise / 100}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.5.sp,
                                                    color = secondaryText
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ─────────────────────────────────────────────
                // 2. STOCK ACTIONS SYSTEM
                // ─────────────────────────────────────────────
                item {
                    Text(
                        text = if (currentUserRole.isReadOnly) "Stock Permissions" else "Stock Actions",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = primaryText
                        ),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                item {
                    if (currentUserRole.isReadOnly) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(
                                            color = if (isDark) Color(0xFF262B3A) else Color(0xFFE2E8F0),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = secondaryText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Read-Only Access",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = primaryText
                                        )
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Viewers have read-only access. You can view parts and search shelf locations, but cannot modify stock.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = secondaryText,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Row 1: Add Stock & Remove Stock
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // ADD STOCK
                                PartStockActionTile(
                                    title = "Add Stock",
                                    subtitle = "",
                                    icon = Icons.Outlined.AddCircleOutline,
                                    iconBg = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7),
                                    iconTint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onClick = { activeActionType = MovementType.ADD },
                                    modifier = Modifier.weight(1f)
                                )

                                // REMOVE STOCK
                                PartStockActionTile(
                                    title = "Remove Stock",
                                    subtitle = "",
                                    icon = Icons.Outlined.RemoveCircleOutline,
                                    iconBg = if (isDark) Color(0xFF38141B) else Color(0xFFFEE2E2),
                                    iconTint = if (isDark) Color(0xFFFB7185) else Color(0xFFDC2626),
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onClick = { activeActionType = MovementType.REMOVE },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Row 2: Return & Adjust Count (Adjust Count only for Owner/Admin)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // RETURN
                                PartStockActionTile(
                                    title = "Return",
                                    subtitle = "",
                                    icon = Icons.Outlined.RotateLeft,
                                    iconBg = if (isDark) Color(0xFF2E2A48) else Color(0xFFEFEBFA),
                                    iconTint = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4F46E5),
                                    cardBg = cardBg,
                                    cardBorder = cardBorder,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onClick = { activeActionType = MovementType.RETURN },
                                    modifier = Modifier.weight(1f)
                                )

                                if (currentUserRole.canEditParts) {
                                    // ADJUST COUNT
                                    PartStockActionTile(
                                        title = "Adjust Count",
                                        subtitle = "",
                                        icon = Icons.Outlined.Tune,
                                        iconBg = if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7),
                                        iconTint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                        cardBg = cardBg,
                                        cardBorder = cardBorder,
                                        primaryText = primaryText,
                                        secondaryText = secondaryText,
                                        onClick = { activeActionType = MovementType.ADJUST },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // ─────────────────────────────────────────────
                // 3. RECENT MOVEMENT HISTORY FOR THIS PART
                // ─────────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Stock Movements",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = primaryText
                            )
                        )

                        if (movements.isNotEmpty()) {
                            Text(
                                text = "${movements.size} events",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.5.sp,
                                    color = secondaryText
                                )
                            )
                        }
                    }
                }

                if (movements.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No stock movements recorded for this part yet.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = secondaryText)
                                )
                            }
                        }
                    }
                } else {
                    items(movements, key = { it.id }) { movement ->
                        PartMovementHistoryCard(
                            movement = movement,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            pillBg = pillBg,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // STOCK ACTION DIALOG
    // ─────────────────────────────────────────────
    activeActionType?.let { type ->
        val part = partWithStock?.part
        val currentStock = partWithStock?.currentStock ?: 0
        if (part != null) {
            StockActionDialog(
                actionType = type,
                partName = "#${part.serialNumber} ${part.name}",
                currentStock = currentStock,
                onDismiss = { activeActionType = null },
                onConfirm = { deltaOrTarget, reason ->
                    when (type) {
                        MovementType.ADD -> viewModel.recordMovement(deltaOrTarget, MovementType.ADD, reason)
                        MovementType.REMOVE -> viewModel.recordMovement(-deltaOrTarget, MovementType.REMOVE, reason)
                        MovementType.RETURN -> viewModel.recordMovement(deltaOrTarget, MovementType.RETURN, reason)
                        MovementType.ADJUST -> viewModel.recordAdjustment(deltaOrTarget, reason)
                    }
                    activeActionType = null
                    scope.launch {
                        snackbarHostState.showSnackbar("Stock change recorded successfully.")
                    }
                }
            )
        }
    }

    // ─────────────────────────────────────────────
    // EDIT PART DIALOG
    // ─────────────────────────────────────────────
    if (showEditDialog && partWithStock != null && currentUserRole.canEditParts) {
        val currentPart = partWithStock!!.part
        EditPartDialog(
            part = currentPart,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, partNumber, shelf, sp, mrp ->
                viewModel.updateMetadata(name, partNumber, shelf, sp, mrp)
                showEditDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Part details updated.")
                }
            }
        )
    }

    // ─────────────────────────────────────────────
    // DELETE / ARCHIVE CONFIRMATION
    // ─────────────────────────────────────────────
    if (showDeleteConfirm && partWithStock != null && currentUserRole.canDeleteParts) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFFB7185) else Color(0xFFEF4444)
                    )
                    Text("Delete Part?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "This will remove '${partWithStock?.part?.name}' from active catalog and stock totals. Historical movement logs will remain safely preserved.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = secondaryText)
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFE11D48) else Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deletePart(onDeleted = onNavigateBack)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete Part", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = secondaryText)
                }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────
// Stock Action Tile (2x2 Grid)
// ─────────────────────────────────────────────────────────
@Composable
private fun PartStockActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    cardBg: Color,
    cardBorder: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
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
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = primaryText
                    )
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = secondaryText,
                            lineHeight = 13.sp
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
// Movement History Card for Specific Part
// ─────────────────────────────────────────────────────────
@Composable
private fun PartMovementHistoryCard(
    movement: MovementEntity,
    cardBg: Color,
    cardBorder: Color,
    primaryText: Color,
    secondaryText: Color,
    pillBg: Color,
    isDark: Boolean
) {
    val (opTitle, opIcon, iconBg, iconTint, deltaColor) = when (movement.type) {
        MovementType.ADD -> {
            val bg = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
            val tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
            Quadruple5("Add Stock", Icons.Outlined.AddCircleOutline, bg, tint, tint)
        }
        MovementType.REMOVE -> {
            val bg = if (isDark) Color(0xFF38141B) else Color(0xFFFEE2E2)
            val tint = if (isDark) Color(0xFFFB7185) else Color(0xFFDC2626)
            Quadruple5("Remove / Sell", Icons.Outlined.RemoveCircleOutline, bg, tint, tint)
        }
        MovementType.RETURN -> {
            val bg = if (isDark) Color(0xFF2E2A48) else Color(0xFFEFEBFA)
            val tint = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4F46E5)
            Quadruple5("Stock Return", Icons.Outlined.RotateLeft, bg, tint, tint)
        }
        MovementType.ADJUST -> {
            val bg = if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7)
            val tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
            Quadruple5("Stock Adjustment", Icons.Outlined.Tune, bg, tint, tint)
        }
    }

    val deltaString = if (movement.delta > 0) "+${movement.delta}" else "${movement.delta}"
    val previousStock = movement.previousRecordedStock ?: 0
    val resultingStock = movement.snapshotCount ?: (previousStock + movement.delta)

    val dateFormat = SimpleDateFormat("dd MMM · hh:mm a", Locale.US)
    val formattedDate = dateFormat.format(Date(movement.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Operation icon + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(color = iconBg, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = opIcon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = opTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryText
                            )
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = secondaryText
                            )
                        )
                    }
                }

                // Delta + Transition
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = deltaString,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = deltaColor
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = pillBg
                    ) {
                        Text(
                            text = "$previousStock → $resultingStock",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryText
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Reason if available
            if (!movement.reason.isNullOrBlank()) {
                Text(
                    text = "Reason: ${movement.reason}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = if (movement.type == MovementType.ADJUST) {
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

// ─────────────────────────────────────────────────────────
// Edit Part Dialog
// ─────────────────────────────────────────────────────────
@Composable
private fun EditPartDialog(
    part: com.marytwowheelers.spares.data.local.PartEntity,
    onDismiss: () -> Unit,
    onConfirm: (name: String, partNumber: String, shelfLocation: String, sellingPricePaise: Long, mrpPaise: Long) -> Unit
) {
    var name by remember { mutableStateOf(part.name) }
    var partNumber by remember { mutableStateOf(part.partNumber) }
    var shelfLocation by remember { mutableStateOf(part.shelfLocation) }
    var sellingPrice by remember { mutableStateOf(if (part.sellingPricePaise > 0) "${part.sellingPricePaise / 100}" else "") }
    var mrp by remember { mutableStateOf(if (part.mrpPaise > 0) "${part.mrpPaise / 100}" else "") }

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.red < 0.5f
    val cardBg = if (isDark) Color(0xFF1B1E26) else Color.White
    val secondaryText = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)

    val isSpValid = sellingPrice.isBlank() || (sellingPrice.toDoubleOrNull() != null && (sellingPrice.toDoubleOrNull() ?: 0.0) >= 0)
    val isMrpValid = mrp.isBlank() || (mrp.toDoubleOrNull() != null && (mrp.toDoubleOrNull() ?: 0.0) >= 0)
    val isFormValid = name.isNotBlank() && isSpValid && isMrpValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Part #${part.serialNumber}",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    label = { Text("Part Name *", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = partNumber,
                    onValueChange = { partNumber = it },
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    label = { Text("Part Number", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = shelfLocation,
                    onValueChange = { shelfLocation = it },
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    label = { Text("Location", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        label = { Text("Selling Price (₹)", fontSize = 12.sp) },
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = mrp,
                        onValueChange = { mrp = it },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        label = { Text("MRP (₹)", fontSize = 12.sp) },
                        prefix = { Text("₹ ", color = secondaryText, fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val spPaise = (sellingPrice.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
                    val mrpPaise = (mrp.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
                    onConfirm(name.trim(), partNumber.trim(), shelfLocation.trim(), spPaise, mrpPaise)
                },
                enabled = isFormValid,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF6366F1) else cs.primary,
                    contentColor = Color.White
                )
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontSize = 13.sp)
            }
        },
        containerColor = cardBg,
        shape = RoundedCornerShape(20.dp)
    )
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
private data class Quadruple5<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
