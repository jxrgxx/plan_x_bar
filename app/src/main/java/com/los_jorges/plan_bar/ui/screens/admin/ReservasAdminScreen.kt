package com.los_jorges.plan_bar.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.Reserva
import com.los_jorges.plan_bar.viewmodel.ReservasViewModel

private val ESTADOS = listOf("pendiente", "confirmada", "cancelada", "completada")

private fun estadoColor(estado: String): Color = when (estado) {
    "confirmada" -> Color(0xFF1976D2)
    "completada" -> Color(0xFF43A047)
    "cancelada" -> Color(0xFFE53935)
    else -> Color(0xFFFFA726)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservasAdminScreen(
    restauranteId: Int,
    onBack: () -> Unit,
    vm: ReservasViewModel = viewModel()
) {
    val reservas by vm.reservas.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var fecha by remember { mutableStateOf(vm.fechaHoy()) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    var showCrear by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(fecha) { vm.cargar(restauranteId, fecha) }
    LaunchedEffect(snackMsg) {
        snackMsg?.let {
            snackbarHostState.showSnackbar(it); snackMsg = null
        }
    }
    error?.let { snackMsg = it; vm.clearError() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reservas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCrear = true }) {
                        Icon(
                            Icons.Default.Add,
                            "Nueva reserva"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            // ── Navegador de fecha ────────────────────────────────────────
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { fecha = vm.desplazarFecha(fecha, -1) }) {
                        Icon(Icons.Default.ChevronLeft, "Día anterior")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            vm.formatearFechaLegible(fecha),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${reservas.size} reserva${if (reservas.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = { fecha = vm.desplazarFecha(fecha, 1) }) {
                        Icon(Icons.Default.ChevronRight, "Día siguiente")
                    }
                }
            }

            if (loading) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
                return@Scaffold
            }

            if (reservas.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.EventBusy,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text("Sin reservas este día", color = MaterialTheme.colorScheme.outline)
                    }
                }
                return@Scaffold
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reservas, key = { it.id }) { reserva ->
                    ReservaCard(
                        reserva = reserva,
                        onCambiarEstado = { nuevoEstado ->
                            vm.cambiarEstado(reserva.id, nuevoEstado, restauranteId, fecha) { ok ->
                                if (!ok) snackMsg = "Error al actualizar estado"
                            }
                        },
                        onEliminar = {
                            vm.eliminar(reserva.id, restauranteId, fecha) { ok, err ->
                                snackMsg = if (ok) "Reserva eliminada" else err ?: "Error"
                            }
                        }
                    )
                }
            }
        }
    }

    if (showCrear) {
        CrearReservaDialog(
            onDismiss = { showCrear = false },
            onConfirm = { nombre, telefono, correo, personas, hora, notas ->
                vm.crear(
                    restauranteId,
                    nombre,
                    telefono,
                    correo,
                    personas,
                    fecha,
                    hora,
                    notas
                ) { ok, err, codigo ->
                    showCrear = false
                    snackMsg = if (ok) "Reserva creada · Código: $codigo" else err ?: "Error"
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReservaCard(
    reserva: Reserva,
    onCambiarEstado: (String) -> Unit,
    onEliminar: () -> Unit
) {
    var expandedEstado by remember { mutableStateOf(false) }
    var showEliminar by remember { mutableStateOf(false) }
    val color = estadoColor(reserva.estado)

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // Hora + nombre + estado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        reserva.hora.take(5),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        reserva.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        reserva.telefono,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                // Badge estado
                ExposedDropdownMenuBox(
                    expanded = expandedEstado,
                    onExpandedChange = { expandedEstado = it }) {
                    Surface(
                        color = color.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.menuAnchor()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                reserva.estado.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                null,
                                tint = color,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    ExposedDropdownMenu(
                        expanded = expandedEstado,
                        onDismissRequest = { expandedEstado = false }) {
                        ESTADOS.forEach { e ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        e.replaceFirstChar { it.uppercase() },
                                        color = estadoColor(e), fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = { onCambiarEstado(e); expandedEstado = false }
                            )
                        }
                    }
                }
            }

            // Detalles
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Group,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "${reserva.num_personas} personas",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    "· #${reserva.codigo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (!reserva.notas.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        reserva.notas, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            }

            // Eliminar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { showEliminar = true }) {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Eliminar",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }

    if (showEliminar) {
        AlertDialog(
            onDismissRequest = { showEliminar = false },
            title = { Text("Eliminar reserva") },
            text = { Text("¿Eliminar la reserva de ${reserva.nombre}?") },
            confirmButton = {
                TextButton(onClick = { onEliminar(); showEliminar = false }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showEliminar = false }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrearReservaDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int, String, String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var personas by remember { mutableStateOf("2") }
    var hora by remember { mutableStateOf("13:00") }
    var notas by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva reserva") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = { Text("Email (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = personas,
                        onValueChange = { personas = it.filter { c -> c.isDigit() } },
                        label = { Text("Personas *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hora,
                        onValueChange = { hora = it },
                        label = { Text("Hora *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("HH:MM") })
                }
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = personas.toIntOrNull() ?: 0
                if (nombre.isNotBlank() && telefono.isNotBlank() && hora.isNotBlank() && p > 0)
                    onConfirm(nombre, telefono, correo, p, hora, notas)
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
