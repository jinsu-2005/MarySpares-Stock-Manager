package com.marytwowheelers.spares.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.marytwowheelers.spares.R
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.marytwowheelers.spares.data.model.UserRole
import com.marytwowheelers.spares.data.repository.AccessRepository
import com.marytwowheelers.spares.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val accessRepo = remember { AccessRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val nameRequester = remember { BringIntoViewRequester() }
    val emailRequester = remember { BringIntoViewRequester() }
    val passwordRequester = remember { BringIntoViewRequester() }
    val confirmPasswordRequester = remember { BringIntoViewRequester() }

    // ─── Theme Detection ───────────────────────────
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val brandAccent = if (isDark) Color(0xFFA78BFA) else BrandPurple
    val primaryText = if (isDark) Color(0xFFF3F4F6) else Color(0xFF1E1B4B)
    val secondaryText = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val inputBg = if (isDark) Color(0xFF1E212A) else Color(0xFFF8FAFC)
    val inputBorder = if (isDark) Color(0xFF33384A) else Color(0xFFE2E8F0)
    val cardBg = if (isDark) Color(0xFF161820) else Color.White
    val cardBorder = if (isDark) Color(0xFF282C3D) else Color(0xFFE5E7EB)
    val pillBg = if (isDark) Color(0xFF212532) else Color(0xFFF1F5F9)

    val backgroundGradient = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F0B18),
                    Color(0xFF161026),
                    Color(0xFF0B0D13)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFBFBFE),
                    Color(0xFFF5F3FF),
                    Color(0xFFEEF2FF)
                )
            )
        }
    }

    // Web Client ID
    val webClientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        "630014392541-a6agtiugpiglok42hpqbebrjvdo0j7ik.apps.googleusercontent.com"
    }

    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember(gso) { GoogleSignIn.getClient(context, gso) }

    var isRegisterMode by remember { mutableStateOf(false) } // False: Sign In, True: Sign Up
    var displayNameInput by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    // Official Firebase Email Verification State
    var showEmailVerificationDialog by remember { mutableStateOf(false) }
    var verificationLoading by remember { mutableStateOf(false) }
    var verificationErrorMsg by remember { mutableStateOf<String?>(null) }
    var verificationSuccessMsg by remember { mutableStateOf<String?>(null) }
    var pendingVerificationEmail by remember { mutableStateOf("") }
    var pendingVerificationName by remember { mutableStateOf("") }
    var pendingVerificationRole by remember { mutableStateOf<UserRole?>(null) }

    // Google Sign-In Activity Launcher with Whitelist Verification
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                val gEmail = account?.email?.lowercase()?.trim() ?: ""

                if (idToken != null && gEmail.isNotBlank()) {
                    isGoogleLoading = true
                    errorMsg = null

                    scope.launch {
                        try {
                            // 1. Authenticate with Firebase first using Google Credential (enables isAuth())
                            val credential = GoogleAuthProvider.getCredential(idToken, null)
                            val authResult = auth.signInWithCredential(credential).await()
                            val currentUser = authResult.user

                            if (currentUser == null) {
                                isGoogleLoading = false
                                errorMsg = "Google authentication failed. Please try again."
                                return@launch
                            }

                            // 2. Verify invitation whitelist
                            val invitation = accessRepo.checkInvitation(gEmail)
                            if (invitation == null) {
                                // Unauthorized user -> sign out immediately
                                auth.signOut()
                                isGoogleLoading = false
                                errorMsg = "Access Denied: '$gEmail' is not pre-authorized by an Admin or Owner. Please request an invitation from store management."
                                return@launch
                            }

                            // 3. Authorized -> Update user profile & provision user document
                            val gName = account.displayName ?: invitation.name
                            val gPhoto = account.photoUrl
                            if (!gName.isNullOrBlank() || gPhoto != null) {
                                val updates = UserProfileChangeRequest.Builder()
                                if (!gName.isNullOrBlank()) updates.setDisplayName(gName)
                                if (gPhoto != null) updates.setPhotoUri(gPhoto)
                                currentUser.updateProfile(updates.build()).await()
                            }

                            accessRepo.provisionUserDocument(
                                uid = currentUser.uid,
                                email = gEmail,
                                displayName = gName,
                                authProvider = "google.com"
                            )

                            isGoogleLoading = false
                            Toast.makeText(
                                context,
                                "Welcome, $gName (${invitation.role.displayName})!",
                                Toast.LENGTH_SHORT
                            ).show()
                            onAuthSuccess()
                        } catch (e: Exception) {
                            isGoogleLoading = false
                            errorMsg = "Google sign-in error: ${e.localizedMessage ?: "Authentication failed"}"
                        }
                    }
                } else {
                    isGoogleLoading = false
                    errorMsg = "Unable to retrieve Google account credentials. Please try again."
                }
            } catch (e: Throwable) {
                isGoogleLoading = false
                if (e is ApiException) {
                    errorMsg = when (e.statusCode) {
                        12501 -> "Google sign-in was cancelled."
                        12500 -> "Google Play services error (12500)."
                        10 -> "Developer error (10). SHA-1 fingerprint required in Firebase."
                        7 -> "Network error. Please check your internet connection."
                        else -> "Google Sign-In failed (${e.statusCode}): ${e.localizedMessage ?: "Unknown error"}"
                    }
                } else {
                    errorMsg = "Google sign-in failed: ${e.localizedMessage ?: "Unknown error"}"
                }
            }
        } else {
            isGoogleLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imeNestedScroll()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ─────────────────────────────────────────────
            // 1. BRAND HEADER (ELEGANT HERO BANNER)
            // ─────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF1E1036) else Color(0xFF34005F),
                shadowElevation = if (isDark) 6.dp else 4.dp,
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color(0xFF45247B) else Color(0x2234005F)
                )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mary_spares_brand_header),
                    contentDescription = "Mary Two Wheeler Spares",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                )
            }

            // Subtle Status Badge
            Row(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 14.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF22163B) else Color(0xFFEDE7F6))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Text(
                    text = "Inventory Management System",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDark) Color(0xFFDDD6FE) else Color(0xFF5B21B6),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp
                    )
                )
            }

            // ─────────────────────────────────────────────
            // 2. MAIN CARD
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 4.dp else 2.dp),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 22.dp)
                ) {
                    // Segmented Tab Switcher (Sign In vs Sign Up)
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        SegmentedButton(
                            selected = !isRegisterMode,
                            onClick = {
                                isRegisterMode = false
                                errorMsg = null
                                successMsg = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = if (isDark) Color(0xFF2E2A48) else Color(0xFFEDE9FE),
                                activeContentColor = brandAccent,
                                inactiveContainerColor = Color.Transparent,
                                inactiveContentColor = secondaryText,
                                activeBorderColor = if (isDark) Color(0xFF4C3E75) else Color(0xFFDDD6FE),
                                inactiveBorderColor = inputBorder
                            ),
                            label = {
                                Text(
                                    text = "Sign In",
                                    fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.5.sp
                                )
                            }
                        )

                        SegmentedButton(
                            selected = isRegisterMode,
                            onClick = {
                                isRegisterMode = true
                                errorMsg = null
                                successMsg = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = if (isDark) Color(0xFF2E2A48) else Color(0xFFEDE9FE),
                                activeContentColor = brandAccent,
                                inactiveContainerColor = Color.Transparent,
                                inactiveContentColor = secondaryText,
                                activeBorderColor = if (isDark) Color(0xFF4C3E75) else Color(0xFFDDD6FE),
                                inactiveBorderColor = inputBorder
                            ),
                            label = {
                                Text(
                                    text = "Sign Up",
                                    fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.5.sp
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = if (isRegisterMode) "Sign Up for Shop Access" else "Welcome Back",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = primaryText
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isRegisterMode) "Pre-authorized store access required for account registration" else "Sign in to access your spare parts inventory",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp,
                            color = secondaryText
                        ),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // ==========================================
                    // CONTINUE WITH GOOGLE BUTTON
                    // ==========================================
                    Surface(
                        onClick = {
                            if (!isLoading && !isGoogleLoading) {
                                isGoogleLoading = true
                                errorMsg = null
                                try {
                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                } catch (e: Exception) {
                                    isGoogleLoading = false
                                    errorMsg = "Unable to start Google Sign-In: ${e.localizedMessage}"
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF222634) else Color(0xFFFFFFFF),
                        border = BorderStroke(
                            1.2.dp,
                            if (isDark) Color(0xFF3B435C) else Color(0xFFD1D5DB)
                        ),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isGoogleLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = brandAccent
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Connecting Google...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryText,
                                        fontSize = 14.sp
                                    )
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                    contentDescription = "Google Logo",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Continue with Google",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryText,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Divider: "or with email"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = inputBorder
                        )
                        Text(
                            text = "or with email",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = secondaryText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = inputBorder
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ==========================================
                    // REGISTRATION: DISPLAY NAME FIELD
                    // ==========================================
                    AnimatedVisibility(
                        visible = isRegisterMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column {
                            OutlinedTextField(
                                value = displayNameInput,
                                onValueChange = {
                                    displayNameInput = it
                                    errorMsg = null
                                },
                                label = { Text("Your Full Name (Optional)") },
                                placeholder = { Text("e.g. Counter Staff / Alex") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Name",
                                        tint = if (isDark) Color(0xFF8B92A5) else Color(0xFF6B7280)
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = primaryText,
                                    unfocusedTextColor = primaryText,
                                    focusedBorderColor = brandAccent,
                                    unfocusedBorderColor = inputBorder,
                                    focusedContainerColor = inputBg,
                                    unfocusedContainerColor = inputBg,
                                    cursorColor = brandAccent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewRequester(nameRequester)
                                    .onFocusChanged {
                                        if (it.isFocused) {
                                            scope.launch {
                                                kotlinx.coroutines.delay(100)
                                                nameRequester.bringIntoView()
                                            }
                                        }
                                    }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // ==========================================
                    // EMAIL FIELD
                    // ==========================================
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMsg = null
                        },
                        label = { Text("Email Address") },
                        placeholder = { Text("your.email@example.com") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = if (isDark) Color(0xFF8B92A5) else Color(0xFF6B7280)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = primaryText,
                            unfocusedTextColor = primaryText,
                            focusedBorderColor = brandAccent,
                            unfocusedBorderColor = inputBorder,
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            cursorColor = brandAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(emailRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    scope.launch {
                                        kotlinx.coroutines.delay(100)
                                        emailRequester.bringIntoView()
                                    }
                                }
                            }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ==========================================
                    // PASSWORD FIELD
                    // ==========================================
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMsg = null
                        },
                        label = { Text("Password") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password",
                                tint = if (isDark) Color(0xFF8B92A5) else Color(0xFF6B7280)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = secondaryText
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            onDone = { focusManager.clearFocus() }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = primaryText,
                            unfocusedTextColor = primaryText,
                            focusedBorderColor = brandAccent,
                            unfocusedBorderColor = inputBorder,
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            cursorColor = brandAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(passwordRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    scope.launch {
                                        kotlinx.coroutines.delay(100)
                                        passwordRequester.bringIntoView()
                                    }
                                }
                            }
                    )

                    // ==========================================
                    // CONFIRM PASSWORD FIELD (REGISTRATION ONLY)
                    // ==========================================
                    AnimatedVisibility(
                        visible = isRegisterMode,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    errorMsg = null
                                },
                                label = { Text("Confirm Password") },
                                placeholder = { Text("••••••••") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Confirm Password",
                                        tint = if (isDark) Color(0xFF8B92A5) else Color(0xFF6B7280)
                                    )
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = primaryText,
                                    unfocusedTextColor = primaryText,
                                    focusedBorderColor = brandAccent,
                                    unfocusedBorderColor = inputBorder,
                                    focusedContainerColor = inputBg,
                                    unfocusedContainerColor = inputBg,
                                    cursorColor = brandAccent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewRequester(confirmPasswordRequester)
                                    .onFocusChanged {
                                        if (it.isFocused) {
                                            scope.launch {
                                                kotlinx.coroutines.delay(100)
                                                confirmPasswordRequester.bringIntoView()
                                            }
                                        }
                                    }
                            )
                        }
                    }

                    // Forgot Password link (Sign In mode only)
                    if (!isRegisterMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showForgotPasswordDialog = true },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Forgot password?",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = brandAccent,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Error & Success Banners
                    AnimatedVisibility(visible = errorMsg != null || successMsg != null) {
                        errorMsg?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF38151D) else Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF5A202D) else Color(0xFFFECACA)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }

                        successMsg?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF047857) else Color(0xFF86EFAC)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ==========================================
                    // PRIMARY SUBMIT BUTTON
                    // ==========================================
                    val isFormValid = email.isNotBlank() && password.isNotBlank() && (!isRegisterMode || confirmPassword.isNotBlank())
                    val buttonEnabled = !isLoading && !isGoogleLoading && isFormValid

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val cleanEmail = email.lowercase().trim()

                            if (isRegisterMode) {
                                if (password != confirmPassword) {
                                    errorMsg = "Passwords do not match."
                                    return@Button
                                }
                                if (password.length < 6) {
                                    errorMsg = "Password must be at least 6 characters."
                                    return@Button
                                }

                                isLoading = true
                                errorMsg = null

                                scope.launch {
                                    try {
                                        // 1. Verify invitation whitelist
                                        val invitation = accessRepo.checkInvitation(cleanEmail)
                                        if (invitation == null) {
                                            isLoading = false
                                            errorMsg = "Access Denied: '$cleanEmail' is not pre-authorized by an Admin or Owner. Please request an invitation from management."
                                            return@launch
                                        }

                                        // 2. Create account in Firebase Auth
                                        val authResult = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                                        val newUser = authResult.user

                                        if (newUser != null) {
                                            val assignedName = displayNameInput.trim().ifBlank { invitation.name }
                                            newUser.updateProfile(
                                                UserProfileChangeRequest.Builder()
                                                    .setDisplayName(assignedName)
                                                    .build()
                                            ).await()

                                            // 3. Send official Firebase verification email directly to inbox
                                            newUser.sendEmailVerification().await()

                                            isLoading = false
                                            pendingVerificationEmail = cleanEmail
                                            pendingVerificationName = assignedName
                                            pendingVerificationRole = invitation.role
                                            verificationErrorMsg = null
                                            verificationSuccessMsg = "Verification email sent to $cleanEmail."
                                            showEmailVerificationDialog = true
                                        } else {
                                            isLoading = false
                                            errorMsg = "Account creation failed. Please try again."
                                        }
                                    } catch (e: Exception) {
                                        isLoading = false
                                        errorMsg = e.localizedMessage ?: "Sign up failed."
                                    }
                                }
                            } else {
                                // Sign In Flow
                                isLoading = true
                                errorMsg = null

                                scope.launch {
                                    try {
                                        // 1. Authenticate with Firebase Auth
                                        val authResult = auth.signInWithEmailAndPassword(cleanEmail, password).await()
                                        val currentUser = authResult.user

                                        if (currentUser == null) {
                                            isLoading = false
                                            errorMsg = "Authentication failed."
                                            return@launch
                                        }

                                        // 2. Check invitation whitelist
                                        val invitation = accessRepo.checkInvitation(cleanEmail)
                                        if (invitation == null) {
                                            auth.signOut()
                                            isLoading = false
                                            errorMsg = "Access Denied: '$cleanEmail' is not authorized to access Mary Spares."
                                            return@launch
                                        }

                                        // 3. Check if email is verified (allow root Admin or verified accounts)
                                        val isRoot = cleanEmail == "jinsu.j2005@gmail.com"
                                        if (!currentUser.isEmailVerified && !isRoot) {
                                            pendingVerificationEmail = cleanEmail
                                            pendingVerificationName = currentUser.displayName ?: invitation.name
                                            pendingVerificationRole = invitation.role
                                            verificationErrorMsg = "Please verify your email before signing in."
                                            verificationSuccessMsg = null
                                            isLoading = false
                                            showEmailVerificationDialog = true
                                            return@launch
                                        }

                                        // 4. Provision User Document
                                        accessRepo.provisionUserDocument(
                                            uid = currentUser.uid,
                                            email = cleanEmail,
                                            displayName = currentUser.displayName ?: invitation.name,
                                            authProvider = "password"
                                        )
                                        isLoading = false
                                        onAuthSuccess()
                                    } catch (e: Exception) {
                                        isLoading = false
                                        errorMsg = e.localizedMessage ?: "Login failed. Check email and password."
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF7C3AED) else BrandPurple,
                            contentColor = Color.White,
                            disabledContainerColor = if (isDark) Color(0xFF242836) else Color(0xFFE2E8F0),
                            disabledContentColor = if (isDark) Color(0xFF6B7280) else Color(0xFF94A3B8)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (buttonEnabled) 3.dp else 0.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = buttonEnabled
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(
                            text = if (isRegisterMode) "Sign Up" else "Sign In to Mary Spares",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 3. SECURITY FOOTER
            // ─────────────────────────────────────────────
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Secure",
                    tint = if (isDark) Color(0xFF6D7488) else Color(0xFF9CA3AF),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "End-to-End Encrypted Cloud Database",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = secondaryText,
                        fontSize = 11.5.sp
                    )
                )
            }
        }
    }

    // ─────────────────────────────────────────────
    // 4. OFFICIAL FIREBASE EMAIL VERIFICATION DIALOG
    // ─────────────────────────────────────────────
    if (showEmailVerificationDialog) {
        AlertDialog(
            onDismissRequest = { /* keep open until confirmed or cancelled */ },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = brandAccent,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Verify Your Email",
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "A verification email has been sent by Firebase to:",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = secondaryText
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = pillBg,
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = pendingVerificationEmail,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = brandAccent
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = "1. Open your email inbox (and check Spam/Junk folder).\n2. Click the verification link in the email from Firebase.\n3. Return here and tap 'I've Verified My Email'.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = secondaryText,
                            lineHeight = 18.sp
                        )
                    )

                    verificationSuccessMsg?.let {
                        Text(
                            text = it,
                            color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        )
                    }

                    verificationErrorMsg?.let {
                        Text(
                            text = it,
                            color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        verificationLoading = true
                        verificationErrorMsg = null
                        verificationSuccessMsg = null

                        scope.launch {
                            val currentUser = auth.currentUser
                            if (currentUser != null) {
                                try {
                                    currentUser.reload().await()
                                    if (currentUser.isEmailVerified) {
                                        // Provision User Document in Firestore
                                        accessRepo.provisionUserDocument(
                                            uid = currentUser.uid,
                                            email = pendingVerificationEmail,
                                            displayName = pendingVerificationName,
                                            authProvider = "password"
                                        )
                                        verificationLoading = false
                                        showEmailVerificationDialog = false
                                        Toast.makeText(
                                            context,
                                            "Email verified successfully! Welcome to Mary Spares.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onAuthSuccess()
                                    } else {
                                        verificationLoading = false
                                        verificationErrorMsg = "Email not yet verified. Please click the link in your email and tap this button again."
                                    }
                                } catch (e: Exception) {
                                    verificationLoading = false
                                    verificationErrorMsg = "Verification check error: ${e.localizedMessage}"
                                }
                            } else {
                                verificationLoading = false
                                verificationErrorMsg = "Session expired. Please sign in again."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF6D28D9) else BrandPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !verificationLoading
                ) {
                    if (verificationLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("I've Verified My Email", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val currentUser = auth.currentUser
                            if (currentUser != null) {
                                scope.launch {
                                    try {
                                        currentUser.sendEmailVerification().await()
                                        verificationSuccessMsg = "New verification email dispatched! Please check your inbox."
                                        verificationErrorMsg = null
                                    } catch (e: Exception) {
                                        verificationErrorMsg = "Failed to resend: ${e.localizedMessage}"
                                    }
                                }
                            }
                        },
                        enabled = !verificationLoading
                    ) {
                        Text("Resend Email", color = brandAccent, fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(
                        onClick = {
                            showEmailVerificationDialog = false
                            auth.signOut()
                        },
                        enabled = !verificationLoading
                    ) {
                        Text("Cancel", color = secondaryText)
                    }
                }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // ─────────────────────────────────────────────
    // 5. FORGOT PASSWORD DIALOG
    // ─────────────────────────────────────────────
    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(email.trim()) }
        var resetLoading by remember { mutableStateOf(false) }
        var resetError by remember { mutableStateOf<String?>(null) }
        val resetEmailRequester = remember { BringIntoViewRequester() }

        AlertDialog(
            onDismissRequest = { if (!resetLoading) showForgotPasswordDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = brandAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Reset Password",
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter your registered email address. Firebase will send a secure password reset link to your inbox.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            color = secondaryText
                        )
                    )

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it; resetError = null },
                        label = { Text("Email Address") },
                        placeholder = { Text("staff@example.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = primaryText,
                            unfocusedTextColor = primaryText,
                            focusedBorderColor = brandAccent,
                            unfocusedBorderColor = inputBorder,
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            cursorColor = brandAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(resetEmailRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    scope.launch {
                                        kotlinx.coroutines.delay(100)
                                        resetEmailRequester.bringIntoView()
                                    }
                                }
                            }
                    )

                    resetError?.let {
                        Text(
                            text = it,
                            color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanResetEmail = resetEmail.lowercase().trim()
                        if (cleanResetEmail.isBlank() || !cleanResetEmail.contains("@")) {
                            resetError = "Please enter a valid email address."
                            return@Button
                        }
                        resetLoading = true
                        resetError = null

                        scope.launch {
                            try {
                                // 1. Verify invitation whitelist
                                val invitation = accessRepo.checkInvitation(cleanResetEmail)
                                if (invitation == null) {
                                    resetLoading = false
                                    resetError = "Email '$cleanResetEmail' is not a registered shop member. Please contact Admin/Owner."
                                    return@launch
                                }

                                // 2. Send official Firebase password reset email
                                auth.sendPasswordResetEmail(cleanResetEmail).await()
                                resetLoading = false
                                showForgotPasswordDialog = false
                                successMsg = "Password reset email sent to $cleanResetEmail. Check your inbox & spam folder."
                                Toast.makeText(context, "Password reset link sent to $cleanResetEmail", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                resetLoading = false
                                resetError = e.localizedMessage ?: "Failed to send reset email."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF6D28D9) else BrandPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !resetLoading && resetEmail.isNotBlank()
                ) {
                    if (resetLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Send Reset Link", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgotPasswordDialog = false },
                    enabled = !resetLoading
                ) {
                    Text("Cancel", color = secondaryText, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(22.dp)
        )
    }
}
