package com.visiontrack.presentation.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visiontrack.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) { viewModel.loadProfile(userId) }

    var showSignOutDialog by remember { mutableStateOf(false) }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text  = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    viewModel.signOut()
                    onSignOut()
                }) { Text("Sign Out", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            },
            containerColor = Color(0xFF12172A)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0E1A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0A0E1A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0D47A1), Color(0xFF0A0E1A))))
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1565C0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            uiState.user?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(uiState.user?.displayName ?: "User", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(uiState.user?.email ?: "", color = Color.White.copy(0.6f), fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    // Plan badge
                    Surface(
                        color = Color(0xFFFFB74D).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            uiState.user?.planType?.name ?: "FREE",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = Color(0xFFFFB74D),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileStatCard("Detections", "${uiState.user?.totalDetections ?: 0}", Color(0xFF4FC3F7), Modifier.weight(1f))
                ProfileStatCard("Sessions",   "${uiState.user?.totalSessions ?: 0}",   Color(0xFFFFB74D), Modifier.weight(1f))
                ProfileStatCard("Classes",    "80",                                     Color(0xFF81C784), Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Settings list
            ProfileSection("Account") {
                ProfileRow(Icons.Default.Edit,           "Edit Profile",         Color(0xFF4FC3F7)) {}
                ProfileRow(Icons.Default.Lock,           "Change Password",      Color(0xFF4FC3F7)) {}
                ProfileRow(Icons.Default.Notifications,  "Notifications",        Color(0xFF4FC3F7)) {}
            }

            Spacer(Modifier.height(12.dp))

            ProfileSection("Data") {
                ProfileRow(Icons.Default.CloudSync,      "Sync Now",             Color(0xFF80CBC4)) {}
                ProfileRow(Icons.Default.FileDownload,   "Export All Data",      Color(0xFF80CBC4)) {}
                ProfileRow(Icons.Default.DeleteForever,  "Clear Local Cache",    Color(0xFFEF9A9A)) {}
            }

            Spacer(Modifier.height(12.dp))

            ProfileSection("About") {
                ProfileRow(Icons.Default.Info,           "App Version 1.0.0",    Color(0xFF9E9E9E)) {}
                ProfileRow(Icons.Default.BugReport,      "Report a Bug",         Color(0xFF9E9E9E)) {}
                ProfileRow(Icons.Default.Policy,         "Privacy Policy",       Color(0xFF9E9E9E)) {}
            }

            Spacer(Modifier.height(24.dp))

            // Sign out
            Button(
                onClick = { showSignOutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350).copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Logout, null, tint = Color(0xFFEF5350))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(76.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12172A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Color.White.copy(0.5f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(title, color = Color.White.copy(0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF12172A)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun ProfileRow(icon: ImageVector, label: String, iconColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = Color.White.copy(0.05f), modifier = Modifier.padding(horizontal = 16.dp))
}
