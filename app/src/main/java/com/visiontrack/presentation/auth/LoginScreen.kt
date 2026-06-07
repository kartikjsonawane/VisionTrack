package com.visiontrack.presentation.auth

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focus   = LocalFocusManager.current

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0E1A), Color(0xFF12172A))
                )
            )
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Logo
            Icon(
                Icons.Default.Visibility, null,
                tint = Color(0xFF4FC3F7),
                modifier = Modifier.size(64.dp)
            )
            Text("VisionTrack", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("Sign in to continue", color = Color.White.copy(0.6f), fontSize = 14.sp)

            Spacer(Modifier.height(8.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor  = Color(0xFF4FC3F7),
                    unfocusedBorderColor= Color.White.copy(0.3f),
                    focusedLabelColor   = Color(0xFF4FC3F7),
                    cursorColor         = Color(0xFF4FC3F7),
                    focusedTextColor    = Color.White,
                    unfocusedTextColor  = Color.White
                )
            )

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { showPass = !showPass }) {
                        Icon(
                            if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null,
                            tint = Color.White.copy(0.6f)
                        )
                    }
                },
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focus.clearFocus()
                    viewModel.signIn(email, password, onLoginSuccess)
                }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor  = Color(0xFF4FC3F7),
                    unfocusedBorderColor= Color.White.copy(0.3f),
                    focusedLabelColor   = Color(0xFF4FC3F7),
                    cursorColor         = Color(0xFF4FC3F7),
                    focusedTextColor    = Color.White,
                    unfocusedTextColor  = Color.White
                )
            )

            // Error
            AnimatedVisibility(uiState.errorMessage != null) {
                uiState.errorMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            msg,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Sign-in button
            Button(
                onClick = { viewModel.signIn(email, password, onLoginSuccess) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = email.isNotBlank() && password.isNotBlank() && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7))
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Sign In", fontWeight = FontWeight.Bold, color = Color(0xFF0A0E1A))
                }
            }

            // Register CTA
            TextButton(onClick = onNavigateToRegister) {
                Text("Don't have an account? ", color = Color.White.copy(0.6f))
                Text("Sign Up", color = Color(0xFF4FC3F7), fontWeight = FontWeight.Bold)
            }
        }
    }
}
