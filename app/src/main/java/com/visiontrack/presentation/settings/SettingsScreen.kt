package com.visiontrack.presentation.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Detection Settings
            SettingsSection("Detection") {
                // Confidence threshold
                SettingSliderRow(
                    icon  = Icons.Default.Tune,
                    label = "Confidence Threshold",
                    value = uiState.confidenceThreshold,
                    displayValue = "${(uiState.confidenceThreshold * 100).toInt()}%",
                    range = 0.1f..0.9f,
                    onValueChange = viewModel::setConfidenceThreshold
                )
                // IOU threshold
                SettingSliderRow(
                    icon  = Icons.Default.FilterAlt,
                    label = "IoU Threshold (NMS)",
                    value = uiState.iouThreshold,
                    displayValue = "${(uiState.iouThreshold * 100).toInt()}%",
                    range = 0.3f..0.8f,
                    onValueChange = viewModel::setIouThreshold
                )
                SettingToggleRow(
                    icon   = Icons.Default.Speed,
                    label  = "GPU Acceleration",
                    sub    = "Use GPU delegate for faster inference",
                    value  = uiState.gpuEnabled,
                    onChange = viewModel::setGpuEnabled
                )
                SettingToggleRow(
                    icon   = Icons.Default.TrackChanges,
                    label  = "Object Tracking",
                    sub    = "Assign persistent IDs across frames",
                    value  = uiState.trackingEnabled,
                    onChange = viewModel::setTrackingEnabled
                )
            }

            // Camera Settings
            SettingsSection("Camera") {
                SettingToggleRow(
                    icon   = Icons.Default.FlashOn,
                    label  = "Flash / Torch",
                    sub    = "Enable torch in low-light conditions",
                    value  = uiState.torchEnabled,
                    onChange = viewModel::setTorchEnabled
                )
                SettingDropdownRow(
                    icon      = Icons.Default.PhotoCamera,
                    label     = "Resolution",
                    selected  = uiState.resolution,
                    options   = listOf("640×480", "1280×720", "1920×1080"),
                    onSelect  = viewModel::setResolution
                )
            }

            // Alerts
            SettingsSection("Alerts & Zones") {
                SettingToggleRow(
                    icon   = Icons.Default.NotificationsActive,
                    label  = "Zone Alerts",
                    sub    = "Notify when objects enter detection zones",
                    value  = uiState.zoneAlertsEnabled,
                    onChange = viewModel::setZoneAlertsEnabled
                )
                SettingToggleRow(
                    icon   = Icons.Default.Vibration,
                    label  = "Haptic Feedback",
                    sub    = "Vibrate on detection alert",
                    value  = uiState.hapticEnabled,
                    onChange = viewModel::setHapticEnabled
                )
            }

            // Storage & Sync
            SettingsSection("Storage & Sync") {
                SettingToggleRow(
                    icon   = Icons.Default.CloudSync,
                    label  = "Auto Cloud Sync",
                    sub    = "Sync sessions to Firebase automatically",
                    value  = uiState.autoSync,
                    onChange = viewModel::setAutoSync
                )
                SettingToggleRow(
                    icon   = Icons.Default.SaveAlt,
                    label  = "Save Detection Frames",
                    sub    = "Save screenshots of detected frames",
                    value  = uiState.saveFrames,
                    onChange = viewModel::setSaveFrames
                )
            }

            // Model info card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12172A)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SmartToy, null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("YOLOv8n INT8 Quantized", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("640×640 input  •  80 classes  •  ~6.2MB", color = Color.White.copy(0.5f), fontSize = 12.sp)
                        Text("GPU: ${if (uiState.gpuEnabled) "Enabled" else "Disabled"}  •  Threads: 4", color = Color(0xFF4FC3F7), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title.uppercase(), color = Color.White.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF12172A)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingToggleRow(icon: ImageVector, label: String, sub: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!value) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 14.sp)
            Text(sub, color = Color.White.copy(0.45f), fontSize = 11.sp)
        }
        Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = Color.White,
                checkedTrackColor  = Color(0xFF4FC3F7),
                uncheckedTrackColor= Color.White.copy(0.2f)
            )
        )
    }
    HorizontalDivider(color = Color.White.copy(0.05f), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingSliderRow(
    icon: ImageVector, label: String, value: Float,
    displayValue: String, range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(displayValue, color = Color(0xFF4FC3F7), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.padding(horizontal = 36.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF4FC3F7),
                activeTrackColor = Color(0xFF4FC3F7),
                inactiveTrackColor = Color.White.copy(0.15f)
            )
        )
    }
    HorizontalDivider(color = Color.White.copy(0.05f), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingDropdownRow(icon: ImageVector, label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(selected, color = Color(0xFF4FC3F7), fontSize = 13.sp)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.ArrowDropDown, null, tint = Color.White.copy(0.5f))

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1A2035))
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, color = if (opt == selected) Color(0xFF4FC3F7) else Color.White) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}
