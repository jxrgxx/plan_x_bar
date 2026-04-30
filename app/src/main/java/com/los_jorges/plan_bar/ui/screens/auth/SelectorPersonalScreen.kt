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
    val loading by vm.loading.collectAsState()
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¿Quién eres?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 20.dp)
            )

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val personal = trabajadores.filter { it.activo }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(130.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(personal, key = { it.id }) { t ->
                        TrabajadorCard(t, onClick = { onTrabajadorSeleccionado(t) })
                    }
                }
            }
        }
    }
}

@Composable
fun TrabajadorCard(trabajador: Trabajador, onClick: () -> Unit) {
    val rolColor = when (trabajador.rol) {
        "admin"  -> MaterialTheme.colorScheme.primaryContainer
        "cocina" -> MaterialTheme.colorScheme.tertiaryContainer
        else     -> MaterialTheme.colorScheme.secondaryContainer
    }
    val rolTexto = when (trabajador.rol) {
        "admin"  -> "Admin"
        "cocina" -> "Cocina"
        else     -> "Camarero"
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
                modifier = Modifier.size(40.dp)
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
