package com.los_jorges.plan_bar.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.los_jorges.plan_bar.model.Trabajador
import com.los_jorges.plan_bar.viewmodel.AuthState
import com.los_jorges.plan_bar.viewmodel.AuthViewModel
import com.los_jorges.plan_bar.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (Trabajador) -> Unit,
    onGoToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state is AuthState.Success) {
            onLoginSuccess((state as AuthState.Success).trabajador)
            viewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBase)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(1f))

            // ── Branding ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(DarkSurface2)
                    .border(1.dp, Platinum40.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WineBar,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = Platinum40
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Plan Bar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = WarmWhite,
                letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tu restaurante, bajo control",
                style = MaterialTheme.typography.bodySmall,
                color = WarmMuted
            )

            Spacer(Modifier.height(40.dp))

            // ── Formulario ────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface1,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email, null,
                                tint = WarmMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = premiumInputColors()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock, null,
                                tint = WarmMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = premiumInputColors()
                    )

                    if (state is AuthState.Error) {
                        Text(
                            text = (state as AuthState.Error).mensaje,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Button(
                        onClick = { viewModel.login(email, password) },
                        enabled = state !is AuthState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Platinum40,
                            contentColor = DarkBase,
                            disabledContainerColor = DarkSurface3
                        )
                    ) {
                        if (state is AuthState.Loading) {
                            CircularProgressIndicator(
                                color = Platinum40,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                "Entrar",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                letterSpacing = 0.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Registro ──────────────────────────────────────────────────
            TextButton(onClick = onGoToRegister) {
                Text(
                    "¿Sin cuenta? Registra tu restaurante",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmMuted
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

