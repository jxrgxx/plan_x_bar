package com.los_jorges.plan_bar.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.Mesa
import com.los_jorges.plan_bar.viewmodel.MesasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MesasAdminScreen(
    restauranteId: Int,
    onBack: () -> Unit,
    vm: MesasViewModel = viewModel()
) {
    val mesas by vm.mesas.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var mesaEditar by remember { mutableStateOf<Mesa?>(null) }
    var mesaEliminar by remember { mutableStateOf<Mesa?>(null) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(restauranteId) { vm.cargar(restauranteId) }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesas") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { mesaEditar = null; showDialog = true }) {
                        Icon(Icons.Default.Add, "Nueva mesa")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (mesas.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No hay mesas. Pulsa + para añadir.", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            items(mesas, key = { it.id }) { mesa ->
                MesaItem(
                    mesa = mesa,
                    onEditar = { mesaEditar = mesa; showDialog = true },
                    onEliminar = { mesaEliminar = mesa }
                )
            }
        }
    }

    if (showDialog) {
        MesaDialog(
            mesa = mesaEditar,
            onDismiss = { showDialog = false },
            onConfirm = { codigo, capacidad ->
                if (mesaEditar == null) {
                    vm.crear(restauranteId, codigo, capacidad) { ok, err ->
                        snackMsg = if (ok) "Mesa creada" else err ?: "Error"
                    }
                } else {
                    vm.editar(restauranteId, mesaEditar!!.id, codigo, capacidad) { ok, err ->
                        snackMsg = if (ok) "Mesa actualizada" else err ?: "Error"
                    }
                }
                showDialog = false
            }
        )
    }

    mesaEliminar?.let { mesa ->
        AlertDialog(
            onDismissRequest = { mesaEliminar = null },
            title = { Text("Eliminar mesa") },
            text = { Text("¿Eliminar la mesa ${mesa.codigo}?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.eliminar(restauranteId, mesa.id) { ok, err ->
                        snackMsg = if (ok) "Mesa eliminada" else err ?: "Error"
                    }
                    mesaEliminar = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { mesaEliminar = null }) { Text("Cancelar") }
            }
        )
    }

    error?.let { snackMsg = it; vm.clearError() }
}

@Composable
private fun MesaItem(mesa: Mesa, onEditar: () -> Unit, onEliminar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(mesa.codigo, style = MaterialTheme.typography.titleMedium)
                Text("${mesa.capacidad} personas · ${mesa.estado}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onEditar) { Icon(Icons.Default.Edit, "Editar") }
            IconButton(onClick = onEliminar) { Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun MesaDialog(mesa: Mesa?, onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var codigo by remember { mutableStateOf(mesa?.codigo ?: "") }
    var capacidad by remember { mutableStateOf(mesa?.capacidad?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mesa == null) "Nueva mesa" else "Editar mesa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = codigo, onValueChange = { codigo = it },
                    label = { Text("Código (ej: M01)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = capacidad, onValueChange = { capacidad = it },
                    label = { Text("Capacidad (personas)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cap = capacidad.toIntOrNull() ?: 0
                if (codigo.isNotBlank() && cap > 0) onConfirm(codigo, cap)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
