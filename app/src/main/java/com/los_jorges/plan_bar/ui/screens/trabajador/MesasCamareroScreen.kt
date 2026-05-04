package com.los_jorges.plan_bar.ui.screens.trabajador

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.ui.screens.admin.PlanoCanvas
import com.los_jorges.plan_bar.viewmodel.MesasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MesasCamareroScreen(
    onVolverAlSelector: () -> Unit,
    onAbrirComanda: (Int, String) -> Unit
) {
    val trabajador by SessionManager.trabajador.collectAsState()
    val mesasVm: MesasViewModel = viewModel()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                mesasVm.cargar(SessionManager.restauranteId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hola, ${trabajador?.nombre ?: ""}") },
                actions = {
                    IconButton(onClick = {
                        SessionManager.desactivarPersonal()
                        onVolverAlSelector()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Cambiar trabajador")
                    }
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
                restauranteId = SessionManager.restauranteId,
                modoEdicion = false,
                onMesaTap = { mesa -> onAbrirComanda(mesa.id, mesa.codigo) },
                vm = mesasVm
            )
        }
    }
}
