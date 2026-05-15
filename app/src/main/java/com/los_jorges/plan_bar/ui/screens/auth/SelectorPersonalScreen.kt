package com.los_jorges.plan_bar.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.Reserva
import com.los_jorges.plan_bar.model.Trabajador
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.viewmodel.AuthState
import com.los_jorges.plan_bar.viewmodel.AuthViewModel
import com.los_jorges.plan_bar.viewmodel.ReservasViewModel
import com.los_jorges.plan_bar.viewmodel.TrabajadoresViewModel
import com.los_jorges.plan_bar.ui.theme.LocalStrings
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorPersonalScreen(
    onTrabajadorSeleccionado: (Trabajador) -> Unit,
    onCerrarSesionRestaurante: () -> Unit,
    onGoToAjustes: () -> Unit = {}
) {
    val s = LocalStrings.current
    val vm: TrabajadoresViewModel = viewModel()
    val reservasVm: ReservasViewModel = viewModel()
    val authVm: AuthViewModel = viewModel()
    val trabajadores by vm.trabajadores.collectAsState()
    val loading by vm.loading.collectAsState()
    val reservas by reservasVm.reservas.collectAsState()
    val authState by authVm.state.collectAsState()
    val restauranteNombre = SessionManager.restauranteNombre
    val restauranteId = SessionManager.restauranteId

    var showNuevaReserva by remember { mutableStateOf(false) }
    var showVerReservas by remember { mutableStateOf(false) }
    var showConfirmarCierre by remember { mutableStateOf(false) }
    var showReservasMenu by remember { mutableStateOf(false) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.cargar(restauranteId) }
    LaunchedEffect(snackMsg) {
        snackMsg?.let {
            snackbarHostState.showSnackbar(it); snackMsg = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            restauranteNombre,
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            s.seleccionaTuPerfil,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onGoToAjustes) {
                        Icon(
                            Icons.Default.Settings, s.ajustes,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { authVm.resetState(); showConfirmarCierre = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout, s.cerrarSesion,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            if (loading) {
                Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else {
                val personal = trabajadores
                    .filter { it.activo }
                    .sortedBy { if (it.rol == "admin") 0 else 1 }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                    contentPadding        = PaddingValues(16.dp),
                    modifier              = Modifier.weight(1f)
                ) {
                    items(personal, key = { it.id }) { t ->
                        TrabajadorCard(t, onClick = { onTrabajadorSeleccionado(t) })
                    }
                }
            }

            // ── Botones de reservas ───────────────────────────────────────
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick   = {
                        reservasVm.cargar(restauranteId, reservasVm.fechaHoy())
                        showVerReservas = true
                    },
                    modifier  = Modifier.weight(1f),
                    shape     = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s.verReservas, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick  = { showNuevaReserva = true },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s.nuevaReserva, fontWeight = FontWeight.Medium)
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

    if (showConfirmarCierre) {
        ConfirmarCierreSesionDialog(
            authState = authState,
            onDismiss = { showConfirmarCierre = false; authVm.resetState() },
            onConfirmar = { password ->
                authVm.verificarAdmin(password) {
                    showConfirmarCierre = false
                    onCerrarSesionRestaurante()
                }
            }
        )
    }
}

@Composable
private fun ConfirmarCierreSesionDialog(
    authState: AuthState,
    onDismiss: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    val s = LocalStrings.current
    var password by remember { mutableStateOf("") }
    val isLoading = authState is AuthState.Loading
    val errorMsg  = (authState as? AuthState.Error)?.mensaje
    val accent    = Color(0xFFE57373)

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Surface(
            shape    = RoundedCornerShape(24.dp),
            color    = MaterialTheme.colorScheme.surface,
            border   = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                            .background(accent.copy(alpha = 0.15f))
                            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = accent, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(
                            s.cerrarSesion,
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            s.introducirContrasenaAdmin,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value                = password,
                        onValueChange        = { password = it },
                        label                = { Text(s.contrasena) },
                        singleLine           = true,
                        leadingIcon          = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier             = Modifier.fillMaxWidth(),
                        enabled              = !isLoading,
                        shape                = RoundedCornerShape(12.dp)
                    )
                    if (errorMsg != null) {
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.errorContainer) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                                Text(errorMsg, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss, enabled = !isLoading) { Text(s.cancelar) }
                    Button(
                        onClick  = { onConfirmar(password) },
                        enabled  = !isLoading,
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        else {
                            Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s.confirmar, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevaReservaRapidaDialog(
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, telefono: String, personas: Int, fecha: String, hora: String, notas: String) -> Unit
) {
    val s = LocalStrings.current
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
    } ?: s.seleccionarFecha

    val horaMostrada = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)

    val ReservaAccent = Color(0xFF06B6D4)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape    = RoundedCornerShape(24.dp),
            color    = MaterialTheme.colorScheme.surface,
            border   = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // ── Cabecera ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ReservaAccent.copy(alpha = 0.08f))
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.spacedBy(14.dp)
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
                                Icons.Default.CalendarMonth, null,
                                tint     = ReservaAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                s.nuevaReserva,
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                s.anadirAlRestaurante,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ── Campos ────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sección cliente
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Person, null, tint = ReservaAccent, modifier = Modifier.size(14.dp))
                        Text(
                            s.datosDelCliente,
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = ReservaAccent
                        )
                    }
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
                                value          = nombre,
                                onValueChange  = { nombre = it },
                                label          = { Text(s.nombreCliente) },
                                singleLine     = true,
                                leadingIcon    = { Icon(Icons.Default.Badge, null, tint = ReservaAccent, modifier = Modifier.size(18.dp)) },
                                modifier       = Modifier.fillMaxWidth(),
                                shape          = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value          = telefono,
                                onValueChange  = { telefono = it },
                                label          = { Text(s.telefonoCliente) },
                                singleLine     = true,
                                leadingIcon    = { Icon(Icons.Default.Phone, null, tint = ReservaAccent, modifier = Modifier.size(18.dp)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier       = Modifier.fillMaxWidth(),
                                shape          = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Sección detalles
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.EventNote, null, tint = ReservaAccent, modifier = Modifier.size(14.dp))
                        Text(
                            s.detallesReserva,
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = ReservaAccent
                        )
                    }
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
                                value          = personas,
                                onValueChange  = { personas = it.filter { c -> c.isDigit() } },
                                label          = { Text(s.personas) },
                                singleLine     = true,
                                leadingIcon    = { Icon(Icons.Default.Group, null, tint = ReservaAccent, modifier = Modifier.size(18.dp)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier       = Modifier.fillMaxWidth(),
                                shape          = RoundedCornerShape(10.dp)
                            )
                            // Fecha
                            OutlinedButton(
                                onClick   = { showDatePicker = true },
                                modifier  = Modifier.fillMaxWidth(),
                                shape     = RoundedCornerShape(10.dp),
                                colors    = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ReservaAccent
                                )
                            ) {
                                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(fechaMostrada, fontWeight = FontWeight.Medium)
                            }
                            // Hora
                            OutlinedButton(
                                onClick   = { showTimePicker = true },
                                modifier  = Modifier.fillMaxWidth(),
                                shape     = RoundedCornerShape(10.dp),
                                colors    = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ReservaAccent
                                )
                            ) {
                                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(horaMostrada, fontWeight = FontWeight.Medium)
                            }
                            OutlinedTextField(
                                value         = notas,
                                onValueChange = { notas = it },
                                label         = { Text(s.comentarioOpcional) },
                                placeholder   = { Text("Cumpleaños, quieren ver el fútbol…") },
                                leadingIcon   = { Icon(Icons.Default.Notes, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                                modifier      = Modifier.fillMaxWidth(),
                                maxLines      = 3,
                                shape         = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Error
                    if (errorMsg != null) {
                        Surface(
                            shape  = RoundedCornerShape(10.dp),
                            color  = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline, null,
                                    tint     = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    errorMsg!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ── Acciones ──────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(s.cancelar, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            val p = personas.toIntOrNull() ?: 0
                            when {
                                nombre.isBlank()   -> errorMsg = "Introduce un nombre"
                                telefono.isBlank() -> errorMsg = "Introduce un teléfono"
                                fechaISO.isBlank() -> errorMsg = s.seleccionarFecha
                                p < 1              -> errorMsg = "Indica el número de personas"
                                else -> onConfirm(nombre, telefono, p, fechaISO, horaMostrada, notas)
                            }
                        },
                        shape  = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ReservaAccent,
                            contentColor   = Color.White
                        )
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s.crearReserva, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
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
private fun VerReservasDialog(
    reservas: List<Reserva>,
    onDismiss: () -> Unit,
    onMarcarLlegado: (Reserva) -> Unit
) {
    val s = LocalStrings.current
    val accent = Color(0xFF06B6D4)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape    = RoundedCornerShape(24.dp),
            color    = MaterialTheme.colorScheme.surface,
            border   = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        Icon(Icons.Default.CalendarMonth, null, tint = accent, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(
                            s.reservasDeHoy,
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${reservas.size} reserva${if (reservas.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = accent
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (reservas.isEmpty()) {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.EventBusy, null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(36.dp))
                            Text(s.sinReservasHoy, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding      = PaddingValues(14.dp),
                        modifier            = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(reservas, key = { it.id }) { r ->
                            ReservaResumenItem(r, onMarcarLlegado = { onMarcarLlegado(r) })
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = onDismiss) { Text(s.cerrar) }
                }
            }
        }
    }
}

private fun estadoColorSimple(estado: String): Color = when (estado) {
    "confirmada" -> Color(0xFF1976D2)
    "completada" -> Color(0xFF43A047)
    "cancelada" -> Color(0xFFE53935)
    else -> Color(0xFFFFA726)
}

@Composable
private fun ReservaResumenItem(reserva: Reserva, onMarcarLlegado: () -> Unit) {
    val s = LocalStrings.current
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
                    contentDescription = if (llegado) s.desmarcarLlegado else s.marcarComoLlegado,
                    tint = if (llegado) verde else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

private val COLORES_EMPLEADOS = listOf(
    Color(0xFF3B82F6), Color(0xFF22C55E), Color(0xFFF97316),
    Color(0xFF8B5CF6), Color(0xFF06B6D4), Color(0xFFEAB308), Color(0xFFEF4444)
)

private fun iniciales(nombre: String): String =
    nombre.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }

@Composable
fun TrabajadorCard(trabajador: Trabajador, onClick: () -> Unit) {
    val s = LocalStrings.current
    val accentColor = if (trabajador.rol == "admin") {
        Color(0xFF1C1917)
    } else {
        COLORES_EMPLEADOS[trabajador.id % COLORES_EMPLEADOS.size]
    }
    val rolTexto = when (trabajador.rol) {
        "admin"  -> s.rolAdmin
        "cocina" -> s.rolCocina
        else     -> s.rolCamarero
    }
    val initials = iniciales(trabajador.nombre)

    Surface(
        onClick    = onClick,
        modifier   = Modifier.fillMaxWidth().aspectRatio(1f),
        shape      = RoundedCornerShape(18.dp),
        color      = MaterialTheme.colorScheme.surface,
        border     = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Círculo con iniciales
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = initials,
                    color     = Color.White,
                    fontSize  = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text       = trabajador.nombre,
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.Center,
                maxLines   = 2,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = rolTexto,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
