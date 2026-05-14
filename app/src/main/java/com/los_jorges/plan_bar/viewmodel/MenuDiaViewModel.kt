package com.los_jorges.plan_bar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.los_jorges.plan_bar.model.GuardarMenuDiaRequest
import com.los_jorges.plan_bar.model.MenuDia
import com.los_jorges.plan_bar.model.MenuDiaLineaRequest
import com.los_jorges.plan_bar.model.MenuDiaUso
import com.los_jorges.plan_bar.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MenuDiaViewModel : ViewModel() {

    private val _menu    = MutableStateFlow<MenuDia?>(null)
    val menu: StateFlow<MenuDia?> = _menu

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error   = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** Mapa producto_id → unidades ya usadas en pedidos activos de todos los camareros */
    private val _usoMenu = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val usoMenu: StateFlow<Map<Int, Int>> = _usoMenu

    fun cargarUso(restauranteId: Int) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.getMenuDiaUso(restauranteId)
                if (r.isSuccessful) {
                    _usoMenu.value = r.body()?.uso
                        ?.associate { it.producto_id to it.usado }
                        ?: emptyMap()
                }
            } catch (_: Exception) {}
        }
    }

    fun cargar(restauranteId: Int) {
        viewModelScope.launch {
            _loading.value = true
            _error.value   = null
            try {
                val r = RetrofitClient.api.getMenuDia(restauranteId)
                if (r.isSuccessful) _menu.value = r.body()?.menu
                else _error.value = "Error al cargar el menú"
            } catch (e: Exception) {
                _error.value = "Error de conexión"
            }
            _loading.value = false
        }
    }

    fun guardar(
        restauranteId: Int,
        precio: Double,
        lineas: List<MenuDiaLineaRequest>,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val request = GuardarMenuDiaRequest(
                    restaurante_id = restauranteId,
                    precio         = precio,
                    activo         = true,
                    lineas         = lineas
                )
                val r = RetrofitClient.api.guardarMenuDia(request)
                if (r.isSuccessful && r.body()?.success == true) {
                    onResult(true, null)
                } else {
                    onResult(false, r.body()?.error ?: "Error al guardar el menú")
                }
            } catch (e: Exception) {
                onResult(false, "Error de conexión")
            }
            _loading.value = false
        }
    }

    fun clearError() { _error.value = null }
}
