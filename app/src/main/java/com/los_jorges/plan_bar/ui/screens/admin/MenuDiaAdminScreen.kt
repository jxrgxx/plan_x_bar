package com.los_jorges.plan_bar.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.MenuDiaLineaRequest
import com.los_jorges.plan_bar.model.Producto
import com.los_jorges.plan_bar.ui.theme.DarkBase
import com.los_jorges.plan_bar.ui.theme.DarkSurface1
import com.los_jorges.plan_bar.ui.theme.WarmMuted
import com.los_jorges.plan_bar.ui.theme.WarmWhite
import com.los_jorges.plan_bar.viewmodel.MenuDiaViewModel
import com.los_jorges.plan_bar.viewmodel.ProductosViewModel

// Cursos disponibles con etiqueta y categoría de producto asociada
private val CURSOS = listOf(
    Triple("bebida", "Bebida", "bebida"),
    Triple("primero", "Primero", "primero"),
    Triple("segundo", "Segundo", "segundo"),
    Triple("postre", "Postre", "postre")
)

// Línea en edición local (antes de guardar)
private data class LineaLocal(
    val producto_id: Int,
    val nombre: String,
    val precio: Double,
    val curso: String,
    var cantidad: Int = 0   // 0 = sin límite
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuDiaAdminScreen(
    restauranteId: Int,
    onBack: () -> Unit
) {
    val menuVm: MenuDiaViewModel = viewModel()
    val productosVm: ProductosViewModel = viewModel()

    val menu by menuVm.menu.collectAsState()
    val loading by menuVm.loading.collectAsState()
    val productos by productosVm.productos.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var snackMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(restauranteId) {
        menuVm.cargar(restauranteId)
        productosVm.cargar(restauranteId)
    }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    // Estado local del formulario
    var precioTexto by remember { mutableStateOf("") }
    val lineas = remember { mutableStateListOf<LineaLocal>() }
    var inicializado by remember { mutableStateOf(false) }

    // Inicializar desde el menú guardado cuando llega
    LaunchedEffect(menu, productos) {
        if (!inicializado && menu != null && productos.isNotEmpty()) {
            if (precioTexto.isEmpty()) precioTexto = menu!!.precio.toString()
            lineas.clear()
            menu!!.lineas.forEach { l ->
                val prod = productos.firstOrNull { it.id == l.producto_id }
                lineas.add(
                    LineaLocal(
                        producto_id = l.producto_id,
                        nombre = l.nombre,
                        precio = prod?.precio ?: l.precio,
                        curso = l.curso,
                        cantidad = l.cantidad
                    )
                )
            }
            inicializado = true
        }
    }

    // Diálogo para añadir producto a un curso
    var cursoDialogo by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    Scaffold(
        containerColor = DarkBase,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Menú del Día", fontWeight = FontWeight.SemiBold, color = WarmWhite)
                        Text(
                            "Configura la oferta de hoy",
                            style = MaterialTheme.typography.labelSmall, color = WarmMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = WarmMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface1)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val precio = precioTexto.replace(',', '.').toDoubleOrNull()
                    if (precio == null || precio <= 0) {
                        snackMsg = null
                        snackMsg = "Introduce un precio válido"
                        return@ExtendedFloatingActionButton
                    }
                    if (loading) return@ExtendedFloatingActionButton
                    menuVm.guardar(
                        restauranteId = restauranteId,
                        precio = precio,
                        lineas = lineas.map {
                            MenuDiaLineaRequest(it.producto_id, it.curso, it.cantidad)
                        }
                    ) { ok, err ->
                        snackMsg = null
                        snackMsg = if (ok) "Menú guardado ✓" else err ?: "Error al guardar"
                        if (ok) {
                            inicializado = false
                            menuVm.cargar(restauranteId)
                        }
                    }
                },
                icon = { Icon(Icons.Default.Check, null) },
                text = { Text("Guardar menú") }
            )
        }
    ) { padding ->

        if (loading && menu == null && productos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Precio ────────────────────────────────────────────────────
            OutlinedTextField(
                value = precioTexto,
                onValueChange = { precioTexto = it.replace(',', '.') },
                label = { Text("Precio del menú (€)") },
                leadingIcon = { Icon(Icons.Default.Euro, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // ── Secciones por curso ───────────────────────────────────────
            CURSOS.forEach { (cursoKey, cursoLabel, categoriaProducto) ->
                val lineasCurso = lineas.filter { it.curso == cursoKey }
                val productosDisponibles =
                    productos.filter { it.categoria == categoriaProducto && it.disponible }
                // Excluir los ya añadidos
                val productosNoAñadidos = productosDisponibles.filter { p ->
                    lineasCurso.none { l -> l.producto_id == p.id }
                }

                CursoSection(
                    label = cursoLabel,
                    lineas = lineasCurso,
                    hayProductos = productosDisponibles.isNotEmpty(),
                    onCantidadCambia = { linea, nuevaCantidad ->
                        val idx = lineas.indexOf(linea)
                        if (idx >= 0) lineas[idx] = linea.copy(cantidad = nuevaCantidad)
                    },
                    onEliminar = { lineas.remove(it) },
                    onAñadir = {
                        if (productosNoAñadidos.isNotEmpty()) cursoDialogo =
                            Triple(cursoKey, cursoLabel, categoriaProducto)
                        else snackMsg = "No hay más productos de esta categoría"
                    }
                )
            }

            Spacer(Modifier.height(72.dp)) // espacio para FAB
        }
    }

    // ── Diálogo añadir producto ───────────────────────────────────────────────
    cursoDialogo?.let { (cursoKey, cursoLabel, categoriaProducto) ->
        val productosDisponibles =
            productos.filter { it.categoria == categoriaProducto && it.disponible }
        val productosNoAñadidos =
            productosDisponibles.filter { p -> lineas.none { it.curso == cursoKey && it.producto_id == p.id } }

        AñadirProductoDialog(
            cursoLabel = cursoLabel,
            opciones = productosNoAñadidos,
            onDismiss = { cursoDialogo = null },
            onConfirm = { producto, cantidad ->
                lineas.add(
                    LineaLocal(
                        producto_id = producto.id,
                        nombre = producto.nombre,
                        precio = producto.precio,
                        curso = cursoKey,
                        cantidad = cantidad
                    )
                )
                cursoDialogo = null
            }
        )
    }
}

// ── Sección de un curso ───────────────────────────────────────────────────────

@Composable
private fun CursoSection(
    label: String,
    lineas: List<LineaLocal>,
    hayProductos: Boolean,
    onCantidadCambia: (LineaLocal, Int) -> Unit,
    onEliminar: (LineaLocal) -> Unit,
    onAñadir: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cabecera del curso
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (hayProductos) {
                    FilledTonalIconButton(
                        onClick = onAñadir,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, "Añadir $label", modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (lineas.isEmpty()) {
                Text(
                    if (hayProductos) "Pulsa + para añadir opciones" else "Sin productos en esta categoría",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                lineas.forEach { linea ->
                    LineaItem(
                        linea = linea,
                        onCantidadCambia = { nueva -> onCantidadCambia(linea, nueva) },
                        onEliminar = { onEliminar(linea) }
                    )
                }
            }
        }
    }
}

// ── Fila de una línea ─────────────────────────────────────────────────────────

@Composable
private fun LineaItem(
    linea: LineaLocal,
    onCantidadCambia: (Int) -> Unit,
    onEliminar: () -> Unit
) {
    var cantidadTexto by remember(linea.producto_id, linea.curso) {
        mutableStateOf(if (linea.cantidad > 0) linea.cantidad.toString() else "")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                linea.nombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                "%.2f €".format(linea.precio),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        OutlinedTextField(
            value = cantidadTexto,
            onValueChange = { v ->
                cantidadTexto = v.filter { it.isDigit() }.take(4)
                onCantidadCambia(cantidadTexto.toIntOrNull() ?: 0)
            },
            label = { Text("Uds.") },
            placeholder = { Text("∞") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(90.dp)
        )

        IconButton(onClick = onEliminar) {
            Icon(
                Icons.Default.Delete, "Quitar",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

// ── Diálogo añadir producto ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AñadirProductoDialog(
    cursoLabel: String,
    opciones: List<Producto>,
    onDismiss: () -> Unit,
    onConfirm: (Producto, Int) -> Unit
) {
    var productoSeleccionado by remember { mutableStateOf<Producto?>(opciones.firstOrNull()) }
    var cantidadTexto by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir $cursoLabel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Selector de producto
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = productoSeleccionado?.nombre ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Producto") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        opciones.forEach { p ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(p.nombre)
                                        Text(
                                            "%.2f €".format(p.precio),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                },
                                onClick = { productoSeleccionado = p; expanded = false }
                            )
                        }
                    }
                }

                // Cantidad disponible
                OutlinedTextField(
                    value = cantidadTexto,
                    onValueChange = { cantidadTexto = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("Cantidad disponible") },
                    placeholder = { Text("Dejar vacío = sin límite") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Si dejas la cantidad vacía o en 0, no habrá límite de raciones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    productoSeleccionado?.let {
                        onConfirm(
                            it,
                            cantidadTexto.toIntOrNull() ?: 0
                        )
                    }
                },
                enabled = productoSeleccionado != null
            ) { Text("Añadir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
