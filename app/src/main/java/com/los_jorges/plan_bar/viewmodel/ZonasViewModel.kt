package com.los_jorges.plan_bar.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.los_jorges.plan_bar.model.Zona
import com.los_jorges.plan_bar.network.RetrofitClient
import com.los_jorges.plan_bar.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "PlanBar_Zonas"

class ZonasViewModel : ViewModel() {

    private val _zonas = MutableStateFlow<List<Zona>>(emptyList())
    val zonas: StateFlow<List<Zona>> = _zonas

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado

    fun cargar(restauranteId: Int) {
        viewModelScope.launch {
            if (_zonas.value.isEmpty()) _loading.value = true
            _error.value = null
            try {
                val r = RetrofitClient.api.getZonas(restauranteId)
                if (r.isSuccessful) {
                    val lista = r.body()?.zonas ?: emptyList()
                    _zonas.value = lista
                    SessionManager.actualizarZonasDB(lista)
                } else {
                    _error.value = "Error al cargar zonas"
                }
            } catch (e: Exception) {
                Log.e(TAG, "cargar", e)
                _error.value = "Error de conexión"
            }
            _loading.value = false
        }
    }

    fun guardar(restauranteId: Int, zonas: List<Zona>, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _loading.value = true
            _guardado.value = false
            try {
                val body = mapOf(
                    "restaurante_id" to restauranteId,
                    "zonas" to zonas.map { z ->
                        mapOf(
                            "id"     to z.id,
                            "clave"  to z.clave,
                            "nombre" to z.nombre,
                            "orden"  to z.orden,
                            "activo" to z.activo
                        )
                    }
                )
                val r = RetrofitClient.api.actualizarZonas(body)
                if (r.isSuccessful && r.body()?.success == true) {
                    _zonas.value = zonas
                    SessionManager.actualizarZonasDB(zonas)
                    _guardado.value = true
                    onDone(true)
                } else {
                    _error.value = r.body()?.error ?: "Error al guardar"
                    onDone(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "guardar", e)
                _error.value = "Error de conexión"
                onDone(false)
            }
            _loading.value = false
        }
    }

    fun resetGuardado() {
        _guardado.value = false
    }

    fun resetError() {
        _error.value = null
    }
}
