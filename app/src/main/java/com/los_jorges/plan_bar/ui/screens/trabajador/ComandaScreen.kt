package com.los_jorges.plan_bar.ui.screens.trabajador

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.ui.window.Dialog
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
    val usoMenu by menuDiaVm.usoMenu.collectAsState()

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
                trabajadorId = trabajadorId,
                comensales = comensales
            )
        }
        productosVm.cargar(SessionManager.restauranteId)
        menuDiaVm.cargar(SessionManager.restauranteId)
        menuDiaVm.cargarUso(SessionManager.restauranteId)
    }

    val handleBack: () -> Unit = { onBack() }

    BackHandler(onBack = handleBack)

    val pedidoId = pedido?.id
    LaunchedEffect(pedidoId) {
        if (pedidoId != null) {
            pedidosVm.iniciarPollingCamarero(pedidoId)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5_000)
            menuDiaVm.cargarUso(SessionManager.restauranteId)
            menuDiaVm.cargar(SessionManager.restauranteId)
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
                                    "${pedido?.comensales?.takeIf { it > 0 } ?: comensales}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
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
        var splitFraction by remember { mutableStateOf(0.5f) }
        var columnHeightPx by remember { mutableStateOf(0f) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .onSizeChanged { columnHeightPx = it.height.toFloat() }
        ) {

            // ── Pedido actual ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(splitFraction)
            ) {
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
                                        onAplicarDescuento = { lineaDescuento = linea },
                                        onMarcarServida = {
                                            val nuevo = if (linea.estado == "servido") "en preparacion" else "servido"
                                            pedidosVm.marcarPlato(linea.id, nuevo) { ok, err ->
                                                if (ok) pedidosVm.cargarPedido(pedido.id)
                                                else snackMsg = err ?: "Error al marcar"
                                            }
                                        }
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
                                                },
                                                onMarcarServida = { prod ->
                                                    val nuevo = if (prod.estado == "servido") "en preparacion" else "servido"
                                                    pedidosVm.marcarPlato(prod.id, nuevo) { ok, err ->
                                                        if (ok) pedidosVm.cargarPedido(pedido.id)
                                                        else snackMsg = err ?: "Error al marcar"
                                                    }
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
                                                    "Envía los productos a cocina antes de cobrar"
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

            // ── Divisor arrastrable ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            if (columnHeightPx > 0) {
                                splitFraction = (splitFraction + delta / columnHeightPx)
                                    .coerceIn(0.15f, 0.85f)
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }

            // ── Carta ────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f - splitFraction)
            ) {
                if (tabs.isNotEmpty()) {
                    BoxWithConstraints {
                        val tabMinWidth = 80.dp
                        val cabenTodos = maxWidth >= tabMinWidth * tabs.size
                        if (cabenTodos) {
                            TabRow(selectedTabIndex = selectedTab.coerceAtMost(tabs.lastIndex)) {
                                tabs.forEachIndexed { index, label ->
                                    Tab(
                                        selected = selectedTab == index,
                                        onClick = { selectedTab = index },
                                        text = { Text(label.replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                        } else {
                            ScrollableTabRow(
                                selectedTabIndex = selectedTab.coerceAtMost(tabs.lastIndex),
                                edgePadding = 0.dp
                            ) {
                                tabs.forEachIndexed { index, label ->
                                    Tab(
                                        selected = selectedTab == index,
                                        onClick = { selectedTab = index },
                                        text = { Text(label.replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                        }
                    }

                    if (tieneMenu && selectedTab == tabMenuIndex) {
                        // ── Panel Menú del Día ───────────────────────────────────
                        var showCantidadDialog by remember { mutableStateOf(false) }
                        MenuDiaSelectorPanel(
                            menu = menu!!,
                            usoMenu = usoMenu,
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
                                    menuFlowSteps = buildMenuSteps(
                                        menu!!,
                                        n,
                                        hayBebidas,
                                        startMenuNum = menusExistentes + 1
                                    )
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
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp
                            )
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
                                                snackMsg =
                                                    if (ok) "${producto.nombre} añadido" else err
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
    }

    // ── Flujo secuencial Menú del Día ─────────────────────────────────────────
    if (menuFlowSteps.isNotEmpty() && menuFlowIndex < menuFlowSteps.size) {
        val step = menuFlowSteps[menuFlowIndex]

        // Helper: unidades ya usadas de un producto en TODOS los pedidos activos del restaurante.
        // maxOf garantiza que si el servidor va ligeramente por detrás usamos el conteo local.
        fun usoProducto(productoId: Int): Int {
            val serverUso = usoMenu[productoId] ?: 0
            val localUso  = productosActivos
                .filter { it.observaciones.orEmpty().startsWith("Menú del día #") && it.producto_id == productoId }
                .sumOf { it.cantidad }
            return maxOf(serverUso, localUso)
        }
        fun MenuDiaLinea.agotado() = cantidad > 0 && usoProducto(producto_id) >= cantidad
        fun MenuDiaLinea.restantes() = if (cantidad > 0) cantidad - usoProducto(producto_id) else -1

        // Bebida: siempre pide elección (catálogo completo). Otros cursos: auto-añadir si solo hay 1 opción.
        LaunchedEffect(menuFlowIndex) {
            val s = menuFlowSteps.getOrNull(menuFlowIndex) ?: return@LaunchedEffect
            if (s.curso != "bebida" && s.opciones.size == 1 && !menuFlowAdding) {
                val opcion = s.opciones[0]
                // Si el único plato disponible está agotado, saltar el paso
                if (opcion.agotado()) {
                    snackMsg = "${opcion.nombre} agotado"
                    val next = menuFlowIndex + 1
                    if (next >= menuFlowSteps.size) { menuFlowSteps = emptyList(); menuFlowIndex = 0 }
                    else menuFlowIndex = next
                    return@LaunchedEffect
                }
                val p = pedido ?: return@LaunchedEffect
                menuFlowAdding = true
                pedidosVm.agregarProducto(
                    p.id, opcion.producto_id, 1, "Menú del día #${s.menuNum}"
                ) { ok, err ->
                    menuFlowAdding = false
                    if (ok) {
                        menuDiaVm.cargarUso(SessionManager.restauranteId)
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
                // Preseleccionar la primera opción no agotada
                mutableStateOf(opcionesEfectivas.firstOrNull { !it.agotado() })
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
                                        val agotado = !esBebida && opcion.agotado()
                                        val restantes = if (!esBebida) opcion.restantes() else -1
                                        val seleccionado =
                                            opcionSeleccionada?.producto_id == opcion.producto_id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = !agotado) {
                                                    opcionSeleccionada = opcion
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = seleccionado,
                                                enabled = !agotado,
                                                onClick = { opcionSeleccionada = opcion }
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    opcion.nombre,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (agotado) MaterialTheme.colorScheme.outline
                                                    else MaterialTheme.colorScheme.onSurface
                                                )
                                                when {
                                                    agotado -> Text(
                                                        "Agotado",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = ColorPlatoCancelado
                                                    )
                                                    restantes > 0 -> Text(
                                                        "$restantes disponibles",
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
                        enabled = !menuFlowAdding && opcionSeleccionada != null
                                && !(esBebida.not() && (opcionSeleccionada?.agotado() == true)),
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
                                        menuDiaVm.cargarUso(SessionManager.restauranteId)
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
            if (numero != null) "Menú del día · $numero" else "Menú del día"
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
                        when {
                            oldProd == null -> doAgregar()  // adición nueva sin producto previo
                            oldProd.estado == "" -> pedidosVm.eliminarProducto(oldProd.id, p.id) { ok, _ ->
                                if (ok) doAgregar()
                            }
                            else -> pedidosVm.cancelarProducto(oldProd.id, p.id) { ok, _ ->
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
                            snackMsg =
                                if (ok) "Pedido enviado a cocina" else err ?: "Error al enviar"
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
    usoMenu: Map<Int, Int>,
    onIniciar: () -> Unit
) {
    // Helper local: unidades restantes de una línea (-1 = sin límite)
    fun restantes(linea: com.los_jorges.plan_bar.model.MenuDiaLinea): Int {
        if (linea.cantidad <= 0) return -1
        return (linea.cantidad - (usoMenu[linea.producto_id] ?: 0)).coerceAtLeast(0)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
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
                            val linea = lineasCurso[0]
                            val r = restantes(linea)
                            MenuCursoFila(
                                label = cursoLabel,
                                nombre = linea.nombre,
                                stockLabel = when {
                                    r == 0  -> "Agotado"
                                    r == -1 -> null
                                    else    -> "$r restantes"
                                },
                                agotado = r == 0
                            )
                        } else {
                            Text(
                                cursoLabel, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            lineasCurso.forEach { linea ->
                                val r = restantes(linea)
                                val agotado = r == 0
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "• ${linea.nombre}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (agotado) MaterialTheme.colorScheme.outline
                                        else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    when {
                                        agotado -> Text(
                                            "Agotado",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ColorPlatoCancelado,
                                            fontWeight = FontWeight.Bold
                                        )
                                        r > 0 -> Text(
                                            "$r restantes",
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
    onEliminarGrupo: () -> Unit,
    onMarcarServida: (PedidoProducto) -> Unit
) {
    // Título: si solo hay un menú, "Menú del día"; si hay varios, "Menú del día · 1"
    val numero = clave.removePrefix("Menú del día #").toIntOrNull()
    val titulo = if (numero != null) "Menú del día · $numero" else "Menú del día"

    val productosNoCancel = productos.filter { it.estado != "cancelado" }
    val todosListos =
        productosNoCancel.isNotEmpty() && productosNoCancel.all { it.estado == "preparado" || it.estado == "servido" }
    val algunEnCocina = productosNoCancel.any { it.categoria != "bebida" && it.estado in listOf("en preparacion", "preparado") }
    val todoEnCocina = algunEnCocina
    val algunCancelado = productos.any { it.estado == "cancelado" }

    val tituloColor = when {
        todosListos -> ColorPlatoListo
        algunEnCocina -> ColorPlatoEnCocina
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
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
            IconButton(
                onClick = onEliminarGrupo,
                modifier = Modifier
                    .size(36.dp)
            ) {
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
            val servida = prod.estado == "servido"
            val esBebidaProd = prod.categoria == "bebida"
            val prodColor = when {
                prod.estado == "cancelado" -> ColorPlatoCancelado
                servida -> ColorPlatoListo
                prod.estado == "preparado" -> ColorPlatoListo
                prod.estado == "en preparacion" && !esBebidaProd -> ColorPlatoEnCocina
                else -> MaterialTheme.colorScheme.onSurface
            }

            if (esBebidaProd && prod.estado != "cancelado") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMarcarServida(prod) }
                        .padding(start = 22.dp, top = 2.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$cursoLabel: ${prod.nombre}",
                        style = MaterialTheme.typography.bodySmall,
                        color = prodColor,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (servida) Icons.Default.CheckCircle
                        else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (servida) "Desmarcar servida" else "Marcar servida",
                        tint = if (servida) ColorPlatoListo
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                Text(
                    "$cursoLabel: ${prod.nombre}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 22.dp, top = 2.dp),
                    color = prodColor
                )
            }
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
    // oldProd == null → adición nueva; oldProd != null → sustitución
    onConfirm: (List<Pair<PedidoProducto?, Int>>) -> Unit
) {
    val prodPorCurso: Map<String, PedidoProducto> = buildMap {
        productosEnGrupo.forEach { prod ->
            val curso = menu.lineas.firstOrNull { it.producto_id == prod.producto_id }?.curso
            if (curso != null) put(curso, prod)
        }
    }

    // -1 = "No añadir" (para cursos que faltan en el grupo)
    val selecciones = remember {
        mutableStateMapOf<String, Int>().apply {
            CURSO_ORDEN.forEach { curso ->
                val prodActual = prodPorCurso[curso]
                val hayOpciones = menu.lineas.any { it.curso == curso }
                when {
                    prodActual != null -> put(curso, prodActual.producto_id)
                    hayOpciones -> put(curso, -1)  // curso faltante, por defecto "no añadir"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modificar $clave") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CURSO_ORDEN.forEach { curso ->
                    val prodActual = prodPorCurso[curso]
                    val opciones: List<MenuDiaLinea> = menu.lineas.filter { it.curso == curso }
                    if (opciones.isEmpty()) return@forEach

                    val cursoLabel = CURSO_LABELS[curso] ?: curso
                    val faltante = prodActual == null

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                cursoLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (faltante) ColorPlatoCancelado
                                else MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (faltante) {
                                Text(
                                    "· sin añadir",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Si ya existe y solo hay 1 opción → no se puede cambiar, solo mostrar
                        if (!faltante && opciones.size == 1) {
                            Text(
                                prodActual!!.nombre,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .heightIn(max = 200.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                // Opción "No añadir" solo para cursos faltantes
                                if (faltante) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selecciones[curso] = -1 }
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selecciones[curso] == -1,
                                            onClick = { selecciones[curso] = -1 }
                                        )
                                        Text(
                                            "No añadir",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                            fontWeight = if (selecciones[curso] == -1) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
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
                val cambios = mutableListOf<Pair<PedidoProducto?, Int>>()
                CURSO_ORDEN.forEach { curso ->
                    val nuevaId = selecciones[curso] ?: return@forEach
                    if (nuevaId == -1) return@forEach  // "no añadir"
                    val prodActual = prodPorCurso[curso]
                    when {
                        prodActual == null -> cambios.add(null to nuevaId)          // adición nueva
                        nuevaId != prodActual.producto_id -> cambios.add(prodActual to nuevaId) // sustitución
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(260.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("¿Cuántos menús?", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Nº de menús") },
                    placeholder = { Text("1") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(texto.toIntOrNull()?.coerceAtLeast(1) ?: 1) }) {
                        Text("Empezar")
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuCursoFila(
    label: String,
    nombre: String,
    stockLabel: String? = null,
    agotado: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline, modifier = Modifier.width(60.dp)
        )
        Text(
            nombre, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (agotado) MaterialTheme.colorScheme.outline
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (stockLabel != null) {
            Text(
                stockLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (agotado) ColorPlatoCancelado else MaterialTheme.colorScheme.outline,
                fontWeight = if (agotado) FontWeight.Bold else FontWeight.Normal
            )
        }
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
    onMarcarServida: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cancelado = linea.estado == "cancelado"
    val servido  = linea.estado == "servido"
    val esBebida = linea.categoria == "bebida"
    val enCocina = linea.estado in listOf("en preparacion", "preparado", "servido")
    val estadoColor = when {
        cancelado -> ColorPlatoCancelado
        servido   -> ColorPlatoListo
        linea.estado == "preparado" -> ColorPlatoListo
        linea.estado == "en preparacion" && !esBebida -> ColorPlatoEnCocina
        else -> ColorPlatoDefault
    }
    val totalLinea = if (cancelado || servido) 0.0 else precioConDescuento(linea, descuento)

    val bgColor by animateColorAsState(
        targetValue = when {
            cancelado -> ColorPlatoCancelado.copy(alpha = 0.05f)
            servido   -> ColorPlatoListo.copy(alpha = 0.06f)
            linea.estado == "preparado" -> ColorPlatoListo.copy(alpha = 0.06f)
            linea.estado == "en preparacion" && !esBebida -> ColorPlatoEnCocina.copy(alpha = 0.06f)
            else -> Color.Transparent
        },
        animationSpec = tween(600), label = "lineaBg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(top = 8.dp, bottom = 8.dp, end = 16.dp),
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

            servido -> {
                // Bebida ya servida — toca el check para desmarcar
                IconButton(onClick = onMarcarServida, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.CheckCircle, "Desmarcar servida",
                        tint = ColorPlatoListo, modifier = Modifier.size(20.dp)
                    )
                }
            }

            esBebida -> {
                // Bebida pendiente — controles normales + botón Servir
                IconButton(
                    onClick = {
                        if (linea.cantidad > 1) onCambiarCantidad(linea.cantidad - 1)
                        else onEliminar()
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
                // Botón Servir
                IconButton(onClick = onMarcarServida, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.CheckCircle, "Marcar como servida",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete, "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            else -> {
                if (!enCocina) {
                    IconButton(
                        onClick = {
                            if (linea.cantidad > 1) onCambiarCantidad(linea.cantidad - 1)
                            else onEliminar()
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
                }
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
                if (enCocina) {
                    IconButton(onClick = onCancelar, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Cancel, "Cancelar",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) {
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
