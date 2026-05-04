package com.los_jorges.plan_bar.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    restauranteId: Int,
    restauranteNombre: String,
    onMesas: () -> Unit,
    onProductos: () -> Unit,
    onTrabajadores: () -> Unit,
    onReservas: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión del restaurante") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Administración", style = MaterialTheme.typography.titleLarge)
            if (restauranteNombre.isNotBlank()) {
                Text(
                    restauranteNombre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(4.dp))

            AdminMenuCard(
                icon = { Icon(Icons.Default.TableBar, null, modifier = Modifier.size(32.dp)) },
                titulo = "Mesas",
                descripcion = "Añadir, editar y eliminar mesas",
                onClick = onMesas
            )
            AdminMenuCard(
                icon = { Icon(Icons.Default.Restaurant, null, modifier = Modifier.size(32.dp)) },
                titulo = "Productos",
                descripcion = "Gestionar la carta del restaurante",
                onClick = onProductos
            )
            AdminMenuCard(
                icon = { Icon(Icons.Default.Group, null, modifier = Modifier.size(32.dp)) },
                titulo = "Trabajadores",
                descripcion = "Gestionar el equipo",
                onClick = onTrabajadores
            )
            AdminMenuCard(
                icon = { Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(32.dp)) },
                titulo = "Reservas",
                descripcion = "Ver y gestionar reservas por día",
                onClick = onReservas
            )

        }
    }
}

@Composable
private fun AdminMenuCard(
    icon: @Composable () -> Unit,
    titulo: String,
    descripcion: String,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            icon()
            Column {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    descripcion, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
