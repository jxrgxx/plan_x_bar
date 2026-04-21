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
import com.los_jorges.plan_bar.model.Producto
import com.los_jorges.plan_bar.viewmodel.ProductosViewModel

private val CATEGORIAS = listOf("entrante", "principal", "postre", "bebida")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductosAdminScreen(
    restauranteId: Int,
    onBack: () -> Unit,
    vm: ProductosViewModel = viewModel()
) {
    val productos by vm.productos.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var productoEditar by remember { mutableStateOf<Producto?>(null) }
    var productoEliminar by remember { mutableStateOf<Producto?>(null) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(restauranteId) { vm.cargar(restauranteId) }
    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Productos") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { productoEditar = null; showDialog = true }) {
                        Icon(Icons.Default.Add, "Nuevo producto")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        val agrupados = productos.groupBy { it.categoria }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (productos.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No hay productos. Pulsa + para añadir.", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            CATEGORIAS.forEach { cat ->
                val lista = agrupados[cat] ?: return@forEach
                item {
                    Text(cat.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
                items(lista, key = { it.id }) { producto ->
                    ProductoItem(
                        producto = producto,
                        onEditar = { productoEditar = producto; showDialog = true },
                        onEliminar = { productoEliminar = producto }
                    )
                }
            }
        }
    }

    if (showDialog) {
        ProductoDialog(
            producto = productoEditar,
            onDismiss = { showDialog = false },
            onConfirm = { nombre, categoria, descripcion, precio, disponible ->
                if (productoEditar == null) {
                    vm.crear(restauranteId, nombre, categoria, descripcion, precio, disponible) { ok, err ->
                        snackMsg = if (ok) "Producto creado" else err ?: "Error"
                    }
                } else {
                    vm.editar(restauranteId, productoEditar!!.id, nombre, categoria, descripcion, precio, disponible) { ok, err ->
                        snackMsg = if (ok) "Producto actualizado" else err ?: "Error"
                    }
                }
                showDialog = false
            }
        )
    }

    productoEliminar?.let { p ->
        AlertDialog(
            onDismissRequest = { productoEliminar = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Eliminar \"${p.nombre}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.eliminar(restauranteId, p.id) { ok, err ->
                        snackMsg = if (ok) "Producto eliminado" else err ?: "Error"
                    }
                    productoEliminar = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { productoEliminar = null }) { Text("Cancelar") } }
        )
    }

    error?.let { snackMsg = it; vm.clearError() }
}

@Composable
private fun ProductoItem(producto: Producto, onEditar: () -> Unit, onEliminar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(producto.nombre, style = MaterialTheme.typography.titleMedium)
                Text("%.2f €".format(producto.precio), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
                if (!producto.disponible)
                    Text("No disponible", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = onEditar) { Icon(Icons.Default.Edit, "Editar") }
            IconButton(onClick = onEliminar) { Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductoDialog(
    producto: Producto?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double, Boolean) -> Unit
) {
    var nombre      by remember { mutableStateOf(producto?.nombre ?: "") }
    var categoria   by remember { mutableStateOf(producto?.categoria ?: CATEGORIAS[0]) }
    var descripcion by remember { mutableStateOf(producto?.descripcion ?: "") }
    var precio      by remember { mutableStateOf(producto?.precio?.toString() ?: "") }
    var disponible  by remember { mutableStateOf(producto?.disponible ?: true) }
    var expandedCat by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (producto == null) "Nuevo producto" else "Editar producto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre *") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = expandedCat, onExpandedChange = { expandedCat = it }) {
                    OutlinedTextField(
                        value = categoria.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCat) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                        CATEGORIAS.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.replaceFirstChar { it.uppercase() }) },
                                onClick = { categoria = cat; expandedCat = false }
                            )
                        }
                    }
                }

                OutlinedTextField(value = descripcion, onValueChange = { descripcion = it },
                    label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())

                OutlinedTextField(value = precio, onValueChange = { precio = it },
                    label = { Text("Precio (€) *") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth())

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = disponible, onCheckedChange = { disponible = it })
                    Text("Disponible")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = precio.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (nombre.isNotBlank() && p > 0) onConfirm(nombre, categoria, descripcion, p, disponible)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
