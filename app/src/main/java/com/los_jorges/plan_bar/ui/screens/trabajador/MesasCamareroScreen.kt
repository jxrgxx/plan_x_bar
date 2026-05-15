package com.los_jorges.plan_bar.ui.screens.trabajador

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.Mesa
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.ui.screens.admin.PlanoCanvas
import com.los_jorges.plan_bar.ui.theme.LocalStrings
import com.los_jorges.plan_bar.viewmodel.MesasViewModel
import com.los_jorges.plan_bar.viewmodel.ZonasViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MesasCamareroScreen(
    onVolverAlSelector: () -> Unit,
    onAbrirComanda: (mesaId: Int, mesaCodigo: String, comensales: Int) -> Unit
) {
    val s = LocalStrings.current
    val trabajador by SessionManager.trabajador.collectAsState()
    val mesasVm: MesasViewModel = viewModel()
    val zonasVm: ZonasViewModel = viewModel()

    val zonasDB by zonasVm.zonas.collectAsState()
    // Zonas activas ordenadas — usa BD si está disponible, caché si no
    val zonas = remember(zonasDB) {
        if (zonasDB.isNotEmpty())
            zonasDB.filter { it.activo }.sortedBy { it.orden }.map { it.clave to it.nombre }
        else
            SessionManager.zonas
    }

    var zonaActual by remember {
        mutableStateOf(
            SessionManager.zonas.firstOrNull()?.first ?: "piso1"
        )
    }
    var mesaSeleccionada by remember { mutableStateOf<Mesa?>(null) }

    // Si la zona activa desaparece (la desactivaron desde otro dispositivo), volver a la primera
    LaunchedEffect(zonas) {
        if (zonas.isNotEmpty() && zonas.none { it.first == zonaActual }) {
            zonaActual = zonas.first().first
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                mesasVm.cargar(SessionManager.restauranteId)
                zonasVm.cargar(SessionManager.restauranteId)
                delay(5_000)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("${s.hola} ${trabajador?.nombre ?: ""}") },
                actions = {
                    IconButton(onClick = {
                        SessionManager.desactivarPersonal()
                        onVolverAlSelector()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            s.cambiarTrabajador
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
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
    val s = LocalStrings.current
    var texto by remember { mutableStateOf("") }
    val accent = Color(0xFF83C9A5)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Cabecera
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accent.copy(alpha = 0.08f))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(accent.copy(alpha = 0.18f))
                            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.TableRestaurant,
                            null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            "${s.mesaLabel} $mesaCodigo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            s.cuantosComensales,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { v -> texto = v.filter { it.isDigit() } },
                        placeholder = { Text(s.unoPorDefecto) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Group,
                                null,
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) { Text(s.cancelar) }
                    Button(
                        onClick = { onConfirm(texto.toIntOrNull()?.coerceAtLeast(1) ?: 1) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s.abrirMesa, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
