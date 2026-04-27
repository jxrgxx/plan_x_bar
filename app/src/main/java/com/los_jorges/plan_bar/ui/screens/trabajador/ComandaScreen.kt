package com.los_jorges.plan_bar.ui.screens.trabajador

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.PedidoProducto
import com.los_jorges.plan_bar.model.Producto
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.viewmodel.PedidosViewModel
import com.los_jorges.plan_bar.viewmodel.ProductosViewModel

private val METODOS_PAGO = listOf("efectivo", "tarjeta", "otro")
private val CATEGORIAS = listOf("bebida", "entrante", "primero", "segundo", "postre")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComandaScreen(
    mesaId: Int,
    mesaCodigo: String,
    onBack: () -> Unit,
    pedidosVm: PedidosViewModel = viewModel(),
    productosVm: ProductosViewModel = viewModel()
) {
    val trabajador by SessionManager.trabajador.collectAsState()
    val pedido by pedidosVm.pedido.collectAsState()
    val loading by pedidosVm.loading.collectAsState()
    val error by pedidosVm.error.collectAsState()
    val productos by productosVm.productos.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var productoSeleccionado by remember { mutableStateOf<Producto?>(null) }
    var showCobrarDialog by remember { mutableStateOf(false) }
    var showCancelarDialog by remember { mutableStateOf(false) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val pedidoVacio = pedido?.productos?.isEmpty() == true

    LaunchedEffect(mesaId) {
        pedidosVm.cargarPedidoPorMesa(mesaId)
        productosVm.cargar(SessionManager.restauranteId)
    }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    LaunchedEffect(error) {
        error?.let { snackMsg = it; pedidosVm.clearError() }
    }

    val tabs = CATEGORIAS.filter { cat -> productos.any { it.categoria == cat } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(mesaCodigo, style = MaterialTheme.typography.titleLarge)
                        pedido?.estado?.let { estado ->
                            Text(
                                estado.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    if (pedido == null) {
                        TextButton(
                            onClick = {
                                pedidosVm.crearNuevoPedido(
                                    restauranteId = SessionManager.restauranteId,
                                    mesaId = mesaId,
                                    trabajadorId = trabajador?.id
                                )
                            },
                            enabled = !loading
                        ) { Text("Crear pedido") }
                    } else if (pedidoVacio) {
                        TextButton(onClick = { showCancelarDialog = true }) {
                            Text("Cancelar pedido", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        TextButton(onClick = { /* TODO: pantalla cocinero */ }) {
                            Text("Enviar")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Mitad superior: pedido actual ───────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                when {
                    loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    pedido == null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Sin pedido abierto",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    else -> {
                        @Suppress("NAME_SHADOWING")
                        val pedido = pedido!!

                        Column(Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val lineas = pedido.productos
                                if (lineas.isEmpty()) {
                                    item {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Sin productos aún",
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                } else {
                                    items(lineas, key = { it.id }) { linea ->
                                        LineaPedidoItem(
                                            linea = linea,
                                            onEliminar = {
                                                pedidosVm.eliminarProducto(
                                                    linea.id,
                                                    pedido.id
                                                ) { ok, err ->
                                                    if (!ok) snackMsg = err ?: "Error al eliminar"
                                                }
                                            },
                                            onCambiarCantidad = { nuevaCantidad ->
                                                pedidosVm.actualizarCantidad(
                                                    linea.id,
                                                    nuevaCantidad,
                                                    pedido.id
                                                ) { ok, err ->
                                                    if (!ok) snackMsg = err ?: "Error al actualizar"
                                                }
                                            }
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            }

                            // Total + Cobrar
                            HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total  %.2f €".format(pedido.total ?: 0.0),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Button(
                                    onClick = { showCobrarDialog = true },
                                    enabled = pedido.productos.isNotEmpty()
                                ) {
                                    Text("Cobrar")
                                }
                            }
                        }
                    }
                }
            }

            // ── Mitad inferior: carta siempre visible ───────────────────────
            HorizontalDivider(thickness = 2.dp)

            if (tabs.isNotEmpty()) {
                ScrollableTabRow(selectedTabIndex = selectedTab.coerceAtMost(tabs.lastIndex)) {
                    tabs.forEachIndexed { index, cat ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(cat.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                val listaActual = productos.filter {
                    it.categoria == tabs[selectedTab.coerceAtMost(tabs.lastIndex)]
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(listaActual, key = { it.id }) { producto ->
                        ProductoSelectorItem(
                            producto = producto,
                            enabled = pedido != null,
                            onClick = {
                                if (pedido != null) productoSeleccionado = producto
                                else snackMsg = "Crea el pedido primero"
                            }
                        )
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
                pedido?.let { p ->
                    pedidosVm.agregarProducto(
                        p.id,
                        producto.id,
                        cantidad,
                        observaciones
                    ) { ok, err ->
                        snackMsg = if (ok) "${producto.nombre} añadido" else err ?: "Error"
                    }
                }
                productoSeleccionado = null
            }
        )
    }

    // ── Diálogo cancelar pedido ───────────────────────────────────────────────
    if (showCancelarDialog) {
        AlertDialog(
            onDismissRequest = { showCancelarDialog = false },
            title = { Text("¿Cancelar pedido?") },
            text = { Text("¿Quieres cancelar el pedido y liberar la mesa?") },
            confirmButton = {
                TextButton(onClick = {
                    pedido?.let { p ->
                        pedidosVm.cancelarPedido(p.id) { ok, err ->
                            if (ok) onBack() else snackMsg = err ?: "Error al cancelar"
                        }
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
                pedido?.let { p ->
                    pedidosVm.cerrarPedido(p.id, metodoPago) { ok, err ->
                        if (ok) onBack() else snackMsg = err ?: "Error al cobrar"
                    }
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
        Text(
            "%.2f €".format(linea.cantidad * linea.precio_unitario),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 6.dp)
        )
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
private fun ProductoSelectorItem(producto: Producto, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                producto.nombre,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                "%.2f €".format(producto.precio),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        FilledTonalIconButton(onClick = onClick, enabled = enabled) {
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
    var cantidad by remember { mutableStateOf(1) }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (cantidad > 1) cantidad-- },
                        enabled = cantidad > 1
                    ) {
                        Icon(Icons.Default.Remove, "Menos")
                    }
                    Text(
                        "$cantidad",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    IconButton(onClick = { cantidad++ }) {
                        Icon(Icons.Default.Add, "Más")
                    }
                }
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
                onConfirm(cantidad, observaciones)
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
