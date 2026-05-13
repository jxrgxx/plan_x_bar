package com.los_jorges.plan_bar.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.los_jorges.plan_bar.session.SessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EspaciosAdminScreen(onBack: () -> Unit) {

    var numZonas by remember { mutableStateOf(SessionManager.numZonas) }
    val nombres  = remember {
        mutableStateListOf(*SessionManager.zonaNombres.toTypedArray())
    }
    var guardado by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Espacios de trabajo") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ── Número de espacios ────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Número de espacios",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Cuántas zonas distintas tendrá el local (hasta 4)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    // Selector 1-4 con botones
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        (1..4).forEach { n ->
                            FilterChip(
                                selected = numZonas == n,
                                onClick  = { numZonas = n; guardado = false },
                                label    = { Text("$n") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── Nombres de cada espacio ───────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Nombres de los espacios",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    (0 until numZonas).forEach { i ->
                        OutlinedTextField(
                            value         = nombres.getOrElse(i) { "Zona ${i + 1}" },
                            onValueChange = { v ->
                                while (nombres.size <= i) nombres.add("")
                                nombres[i] = v
                                guardado = false
                            },
                            label     = { Text("Espacio ${i + 1}") },
                            singleLine = true,
                            modifier  = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ── Guardar ───────────────────────────────────────────────────────
            Button(
                onClick = {
                    val nombresActuales = (0 until 4).map { i ->
                        nombres.getOrElse(i) { "Zona ${i + 1}" }.ifBlank { "Zona ${i + 1}" }
                    }
                    SessionManager.saveZonaConfig(numZonas, nombresActuales)
                    guardado = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (guardado) "¡Guardado!" else "Guardar configuración",
                    fontWeight = FontWeight.SemiBold)
            }

            if (guardado) {
                Text(
                    "Los cambios se aplican al reiniciar la pantalla de mesas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
