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
    Scaffold(
        containerColor = DarkBase,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = restauranteNombre.ifBlank { "Administración" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmWhite
                        )
                        Text(
                            text = "Panel de gestión",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface1,
                    titleContentColor = WarmWhite
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
                text = "¿Qué quieres gestionar?",
                style = MaterialTheme.typography.labelMedium,
                color = WarmMuted,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
            )

            AdminMenuCard(
                icon = Icons.Default.TableBar,
                titulo = "Mesas",
                descripcion = "Disposición y estado del salón",
                accentColor = Platinum40,
                onClick = onMesas
            )
            AdminMenuCard(
                icon = Icons.Default.Restaurant,
                titulo = "Productos",
                descripcion = "Gestionar la carta",
                accentColor = Color(0xFF83C9A5),
                onClick = onProductos
            )
            AdminMenuCard(
                icon = Icons.Default.Group,
                titulo = "Trabajadores",
                descripcion = "Gestionar el equipo",
                accentColor = Color(0xFF8B7AE8),
                onClick = onTrabajadores
            )
            AdminMenuCard(
                icon = Icons.Default.CalendarMonth,
                titulo = "Reservas",
                descripcion = "Ver y gestionar reservas por día",
                accentColor = Color(0xFF06B6D4),
                onClick = onReservas
            )
            AdminMenuCard(
                icon = Icons.Default.BarChart,
                titulo = "Estadísticas",
                descripcion = "Ventas, platos, meseros y más",
                accentColor = Color(0xFF83C9A5),
                onClick = onEstadisticas
            )
            AdminMenuCard(
                icon = Icons.Default.MenuBook,
                titulo = "Menú del Día",
                descripcion = "Configura la oferta diaria",
                accentColor = Color(0xFFF4A261),
                onClick = onMenuDia
            )
            AdminMenuCard(
                icon = Icons.Default.GridView,
                titulo = "Espacios de trabajo",
                descripcion = "Número y nombres de las zonas del local",
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
        colors = CardDefaults.cardColors(containerColor = DarkSurface1),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
        ).let { androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282420)) }
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
                    color = WarmWhite
                )
                Text(
                    descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmMuted
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = Color(0xFF4D4844),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
