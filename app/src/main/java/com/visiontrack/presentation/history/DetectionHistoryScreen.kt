package com.visiontrack.presentation.history

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.visiontrack.domain.model.DetectionSession
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionHistoryScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) { viewModel.loadHistory(userId) }

    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(uiState.exportPath) {
        uiState.exportPath?.let {
            snackbarHost.showSnackbar("Exported to: $it")
            viewModel.clearExportPath()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Detection History", fontWeight = FontWeight.Bold) },
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
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF4FC3F7))
            }
        } else if (uiState.sessions.isEmpty()) {
            EmptyHistoryContent()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Summary header
                    HistorySummaryCard(
                        totalSessions = uiState.sessions.size,
                        totalDetections = uiState.sessions.sumOf { it.totalDetections }
                    )
                }
                items(uiState.sessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        onExport = { viewModel.exportSession(session.id) },
                        onDelete = { viewModel.deleteSession(session.id) },
                        onSync   = { viewModel.syncSession(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorySummaryCard(totalSessions: Int, totalDetections: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D47A1)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            SummaryMetric("Sessions", "$totalSessions", Color(0xFF4FC3F7))
            SummaryMetric("Detections", "$totalDetections", Color(0xFFFFB74D))
            SummaryMetric("Avg/Session",
                if (totalSessions > 0) "${totalDetections / totalSessions}" else "0",
                Color(0xFF80CBC4)
            )
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color.White.copy(0.7f), fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionCard(
    session: DetectionSession,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onSync:   () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault()) }

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12172A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        dateFormat.format(Date(session.startTimeMs)),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatDuration(session.durationMs),
                        color = Color.White.copy(0.6f),
                        fontSize = 12.sp
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sync indicator
                    Icon(
                        if (session.isSynced) Icons.Default.Cloud else Icons.Default.CloudOff,
                        null,
                        tint = if (session.isSynced) Color(0xFF4FC3F7) else Color.White.copy(0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    // Detection count badge
                    Surface(
                        color = Color(0xFF0D47A1),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "${session.totalDetections}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        tint = Color.White.copy(0.6f)
                    )
                }
            }

            // Expanded detail
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color.White.copy(0.1f))
                    Spacer(Modifier.height(12.dp))

                    // Labels row
                    if (session.uniqueLabels.isNotEmpty()) {
                        Text("Detected:", color = Color.White.copy(0.6f), fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(session.uniqueLabels)
                        Spacer(Modifier.height(12.dp))
                    }

                    // Performance row
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MetricPill("${session.avgFps.toInt()} FPS", Color(0xFF4FC3F7))
                        MetricPill("${session.avgLatencyMs.toInt()}ms latency", Color(0xFFFFB74D))
                    }

                    Spacer(Modifier.height(12.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onExport) {
                            Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Export CSV", fontSize = 12.sp)
                        }
                        if (!session.isSynced) {
                            TextButton(onClick = onSync) {
                                Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Sync", fontSize = 12.sp)
                            }
                        }
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowRow(labels: List<String>) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.take(8).forEach { label ->
            Surface(
                color = Color(0xFF1A2035),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = Color(0xFF4FC3F7),
                    fontSize = 11.sp
                )
            }
        }
        if (labels.size > 8) {
            Text("+${labels.size - 8} more", color = Color.White.copy(0.5f), fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}

@Composable
private fun MetricPill(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyHistoryContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.History, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(16.dp))
            Text("No sessions yet", color = Color.White.copy(0.5f), fontSize = 18.sp)
            Text("Start a detection session to see history", color = Color.White.copy(0.3f), fontSize = 13.sp)
        }
    }
}

private fun formatDuration(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}
