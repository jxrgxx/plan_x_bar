package com.los_jorges.plan_bar.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.Mesa
import com.los_jorges.plan_bar.viewmodel.MesasViewModel

private const val MESA_W = 100f
private const val MESA_H = 100f

// ─── Canvas reutilizable (admin arrastra, camarero pulsa) ───────────────────

@Composable
fun PlanoCanvas(
    restauranteId: Int,
    modoEdicion: Boolean,
    onMesaTap: ((Mesa) -> Unit)? = null,
    vm: MesasViewModel = viewModel()
) {
    val mesas by vm.mesas.collectAsState()
    val loading by vm.loading.collectAsState()

    LaunchedEffect(restauranteId) { vm.cargar(restauranteId) }

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

        if (mesas.isEmpty()) {
            Text(
                "No hay mesas creadas",
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.outline
            )
            return@BoxWithConstraints
        }

        if (modoEdicion) {
            Text(
                "Arrastra las mesas para posicionarlas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }

        mesas.forEach { mesa ->
            key(mesa.id) {
                MesaPlanoItem(
                    mesa = mesa,
                    modoEdicion = modoEdicion,
                    canvasW = canvasW,
                    canvasH = canvasH,
                    onPosicionCambiada = { id, x, y ->
                        vm.actualizarPosicion(
                            restauranteId,
                            id,
                            x,
                            y
                        )
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
    vm: MesasViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plano del restaurante") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PlanoCanvas(
                restauranteId = restauranteId,
                modoEdicion = modoEdicion,
                vm = vm
            )
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
        "ocupada" -> Color(0xFFE53935) // rojo
        "reservada" -> Color(0xFFFDD835) // amarillo
        else -> Color(0xFF43A047) // verde
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
                    contentDescription = null,
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
