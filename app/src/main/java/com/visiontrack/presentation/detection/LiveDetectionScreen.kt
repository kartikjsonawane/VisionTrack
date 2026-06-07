package com.visiontrack.presentation.detection

import android.Manifest
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.visiontrack.domain.model.DetectedObject
import com.visiontrack.ml.BoundingBoxOverlay

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LiveDetectionScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    viewModel: DetectionViewModel = hiltViewModel()
) {
    val context       = LocalContext.current
    val lifecycleOwner= LocalLifecycleOwner.current
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    var overlayRef: BoundingBoxOverlay? by remember { mutableStateOf(null) }
    var cameraManagerRef: CameraManager? by remember { mutableStateOf(null) }

    LaunchedEffect(userId) { viewModel.initialize(userId) }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    // Alert snackbars
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.alerts.collect { alert ->
            snackbarHostState.showSnackbar(alert.message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            if (cameraPermission.status.isGranted) {
                // ── Camera Preview ─────────────────────────────────────
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val container = LinearLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            orientation = LinearLayout.VERTICAL
                        }

                        val previewView = PreviewView(ctx).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
                            )
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }

                        val overlay = BoundingBoxOverlay(ctx).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
                            )
                        }
                        overlayRef = overlay

                        container.addView(previewView)
                        container.addView(overlay)

                        val manager = CameraManager()
                        cameraManagerRef = manager

                        val analyzer = DetectionAnalyzer(
                            helper    = viewModel.helper,   // use the Hilt-injected singleton
                            sessionId = uiState.currentSession?.id ?: "",
                            onResults = { results ->
                                viewModel.onFrameDetected(results)
                                // Update overlay on main thread
                                overlay.post {
                                    val state = viewModel.uiState.value
                                    overlay.setDetections(results, previewView.width, previewView.height)
                                    overlay.setFps(state.fps, state.latencyMs)
                                }
                            }
                        )
                        manager.startCamera(ctx, lifecycleOwner, previewView, analyzer)
                        container
                    }
                )

                // ── Top HUD ────────────────────────────────────────────
                DetectionHud(
                    uiState    = uiState,
                    onBack     = onNavigateBack,
                    modifier   = Modifier.align(Alignment.TopCenter)
                )

                // ── Bottom Controls ─────────────────────────────────────
                DetectionControls(
                    uiState         = uiState,
                    onStartStop     = {
                        if (uiState.isDetecting) viewModel.stopDetection()
                        else viewModel.startDetection()
                    },
                    onConfidenceChange = viewModel::setConfidenceThreshold,
                    modifier        = Modifier.align(Alignment.BottomCenter)
                )

            } else {
                PermissionDeniedContent(
                    onRequest = { cameraPermission.launchPermissionRequest() }
                )
            }

            // Error snackbar
            uiState.errorMessage?.let { msg ->
                LaunchedEffect(msg) {
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearError()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraManagerRef?.shutdown()
            if (uiState.isDetecting) viewModel.stopDetection()
        }
    }
}

@Composable
private fun DetectionHud(
    uiState: DetectionUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        // Stats pill
        if (uiState.isDetecting) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatChip(label = "FPS",      value = "%.0f".format(uiState.fps))
                    StatChip(label = "Latency",  value = "${uiState.latencyMs.toInt()}ms")
                    StatChip(label = "Objects",  value = "${uiState.currentObjects.size}")
                }
            }
        }

        // Recording indicator
        if (uiState.isDetecting) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}

@Composable
private fun DetectionControls(
    uiState: DetectionUiState,
    onStartStop: () -> Unit,
    onConfidenceChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Confidence slider
        AnimatedVisibility(showSettings) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Confidence: ${(uiState.confidenceThreshold * 100).toInt()}%",
                        color = Color.White, fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value    = uiState.confidenceThreshold,
                        onValueChange = onConfidenceChange,
                        valueRange = 0.1f..0.9f,
                        steps  = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Session total
        if (uiState.isDetecting) {
            Text(
                "Session detections: ${uiState.totalDetectionsInSession}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Settings toggle
            IconButton(
                onClick = { showSettings = !showSettings },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .size(52.dp)
            ) {
                Icon(
                    if (showSettings) Icons.Default.ExpandLess else Icons.Default.Tune,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }

            // Start / Stop main button
            FloatingActionButton(
                onClick    = onStartStop,
                containerColor = if (uiState.isDetecting) Color.Red else MaterialTheme.colorScheme.primary,
                modifier   = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isDetecting) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isDetecting) "Stop" else "Start",
                    modifier = Modifier.size(36.dp)
                )
            }

            // Placeholder for screenshot
            IconButton(
                onClick = { /* TODO: capture frame */ },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .size(52.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Capture", tint = Color.White)
            }
        }
    }
}

@Composable
private fun PermissionDeniedContent(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.White)
        Spacer(Modifier.height(16.dp))
        Text("Camera permission required", color = Color.White, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRequest) { Text("Grant Permission") }
    }
}
