package com.los_jorges.plan_bar.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.ui.screens.admin.*
import com.los_jorges.plan_bar.ui.screens.auth.*
import com.los_jorges.plan_bar.ui.screens.trabajador.ComandaScreen
import com.los_jorges.plan_bar.ui.screens.trabajador.HomeScreen
import com.los_jorges.plan_bar.viewmodel.AuthViewModel

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val ADMIN = "admin/{restauranteId}"
    const val ADMIN_MESAS = "admin/{restauranteId}/mesas"
    const val ADMIN_PRODUCTOS = "admin/{restauranteId}/productos"
    const val ADMIN_TRABAJADORES = "admin/{restauranteId}/trabajadores"
    const val LOGIN_TRABAJADOR = "admin/{restauranteId}/login_trabajador"
    const val ADMIN_PLANO = "admin/{restauranteId}/plano"
    const val HOME_TRABAJADOR = "trabajador/home"
    const val COMANDA = "trabajador/comanda/{mesaId}/{mesaCodigo}"

    fun admin(id: Int) = "admin/$id"
    fun adminMesas(id: Int) = "admin/$id/mesas"
    fun adminProductos(id: Int) = "admin/$id/productos"
    fun adminTrabajadores(id: Int) = "admin/$id/trabajadores"
    fun loginTrabajador(id: Int) = "admin/$id/login_trabajador"
    fun adminPlano(id: Int) = "admin/$id/plano"
    fun comanda(mesaId: Int, mesaCodigo: String) =
        "trabajador/comanda/$mesaId/${Uri.encode(mesaCodigo)}"
}

@Composable
fun NavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel = viewModel()

    val startDestination = when {
        SessionManager.hayTrabajadorActivo -> Routes.HOME_TRABAJADOR
        SessionManager.admin.value != null -> Routes.admin(SessionManager.restauranteId)
        else -> Routes.LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { trabajador ->
                    navController.navigate(Routes.admin(trabajador.restaurante_id)) {
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

        composable(Routes.ADMIN) { back ->
            val restauranteId = back.arguments?.getString("restauranteId")?.toIntOrNull() ?: 1
            AdminScreen(
                restauranteId = restauranteId,
                restauranteNombre = SessionManager.restauranteNombre,
                onMesas = { navController.navigate(Routes.adminMesas(restauranteId)) },
                onProductos = { navController.navigate(Routes.adminProductos(restauranteId)) },
                onTrabajadores = { navController.navigate(Routes.adminTrabajadores(restauranteId)) },
                onAccesoTrabajador = { navController.navigate(Routes.loginTrabajador(restauranteId)) },
                onCerrarSesion = {
                    SessionManager.cerrarSesionTotal()
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                }
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
                onBack = { navController.popBackStack() })
        }

        composable(Routes.ADMIN_TRABAJADORES) { back ->
            val restauranteId = back.arguments?.getString("restauranteId")?.toIntOrNull() ?: 1
            TrabajadoresAdminScreen(
                restauranteId = restauranteId,
                onBack = { navController.popBackStack() })
        }

        composable(Routes.LOGIN_TRABAJADOR) { back ->
            val restauranteId = back.arguments?.getString("restauranteId")?.toIntOrNull() ?: 1
            LoginTrabajadorScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME_TRABAJADOR) {
                        // No puede volver atrás al panel admin
                        popUpTo(Routes.admin(restauranteId)) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.HOME_TRABAJADOR) {
            HomeScreen(
                authViewModel = authViewModel,
                onCerrarSesion = {
                    val id = SessionManager.restauranteId
                    navController.navigate(Routes.admin(id)) {
                        popUpTo(Routes.HOME_TRABAJADOR) { inclusive = true }
                    }
                },
                onAbrirComanda = { mesaId, mesaCodigo ->
                    navController.navigate(Routes.comanda(mesaId, mesaCodigo))
                }
            )
        }

        composable(Routes.COMANDA) { back ->
            val mesaId = back.arguments?.getString("mesaId")?.toIntOrNull() ?: return@composable
            val mesaCodigo = back.arguments?.getString("mesaCodigo") ?: return@composable
            ComandaScreen(
                mesaId = mesaId,
                mesaCodigo = mesaCodigo,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
