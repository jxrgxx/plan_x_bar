package com.los_jorges.plan_bar.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.EstadisticasResponse
import com.los_jorges.plan_bar.ui.theme.*
import com.los_jorges.plan_bar.viewmodel.EstadisticasViewModel
import com.los_jorges.plan_bar.viewmodel.EstadisticasViewModel.Periodo

private val GreenAccent  = Color(0xFF83C9A5)
private val BlueAccent   = Color(0xFF06B6D4)
private val PurpleAccent = Color(0xFF8B7AE8)
private val AmberAccent  = Color(0xFFD4A853)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasScreen(
    restauranteId: Int,
    onBack: () -> Unit,
    vm: EstadisticasViewModel = viewModel()
) {
    val periodo by vm.periodo.collectAsState()
    val stats   by vm.stats.collectAsState()
    val loading by vm.loading.collectAsState()
    val error   by vm.error.collectAsState()
    val snack   = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.cargar(restauranteId) }
    LaunchedEffect(error) { error?.let { snack.showSnackbar(it); vm.clearError() } }

    Scaffold(
        containerColor = DarkBase,
        snackbarHost   = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Estadísticas",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = WarmWhite
                        )
                        Text(
                            "Panel del jefe",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = WarmGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface1)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding      = PaddingValues(vertical = 16.dp)
        ) {
            item {
                PeriodoSelector(selected = periodo, onSelect = { vm.cargar(restauranteId, it) })
            }

            if (loading) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = Platinum40) }
                }
                return@LazyColumn
            }

            val s = stats
            if (s == null) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Sin datos para el período", color = WarmMuted) }
                }
                return@LazyColumn
            }

            item { StatSectionLabel("Ventas") }
            item { VentasCard(s) }

            item { StatSectionLabel("Servicio") }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatMetricCard(
                        modifier  = Modifier.weight(1f),
                        titulo    = "Comensales",
                        valor     = "${s.total_pax}",
                        subtitulo = "pax totales",
                        color     = BlueAccent
                    )
                    StatMetricCard(
                        modifier  = Modifier.weight(1f),
                        titulo    = "T. medio",
                        valor     = "${s.tiempo_medio_minutos} min",
                        subtitulo = "por mesa",
                        color     = PurpleAccent
                    )
                }
            }

            if (s.total_descuentos > 0 || s.total_cortesias > 0) {
                item { StatSectionLabel("Ajustes") }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatMetricCard(
                            modifier  = Modifier.weight(1f),
                            titulo    = "Descuentos",
                            valor     = "%.2f €".format(s.total_descuentos),
                            subtitulo = "aplicados",
                            color     = AmberAccent
                        )
                        StatMetricCard(
                            modifier  = Modifier.weight(1f),
                            titulo    = "Cortesías",
                            valor     = "%.2f €".format(s.total_cortesias),
                            subtitulo = "invitadas",
                            color     = AmberAccent
                        )
                    }
                }
            }

            if (s.top_platos.isNotEmpty()) {
                item { StatSectionLabel("Top 3 platos") }
                s.top_platos.forEachIndexed { idx, plato ->
                    item {
                        StatRankCard(
                            posicion  = idx + 1,
                            nombre    = plato.nombre,
                            subtitulo = "${plato.total_unidades} uds · ${plato.categoria}",
                            importe   = plato.total_importe,
                            color     = when (idx) {
                                0    -> Color(0xFFFFD700)
                                1    -> Color(0xFFC0C0C0)
                                else -> Color(0xFFCD7F32)
                            }
                        )
                    }
                }
            }

            if (s.ventas_categoria.isNotEmpty()) {
                item { StatSectionLabel("Por categoría") }
                val totalCat = s.ventas_categoria.sumOf { it.total_importe }.takeIf { it > 0 } ?: 1.0
                s.ventas_categoria.forEach { cat ->
                    item {
                        StatCategoriaCard(
                            nombre   = cat.categoria.replaceFirstChar { it.uppercase() },
                            unidades = cat.total_unidades,
                            importe  = cat.total_importe,
                            pct      = (cat.total_importe / totalCat).toFloat()
                        )
                    }
                }
            }

            if (s.ventas_mesero.isNotEmpty()) {
                item { StatSectionLabel("Por mesero") }
                s.ventas_mesero.forEach { mesero ->
                    item {
                        StatMeseroCard(
                            nombre  = mesero.nombre,
                            pedidos = mesero.total_pedidos,
                            importe = mesero.total_importe
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun PeriodoSelector(selected: Periodo, onSelect: (Periodo) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface1)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(Periodo.HOY to "Hoy", Periodo.SEMANA to "Semana", Periodo.MES to "Mes")
            .forEach { (p, label) ->
                val active = p == selected
                Button(
                    onClick        = { onSelect(p) },
                    modifier       = Modifier.weight(1f).height(36.dp),
                    shape          = RoundedCornerShape(8.dp),
                    colors         = ButtonDefaults.buttonColors(
                        containerColor = if (active) Platinum40 else Color.Transparent,
                        contentColor   = if (active) DarkBase else WarmMuted
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        label,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
    }
}

@Composable
private fun VentasCard(s: EstadisticasResponse) {
    Surface(shape = RoundedCornerShape(16.dp), color = DarkSurface1, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Venta Bruta", style = MaterialTheme.typography.labelSmall, color = WarmMuted)
                    Text(
                        "%.2f €".format(s.venta_bruta),
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = WarmGray
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Venta Neta", style = MaterialTheme.typography.labelSmall, color = WarmMuted)
                    Text(
                        "%.2f €".format(s.venta_neta),
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = GreenAccent
                    )
                }
            }
            if (s.venta_bruta > 0) {
                val pct = (s.venta_neta / s.venta_bruta).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress   = { pct },
                    modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color      = GreenAccent,
                    trackColor = DarkSurface3
                )
                Text(
                    "%.1f%% del bruto".format(pct * 100),
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmMuted
                )
            }
        }
    }
}

@Composable
private fun StatMetricCard(modifier: Modifier, titulo: String, valor: String, subtitulo: String, color: Color) {
    Surface(shape = RoundedCornerShape(16.dp), color = DarkSurface1, modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelSmall, color = WarmMuted)
            Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(subtitulo, style = MaterialTheme.typography.labelSmall, color = WarmMuted)
        }
    }
}

@Composable
private fun StatRankCard(posicion: Int, nombre: String, subtitulo: String, importe: Double, color: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = DarkSurface1, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("#$posicion", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
            }
            Column(Modifier.weight(1f)) {
                Text(nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = WarmWhite)
                Text(subtitulo, style = MaterialTheme.typography.labelSmall, color = WarmMuted)
            }
            Text("%.2f €".format(importe), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = WarmWhite)
        }
    }
}

@Composable
private fun StatCategoriaCard(nombre: String, unidades: Int, importe: Double, pct: Float) {
    Surface(shape = RoundedCornerShape(12.dp), color = DarkSurface1, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = WarmWhite)
                Column(horizontalAlignment = Alignment.End) {
                    Text("%.2f €".format(importe), style = MaterialTheme.typography.bodyMedium, color = WarmWhite, fontWeight = FontWeight.SemiBold)
                    Text("$unidades uds", style = MaterialTheme.typography.labelSmall, color = WarmMuted)
                }
            }
            LinearProgressIndicator(
                progress   = { pct },
                modifier   = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color      = Platinum40,
                trackColor = DarkSurface3
            )
        }
    }
}

@Composable
private fun StatMeseroCard(nombre: String, pedidos: Int, importe: Double) {
    Surface(shape = RoundedCornerShape(12.dp), color = DarkSurface1, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = WarmWhite)
                Text("$pedidos comandas", style = MaterialTheme.typography.labelSmall, color = WarmMuted)
            }
            Text("%.2f €".format(importe), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GreenAccent)
        }
    }
}

@Composable
private fun StatSectionLabel(text: String) {
    Text(
        text.uppercase(),
        style         = MaterialTheme.typography.labelSmall,
        color         = WarmMuted,
        letterSpacing = 1.2.sp,
        modifier      = Modifier.padding(start = 2.dp, top = 4.dp)
    )
}
