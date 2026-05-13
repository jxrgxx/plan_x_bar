package com.los_jorges.plan_bar.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.ui.screens.admin.*
import com.los_jorges.plan_bar.ui.screens.auth.*
import com.los_jorges.plan_bar.ui.screens.auth.FormularioReservaScreen
import com.los_jorges.plan_bar.ui.screens.trabajador.CocinaScreen
import com.los_jorges.plan_bar.ui.screens.trabajador.ComandaScreen
import com.los_jorges.plan_bar.ui.screens.trabajador.MesasCamareroScreen
import com.los_jorges.plan_bar.viewmodel.AuthViewModel
import com.los_jorges.plan_bar.viewmodel.MesasViewModel

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val ADMIN = "admin/{restauranteId}"
    const val ADMIN_MESAS = "admin/{restauranteId}/mesas"
    const val ADMIN_PRODUCTOS = "admin/{restauranteId}/productos"
    const val ADMIN_TRABAJADORES = "admin/{restauranteId}/trabajadores"
    const val ADMIN_RESERVAS = "admin/{restauranteId}/reservas"
    const val ADMIN_PLANO = "admin/{restauranteId}/plano"
    const val ADMIN_ESTADISTICAS = "admin/{restauranteId}/estadisticas"
    const val ADMIN_MENU_DIA = "admin/{restauranteId}/menudia"
    const val ADMIN_ESPACIOS = "admin/{restauranteId}/espacios"
    const val FORMULARIO_RESERVA = "reserva/formulario"
    const val SELECTOR_PERSONAL = "admin/{restauranteId}/selector"
    const val PIN = "pin/{trabajadorId}/{trabajadorNombre}"
    const val HOME_TRABAJADOR = "trabajador/home"
    const val HOME_COCINA = "trabajador/cocina"
    const val COMANDA = "trabajador/comanda/{mesaId}/{mesaCodigo}/{comensales}"

    fun admin(id: Int) = "admin/$id"
    fun adminMesas(id: Int) = "admin/$id/mesas"
    fun adminProductos(id: Int) = "admin/$id/productos"
    fun adminTrabajadores(id: Int) = "admin/$id/trabajadores"
    fun adminReservas(id: Int) = "admin/$id/reservas"
    fun adminPlano(id: Int) = "admin/$id/plano"
    fun adminEstadisticas(id: Int) = "admin/$id/estadisticas"
    fun adminMenuDia(id: Int) = "admin/$id/menudia"
    fun adminEspacios(id: Int) = "admin/$id/espacios"
    fun selectorPersonal(id: Int) = "admin/$id/selector"
    fun pin(trabajadorId: Int, nombre: String) =
        "pin/$trabajadorId/${Uri.encode(nombre)}"

    fun comanda(mesaId: Int, mesaCodigo: String, comensales: Int = 1) =
        "trabajador/comanda/$mesaId/${Uri.encode(mesaCodigo)}/$comensales"
}

@Composable
fun NavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel = viewModel()
    val trabajadorActual by SessionManager.trabajador.collectAsState()

    val startDestination = when {
        SessionManager.hayTrabajadorActivo && trabajadorActual?.rol == "cocina" -> Routes.HOME_COCINA
        SessionManager.hayTrabajadorActivo -> Routes.HOME_TRABAJADOR
        SessionManager.hayRestauranteActivo -> Routes.selectorPersonal(SessionManager.restauranteId)
        else -> Routes.LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // ── Auth ──────────────────────────────────────────────────────────────

        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { trabajador ->
                    navController.navigate(Routes.selectorPersonal(trabajador.restaurante_id)) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onGoToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegistroExitoso = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onGoToLogin = { navController.popBackStack() }
            )
        }

        // ── Selector de personal ──────────────────────────────────────────────

        composable(Routes.SELECTOR_PERSONAL) { back ->
            val restauranteId = back.arguments?.getString("restauranteId")?.toIntOrNull() ?: 1
            SelectorPersonalScreen(
                onTrabajadorSeleccionado = { trabajador ->
                    SessionManager.seleccionarTrabajador(trabajador)
                    navController.navigate(Routes.pin(trabajador.id, trabajador.nombre))
                },
                onCerrarSesionRestaurante = {
                    SessionManager.cerrarSesionTotal()
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        // ── PIN ───────────────────────────────────────────────────────────────

        composable(Routes.PIN) { back ->
            val trabajadorId =
                back.arguments?.getString("trabajadorId")?.toIntOrNull() ?: return@composable
            val trabajadorNombre = back.arguments?.getString("trabajadorNombre") ?: ""
            val trabajador by SessionManager.trabajador.collectAsState()

            PinScreen(
                trabajadorId = trabajadorId,
                trabajadorNombre = trabajadorNombre,
                tienePinPrevio = trabajador?.tiene_pin ?: true,
                viewModel = authViewModel,
                onPinCorrecto = {
                    // Si es admin, va al panel de admin; si no, al home de trabajador
                    if (trabajador?.rol == "admin") {
                        navController.navigate(Routes.admin(SessionManager.restauranteId)) {
                            popUpTo(Routes.selectorPersonal(SessionManager.restauranteId))
                        }
                    } else if (trabajador?.rol == "cocina") {
                        navController.navigate(Routes.HOME_COCINA) {
                            popUpTo(Routes.selectorPersonal(SessionManager.restauranteId))
                        }
                    } else {
                        navController.navigate(Routes.HOME_TRABAJADOR) {
                            popUpTo(Routes.selectorPersonal(SessionManager.restauranteId))
                        }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ── Admin ─────────────────────────────────────────────────────────────

        composable(Routes.ADMIN) { back ->
            val restauranteId = back.arguments?.getString("restauranteId")?.toIntOrNull() ?: 1
            AdminScreen(
                restauranteId = restauranteId,
                restauranteNombre = SessionManager.restauranteNombre,
                onMesas = { navController.navigate(Routes.adminMesas(restauranteId)) },
                onProductos = { navController.navigate(Routes.adminProductos(restauranteId)) },
                onTrabajadores = { navController.navigate(Routes.adminTrabajadores(restauranteId)) },
                onReservas = { navController.navigate(Routes.adminReservas(restauranteId)) },
                onEstadisticas = { navController.navigate(Routes.adminEstadisticas(restauranteId)) },
                onMenuDia = { navController.navigate(Routes.adminMenuDia(restauranteId)) },
                onEspacios = { navController.navigate(Routes.adminEspacios(restauranteId)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.ADMIN_MESAS) { back ->
            val restauranteId = back.arguments?.getString("restauranteId")?.toIntOrNull() ?: 1
            MesasAdminScreen(
                restauranteId = restauranteId,
                onBack = { navController.popBackStack() },
                onPlano = { navController.navigate(Routes.adminPlano(restauranteId)) }
            )
        }

        composable(Routes.ADMIN_PLANO) { back ->
            val restauranteId = back.arguments?.getString("restauranteId")?.toIntOrNull() ?: 1
            PlanoMesasScreen(
                restauranteId = restauranteId,
                modoEdicion = true,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADMIN_PRODUCTOS) { back ->
            val restauranteId = back.arguments?.getString("restauranteId")?.toIntOrNull() ?: 1
            ProductosAdminScreen(
                restauranteId = restauranteId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADMIN_TRABAJADORES) { back ->
            val restauranteId = back.arguments?.getString("restauranteId")?.toIntOrNull() ?: 1
            TrabajadoresAdminScreen(
                restauranteId = restauranteId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADMIN_RESERVAS) { back ->
            val restauranteId = back.arguments?.getString("restauranteId")?.toIntOrNull() ?: 1
            ReservasAdminScreen(
                restauranteId = restauranteId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.FORMULARIO_RESERVA) {
            FormularioReservaScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ADMIN_ESTADISTICAS) { back ->
            val restauranteId = back.arguments?.getString("restauranteId")?.toIntOrNull() ?: 1
            EstadisticasScreen(
                restauranteId = restauranteId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADMIN_MENU_DIA) { back ->
            val restauranteId = back.arguments?.getString("restauranteId")?.toIntOrNull() ?: 1
            MenuDiaAdminScreen(
                restauranteId = restauranteId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADMIN_ESPACIOS) {
            EspaciosAdminScreen(onBack = { navController.popBackStack() })
        }

        // ── Trabajador ────────────────────────────────────────────────────────

        composable(Routes.HOME_COCINA) {
            CocinaScreen(
                onCerrarSesion = {
                    val id = SessionManager.restauranteId
                    SessionManager.desactivarPersonal()
                    navController.navigate(Routes.selectorPersonal(id)) {
                        popUpTo(Routes.HOME_COCINA) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME_TRABAJADOR) {
            MesasCamareroScreen(
                onVolverAlSelector = {
                    navController.navigate(Routes.selectorPersonal(SessionManager.restauranteId)) {
                        popUpTo(Routes.HOME_TRABAJADOR) { inclusive = true }
                    }
                },
                onAbrirComanda = { mesaId, mesaCodigo, comensales ->
                    navController.navigate(Routes.comanda(mesaId, mesaCodigo, comensales))
                }
            )
        }

        composable(Routes.COMANDA) { back ->
            val mesaId = back.arguments?.getString("mesaId")?.toIntOrNull() ?: return@composable
            val mesaCodigo = back.arguments?.getString("mesaCodigo") ?: return@composable
            val comensales = back.arguments?.getString("comensales")?.toIntOrNull() ?: 1
            // Reutilizamos el MesasViewModel de HOME_TRABAJADOR para refrescar el plano al salir
            val homeEntry = remember { navController.getBackStackEntry(Routes.HOME_TRABAJADOR) }
            val mesasVm = viewModel<MesasViewModel>(homeEntry)
            ComandaScreen(
                mesaId = mesaId,
                mesaCodigo = mesaCodigo,
                comensales = comensales,
                onBack = {
                    navController.popBackStack()
                    mesasVm.cargar(SessionManager.restauranteId)
                }
            )
        }
    }
}
