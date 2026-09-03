package com.marytwowheelers.spares.ui.screens

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.marytwowheelers.spares.R
import com.marytwowheelers.spares.data.local.SyncState
import com.marytwowheelers.spares.data.model.AccessMember
import com.marytwowheelers.spares.data.model.AccessStatus
import com.marytwowheelers.spares.data.model.HistoryRetentionPeriod
import com.marytwowheelers.spares.data.model.UserRole
import com.marytwowheelers.spares.sync.AppSyncStatus
import com.marytwowheelers.spares.sync.SyncManager
import com.marytwowheelers.spares.ui.components.AppSnackbarHost
import com.marytwowheelers.spares.ui.components.CloudWipeAnimationDialog
import com.marytwowheelers.spares.ui.components.SyncStatusIndicator
import com.marytwowheelers.spares.ui.theme.ThemeMode
import com.marytwowheelers.spares.ui.viewmodels.SettingsViewModel
import com.marytwowheelers.spares.util.CsvExporter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val partsList by viewModel.partsList.collectAsState()
    val accessMembers by viewModel.accessMembers.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.red < 0.5f

    // ─── Theme Colors ─────────────────────────────
    val pageBg = if (isDark) Color(0xFF181A22) else Color(0xFFF7F8FC)
    val cardBg = if (isDark) Color(0xFF222530) else Color.White
    val cardBorder = if (isDark) Color(0xFF323748) else Color(0xFFEEF0FA)
    val primaryText = if (isDark) Color(0xFFF3F4F6) else Color(0xFF1E1B4B)
    val secondaryText = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val pillBg = if (isDark) Color(0xFF2C3140) else Color(0xFFF1F3F9)
    val accentColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF5046E5)

    // User Provider & Profile Data
    val hasGoogle = user?.providerData?.any { it.providerId == "google.com" } == true
    val hasPassword = user?.providerData?.any { it.providerId == "password" } == true
    val userEmail = user?.email?.lowercase()?.trim() ?: "No Email Registered"
    var currentDisplayName by remember(user) {
        mutableStateOf(user?.displayName?.ifBlank { null } ?: userEmail.substringBefore("@").replaceFirstChar { it.uppercase() })
    }

    // Role Capabilities
    val isUserAdmin = currentUserRole == UserRole.ADMIN || userEmail == "jinsu.j2005@gmail.com"
    val isUserOwner = currentUserRole == UserRole.OWNER
    val canManageUsers = isUserAdmin || isUserOwner || currentUserRole.canManageUsers
    val canClearHistory = isUserAdmin || isUserOwner || currentUserRole.canClearHistory
    val canResetLocalDb = isUserAdmin || isUserOwner || currentUserRole.canResetLocalDb
    val canDeleteCloudDb = isUserAdmin

    val effectiveRole = when {
        isUserAdmin -> UserRole.ADMIN
        isUserOwner -> UserRole.OWNER
        else -> currentUserRole
    }

    // Google Linking Launcher
    val webClientId = "630014392541-a6agtiugpiglok42hpqbebrjvdo0j7ik.apps.googleusercontent.com"
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val linkGoogleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null && user != null) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    user.linkWithCredential(credential).addOnCompleteListener { linkTask ->
                        if (linkTask.isSuccessful) {
                            scope.launch { snackbarHostState.showSnackbar("Google account linked successfully!") }
                        } else {
                            val msg = linkTask.exception?.localizedMessage ?: "Failed to link Google account"
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    }
                }
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Google linking error: ${e.localizedMessage}") }
            }
        }
    }

    var lastSyncTime by remember {
        mutableStateOf(SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date()))
    }

    // Clear History State
    var selectedRetentionPeriod by remember { mutableStateOf(HistoryRetentionPeriod.NINETY_DAYS) }
    var customDaysInput by remember { mutableStateOf("30") }
    val customDays = if (selectedRetentionPeriod == HistoryRetentionPeriod.CUSTOM) customDaysInput.toIntOrNull() else null
    var matchingHistoryCount by remember { mutableStateOf(0) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showCustomDaysDialog by remember { mutableStateOf(false) }
    var isClearingHistory by remember { mutableStateOf(false) }

    LaunchedEffect(selectedRetentionPeriod, customDaysInput, partsList) {
        matchingHistoryCount = viewModel.countHistoricalRecords(selectedRetentionPeriod, customDays)
    }

    // Dialogs State
    var showProfileDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showSetPasswordDialog by remember { mutableStateOf(false) }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var personToRevoke by remember { mutableStateOf<AccessMember?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showResetLocalDbDialog by remember { mutableStateOf(false) }
    var showDeleteCloudDbDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var isCloudDeleting by remember { mutableStateOf(false) }
    var isCloudDeleteComplete by remember { mutableStateOf(false) }
    var cloudBackupSavedPath by remember { mutableStateOf<String?>(null) }
    var cloudDeleteStatus by remember { mutableStateOf<String?>(null) }
    var cloudDeleteError by remember { mutableStateOf<String?>(null) }

    // SAF Document Launchers (User Chooses Target Directory / File)
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            isExporting = true
            val success = CsvExporter.exportInventoryToUri(context, uri, partsList)
            isExporting = false
            if (success) {
                scope.launch { snackbarHostState.showSnackbar("Inventory saved to chosen location (${partsList.size} parts).") }
            } else {
                scope.launch { snackbarHostState.showSnackbar("Failed to save CSV to chosen location.") }
            }
        }
    }

    val cloudBackupZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            isCloudDeleting = true
            isCloudDeleteComplete = false
            cloudBackupSavedPath = null
            cloudDeleteError = null
            cloudDeleteStatus = "Writing full ZIP backup to selected location..."

            viewModel.deleteEntireCloudDatabase(
                context = context,
                targetUri = uri,
                onProgress = { status ->
                    cloudDeleteStatus = status
                },
                onSuccess = { backupPath ->
                    isCloudDeleting = false
                    isCloudDeleteComplete = true
                    cloudBackupSavedPath = backupPath
                    cloudDeleteStatus = "Cloud & Local Database Successfully Wiped!"
                },
                onError = { err ->
                    isCloudDeleting = false
                    cloudDeleteStatus = null
                    cloudDeleteError = err
                }
            )
        }
    }

    Scaffold(
        containerColor = pageBg,
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
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
                        text = "Settings",
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
                        SyncStatusIndicator(
                            syncStatus = syncStatus,
                            isDark = isDark,
                            onClick = { viewModel.triggerSync() }
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF3E1824) else Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF882336) else Color(0xFFFECACA)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showLogoutDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "Sign Out",
                                    tint = if (isDark) Color(0xFFFF4D6D) else Color(0xFFDC2626),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Sign Out",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFF4D6D) else Color(0xFFDC2626)
                                    )
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ─────────────────────────────────────────────
            // 1. PROFILE & ACCOUNT (HERO CARD WITH ROLE BADGE)
            // ─────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showProfileDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, cardBorder),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Initials Avatar
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    color = if (isDark) Color(0xFF2E2A48) else Color(0xFFEFEBFA),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentDisplayName.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = accentColor
                                )
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = currentDisplayName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = primaryText
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (effectiveRole) {
                                        UserRole.ADMIN -> if (isDark) Color(0xFF4C1D95) else Color(0xFFEDE9FE)
                                        UserRole.OWNER -> if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
                                        UserRole.STAFF -> if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
                                        UserRole.VIEWER -> if (isDark) Color(0xFF134E4A) else Color(0xFFCCFBF1)
                                    }
                                ) {
                                    Text(
                                        text = effectiveRole.displayName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (effectiveRole) {
                                                UserRole.ADMIN -> if (isDark) Color(0xFFDDD6FE) else Color(0xFF6D28D9)
                                                UserRole.OWNER -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                                UserRole.STAFF -> if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)
                                                UserRole.VIEWER -> if (isDark) Color(0xFF5EEAD4) else Color(0xFF0D9488)
                                            }
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(3.dp))

                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.5.sp,
                                    color = secondaryText
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "View Profile",
                            tint = secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 2. ACCESS MANAGEMENT (ADMIN & OWNER ONLY)
            // ─────────────────────────────────────────────
            if (canManageUsers) {
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "User Access & Roles",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = primaryText
                                        )
                                    )
                                    Text(
                                        text = "Only invited members can register & access the store inventory",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            color = secondaryText
                                        )
                                    )
                                }

                                Button(
                                    onClick = { showAddPersonDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Color(0xFF2E2A48) else Color(0xFFEEF2FF),
                                        contentColor = accentColor
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add User", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Members List (Hide Admin from non-admin users)
                            val visibleMembers = if (isUserAdmin) {
                                accessMembers
                            } else {
                                accessMembers.filter { it.role != UserRole.ADMIN && it.email.lowercase() != "jinsu.j2005@gmail.com" }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (visibleMembers.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = pillBg,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "No additional users invited. Tap 'Add User' to grant access.",
                                            style = MaterialTheme.typography.bodySmall.copy(color = secondaryText),
                                            modifier = Modifier.padding(14.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    visibleMembers.forEach { member ->
                                        val isRootAccount = member.email.lowercase() == "jinsu.j2005@gmail.com"

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = pillBg,
                                            border = BorderStroke(1.dp, cardBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .background(
                                                                color = if (isDark) Color(0xFF262B3A) else Color(0xFFE2E8F0),
                                                                shape = CircleShape
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = member.name.take(1).uppercase(),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = primaryText
                                                        )
                                                    }

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = member.name,
                                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = primaryText,
                                                                    fontSize = 13.5.sp
                                                                ),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )

                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = when (member.role) {
                                                                    UserRole.ADMIN -> if (isDark) Color(0xFF4C1D95) else Color(0xFFEDE9FE)
                                                                    UserRole.OWNER -> if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
                                                                    UserRole.STAFF -> if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
                                                                    UserRole.VIEWER -> if (isDark) Color(0xFF134E4A) else Color(0xFFCCFBF1)
                                                                }
                                                            ) {
                                                                Text(
                                                                    text = member.role.displayName,
                                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                                        fontSize = 9.5.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = when (member.role) {
                                                                            UserRole.ADMIN -> if (isDark) Color(0xFFDDD6FE) else Color(0xFF6D28D9)
                                                                            UserRole.OWNER -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                                                            UserRole.STAFF -> if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)
                                                                            UserRole.VIEWER -> if (isDark) Color(0xFF5EEAD4) else Color(0xFF0D9488)
                                                                        }
                                                                    ),
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                )
                                                            }

                                                            if (member.email.equals(userEmail, ignoreCase = true)) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = if (isDark) Color(0xFF2E2A48) else Color(0xFFEDE9FE)
                                                                ) {
                                                                    Text(
                                                                        text = "You",
                                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                                            fontSize = 9.5.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = if (isDark) Color(0xFFC4B5FD) else Color(0xFF4F46E5)
                                                                        ),
                                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        Text(
                                                            text = member.email,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 11.5.sp,
                                                                color = secondaryText
                                                            ),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                val isSelf = member.email.equals(userEmail, ignoreCase = true)
                                                val canDeleteMember = isUserAdmin || (isUserOwner && member.role != UserRole.OWNER && member.role != UserRole.ADMIN)

                                                if (!isRootAccount && !isSelf && canDeleteMember) {
                                                    IconButton(
                                                        onClick = { personToRevoke = member },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.RemoveCircleOutline,
                                                            contentDescription = "Remove User",
                                                            tint = Color(0xFFEF4444),
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
                }
            }

            // ─────────────────────────────────────────────
            // 3. APPEARANCE THEME SELECTOR
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = primaryText
                            )
                        )

                        Text(
                            text = "Choose your preferred theme across the application.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = secondaryText
                            )
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf(
                                ThemeMode.LIGHT to "☀️ Light",
                                ThemeMode.SYSTEM to "🖥️ System",
                                ThemeMode.DARK to "🌙 Dark"
                            ).forEachIndexed { index, (mode, label) ->
                                SegmentedButton(
                                    selected = currentTheme == mode,
                                    onClick = { onThemeChange(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = if (isDark) Color(0xFF2E2A48) else Color(0xFFEFEBFA),
                                        activeContentColor = accentColor,
                                        inactiveContainerColor = Color.Transparent,
                                        inactiveContentColor = secondaryText,
                                        activeBorderColor = if (isDark) Color(0xFF3E3666) else Color(0xFFDDD6FE),
                                        inactiveBorderColor = cardBorder
                                    ),
                                    label = {
                                        Text(
                                            text = label,
                                            fontWeight = if (currentTheme == mode) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.5.sp
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 4. CLOUD SYNCHRONIZATION & EXPORT
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Cloud Synchronization",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = primaryText
                                    )
                                )
                                Text(
                                    text = when (val s = syncStatus) {
                                        is AppSyncStatus.Offline -> "Offline mode. Changes will sync when network is restored."
                                        is AppSyncStatus.PendingChangesOffline -> "${s.pendingCount} pending change(s) saved offline."
                                        is AppSyncStatus.Syncing -> "Synchronizing changes with Firebase Cloud..."
                                        is AppSyncStatus.PendingChanges -> "${s.pendingCount} pending change(s) ready to sync."
                                        is AppSyncStatus.Synced -> "All inventory data is synced with Firebase cloud."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = secondaryText
                                    )
                                )
                            }

                            SyncStatusIndicator(
                                syncStatus = syncStatus,
                                isDark = isDark,
                                onClick = { viewModel.triggerSync() }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "LAST SUCCESSFUL SYNC",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = secondaryText
                                    )
                                )
                                Text(
                                    text = lastSyncTime,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryText
                                    )
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.triggerSync()
                                    lastSyncTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date())
                                    scope.launch { snackbarHostState.showSnackbar("Sync requested.") }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF2E2A48) else Color(0xFFEEF2FF),
                                    contentColor = accentColor
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Sync Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // CSV Export Action Tile
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = pillBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (partsList.isEmpty()) {
                                        scope.launch { snackbarHostState.showSnackbar("Inventory is empty. Add parts to export.") }
                                    } else {
                                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                        exportCsvLauncher.launch("Mary_Spares_Inventory_$timeStamp.csv")
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            color = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FileDownload,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Export Inventory (CSV)",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = primaryText
                                        )
                                    )
                                    Text(
                                        text = "Download complete inventory spreadsheet for Excel or Google Sheets",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.5.sp,
                                            color = secondaryText
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = secondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 5. CLEAR HISTORY (ADMIN & OWNER ONLY)
            // ─────────────────────────────────────────────
            if (canClearHistory) {
                item {
                    var isDropdownExpanded by remember { mutableStateOf(false) }

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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Clear History",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = primaryText
                                    )
                                )

                                if (matchingHistoryCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isDark) Color(0xFF382A11) else Color(0xFFFEF3C7)
                                    ) {
                                        Text(
                                            text = "$matchingHistoryCount logs found",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                                            ),
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = pillBg,
                                        border = BorderStroke(1.dp, cardBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { isDropdownExpanded = true }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 11.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = when {
                                                        selectedRetentionPeriod == HistoryRetentionPeriod.NEVER -> "Never (Keep all)"
                                                        selectedRetentionPeriod == HistoryRetentionPeriod.CUSTOM && customDaysInput.isNotBlank() -> "Custom (${customDaysInput}d)"
                                                        else -> selectedRetentionPeriod.label
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = primaryText,
                                                        fontSize = 13.5.sp
                                                    )
                                                )
                                                if (selectedRetentionPeriod == HistoryRetentionPeriod.CUSTOM) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Edit,
                                                        contentDescription = "Edit days",
                                                        tint = accentColor,
                                                        modifier = Modifier
                                                            .size(15.dp)
                                                            .clickable { showCustomDaysDialog = true }
                                                    )
                                                }
                                            }

                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Select period",
                                                tint = secondaryText,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = isDropdownExpanded,
                                        onDismissRequest = { isDropdownExpanded = false },
                                        modifier = Modifier.background(cardBg)
                                    ) {
                                        HistoryRetentionPeriod.values().forEach { period ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = if (period == HistoryRetentionPeriod.NEVER) "Never (Keep all)" else period.label,
                                                        fontWeight = if (selectedRetentionPeriod == period) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (selectedRetentionPeriod == period) accentColor else primaryText
                                                    )
                                                },
                                                onClick = {
                                                    selectedRetentionPeriod = period
                                                    isDropdownExpanded = false
                                                    if (period == HistoryRetentionPeriod.CUSTOM) {
                                                        showCustomDaysDialog = true
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = { showClearHistoryDialog = true },
                                    enabled = selectedRetentionPeriod != HistoryRetentionPeriod.NEVER && matchingHistoryCount > 0 && !isClearingHistory,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Color(0xFF381520) else Color(0xFFFEE2E2),
                                        contentColor = if (isDark) Color(0xFFFF4D6D) else Color(0xFFDC2626),
                                        disabledContainerColor = if (isDark) Color(0xFF1E212A) else Color(0xFFF3F4F6),
                                        disabledContentColor = if (isDark) Color(0xFF4B5563) else Color(0xFF9CA3AF)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp)
                                ) {
                                    Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Clear", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 6. RESET LOCAL DATABASE (ADMIN & OWNER ONLY)
            // ─────────────────────────────────────────────
            if (canResetLocalDb) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF882336) else Color(0xFFFEE2E2)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Reset Local Database",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isDark) Color(0xFFFF4D6D) else Color(0xFFEF4444)
                                )
                            )

                            Text(
                                text = "Deletes cached local SQLite data on this device. Cloud records in Firebase remain safe and can be re-synced.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = secondaryText,
                                    lineHeight = 16.sp
                                )
                            )

                            OutlinedButton(
                                onClick = { showResetLocalDbDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF882336) else Color(0xFFEF4444)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.DeleteForever, null, tint = if (isDark) Color(0xFFFF4D6D) else Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Reset Local Database Cache", color = if (isDark) Color(0xFFFF4D6D) else Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 7. DELETE ENTIRE CLOUD DATABASE (STRICTLY ADMIN ONLY)
            // ─────────────────────────────────────────────
            if (canDeleteCloudDb) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF26151B) else Color(0xFFFFF1F2)
                        ),
                        border = BorderStroke(1.5.dp, if (isDark) Color(0xFF882336) else Color(0xFFDC2626)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFFF4D6D) else Color(0xFFDC2626),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Delete Entire Cloud Database",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = if (isDark) Color(0xFFFF4D6D) else Color(0xFFDC2626)
                                    )
                                )
                            }

                            Text(
                                text = "Admin-only highly destructive operation. Permanently purges all Firestore cloud inventory, stock movements, and user records. A verified CSV backup will be saved locally on your device before cloud deletion is executed.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B),
                                    lineHeight = 16.sp
                                )
                            )

                            Button(
                                onClick = { showDeleteCloudDbDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFFE11D48) else Color(0xFFDC2626),
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.DeleteSweep, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Delete Entire Cloud Database (Admin)", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }



            // ─────────────────────────────────────────────
            // 8. BRAND HEADER & CREATOR / GITHUB UPDATES SECTION
            // ─────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, cardBorder),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Official Brand Header Graphic
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0xFF1E1036) else Color(0xFF34005F),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF45247B) else Color(0x2234005F)
                            )
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.mary_spares_brand_header),
                                contentDescription = "Mary Two Wheelers Spares",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }

                        // Version & Cloud Sync Badges
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = pillBg
                            ) {
                                Text(
                                    text = "v1.0.4 • Build 2026",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = secondaryText
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(6.dp).background(if (isDark) Color(0xFF34D399) else Color(0xFF059669), CircleShape)
                                    )
                                    Text(
                                        text = "Cloud Sync Active",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                        )
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            thickness = 0.8.dp,
                            color = cardBorder
                        )

                        // Creator Info & GitHub Updates Action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            color = if (isDark) Color(0xFF2E2A48) else Color(0xFFEEF2FF),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "JJ",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor,
                                            fontSize = 13.sp
                                        )
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Jinsu J",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = primaryText
                                        )
                                    )
                                    Text(
                                        text = "Developer & Maintainer",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            color = secondaryText
                                        )
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/jinsu-2005/MarySpares-Stock-Manager/releases"))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        scope.launch { snackbarHostState.showSnackbar("Unable to open browser.") }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF323748) else Color(0xFF1E1B4B),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Code,
                                    contentDescription = "GitHub",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "GitHub / Updates",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
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
    // PROFILE DETAILS MODAL
    // ─────────────────────────────────────────────
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(color = if (isDark) Color(0xFF2E2A48) else Color(0xFFEFEBFA), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentDisplayName.take(2).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = accentColor)
                        )
                    }

                    Column {
                        Text(text = currentDisplayName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = primaryText))
                        Text(text = "$userEmail • ${effectiveRole.displayName}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = secondaryText))
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Google Sign-In Tile
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = pillBg,
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(painter = painterResource(id = R.drawable.ic_google_logo), contentDescription = "Google", tint = Color.Unspecified, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(text = "Google Account", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = primaryText))
                                    Text(text = if (hasGoogle) "Connected" else "Not connected", style = MaterialTheme.typography.labelSmall.copy(color = if (hasGoogle) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else secondaryText))
                                }
                            }

                            if (!hasGoogle) {
                                Button(
                                    onClick = {
                                        showProfileDialog = false
                                        googleSignInClient.signOut().addOnCompleteListener {
                                            linkGoogleLauncher.launch(googleSignInClient.signInIntent)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2E2A48) else Color(0xFFEEF2FF), contentColor = accentColor),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Connected", tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Email & Password Tile
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = pillBg,
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(imageVector = Icons.Outlined.Lock, contentDescription = "Password", tint = if (hasPassword) accentColor else secondaryText, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(text = "Email & Password", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = primaryText))
                                    Text(text = if (hasPassword) "Active" else "Not set up", style = MaterialTheme.typography.labelSmall.copy(color = if (hasPassword) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else secondaryText))
                                }
                            }

                            if (!hasPassword) {
                                Button(
                                    onClick = {
                                        showProfileDialog = false
                                        showSetPasswordDialog = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2E2A48) else Color(0xFFEEF2FF), contentColor = accentColor),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Set up", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                TextButton(
                                    onClick = {
                                        if (user?.email != null) {
                                            auth.sendPasswordResetEmail(user.email!!)
                                            showProfileDialog = false
                                            scope.launch { snackbarHostState.showSnackbar("Password reset link sent to ${user.email}") }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Reset", fontSize = 12.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showProfileDialog = false
                            showEditNameDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit Display Name")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) { Text("Close", color = secondaryText) }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // ─────────────────────────────────────────────
    // EDIT DISPLAY NAME DIALOG
    // ─────────────────────────────────────────────
    if (showEditNameDialog) {
        var newNameInput by remember { mutableStateOf(currentDisplayName) }

        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Display Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    label = { Text("Display Name") },
                    placeholder = { Text("e.g. John Owner") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newNameInput.trim()
                        if (trimmed.isNotBlank() && user != null) {
                            val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(trimmed).build()
                            user.updateProfile(profileUpdates).addOnCompleteListener {
                                currentDisplayName = trimmed
                                scope.launch { snackbarHostState.showSnackbar("Display name updated.") }
                            }
                        }
                        showEditNameDialog = false
                    },
                    enabled = newNameInput.isNotBlank(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel", color = secondaryText) }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // ─────────────────────────────────────────────
    // SET UP PASSWORD DIALOG
    // ─────────────────────────────────────────────
    if (showSetPasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var passwordError by remember { mutableStateOf<String?>(null) }
        var isSettingPassword by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSettingPassword) showSetPasswordDialog = false },
            title = { Text(if (hasPassword) "Change Password" else "Set up Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (hasPassword) "Enter a new password for your account ($userEmail)." else "Set a password so you can sign in directly using $userEmail.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryText
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; passwordError = null },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; passwordError = null },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (passwordError != null) {
                        Text(text = passwordError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.length < 6) {
                            passwordError = "Password must be at least 6 characters"
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            passwordError = "Passwords do not match"
                            return@Button
                        }
                        if (user != null && user.email != null) {
                            isSettingPassword = true
                            val credential = EmailAuthProvider.getCredential(user.email!!, newPassword)
                            user.linkWithCredential(credential).addOnCompleteListener { task ->
                                isSettingPassword = false
                                if (task.isSuccessful) {
                                    showSetPasswordDialog = false
                                    scope.launch { snackbarHostState.showSnackbar("Password set up successfully!") }
                                } else {
                                    passwordError = task.exception?.localizedMessage ?: "Failed to set up password"
                                }
                            }
                        }
                    },
                    enabled = !isSettingPassword,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSettingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Save Password", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPasswordDialog = false }, enabled = !isSettingPassword) { Text("Cancel", color = secondaryText) }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // ─────────────────────────────────────────────
    // ADD MEMBER INVITATION DIALOG (WITH ROLE PICKER: Owner, Staff, Relative, Friend)
    // ─────────────────────────────────────────────
    if (showAddPersonDialog) {
        var newPersonEmail by remember { mutableStateOf("") }
        var newPersonName by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf(UserRole.STAFF) }
        var isAddingMember by remember { mutableStateOf(false) }
        var addMemberError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isAddingMember) showAddPersonDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.PersonAdd, null, tint = accentColor)
                    Text("Grant Store Access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Authorize a user to access the Mary Spares inventory and select their role.",
                        style = MaterialTheme.typography.bodySmall.copy(color = secondaryText)
                    )

                    OutlinedTextField(
                        value = newPersonEmail,
                        onValueChange = { newPersonEmail = it; addMemberError = null },
                        label = { Text("Email Address *") },
                        placeholder = { Text("e.g. member@gmail.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPersonName,
                        onValueChange = { newPersonName = it },
                        label = { Text("Name / Label (Optional)") },
                        placeholder = { Text("e.g. Counter Staff") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "ASSIGN ROLE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = secondaryText, fontSize = 11.sp)
                    )

                    // Role Picker Segmented / Chip Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(UserRole.OWNER, UserRole.STAFF, UserRole.VIEWER).forEach { roleOption ->
                            val isSelected = selectedRole == roleOption
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) {
                                    if (isDark) Color(0xFF2E2A48) else Color(0xFFEDE9FE)
                                } else {
                                    pillBg
                                },
                                border = BorderStroke(1.dp, if (isSelected) accentColor else cardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedRole = roleOption }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = roleOption.displayName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.5.sp,
                                            color = if (isSelected) accentColor else secondaryText
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    addMemberError?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = newPersonEmail.lowercase().trim()
                        val name = newPersonName.trim().ifBlank { email.substringBefore("@") }
                        isAddingMember = true
                        addMemberError = null

                        viewModel.addMemberInvitation(
                            email = email,
                            name = name,
                            role = selectedRole,
                            invitedBy = currentDisplayName
                        ) { success, err ->
                            isAddingMember = false
                            if (success) {
                                showAddPersonDialog = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Access granted to $name as ${selectedRole.displayName}!")
                                }
                            } else {
                                addMemberError = err ?: "Failed to grant access"
                            }
                        }
                    },
                    enabled = newPersonEmail.contains("@") && newPersonEmail.contains(".") && !isAddingMember,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isAddingMember) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Authorize Member", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPersonDialog = false }, enabled = !isAddingMember) {
                    Text("Cancel", color = secondaryText)
                }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // ─────────────────────────────────────────────
    // REMOVE MEMBER CONFIRMATION
    // ─────────────────────────────────────────────
    personToRevoke?.let { person ->
        AlertDialog(
            onDismissRequest = { personToRevoke = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.WarningAmber, null, tint = Color(0xFFEF4444))
                    Text("Remove User Access?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to remove '${person.name}' (${person.email})? They will no longer be able to log in or access Mary Spares.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = secondaryText)
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                    onClick = {
                        viewModel.removeMemberInvitation(person.email) { success, err ->
                            personToRevoke = null
                            if (success) {
                                scope.launch { snackbarHostState.showSnackbar("Access removed for ${person.name}") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Remove User", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { personToRevoke = null }) { Text("Cancel", color = secondaryText) }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // ─────────────────────────────────────────────
    // RESET LOCAL DATABASE DIALOG
    // ─────────────────────────────────────────────
    if (showResetLocalDbDialog) {
        AlertDialog(
            onDismissRequest = { showResetLocalDbDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.DeleteForever, null, tint = Color(0xFFEF4444))
                    Text("Reset Local Database?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This will clear all local cached tables on this device.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    )
                    Text(
                        text = "Firebase cloud records remain safe and will re-download upon sync.",
                        style = MaterialTheme.typography.bodySmall.copy(color = secondaryText, lineHeight = 16.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                    onClick = {
                        showResetLocalDbDialog = false
                        viewModel.resetLocalDatabase {
                            scope.launch { snackbarHostState.showSnackbar("Local SQLite database cache reset.") }
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reset Local DB", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetLocalDbDialog = false }) { Text("Cancel", color = secondaryText) }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // ─────────────────────────────────────────────
    // ─────────────────────────────────────────────
    // DELETE ENTIRE CLOUD DATABASE DIALOG & ANIMATION
    // ─────────────────────────────────────────────
    if (isCloudDeleting || isCloudDeleteComplete || (cloudDeleteError != null && showDeleteCloudDbDialog)) {
        CloudWipeAnimationDialog(
            statusText = cloudDeleteStatus,
            isComplete = isCloudDeleteComplete,
            errorMessage = cloudDeleteError,
            backupDestination = cloudBackupSavedPath,
            onDismiss = {
                isCloudDeleting = false
                isCloudDeleteComplete = false
                cloudDeleteStatus = null
                cloudDeleteError = null
                showDeleteCloudDbDialog = false
            }
        )
    } else if (showDeleteCloudDbDialog) {
        var confirmationInput by remember { mutableStateOf("") }
        val requiredKeyword = "DELETE ENTIRE CLOUD DATABASE"
        val isKeywordMatch = confirmationInput.trim() == requiredKeyword

        AlertDialog(
            onDismissRequest = { showDeleteCloudDbDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFDC2626), modifier = Modifier.size(26.dp))
                    Text("Delete Entire Cloud Database", fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF38151D) else Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "CRITICAL WARNING: This permanently wipes all inventory, movements, and user records from Firebase Cloud.",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), fontSize = 12.sp)
                            )
                            Text(
                                text = "1. An export of all Firestore data as a full ZIP archive will be saved directly to your chosen folder before deletion.\n2. Cloud deletion will only proceed if the backup is verified.",
                                style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B), fontSize = 11.5.sp)
                            )
                        }
                    }

                    Text(
                        text = "To confirm, type exactly: $requiredKeyword",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = primaryText, fontSize = 12.sp)
                    )

                    OutlinedTextField(
                        value = confirmationInput,
                        onValueChange = { confirmationInput = it },
                        placeholder = { Text(requiredKeyword, color = secondaryText.copy(alpha = 0.5f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFDC2626),
                            unfocusedBorderColor = cardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        cloudBackupZipLauncher.launch("MarySpares_Full_Backup_$timeStamp.zip")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    enabled = isKeywordMatch
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderZip,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Choose Location & Wipe Database", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCloudDbDialog = false }) {
                    Text("Cancel", color = secondaryText)
                }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // ─────────────────────────────────────────────
    // CUSTOM DAYS DIALOG
    // ─────────────────────────────────────────────
    if (showCustomDaysDialog) {
        var tempDaysInput by remember { mutableStateOf(customDaysInput.ifBlank { "30" }) }
        val tempDays = tempDaysInput.toIntOrNull()
        val isValid = tempDays != null && tempDays in 1..3650

        AlertDialog(
            onDismissRequest = { showCustomDaysDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = accentColor
                    )
                    Text(
                        text = "Custom History Retention",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = primaryText
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter the number of days of history logs to retain. Logs older than this will be matched for clearing.",
                        style = MaterialTheme.typography.bodySmall.copy(color = secondaryText)
                    )

                    OutlinedTextField(
                        value = tempDaysInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 4) {
                                tempDaysInput = input
                            }
                        },
                        label = { Text("Retention Duration") },
                        placeholder = { Text("e.g. 30") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            focusedLabelColor = accentColor
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        suffix = { Text("Days", color = secondaryText, fontSize = 12.sp) }
                    )

                    // Quick Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(7, 14, 30, 60, 180).forEach { days ->
                            val isChipSelected = tempDaysInput == "$days"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChipSelected) (if (isDark) Color(0xFF2E2A48) else Color(0xFFEEF2FF)) else pillBg,
                                border = BorderStroke(1.dp, if (isChipSelected) (if (isDark) Color(0xFF6366F1) else Color(0xFFC7D2FE)) else cardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { tempDaysInput = "$days" }
                            ) {
                                Text(
                                    text = "${days}d",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isChipSelected) (if (isDark) Color(0xFFC4B5FD) else Color(0xFF4338CA)) else secondaryText
                                    ),
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    if (isValid) {
                        val cutoffTime = System.currentTimeMillis() - (tempDays!!.toLong() * 24L * 60L * 60L * 1000L)
                        val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(cutoffTime))
                        Text(
                            text = "Logs created before $formattedDate will be deleted.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isValid) {
                            customDaysInput = tempDaysInput
                            selectedRetentionPeriod = HistoryRetentionPeriod.CUSTOM
                            showCustomDaysDialog = false
                        }
                    },
                    enabled = isValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF6366F1) else Color(0xFF5046E5),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply (${tempDaysInput}d)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDaysDialog = false }) {
                    Text("Cancel", color = secondaryText)
                }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // ─────────────────────────────────────────────
    // CLEAR HISTORY DIALOG
    // ─────────────────────────────────────────────
    if (showClearHistoryDialog) {
        val periodLabel = if (selectedRetentionPeriod == HistoryRetentionPeriod.CUSTOM) {
            "${customDaysInput} days"
        } else {
            selectedRetentionPeriod.label
        }

        AlertDialog(
            onDismissRequest = { if (!isClearingHistory) showClearHistoryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.DeleteOutline, null, tint = Color(0xFFEF4444))
                    Text("Clear Historical Logs?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete $matchingHistoryCount history log(s) older than $periodLabel? Current part quantities will NOT be affected.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = secondaryText)
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                    onClick = {
                        isClearingHistory = true
                        viewModel.clearHistory(
                            period = selectedRetentionPeriod,
                            customDays = customDays,
                            onSuccess = { deleted ->
                                isClearingHistory = false
                                showClearHistoryDialog = false
                                scope.launch { snackbarHostState.showSnackbar("Cleared $deleted historical log(s).") }
                            },
                            onError = { err ->
                                isClearingHistory = false
                                showClearHistoryDialog = false
                                scope.launch { snackbarHostState.showSnackbar("Failed: $err") }
                            }
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isClearingHistory
                ) {
                    if (isClearingHistory) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Clear Logs", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }, enabled = !isClearingHistory) {
                    Text("Cancel", color = secondaryText)
                }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // ─────────────────────────────────────────────
    // LOGOUT CONFIRMATION
    // ─────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFFF4D6D) else Color(0xFFDC2626)
                    )
                    Text("Log Out?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to log out of Mary Spares? You will need to sign in again to access stock records.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = secondaryText)
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFE11D48) else Color(0xFFDC2626),
                        contentColor = Color.White
                    ),
                    onClick = {
                        showLogoutDialog = false
                        com.marytwowheelers.spares.data.repository.AccessRepository.getInstance(context).clearSession()
                        auth.signOut()
                        googleSignInClient.signOut().addOnCompleteListener {
                            onLogout()
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Log Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel", color = secondaryText) }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(22.dp)
        )
    }
}
