package com.los_jorges.plan_bar.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.los_jorges.plan_bar.model.Mesa
import com.los_jorges.plan_bar.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "PlanBar_Mesas"

class MesasViewModel : ViewModel() {

    private val _mesas = MutableStateFlow<List<Mesa>>(emptyList())
    val mesas: StateFlow<List<Mesa>> = _mesas

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun cargar(restauranteId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val r = RetrofitClient.api.getMesas(restauranteId)
                if (r.isSuccessful) _mesas.value = r.body()?.mesas ?: emptyList()
                else _error.value = "Error al cargar mesas"
            } catch (e: Exception) {
                Log.e(TAG, "cargar", e)
                _error.value = "Error de conexión"
            }
            _loading.value = false
        }
    }

    fun crear(restauranteId: Int, codigo: String, capacidad: Int, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.crearMesa(
                    mapOf("restaurante_id" to restauranteId, "codigo" to codigo, "capacidad" to capacidad)
                )
                if (r.isSuccessful && r.body()?.success == true) {
                    cargar(restauranteId)
                    onDone(true, null)
                } else onDone(false, r.body()?.error ?: "Error al crear mesa")
            } catch (e: Exception) {
                Log.e(TAG, "crear", e)
                onDone(false, "Error de conexión")
            }
        }
    }

    fun editar(restauranteId: Int, id: Int, codigo: String, capacidad: Int, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.editarMesa(
                    mapOf("id" to id, "codigo" to codigo, "capacidad" to capacidad)
                )
                if (r.isSuccessful && r.body()?.success == true) {
                    cargar(restauranteId)
                    onDone(true, null)
                } else onDone(false, r.body()?.error ?: "Error al editar mesa")
            } catch (e: Exception) {
                Log.e(TAG, "editar", e)
                onDone(false, "Error de conexión")
            }
        }
    }

    fun eliminar(restauranteId: Int, id: Int, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.eliminarMesa(mapOf("id" to id))
                if (r.isSuccessful && r.body()?.success == true) {
                    cargar(restauranteId)
                    onDone(true, null)
                } else onDone(false, r.body()?.error ?: "Error al eliminar mesa")
            } catch (e: Exception) {
                Log.e(TAG, "eliminar", e)
                onDone(false, "Error de conexión")
            }
        }
    }

    fun clearError() { _error.value = null }
}
