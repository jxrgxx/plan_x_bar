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
import com.los_jorges.plan_bar.ui.theme.LocalStrings
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioReservaScreen(
    onBack: () -> Unit,
    vm: ReservasViewModel = viewModel()
) {
    val s = LocalStrings.current
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
    } ?: s.seleccionaUnaFecha

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
                title = { Text(s.hacerUnaReserva) },
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
                s.datosDelaReserva,
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
                    label = { Text(s.restauranteRequerido) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(restauranteExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    placeholder = { Text(s.seleccionaUnRestaurante) }
                )
                ExposedDropdownMenu(
                    expanded = restauranteExpanded,
                    onDismissRequest = { restauranteExpanded = false }) {
                    if (restaurantes.isEmpty()) {
                        DropdownMenuItem(text = { Text(s.cargando) }, onClick = {})
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
                s.tusDatos,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text(s.nombreRequerido) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = telefono, onValueChange = { telefono = it },
                label = { Text(s.telefonoRequerido) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = correo, onValueChange = { correo = it },
                label = { Text(s.emailOpcional) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()
            Text(
                s.detallesReserva,
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
                label = { Text(s.numeroPersonasRequerido) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notas, onValueChange = { notas = it },
                label = { Text(s.observacionesOpcionales) },
                placeholder = { Text(s.alergiasPlaceholder) },
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
                        r == null -> errorMsg = s.seleccionaUnRestaurante
                        nombre.isBlank() -> errorMsg = s.introduceTuNombre
                        telefono.isBlank() -> errorMsg = s.introduceTelefono
                        fechaISO.isBlank() -> errorMsg = s.seleccionaUnaFecha
                        p < 1 -> errorMsg = s.indicaNumeroPersonas
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
                            else errorMsg = err ?: s.errorAlCrearReserva
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
                else Text(s.solicitarReserva, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(s.aceptar) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(s.cancelar) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(s.seleccionarLaHora) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(s.aceptar) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(s.cancelar) }
            }
        )
    }
}

@Composable
private fun ConfirmacionReserva(codigo: String, onBack: () -> Unit) {
    val s = LocalStrings.current
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
                s.reservaSolicitada, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
            Text(
                s.tuCodigoDeReserva, style = MaterialTheme.typography.bodyMedium,
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
                s.guardaloTeLoPediran,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) { Text(s.volverAlInicio) }
        }
    }
}
