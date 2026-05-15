package com.los_jorges.plan_bar.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.los_jorges.plan_bar.ui.theme.*
import com.los_jorges.plan_bar.ui.theme.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    restauranteId: Int,
    restauranteNombre: String,
    onMesas: () -> Unit,
    onProductos: () -> Unit,
    onTrabajadores: () -> Unit,
    onReservas: () -> Unit,
    onEstadisticas: () -> Unit,
    onMenuDia: () -> Unit,
    onEspacios: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val s = LocalStrings.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = restauranteNombre.ifBlank { "Administración" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = s.panelDeGestion,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = s.queQuieresGestionar,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
            )

            AdminMenuCard(
                icon = Icons.Default.TableBar,
                titulo = s.mesas,
                descripcion = s.disposicionSalon,
                accentColor = Platinum40,
                onClick = onMesas
            )
            AdminMenuCard(
                icon = Icons.Default.Restaurant,
                titulo = s.productos,
                descripcion = s.gestionarLaCarta,
                accentColor = Color(0xFF83C9A5),
                onClick = onProductos
            )
            AdminMenuCard(
                icon = Icons.Default.Group,
                titulo = s.trabajadores,
                descripcion = s.gestionarElEquipo,
                accentColor = Color(0xFF8B7AE8),
                onClick = onTrabajadores
            )
            AdminMenuCard(
                icon = Icons.Default.CalendarMonth,
                titulo = s.reservas,
                descripcion = s.verYGestionarReservas,
                accentColor = Color(0xFF06B6D4),
                onClick = onReservas
            )
            AdminMenuCard(
                icon = Icons.Default.BarChart,
                titulo = s.estadisticas,
                descripcion = s.ventasPlatosYMas,
                accentColor = Color(0xFF83C9A5),
                onClick = onEstadisticas
            )
            AdminMenuCard(
                icon = Icons.Default.MenuBook,
                titulo = s.menuDelDia,
                descripcion = s.configurarOfertaDiaria,
                accentColor = Color(0xFFF4A261),
                onClick = onMenuDia
            )
            AdminMenuCard(
                icon = Icons.Default.GridView,
                titulo = s.espaciosDeTrabajo,
                descripcion = s.numeroYNombresZonas,
                accentColor = Color(0xFF60A5FA),
                onClick = onEspacios
            )
        }
    }
}

@Composable
private fun AdminMenuCard(
    icon: ImageVector,
    titulo: String,
    descripcion: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
        ).let { androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) }
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
