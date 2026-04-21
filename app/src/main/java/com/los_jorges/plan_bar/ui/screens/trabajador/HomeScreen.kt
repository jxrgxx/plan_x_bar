package com.los_jorges.plan_bar.ui.screens.trabajador

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.ui.screens.admin.PlanoCanvas
import com.los_jorges.plan_bar.viewmodel.AuthState
import com.los_jorges.plan_bar.viewmodel.AuthViewModel
import com.los_jorges.plan_bar.viewmodel.MesasViewModel
import com.los_jorges.plan_bar.viewmodel.PedidosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCerrarSesion: () -> Unit,
    onAbrirComanda: (Int) -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val trabajador       by SessionManager.trabajador.collectAsState()
    val mesasVm: MesasViewModel   = viewModel()
    val pedidosVm: PedidosViewModel = viewModel()
    val abriendo         by pedidosVm.loading.collectAsState()
    val pedidosError     by pedidosVm.error.collectAsState()

    var showDialogCerrar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Recargar mesas al volver desde la comanda
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                mesasVm.cargar(SessionManager.restauranteId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(pedidosError) {
        pedidosError?.let {
            snackbarHostState.showSnackbar(it)
            pedidosVm.clearError()
        }
    }

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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PlanoCanvas(
                restauranteId = SessionManager.restauranteId,
                modoEdicion   = false,
                onMesaTap     = { mesa ->
                    pedidosVm.abrirOCrearPedido(
                        restauranteId = SessionManager.restauranteId,
                        mesaId        = mesa.id,
                        trabajadorId  = trabajador?.id,
                        onPedidoId    = onAbrirComanda
                    )
                },
                vm = mesasVm
            )

            // Overlay de carga mientras abre/crea el pedido
            if (abriendo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
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

// ─── Diálogo verificación admin ──────────────────────────────────────────────

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
                Text(
                    "Introduce la contraseña del administrador para cerrar sesión.",
                    style = MaterialTheme.typography.bodyMedium
                )
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
