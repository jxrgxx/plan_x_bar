package com.los_jorges.plan_bar.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.Estructura
import com.los_jorges.plan_bar.model.Mesa
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.viewmodel.EstructurasViewModel
import com.los_jorges.plan_bar.viewmodel.MesasViewModel

private const val MESA_W = 100f
private const val MESA_H = 100f

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
    vm: MesasViewModel = viewModel(),
    vmEstructuras: EstructurasViewModel = viewModel()
) {
    val todasMesas by vm.mesas.collectAsState()
    val loading by vm.loading.collectAsState()
    val todasEstructuras by vmEstructuras.estructuras.collectAsState()
    var selectedId by remember { mutableStateOf<Int?>(null) }

    val mesas = todasMesas.filter { it.zona == zona }
    val estructuras = todasEstructuras.filter { it.zona == zona }

    LaunchedEffect(restauranteId) {
        vm.cargar(restauranteId)
        vmEstructuras.cargar(restauranteId)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF100E0C))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { selectedId = null })
            }
    ) {
        val canvasW = maxWidth.value
        val canvasH = maxHeight.value

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@BoxWithConstraints
        }

        if (modoEdicion && mesas.isEmpty() && estructuras.isEmpty()) {
            Text(
                "Usa el botón + para añadir elementos y mesas",
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF78716C)
            )
            return@BoxWithConstraints
        }

        if (modoEdicion) {
            Text(
                "Toca un elemento para seleccionarlo",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF78716C),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }

        // Estructuras (fondo)
        estructuras.forEach { estructura ->
            key(estructura.id) {
                EstructuraPlanoItem(
                    estructura = estructura,
                    modoEdicion = modoEdicion,
                    isSeleccionado = selectedId == estructura.id,
                    canvasW = canvasW,
                    canvasH = canvasH,
                    onSeleccionar = {
                        selectedId = if (selectedId == estructura.id) null else estructura.id
                    },
                    onPosicionCambiada = { id, x, y ->
                        vmEstructuras.actualizarPosicion(id, restauranteId, x, y)
                    },
                    onEliminar = onEliminarEstructura?.let { cb -> { cb(estructura) } }
                )
            }
        }

        // Mesas (encima)
        mesas.forEach { mesa ->
            key(mesa.id) {
                MesaPlanoItem(
                    mesa = mesa,
                    modoEdicion = modoEdicion,
                    canvasW = canvasW,
                    canvasH = canvasH,
                    onPosicionCambiada = { id, x, y ->
                        vm.actualizarPosicion(restauranteId, id, x, y)
                    },
                    onMesaTap = onMesaTap
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
    vmEstructuras: EstructurasViewModel = viewModel()
) {
    var zonaActual by remember { mutableStateOf("piso1") }
    var showNuevaEstructura by remember { mutableStateOf(false) }
    var estructuraAEliminar by remember { mutableStateOf<Estructura?>(null) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plano del restaurante") },
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
                    text = { Text("Nuevo elemento") }
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
            val zonas = SessionManager.zonas
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
                    } else null
                )
            }
        }
    }

    if (showNuevaEstructura) {
        NuevaEstructuraDialog(
            onDismiss = { showNuevaEstructura = false },
            onConfirm = { nombre, color, ancho, alto ->
                vmEstructuras.crear(
                    restauranteId, nombre, color,
                    posX = 80f, posY = 12f, ancho = ancho, alto = alto,
                    zona = zonaActual
                ) { ok, err ->
                    snackMsg = if (ok) "Elemento creado" else err ?: "Error"
                }
                showNuevaEstructura = false
            }
        )
    }

    estructuraAEliminar?.let { e ->
        val label = e.nombre.ifBlank { "este elemento" }
        AlertDialog(
            onDismissRequest = { estructuraAEliminar = null },
            title = { Text("Eliminar elemento") },
            text = { Text("¿Eliminar $label?") },
            confirmButton = {
                TextButton(onClick = {
                    vmEstructuras.eliminar(e.id, restauranteId) { ok, err ->
                        snackMsg = if (ok) "Eliminado" else err ?: "Error"
                    }
                    estructuraAEliminar = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { estructuraAEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

// ─── Item de estructura ──────────────────────────────────────────────────────

@Composable
private fun EstructuraPlanoItem(
    estructura: Estructura,
    modoEdicion: Boolean,
    isSeleccionado: Boolean = false,
    canvasW: Float,
    canvasH: Float,
    onSeleccionar: () -> Unit = {},
    onPosicionCambiada: (Int, Float, Float) -> Unit,
    onEliminar: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    var posX by remember(estructura.id) { mutableStateOf(estructura.posX) }
    var posY by remember(estructura.id) { mutableStateOf(estructura.posY) }
    val color = parseColor(estructura.color)

    val interactionModifier = if (modoEdicion) {
        Modifier
            .pointerInput(estructura.id) {
                detectDragGestures(
                    onDragEnd = { onPosicionCambiada(estructura.id, posX, posY) }
                ) { change, dragAmount ->
                    change.consume()
                    with(density) {
                        posX = (posX + dragAmount.x.toDp().value).coerceIn(
                            0f,
                            canvasW - estructura.ancho
                        )
                        posY = (posY + dragAmount.y.toDp().value).coerceIn(
                            0f,
                            canvasH - estructura.alto
                        )
                    }
                }
            }
            .pointerInput(estructura.id + 10000) {
                detectTapGestures(onTap = { onSeleccionar() })
            }
    } else Modifier

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    with(density) { posX.dp.roundToPx() },
                    with(density) { posY.dp.roundToPx() }
                )
            }
            .size(width = estructura.ancho.dp, height = estructura.alto.dp)
            .then(interactionModifier)
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .then(
                if (isSeleccionado)
                    Modifier.border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                else Modifier
            )
    ) {
        // Botón eliminar — solo si está seleccionado
        if (isSeleccionado && onEliminar != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { onEliminar() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Eliminar",
                    modifier = Modifier.size(13.dp),
                    tint = Color.White
                )
            }
        }
    }
}

// ─── Item de mesa ────────────────────────────────────────────────────────────

@Composable
private fun MesaPlanoItem(
    mesa: Mesa,
    modoEdicion: Boolean,
    canvasW: Float,
    canvasH: Float,
    onPosicionCambiada: (Int, Float, Float) -> Unit,
    onMesaTap: ((Mesa) -> Unit)?
) {
    val density = LocalDensity.current
    var posX by remember(mesa.id) { mutableStateOf(mesa.posX) }
    var posY by remember(mesa.id) { mutableStateOf(mesa.posY) }

    val containerColor = when (mesa.estado) {
        "ocupada" -> Color(0xFFDC2626)
        "reservada" -> Color(0xFFCA8A04)
        else -> Color(0xFF16A34A)
    }

    val interactionModifier = when {
        modoEdicion -> Modifier.pointerInput(mesa.id) {
            detectDragGestures(
                onDragEnd = { onPosicionCambiada(mesa.id, posX, posY) }
            ) { change, dragAmount ->
                change.consume()
                with(density) {
                    posX = (posX + dragAmount.x.toDp().value).coerceIn(0f, canvasW - MESA_W)
                    posY = (posY + dragAmount.y.toDp().value).coerceIn(0f, canvasH - MESA_H)
                }
            }
        }

        onMesaTap != null -> Modifier.clickable { onMesaTap(mesa) }
        else -> Modifier
    }

    Card(
        modifier = Modifier
            .offset {
                IntOffset(
                    with(density) { posX.dp.roundToPx() },
                    with(density) { posY.dp.roundToPx() }
                )
            }
            .size(width = MESA_W.dp, height = MESA_H.dp)
            .then(interactionModifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                mesa.codigo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${mesa.capacidad}", style = MaterialTheme.typography.bodySmall)
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
}

// ─── Diálogo nuevo elemento ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevaEstructuraDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Float, Float) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var tipoSeleccionado by remember { mutableStateOf(TIPOS_ELEMENTO.first()) }
    var ancho by remember { mutableStateOf("200") }
    var alto by remember { mutableStateOf("150") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo elemento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Selector de tipo
                Text(
                    "Tipo:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TIPOS_ELEMENTO.chunked(3).forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            fila.forEach { tipo ->
                                val seleccionado = tipoSeleccionado == tipo
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
                                        .clickable { tipoSeleccionado = tipo }
                                )
                            }
                            // Rellenar fila incompleta
                            repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                // Nombre opcional
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre (opcional)") },
                    placeholder = { Text("Ej: Entrada, Terraza…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Dimensiones
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ancho,
                        onValueChange = { ancho = it.filter { c -> c.isDigit() } },
                        label = { Text("Anchura") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = alto,
                        onValueChange = { alto = it.filter { c -> c.isDigit() } },
                        label = { Text("Altura") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    nombre.trim().ifBlank { tipoSeleccionado.etiqueta },
                    tipoSeleccionado.hex,
                    ancho.toFloatOrNull()?.coerceAtLeast(30f) ?: 200f,
                    alto.toFloatOrNull()?.coerceAtLeast(30f) ?: 150f
                )
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
