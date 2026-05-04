package com.los_jorges.plan_bar.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.Reserva
import com.los_jorges.plan_bar.model.Trabajador
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.viewmodel.ReservasViewModel
import com.los_jorges.plan_bar.viewmodel.TrabajadoresViewModel
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorPersonalScreen(
    onTrabajadorSeleccionado: (Trabajador) -> Unit,
    onCerrarSesionRestaurante: () -> Unit
) {
    val vm: TrabajadoresViewModel = viewModel()
    val reservasVm: ReservasViewModel = viewModel()
    val trabajadores by vm.trabajadores.collectAsState()
    val loading by vm.loading.collectAsState()
    val reservas by reservasVm.reservas.collectAsState()
    val restauranteNombre = SessionManager.restauranteNombre
    val restauranteId = SessionManager.restauranteId

    var showNuevaReserva by remember { mutableStateOf(false) }
    var showVerReservas by remember { mutableStateOf(false) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.cargar(restauranteId) }
    LaunchedEffect(snackMsg) {
        snackMsg?.let {
            snackbarHostState.showSnackbar(it); snackMsg = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(restauranteNombre) },
                actions = {
                    TextButton(onClick = onCerrarSesionRestaurante) { Text("Cerrar sesión") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¿Quién eres?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 20.dp)
            )

            if (loading) {
                Box(Modifier
                    .weight(1f)
                    .fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val personal = trabajadores.filter { it.activo }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(130.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(personal, key = { it.id }) { t ->
                        TrabajadorCard(t, onClick = { onTrabajadorSeleccionado(t) })
                    }
                }
            }

            // ── Botones de reservas ───────────────────────────────────────
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        showVerReservas = true; reservasVm.cargar(
                        restauranteId,
                        reservasVm.fechaHoy()
                    )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ver reservas")
                }
                Button(
                    onClick = { showNuevaReserva = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nueva reserva")
                }
            }
        }
    }

    if (showNuevaReserva) {
        NuevaReservaRapidaDialog(
            onDismiss = { showNuevaReserva = false },
            onConfirm = { nombre, telefono, personas, fecha, hora, notas ->
                reservasVm.crear(
                    restauranteId, nombre, telefono, "", personas, fecha, hora, notas
                ) { ok, err, codigo ->
                    showNuevaReserva = false
                    snackMsg = if (ok) "Reserva creada · Código: $codigo" else err
                        ?: "Error al crear la reserva"
                }
            }
        )
    }

    if (showVerReservas) {
        VerReservasDialog(
            reservas = reservas,
            onDismiss = { showVerReservas = false },
            onMarcarLlegado = { reserva ->
                val nuevoEstado = if (reserva.estado == "completada") "confirmada" else "completada"
                reservasVm.cambiarEstado(
                    reserva.id,
                    nuevoEstado,
                    restauranteId,
                    reservasVm.fechaHoy()
                ) {}
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevaReservaRapidaDialog(
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, telefono: String, personas: Int, fecha: String, hora: String, notas: String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var personas by remember { mutableStateOf("2") }
    var notas by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val timePickerState = rememberTimePickerState(initialHour = 13, initialMinute = 0)

    val fechaISO = datePickerState.selectedDateMillis?.let { millis ->
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = millis
        "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
        )
    } ?: ""

    val fechaMostrada = datePickerState.selectedDateMillis?.let { millis ->
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = millis
        val meses = listOf(
            "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
            "agosto", "septiembre", "octubre", "noviembre", "diciembre"
        )
        "${cal.get(Calendar.DAY_OF_MONTH)} de ${meses[cal.get(Calendar.MONTH)]}"
    } ?: "Selecciona fecha"

    val horaMostrada = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva reserva") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = personas,
                    onValueChange = { personas = it.filter { c -> c.isDigit() } },
                    label = { Text("Personas *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Selector de fecha ─────────────────────────────────
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(fechaMostrada)
                }

                // ── Selector de hora ──────────────────────────────────
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(horaMostrada)
                }

                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Comentario (opcional)") },
                    placeholder = { Text("Ej: cumpleaños, quieren ver el fútbol…") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                if (errorMsg != null) {
                    Text(
                        errorMsg!!, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = personas.toIntOrNull() ?: 0
                when {
                    nombre.isBlank() -> errorMsg = "Introduce un nombre"
                    telefono.isBlank() -> errorMsg = "Introduce un teléfono"
                    fechaISO.isBlank() -> errorMsg = "Selecciona una fecha"
                    p < 1 -> errorMsg = "Indica el número de personas"
                    else -> onConfirm(nombre, telefono, p, fechaISO, horaMostrada, notas)
                }
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Selecciona la hora") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun VerReservasDialog(
    reservas: List<Reserva>,
    onDismiss: () -> Unit,
    onMarcarLlegado: (Reserva) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reservas de hoy") },
        text = {
            if (reservas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sin reservas hoy", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(reservas, key = { it.id }) { r ->
                        ReservaResumenItem(r, onMarcarLlegado = { onMarcarLlegado(r) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

private fun estadoColorSimple(estado: String): Color = when (estado) {
    "confirmada" -> Color(0xFF1976D2)
    "completada" -> Color(0xFF43A047)
    "cancelada" -> Color(0xFFE53935)
    else -> Color(0xFFFFA726)
}

@Composable
private fun ReservaResumenItem(reserva: Reserva, onMarcarLlegado: () -> Unit) {
    val llegado = reserva.estado == "completada"
    val color = estadoColorSimple(reserva.estado)
    val verde = Color(0xFF43A047)

    Surface(
        color = if (llegado) verde.copy(alpha = 0.08f) else color.copy(alpha = 0.08f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = color.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    reserva.hora.take(5),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    reserva.nombre,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (llegado) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = if (llegado) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${reserva.num_personas} pers. · ${reserva.telefono}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (!reserva.notas.isNullOrBlank()) {
                    Text(
                        reserva.notas, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            IconButton(onClick = onMarcarLlegado) {
                Icon(
                    imageVector = if (llegado) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (llegado) "Desmarcar" else "Marcar como llegado",
                    tint = if (llegado) verde else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun TrabajadorCard(trabajador: Trabajador, onClick: () -> Unit) {
    val rolColor = when (trabajador.rol) {
        "admin" -> MaterialTheme.colorScheme.primaryContainer
        "cocina" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val rolTexto = when (trabajador.rol) {
        "admin" -> "Admin"
        "cocina" -> "Cocina"
        else -> "Camarero"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = rolColor),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = trabajador.nombre,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Text(
                text = rolTexto,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
