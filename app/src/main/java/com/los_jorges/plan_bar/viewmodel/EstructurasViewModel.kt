package com.los_jorges.plan_bar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.los_jorges.plan_bar.model.Estructura
import com.los_jorges.plan_bar.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EstructurasViewModel : ViewModel() {

    private val _estructuras = MutableStateFlow<List<Estructura>>(emptyList())
    val estructuras: StateFlow<List<Estructura>> = _estructuras

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun cargar(restauranteId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val r = RetrofitClient.api.getEstructuras(restauranteId)
                if (r.isSuccessful) _estructuras.value = r.body()?.estructuras ?: emptyList()
                else _error.value = "Error al cargar estructuras"
            } catch (e: Exception) {
                _error.value = "Error de conexión"
            } finally {
                _loading.value = false
            }
        }
    }

    fun crear(
        restauranteId: Int, nombre: String, color: String,
        posX: Float = 50f, posY: Float = 50f,
        ancho: Float = 120f, alto: Float = 80f,
        zona: String = "piso1",
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.crearEstructura(
                    mapOf(
                        "restaurante_id" to restauranteId,
                        "nombre" to nombre,
                        "color" to color,
                        "posX" to posX,
                        "posY" to posY,
                        "ancho" to ancho,
                        "alto" to alto,
                        "zona" to zona,
                        "rotacion" to 0f
                    )
                )
                if (r.isSuccessful && r.body()?.success == true) {
                    cargar(restauranteId)
                    onResult(true, null)
                } else {
                    onResult(false, r.body()?.error ?: "Error al crear")
                }
            } catch (e: Exception) {
                onResult(false, "Error de conexión")
            }
        }
    }

    fun actualizarTransforma(
        id: Int, restauranteId: Int,
        posX: Float, posY: Float,
        ancho: Float, alto: Float,
        rotacion: Float
    ) {
        val actual = _estructuras.value.firstOrNull { it.id == id } ?: return
        _estructuras.value = _estructuras.value.map {
            if (it.id == id) it.copy(posX = posX, posY = posY, ancho = ancho, alto = alto, rotacion = rotacion) else it
        }
        viewModelScope.launch {
            try {
                RetrofitClient.api.editarEstructura(
                    mapOf(
                        "id" to id,
                        "nombre" to actual.nombre,
                        "posX" to posX,
                        "posY" to posY,
                        "ancho" to ancho,
                        "alto" to alto,
                        "color" to actual.color,
                        "rotacion" to rotacion
                    )
                )
            } catch (_: Exception) {}
        }
    }

    fun eliminar(id: Int, restauranteId: Int, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.eliminarEstructura(mapOf("id" to id))
                if (r.isSuccessful && r.body()?.success == true) {
                    _estructuras.value = _estructuras.value.filter { it.id != id }
                    onResult(true, null)
                } else {
                    onResult(false, "Error al eliminar")
                }
            } catch (e: Exception) {
                onResult(false, "Error de conexión")
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
