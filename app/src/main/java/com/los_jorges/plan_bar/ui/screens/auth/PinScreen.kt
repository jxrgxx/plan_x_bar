package com.los_jorges.plan_bar.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.viewmodel.AuthState
import com.los_jorges.plan_bar.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinScreen(
    trabajadorId: Int,
    trabajadorNombre: String,
    viewModel: AuthViewModel,
    onPinCorrecto: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var pin by remember { mutableStateOf("") }
    val maxDigits = 6

    // Limpiar error al escribir
    LaunchedEffect(pin) {
        if (state is AuthState.Error) viewModel.resetState()
    }

    fun intentarVerificar() {
        if (pin.isNotBlank()) {
            viewModel.verificarPin(trabajadorId, pin, onSuccess = onPinCorrecto)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Introduce tu PIN") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetState()
                        SessionManager.desactivarPersonal()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Hola, $trabajadorNombre",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Introduce tu PIN para continuar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Puntos visuales del PIN
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                repeat(maxDigits) { i ->
                    val filled = i < pin.length
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (filled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(16.dp)
                    ) {}
                }
            }

            // Campo oculto para teclado numérico
            OutlinedTextField(
                value = pin,
                onValueChange = { v ->
                    val digits = v.filter { it.isDigit() }
                    if (digits.length <= maxDigits) pin = digits
                },
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { intentarVerificar() }),
                isError = state is AuthState.Error,
                supportingText = {
                    if (state is AuthState.Error) {
                        Text((state as AuthState.Error).mensaje)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // Teclado numérico visual
            PinNumpad(
                onDigit = { d ->
                    if (pin.length < maxDigits) pin += d
                },
                onBorrar = {
                    if (pin.isNotEmpty()) pin = pin.dropLast(1)
                }
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { intentarVerificar() },
                enabled = pin.isNotBlank() && state !is AuthState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (state is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Entrar", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun PinNumpad(onDigit: (String) -> Unit, onBorrar: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { label ->
                    if (label.isEmpty()) {
                        Spacer(Modifier.weight(1f))
                    } else if (label == "⌫") {
                        OutlinedButton(
                            onClick = onBorrar,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Icon(Icons.Default.Backspace, contentDescription = "Borrar")
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { onDigit(label) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}
