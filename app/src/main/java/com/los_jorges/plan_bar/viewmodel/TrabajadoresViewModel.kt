package com.los_jorges.plan_bar.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.los_jorges.plan_bar.model.Trabajador
import com.los_jorges.plan_bar.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "PlanBar_Trabajadores"

class TrabajadoresViewModel : ViewModel() {

    private val _trabajadores = MutableStateFlow<List<Trabajador>>(emptyList())
    val trabajadores: StateFlow<List<Trabajador>> = _trabajadores

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun cargar(restauranteId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val r = RetrofitClient.api.getTrabajadores(restauranteId)
                if (r.isSuccessful) _trabajadores.value = r.body()?.trabajadores ?: emptyList()
                else _error.value = "Error al cargar trabajadores"
            } catch (e: Exception) {
                Log.e(TAG, "cargar", e)
                _error.value = "Error de conexión"
            }
            _loading.value = false
        }
    }

    fun crear(
        restauranteId: Int, nombre: String, rol: String, email: String,
        password: String, pin: String = "", onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val body = mutableMapOf<String, Any>(
                    "restaurante_id" to restauranteId, "nombre" to nombre,
                    "rol" to rol, "email" to email, "password" to password
                )
                if (pin.isNotBlank()) body["pin"] = pin
                val r = RetrofitClient.api.crearTrabajador(body)
                if (r.isSuccessful && r.body()?.success == true) {
                    cargar(restauranteId); onDone(true, null)
                } else onDone(false, r.body()?.error ?: "Error al crear trabajador")
            } catch (e: Exception) {
                Log.e(TAG, "crear", e); onDone(false, "Error de conexión")
            }
        }
    }

    fun editar(
        restauranteId: Int, id: Int, nombre: String, rol: String, email: String,
        activo: Boolean, password: String, pin: String = "", onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val body = mutableMapOf<String, Any>(
                    "id" to id, "nombre" to nombre, "rol" to rol,
                    "email" to email, "activo" to activo
                )
                if (password.isNotBlank()) body["password"] = password
                if (pin.isNotBlank()) body["pin"] = pin
                val r = RetrofitClient.api.editarTrabajador(body)
                if (r.isSuccessful && r.body()?.success == true) {
                    cargar(restauranteId); onDone(true, null)
                } else onDone(false, r.body()?.error ?: "Error al editar trabajador")
            } catch (e: Exception) {
                Log.e(TAG, "editar", e); onDone(false, "Error de conexión")
            }
        }
    }

    fun eliminar(restauranteId: Int, id: Int, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.eliminarTrabajador(mapOf("id" to id))
                if (r.isSuccessful && r.body()?.success == true) {
                    cargar(restauranteId); onDone(true, null)
                } else onDone(false, r.body()?.error ?: "Error al eliminar trabajador")
            } catch (e: Exception) {
                Log.e(TAG, "eliminar", e); onDone(false, "Error de conexión")
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
