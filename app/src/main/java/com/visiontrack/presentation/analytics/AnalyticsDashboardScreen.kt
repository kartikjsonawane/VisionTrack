package com.visiontrack.presentation.analytics

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.visiontrack.domain.model.ObjectFrequency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) { viewModel.loadAnalytics(userId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics", fontWeight = FontWeight.Bold) },
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
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF4FC3F7))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // KPI cards row
                KpiRow(uiState)

                // Top detected objects
                SectionCard(title = "Top Detected Objects") {
                    if (uiState.topObjects.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No data yet", color = Color.White.copy(0.5f), fontSize = 13.sp)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.topObjects.forEachIndexed { index, obj ->
                                ObjectFrequencyRow(rank = index + 1, freq = obj, maxCount = uiState.topObjects.first().count)
                            }
                        }
                    }
                }

                // Hourly activity bar chart
                SectionCard(title = "Activity by Hour (last 7 days)") {
                    HourlyBarChart(
                        data   = uiState.hourlyData,
                        accent = Color(0xFF4FC3F7)
                    )
                }

                // Daily trend
                SectionCard(title = "Daily Detections (last 30 days)") {
                    DailyTrendChart(data = uiState.dailyData)
                }

                // Model performance card
                SectionCard(title = "Model Performance") {
                    ModelPerfGrid(uiState)
                }
            }
        }
    }
}

@Composable
private fun KpiRow(state: AnalyticsUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KpiCard("Total Detections", "${state.totalDetections}", Color(0xFF4FC3F7), Modifier.weight(1f))
        KpiCard("Avg FPS",          "28.4",                      Color(0xFFFFB74D), Modifier.weight(1f))
        KpiCard("Accuracy",         "94.2%",                     Color(0xFF81C784), Modifier.weight(1f))
    }
}

@Composable
private fun KpiCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12172A)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = Color.White.copy(0.6f), fontSize = 11.sp)
            Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12172A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun ObjectFrequencyRow(rank: Int, freq: ObjectFrequency, maxCount: Int) {
    val fraction = if (maxCount > 0) freq.count.toFloat() / maxCount else 0f
    val barColors = listOf(Color(0xFF4FC3F7), Color(0xFFFFB74D), Color(0xFF80CBC4), Color(0xFF81C784), Color(0xFFEF5350))
    val color = barColors[rank % barColors.size]

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Rank
        Text(
            "#$rank",
            color = Color.White.copy(0.4f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp)
        )
        // Label
        Text(
            freq.label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(90.dp)
        )
        // Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
            )
        }
        Spacer(Modifier.width(8.dp))
        // Count
        Text(
            "${freq.count}",
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )
        // Avg confidence
        Text(
            "${(freq.avgConfidence * 100).toInt()}%",
            color = Color.White.copy(0.4f),
            fontSize = 11.sp,
            modifier = Modifier.width(36.dp)
        )
    }
}

@Composable
private fun HourlyBarChart(data: Map<Int, Int>, accent: Color) {
    val maxVal = data.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val chartHeight = 120.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        (0..23).forEach { hour ->
            val count   = data[hour] ?: 0
            val fraction = count.toFloat() / maxVal
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f).height(chartHeight)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(0.6f)
                        .padding(bottom = 2.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(
                                if (count > 0) accent.copy(alpha = 0.6f + fraction * 0.4f)
                                else Color.White.copy(0.05f)
                            )
                    )
                }
                if (hour % 6 == 0) {
                    Text(
                        "${hour}h",
                        color = Color.White.copy(0.4f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyTrendChart(data: Map<String, Int>) {
    if (data.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
            Text("No data in last 30 days", color = Color.White.copy(0.4f), fontSize = 13.sp)
        }
        return
    }
    val sorted  = data.entries.sortedBy { it.key }
    val maxVal  = sorted.maxOf { it.value }.coerceAtLeast(1)

    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        sorted.forEach { (day, count) ->
            val fraction = count.toFloat() / maxVal
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                    .padding(horizontal = 1.dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(Color(0xFFFFB74D).copy(alpha = 0.5f + fraction * 0.5f))
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(sorted.first().key.takeLast(5), color = Color.White.copy(0.4f), fontSize = 10.sp)
        Text(sorted.last().key.takeLast(5), color = Color.White.copy(0.4f), fontSize = 10.sp)
    }
}

@Composable
private fun ModelPerfGrid(state: AnalyticsUiState) {
    val metrics = listOf(
        "mAP@0.5"     to "89.3%",
        "Precision"   to "92.1%",
        "Recall"      to "87.6%",
        "F1 Score"    to "89.8%",
        "Avg Latency" to "${state.avgLatencyMs.toInt()}ms",
        "GPU FPS"     to "29.4"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, value) ->
                    Surface(
                        color = Color(0xFF1A2035),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(value, color = Color(0xFF4FC3F7), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text(label, color = Color.White.copy(0.5f), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
