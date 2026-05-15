package com.los_jorges.plan_bar.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.los_jorges.plan_bar.ui.theme.premiumInputColors
import com.los_jorges.plan_bar.ui.theme.LocalStrings
import com.los_jorges.plan_bar.model.Reserva
import com.los_jorges.plan_bar.viewmodel.ReservasViewModel

private val ReservaAccent = Color(0xFF06B6D4)

private val ESTADOS = listOf("pendiente", "confirmada", "cancelada", "completada")

private fun estadoColor(estado: String): Color = when (estado) {
    "confirmada" -> Color(0xFF06B6D4)
    "completada" -> Color(0xFF83C9A5)
    "cancelada"  -> Color(0xFFE57373)
    else         -> Color(0xFFD4A853)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservasAdminScreen(
    restauranteId: Int,
    onBack: () -> Unit,
    vm: ReservasViewModel = viewModel()
) {
    val s = LocalStrings.current
    val reservas by vm.reservas.collectAsState()
    val loading  by vm.loading.collectAsState()
    val error    by vm.error.collectAsState()

    var fecha         by remember { mutableStateOf(vm.fechaHoy()) }
    var snackMsg      by remember { mutableStateOf<String?>(null) }
    var showCrear     by remember { mutableStateOf(false) }
    var reservaEditar by remember { mutableStateOf<Reserva?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(fecha) {
        while (true) {
            vm.cargar(restauranteId, fecha)
            delay(5_000)
        }
    }
    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }
    error?.let { snackMsg = it; vm.clearError() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            s.reservas,
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            s.verYGestionarReservas,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { showCrear = true }) {
                        Icon(Icons.Default.Add, s.nuevaReservaAdmin,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Navegador de fecha ────────────────────────────────────────────
            Surface(
                color  = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    width = 0.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { fecha = vm.desplazarFecha(fecha, -1) }) {
                        Icon(Icons.Default.ChevronLeft, s.diaAnterior,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            vm.formatearFechaLegible(fecha),
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${reservas.size} reserva${if (reservas.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ReservaAccent
                        )
                    }
                    IconButton(onClick = { fecha = vm.desplazarFecha(fecha, 1) }) {
                        Icon(Icons.Default.ChevronRight, s.diaSiguiente,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ReservaAccent)
                }
                return@Scaffold
            }

            if (reservas.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(ReservaAccent.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EventBusy, null,
                                modifier = Modifier.size(32.dp),
                                tint     = ReservaAccent.copy(alpha = 0.6f))
                        }
                        Text(
                            s.sinReservasEsteDia,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { showCrear = true }) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(s.anadirReserva)
                        }
                    }
                }
                return@Scaffold
            }

            LazyColumn(
                contentPadding      = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reservas, key = { it.id }) { reserva ->
                    ReservaCard(
                        reserva = reserva,
                        onCambiarEstado = { nuevoEstado ->
                            vm.cambiarEstado(reserva.id, nuevoEstado, restauranteId, fecha) { ok ->
                                if (!ok) snackMsg = s.errorActualizarEstado
                            }
                        },
                        onEditar   = { reservaEditar = reserva },
                        onEliminar = {
                            vm.eliminar(reserva.id, restauranteId, fecha) { ok, err ->
                                snackMsg = if (ok) s.reservaEliminada else err ?: "Error"
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
                vm.crear(restauranteId, nombre, telefono, correo, personas, fecha, hora, notas) { ok, err, codigo ->
                    showCrear = false
                    snackMsg  = if (ok) "${s.reservaCreadaCodigo} $codigo" else err ?: "Error"
                }
            }
        )
    }

    reservaEditar?.let { r ->
        EditarReservaDialog(
            reserva   = r,
            onDismiss = { reservaEditar = null },
            onConfirm = { nombre, telefono, correo, personas, hora, notas ->
                vm.editar(r.id, nombre, telefono, correo, personas, hora, notas, restauranteId, fecha) { ok, err ->
                    reservaEditar = null
                    snackMsg      = if (ok) s.reservaActualizada else err ?: "Error"
                }
            }
        )
    }
}

// ── Tarjeta de reserva ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReservaCard(
    reserva: Reserva,
    onCambiarEstado: (String) -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val s = LocalStrings.current
    var expandedEstado by remember { mutableStateOf(false) }
    var showEliminar   by remember { mutableStateOf(false) }
    val estadoCol = estadoColor(reserva.estado)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Fila superior: hora + nombre + estado ─────────────────────
            Row(
                verticalAlignment  = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Caja de hora
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ReservaAccent.copy(alpha = 0.12f))
                        .border(1.dp, ReservaAccent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        reserva.hora.take(5),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = ReservaAccent
                    )
                }

                // Nombre y teléfono
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        reserva.nombre,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        reserva.telefono,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Badge de estado (desplegable)
                ExposedDropdownMenuBox(
                    expanded = expandedEstado,
                    onExpandedChange = { expandedEstado = it }
                ) {
                    Surface(
                        color    = estadoCol.copy(alpha = 0.12f),
                        shape    = RoundedCornerShape(8.dp),
                        modifier = Modifier.menuAnchor()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                reserva.estado.replaceFirstChar { it.uppercase() },
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color      = estadoCol
                            )
                            Icon(Icons.Default.ArrowDropDown, null,
                                tint = estadoCol, modifier = Modifier.size(14.dp))
                        }
                    }
                    ExposedDropdownMenu(
                        expanded = expandedEstado,
                        onDismissRequest = { expandedEstado = false }
                    ) {
                        ESTADOS.forEach { e ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        e.replaceFirstChar { it.uppercase() },
                                        color      = estadoColor(e),
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = { onCambiarEstado(e); expandedEstado = false }
                            )
                        }
                    }
                }
            }

            // ── Fila detalles: personas + código ──────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Group, null,
                        modifier = Modifier.size(14.dp),
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${reserva.num_personas} ${s.personasLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("·", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "#${reserva.codigo}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Notas ─────────────────────────────────────────────────────
            if (!reserva.notas.isNullOrBlank()) {
                Surface(
                    color  = MaterialTheme.colorScheme.surfaceVariant,
                    shape  = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Notes, null,
                            modifier = Modifier.size(14.dp).padding(top = 1.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            reserva.notas,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Acciones ──────────────────────────────────────────────────
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEditar) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.editar, style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = { showEliminar = true }) {
                    Icon(Icons.Default.Delete, null,
                        modifier = Modifier.size(15.dp),
                        tint     = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text(s.eliminar,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    if (showEliminar) {
        AlertDialog(
            onDismissRequest = { showEliminar = false },
            icon  = { Icon(Icons.Default.EventBusy, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(s.eliminarReserva) },
            text  = { Text("${s.confirmarEliminarReserva} ${reserva.nombre}?") },
            confirmButton = {
                Button(
                    onClick = { onEliminar(); showEliminar = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(s.eliminar) }
            },
            dismissButton = {
                TextButton(onClick = { showEliminar = false }) { Text(s.cancelar) }
            }
        )
    }
}

// ── Diálogos crear / editar ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditarReservaDialog(
    reserva: Reserva,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int, String, String) -> Unit
) {
    val s = LocalStrings.current
    var nombre   by remember { mutableStateOf(reserva.nombre) }
    var telefono by remember { mutableStateOf(reserva.telefono) }
    var correo   by remember { mutableStateOf(reserva.correo ?: "") }
    var personas by remember { mutableStateOf(reserva.num_personas.toString()) }
    var hora     by remember { mutableStateOf(reserva.hora.take(5)) }
    var notas    by remember { mutableStateOf(reserva.notas ?: "") }

    ReservaFormDialog(
        titulo    = s.editarReserva,
        subtitulo = "Reserva #${reserva.codigo}",
        isEdicion = true,
        nombre = nombre, onNombreChange = { nombre = it },
        telefono = telefono, onTelefonoChange = { telefono = it },
        correo = correo, onCorreoChange = { correo = it },
        personas = personas, onPersonasChange = { personas = it },
        hora = hora, onHoraChange = { hora = it },
        notas = notas, onNotasChange = { notas = it },
        labelConfirm = s.guardar,
        onDismiss = onDismiss,
        onConfirm = {
            val p = personas.toIntOrNull() ?: 0
            if (nombre.isNotBlank() && telefono.isNotBlank() && hora.isNotBlank() && p > 0)
                onConfirm(nombre, telefono, correo, p, hora, notas)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrearReservaDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int, String, String) -> Unit
) {
    val s = LocalStrings.current
    var nombre   by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var correo   by remember { mutableStateOf("") }
    var personas by remember { mutableStateOf("2") }
    var hora     by remember { mutableStateOf("13:00") }
    var notas    by remember { mutableStateOf("") }

    ReservaFormDialog(
        titulo    = s.nuevaReservaAdmin,
        subtitulo = s.anadirReservaManualmente,
        isEdicion = false,
        nombre = nombre, onNombreChange = { nombre = it },
        telefono = telefono, onTelefonoChange = { telefono = it },
        correo = correo, onCorreoChange = { correo = it },
        personas = personas, onPersonasChange = { personas = it },
        hora = hora, onHoraChange = { hora = it },
        notas = notas, onNotasChange = { notas = it },
        labelConfirm = s.crearReserva,
        onDismiss = onDismiss,
        onConfirm = {
            val p = personas.toIntOrNull() ?: 0
            if (nombre.isNotBlank() && telefono.isNotBlank() && hora.isNotBlank() && p > 0)
                onConfirm(nombre, telefono, correo, p, hora, notas)
        }
    )
}

@Composable
private fun ReservaFormDialog(
    titulo: String,
    subtitulo: String,
    isEdicion: Boolean,
    nombre: String,   onNombreChange: (String) -> Unit,
    telefono: String, onTelefonoChange: (String) -> Unit,
    correo: String,   onCorreoChange: (String) -> Unit,
    personas: String, onPersonasChange: (String) -> Unit,
    hora: String,     onHoraChange: (String) -> Unit,
    notas: String,    onNotasChange: (String) -> Unit,
    labelConfirm: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val s = LocalStrings.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape    = RoundedCornerShape(24.dp),
            color    = MaterialTheme.colorScheme.surface,
            border   = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {

                // ── Cabecera ──────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ReservaAccent.copy(alpha = 0.08f))
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ReservaAccent.copy(alpha = 0.18f))
                                .border(1.dp, ReservaAccent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isEdicion) Icons.Default.EditCalendar else Icons.Default.CalendarMonth,
                                null, tint = ReservaAccent, modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                titulo,
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                subtitulo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ── Campos ────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Sección: cliente
                    SeccionLabel(s.datosDelCliente, Icons.Default.Person)
                    Surface(
                        shape  = RoundedCornerShape(14.dp),
                        color  = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = nombre, onValueChange = onNombreChange,
                                label = { Text(s.nombreCliente) }, singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Badge, null, tint = ReservaAccent, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp), colors = premiumInputColors()
                            )
                            OutlinedTextField(
                                value = telefono, onValueChange = onTelefonoChange,
                                label = { Text(s.telefonoCliente) }, singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Phone, null, tint = ReservaAccent, modifier = Modifier.size(18.dp)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp), colors = premiumInputColors()
                            )
                            OutlinedTextField(
                                value = correo, onValueChange = onCorreoChange,
                                label = { Text(s.emailOpcional) }, singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp), colors = premiumInputColors()
                            )
                        }
                    }

                    // Sección: detalles
                    SeccionLabel(s.detallesReserva, Icons.Default.EventNote)
                    Surface(
                        shape  = RoundedCornerShape(14.dp),
                        color  = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = personas,
                                    onValueChange = { onPersonasChange(it.filter { c -> c.isDigit() }) },
                                    label = { Text(s.personas) }, singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Group, null, tint = ReservaAccent, modifier = Modifier.size(18.dp)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp), colors = premiumInputColors()
                                )
                                OutlinedTextField(
                                    value = hora, onValueChange = onHoraChange,
                                    label = { Text(s.hora) }, singleLine = true,
                                    placeholder = { Text("13:00") },
                                    leadingIcon = { Icon(Icons.Default.Schedule, null, tint = ReservaAccent, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp), colors = premiumInputColors()
                                )
                            }
                            OutlinedTextField(
                                value = notas, onValueChange = onNotasChange,
                                label = { Text(s.notasOpcional) },
                                leadingIcon = { Icon(Icons.Default.Notes, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.fillMaxWidth(), maxLines = 3,
                                shape = RoundedCornerShape(10.dp), colors = premiumInputColors()
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ── Acciones ──────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(s.cancelar, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = onConfirm,
                        shape   = RoundedCornerShape(12.dp),
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = ReservaAccent,
                            contentColor   = Color.White
                        )
                    ) {
                        Icon(
                            if (isEdicion) Icons.Default.Check else Icons.Default.CalendarMonth,
                            null, modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(labelConfirm, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionLabel(texto: String, icono: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icono, null, tint = ReservaAccent, modifier = Modifier.size(14.dp))
        Text(
            texto,
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color      = ReservaAccent
        )
    }
}
