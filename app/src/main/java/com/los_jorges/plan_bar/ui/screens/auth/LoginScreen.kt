package com.los_jorges.plan_bar.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.los_jorges.plan_bar.R
import com.los_jorges.plan_bar.model.Trabajador
import com.los_jorges.plan_bar.viewmodel.AuthErrorMessages
import com.los_jorges.plan_bar.viewmodel.AuthState
import com.los_jorges.plan_bar.viewmodel.AuthViewModel
import com.los_jorges.plan_bar.ui.theme.*
import com.los_jorges.plan_bar.ui.theme.LocalStrings

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (Trabajador) -> Unit,
    onGoToRegister: () -> Unit,
    onGoToAjustes: () -> Unit = {}
) {
    val s = LocalStrings.current
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Icono de ajustes (esquina superior derecha) ────────────────
        IconButton(
            onClick = onGoToAjustes,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = s.ajustes,
                tint = WarmMuted,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(1f))

            // ── Branding ──────────────────────────────────────────────────
            Image(
                painter = painterResource(R.drawable.planbar_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Plan Bar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = s.loginSubtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(40.dp))

            // ── Formulario ────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
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
                        label = { Text(s.emailLabel) },
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
                        label = { Text(s.contrasena) },
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
                        onClick = { viewModel.login(email, password, AuthErrorMessages(
                            rellenaTodosLosCampos = s.rellenaTodosLosCampos,
                            soloAdministradores = s.soloAdministradores,
                            emailOContrasenaIncorrectos = s.emailOContrasenaIncorrectos,
                            errorDeConexionRevisa = s.errorDeConexionRevisa
                        )) },
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
                                s.entrar,
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
                    s.sinCuentaRegistra,
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmMuted
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

