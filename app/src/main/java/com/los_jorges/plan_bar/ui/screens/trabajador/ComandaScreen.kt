package com.los_jorges.plan_bar.ui.screens.trabajador

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.PedidoProducto
import com.los_jorges.plan_bar.model.Producto
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.viewmodel.PedidosViewModel
import com.los_jorges.plan_bar.viewmodel.ProductosViewModel

private val METODOS_PAGO = listOf("efectivo", "tarjeta", "otro")
private val CATEGORIAS = listOf("bebida", "entrante", "principal", "postre")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComandaScreen(
    pedidoId: Int,
    onBack: () -> Unit,
    pedidosVm: PedidosViewModel = viewModel(),
    productosVm: ProductosViewModel = viewModel()
) {
    val pedido by pedidosVm.pedido.collectAsState()
    val loading by pedidosVm.loading.collectAsState()
    val error by pedidosVm.error.collectAsState()
    val productos by productosVm.productos.collectAsState()

    var showSelectorProductos  by remember { mutableStateOf(false) }
    var productoSeleccionado   by remember { mutableStateOf<Producto?>(null) }
    var showCobrarDialog       by remember { mutableStateOf(false) }
    var showCancelarDialog     by remember { mutableStateOf(false) }
    var snackMsg               by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val pedidoVacio = pedido?.productos?.isEmpty() != false

    // Función de volver: si el pedido está vacío pregunta antes de cancelar
    val handleBack: () -> Unit = {
        if (pedidoVacio) showCancelarDialog = true else onBack()
    }

    LaunchedEffect(pedidoId) { pedidosVm.cargarPedido(pedidoId) }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    LaunchedEffect(error) {
        error?.let { snackMsg = it; pedidosVm.clearError() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesa ${pedido?.mesa_codigo ?: "..."}") },
                navigationIcon = {
                    IconButton(onClick = handleBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (loading && pedido == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Lista de líneas del pedido ──────────────────────────────────
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val lineas = pedido?.productos ?: emptyList()
                if (lineas.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Sin productos aún", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    items(lineas, key = { it.id }) { linea ->
                        LineaPedidoItem(
                            linea = linea,
                            onEliminar = {
                                pedidosVm.eliminarProducto(linea.id, pedidoId) { ok, err ->
                                    if (!ok) snackMsg = err ?: "Error al eliminar"
                                }
                            },
                            onCambiarCantidad = { nuevaCantidad ->
                                pedidosVm.actualizarCantidad(linea.id, nuevaCantidad, pedidoId) { ok, err ->
                                    if (!ok) snackMsg = err ?: "Error al actualizar"
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }

            // ── Barra inferior ──────────────────────────────────────────────
            HorizontalDivider()
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "%.2f €".format(pedido?.total ?: 0.0),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            productosVm.cargar(SessionManager.restauranteId)
                            showSelectorProductos = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Añadir")
                    }
                    Button(
                        onClick = { showCobrarDialog = true },
                        modifier = Modifier.weight(1f),
                        enabled = pedido?.productos?.isNotEmpty() == true
                    ) {
                        Text("Cobrar")
                    }
                }
            }
        }
    }

    // ── Selector de productos (bottom sheet) ─────────────────────────────────
    if (showSelectorProductos) {
        ModalBottomSheet(onDismissRequest = { showSelectorProductos = false }) {
            Text(
                "Carta",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                CATEGORIAS.forEach { cat ->
                    val lista = productos.filter { it.categoria == cat }
                    if (lista.isNotEmpty()) {
                        item {
                            Text(
                                cat.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(lista, key = { it.id }) { producto ->
                            ProductoSelectorItem(producto) { productoSeleccionado = producto }
                        }
                    }
                }
            }
        }
    }

    // ── Diálogo cantidad + observaciones ─────────────────────────────────────
    productoSeleccionado?.let { producto ->
        DetalleProductoDialog(
            producto = producto,
            onDismiss = { productoSeleccionado = null },
            onConfirm = { cantidad, observaciones ->
                pedidosVm.agregarProducto(
                    pedidoId,
                    producto.id,
                    cantidad,
                    observaciones
                ) { ok, err ->
                    snackMsg = if (ok) "${producto.nombre} añadido" else err ?: "Error"
                }
                productoSeleccionado = null
            }
        )
    }

    // ── Diálogo cancelar pedido vacío ─────────────────────────────────────────
    if (showCancelarDialog) {
        AlertDialog(
            onDismissRequest = { showCancelarDialog = false },
            title = { Text("¿Cancelar pedido?") },
            text  = { Text("El pedido está vacío. ¿Quieres cancelarlo y liberar la mesa?") },
            confirmButton = {
                TextButton(onClick = {
                    pedidosVm.cancelarPedido(pedidoId) { ok, err ->
                        if (ok) onBack() else snackMsg = err ?: "Error al cancelar"
                    }
                    showCancelarDialog = false
                }) { Text("Cancelar pedido", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelarDialog = false }) { Text("Mantener") }
            }
        )
    }

    // ── Diálogo cobrar ────────────────────────────────────────────────────────
    if (showCobrarDialog) {
        CobrarDialog(
            total = pedido?.total ?: 0.0,
            onDismiss = { showCobrarDialog = false },
            onConfirm = { metodoPago ->
                pedidosVm.cerrarPedido(pedidoId, metodoPago) { ok, err ->
                    if (ok) onBack() else snackMsg = err ?: "Error al cobrar"
                }
                showCobrarDialog = false
            }
        )
    }
}

// ─── Composables auxiliares ──────────────────────────────────────────────────

@Composable
private fun LineaPedidoItem(
    linea: PedidoProducto,
    onEliminar: () -> Unit,
    onCambiarCantidad: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nombre + observaciones
        Column(modifier = Modifier.weight(1f)) {
            Text(linea.nombre, style = MaterialTheme.typography.bodyMedium)
            if (!linea.observaciones.isNullOrBlank()) {
                Text(
                    linea.observaciones,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Stepper de cantidad
        IconButton(
            onClick = {
                if (linea.cantidad > 1) onCambiarCantidad(linea.cantidad - 1)
                else onEliminar()
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Remove, "Menos", modifier = Modifier.size(16.dp))
        }
        Text(
            "${linea.cantidad}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        IconButton(
            onClick = { onCambiarCantidad(linea.cantidad + 1) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Add, "Más", modifier = Modifier.size(16.dp))
        }

        // Precio total de la línea
        Text(
            "%.2f €".format(linea.cantidad * linea.precio_unitario),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 6.dp)
        )

        // Eliminar línea completa
        IconButton(onClick = onEliminar) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Eliminar",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ProductoSelectorItem(producto: Producto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(producto.nombre, style = MaterialTheme.typography.bodyMedium)
            Text(
                "%.2f €".format(producto.precio),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        FilledTonalIconButton(onClick = onClick) {
            Icon(Icons.Default.Add, "Añadir")
        }
    }
}

@Composable
private fun DetalleProductoDialog(
    producto: Producto,
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    var cantidad by remember { mutableStateOf("1") }
    var observaciones by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(producto.nombre) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "%.2f €".format(producto.precio),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { if (it.all { c -> c.isDigit() }) cantidad = it },
                    label = { Text("Cantidad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = observaciones,
                    onValueChange = { observaciones = it },
                    label = { Text("Observaciones (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cant = cantidad.toIntOrNull() ?: 0
                if (cant > 0) onConfirm(cant, observaciones)
            }) { Text("Añadir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun CobrarDialog(
    total: Double,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var metodoPago by remember { mutableStateOf("efectivo") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cobrar pedido") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Total: %.2f €".format(total),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                METODOS_PAGO.forEach { metodo ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = metodoPago == metodo,
                            onClick = { metodoPago = metodo }
                        )
                        Text(metodo.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(metodoPago) }) { Text("Cobrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
