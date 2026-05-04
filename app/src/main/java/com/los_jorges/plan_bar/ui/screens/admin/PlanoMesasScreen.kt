package com.los_jorges.plan_bar.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.Estructura
import com.los_jorges.plan_bar.model.Mesa
import com.los_jorges.plan_bar.viewmodel.EstructurasViewModel
import com.los_jorges.plan_bar.viewmodel.MesasViewModel

private const val MESA_W = 100f
private const val MESA_H = 100f

private fun parseColor(hex: String): Color = try {
    val clean = hex.trimStart('#')
    val value = clean.toLong(16)
    if (clean.length == 6) Color(0xFF000000 or value) else Color(value)
} catch (_: Exception) {
    Color(0xFFBBDEFB)
}

// ─── Canvas reutilizable (admin arrastra, camarero pulsa) ────────────────────

@Composable
fun PlanoCanvas(
    restauranteId: Int,
    modoEdicion: Boolean,
    onMesaTap: ((Mesa) -> Unit)? = null,
    vm: MesasViewModel = viewModel(),
    vmEstructuras: EstructurasViewModel = viewModel()
) {
    val mesas by vm.mesas.collectAsState()
    val loading by vm.loading.collectAsState()
    val estructuras by vmEstructuras.estructuras.collectAsState()

    LaunchedEffect(restauranteId) {
        vm.cargar(restauranteId)
        vmEstructuras.cargar(restauranteId)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val canvasW = maxWidth.value
        val canvasH = maxHeight.value

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@BoxWithConstraints
        }

        if (modoEdicion && mesas.isEmpty() && estructuras.isEmpty()) {
            Text(
                "Usa el botón + para añadir zonas y mesas",
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.outline
            )
            return@BoxWithConstraints
        }

        if (modoEdicion) {
            Text(
                "Arrastra zonas y mesas para posicionarlas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }

        // Estructuras (fondo, primero)
        estructuras.forEach { estructura ->
            key(estructura.id) {
                EstructuraPlanoItem(
                    estructura = estructura,
                    modoEdicion = modoEdicion,
                    canvasW = canvasW,
                    canvasH = canvasH,
                    onPosicionCambiada = { id, x, y ->
                        vmEstructuras.actualizarPosicion(id, restauranteId, x, y)
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanoMesasScreen(
    restauranteId: Int,
    modoEdicion: Boolean = true,
    onBack: () -> Unit,
    vm: MesasViewModel = viewModel(),
    vmEstructuras: EstructurasViewModel = viewModel()
) {
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
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val estructuras by vmEstructuras.estructuras.collectAsState()
                    if (estructuras.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = { estructuraAEliminar = estructuras.last() },
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                "Eliminar última zona",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    ExtendedFloatingActionButton(
                        onClick = { showNuevaEstructura = true },
                        icon = { Icon(Icons.Default.Add, null) },
                        text = { Text("Nueva zona") }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PlanoCanvas(
                restauranteId = restauranteId,
                modoEdicion = modoEdicion,
                vm = vm,
                vmEstructuras = vmEstructuras
            )
        }
    }

    if (showNuevaEstructura) {
        NuevaEstructuraDialog(
            onDismiss = { showNuevaEstructura = false },
            onConfirm = { nombre, color ->
                vmEstructuras.crear(restauranteId, nombre, color) { ok, err ->
                    snackMsg = if (ok) "Zona \"$nombre\" creada" else err ?: "Error"
                }
                showNuevaEstructura = false
            }
        )
    }

    estructuraAEliminar?.let { e ->
        AlertDialog(
            onDismissRequest = { estructuraAEliminar = null },
            title = { Text("Eliminar zona") },
            text = { Text("¿Eliminar la zona \"${e.nombre}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    vmEstructuras.eliminar(e.id, restauranteId) { ok, err ->
                        snackMsg = if (ok) "Zona eliminada" else err ?: "Error"
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

// ─── Item de estructura (zona de fondo) ──────────────────────────────────────

@Composable
private fun EstructuraPlanoItem(
    estructura: Estructura,
    modoEdicion: Boolean,
    canvasW: Float,
    canvasH: Float,
    onPosicionCambiada: (Int, Float, Float) -> Unit
) {
    val density = LocalDensity.current
    var posX by remember(estructura.id) { mutableStateOf(estructura.posX) }
    var posY by remember(estructura.id) { mutableStateOf(estructura.posY) }
    val bgColor = parseColor(estructura.color).copy(alpha = 0.35f)
    val borderColor = parseColor(estructura.color)

    val dragModifier = if (modoEdicion) {
        Modifier.pointerInput(estructura.id) {
            detectDragGestures(
                onDragEnd = { onPosicionCambiada(estructura.id, posX, posY) }
            ) { change, dragAmount ->
                change.consume()
                with(density) {
                    posX =
                        (posX + dragAmount.x.toDp().value).coerceIn(0f, canvasW - estructura.ancho)
                    posY =
                        (posY + dragAmount.y.toDp().value).coerceIn(0f, canvasH - estructura.alto)
                }
            }
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
            .then(dragModifier)
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Text(
            text = estructura.nombre,
            style = MaterialTheme.typography.labelMedium,
            color = borderColor,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        )
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
        "ocupada" -> Color(0xFFE53935)
        "reservada" -> Color(0xFFFDD835)
        else -> Color(0xFF43A047)
    }
    val contentColor = when (mesa.estado) {
        "reservada" -> Color(0xFF212121)
        else -> Color.White
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
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(mesa.codigo, style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${mesa.capacidad}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.Person,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = contentColor
                )
            }
            Text(
                mesa.estado,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

// ─── Diálogo nueva estructura ─────────────────────────────────────────────────

private val COLORES_ZONA = listOf(
    "#BBDEFB" to "Azul",
    "#C8E6C9" to "Verde",
    "#FFF9C4" to "Amarillo",
    "#F8BBD0" to "Rosa",
    "#FFE0B2" to "Naranja",
    "#E1BEE7" to "Morado"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevaEstructuraDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var colorSeleccionado by remember { mutableStateOf(COLORES_ZONA.first().first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva zona") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre (ej: Terraza, Salón, Barra)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Color:", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    COLORES_ZONA.forEach { (hex, label) ->
                        val selected = colorSeleccionado == hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(parseColor(hex), RoundedCornerShape(6.dp))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { colorSeleccionado = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nombre.isNotBlank()) onConfirm(nombre.trim(), colorSeleccionado)
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
