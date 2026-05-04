package com.los_jorges.plan_bar.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
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
import com.los_jorges.plan_bar.model.RestauranteItem
import com.los_jorges.plan_bar.viewmodel.ReservasViewModel
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioReservaScreen(
    onBack: () -> Unit,
    vm: ReservasViewModel = viewModel()
) {
    val restaurantes by vm.restaurantes.collectAsState()
    val loading by vm.loading.collectAsState()

    var restauranteSeleccionado by remember { mutableStateOf<RestauranteItem?>(null) }
    var restauranteExpanded by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var personas by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
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
        "${cal.get(Calendar.DAY_OF_MONTH)} de ${meses[cal.get(Calendar.MONTH)]} de ${
            cal.get(
                Calendar.YEAR
            )
        }"
    } ?: "Selecciona una fecha"

    val horaMostrada = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)

    var codigoConfirmacion by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.cargarRestaurantes() }

    // Pantalla de confirmación
    if (codigoConfirmacion != null) {
        ConfirmacionReserva(codigo = codigoConfirmacion!!, onBack = onBack)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hacer una reserva") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            null
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Text(
                "Datos de la reserva",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Selector de restaurante
            ExposedDropdownMenuBox(
                expanded = restauranteExpanded,
                onExpandedChange = { restauranteExpanded = it }) {
                OutlinedTextField(
                    value = restauranteSeleccionado?.nombre ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Restaurante *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(restauranteExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    placeholder = { Text("Selecciona un restaurante") }
                )
                ExposedDropdownMenu(
                    expanded = restauranteExpanded,
                    onDismissRequest = { restauranteExpanded = false }) {
                    if (restaurantes.isEmpty()) {
                        DropdownMenuItem(text = { Text("Cargando…") }, onClick = {})
                    } else {
                        restaurantes.forEach { r ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(r.nombre, style = MaterialTheme.typography.bodyMedium)
                                        if (!r.direccion.isNullOrBlank())
                                            Text(
                                                r.direccion,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                    }
                                },
                                onClick = {
                                    restauranteSeleccionado = r; restauranteExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()
            Text(
                "Tus datos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = nombre, onValueChange = { nombre = it },
                label = { Text("Nombre *") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = telefono, onValueChange = { telefono = it },
                label = { Text("Teléfono *") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = correo, onValueChange = { correo = it },
                label = { Text("Email (opcional)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()
            Text(
                "Detalles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(fechaMostrada)
            }

            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(horaMostrada)
            }

            OutlinedTextField(
                value = personas, onValueChange = { personas = it.filter { c -> c.isDigit() } },
                label = { Text("Número de personas *") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notas, onValueChange = { notas = it },
                label = { Text("Observaciones (opcional)") },
                placeholder = { Text("Alergias, ocasión especial…") },
                modifier = Modifier.fillMaxWidth(), maxLines = 4
            )

            if (errorMsg != null) {
                Text(
                    errorMsg!!, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    errorMsg = null
                    val r = restauranteSeleccionado
                    val p = personas.toIntOrNull() ?: 0
                    when {
                        r == null -> errorMsg = "Selecciona un restaurante"
                        nombre.isBlank() -> errorMsg = "Introduce tu nombre"
                        telefono.isBlank() -> errorMsg = "Introduce un teléfono"
                        fechaISO.isBlank() -> errorMsg = "Selecciona una fecha"
                        p < 1 -> errorMsg = "Indica el número de personas"
                        else -> vm.crear(
                            r.id,
                            nombre,
                            telefono,
                            correo,
                            p,
                            fechaISO,
                            horaMostrada,
                            notas
                        ) { ok, err, codigo ->
                            if (ok) codigoConfirmacion = codigo
                            else errorMsg = err ?: "Error al crear la reserva"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !loading
            ) {
                if (loading) CircularProgressIndicator(
                    Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                else Text("Solicitar reserva", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }

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
private fun ConfirmacionReserva(codigo: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle, null,
                modifier = Modifier.size(72.dp), tint = Color(0xFF43A047)
            )
            Text(
                "¡Reserva solicitada!", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
            Text(
                "Tu código de reserva es:", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    codigo, style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
                )
            }
            Text(
                "Guárdalo, te lo pedirán al llegar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Volver al inicio") }
        }
    }
}
