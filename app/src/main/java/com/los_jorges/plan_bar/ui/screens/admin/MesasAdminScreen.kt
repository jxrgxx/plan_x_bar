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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.los_jorges.plan_bar.model.Mesa
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.viewmodel.MesasViewModel
import com.los_jorges.plan_bar.viewmodel.ZonasViewModel
import com.los_jorges.plan_bar.ui.theme.Platinum40
import com.los_jorges.plan_bar.ui.theme.premiumInputColors
import com.los_jorges.plan_bar.ui.theme.LocalStrings

private val MesaAccent = Platinum40
private val LibreColor = Color(0xFF83C9A5)
private val OcupadaColor = Color(0xFFE57373)
private val ReservadaColor = Color(0xFFD4A853)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MesasAdminScreen(
    restauranteId: Int,
    onBack: () -> Unit,
    onPlano: () -> Unit = {},
    vm: MesasViewModel = viewModel(),
    zonasVm: ZonasViewModel = viewModel()
) {
    val s = LocalStrings.current
    val mesas by vm.mesas.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    val zonasDB by zonasVm.zonas.collectAsState()
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
    LaunchedEffect(zonas) {
        if (zonas.isNotEmpty() && zonas.none { it.first == zonaActual }) zonaActual =
            zonas.first().first
    }

    var showDialog by remember { mutableStateOf(false) }
    var mesaEditar by remember { mutableStateOf<Mesa?>(null) }
    var mesaEliminar by remember { mutableStateOf<Mesa?>(null) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(restauranteId) {
        while (true) {
            vm.cargar(restauranteId)
            zonasVm.cargar(restauranteId)
            delay(5_000)
        }
    }
    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            s.mesas,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            s.disposicionSalon, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onPlano) {
                        Icon(
                            Icons.Default.Dashboard, s.verPlano,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { mesaEditar = null; showDialog = true }) {
                        Icon(
                            Icons.Default.Add, s.nuevaMesa,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (zonas.size > 1) {
                TabRow(
                    selectedTabIndex = zonas.indexOfFirst { it.first == zonaActual }
                        .coerceAtLeast(0),
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
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MesaAccent)
                }
            } else {
                val mesasFiltradas = mesas.filter { it.zona == zonaActual }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    if (mesasFiltradas.isEmpty()) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    s.sinMesasEnZona,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    items(mesasFiltradas, key = { it.id }) { mesa ->
                        MesaItem(
                            mesa = mesa,
                            onEditar = { mesaEditar = mesa; showDialog = true },
                            onEliminar = { mesaEliminar = mesa }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        MesaDialog(
            mesa = mesaEditar,
            onDismiss = { showDialog = false },
            onConfirm = { codigo, capacidad, estado, zonaId ->
                if (mesaEditar == null) {
                    vm.crear(restauranteId, codigo, capacidad, zonaId) { ok, err ->
                        snackMsg = if (ok) s.mesaCreada else err ?: "Error"
                    }
                } else {
                    vm.editar(
                        restauranteId, mesaEditar!!.id, codigo, capacidad, estado,
                        mesaEditar!!.posX, mesaEditar!!.posY, zonaId
                    ) { ok, err ->
                        snackMsg = if (ok) s.mesaActualizada else err ?: "Error"
                    }
                }
                showDialog = false
            }
        )
    }

    mesaEliminar?.let { mesa ->
        AlertDialog(
            onDismissRequest = { mesaEliminar = null },
            icon = { Icon(Icons.Default.TableBar, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(s.eliminarMesa) },
            text = { Text("${s.confirmarEliminarMesa} ${mesa.codigo}?") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.eliminar(restauranteId, mesa.id) { ok, err ->
                            snackMsg = if (ok) s.mesaEliminadaOk else err ?: "Error"
                        }
                        mesaEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(s.eliminar) }
            },
            dismissButton = { TextButton(onClick = { mesaEliminar = null }) { Text(s.cancelar) } }
        )
    }

    error?.let { snackMsg = it; vm.clearError() }
}

// ── Item de mesa ──────────────────────────────────────────────────────────────

@Composable
private fun MesaItem(mesa: Mesa, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val s = LocalStrings.current
    val estadoColor = when (mesa.estado) {
        "ocupada" -> OcupadaColor
        "reservada" -> ReservadaColor
        else -> LibreColor
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MesaAccent.copy(alpha = 0.12f))
                    .border(1.dp, MesaAccent.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.TableBar,
                    null,
                    tint = MesaAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    mesa.codigo, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "${mesa.capacidad} ${s.personasLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "·", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        mesa.estado, style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium, color = estadoColor
                    )
                }
            }
            IconButton(onClick = onEditar, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Edit, s.editar, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete, s.eliminar, tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── Diálogo crear / editar mesa ───────────────────────────────────────────────

private val ESTADOS_MESA = listOf("libre", "ocupada", "reservada")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MesaDialog(
    mesa: Mesa?,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String, Int) -> Unit   // código, capacidad, estado, zona_id
) {
    val s = LocalStrings.current
    val zonas = SessionManager.zonasActivas          // List<Zona> con ids reales
    var codigo by remember { mutableStateOf(mesa?.codigo ?: "") }
    var capacidad by remember { mutableStateOf(mesa?.capacidad?.toString() ?: "") }
    var estado by remember { mutableStateOf(mesa?.estado ?: "libre") }
    var zonaId by remember {
        mutableStateOf(
            if (mesa != null && mesa.zona_id != 0) mesa.zona_id
            else zonas.firstOrNull()?.id ?: 0
        )
    }
    var estadoExpanded by remember { mutableStateOf(false) }
    var zonaExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
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
                // ── Cabecera ──────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MesaAccent.copy(alpha = 0.07f))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MesaAccent.copy(alpha = 0.15f))
                            .border(
                                1.dp,
                                MesaAccent.copy(alpha = 0.30f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.TableBar,
                            null,
                            tint = MesaAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            if (mesa == null) s.nuevaMesa else s.editarMesa,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (mesa == null) s.anadirNuevaMesa else "${s.modificando} ${mesa.codigo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ── Campos ────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = codigo, onValueChange = { codigo = it },
                        label = { Text(s.codigoMesa) }, singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Tag,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
                    )
                    OutlinedTextField(
                        value = capacidad, onValueChange = { capacidad = it },
                        label = { Text(s.capacidadPersonas) }, singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Group,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
                    )
                    if (zonas.size > 1) {
                        ExposedDropdownMenuBox(
                            expanded = zonaExpanded,
                            onExpandedChange = { zonaExpanded = it }) {
                            OutlinedTextField(
                                value = zonas.firstOrNull { it.id == zonaId }?.nombre ?: "",
                                onValueChange = {}, readOnly = true, label = { Text(s.espacio) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.GridView,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zonaExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
                            )
                            ExposedDropdownMenu(
                                expanded = zonaExpanded,
                                onDismissRequest = { zonaExpanded = false }) {
                                zonas.forEach { z ->
                                    DropdownMenuItem(
                                        text = { Text(z.nombre) },
                                        onClick = { zonaId = z.id; zonaExpanded = false })
                                }
                            }
                        }
                    }
                    if (mesa != null) {
                        val estadoCol = when (estado) {
                            "ocupada" -> OcupadaColor
                            "reservada" -> ReservadaColor
                            else -> LibreColor
                        }
                        ExposedDropdownMenuBox(
                            expanded = estadoExpanded,
                            onExpandedChange = { estadoExpanded = it }) {
                            OutlinedTextField(
                                value = estado, onValueChange = {}, readOnly = true,
                                label = { Text(s.estado) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(estadoCol)
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = estadoExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
                            )
                            ExposedDropdownMenu(
                                expanded = estadoExpanded,
                                onDismissRequest = { estadoExpanded = false }) {
                                ESTADOS_MESA.forEach { opcion ->
                                    val col = when (opcion) {
                                        "ocupada" -> OcupadaColor; "reservada" -> ReservadaColor; else -> LibreColor
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                opcion,
                                                color = col,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        onClick = { estado = opcion; estadoExpanded = false })
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ── Acciones ──────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) { Text(s.cancelar) }
                    Button(
                        onClick = {
                            val cap = capacidad.toIntOrNull() ?: 0
                            if (codigo.isNotBlank() && cap > 0 && zonaId != 0) onConfirm(
                                codigo,
                                cap,
                                estado,
                                zonaId
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s.guardar, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
