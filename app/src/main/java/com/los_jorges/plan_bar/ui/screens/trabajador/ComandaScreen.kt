package com.los_jorges.plan_bar.ui.screens.trabajador

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.MenuDia
import com.los_jorges.plan_bar.model.MenuDiaLinea
import com.los_jorges.plan_bar.model.PedidoProducto
import com.los_jorges.plan_bar.model.Producto
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.viewmodel.MenuDiaViewModel
import com.los_jorges.plan_bar.viewmodel.PedidosViewModel
import com.los_jorges.plan_bar.viewmodel.ProductosViewModel
import kotlinx.coroutines.launch

private val METODOS_PAGO = listOf("efectivo", "tarjeta", "otro")
private val CATEGORIAS = listOf("bebida", "entrante", "primero", "segundo", "postre")

private data class DescuentoLinea(val valor: Double, val esPorcentaje: Boolean)

private fun precioConDescuento(linea: PedidoProducto, descuento: DescuentoLinea?): Double {
    val base = linea.cantidad * linea.precio_unitario
    return when {
        descuento == null -> base
        descuento.esPorcentaje -> base * (1.0 - descuento.valor / 100.0)
        else -> (base - descuento.valor).coerceAtLeast(0.0)
    }
}

// ── Estado del flujo secuencial de menú del día ──────────────────────────────

private val CURSO_LABELS = mapOf(
    "bebida" to "Bebida",
    "primero" to "Primero",
    "segundo" to "Segundo",
    "postre" to "Postre"
)
private val CURSO_ORDEN = listOf("bebida", "primero", "segundo", "postre")

private data class MenuFlowStep(
    val menuNum: Int,
    val totalMenus: Int,
    val curso: String,
    val cursoLabel: String,
    val opciones: List<MenuDiaLinea>   // vacío para bebida → se usa catálogo completo
)

private fun buildMenuSteps(
    menu: MenuDia,
    totalMenus: Int,
    tieneBebidas: Boolean = true,
    startMenuNum: Int = 1
): List<MenuFlowStep> {
    // Bebida aparece solo si hay bebidas disponibles en el catálogo; luego los cursos configurados en el menú
    val cursosSorted = CURSO_ORDEN.filter { curso ->
        when (curso) {
            "bebida" -> tieneBebidas
            else -> menu.lineas.any { it.curso == curso }
        }
    }
    val lastMenuNum = startMenuNum + totalMenus - 1
    // Primero todos las bebidas (startMenuNum/lastMenuNum … lastMenuNum/lastMenuNum), luego primeros, etc.
    return cursosSorted.flatMap { curso ->
        (0 until totalMenus).map { i ->
            MenuFlowStep(
                menuNum = startMenuNum + i,
                totalMenus = lastMenuNum,
                curso = curso,
                cursoLabel = CURSO_LABELS[curso] ?: curso.replaceFirstChar { it.uppercase() },
                // Para bebida usamos emptyList — el diálogo mostrará el catálogo
                opciones = if (curso == "bebida") emptyList()
                else menu.lineas.filter { it.curso == curso }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComandaScreen(
    mesaId: Int,
    mesaCodigo: String,
    comensales: Int = 1,
    onBack: () -> Unit,
    pedidosVm: PedidosViewModel = viewModel(),
    productosVm: ProductosViewModel = viewModel(),
    menuDiaVm: MenuDiaViewModel = viewModel()
) {
    val trabajador by SessionManager.trabajador.collectAsState()
    val pedido by pedidosVm.pedido.collectAsState()
    val loading by pedidosVm.loading.collectAsState()
    val error by pedidosVm.error.collectAsState()
    val productos by productosVm.productos.collectAsState()
    val menu by menuDiaVm.menu.collectAsState()

    val descuentos = remember { mutableStateMapOf<Int, DescuentoLinea>() }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    var lineaDescuento by remember { mutableStateOf<PedidoProducto?>(null) }
    var productoAEliminar by remember { mutableStateOf<PedidoProducto?>(null) }
    var showCobrarDialog by remember { mutableStateOf(false) }
    var showCancelarDialog by remember { mutableStateOf(false) }
    var showEnviarDialog by remember { mutableStateOf(false) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Menú del día flow ──────────────────────────────────────────────────
    var menuFlowSteps by remember { mutableStateOf<List<MenuFlowStep>>(emptyList()) }
    var menuFlowIndex by remember { mutableStateOf(0) }
    var menuFlowAdding by remember { mutableStateOf(false) }
    // (clave del grupo "Menú del día #N", lista de productos de ese grupo)
    var grupoAModificar by remember { mutableStateOf<Pair<String, List<PedidoProducto>>?>(null) }
    var grupoAEliminar by remember { mutableStateOf<Pair<String, List<PedidoProducto>>?>(null) }

    // Productos activos = todos excepto cancelados (los cancelados solo viven en cocina)
    val productosActivos = pedido?.productos?.filter { it.estado != "cancelado" } ?: emptyList()
    val pedidoVacio = productosActivos.isEmpty()
    val tieneNoEnviados = productosActivos.any { it.estado == "" }

    // Los items de menú del día tienen precio fijo (menu.precio por grupo),
    // independientemente de qué platos o bebidas se hayan elegido.
    val totalConDescuentos = run {
        val normales = productosActivos
            .filter { !it.observaciones.orEmpty().startsWith("Menú del día #") }
            .sumOf { precioConDescuento(it, descuentos[it.id]) }

        val numGruposMenu = productosActivos
            .filter { it.observaciones.orEmpty().startsWith("Menú del día #") }
            .mapNotNull { it.observaciones }
            .distinct()
            .size

        val precioMenus = (menu?.precio ?: 0.0) * numGruposMenu

        normales + precioMenus
    }

    LaunchedEffect(mesaId) {
        val trabajadorId = SessionManager.trabajador.value?.id
        pedidosVm.cargarPedidoPorMesa(mesaId) {
            // Solo se ejecuta si NO existe pedido abierto para esta mesa
            pedidosVm.crearNuevoPedido(
                restauranteId = SessionManager.restauranteId,
                mesaId = mesaId,
                trabajadorId = trabajadorId
            )
        }
        productosVm.cargar(SessionManager.restauranteId)
        menuDiaVm.cargar(SessionManager.restauranteId)
    }

    val handleBack: () -> Unit = { onBack() }

    BackHandler(onBack = handleBack)

    val pedidoEstado = pedido?.estado
    val pedidoId = pedido?.id
    LaunchedEffect(pedidoEstado, pedidoId) {
        if (pedidoEstado in listOf("en_cocina", "listo") && pedidoId != null) {
            pedidosVm.iniciarPollingCamarero(pedidoId)
        }
    }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }
    LaunchedEffect(error) {
        error?.let { snackMsg = it; pedidosVm.clearError() }
    }

    // Tabs: "Menú del Día" si existe, luego categorías con productos
    val categoriaTabs = CATEGORIAS.filter { cat -> productos.any { it.categoria == cat } }
    val tieneMenu = menu != null
    val tabs = if (tieneMenu) listOf("Menú del Día") + categoriaTabs else categoriaTabs
    val tabMenuIndex = if (tieneMenu) 0 else -1

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(mesaCodigo, style = MaterialTheme.typography.titleLarge)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Person, null, modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "$comensales", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        pedido?.estado?.let { estado ->
                            val badgeBg = when (estado) {
                                "en_cocina" -> Color(0xFFFFA726).copy(alpha = 0.22f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val badgeFg = when (estado) {
                                "en_cocina" -> Color(0xFFE65100)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(color = badgeBg, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    estado.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeFg,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    if (pedido != null) {
                        if (pedidoVacio) {
                            TextButton(onClick = { showCancelarDialog = true }) {
                                Text("Cancelar pedido", color = MaterialTheme.colorScheme.error)
                            }
                        } else if (tieneNoEnviados) {
                            TextButton(onClick = { showEnviarDialog = true }) {
                                Text("Enviar a cocina")
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            // ── Pedido actual ────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                    pedido == null -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sin pedido abierto", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    else -> {
                        @Suppress("NAME_SHADOWING")
                        val pedido = pedido!!
                        Column(Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                // Separar items normales de los que pertenecen a un menú del día
                                val itemsMenu = productosActivos.filter {
                                    it.observaciones.orEmpty().startsWith("Menú del día #")
                                }
                                val itemsNormales = productosActivos.filter {
                                    !it.observaciones.orEmpty().startsWith("Menú del día #")
                                }
                                val gruposMenu = itemsMenu
                                    .groupBy { it.observaciones ?: "" }
                                    .entries.sortedBy { it.key }

                                if (productosActivos.isEmpty()) {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 36.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ShoppingCart, null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                            )
                                            Text(
                                                "Sin productos aún",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }

                                // Items normales (no son de menú del día)
                                items(itemsNormales, key = { it.id }) { linea ->
                                    LineaPedidoItem(
                                        modifier = Modifier.animateItem(),
                                        linea = linea,
                                        descuento = descuentos[linea.id],
                                        onEliminar = { productoAEliminar = linea },
                                        onCancelar = {
                                            pedidosVm.cancelarProducto(
                                                linea.id,
                                                pedido.id
                                            ) { ok, err ->
                                                if (!ok) snackMsg =
                                                    err ?: "Error al cancelar producto"
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
                                        },
                                        onAplicarDescuento = { lineaDescuento = linea }
                                    )
                                    HorizontalDivider()
                                }

                                // Grupos de menú del día
                                gruposMenu.forEach { (clave, prods) ->
                                    item(key = "grupo_$clave") {
                                        Box(Modifier.animateItem()) {
                                            MenuGrupoCard(
                                                clave = clave,
                                                productos = prods,
                                                totalMenus = gruposMenu.size,
                                                onModificar = {
                                                    grupoAModificar = clave to prods
                                                },
                                                onEliminarGrupo = {
                                                    grupoAEliminar = clave to prods
                                                }
                                            )
                                        }
                                        HorizontalDivider(thickness = 2.dp)
                                    }
                                }
                            }

                            // ── Total + Cobrar ────────────────────────────────
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        if (descuentos.isNotEmpty()) {
                                            val totalSinDescuento = productosActivos
                                                .filter {
                                                    !it.observaciones.orEmpty()
                                                        .startsWith("Menú del día #")
                                                }
                                                .sumOf { it.cantidad * it.precio_unitario } +
                                                    (menu?.precio ?: 0.0) * productosActivos
                                                .filter {
                                                    it.observaciones.orEmpty()
                                                        .startsWith("Menú del día #")
                                                }
                                                .mapNotNull { it.observaciones }.distinct().size
                                            Text(
                                                "%.2f €".format(totalSinDescuento),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                fontWeight = FontWeight.Normal,
                                                textDecoration = TextDecoration.LineThrough
                                            )
                                        }
                                        Text(
                                            "Total  %.2f €".format(totalConDescuentos),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            val sinEnviar = productosActivos.any { it.estado == "" }
                                            if (sinEnviar) {
                                                snackMsg =
                                                    "Pulsa ← para enviar los productos a cocina antes de cobrar"
                                            } else {
                                                showCobrarDialog = true
                                            }
                                        },
                                        enabled = productosActivos.isNotEmpty(),
                                        contentPadding = PaddingValues(
                                            horizontal = 24.dp,
                                            vertical = 12.dp
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Payments,
                                            null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Cobrar", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Carta ────────────────────────────────────────────────────────
            HorizontalDivider(thickness = 2.dp)

            if (tabs.isNotEmpty()) {
                ScrollableTabRow(selectedTabIndex = selectedTab.coerceAtMost(tabs.lastIndex)) {
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(label.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                if (tieneMenu && selectedTab == tabMenuIndex) {
                    // ── Panel Menú del Día ───────────────────────────────────
                    var showCantidadDialog by remember { mutableStateOf(false) }
                    MenuDiaSelectorPanel(
                        menu = menu!!,
                        onIniciar = { showCantidadDialog = true }
                    )
                    if (showCantidadDialog) {
                        CantidadMenusDialog(
                            onDismiss = { showCantidadDialog = false },
                            onConfirm = { n ->
                                showCantidadDialog = false
                                val hayBebidas =
                                    productos.any { it.categoria == "bebida" && it.disponible }
                                val menusExistentes = productosActivos
                                    .mapNotNull { it.observaciones }
                                    .filter { it.startsWith("Menú del día #") }
                                    .distinct()
                                    .size
                                menuFlowSteps = buildMenuSteps(menu!!, n, hayBebidas, startMenuNum = menusExistentes + 1)
                                menuFlowIndex = 0
                            }
                        )
                    }
                } else {
                    val catIndex = if (tieneMenu) selectedTab - 1 else selectedTab
                    val listaActual = if (catIndex in categoriaTabs.indices) {
                        productos.filter { it.categoria == categoriaTabs[catIndex] }
                    } else emptyList()

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        items(listaActual, key = { it.id }) { producto ->
                            ProductoSelectorItem(
                                producto = producto,
                                enabled = pedido != null,
                                onClick = {
                                    pedido?.let { p ->
                                        pedidosVm.agregarProducto(
                                            p.id,
                                            producto.id,
                                            1,
                                            ""
                                        ) { ok, err ->
                                            snackMsg = if (ok) "${producto.nombre} añadido" else err
                                                ?: "Error"
                                        }
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    // ── Flujo secuencial Menú del Día ─────────────────────────────────────────
    if (menuFlowSteps.isNotEmpty() && menuFlowIndex < menuFlowSteps.size) {
        val step = menuFlowSteps[menuFlowIndex]

        // Bebida: siempre pide elección (catálogo completo). Otros cursos: auto-añadir si solo hay 1 opción.
        LaunchedEffect(menuFlowIndex) {
            val s = menuFlowSteps.getOrNull(menuFlowIndex) ?: return@LaunchedEffect
            if (s.curso != "bebida" && s.opciones.size == 1 && !menuFlowAdding) {
                val p = pedido ?: return@LaunchedEffect
                menuFlowAdding = true
                pedidosVm.agregarProducto(
                    p.id,
                    s.opciones[0].producto_id,
                    1,
                    "Menú del día #${s.menuNum}"
                ) { ok, err ->
                    menuFlowAdding = false
                    if (ok) {
                        val next = menuFlowIndex + 1
                        if (next >= menuFlowSteps.size) {
                            menuFlowSteps = emptyList(); menuFlowIndex = 0
                            snackMsg = "Menú del día añadido"
                        } else menuFlowIndex = next
                    } else {
                        snackMsg = err ?: "Error al añadir"
                        menuFlowSteps = emptyList(); menuFlowIndex = 0
                    }
                }
            }
        }

        // Mostrar diálogo si: es bebida (siempre) o hay varias opciones
        val esBebida = step.curso == "bebida"
        val bebidasCatalogo = if (esBebida)
            productos.filter { it.categoria == "bebida" && it.disponible }
                .map {
                    MenuDiaLinea(
                        producto_id = it.id,
                        nombre = it.nombre,
                        precio = it.precio,
                        curso = "bebida"
                    )
                }
        else emptyList()
        val opcionesEfectivas = if (esBebida) bebidasCatalogo else step.opciones

        if (esBebida || step.opciones.size > 1) {
            var opcionSeleccionada by remember(menuFlowIndex) {
                mutableStateOf(opcionesEfectivas.firstOrNull())
            }
            AlertDialog(
                onDismissRequest = { menuFlowSteps = emptyList(); menuFlowIndex = 0 },
                title = {
                    Text(
                        "${step.cursoLabel}  ${step.menuNum}/${step.totalMenus}",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (opcionesEfectivas.isEmpty()) {
                            Text(
                                "No hay bebidas disponibles en la carta.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            // Lista scrollable con indicador de desplazamiento
                            val scrollState = rememberScrollState()
                            val mostrarFlecha by remember {
                                derivedStateOf { scrollState.value == 0 && scrollState.maxValue > 0 }
                            }
                            Box {
                                Column(
                                    modifier = Modifier
                                        .heightIn(max = 320.dp)
                                        .verticalScroll(scrollState),
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    opcionesEfectivas.forEach { opcion ->
                                        val seleccionado =
                                            opcionSeleccionada?.producto_id == opcion.producto_id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { opcionSeleccionada = opcion }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = seleccionado,
                                                onClick = { opcionSeleccionada = opcion }
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    opcion.nombre,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                                if (!esBebida && opcion.cantidad > 0) {
                                                    Text(
                                                        "${opcion.cantidad} disponibles",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                // Flecha: visible solo mientras el usuario no ha hecho scroll
                                if (mostrarFlecha) {
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = "Más opciones",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 2.dp)
                                    )
                                }
                            }
                        }
                        if (menuFlowAdding) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !menuFlowAdding && opcionSeleccionada != null,
                        onClick = {
                            val sel = opcionSeleccionada ?: return@Button
                            val p = pedido ?: return@Button
                            menuFlowAdding = true
                            scope.launch {
                                pedidosVm.agregarProducto(
                                    p.id, sel.producto_id, 1, "Menú del día #${step.menuNum}"
                                ) { ok, err ->
                                    menuFlowAdding = false
                                    if (ok) {
                                        val next = menuFlowIndex + 1
                                        if (next >= menuFlowSteps.size) {
                                            menuFlowSteps = emptyList(); menuFlowIndex = 0
                                            snackMsg = "Menú del día añadido"
                                        } else menuFlowIndex = next
                                    } else {
                                        snackMsg = err ?: "Error al añadir"
                                        menuFlowSteps = emptyList(); menuFlowIndex = 0
                                    }
                                }
                            }
                        }
                    ) { Text("Confirmar") }
                },
                dismissButton = {
                    TextButton(onClick = { menuFlowSteps = emptyList(); menuFlowIndex = 0 }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }

    // ── Eliminar grupo de menú del día ───────────────────────────────────────
    grupoAEliminar?.let { (clave, prods) ->
        val numero = clave.removePrefix("Menú del día #").toIntOrNull()
        // Calculamos cuántos grupos hay a partir de productosActivos
        val numGrupos =
            productosActivos.filter { it.observaciones.orEmpty().startsWith("Menú del día #") }
                .map { it.observaciones }.distinct().size
        val titulo =
            if (numero != null && numGrupos > 1) "Menú del día · $numero" else "Menú del día"
        AlertDialog(
            onDismissRequest = { grupoAEliminar = null },
            title = { Text("Eliminar $titulo") },
            text = { Text("¿Quitar todos los productos de este menú del pedido?") },
            confirmButton = {
                TextButton(onClick = {
                    grupoAEliminar = null
                    prods.forEach { prod ->
                        val p = pedido ?: return@forEach
                        if (prod.estado == "") {
                            pedidosVm.eliminarProducto(prod.id, p.id) { _, _ -> }
                        } else {
                            pedidosVm.cancelarProducto(prod.id, p.id) { _, _ -> }
                        }
                    }
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { grupoAEliminar = null }) { Text("Cancelar") } }
        )
    }

    // ── Modificar grupo de menú del día ──────────────────────────────────────
    grupoAModificar?.let { (clave, prods) ->
        menu?.let { m ->
            ModificarMenuDialog(
                clave = clave,
                productosEnGrupo = prods,
                menu = m,
                onDismiss = { grupoAModificar = null },
                onConfirm = { cambios ->
                    grupoAModificar = null
                    cambios.forEach { (oldProd, newProductoId) ->
                        val p = pedido ?: return@forEach
                        val doAgregar = {
                            pedidosVm.agregarProducto(p.id, newProductoId, 1, clave) { ok, err ->
                                if (!ok) snackMsg = err ?: "Error al modificar"
                            }
                        }
                        if (oldProd.estado == "") {
                            pedidosVm.eliminarProducto(oldProd.id, p.id) { ok, _ ->
                                if (ok) doAgregar()
                            }
                        } else {
                            // Ya está en cocina: lo cancelamos (queda visible como cancelado) y añadimos el nuevo
                            pedidosVm.cancelarProducto(oldProd.id, p.id) { ok, _ ->
                                if (ok) doAgregar()
                            }
                        }
                    }
                }
            )
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    lineaDescuento?.let { linea ->
        DescuentoDialog(
            nombreProducto = linea.nombre,
            descuentoActual = descuentos[linea.id],
            onDismiss = { lineaDescuento = null },
            onConfirm = { valor, esPorcentaje ->
                if (valor > 0.0) descuentos[linea.id] = DescuentoLinea(valor, esPorcentaje)
                else descuentos.remove(linea.id)
                lineaDescuento = null
            }
        )
    }

    productoAEliminar?.let { linea ->
        AlertDialog(
            onDismissRequest = { productoAEliminar = null },
            title = { Text("¿Eliminar producto?") },
            text = { Text("¿Seguro que quieres quitar \"${linea.nombre}\" del pedido?") },
            confirmButton = {
                TextButton(onClick = {
                    pedido?.let { p ->
                        pedidosVm.eliminarProducto(linea.id, p.id) { ok, err ->
                            if (!ok) snackMsg = err ?: "Error al eliminar"
                        }
                    }
                    productoAEliminar = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { productoAEliminar = null }) { Text("Mantener") }
            }
        )
    }

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
                TextButton(onClick = {
                    showCancelarDialog = false
                }) { Text("Mantener") }
            }
        )
    }

    if (showEnviarDialog) {
        AlertDialog(
            onDismissRequest = { showEnviarDialog = false },
            title = { Text("¿Enviar a cocina?") },
            text = { Text("Se enviarán todos los productos pendientes a cocina.") },
            confirmButton = {
                TextButton(onClick = {
                    pedido?.let { p ->
                        pedidosVm.enviarACocina(p.id) { ok, err ->
                            snackMsg = if (ok) "Pedido enviado a cocina" else err ?: "Error al enviar"
                        }
                    }
                    showEnviarDialog = false
                }) { Text("Enviar") }
            },
            dismissButton = {
                TextButton(onClick = { showEnviarDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showCobrarDialog) {
        CobrarDialog(
            total = totalConDescuentos,
            onDismiss = { showCobrarDialog = false },
            onConfirm = { metodoPago ->
                pedido?.let { p ->
                    pedidosVm.cerrarPedido(p.id, metodoPago, totalConDescuentos) { ok, err ->
                        if (ok) onBack() else snackMsg = err ?: "Error al cobrar"
                    }
                }
                showCobrarDialog = false
            }
        )
    }
}

// ─── Panel selector de Menú del Día ─────────────────────────────────────────

@Composable
private fun MenuDiaSelectorPanel(
    menu: MenuDia,
    onIniciar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Menú del día", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
        )

        // Resumen de opciones por curso
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CURSO_ORDEN.forEach { curso ->
                    val lineasCurso = menu.lineas.filter { it.curso == curso }
                    if (lineasCurso.isNotEmpty()) {
                        val cursoLabel = CURSO_LABELS[curso] ?: curso
                        if (lineasCurso.size == 1) {
                            MenuCursoFila(cursoLabel, lineasCurso[0].nombre)
                        } else {
                            Text(
                                cursoLabel, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            lineasCurso.forEach { linea ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "• ${linea.nombre}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (linea.cantidad > 0) {
                                        Text(
                                            "${linea.cantidad} uds.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Precio menú", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "%.2f €".format(menu.precio), style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Button(onClick = onIniciar, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Restaurant, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Comandar menú del día", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Tarjeta agrupada de un menú del día ────────────────────────────────────

@Composable
private fun MenuGrupoCard(
    clave: String,          // "Menú del día #1"
    productos: List<PedidoProducto>,
    totalMenus: Int,
    onModificar: () -> Unit,
    onEliminarGrupo: () -> Unit
) {
    // Título: si solo hay un menú, "Menú del día"; si hay varios, "Menú del día · 1"
    val numero = clave.removePrefix("Menú del día #").toIntOrNull()
    val titulo = if (totalMenus > 1 && numero != null) "Menú del día · $numero" else "Menú del día"

    val productosNoCancel = productos.filter { it.estado != "cancelado" }
    val todosListos = productosNoCancel.isNotEmpty() && productosNoCancel.all { it.estado == "preparado" }
    val algunEnCocina = productosNoCancel.any { it.estado in listOf("en preparacion", "preparado") }
    val todoEnCocina = algunEnCocina
    val algunCancelado = productos.any { it.estado == "cancelado" }

    val tituloColor = when {
        todosListos  -> ColorPlatoListo
        algunEnCocina -> ColorPlatoEnCocina
        else -> MaterialTheme.colorScheme.primary
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Restaurant, null,
                tint = tituloColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                titulo, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = tituloColor,
                modifier = Modifier.weight(1f)
            )
            if (!todoEnCocina) {
                TextButton(
                    onClick = onModificar,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Modificar", style = MaterialTheme.typography.labelMedium)
                }
            }
            // Botón eliminar menú completo
            IconButton(onClick = onEliminarGrupo, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete, "Eliminar menú",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        val productosOrdenados = productos.sortedBy { prod ->
            CURSO_ORDEN.indexOf(prod.categoria).takeIf { it >= 0 } ?: Int.MAX_VALUE
        }
        productosOrdenados.forEach { prod ->
            val cursoLabel =
                CURSO_LABELS[prod.categoria] ?: prod.categoria.replaceFirstChar { it.uppercase() }
            val prodColor = when (prod.estado) {
                "cancelado"      -> ColorPlatoCancelado
                "preparado"      -> ColorPlatoListo
                "en preparacion" -> ColorPlatoEnCocina
                else             -> MaterialTheme.colorScheme.onSurface
            }
            Text(
                "$cursoLabel: ${prod.nombre}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 22.dp, top = 2.dp),
                color = prodColor
            )
        }
    }
}

// ─── Diálogo para modificar los platos de un menú ya comandado ───────────────

@Composable
private fun ModificarMenuDialog(
    clave: String,
    productosEnGrupo: List<PedidoProducto>,
    menu: MenuDia,
    onDismiss: () -> Unit,
    onConfirm: (List<Pair<PedidoProducto, Int>>) -> Unit  // (productoViejo, nuevoProductoId)
) {
    // Mapear cada producto del grupo a su curso usando producto_id para identificar bebidas y platos
    val prodPorCurso: Map<String, PedidoProducto> = buildMap {
        productosEnGrupo.forEach { prod ->
            val curso = menu.lineas.firstOrNull { it.producto_id == prod.producto_id }?.curso
            if (curso != null) put(curso, prod)
        }
    }

    // Selección actual: curso → producto_id
    val selecciones = remember {
        mutableStateMapOf<String, Int>().apply {
            prodPorCurso.forEach { (curso, prod) ->
                // El producto ya tiene producto_id; usarlo directamente
                put(curso, prod.producto_id)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modificar $clave") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CURSO_ORDEN.forEach { curso ->
                    val prodActual = prodPorCurso[curso] ?: return@forEach
                    val cursoLabel = CURSO_LABELS[curso] ?: curso

                    // Opciones disponibles para este curso — siempre desde menu.lineas
                    val opciones: List<MenuDiaLinea> = menu.lineas.filter { it.curso == curso }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            cursoLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (opciones.size <= 1) {
                            Text(
                                prodActual.nombre,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .heightIn(max = 200.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                opciones.forEach { opcion ->
                                    val sel = selecciones[curso] == opcion.producto_id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selecciones[curso] = opcion.producto_id }
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = sel,
                                            onClick = { selecciones[curso] = opcion.producto_id }
                                        )
                                        Column {
                                            Text(
                                                opcion.nombre,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                            if (curso != "bebida" && opcion.cantidad > 0) {
                                                Text(
                                                    "${opcion.cantidad} disponibles",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cambios = mutableListOf<Pair<PedidoProducto, Int>>()
                CURSO_ORDEN.forEach { curso ->
                    val prodActual = prodPorCurso[curso] ?: return@forEach
                    val seleccionActualId = prodActual.producto_id
                    val nuevaId = selecciones[curso]
                    if (nuevaId != null && nuevaId != seleccionActualId) {
                        cambios.add(prodActual to nuevaId)
                    }
                }
                onConfirm(cambios)
            }) { Text("Guardar cambios") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun CantidadMenusDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var texto by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Cuántos menús?") },
        text = {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text("Número de menús") },
                placeholder = { Text("1") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(texto.toIntOrNull()?.coerceAtLeast(1) ?: 1) },
            ) { Text("Empezar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun MenuCursoFila(label: String, nombre: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline, modifier = Modifier.width(60.dp)
        )
        Text(
            nombre, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)
        )
    }
}

// ─── Composables auxiliares ──────────────────────────────────────────────────

private val ColorPlatoListo = Color(0xFF43A047)
private val ColorPlatoEnCocina = Color(0xFFFFA726)
private val ColorPlatoCancelado = Color(0xFFE53935)
private val ColorPlatoDefault = Color.Gray

@Composable
private fun LineaPedidoItem(
    linea: PedidoProducto,
    descuento: DescuentoLinea?,
    onEliminar: () -> Unit,
    onCancelar: () -> Unit,
    onCambiarCantidad: (Int) -> Unit,
    onAplicarDescuento: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cancelado = linea.estado == "cancelado"
    val enCocina = linea.estado in listOf("en preparacion", "preparado", "servido")
    val estadoColor = when {
        cancelado -> ColorPlatoCancelado
        linea.estado == "preparado" -> ColorPlatoListo
        linea.estado == "en preparacion" -> ColorPlatoEnCocina
        else -> ColorPlatoDefault
    }
    val totalLinea = if (cancelado) 0.0 else precioConDescuento(linea, descuento)

    val bgColor by animateColorAsState(
        targetValue = when {
            cancelado -> ColorPlatoCancelado.copy(alpha = 0.05f)
            linea.estado == "preparado" -> ColorPlatoListo.copy(alpha = 0.06f)
            linea.estado == "en preparacion" -> ColorPlatoEnCocina.copy(alpha = 0.06f)
            else -> Color.Transparent
        },
        animationSpec = tween(600), label = "lineaBg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .background(estadoColor, androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                linea.nombre,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = if (cancelado) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (cancelado) MaterialTheme.colorScheme.outline else estadoColor
            )
            if (!linea.observaciones.isNullOrBlank()) {
                Text(
                    linea.observaciones, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (cancelado) {
                Surface(
                    color = ColorPlatoCancelado.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        "CANCELADO", style = MaterialTheme.typography.labelSmall,
                        color = ColorPlatoCancelado, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            } else if (descuento != null) {
                val label = if (descuento.esPorcentaje) "-${descuento.valor.toInt()}%"
                else "-%.2f€".format(descuento.valor)
                Surface(
                    color = Color(0xFF43A047).copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        label, style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        when {
            cancelado -> {
                Text(
                    "–", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            else -> {
                // Vista idéntica siempre: − cantidad + precio % papelera
                // Si ya en cocina: − reduce (hasta 0 = cancela), + aumenta, papelera cancela
                // Si no en cocina: comportamiento normal (elimina al llegar a 0)
                IconButton(
                    onClick = {
                        if (linea.cantidad > 1) onCambiarCantidad(linea.cantidad - 1)
                        else if (enCocina) onCancelar() else onEliminar()
                    },
                    modifier = Modifier.size(32.dp)
                ) { Icon(Icons.Default.Remove, "Menos", modifier = Modifier.size(16.dp)) }
                Text(
                    "${linea.cantidad}", style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = { onCambiarCantidad(linea.cantidad + 1) },
                    modifier = Modifier.size(32.dp)
                ) { Icon(Icons.Default.Add, "Más", modifier = Modifier.size(16.dp)) }
                Text(
                    "%.2f €".format(totalLinea), style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 6.dp)
                )
                IconButton(onClick = onAplicarDescuento, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Percent, "Descuento", modifier = Modifier.size(16.dp),
                        tint = if (descuento != null) Color(0xFF43A047) else MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(
                    onClick = if (enCocina) onCancelar else onEliminar,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete, "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DescuentoDialog(
    nombreProducto: String,
    descuentoActual: DescuentoLinea?,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean) -> Unit
) {
    var valorTexto by remember { mutableStateOf(descuentoActual?.valor?.toString() ?: "") }
    var esPorcentaje by remember { mutableStateOf(descuentoActual?.esPorcentaje ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Descuento — $nombreProducto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = esPorcentaje,
                        onClick = { esPorcentaje = true },
                        label = { Text("Porcentaje (%)") }
                    )
                    FilterChip(
                        selected = !esPorcentaje,
                        onClick = { esPorcentaje = false },
                        label = { Text("Fijo (€)") }
                    )
                }
                OutlinedTextField(
                    value = valorTexto,
                    onValueChange = { valorTexto = it.replace(',', '.') },
                    label = { Text(if (esPorcentaje) "Porcentaje (%)" else "Importe (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (descuentoActual != null) {
                    TextButton(onClick = { onConfirm(0.0, esPorcentaje) }) {
                        Text("Quitar descuento", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = valorTexto.toDoubleOrNull() ?: 0.0
                onConfirm(v.coerceAtLeast(0.0), esPorcentaje)
            }) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ProductoSelectorItem(producto: Producto, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                producto.nombre,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                "%.2f €".format(producto.precio),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
            )
        }
        FilledTonalIconButton(onClick = onClick, enabled = enabled) {
            Icon(Icons.Default.Add, "Añadir")
        }
    }
}

@Composable
private fun CobrarDialog(total: Double, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var metodoPago by remember { mutableStateOf("efectivo") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cobrar pedido") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Total: %.2f €".format(total), style = MaterialTheme.typography.titleMedium,
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
                            onClick = { metodoPago = metodo })
                        Text(metodo.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(metodoPago) }) { Text("Cobrar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
