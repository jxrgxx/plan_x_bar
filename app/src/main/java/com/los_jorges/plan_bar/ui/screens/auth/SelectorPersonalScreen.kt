package com.los_jorges.plan_bar.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.model.Trabajador
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.viewmodel.TrabajadoresViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorPersonalScreen(
    onTrabajadorSeleccionado: (Trabajador) -> Unit,
    onCerrarSesionRestaurante: () -> Unit
) {
    val vm: TrabajadoresViewModel = viewModel()
    val trabajadores by vm.trabajadores.collectAsState()
    val restauranteNombre = SessionManager.restauranteNombre

    LaunchedEffect(Unit) {
        vm.cargar(SessionManager.restauranteId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(restauranteNombre) },
                actions = {
                    TextButton(onClick = onCerrarSesionRestaurante) {
                        Text("Cerrar sesión")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¿Quién eres?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (trabajadores.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Filtramos admins — solo camareros y cocina en el selector
                val personal = trabajadores.filter { it.activo && it.rol != "admin" }
                val admins = trabajadores.filter { it.activo && it.rol == "admin" }

                if (personal.isNotEmpty()) {
                    Text(
                        "Personal",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(140.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(personal) { t ->
                            TrabajadorCard(t, onClick = { onTrabajadorSeleccionado(t) })
                        }
                    }
                }

                if (admins.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Administración",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(140.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(admins) { t ->
                            TrabajadorCard(t, onClick = { onTrabajadorSeleccionado(t) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrabajadorCard(trabajador: Trabajador, onClick: () -> Unit) {
    val rolColor = when (trabajador.rol) {
        "admin" -> MaterialTheme.colorScheme.primaryContainer
        "cocina" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val rolTexto = when (trabajador.rol) {
        "admin" -> "Administrador"
        "cocina" -> "Cocina"
        else -> "Camarero"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = rolColor),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = trabajador.nombre,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Text(
                text = rolTexto,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
