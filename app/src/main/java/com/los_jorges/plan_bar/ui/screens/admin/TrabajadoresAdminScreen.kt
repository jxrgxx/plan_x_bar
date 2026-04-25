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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.Trabajador
import com.los_jorges.plan_bar.viewmodel.TrabajadoresViewModel

private val ROLES = listOf("camarero", "cocina", "admin")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrabajadoresAdminScreen(
    restauranteId: Int,
    onBack: () -> Unit,
    vm: TrabajadoresViewModel = viewModel()
) {
    val trabajadores by vm.trabajadores.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var trabajadorEditar by remember { mutableStateOf<Trabajador?>(null) }
    var trabajadorEliminar by remember { mutableStateOf<Trabajador?>(null) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(restauranteId) { vm.cargar(restauranteId) }
    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trabajadores") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { trabajadorEditar = null; showDialog = true }) {
                        Icon(Icons.Default.Add, "Nuevo trabajador")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (loading) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
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
                            "No hay trabajadores. Pulsa + para añadir.",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            items(trabajadores, key = { it.id }) { t ->
                TrabajadorItem(
                    trabajador = t,
                    onEditar = { trabajadorEditar = t; showDialog = true },
                    onEliminar = { trabajadorEliminar = t }
                )
            }
        }
    }

    if (showDialog) {
        TrabajadorDialog(
            trabajador = trabajadorEditar,
            onDismiss = { showDialog = false },
            onConfirm = { nombre, rol, email, activo, password ->
                if (trabajadorEditar == null) {
                    vm.crear(restauranteId, nombre, rol, email, password) { ok, err ->
                        snackMsg = if (ok) "Trabajador creado" else err ?: "Error"
                    }
                } else {
                    vm.editar(
                        restauranteId,
                        trabajadorEditar!!.id,
                        nombre,
                        rol,
                        email,
                        activo,
                        password
                    ) { ok, err ->
                        snackMsg = if (ok) "Trabajador actualizado" else err ?: "Error"
                    }
                }
                showDialog = false
            }
        )
    }

    trabajadorEliminar?.let { t ->
        AlertDialog(
            onDismissRequest = { trabajadorEliminar = null },
            title = { Text("Eliminar trabajador") },
            text = { Text("¿Eliminar a ${t.nombre}?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.eliminar(restauranteId, t.id) { ok, err ->
                        snackMsg = if (ok) "Trabajador eliminado" else err ?: "Error"
                    }
                    trabajadorEliminar = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = {
                    trabajadorEliminar = null
                }) { Text("Cancelar") }
            }
        )
    }

    error?.let { snackMsg = it; vm.clearError() }
}

@Composable
private fun TrabajadorItem(trabajador: Trabajador, onEditar: () -> Unit, onEliminar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(trabajador.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${trabajador.rol} · ${trabajador.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (!trabajador.activo)
                    Text(
                        "Inactivo", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
            }
            if (trabajador.rol != "admin") {
                IconButton(onClick = onEditar) { Icon(Icons.Default.Edit, "Editar") }
                IconButton(onClick = onEliminar) {
                    Icon(
                        Icons.Default.Delete,
                        "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrabajadorDialog(
    trabajador: Trabajador?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean, String) -> Unit
) {
    var nombre by remember { mutableStateOf(trabajador?.nombre ?: "") }
    var rol by remember { mutableStateOf(trabajador?.rol ?: ROLES[0]) }
    var email by remember { mutableStateOf(trabajador?.email ?: "") }
    var activo by remember { mutableStateOf(trabajador?.activo ?: true) }
    var password by remember { mutableStateOf("") }
    var expandedRol by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (trabajador == null) "Nuevo trabajador" else "Editar trabajador") },
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
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expandedRol,
                    onExpandedChange = { expandedRol = it }) {
                    OutlinedTextField(
                        value = rol.replaceFirstChar { it.uppercase() },
                        onValueChange = {}, readOnly = true,
                        label = { Text("Rol") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedRol) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRol,
                        onDismissRequest = { expandedRol = false }) {
                        ROLES.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.replaceFirstChar { it.uppercase() }) },
                                onClick = { rol = r; expandedRol = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text(if (trabajador == null) "Contraseña *" else "Nueva contraseña (opcional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (trabajador != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = activo, onCheckedChange = { activo = it })
                        Text("Activo")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val passOk = trabajador != null || password.isNotBlank()
                if (nombre.isNotBlank() && email.isNotBlank() && passOk)
                    onConfirm(nombre, rol, email, activo, password)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
