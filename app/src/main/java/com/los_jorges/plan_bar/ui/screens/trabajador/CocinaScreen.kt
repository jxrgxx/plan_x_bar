package com.los_jorges.plan_bar.ui.screens.trabajador

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.PedidoCocina
import com.los_jorges.plan_bar.model.PedidoProducto
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.viewmodel.PedidosViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val ColorListo = Color(0xFF43A047)
private val ColorPendiente = Color(0xFFFFA726)
private val ColorUrgente = Color(0xFFE53935)

private fun minutosDesde(fechaStr: String?): Int {
    if (fechaStr == null) return 0
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val fecha = sdf.parse(fechaStr) ?: return 0
        ((System.currentTimeMillis() - fecha.time) / 60_000).toInt()
    } catch (_: Exception) {
        0
    }
}

private fun formatTiempo(minutos: Int): String = when {
    minutos < 1 -> "Ahora"
    minutos < 60 -> "${minutos}min"
    else -> "${minutos / 60}h ${minutos % 60}min"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CocinaScreen(
    onCerrarSesion: () -> Unit,
    vm: PedidosViewModel = viewModel()
) {
    val restauranteId = SessionManager.restauranteId
    val pedidosActivos by vm.pedidosActivos.collectAsState()
    val loading by vm.loading.collectAsState()
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Ticker para actualizar tiempos cada minuto
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000); tick++
        }
    }

    LaunchedEffect(restauranteId) {
        vm.iniciarPollingCocina(restauranteId)
    }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    val pendientesPorTerminar = pedidosActivos.count { p ->
        p.productos.any { it.estado != "preparado" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cocina")
                        if (pedidosActivos.isNotEmpty()) {
                            Text(
                                if (pendientesPorTerminar == 0) "Todo al día"
                                else "$pendientesPorTerminar pedido${if (pendientesPorTerminar > 1) "s" else ""} en marcha",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (pendientesPorTerminar == 0)
                                    ColorListo
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onCerrarSesion) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Salir")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (loading && pedidosActivos.isEmpty()) {
            Box(Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (pedidosActivos.isEmpty()) {
            Box(Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Sin pedidos en cocina", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "Actualizando cada 5 segundos…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Primero los pedidos con platos pendientes, luego los ya terminados
            val ordenados = pedidosActivos.sortedWith(
                compareBy(
                    { p -> p.productos.all { it.estado == "preparado" } },
                    { it.fecha_apertura })
            )
            items(ordenados, key = { it.id }) { pedido ->
                @Suppress("NAME_SHADOWING")
                val minutos = remember(pedido.id, tick) { minutosDesde(pedido.fecha_apertura) }
                PedidoCocinaCard(
                    pedido = pedido,
                    minutos = minutos,
                    onMarcarPlato = { producto ->
                        val nuevoEstado =
                            if (producto.estado == "preparado") "en preparacion" else "preparado"
                        vm.marcarPlato(producto.id, nuevoEstado) { ok, err ->
                            if (ok) vm.cargarPedidosActivos(restauranteId)
                            else snackMsg = err ?: "Error al marcar"
                        }
                    },
                    onMarcarTodoListo = {
                        vm.marcarPedidoListo(pedido.id, restauranteId) { ok, err ->
                            if (!ok) snackMsg = err ?: "Error"
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PedidoCocinaCard(
    pedido: PedidoCocina,
    minutos: Int,
    onMarcarPlato: (PedidoProducto) -> Unit,
    onMarcarTodoListo: () -> Unit
) {
    val listos = pedido.productos.count { it.estado == "preparado" }
    val total = pedido.productos.size
    val todoListo = listos == total && total > 0

    val tiempoColor = when {
        minutos > 20 -> ColorUrgente
        minutos > 10 -> ColorPendiente
        else -> MaterialTheme.colorScheme.outline
    }

    val cardColor by animateColorAsState(
        targetValue = if (todoListo) ColorListo.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(500), label = "cardColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (todoListo) 1.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Cabecera: mesa + tiempo + badge ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Mesa ${pedido.mesa_codigo}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Timer,
                        null,
                        tint = tiempoColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        formatTiempo(minutos),
                        style = MaterialTheme.typography.labelMedium,
                        color = tiempoColor
                    )
                    if (todoListo) {
                        Surface(color = ColorListo, shape = RoundedCornerShape(12.dp)) {
                            Text(
                                "LISTO", style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── Barra de progreso ─────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Progreso", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "$listos / $total platos",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (todoListo) ColorListo else MaterialTheme.colorScheme.outline,
                        fontWeight = if (todoListo) FontWeight.Bold else FontWeight.Normal
                    )
                }
                LinearProgressIndicator(
                    progress = { if (total > 0) listos.toFloat() / total else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = if (todoListo) ColorListo else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            HorizontalDivider()

            // ── Lista de platos: pendientes primero ───────────────────────
            val platosOrdenados = pedido.productos.sortedBy { it.estado == "preparado" }
            platosOrdenados.forEach { producto ->
                PlatoItem(producto = producto, onClick = { onMarcarPlato(producto) })
            }

            // ── Botón SERVIR ──────────────────────────────────────────────
            if (todoListo) {
                Button(
                    onClick = onMarcarTodoListo,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorListo)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Servido — retirar de cocina", fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onMarcarTodoListo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Marcar todo como listo")
                }
            }
        }
    }
}

@Composable
private fun PlatoItem(producto: PedidoProducto, onClick: () -> Unit) {
    val preparado = producto.estado == "preparado"

    val bgColor by animateColorAsState(
        targetValue = when (producto.estado) {
            "preparado" -> ColorListo.copy(alpha = 0.1f)
            "en preparacion" -> ColorPendiente.copy(alpha = 0.08f)
            else -> Color.Transparent
        },
        animationSpec = tween(300), label = "platoBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(
                width = if (preparado) 1.dp else 0.dp,
                color = if (preparado) ColorListo.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (preparado) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (preparado) ColorListo else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${producto.cantidad}× ${producto.nombre}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (preparado) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (preparado) MaterialTheme.colorScheme.outline
                else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (!preparado) FontWeight.Medium else FontWeight.Normal
            )
            if (!producto.observaciones.isNullOrBlank()) {
                Text(
                    producto.observaciones,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        if (!preparado) {
            Surface(
                color = ColorPendiente.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    "PENDIENTE",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorPendiente,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
