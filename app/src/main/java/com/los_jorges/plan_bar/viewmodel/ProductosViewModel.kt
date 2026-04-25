package com.los_jorges.plan_bar.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.los_jorges.plan_bar.model.Producto
import com.los_jorges.plan_bar.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "PlanBar_Productos"

class ProductosViewModel : ViewModel() {

    private val _productos = MutableStateFlow<List<Producto>>(emptyList())
    val productos: StateFlow<List<Producto>> = _productos

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun cargar(restauranteId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val r = RetrofitClient.api.getProductos(restauranteId)
                if (r.isSuccessful) _productos.value = r.body()?.productos ?: emptyList()
                else _error.value = "Error al cargar productos"
            } catch (e: Exception) {
                Log.e(TAG, "cargar", e)
                _error.value = "Error de conexión"
            }
            _loading.value = false
        }
    }

    fun crear(
        restauranteId: Int, nombre: String, categoria: String, descripcion: String,
        precio: Double, disponible: Boolean, onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.crearProducto(
                    mapOf(
                        "restaurante_id" to restauranteId, "nombre" to nombre,
                        "categoria" to categoria, "descripcion" to descripcion,
                        "precio" to precio, "disponible" to disponible
                    )
                )
                if (r.isSuccessful && r.body()?.success == true) {
                    cargar(restauranteId); onDone(true, null)
                } else onDone(false, r.body()?.error ?: "Error al crear producto")
            } catch (e: Exception) {
                Log.e(TAG, "crear", e); onDone(false, "Error de conexión")
            }
        }
    }

    fun editar(
        restauranteId: Int, id: Int, nombre: String, categoria: String, descripcion: String,
        precio: Double, disponible: Boolean, onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.editarProducto(
                    mapOf(
                        "id" to id, "nombre" to nombre, "categoria" to categoria,
                        "descripcion" to descripcion, "precio" to precio, "disponible" to disponible
                    )
                )
                if (r.isSuccessful && r.body()?.success == true) {
                    cargar(restauranteId); onDone(true, null)
                } else onDone(false, r.body()?.error ?: "Error al editar producto")
            } catch (e: Exception) {
                Log.e(TAG, "editar", e); onDone(false, "Error de conexión")
            }
        }
    }

    fun eliminar(restauranteId: Int, id: Int, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.eliminarProducto(mapOf("id" to id))
                if (r.isSuccessful && r.body()?.success == true) {
                    cargar(restauranteId); onDone(true, null)
                } else onDone(false, r.body()?.error ?: "Error al eliminar producto")
            } catch (e: Exception) {
                Log.e(TAG, "eliminar", e); onDone(false, "Error de conexión")
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
