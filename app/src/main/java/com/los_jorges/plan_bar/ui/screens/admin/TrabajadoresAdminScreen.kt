package com.los_jorges.plan_bar.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.los_jorges.plan_bar.model.Trabajador
import com.los_jorges.plan_bar.viewmodel.TrabajadoresViewModel
import com.los_jorges.plan_bar.ui.theme.Platinum40
import com.los_jorges.plan_bar.ui.theme.premiumInputColors
import com.los_jorges.plan_bar.ui.theme.LocalStrings

private val ROLES = listOf("camarero", "cocina", "admin")
private val TrabajadorAccent = Color(0xFF8B7AE8)

private fun rolIcon(rol: String): ImageVector = when (rol) {
    "admin" -> Icons.Default.AdminPanelSettings
    "cocina" -> Icons.Default.OutdoorGrill
    else -> Icons.Default.Person
}

private fun rolColor(rol: String): Color = when (rol) {
    "admin" -> Platinum40
    "cocina" -> Color(0xFFF4A261)
    else -> TrabajadorAccent
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrabajadoresAdminScreen(
    restauranteId: Int,
    onBack: () -> Unit,
    vm: TrabajadoresViewModel = viewModel()
) {
    val s = LocalStrings.current
    val trabajadores by vm.trabajadores.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var trabajadorEditar by remember { mutableStateOf<Trabajador?>(null) }
    var trabajadorEliminar by remember { mutableStateOf<Trabajador?>(null) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(restauranteId) {
        while (true) {
            vm.cargar(restauranteId)
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
                            s.trabajadores,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            s.gestionaElEquipo, style = MaterialTheme.typography.labelSmall,
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
                    IconButton(onClick = { trabajadorEditar = null; showDialog = true }) {
                        Icon(
                            Icons.Default.Add, s.nuevoTrabajador,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (loading) {
            Box(Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TrabajadorAccent)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (trabajadores.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            s.sinTrabajadoresPulsaPlus,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            items(trabajadores, key = { it.id }) { t ->
                TrabajadorItem(
                    trabajador = t,
                    onEditar = { trabajadorEditar = t; showDialog = true },
                    onEliminar = { trabajadorEliminar = t })
            }
        }
    }

    if (showDialog) {
        TrabajadorDialog(
            trabajador = trabajadorEditar,
            onDismiss = { showDialog = false },
            onConfirm = { nombre, rol, email, activo, pin ->
                if (trabajadorEditar == null) {
                    vm.crear(restauranteId, nombre, rol, email, pin) { ok, err ->
                        snackMsg = if (ok) s.trabajadorCreado else err ?: "Error"
                    }
                } else {
                    vm.editar(
                        restauranteId,
                        trabajadorEditar!!.id,
                        nombre,
                        rol,
                        email,
                        activo,
                        pin
                    ) { ok, err ->
                        snackMsg = if (ok) s.trabajadorActualizado else err ?: "Error"
                    }
                }
                showDialog = false
            }
        )
    }

    trabajadorEliminar?.let { t ->
        AlertDialog(
            onDismissRequest = { trabajadorEliminar = null },
            icon = { Icon(Icons.Default.PersonOff, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(s.eliminarTrabajador) },
            text = { Text("${s.confirmarEliminarTrabajador} ${t.nombre}?") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.eliminar(restauranteId, t.id) { ok, err ->
                            snackMsg = if (ok) s.trabajadorEliminado else err ?: "Error"
                        }
                        trabajadorEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(s.eliminar) }
            },
            dismissButton = {
                TextButton(onClick = {
                    trabajadorEliminar = null
                }) { Text(s.cancelar) }
            }
        )
    }

    error?.let { snackMsg = it; vm.clearError() }
}

// ── Item de trabajador ────────────────────────────────────────────────────────

@Composable
private fun TrabajadorItem(trabajador: Trabajador, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val s = LocalStrings.current
    val accent = rolColor(trabajador.rol)
    val icon = rolIcon(trabajador.rol)
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
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    trabajador.nombre, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        trabajador.rol.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = accent
                    )
                    if (trabajador.email.isNotBlank()) {
                        Text(
                            "·",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            trabajador.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!trabajador.activo)
                    Text(
                        s.inactivo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
            }
            if (trabajador.rol != "admin") {
                IconButton(onClick = onEditar, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        s.editar,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        s.eliminar,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Diálogo crear / editar trabajador ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrabajadorDialog(
    trabajador: Trabajador?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean, String) -> Unit
) {
    val s = LocalStrings.current
    var nombre by remember { mutableStateOf(trabajador?.nombre ?: "") }
    var rol by remember { mutableStateOf(trabajador?.rol ?: ROLES[0]) }
    var email by remember { mutableStateOf(trabajador?.email ?: "") }
    var activo by remember { mutableStateOf(trabajador?.activo ?: true) }
    var pin by remember { mutableStateOf("") }
    var expandedRol by remember { mutableStateOf(false) }

    val accentColor = rolColor(rol)
    val rolIcono = rolIcon(rol)

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
                        .background(accentColor.copy(alpha = 0.07f))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(
                                1.dp,
                                accentColor.copy(alpha = 0.30f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(rolIcono, null, tint = accentColor, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(
                            if (trabajador == null) s.nuevoTrabajador else s.editarTrabajador,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (trabajador == null) s.anadirMiembro else "${s.modificando} ${trabajador.nombre}",
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
                        value = nombre, onValueChange = { nombre = it },
                        label = { Text(s.nombreObligatorio) }, singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Badge,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
                    )
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text(s.emailObligatorio) }, singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
                    )

                    // Selector de rol con icono dinámico
                    ExposedDropdownMenuBox(
                        expanded = expandedRol,
                        onExpandedChange = { expandedRol = it }) {
                        OutlinedTextField(
                            value = rol.replaceFirstChar { it.uppercase() },
                            onValueChange = {}, readOnly = true, label = { Text(s.rol) },
                            leadingIcon = {
                                Icon(
                                    rolIcono,
                                    null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedRol) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedRol,
                            onDismissRequest = { expandedRol = false }) {
                            ROLES.forEach { r ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                rolIcon(r),
                                                null,
                                                tint = rolColor(r),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(r.replaceFirstChar { it.uppercase() })
                                        }
                                    },
                                    onClick = { rol = r; expandedRol = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { v -> pin = v.filter { it.isDigit() }.take(6) },
                        label = { Text(if (trabajador == null) s.pinOpcional else s.nuevoPinOpcional) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Pin,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        supportingText = { Text(s.maxSeisDig) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
                    )

                    if (trabajador != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (activo) accentColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (activo) accentColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    s.trabajadorActivo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (activo) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = activo, onCheckedChange = { activo = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = accentColor,
                                        checkedTrackColor = accentColor.copy(alpha = 0.3f)
                                    )
                                )
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
                            if (nombre.isNotBlank() && email.isNotBlank())
                                onConfirm(nombre, rol, email, activo, pin)
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
