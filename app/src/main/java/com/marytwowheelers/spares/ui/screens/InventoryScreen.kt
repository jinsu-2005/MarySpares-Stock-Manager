package com.marytwowheelers.spares.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.marytwowheelers.spares.data.local.StockAlertManager
import com.marytwowheelers.spares.data.model.PartWithStock
import com.marytwowheelers.spares.data.model.StockState
import com.marytwowheelers.spares.data.model.stockState
import com.marytwowheelers.spares.sync.AppSyncStatus
import com.marytwowheelers.spares.ui.components.AddPartDialog
import com.marytwowheelers.spares.ui.components.AppSnackbarHost
import com.marytwowheelers.spares.ui.components.StockAlertDialog
import com.marytwowheelers.spares.ui.components.SyncStatusIndicator
import com.marytwowheelers.spares.ui.viewmodels.InventoryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class InventoryFilter { ALL, LOW_STOCK, OUT_OF_STOCK, IN_STOCK }
enum class InventorySort(val label: String, val icon: ImageVector) {
    SERIAL_ASC("Serial Number (#1, #2...)", Icons.Outlined.FormatListNumbered),
    NAME_ASC("Part Name (A–Z)", Icons.AutoMirrored.Outlined.Sort),
    NAME_DESC("Part Name (Z–A)", Icons.AutoMirrored.Outlined.Sort),
    STOCK_DESC("Highest Stock", Icons.Outlined.TrendingUp),
    STOCK_ASC("Lowest Stock", Icons.Outlined.TrendingDown),
    PRICE_DESC("Highest Price", Icons.Outlined.CurrencyRupee),
    PRICE_ASC("Lowest Price", Icons.Outlined.CurrencyRupee),
    LOCATION_ASC("Location (A–Z)", Icons.Outlined.LocationOn)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    autoFocusSearch: Boolean = false,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToPartDetails: (String) -> Unit
) {
    val searchQuery       by viewModel.searchQuery.collectAsState()
    val partsList         by viewModel.partsList.collectAsState()
    val syncStatus        by viewModel.syncStatus.collectAsState()
    val currentUserRole   by viewModel.currentUserRole.collectAsState()
    var selectedFilter    by remember { mutableStateOf(InventoryFilter.ALL) }
    var selectedSort      by remember { mutableStateOf(InventorySort.SERIAL_ASC) }
    var showSortSheet     by remember { mutableStateOf(false) }
    var showAddPartDialog by remember { mutableStateOf(false) }
    var showStockAlertDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        StockAlertManager.init(context)
        viewModel.triggerSync()
    }

    val acknowledgedKeys by StockAlertManager.acknowledgedKeys.collectAsState()
    val unreviewedAlertCount = remember(partsList, acknowledgedKeys) {
        partsList.filter { it.stockState != StockState.HEALTHY }
            .count { !acknowledgedKeys.contains(StockAlertManager.createAlertKey(it.part.id, it.currentStock)) }
    }

    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var isSearchFocused by remember { mutableStateOf(false) }

    // Multi-select state
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedPartIds = remember { mutableStateListOf<String>() }
    var showBulkDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeletingBulk by remember { mutableStateOf(false) }

    // Intercept back button when in selection mode
    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedPartIds.clear()
    }

    // Intercept back button when searching or focused
    BackHandler(enabled = searchQuery.isNotEmpty() || isSearchFocused || autoFocusSearch) {
        if (searchQuery.isNotEmpty()) {
            viewModel.onSearchQueryChange("")
            focusManager.clearFocus()
            keyboardController?.hide()
            isSearchFocused = false
        } else if (isSearchFocused) {
            focusManager.clearFocus()
            keyboardController?.hide()
            isSearchFocused = false
            if (autoFocusSearch && onNavigateBack != null) {
                onNavigateBack()
            }
        } else if (autoFocusSearch && onNavigateBack != null) {
            onNavigateBack()
        }
    }

    LaunchedEffect(autoFocusSearch) {
        if (autoFocusSearch) {
            delay(150)
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.red < 0.5f

    // ─── Theme Colors ─────────────────────────────
    val pageBg = if (isDark) Color(0xFF111318) else Color(0xFFF7F8FC)
    val cardBg = if (isDark) Color(0xFF1B1E26) else Color.White
    val cardBorder = if (isDark) Color(0xFF2A2E3D) else Color(0xFFEEF0FA)
    val primaryText = if (isDark) Color(0xFFF3F4F6) else Color(0xFF1E1B4B)
    val secondaryText = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val pillBg = if (isDark) Color(0xFF232734) else Color(0xFFF1F3F9)

    // Pill active colors (indigo/violet)
    val activeTabBg = if (isDark) Color(0xFF2E2A48) else Color(0xFFEEF2FF)
    val activeTabBorder = if (isDark) Color(0xFF6366F1) else Color(0xFFC7D2FE)
    val activeTabColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4338CA)

    val lowStockCount = partsList.count { it.stockState == StockState.LOW }
    val outOfStockCount = partsList.count { it.stockState == StockState.OUT }
    val inStockCount = partsList.count { it.stockState == StockState.HEALTHY }

    val filteredAndSortedList = remember(partsList, selectedFilter, searchQuery, selectedSort) {
        val filtered = when (selectedFilter) {
            InventoryFilter.ALL          -> partsList
            InventoryFilter.LOW_STOCK    -> partsList.filter { it.stockState == StockState.LOW }
            InventoryFilter.OUT_OF_STOCK -> partsList.filter { it.stockState == StockState.OUT }
            InventoryFilter.IN_STOCK     -> partsList.filter { it.stockState == StockState.HEALTHY }
        }

        when (selectedSort) {
            InventorySort.SERIAL_ASC   -> filtered.sortedBy { it.part.serialNumber }
            InventorySort.NAME_ASC     -> filtered.sortedBy { it.part.name.lowercase() }
            InventorySort.NAME_DESC    -> filtered.sortedByDescending { it.part.name.lowercase() }
            InventorySort.STOCK_DESC   -> filtered.sortedByDescending { it.currentStock }
            InventorySort.STOCK_ASC    -> filtered.sortedBy { it.currentStock }
            InventorySort.PRICE_DESC   -> filtered.sortedByDescending { it.part.sellingPricePaise }
            InventorySort.PRICE_ASC    -> filtered.sortedBy { it.part.sellingPricePaise }
            InventorySort.LOCATION_ASC -> filtered.sortedBy { it.part.shelfLocation.lowercase() }
        }
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = pageBg,
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
        topBar = {
            AnimatedContent(
                targetState = isSelectionMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "inventoryTopBar"
            ) { inSelection ->
                if (inSelection) {
                    Surface(
                        color = if (isDark) Color(0xFF1B1E26) else Color(0xFFF3F0FF),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF2A2E3D) else Color(0xFFDDD6FE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(onClick = {
                                    isSelectionMode = false
                                    selectedPartIds.clear()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel Selection",
                                        tint = primaryText
                                    )
                                }

                                Text(
                                    text = "${selectedPartIds.size} Selected",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = primaryText
                                    )
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val allSelected = filteredAndSortedList.isNotEmpty() && selectedPartIds.size == filteredAndSortedList.size
                                TextButton(
                                    onClick = {
                                        if (allSelected) {
                                            selectedPartIds.clear()
                                        } else {
                                            selectedPartIds.clear()
                                            selectedPartIds.addAll(filteredAndSortedList.map { it.part.id })
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (allSelected) "Deselect All" else "Select All",
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4F46E5),
                                        fontSize = 13.sp
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (selectedPartIds.isNotEmpty()) {
                                            showBulkDeleteConfirmDialog = true
                                        }
                                    },
                                    enabled = selectedPartIds.isNotEmpty()
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Bulk Delete",
                                        tint = if (selectedPartIds.isNotEmpty()) Color(0xFFEF4444) else secondaryText.copy(alpha = 0.35f)
                                    )
                                }
                            }
                        }
                    }
                } else {
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
                                text = "Inventory",
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
                                // Total parts count pill
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDark) Color(0xFF2E2A48) else Color(0xFFEFEBFA)
                                ) {
                                    Text(
                                        text = "${partsList.size} parts",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF5046E5)
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }

                                // Real Sync Status Indicator with Motion
                                SyncStatusIndicator(
                                    syncStatus = syncStatus,
                                    isDark = isDark,
                                    onClick = { viewModel.triggerSync() }
                                )

                                // Bell / Stock Alert Icon (Opens Stock Alert notification sheet, DOES NOT navigate to Add Part)
                                IconButton(
                                    onClick = { showStockAlertDialog = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (unreviewedAlertCount > 0) {
                                                Badge(
                                                    containerColor = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626),
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
            }
        },
        floatingActionButton = {
            if (!isSelectionMode && currentUserRole.canAddParts) {
                FloatingActionButton(
                    onClick = { showAddPartDialog = true },
                    containerColor = if (isDark) Color(0xFF5046E5) else Color(0xFF5046E5),
                    contentColor   = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add New Part",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // ─────────────────────────────────────────────
            // 1. SEARCH INPUT WITH FILTER ICON
            // ─────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .focusRequester(searchFocusRequester)
                    .onFocusChanged { isSearchFocused = it.isFocused },
                placeholder = {
                    Text(
                        "Search part name, no., or location",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = secondaryText,
                            fontSize = 15.sp
                        )
                    )
                },
                leadingIcon = {
                    if (isSearchFocused || searchQuery.isNotEmpty() || autoFocusSearch) {
                        IconButton(onClick = {
                            if (searchQuery.isNotEmpty()) {
                                viewModel.onSearchQueryChange("")
                            }
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            isSearchFocused = false
                            if (autoFocusSearch && onNavigateBack != null) {
                                onNavigateBack()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = primaryText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = secondaryText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = secondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = cardBg,
                    focusedContainerColor   = cardBg,
                    unfocusedBorderColor    = cardBorder,
                    focusedBorderColor      = if (isDark) Color(0xFF818CF8) else cs.primary,
                    unfocusedTextColor      = primaryText,
                    focusedTextColor        = primaryText
                )
            )

            // ─────────────────────────────────────────────
            // 2. ALWAYS-ACTIVE 3-FILTER SEGMENTED ROW (NO SLIDING NEEDED)
            // ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Filter 1: ALL
                InventorySegmentFilterChip(
                    label = "All (${partsList.size})",
                    isSelected = selectedFilter == InventoryFilter.ALL,
                    activeBg = if (isDark) Color(0xFF2E2A48) else Color(0xFFEFEBFA),
                    activeTextColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4338CA),
                    activeBorderColor = if (isDark) Color(0xFF4C4576) else Color(0xFFC7D2FE),
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    secondaryText = secondaryText,
                    onClick = { selectedFilter = InventoryFilter.ALL },
                    modifier = Modifier.weight(1f)
                )

                // Filter 2: LOW STOCK
                InventorySegmentFilterChip(
                    label = "Low ($lowStockCount)",
                    isSelected = selectedFilter == InventoryFilter.LOW_STOCK,
                    activeBg = if (isDark) Color(0xFF382A11) else Color(0xFFFEF3C7),
                    activeTextColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                    activeBorderColor = if (isDark) Color(0xFF6B4D16) else Color(0xFFFDE68A),
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    secondaryText = secondaryText,
                    onClick = { selectedFilter = InventoryFilter.LOW_STOCK },
                    modifier = Modifier.weight(1f)
                )

                // Filter 3: OUT OF STOCK / EMPTY
                InventorySegmentFilterChip(
                    label = "Empty ($outOfStockCount)",
                    isSelected = selectedFilter == InventoryFilter.OUT_OF_STOCK,
                    activeBg = if (isDark) Color(0xFF38141B) else Color(0xFFFEE2E2),
                    activeTextColor = if (isDark) Color(0xFFFB7185) else Color(0xFFDC2626),
                    activeBorderColor = if (isDark) Color(0xFF6B2130) else Color(0xFFFECACA),
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    secondaryText = secondaryText,
                    onClick = { selectedFilter = InventoryFilter.OUT_OF_STOCK },
                    modifier = Modifier.weight(1f)
                )
            }

            // ─────────────────────────────────────────────
            // 3. PARTS COUNT & ENHANCED SORT / SELECT BUTTON ROW
            // ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${String.format(Locale.US, "%,d", filteredAndSortedList.size)} parts",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = secondaryText,
                        fontWeight = FontWeight.Medium
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Select Button (Only Owner / Admin can bulk select/delete)
                    if (currentUserRole.canDeleteParts) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelectionMode) (if (isDark) Color(0xFF2E2A48) else Color(0xFFEEF2FF)) else pillBg,
                            border = BorderStroke(1.dp, if (isSelectionMode) (if (isDark) Color(0xFF6366F1) else Color(0xFFC7D2FE)) else cardBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    isSelectionMode = !isSelectionMode
                                    if (!isSelectionMode) selectedPartIds.clear()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelectionMode) Icons.Default.Close else Icons.Outlined.Checklist,
                                    contentDescription = "Select",
                                    tint = if (isSelectionMode) (if (isDark) Color(0xFFC4B5FD) else Color(0xFF5046E5)) else secondaryText,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isSelectionMode) "Done" else "Select",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.5.sp,
                                        color = if (isSelectionMode) (if (isDark) Color(0xFFC4B5FD) else Color(0xFF5046E5)) else primaryText,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }

                    // Sort Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = pillBg,
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showSortSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = selectedSort.icon,
                                contentDescription = "Sort",
                                tint = if (isDark) Color(0xFFC4B5FD) else Color(0xFF5046E5),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Sort: ${selectedSort.label}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.5.sp,
                                    color = primaryText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 4. INVENTORY PARTS LIST
            // ─────────────────────────────────────────────
            if (filteredAndSortedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (searchQuery.isBlank()) "No parts in this category." else "No results for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge.copy(color = secondaryText),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredAndSortedList, key = { it.part.id }) { part ->
                        val isPartSelected = selectedPartIds.contains(part.part.id)
                        InventoryPartCard(
                            part = part,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            pillBg = pillBg,
                            isDark = isDark,
                            isSelectionMode = isSelectionMode,
                            isSelected = isPartSelected,
                            onToggleSelect = {
                                if (isPartSelected) {
                                    selectedPartIds.remove(part.part.id)
                                } else {
                                    selectedPartIds.add(part.part.id)
                                }
                            },
                            onLongClick = {
                                if (currentUserRole.canDeleteParts) {
                                    isSelectionMode = true
                                    if (!isPartSelected) {
                                        selectedPartIds.add(part.part.id)
                                    }
                                }
                            },
                            onClick = { onNavigateToPartDetails(part.part.id) }
                        )
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // SORT DIALOG
    // ─────────────────────────────────────────────
    if (showSortSheet) {
        EnhancedSortDialog(
            currentSort = selectedSort,
            isDark = isDark,
            primaryText = primaryText,
            secondaryText = secondaryText,
            cardBg = cardBg,
            cardBorder = cardBorder,
            activeColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF5046E5),
            onSelectSort = { newSort ->
                selectedSort = newSort
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false }
        )
    }

    // ─────────────────────────────────────────────
    // ADD PART DIALOG
    // ─────────────────────────────────────────────
    if (showAddPartDialog) {
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

    // ─────────────────────────────────────────────
    // STOCK ALERT NOTIFICATION DIALOG
    // ─────────────────────────────────────────────
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
                selectedFilter = if (outOfStockCount > 0) InventoryFilter.OUT_OF_STOCK else InventoryFilter.LOW_STOCK
            }
        )
    }

    // ─────────────────────────────────────────────
    // BULK DELETE CONFIRMATION DIALOG
    // ─────────────────────────────────────────────
    if (showBulkDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingBulk) showBulkDeleteConfirmDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = Color(0xFFEF4444)
                    )
                    Text(
                        text = "Delete ${selectedPartIds.size} Part${if (selectedPartIds.size > 1) "s" else ""}?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = primaryText
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently remove these ${selectedPartIds.size} parts from inventory? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = secondaryText)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeletingBulk = true
                        val count = selectedPartIds.size
                        val ids = selectedPartIds.toList()
                        viewModel.deleteParts(ids) {
                            isDeletingBulk = false
                            showBulkDeleteConfirmDialog = false
                            isSelectionMode = false
                            selectedPartIds.clear()
                            scope.launch {
                                snackbarHostState.showSnackbar("Deleted $count parts from inventory.")
                            }
                        }
                    },
                    enabled = !isDeletingBulk,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isDeletingBulk) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBulkDeleteConfirmDialog = false },
                    enabled = !isDeletingBulk
                ) {
                    Text("Cancel", color = secondaryText)
                }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(22.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────
// Segmented Filter Chip (Fits full width without horizontal slide)
// ─────────────────────────────────────────────────────────
@Composable
private fun InventorySegmentFilterChip(
    label: String,
    isSelected: Boolean,
    activeBg: Color,
    activeTextColor: Color,
    activeBorderColor: Color,
    cardBg: Color,
    cardBorder: Color,
    secondaryText: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) activeBg else cardBg,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) activeBorderColor else cardBorder),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) activeTextColor else secondaryText
                ),
                maxLines = 1
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Sleek Inventory Part Card (with Serial Number & Pulsing Badge)
// ─────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoryPartCard(
    part: PartWithStock,
    cardBg: Color,
    cardBorder: Color,
    primaryText: Color,
    secondaryText: Color,
    pillBg: Color,
    isDark: Boolean,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onClick: () -> Unit
) {
    val stock = part.currentStock
    val isOut = stock <= 0
    val isLow = stock in 1..5

    // Pulsing animation for Low and Empty stock
    val infiniteTransition = rememberInfiniteTransition(label = "stockPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isLow || isOut) 1.32f else 1.0f,
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

    val statusBadgeBg = when {
        isOut -> if (isDark) Color(0xFF38141B) else Color(0xFFFEE2E2)
        isLow -> if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7)
        else  -> if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
    }

    val statusBadgeTint = when {
        isOut -> if (isDark) Color(0xFFFB7185) else Color(0xFFEF4444)
        isLow -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
        else  -> if (isDark) Color(0xFF34D399) else Color(0xFF10B981)
    }

    val stockNumberColor = when {
        isOut -> if (isDark) Color(0xFFFB7185) else Color(0xFFEF4444)
        isLow -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
        else  -> primaryText
    }

    val stockLabelText = when {
        isOut -> "Empty"
        isLow -> "Low"
        else  -> "Units"
    }

    val stockLabelColor = when {
        isOut -> if (isDark) Color(0xFFFB7185) else Color(0xFFEF4444)
        isLow -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
        else  -> secondaryText
    }

    val selectedBorderColor = if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1)
    val selectedCardBg = if (isDark) Color(0xFF232038) else Color(0xFFF5F3FF)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) selectedCardBg else cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) selectedBorderColor else cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection Checkbox Circle
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) (if (isDark) Color(0xFF6366F1) else Color(0xFF5046E5))
                            else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 0.dp else 1.5.dp,
                            color = if (isSelected) Color.Transparent else secondaryText.copy(alpha = 0.45f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
            }

            // ─── 1. SERIAL NUMBER COLUMN (FAR LEFT) ───────
            Box(
                modifier = Modifier.width(26.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "${part.part.serialNumber}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = secondaryText
                    ),
                    textAlign = TextAlign.Start
                )
            }

            // ─── 2. CENTER: TITLE & PILLS ─────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = part.part.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (part.part.partNumber.isNotBlank() || part.part.shelfLocation.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Part Number Pill (only if non-blank)
                        if (part.part.partNumber.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = pillBg
                            ) {
                                Text(
                                    text = if (part.part.partNumber.startsWith("PN:", ignoreCase = true)) {
                                        part.part.partNumber
                                    } else {
                                        "PN: ${part.part.partNumber}"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = secondaryText
                                    ),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Shelf Location with Location Pin Icon
                        if (part.part.shelfLocation.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Place,
                                    contentDescription = "Shelf Location",
                                    tint = if (isDark) Color(0xFFC4B5FD) else Color(0xFF5046E5),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = part.part.shelfLocation,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = secondaryText
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // ─── 3. RIGHT: STOCK QUANTITY & PULSING BADGE ─
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$stock",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = stockNumberColor
                        )
                    )
                    Text(
                        text = stockLabelText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = stockLabelColor
                        )
                    )
                }

                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLow || isOut) {
                        Box(
                            modifier = Modifier
                                .size(36.dp * pulseScale)
                                .background(
                                    color = statusBadgeBg.copy(alpha = pulseAlpha),
                                    shape = CircleShape
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(color = statusBadgeBg, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isOut -> Icons.Default.ErrorOutline
                                isLow -> Icons.Outlined.WarningAmber
                                else  -> Icons.Outlined.CheckCircle
                            },
                            contentDescription = null,
                            tint = statusBadgeTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Enhanced Sort Dialog / Modal (Lean & Focused)
// ─────────────────────────────────────────────────────────
@Composable
private fun EnhancedSortDialog(
    currentSort: InventorySort,
    isDark: Boolean,
    primaryText: Color,
    secondaryText: Color,
    cardBg: Color,
    cardBorder: Color,
    activeColor: Color,
    onSelectSort: (InventorySort) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = null,
                    tint = activeColor,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Sort Parts By",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InventorySort.values().forEach { option ->
                    val isSelected = currentSort == option
                    val optBg = if (isSelected) {
                        if (isDark) Color(0xFF2E2A48) else Color(0xFFEFEBFA)
                    } else {
                        if (isDark) Color(0xFF171A21) else Color(0xFFF9FAFB)
                    }
                    val optBorder = if (isSelected) {
                        if (isDark) Color(0xFF6366F1) else Color(0xFF6366F1)
                    } else {
                        cardBorder
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectSort(option) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = optBg),
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, optBorder),
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
                                    .size(34.dp)
                                    .background(
                                        color = if (isSelected) {
                                            if (isDark) Color(0xFF4338CA) else Color(0xFFC7D2FE)
                                        } else {
                                            if (isDark) Color(0xFF232734) else Color(0xFFE5E7EB)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        if (isDark) Color(0xFFC4B5FD) else Color(0xFF4338CA)
                                    } else {
                                        secondaryText
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.5.sp,
                                    color = if (isSelected) {
                                        if (isDark) Color(0xFFC4B5FD) else Color(0xFF4338CA)
                                    } else {
                                        primaryText
                                    }
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4338CA),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Close",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = activeColor
                    )
                )
            }
        },
        containerColor = cardBg,
        shape = RoundedCornerShape(20.dp)
    )
}
