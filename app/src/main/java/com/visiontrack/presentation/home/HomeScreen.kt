package com.visiontrack.presentation.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetection: (String) -> Unit,
    onNavigateToHistory:   (String) -> Unit,
    onNavigateToAnalytics: (String) -> Unit,
    onNavigateToProfile:   (String) -> Unit,
    onNavigateToSettings:  () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("VisionTrack", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { uiState.userId?.let { onNavigateToProfile(it) } }) {
                        Icon(Icons.Default.AccountCircle, "Profile", tint = Color(0xFF4FC3F7), modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0E1A),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0A0E1A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Welcome banner
            WelcomeBanner(
                displayName = uiState.displayName,
                totalDetections = uiState.totalDetections,
                totalSessions   = uiState.totalSessions
            )

            // Quick-start detection card
            Spacer(Modifier.height(24.dp))
            StartDetectionCard(
                onClick = { uiState.userId?.let { onNavigateToDetection(it) } }
            )

            // Stats row
            Spacer(Modifier.height(24.dp))
            Text("Overview", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.statsCards) { card ->
                    StatCard(card)
                }
            }

            // Navigation grid
            Spacer(Modifier.height(28.dp))
            Text("Features", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(12.dp))
            NavigationGrid(
                userId = uiState.userId ?: "",
                onHistory   = onNavigateToHistory,
                onAnalytics = onNavigateToAnalytics
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WelcomeBanner(displayName: String, totalDetections: Int, totalSessions: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1565C0)))
            )
            .padding(24.dp)
    ) {
        Column {
            Text("Welcome back,", color = Color.White.copy(0.8f), fontSize = 14.sp)
            Text(displayName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column {
                    Text("$totalDetections", color = Color(0xFF4FC3F7), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Total Detections", color = Color.White.copy(0.7f), fontSize = 12.sp)
                }
                Column {
                    Text("$totalSessions", color = Color(0xFFFFB74D), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Sessions", color = Color.White.copy(0.7f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StartDetectionCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12172A)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Start Detection", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Tap to launch live AI camera", color = Color.White.copy(0.6f), fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4FC3F7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF0A0E1A), modifier = Modifier.size(32.dp))
            }
        }
    }
}

data class StatCardData(val title: String, val value: String, val icon: ImageVector, val color: Color)

@Composable
private fun StatCard(data: StatCardData) {
    Card(
        modifier = Modifier.width(140.dp).height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12172A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(data.icon, null, tint = data.color, modifier = Modifier.size(24.dp))
            Column {
                Text(data.value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text(data.title, color = Color.White.copy(0.6f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun NavigationGrid(userId: String, onHistory: (String) -> Unit, onAnalytics: (String) -> Unit) {
    val items = listOf(
        Triple("History",   Icons.Default.History,       Color(0xFF4FC3F7)) to { onHistory(userId) },
        Triple("Analytics", Icons.Default.BarChart,      Color(0xFFFFB74D)) to { onAnalytics(userId) },
        Triple("Zones",     Icons.Default.CropFree,      Color(0xFF80CBC4)) to { },
        Triple("Export",    Icons.Default.FileDownload,  Color(0xFF81C784)) to { }
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { (info, action) ->
            Card(
                onClick = { action() },
                modifier = Modifier.weight(1f).height(88.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12172A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(info.second, null, tint = info.third, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(info.first, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
