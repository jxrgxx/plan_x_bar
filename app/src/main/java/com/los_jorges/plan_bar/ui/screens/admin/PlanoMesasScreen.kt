package com.los_jorges.plan_bar.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.Estructura
import com.los_jorges.plan_bar.model.Mesa
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.ui.theme.LocalStrings
import com.los_jorges.plan_bar.viewmodel.EstructurasViewModel
import com.los_jorges.plan_bar.viewmodel.MesasViewModel
import com.los_jorges.plan_bar.viewmodel.ZonasViewModel
import kotlinx.coroutines.delay

private const val MESA_W = 100f
private const val MESA_H = 100f

// Tamaño virtual de referencia (dp en el que se diseñó el plano originalmente)
private const val VIRTUAL_W = 400f
private const val VIRTUAL_H = 700f

private fun parseColor(hex: String): Color = try {
    val clean = hex.trimStart('#')
    val value = clean.toLong(16)
    if (clean.length == 6) Color(0xFF000000 or value) else Color(value)
} catch (_: Exception) {
    Color(0xFF78716C)
}

// ─── Tipos de elemento del plano ────────────────────────────────────────────

private data class TipoElemento(val etiqueta: String, val hex: String)

private val TIPOS_ELEMENTO = listOf(
    TipoElemento("Puerta", "#8B5E3C"),
    TipoElemento("Pared gris", "#78716C"),
    TipoElemento("Pared clara", "#A8A29E"),
    TipoElemento("Pared beige", "#C4A882"),
    TipoElemento("Barra", "#1C1917"),
    TipoElemento("Columna", "#57534E"),
)

// ─── Canvas reutilizable ─────────────────────────────────────────────────────

@Composable
fun PlanoCanvas(
    restauranteId: Int,
    zona: String = "piso1",
    modoEdicion: Boolean,
    onMesaTap: ((Mesa) -> Unit)? = null,
    onEliminarEstructura: ((Estructura) -> Unit)? = null,
    onEditarDimensionesMesa: ((Mesa) -> Unit)? = null,
    vm: MesasViewModel = viewModel(),
    vmEstructuras: EstructurasViewModel = viewModel()
) {
    val s = LocalStrings.current
    val todasMesas by vm.mesas.collectAsState()
    val loading by vm.loading.collectAsState()
    val todasEstructuras by vmEstructuras.estructuras.collectAsState()
    var selectedMesaId by remember { mutableStateOf<Int?>(null) }
    var selectedEstructuraId by remember { mutableStateOf<Int?>(null) }

    val mesas = todasMesas.filter { it.zona == zona }
    val estructuras = todasEstructuras.filter { it.zona == zona }

    LaunchedEffect(restauranteId) {
        while (true) {
            vm.cargar(restauranteId)
            vmEstructuras.cargar(restauranteId)
            delay(5_000)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { selectedMesaId = null; selectedEstructuraId = null })
            }
    ) {
        val canvasW = maxWidth.value
        // Escala uniforme basada en el ancho — la app es siempre vertical
        val scale = canvasW / VIRTUAL_W

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@BoxWithConstraints
        }

        if (modoEdicion && mesas.isEmpty() && estructuras.isEmpty()) {
            Text(
                s.usaElBotonParaAnadir,
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF78716C)
            )
            return@BoxWithConstraints
        }

        if (modoEdicion) {
            Text(
                s.tocaUnElemento,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF78716C),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }

        // Mesas (fondo)
        mesas.forEach { mesa ->
            key(mesa.id) {
                MesaPlanoItem(
                    mesa = mesa,
                    modoEdicion = modoEdicion,
                    isSeleccionado = selectedMesaId == mesa.id,
                    scale = scale,
                    onSeleccionar = {
                        selectedMesaId = if (selectedMesaId == mesa.id) null else mesa.id
                        selectedEstructuraId = null
                    },
                    onTransformaCambiada = { id, x, y, w, h, r ->
                        vm.actualizarTransforma(restauranteId, id, x, y, w, h, r)
                    },
                    onLongPress = if (modoEdicion) onEditarDimensionesMesa?.let { cb -> { cb(mesa) } } else null,
                    onMesaTap = onMesaTap
                )
            }
        }

        // Estructuras (encima de las mesas)
        estructuras.forEach { estructura ->
            key(estructura.id) {
                EstructuraPlanoItem(
                    estructura = estructura,
                    modoEdicion = modoEdicion,
                    isSeleccionado = selectedEstructuraId == estructura.id,
                    scale = scale,
                    onSeleccionar = {
                        selectedEstructuraId =
                            if (selectedEstructuraId == estructura.id) null else estructura.id
                        selectedMesaId = null
                    },
                    onTransformaCambiada = { id, x, y, w, h, r ->
                        vmEstructuras.actualizarTransforma(id, restauranteId, x, y, w, h, r)
                    },
                    onEliminar = onEliminarEstructura?.let { cb -> { cb(estructura) } }
                )
            }
        }
    }
}

// ─── Pantalla admin ──────────────────────────────────────────────────────────

// Las zonas se leen de SessionManager (configurables por el admin)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanoMesasScreen(
    restauranteId: Int,
    modoEdicion: Boolean = true,
    onBack: () -> Unit,
    vm: MesasViewModel = viewModel(),
    vmEstructuras: EstructurasViewModel = viewModel(),
    vmZonas: ZonasViewModel = viewModel()
) {
    val s = LocalStrings.current
    val zonasDB by vmZonas.zonas.collectAsState()
    val zonas = remember(zonasDB) {
        if (zonasDB.isNotEmpty())
            zonasDB.filter { it.activo }.sortedBy { it.orden }.map { it.clave to it.nombre }
        else
            SessionManager.zonas
    }

    var zonaActual by remember {
        mutableStateOf(
            SessionManager.zonas.firstOrNull()?.first ?: "piso1"
        )
    }

    // Recargar zonas con polling (por si cambiaron desde otro dispositivo)
    LaunchedEffect(restauranteId) {
        while (true) {
            vmZonas.cargar(restauranteId)
            delay(5_000)
        }
    }

    // Si la zona activa ya no existe, volver a la primera disponible
    LaunchedEffect(zonas) {
        if (zonas.isNotEmpty() && zonas.none { it.first == zonaActual }) {
            zonaActual = zonas.first().first
        }
    }
    var showNuevaEstructura by remember { mutableStateOf(false) }
    var estructuraAEliminar by remember { mutableStateOf<Estructura?>(null) }
    var mesaAEditarDimId by remember { mutableStateOf<Int?>(null) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.planoDelRestaurante) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            if (modoEdicion) {
                ExtendedFloatingActionButton(
                    onClick = { showNuevaEstructura = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text(s.nuevoElemento) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = zonas.indexOfFirst { it.first == zonaActual }
                .coerceAtLeast(0)) {
                zonas.forEach { (key, label) ->
                    Tab(
                        selected = zonaActual == key,
                        onClick = { zonaActual = key },
                        text = { Text(label) }
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                PlanoCanvas(
                    restauranteId = restauranteId,
                    zona = zonaActual,
                    modoEdicion = modoEdicion,
                    vm = vm,
                    vmEstructuras = vmEstructuras,
                    onEliminarEstructura = if (modoEdicion) { e ->
                        estructuraAEliminar = e
                    } else null,
                    onEditarDimensionesMesa = if (modoEdicion) { m ->
                        mesaAEditarDimId = m.id
                    } else null
                )
            }
        }
    }

    if (showNuevaEstructura) {
        NuevaEstructuraDialog(
            onDismiss = { showNuevaEstructura = false },
            onConfirm = { nombre, color ->
                val zonaId = SessionManager.zonasActivas
                    .find { it.clave == zonaActual }?.id ?: 0
                vmEstructuras.crear(
                    restauranteId, nombre, color,
                    posX = (VIRTUAL_W - 120f) / 2f,
                    posY = (VIRTUAL_H - 80f) / 2f,
                    zonaId = zonaId
                ) { ok, err ->
                    snackMsg = if (ok) s.elementoCreado else err ?: "Error"
                }
                showNuevaEstructura = false
            }
        )
    }

    estructuraAEliminar?.let { e ->
        val label = e.nombre.ifBlank { "este elemento" }
        AlertDialog(
            onDismissRequest = { estructuraAEliminar = null },
            icon  = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(s.eliminarElemento) },
            text = { Text("¿${s.eliminar} $label?") },
            confirmButton = {
                Button(
                    onClick = {
                        vmEstructuras.eliminar(e.id, restauranteId) { ok, err ->
                            snackMsg = if (ok) s.eliminado else err ?: "Error"
                        }
                        estructuraAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(s.eliminar) }
            },
            dismissButton = {
                TextButton(onClick = { estructuraAEliminar = null }) { Text(s.cancelar) }
            }
        )
    }

    val todasMesasDialog by vm.mesas.collectAsState()
    mesaAEditarDimId?.let { mesaId ->
        val mesa = todasMesasDialog.firstOrNull { it.id == mesaId } ?: return@let
        var anchoText by remember(mesa.ancho) { mutableStateOf(mesa.ancho.toInt().toString()) }
        var altoText by remember(mesa.alto) { mutableStateOf(mesa.alto.toInt().toString()) }
        var rotacionText by remember(mesa.rotacion) {
            mutableStateOf(
                mesa.rotacion.toInt().toString()
            )
        }
        val dimAccent = Color(0xFF7C9EE8)
        Dialog(onDismissRequest = { mesaAEditarDimId = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(dimAccent.copy(alpha = 0.08f))
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(dimAccent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Straighten, null, tint = dimAccent, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                "${s.mesaLabel} ${mesa.codigo}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                s.dimensionesYRotacion,
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
                            value = anchoText,
                            onValueChange = { anchoText = it.filter { c -> c.isDigit() } },
                            label = { Text(s.ancho) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = altoText,
                            onValueChange = { altoText = it.filter { c -> c.isDigit() } },
                            label = { Text(s.alto) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = rotacionText,
                            onValueChange = { v ->
                                rotacionText = v.filter { c -> c.isDigit() || c == '-' }
                            },
                            label = { Text(s.rotacion) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = { mesaAEditarDimId = null }) { Text(s.cancelar) }
                        Button(
                            onClick = {
                                val newAncho = anchoText.toFloatOrNull()?.coerceAtLeast(5f) ?: mesa.ancho
                                val newAlto = altoText.toFloatOrNull()?.coerceAtLeast(5f) ?: mesa.alto
                                val newRot = rotacionText.toFloatOrNull() ?: mesa.rotacion
                                vm.actualizarTransforma(
                                    restauranteId, mesa.id,
                                    mesa.posX, mesa.posY,
                                    newAncho, newAlto, newRot
                                )
                                mesaAEditarDimId = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = dimAccent, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s.aplicar, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ─── Item de estructura ──────────────────────────────────────────────────────

@Composable
private fun EstructuraPlanoItem(
    estructura: Estructura,
    modoEdicion: Boolean,
    isSeleccionado: Boolean = false,
    scale: Float,
    onSeleccionar: () -> Unit = {},
    onTransformaCambiada: (Int, Float, Float, Float, Float, Float) -> Unit,
    onEliminar: (() -> Unit)? = null
) {
    val density = LocalDensity.current

    var posX by remember(estructura.id) { mutableStateOf(estructura.posX) }
    var posY by remember(estructura.id) { mutableStateOf(estructura.posY) }
    var ancho by remember(estructura.id) { mutableStateOf(estructura.ancho) }
    var alto by remember(estructura.id) { mutableStateOf(estructura.alto) }
    var rotacion by remember(estructura.id) { mutableStateOf(estructura.rotacion) }

    // Sincroniza cuando el ViewModel actualiza la estructura desde fuera
    LaunchedEffect(
        estructura.posX,
        estructura.posY,
        estructura.ancho,
        estructura.alto,
        estructura.rotacion
    ) {
        posX = estructura.posX
        posY = estructura.posY
        ancho = estructura.ancho
        alto = estructura.alto
        rotacion = estructura.rotacion
    }

    val color = parseColor(estructura.color)

    val renderW = ancho * scale
    val renderH = alto * scale

    // Área táctil mínima de 44dp para poder seleccionar estructuras muy finas
    val minTouch = 44f
    val touchW = renderW.coerceAtLeast(minTouch)
    val touchH = renderH.coerceAtLeast(minTouch)
    val extraW = (touchW - renderW) / 2f  // padding invisible a cada lado en dp
    val extraH = (touchH - renderH) / 2f

    // Matriz de rotación para posicionar handles en esquinas visuales reales
    val rad = Math.toRadians(rotacion.toDouble())
    val cosR = cos(rad).toFloat()
    val sinR = sin(rad).toFloat()

    // Posición visual de una esquina (en dp, relativa al top-left del VISUAL box)
    // Para offset dentro del outer Box hay que sumar extraW/extraH
    val halfW = renderW / 2f
    val halfH = renderH / 2f
    fun vx(lx: Float, ly: Float) = extraW + halfW + lx * cosR - ly * sinR
    fun vy(lx: Float, ly: Float) = extraH + halfH + lx * sinR + ly * cosR

    // Convierte drag de píxeles → dp → espacio local del elemento (desrotado) → virtual
    fun localDx(pxX: Float, pxY: Float): Float {
        val dp = with(density) { pxX.toDp().value }
        val dpY = with(density) { pxY.toDp().value }
        return (dp * cosR + dpY * sinR) / scale
    }

    fun localDy(pxX: Float, pxY: Float): Float {
        val dp = with(density) { pxX.toDp().value }
        val dpY = with(density) { pxY.toDp().value }
        return (-dp * sinR + dpY * cosR) / scale
    }

    // Convierte drag de píxeles → dp → virtual (sin compensar rotación, para mover)
    fun Float.toVirtual() = with(density) { this@toVirtual.toDp().value } / scale

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    with(density) { (posX * scale - extraW).dp.roundToPx() },
                    with(density) { (posY * scale - extraH).dp.roundToPx() }
                )
            }
            .size(width = touchW.dp, height = touchH.dp)
            .then(
                if (modoEdicion) Modifier.pointerInput(estructura.id) {
                    detectDragGestures(
                        onDragEnd = {
                            onTransformaCambiada(estructura.id, posX, posY, ancho, alto, rotacion)
                        }
                    ) { change, drag ->
                        change.consume()
                        posX =
                            (posX + drag.x.toVirtual()).coerceIn(0f, maxOf(0f, VIRTUAL_W - ancho))
                        posY = (posY + drag.y.toVirtual()).coerceIn(0f, maxOf(0f, VIRTUAL_H - alto))
                    }
                } else Modifier
            )
            .then(
                if (modoEdicion) Modifier.pointerInput(estructura.id + 10000) {
                    detectTapGestures(onTap = { onSeleccionar() })
                } else Modifier
            )
    ) {
        // Solo el fondo rota visualmente — tamaño real centrado dentro del área táctil
        Box(
            modifier = Modifier
                .size(width = renderW.dp, height = renderH.dp)
                .align(Alignment.Center)
                .graphicsLayer { rotationZ = rotacion }
                .clip(RoundedCornerShape(10.dp))
                .background(color)
                .then(
                    if (isSeleccionado)
                        Modifier.border(
                            2.dp,
                            Color.White.copy(alpha = 0.8f),
                            RoundedCornerShape(10.dp)
                        )
                    else Modifier
                )
        )

        if (isSeleccionado && modoEdicion) {
            val btnSize = 28.dp
            val handleSize = 22.dp
            val btnHalf = 14f
            val handleHalf = 11f

            // ── Botón eliminar: esquina visual TL ────────────────────────────
            if (onEliminar != null) {
                val ex = vx(-halfW, -halfH);
                val ey = vy(-halfW, -halfH)
                Box(
                    modifier = Modifier
                        .offset(x = (ex - btnHalf).dp, y = (ey - btnHalf).dp)
                        .size(btnSize)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .clickable { onEliminar() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(15.dp), tint = Color.White)
                }
            }

            // ── Botón rotar: esquina visual TR — arrastrar para rotar libremente ──
            val rx = vx(halfW, -halfH);
            val ry = vy(halfW, -halfH)
            Box(
                modifier = Modifier
                    .offset(x = (rx - btnHalf).dp, y = (ry - btnHalf).dp)
                    .size(btnSize)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6))
                    .pointerInput("rot_${estructura.id}") {
                        var startAngle = 0f
                        var startRot = 0f
                        var centerXPx = 0f
                        var centerYPx = 0f
                        var touchXPx = 0f
                        var touchYPx = 0f
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val rW = ancho * scale
                                val rH = alto * scale
                                val tW = rW.coerceAtLeast(44f)
                                val tH = rH.coerceAtLeast(44f)
                                val eW = (tW - rW) / 2f
                                val eH = (tH - rH) / 2f
                                val hW = rW / 2f;
                                val hH = rH / 2f
                                val rad = Math.toRadians(rotacion.toDouble())
                                val c = cos(rad).toFloat();
                                val s = sin(rad).toFloat()
                                // Posición del botón TL dentro del outer Box (dp)
                                val btnTLx = eW + hW + hW * c + hH * s - btnHalf
                                val btnTLy = eH + hH + hW * s - hH * c - btnHalf
                                val dpToPx = density.density
                                centerXPx = tW / 2f * dpToPx
                                centerYPx = tH / 2f * dpToPx
                                touchXPx = btnTLx * dpToPx + startOffset.x
                                touchYPx = btnTLy * dpToPx + startOffset.y
                                startAngle = Math.toDegrees(
                                    atan2(
                                        (touchYPx - centerYPx).toDouble(),
                                        (touchXPx - centerXPx).toDouble()
                                    )
                                ).toFloat()
                                startRot = rotacion
                            },
                            onDragEnd = {
                                onTransformaCambiada(
                                    estructura.id,
                                    posX,
                                    posY,
                                    ancho,
                                    alto,
                                    rotacion
                                )
                            }
                        ) { change, drag ->
                            change.consume()
                            touchXPx += drag.x
                            touchYPx += drag.y
                            val currentAngle = Math.toDegrees(
                                atan2(
                                    (touchYPx - centerYPx).toDouble(),
                                    (touchXPx - centerXPx).toDouble()
                                )
                            ).toFloat()
                            var delta = currentAngle - startAngle
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f
                            rotacion = startRot + delta
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Refresh, null, Modifier.size(15.dp), tint = Color.White)
            }

            // ── Handle lado superior ─────────────────────────────────────────
            // Solo dy en local; mantiene el lado inferior fijo
            val topX = vx(0f, -halfH);
            val topY = vy(0f, -halfH)
            Box(
                modifier = Modifier
                    .offset(x = (topX - handleHalf).dp, y = (topY - handleHalf).dp)
                    .size(handleSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .pointerInput("top_${estructura.id}", rotacion) {
                        detectDragGestures(
                            onDragEnd = {
                                onTransformaCambiada(
                                    estructura.id,
                                    posX,
                                    posY,
                                    ancho,
                                    alto,
                                    rotacion
                                )
                            }
                        ) { change, drag ->
                            change.consume()
                            val dy = localDy(drag.x, drag.y)
                            val newAlto = (alto - dy).coerceAtLeast(5f)
                            val dH = alto - newAlto
                            posX -= dH * sinR / 2f
                            posY += dH * (1 + cosR) / 2f
                            alto = newAlto
                        }
                    }
            )

            // ── Handle lado inferior ─────────────────────────────────────────
            // Solo dy en local; mantiene el lado superior fijo
            val botX = vx(0f, halfH);
            val botY = vy(0f, halfH)
            Box(
                modifier = Modifier
                    .offset(x = (botX - handleHalf).dp, y = (botY - handleHalf).dp)
                    .size(handleSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .pointerInput("bot_${estructura.id}", rotacion) {
                        detectDragGestures(
                            onDragEnd = {
                                onTransformaCambiada(
                                    estructura.id,
                                    posX,
                                    posY,
                                    ancho,
                                    alto,
                                    rotacion
                                )
                            }
                        ) { change, drag ->
                            change.consume()
                            val dy = localDy(drag.x, drag.y)
                            val newAlto = (alto + dy).coerceAtLeast(5f)
                            val dH = newAlto - alto
                            posX -= dH * sinR / 2f
                            posY += dH * (cosR - 1) / 2f
                            alto = newAlto
                        }
                    }
            )

            // ── Handle lado izquierdo ────────────────────────────────────────
            // Solo dx en local; mantiene el lado derecho fijo
            val lefX = vx(-halfW, 0f);
            val lefY = vy(-halfW, 0f)
            Box(
                modifier = Modifier
                    .offset(x = (lefX - handleHalf).dp, y = (lefY - handleHalf).dp)
                    .size(handleSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .pointerInput("lef_${estructura.id}", rotacion) {
                        detectDragGestures(
                            onDragEnd = {
                                onTransformaCambiada(
                                    estructura.id,
                                    posX,
                                    posY,
                                    ancho,
                                    alto,
                                    rotacion
                                )
                            }
                        ) { change, drag ->
                            change.consume()
                            val dx = localDx(drag.x, drag.y)
                            val newAncho = (ancho - dx).coerceAtLeast(5f)
                            val dW = ancho - newAncho
                            posX += dW * (1 + cosR) / 2f
                            posY += dW * sinR / 2f
                            ancho = newAncho
                        }
                    }
            )

            // ── Handle lado derecho ──────────────────────────────────────────
            // Solo dx en local; mantiene el lado izquierdo fijo
            val rigX = vx(halfW, 0f);
            val rigY = vy(halfW, 0f)
            Box(
                modifier = Modifier
                    .offset(x = (rigX - handleHalf).dp, y = (rigY - handleHalf).dp)
                    .size(handleSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .pointerInput("rig_${estructura.id}", rotacion) {
                        detectDragGestures(
                            onDragEnd = {
                                onTransformaCambiada(
                                    estructura.id,
                                    posX,
                                    posY,
                                    ancho,
                                    alto,
                                    rotacion
                                )
                            }
                        ) { change, drag ->
                            change.consume()
                            val dx = localDx(drag.x, drag.y)
                            val newAncho = (ancho + dx).coerceAtLeast(5f)
                            val dW = newAncho - ancho
                            posX += dW * (cosR - 1) / 2f
                            posY += dW * sinR / 2f
                            ancho = newAncho
                        }
                    }
            )
        }
    }
}

// ─── Item de mesa ────────────────────────────────────────────────────────────

@Composable
private fun MesaPlanoItem(
    mesa: Mesa,
    modoEdicion: Boolean,
    isSeleccionado: Boolean = false,
    scale: Float,
    onSeleccionar: () -> Unit = {},
    onTransformaCambiada: (Int, Float, Float, Float, Float, Float) -> Unit,
    onLongPress: (() -> Unit)? = null,
    onMesaTap: ((Mesa) -> Unit)?
) {
    val density = LocalDensity.current

    var posX by remember(mesa.id) { mutableStateOf(mesa.posX) }
    var posY by remember(mesa.id) { mutableStateOf(mesa.posY) }
    var ancho by remember(mesa.id) { mutableStateOf(mesa.ancho) }
    var alto by remember(mesa.id) { mutableStateOf(mesa.alto) }
    var rotacion by remember(mesa.id) { mutableStateOf(mesa.rotacion) }

    // Sincroniza cuando el ViewModel actualiza la mesa desde fuera (dialog, etc.)
    LaunchedEffect(mesa.posX, mesa.posY, mesa.ancho, mesa.alto, mesa.rotacion) {
        posX = mesa.posX
        posY = mesa.posY
        ancho = mesa.ancho
        alto = mesa.alto
        rotacion = mesa.rotacion
    }

    val containerColor = when (mesa.estado) {
        "ocupada" -> Color(0xFFDC2626)
        "reservada" -> Color(0xFFCA8A04)
        else -> Color(0xFF16A34A)
    }

    val renderW = ancho * scale
    val renderH = alto * scale
    val minTouch = 44f
    val touchW = renderW.coerceAtLeast(minTouch)
    val touchH = renderH.coerceAtLeast(minTouch)
    val extraW = (touchW - renderW) / 2f
    val extraH = (touchH - renderH) / 2f

    val rad = Math.toRadians(rotacion.toDouble())
    val cosR = cos(rad).toFloat()
    val sinR = sin(rad).toFloat()

    val halfW = renderW / 2f
    val halfH = renderH / 2f
    fun vx(lx: Float, ly: Float) = extraW + halfW + lx * cosR - ly * sinR
    fun vy(lx: Float, ly: Float) = extraH + halfH + lx * sinR + ly * cosR

    fun localDx(pxX: Float, pxY: Float): Float {
        val dp = with(density) { pxX.toDp().value }
        val dpY = with(density) { pxY.toDp().value }
        return (dp * cosR + dpY * sinR) / scale
    }

    fun localDy(pxX: Float, pxY: Float): Float {
        val dp = with(density) { pxX.toDp().value }
        val dpY = with(density) { pxY.toDp().value }
        return (-dp * sinR + dpY * cosR) / scale
    }

    fun Float.toVirtual() = with(density) { this@toVirtual.toDp().value } / scale

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    with(density) { (posX * scale - extraW).dp.roundToPx() },
                    with(density) { (posY * scale - extraH).dp.roundToPx() }
                )
            }
            .size(width = touchW.dp, height = touchH.dp)
            .then(
                if (modoEdicion) Modifier.pointerInput(mesa.id) {
                    detectDragGestures(
                        onDragEnd = {
                            onTransformaCambiada(
                                mesa.id,
                                posX,
                                posY,
                                ancho,
                                alto,
                                rotacion
                            )
                        }
                    ) { change, drag ->
                        change.consume()
                        posX =
                            (posX + drag.x.toVirtual()).coerceIn(0f, maxOf(0f, VIRTUAL_W - ancho))
                        posY = (posY + drag.y.toVirtual()).coerceIn(0f, maxOf(0f, VIRTUAL_H - alto))
                    }
                } else Modifier
            )
            .pointerInput(mesa.id + 20000) {
                detectTapGestures(
                    onTap = { if (modoEdicion) onSeleccionar() else onMesaTap?.invoke(mesa) },
                    onLongPress = { onLongPress?.invoke() }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .size(width = renderW.dp, height = renderH.dp)
                .align(Alignment.Center)
                .graphicsLayer { rotationZ = rotacion }
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor)
                .then(
                    if (isSeleccionado)
                        Modifier.border(
                            2.dp,
                            Color.White.copy(alpha = 0.8f),
                            RoundedCornerShape(12.dp)
                        )
                    else Modifier
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    mesa.codigo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${mesa.capacidad}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        Icons.Default.Person,
                        null,
                        modifier = Modifier.size(13.dp),
                        tint = Color.White
                    )
                }
                Text(
                    mesa.estado,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        if (isSeleccionado && modoEdicion) {
            val btnSize = 28.dp
            val handleSize = 22.dp
            val btnHalf = 14f
            val handleHalf = 11f

            // ── Botón rotar: esquina visual TR ───────────────────────────────
            val rx = vx(halfW, -halfH);
            val ry = vy(halfW, -halfH)
            Box(
                modifier = Modifier
                    .offset(x = (rx - btnHalf).dp, y = (ry - btnHalf).dp)
                    .size(btnSize)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6))
                    .pointerInput("rot_m_${mesa.id}") {
                        var startAngle = 0f;
                        var startRot = 0f
                        var centerXPx = 0f;
                        var centerYPx = 0f
                        var touchXPx = 0f;
                        var touchYPx = 0f
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val rW = ancho * scale;
                                val rH = alto * scale
                                val tW = rW.coerceAtLeast(44f);
                                val tH = rH.coerceAtLeast(44f)
                                val eW = (tW - rW) / 2f;
                                val eH = (tH - rH) / 2f
                                val hW = rW / 2f;
                                val hH = rH / 2f
                                val r2 = Math.toRadians(rotacion.toDouble())
                                val c = cos(r2).toFloat();
                                val s = sin(r2).toFloat()
                                val btnTLx = eW + hW + hW * c + hH * s - btnHalf
                                val btnTLy = eH + hH + hW * s - hH * c - btnHalf
                                val dpToPx = density.density
                                centerXPx = tW / 2f * dpToPx; centerYPx = tH / 2f * dpToPx
                                touchXPx = btnTLx * dpToPx + startOffset.x
                                touchYPx = btnTLy * dpToPx + startOffset.y
                                startAngle = Math.toDegrees(
                                    atan2(
                                        (touchYPx - centerYPx).toDouble(),
                                        (touchXPx - centerXPx).toDouble()
                                    )
                                ).toFloat()
                                startRot = rotacion
                            },
                            onDragEnd = {
                                onTransformaCambiada(
                                    mesa.id,
                                    posX,
                                    posY,
                                    ancho,
                                    alto,
                                    rotacion
                                )
                            }
                        ) { change, drag ->
                            change.consume()
                            touchXPx += drag.x; touchYPx += drag.y
                            val currentAngle = Math.toDegrees(
                                atan2(
                                    (touchYPx - centerYPx).toDouble(),
                                    (touchXPx - centerXPx).toDouble()
                                )
                            ).toFloat()
                            var delta = currentAngle - startAngle
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f
                            rotacion = startRot + delta
                        }
                    },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Refresh, null, Modifier.size(15.dp), tint = Color.White) }

            // ── Handle superior ──────────────────────────────────────────────
            val topX = vx(0f, -halfH);
            val topY = vy(0f, -halfH)
            Box(
                modifier = Modifier
                    .offset(x = (topX - handleHalf).dp, y = (topY - handleHalf).dp)
                    .size(handleSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .pointerInput("top_m_${mesa.id}", rotacion) {
                        detectDragGestures(onDragEnd = {
                            onTransformaCambiada(
                                mesa.id,
                                posX,
                                posY,
                                ancho,
                                alto,
                                rotacion
                            )
                        }) { change, drag ->
                            change.consume()
                            val dy = localDy(drag.x, drag.y);
                            val newAlto = (alto - dy).coerceAtLeast(5f)
                            val dH =
                                alto - newAlto; posX -= dH * sinR / 2f; posY += dH * (1 + cosR) / 2f; alto =
                            newAlto
                        }
                    })

            // ── Handle inferior ──────────────────────────────────────────────
            val botX = vx(0f, halfH);
            val botY = vy(0f, halfH)
            Box(
                modifier = Modifier
                    .offset(x = (botX - handleHalf).dp, y = (botY - handleHalf).dp)
                    .size(handleSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .pointerInput("bot_m_${mesa.id}", rotacion) {
                        detectDragGestures(onDragEnd = {
                            onTransformaCambiada(
                                mesa.id,
                                posX,
                                posY,
                                ancho,
                                alto,
                                rotacion
                            )
                        }) { change, drag ->
                            change.consume()
                            val dy = localDy(drag.x, drag.y);
                            val newAlto = (alto + dy).coerceAtLeast(5f)
                            val dH =
                                newAlto - alto; posX -= dH * sinR / 2f; posY += dH * (cosR - 1) / 2f; alto =
                            newAlto
                        }
                    })

            // ── Handle izquierdo ─────────────────────────────────────────────
            val lefX = vx(-halfW, 0f);
            val lefY = vy(-halfW, 0f)
            Box(
                modifier = Modifier
                    .offset(x = (lefX - handleHalf).dp, y = (lefY - handleHalf).dp)
                    .size(handleSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .pointerInput("lef_m_${mesa.id}", rotacion) {
                        detectDragGestures(onDragEnd = {
                            onTransformaCambiada(
                                mesa.id,
                                posX,
                                posY,
                                ancho,
                                alto,
                                rotacion
                            )
                        }) { change, drag ->
                            change.consume()
                            val dx = localDx(drag.x, drag.y);
                            val newAncho = (ancho - dx).coerceAtLeast(5f)
                            val dW =
                                ancho - newAncho; posX += dW * (1 + cosR) / 2f; posY += dW * sinR / 2f; ancho =
                            newAncho
                        }
                    })

            // ── Handle derecho ───────────────────────────────────────────────
            val rigX = vx(halfW, 0f);
            val rigY = vy(halfW, 0f)
            Box(
                modifier = Modifier
                    .offset(x = (rigX - handleHalf).dp, y = (rigY - handleHalf).dp)
                    .size(handleSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .pointerInput("rig_m_${mesa.id}", rotacion) {
                        detectDragGestures(onDragEnd = {
                            onTransformaCambiada(
                                mesa.id,
                                posX,
                                posY,
                                ancho,
                                alto,
                                rotacion
                            )
                        }) { change, drag ->
                            change.consume()
                            val dx = localDx(drag.x, drag.y);
                            val newAncho = (ancho + dx).coerceAtLeast(5f)
                            val dW =
                                newAncho - ancho; posX += dW * (cosR - 1) / 2f; posY += dW * sinR / 2f; ancho =
                            newAncho
                        }
                    })
        }
    }
}

// ─── Diálogo nuevo elemento ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevaEstructuraDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val s = LocalStrings.current
    var nombre by remember { mutableStateOf("") }
    var colorSeleccionado by remember { mutableStateOf(TIPOS_ELEMENTO.first()) }

    val accent = Color(0xFF7C9EE8)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        Icon(Icons.Default.Palette, null, tint = accent, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(
                            s.nuevoElemento,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            s.tipoElemento,
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
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text(s.nombre) },
                        placeholder = { Text(s.tipoElemento) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        s.color,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TIPOS_ELEMENTO.chunked(3).forEach { fila ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                fila.forEach { tipo ->
                                    val seleccionado = colorSeleccionado == tipo
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(parseColor(tipo.hex))
                                            .then(
                                                if (seleccionado)
                                                    Modifier.border(
                                                        2.5.dp,
                                                        Color.White,
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                else Modifier
                                            )
                                            .clickable { colorSeleccionado = tipo }
                                    )
                                }
                                repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) { Text(s.cancelar) }
                    Button(
                        onClick = {
                            onConfirm(
                                nombre.trim().ifBlank { colorSeleccionado.etiqueta },
                                colorSeleccionado.hex
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s.crear, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
