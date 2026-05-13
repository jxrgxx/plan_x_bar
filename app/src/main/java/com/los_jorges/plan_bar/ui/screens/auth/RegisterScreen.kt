package com.los_jorges.plan_bar.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.los_jorges.plan_bar.viewmodel.AuthState
import com.los_jorges.plan_bar.viewmodel.AuthViewModel
import com.los_jorges.plan_bar.ui.theme.*

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegistroExitoso: () -> Unit,
    onGoToLogin: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var adminNombre by remember { mutableStateOf("") }
    var adminEmail by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }

    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBase)
    ) {
        // ── Cabecera ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface1)
                .padding(start = 4.dp, top = 12.dp, end = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onGoToLogin) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, null,
                    tint = WarmGray
                )
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text(
                    "Registro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = WarmWhite
                )
                Text(
                    "Crea la cuenta de tu restaurante",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmMuted
                )
            }
        }

        // ── Formulario ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 24.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionLabel(icon = Icons.Default.Storefront, title = "Datos del restaurante")

            OutlinedTextField(
                value = nombre, onValueChange = { nombre = it },
                label = { Text("Nombre *") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Badge,
                        null,
                        tint = WarmMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email del restaurante *") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Email,
                        null,
                        tint = WarmMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
            )
            OutlinedTextField(
                value = direccion, onValueChange = { direccion = it },
                label = { Text("Dirección") },
                leadingIcon = {
                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        tint = WarmMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
            )
            OutlinedTextField(
                value = telefono, onValueChange = { telefono = it },
                label = { Text("Teléfono") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Phone,
                        null,
                        tint = WarmMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
            )

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = DarkSurface3)
            Spacer(Modifier.height(4.dp))

            SectionLabel(icon = Icons.Default.AdminPanelSettings, title = "Cuenta de administrador")

            OutlinedTextField(
                value = adminNombre, onValueChange = { adminNombre = it },
                label = { Text("Nombre del administrador *") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        null,
                        tint = WarmMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
            )
            OutlinedTextField(
                value = adminEmail, onValueChange = { adminEmail = it },
                label = { Text("Email *") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Email,
                        null,
                        tint = WarmMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
            )
            OutlinedTextField(
                value = adminPassword, onValueChange = { adminPassword = it },
                label = { Text("Contraseña *") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        null,
                        tint = WarmMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
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

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    viewModel.registrarRestaurante(
                        nombre, email, direccion, telefono,
                        adminNombre, adminEmail, adminPassword,
                        onSuccess = onRegistroExitoso
                    )
                },
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
                        "Registrar restaurante",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        letterSpacing = 0.sp
                    )
                }
            }

            TextButton(onClick = onGoToLogin) {
                Text(
                    "¿Ya tienes cuenta? Inicia sesión",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmMuted
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = Platinum40.copy(alpha = 0.8f), modifier = Modifier.size(17.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = WarmGray
        )
    }
}
