package com.visiontrack.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.visiontrack.domain.model.AuthState
import com.visiontrack.presentation.auth.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: (String) -> Unit,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "logo_scale"
    )

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0A0E1A), Color(0xFF0D47A1), Color(0xFF0A0E1A))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = "VisionTrack Logo",
                tint = Color(0xFF4FC3F7),
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "VisionTrack",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Text(
                "Real-Time AI Detection",
                color = Color(0xFF4FC3F7),
                fontSize = 14.sp,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(48.dp))
            CircularProgressIndicator(
                color = Color(0xFF4FC3F7),
                strokeWidth = 2.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }

    LaunchedEffect(authState) {
        delay(1800)
        when (authState) {
            is AuthState.Authenticated -> onNavigateToHome((authState as AuthState.Authenticated).user.uid)
            is AuthState.Unauthenticated -> onNavigateToLogin()
            else -> Unit
        }
    }
}
