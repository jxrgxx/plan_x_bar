package com.los_jorges.plan_bar.ui.screens.trabajador

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.Mesa
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.ui.screens.admin.PlanoCanvas
import com.los_jorges.plan_bar.viewmodel.MesasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MesasCamareroScreen(
    onVolverAlSelector: () -> Unit,
    onAbrirComanda: (mesaId: Int, mesaCodigo: String, comensales: Int) -> Unit
) {
    val trabajador by SessionManager.trabajador.collectAsState()
    val mesasVm: MesasViewModel = viewModel()

    var zonaActual by remember { mutableStateOf("piso1") }
    var mesaSeleccionada by remember { mutableStateOf<Mesa?>(null) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            mesasVm.cargar(SessionManager.restauranteId)
        }
    }

    Scaffold(
        containerColor = Color(0xFF100E0C),
        topBar = {
            TopAppBar(
                title = { Text("Hola, ${trabajador?.nombre ?: ""}", color = Color(0xFFF5F0E8)) },
                actions = {
                    IconButton(onClick = {
                        SessionManager.desactivarPersonal()
                        onVolverAlSelector()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            "Cambiar trabajador",
                            tint = Color(0xFFA09890)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF171411))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val zonas = SessionManager.zonas
            TabRow(
                selectedTabIndex = zonas.indexOfFirst { it.first == zonaActual }.coerceAtLeast(0),
                containerColor = Color(0xFF171411),
                contentColor = Color(0xFF8896A8)
            ) {
                zonas.forEach { (key, label) ->
                    Tab(
                        selected = zonaActual == key,
                        onClick = { zonaActual = key },
                        text = {
                            Text(
                                label,
                                fontWeight = if (zonaActual == key) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                PlanoCanvas(
                    restauranteId = SessionManager.restauranteId,
                    zona = zonaActual,
                    modoEdicion = false,
                    onMesaTap = { mesa ->
                        if (mesa.estado == "ocupada") {
                            onAbrirComanda(mesa.id, mesa.codigo, 1)
                        } else {
                            mesaSeleccionada = mesa
                        }
                    },
                    vm = mesasVm
                )
            }
        }
    }

    mesaSeleccionada?.let { mesa ->
        ComensalesDialog(
            mesaCodigo = mesa.codigo,
            onDismiss = { mesaSeleccionada = null },
            onConfirm = { comensales ->
                mesaSeleccionada = null
                onAbrirComanda(mesa.id, mesa.codigo, comensales)
            }
        )
    }
}

@Composable
private fun ComensalesDialog(
    mesaCodigo: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var texto by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mesa $mesaCodigo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("¿Cuántos comensales?", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = texto,
                    onValueChange = { v -> texto = v.filter { it.isDigit() } },
                    placeholder = { Text("1 (por defecto)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(texto.toIntOrNull()?.coerceAtLeast(1) ?: 1) }) {
                Text("Abrir mesa", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
