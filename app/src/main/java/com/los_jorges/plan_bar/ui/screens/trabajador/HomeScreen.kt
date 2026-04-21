package com.los_jorges.plan_bar.ui.screens.trabajador

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.viewmodel.AuthState
import com.los_jorges.plan_bar.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCerrarSesion: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val trabajador by SessionManager.trabajador.collectAsState()
    var showDialogCerrar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hola, ${trabajador?.nombre ?: ""}") },
                actions = {
                    IconButton(onClick = { showDialogCerrar = true }) {
                        Icon(Icons.Default.ExitToApp, "Cerrar sesión")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("Área de trabajo — próximamente", color = MaterialTheme.colorScheme.outline)
        }
    }

    if (showDialogCerrar) {
        DialogVerificarAdmin(
            authViewModel = authViewModel,
            onConfirm = {
                SessionManager.cerrarSesionTrabajador()
                showDialogCerrar = false
                onCerrarSesion()
            },
            onDismiss = { showDialogCerrar = false }
        )
    }
}

@Composable
fun DialogVerificarAdmin(
    authViewModel: AuthViewModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    val state by authViewModel.state.collectAsState()

    LaunchedEffect(Unit) { authViewModel.resetState() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Verificación de administrador") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Introduce la contraseña del administrador para cerrar sesión.",
                    style = MaterialTheme.typography.bodyMedium)

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña admin") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (state is AuthState.Error) {
                    Text(
                        text = (state as AuthState.Error).mensaje,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { authViewModel.verificarAdmin(password, onConfirm) },
                enabled = state !is AuthState.Loading
            ) {
                if (state is AuthState.Loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text("Confirmar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { authViewModel.resetState(); onDismiss() }) {
                Text("Cancelar")
            }
        }
    )
}
