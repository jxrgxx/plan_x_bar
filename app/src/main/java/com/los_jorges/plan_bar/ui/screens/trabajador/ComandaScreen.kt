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
import com.los_jorges.plan_bar.ui.theme.LocalStrings
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
    val s = LocalStrings.current
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
    var showConfirmarSalidaDialog by remember { mutableStateOf(false) }
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
        menuDiaVm.cargar(SessionManager.restauranteId)
    }

    val handleBack: () -> Unit = {
        if (tieneNoEnviados) showConfirmarSalidaDialog = true else onBack()
    }

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
            menuDiaVm.cargar(SessionManager.restauranteId)
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
                                Text(s.cancelarPedido, color = MaterialTheme.colorScheme.error)
                            }
                        } else if (tieneNoEnviados) {
                            TextButton(onClick = { showEnviarDialog = true }) {
                                Text(s.enviarACocina)
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
                            s.sinPedidoAbierto, style = MaterialTheme.typography.bodyLarge,
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
                                                s.sinProductosAun,
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
                                            val nuevo =
                                                if (linea.estado == "servido") "en preparacion" else "servido"
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
                                                    val nuevo =
                                                        if (prod.estado == "servido") "en preparacion" else "servido"
                                                    pedidosVm.marcarPlato(
                                                        prod.id,
                                                        nuevo
                                                    ) { ok, err ->
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
                                            "${s.totalLabel}  %.2f €".format(totalConDescuentos),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            val sinEnviar = productosActivos.any { it.estado == "" }
                                            if (sinEnviar) {
                                                snackMsg = s.enviaProductosAntesDeCobrar
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
                                        Text(s.cobrar, fontWeight = FontWeight.SemiBold)
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
                                horizontal = 10.dp,
                                vertical = 8.dp
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
                                                    if (ok) s.anadidoFmt.format(producto.nombre) else err
                                                        ?: "Error"
                                            }
                                        }
                                    }
                                )
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

        fun MenuDiaLinea.agotado() = cantidad == 0
        fun MenuDiaLinea.restantes() = cantidad

        // Bebida: siempre pide elección (catálogo completo). Otros cursos: auto-añadir si solo hay 1 opción.
        LaunchedEffect(menuFlowIndex) {
            val flowStep = menuFlowSteps.getOrNull(menuFlowIndex) ?: return@LaunchedEffect
            if (flowStep.curso != "bebida" && flowStep.opciones.size == 1 && !menuFlowAdding) {
                val opcion = flowStep.opciones[0]
                // Si el único plato disponible está agotado, saltar el paso
                if (opcion.agotado()) {
                    snackMsg = s.agotadoFmt.format(opcion.nombre)
                    val next = menuFlowIndex + 1
                    if (next >= menuFlowSteps.size) {
                        menuFlowSteps = emptyList(); menuFlowIndex = 0
                    } else menuFlowIndex = next
                    return@LaunchedEffect
                }
                val p = pedido ?: return@LaunchedEffect
                menuFlowAdding = true
                pedidosVm.agregarProducto(
                    p.id, opcion.producto_id, 1, "Menú del día #${flowStep.menuNum}"
                ) { ok, err ->
                    menuFlowAdding = false
                    if (ok) {
                        menuDiaVm.cargar(SessionManager.restauranteId)
                        val next = menuFlowIndex + 1
                        if (next >= menuFlowSteps.size) {
                            menuFlowSteps = emptyList(); menuFlowIndex = 0
                            snackMsg = s.menuDelDiaAnadido
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
                .map { prod ->
                    val cantidadMenu =
                        menu?.lineas?.firstOrNull { it.producto_id == prod.id }?.cantidad ?: -1
                    MenuDiaLinea(
                        producto_id = prod.id,
                        nombre = prod.nombre,
                        precio = prod.precio,
                        curso = "bebida",
                        cantidad = cantidadMenu
                    )
                }
        else emptyList()
        val opcionesEfectivas = if (esBebida) bebidasCatalogo else step.opciones

        if (esBebida || step.opciones.size > 1) {
            var opcionSeleccionada by remember(menuFlowIndex) {
                // Preseleccionar la primera opción no agotada
                mutableStateOf(opcionesEfectivas.firstOrNull { !it.agotado() })
            }
            val menuAccent = Color(0xFF7C9EE8)
            Dialog(onDismissRequest = { menuFlowSteps = emptyList(); menuFlowIndex = 0 }) {
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
                        // Cabecera
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(menuAccent.copy(alpha = 0.08f))
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(menuAccent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    null,
                                    tint = menuAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    step.cursoLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${s.menuDelDiaLabel} ${step.menuNum} ${s.deLabel} ${step.totalMenus}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        if (opcionesEfectivas.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    s.noHayBebidasDisponibles,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            val scrollState = rememberScrollState()
                            val mostrarFlecha by remember {
                                derivedStateOf { scrollState.value == 0 && scrollState.maxValue > 0 }
                            }
                            Box(
                                modifier = Modifier
                                    .heightIn(max = 340.dp)
                                    .padding(horizontal = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.verticalScroll(scrollState)
                                ) {
                                    opcionesEfectivas.forEach { opcion ->
                                        val agotado = !esBebida && opcion.agotado()
                                        val restantes = if (!esBebida) opcion.restantes() else -1
                                        val seleccionado =
                                            opcionSeleccionada?.producto_id == opcion.producto_id
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = when {
                                                seleccionado -> menuAccent.copy(alpha = 0.10f)
                                                agotado -> MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.3f
                                                )

                                                else -> Color.Transparent
                                            },
                                            border = if (seleccionado)
                                                androidx.compose.foundation.BorderStroke(
                                                    1.5.dp,
                                                    menuAccent.copy(alpha = 0.5f)
                                                )
                                            else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .clickable(enabled = !agotado) {
                                                    opcionSeleccionada = opcion
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 6.dp
                                                ),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = seleccionado,
                                                    enabled = !agotado,
                                                    onClick = { opcionSeleccionada = opcion },
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = menuAccent
                                                    )
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
                                                            s.agotado,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = ColorPlatoCancelado
                                                        )

                                                        restantes > 0 -> Text(
                                                            "$restantes ${s.disponibles}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.outline
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }
                                if (mostrarFlecha) {
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = "Más opciones",
                                        tint = menuAccent,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 2.dp)
                                    )
                                }
                            }
                        }

                        if (menuFlowAdding) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = menuAccent
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                        ) {
                            TextButton(onClick = {
                                menuFlowSteps = emptyList(); menuFlowIndex = 0
                            }) {
                                Text(s.cancelar)
                            }
                            Button(
                                enabled = !menuFlowAdding && opcionSeleccionada != null
                                        && !(esBebida.not() && (opcionSeleccionada?.agotado() == true)),
                                onClick = {
                                    val sel = opcionSeleccionada ?: return@Button
                                    val p = pedido ?: return@Button
                                    menuFlowAdding = true
                                    scope.launch {
                                        pedidosVm.agregarProducto(
                                            p.id,
                                            sel.producto_id,
                                            1,
                                            "Menú del día #${step.menuNum}"
                                        ) { ok, err ->
                                            menuFlowAdding = false
                                            if (ok) {
                                                menuDiaVm.cargar(SessionManager.restauranteId)
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
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = menuAccent,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Confirmar", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
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
            icon = {
                Icon(
                    Icons.Default.DeleteSweep,
                    null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Eliminar $titulo") },
            text = { Text("¿Quitar todos los productos de este menú del pedido?") },
            confirmButton = {
                Button(
                    onClick = {
                        grupoAEliminar = null
                        prods.forEach { prod ->
                            val p = pedido ?: return@forEach
                            if (prod.estado == "") pedidosVm.eliminarProducto(
                                prod.id,
                                p.id
                            ) { _, _ -> }
                            else pedidosVm.cancelarProducto(prod.id, p.id) { _, _ -> }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { grupoAEliminar = null }) { Text(s.cancelar) } }
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
                            oldProd.estado == "" -> pedidosVm.eliminarProducto(
                                oldProd.id,
                                p.id
                            ) { ok, _ ->
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
            icon = {
                Icon(
                    Icons.Default.RemoveShoppingCart,
                    null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("¿Quitar del pedido?") },
            text = { Text("Se eliminará \"${linea.nombre}\" del pedido.") },
            confirmButton = {
                Button(
                    onClick = {
                        pedido?.let { p ->
                            pedidosVm.eliminarProducto(
                                linea.id,
                                p.id
                            ) { ok, err -> if (!ok) snackMsg = err ?: "Error al eliminar" }
                        }
                        productoAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Quitar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    productoAEliminar = null
                }) { Text("Mantener") }
            }
        )
    }

    if (showCancelarDialog) {
        AlertDialog(
            onDismissRequest = { showCancelarDialog = false },
            icon = { Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Cancelar pedido?") },
            text = { Text("Se cancelará el pedido y la mesa quedará libre.") },
            confirmButton = {
                Button(
                    onClick = {
                        pedido?.let { p ->
                            pedidosVm.cancelarPedido(p.id) { ok, err ->
                                if (ok) onBack() else snackMsg = err ?: "Error al cancelar"
                            }
                        }
                        showCancelarDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Cancelar pedido") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCancelarDialog = false
                }) { Text("Mantener") }
            }
        )
    }

    if (showConfirmarSalidaDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmarSalidaDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFD4A853)) },
            title = { Text("Productos sin enviar") },
            text = { Text("Tienes productos que aún no han ido a cocina. ¿Qué quieres hacer?") },
            confirmButton = {
                Button(onClick = {
                    pedido?.let { p ->
                        pedidosVm.enviarACocina(p.id) { ok, err ->
                            snackMsg = if (ok) "Enviado a cocina" else err ?: "Error al enviar"
                        }
                    }
                    showConfirmarSalidaDialog = false
                    onBack()
                }) { Text("Enviar y salir") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmarSalidaDialog = false; onBack()
                }) { Text("Salir sin enviar") }
            }
        )
    }

    if (showEnviarDialog) {
        AlertDialog(
            onDismissRequest = { showEnviarDialog = false },
            icon = { Icon(Icons.Default.Kitchen, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("¿Enviar a cocina?") },
            text = { Text("Se enviarán todos los productos pendientes a cocina.") },
            confirmButton = {
                Button(onClick = {
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
                TextButton(onClick = { showEnviarDialog = false }) { Text(s.cancelar) }
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
    val s = LocalStrings.current
    val menuAccent = Color(0xFF7C9EE8)

    fun restantes(linea: com.los_jorges.plan_bar.model.MenuDiaLinea): Int = linea.cantidad

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cabecera con precio
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = menuAccent.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(1.dp, menuAccent.copy(alpha = 0.20f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(menuAccent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        null,
                        tint = menuAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Menú del día",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Selección del día",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = menuAccent.copy(alpha = 0.16f)
                ) {
                    Text(
                        "%.2f €".format(menu.precio),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = menuAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Cursos
        CURSO_ORDEN.forEach { curso ->
            val lineasCurso = menu.lineas.filter { it.curso == curso }
            if (lineasCurso.isEmpty()) return@forEach

            val cursoLabel = CURSO_LABELS[curso] ?: curso

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        cursoLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = menuAccent,
                        letterSpacing = 1.sp
                    )
                    if (lineasCurso.size == 1) {
                        val linea = lineasCurso[0]
                        val r = restantes(linea)
                        val agotado = r == 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                linea.nombre,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (agotado) MaterialTheme.colorScheme.outline
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            when {
                                agotado -> Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ColorPlatoCancelado.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        s.agotado,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorPlatoCancelado,
                                        modifier = Modifier.padding(
                                            horizontal = 6.dp,
                                            vertical = 2.dp
                                        )
                                    )
                                }

                                r > 0 -> Text(
                                    "$r restantes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        lineasCurso.forEach { linea ->
                            val r = restantes(linea)
                            val agotado = r == 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp),
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
                                        s.agotado,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ColorPlatoCancelado,
                                        fontWeight = FontWeight.Bold
                                    )

                                    r > 0 -> Text(
                                        "$r",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Botón comandar
        Button(
            onClick = onIniciar,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = menuAccent,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
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
    val algunEnCocina = productosNoCancel.any {
        it.categoria != "bebida" && it.estado in listOf(
            "en preparacion",
            "preparado"
        )
    }
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
    val s = LocalStrings.current
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

    val menuAccent = Color(0xFF7C9EE8)
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
                // Cabecera
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(menuAccent.copy(alpha = 0.08f))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(menuAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.EditNote,
                            null,
                            tint = menuAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            "Modificar menú",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            clave,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CURSO_ORDEN.forEach { curso ->
                        val prodActual = prodPorCurso[curso]
                        val opciones: List<MenuDiaLinea> = menu.lineas.filter { it.curso == curso }
                        if (opciones.isEmpty()) return@forEach

                        val cursoLabel = CURSO_LABELS[curso] ?: curso
                        val faltante = prodActual == null

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        cursoLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (faltante) ColorPlatoCancelado
                                        else menuAccent
                                    )
                                    if (faltante) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = ColorPlatoCancelado.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                "sin añadir",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ColorPlatoCancelado,
                                                modifier = Modifier.padding(
                                                    horizontal = 6.dp,
                                                    vertical = 2.dp
                                                )
                                            )
                                        }
                                    }
                                }

                                if (!faltante && opciones.size == 1) {
                                    Text(
                                        prodActual!!.nombre,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                } else {
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
                                                onClick = { selecciones[curso] = -1 },
                                                colors = RadioButtonDefaults.colors(selectedColor = menuAccent)
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
                                                .clickable {
                                                    selecciones[curso] = opcion.producto_id
                                                }
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = sel,
                                                onClick = {
                                                    selecciones[curso] = opcion.producto_id
                                                },
                                                colors = RadioButtonDefaults.colors(selectedColor = menuAccent)
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) { Text(s.cancelar) }
                    Button(
                        onClick = {
                            val cambios = mutableListOf<Pair<PedidoProducto?, Int>>()
                            CURSO_ORDEN.forEach { curso ->
                                val nuevaId = selecciones[curso] ?: return@forEach
                                if (nuevaId == -1) return@forEach
                                val prodActual = prodPorCurso[curso]
                                when {
                                    prodActual == null -> cambios.add(null to nuevaId)
                                    nuevaId != prodActual.producto_id -> cambios.add(prodActual to nuevaId)
                                }
                            }
                            onConfirm(cambios)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = menuAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Guardar cambios", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CantidadMenusDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val s = LocalStrings.current
    var texto by remember { mutableStateOf("") }
    val accent = Color(0xFF7C9EE8)

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accent.copy(alpha = 0.08f))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            "Menú del día",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "¿Cuántos menús?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Nº de menús") },
                        placeholder = { Text("1") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Tag,
                                null,
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) { Text(s.cancelar) }
                    Button(
                        onClick = { onConfirm(texto.toIntOrNull()?.coerceAtLeast(1) ?: 1) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Empezar", fontWeight = FontWeight.SemiBold)
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
    val servido = linea.estado == "servido"
    val esBebida = linea.categoria == "bebida"
    val enCocina = linea.estado in listOf("en preparacion", "preparado", "servido")
    val estadoColor = when {
        cancelado -> ColorPlatoCancelado
        servido -> ColorPlatoListo
        linea.estado == "preparado" -> ColorPlatoListo
        linea.estado == "en preparacion" && !esBebida -> ColorPlatoEnCocina
        else -> ColorPlatoDefault
    }
    val totalLinea = if (cancelado || servido) 0.0 else precioConDescuento(linea, descuento)

    val bgColor by animateColorAsState(
        targetValue = when {
            cancelado -> ColorPlatoCancelado.copy(alpha = 0.05f)
            servido -> ColorPlatoListo.copy(alpha = 0.06f)
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
    val s = LocalStrings.current
    var valorTexto by remember { mutableStateOf(descuentoActual?.valor?.toString() ?: "") }
    var esPorcentaje by remember { mutableStateOf(descuentoActual?.esPorcentaje ?: true) }
    val accent = Color(0xFFFFB74D)

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
                // Cabecera
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accent.copy(alpha = 0.08f))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocalOffer,
                            null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            "Descuento",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            nombreProducto,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Tipo de descuento
                    Text(
                        "Tipo de descuento",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = esPorcentaje,
                            onClick = { esPorcentaje = true },
                            label = { Text("Porcentaje (%)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !esPorcentaje,
                            onClick = { esPorcentaje = false },
                            label = { Text("Fijo (€)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = valorTexto,
                        onValueChange = { valorTexto = it.replace(',', '.') },
                        label = { Text(if (esPorcentaje) "Porcentaje (%)" else "Importe (€)") },
                        leadingIcon = {
                            Text(
                                if (esPorcentaje) "%" else "€",
                                style = MaterialTheme.typography.bodyMedium,
                                color = accent,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (descuentoActual != null) {
                        OutlinedButton(
                            onClick = { onConfirm(0.0, esPorcentaje) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(
                                Icons.Default.RemoveCircleOutline,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Quitar descuento", fontWeight = FontWeight.Medium)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) { Text(s.cancelar) }
                    Button(
                        onClick = {
                            val v = valorTexto.toDoubleOrNull() ?: 0.0
                            onConfirm(v.coerceAtLeast(0.0), esPorcentaje)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Aplicar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductoSelectorItem(producto: Producto, enabled: Boolean, onClick: () -> Unit) {
    val accent = Color(0xFF83C9A5)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        tonalElevation = if (enabled) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    producto.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (enabled) accent.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "%.2f €".format(producto.precio),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) accent.copy(alpha = 0.9f)
                        else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            FilledTonalIconButton(
                onClick = onClick,
                enabled = enabled,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = accent.copy(alpha = 0.20f),
                    contentColor = accent,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
                ),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(Icons.Default.Add, "Añadir", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun CobrarDialog(total: Double, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val s = LocalStrings.current
    var metodoPago by remember { mutableStateOf("efectivo") }
    val accent = Color(0xFF4CAF82)

    val metodoIcono = mapOf(
        "efectivo" to Icons.Default.Money,
        "tarjeta" to Icons.Default.CreditCard,
        "otro" to Icons.Default.MoreHoriz
    )

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
                // Cabecera con total
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accent.copy(alpha = 0.08f))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(accent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.EuroSymbol,
                                null,
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                "Cobrar pedido",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Selecciona el método de pago",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Total destacado
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accent.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Total a cobrar",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "%.2f €".format(total),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Métodos de pago
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Método de pago",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    METODOS_PAGO.forEach { metodo ->
                        val seleccionado = metodoPago == metodo
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (seleccionado) accent.copy(alpha = 0.10f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (seleccionado)
                                androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    accent.copy(alpha = 0.6f)
                                )
                            else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { metodoPago = metodo }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    metodoIcono[metodo] ?: Icons.Default.MoreHoriz,
                                    null,
                                    tint = if (seleccionado) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    metodo.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (seleccionado) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                if (seleccionado) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) { Text(s.cancelar) }
                    Button(
                        onClick = { onConfirm(metodoPago) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.EuroSymbol, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cobrar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
