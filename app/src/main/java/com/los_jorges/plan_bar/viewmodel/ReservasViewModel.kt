package com.los_jorges.plan_bar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.los_jorges.plan_bar.model.Reserva
import com.los_jorges.plan_bar.model.RestauranteItem
import com.los_jorges.plan_bar.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReservasViewModel : ViewModel() {

    private val _reservas = MutableStateFlow<List<Reserva>>(emptyList())
    val reservas: StateFlow<List<Reserva>> = _reservas

    private val _restaurantes = MutableStateFlow<List<RestauranteItem>>(emptyList())
    val restaurantes: StateFlow<List<RestauranteItem>> = _restaurantes

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun cargar(restauranteId: Int, fecha: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val r = RetrofitClient.api.getReservas(restauranteId, fecha)
                if (r.isSuccessful) _reservas.value = r.body()?.reservas ?: emptyList()
                else _error.value = "Error al cargar reservas"
            } catch (e: Exception) {
                _error.value = "Error de conexión"
            } finally {
                _loading.value = false
            }
        }
    }

    fun cargarRestaurantes() {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.getRestaurantes()
                if (r.isSuccessful) _restaurantes.value = r.body()?.restaurantes ?: emptyList()
            } catch (_: Exception) {
            }
        }
    }

    fun crear(
        restauranteId: Int, nombre: String, telefono: String, correo: String,
        numPersonas: Int, fecha: String, hora: String, notas: String,
        onDone: (Boolean, String?, String?) -> Unit  // ok, error, codigo
    ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val body = mutableMapOf<String, Any>(
                    "restaurante_id" to restauranteId,
                    "nombre" to nombre,
                    "telefono" to telefono,
                    "num_personas" to numPersonas,
                    "fecha" to fecha,
                    "hora" to hora
                )
                if (correo.isNotBlank()) body["correo"] = correo
                if (notas.isNotBlank()) body["notas"] = notas

                val r = RetrofitClient.api.crearReserva(body)
                if (r.isSuccessful && r.body()?.success == true) {
                    onDone(true, null, r.body()?.codigo)
                } else {
                    onDone(false, r.body()?.error ?: "Error al crear", null)
                }
            } catch (e: Exception) {
                onDone(false, "Error de conexión", null)
            } finally {
                _loading.value = false
            }
        }
    }

    fun cambiarEstado(
        id: Int,
        estado: String,
        restauranteId: Int,
        fecha: String,
        onDone: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.editarReserva(mapOf("id" to id, "estado" to estado))
                if (r.isSuccessful && r.body()?.success == true) {
                    cargar(restauranteId, fecha)
                    onDone(true)
                } else onDone(false)
            } catch (_: Exception) {
                onDone(false)
            }
        }
    }

    fun eliminar(id: Int, restauranteId: Int, fecha: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.eliminarReserva(mapOf("id" to id))
                if (r.isSuccessful && r.body()?.success == true) {
                    cargar(restauranteId, fecha)
                    onDone(true, null)
                } else onDone(false, "Error al eliminar")
            } catch (_: Exception) {
                onDone(false, "Error de conexión")
            }
        }
    }

    fun fechaHoy(): String = sdf.format(Date())
    fun desplazarFecha(fechaStr: String, dias: Int): String {
        val cal = Calendar.getInstance().apply { time = sdf.parse(fechaStr) ?: Date() }
        cal.add(Calendar.DAY_OF_MONTH, dias)
        return sdf.format(cal.time)
    }

    fun formatearFechaLegible(fechaStr: String): String = try {
        val entrada = SimpleDateFormat("yyyy-MM-dd", Locale("es", "ES"))
        val salida = SimpleDateFormat("EEE d MMM yyyy", Locale("es", "ES"))
        salida.format(entrada.parse(fechaStr) ?: Date()).replaceFirstChar { it.uppercase() }
    } catch (_: Exception) {
        fechaStr
    }

    fun clearError() {
        _error.value = null
    }
}
