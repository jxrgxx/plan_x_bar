package com.los_jorges.plan_bar.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.los_jorges.plan_bar.model.Pedido
import com.los_jorges.plan_bar.model.PedidoCocina
import com.los_jorges.plan_bar.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "PlanBar_Pedidos"

class PedidosViewModel : ViewModel() {

    private val _pedido = MutableStateFlow<Pedido?>(null)
    val pedido: StateFlow<Pedido?> = _pedido

    private val _pedidosActivos = MutableStateFlow<List<PedidoCocina>>(emptyList())
    val pedidosActivos: StateFlow<List<PedidoCocina>> = _pedidosActivos

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** Busca el pedido abierto de una mesa y lo carga completo (con productos).
     *  Si no existe, deja _pedido en null. */
    fun cargarPedidoPorMesa(mesaId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val r = RetrofitClient.api.getPedidoPorMesa(mesaId)
                if (r.isSuccessful) {
                    val pedidoBase = r.body()?.pedido
                    if (pedidoBase != null) {
                        cargarPedido(pedidoBase.id)   // carga completo con productos
                    } else {
                        _pedido.value = null
                        _loading.value = false
                    }
                } else {
                    _error.value = "Error al consultar la mesa"
                    _loading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "cargarPedidoPorMesa", e)
                _error.value = "Error de conexión"
                _loading.value = false
            }
        }
    }

    /** Crea un pedido nuevo para la mesa y lo carga. */
    fun crearNuevoPedido(restauranteId: Int, mesaId: Int, trabajadorId: Int?) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val body = mutableMapOf<String, Any>(
                    "restaurante_id" to restauranteId,
                    "mesa_id" to mesaId
                )
                if (trabajadorId != null) body["trabajador_id"] = trabajadorId
                val r = RetrofitClient.api.crearPedido(body)
                if (r.isSuccessful && r.body()?.success == true) {
                    cargarPedido(r.body()!!.pedido_id!!)
                } else {
                    _error.value = r.body()?.error ?: "Error al crear el pedido"
                    _loading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "crearNuevoPedido", e)
                _error.value = "Error de conexión"
                _loading.value = false
            }
        }
    }

    fun cargarPedido(pedidoId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val r = RetrofitClient.api.getPedidoCompleto(pedidoId)
                if (r.isSuccessful) _pedido.value = r.body()?.pedido
                else _error.value = "Error al cargar el pedido"
            } catch (e: Exception) {
                Log.e(TAG, "cargarPedido", e)
                _error.value = "Error de conexión"
            }
            _loading.value = false
        }
    }

    fun agregarProducto(
        pedidoId: Int,
        productoId: Int,
        cantidad: Int,
        observaciones: String,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val body = mutableMapOf<String, Any>(
                    "pedido_id" to pedidoId,
                    "producto_id" to productoId,
                    "cantidad" to cantidad
                )
                if (observaciones.isNotBlank()) body["observaciones"] = observaciones
                val r = RetrofitClient.api.agregarProducto(body)
                if (r.isSuccessful && r.body()?.success == true) {
                    cargarPedido(pedidoId)
                    onDone(true, null)
                } else {
                    onDone(false, r.body()?.error ?: "Error al añadir producto")
                }
            } catch (e: Exception) {
                Log.e(TAG, "agregarProducto", e)
                onDone(false, "Error de conexión")
            }
        }
    }

    fun cerrarPedido(pedidoId: Int, metodoPago: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.cerrarPedido(
                    mapOf("pedido_id" to pedidoId, "metodo_pago" to metodoPago)
                )
                if (r.isSuccessful && r.body()?.success == true) {
                    _pedido.value = null
                    onDone(true, null)
                } else {
                    val errorBody = r.errorBody()?.string()
                    Log.e(TAG, "cerrarPedido: code=${r.code()} body=${r.body()} errorBody=$errorBody")
                    onDone(false, r.body()?.error ?: "Error al cobrar el pedido")
                }
            } catch (e: Exception) {
                Log.e(TAG, "cerrarPedido", e)
                onDone(false, "Error de conexión")
            }
        }
    }

    fun eliminarProducto(pedidoProductoId: Int, pedidoId: Int, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.eliminarProductoPedido(
                    mapOf("pedido_producto_id" to pedidoProductoId)
                )
                if (r.isSuccessful && r.body()?.success == true) {
                    cargarPedido(pedidoId)
                    onDone(true, null)
                } else {
                    onDone(false, r.body()?.error ?: "Error al eliminar producto")
                }
            } catch (e: Exception) {
                Log.e(TAG, "eliminarProducto", e)
                onDone(false, "Error de conexión")
            }
        }
    }

    fun actualizarCantidad(
        pedidoProductoId: Int,
        cantidad: Int,
        pedidoId: Int,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.actualizarCantidadProducto(
                    mapOf("pedido_producto_id" to pedidoProductoId, "cantidad" to cantidad)
                )
                if (r.isSuccessful && r.body()?.success == true) {
                    cargarPedido(pedidoId)
                    onDone(true, null)
                } else {
                    onDone(false, r.body()?.error ?: "Error al actualizar cantidad")
                }
            } catch (e: Exception) {
                Log.e(TAG, "actualizarCantidad", e)
                onDone(false, "Error de conexión")
            }
        }
    }

    fun cancelarPedido(pedidoId: Int, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.cancelarPedido(mapOf("pedido_id" to pedidoId))
                if (r.isSuccessful && r.body()?.success == true) {
                    _pedido.value = null
                    onDone(true, null)
                } else {
                    onDone(false, r.body()?.error ?: "Error al cancelar el pedido")
                }
            } catch (e: Exception) {
                Log.e(TAG, "cancelarPedido", e)
                onDone(false, "Error de conexión")
            }
        }
    }

    fun enviarACocina(pedidoId: Int, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.enviarACocina(mapOf("pedido_id" to pedidoId))
                if (r.isSuccessful && r.body()?.success == true) {
                    cargarPedido(pedidoId)
                    onDone(true, null)
                } else {
                    onDone(false, r.body()?.error ?: "Error al enviar")
                }
            } catch (e: Exception) {
                Log.e(TAG, "enviarACocina", e)
                onDone(false, "Error de conexión")
            }
        }
    }

    fun cargarPedidosActivos(restauranteId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val r = RetrofitClient.api.getPedidosActivosCocina(restauranteId)
                if (r.isSuccessful) _pedidosActivos.value = r.body()?.pedidos ?: emptyList()
                else _error.value = "Error al cargar pedidos"
            } catch (e: Exception) {
                Log.e(TAG, "cargarPedidosActivos", e)
                _error.value = "Error de conexión"
            }
            _loading.value = false
        }
    }

    fun iniciarPollingCocina(restauranteId: Int) {
        viewModelScope.launch {
            while (true) {
                try {
                    val r = RetrofitClient.api.getPedidosActivosCocina(restauranteId)
                    if (r.isSuccessful) _pedidosActivos.value = r.body()?.pedidos ?: emptyList()
                } catch (_: Exception) {
                }
                delay(5_000)
            }
        }
    }

    fun iniciarPollingCamarero(pedidoId: Int) {
        viewModelScope.launch {
            while (true) {
                delay(5_000)
                try {
                    val r = RetrofitClient.api.getPedidoCompleto(pedidoId)
                    if (r.isSuccessful) _pedido.value = r.body()?.pedido
                } catch (_: Exception) {
                }
            }
        }
    }

    fun marcarPedidoListo(pedidoId: Int, restauranteId: Int, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.marcarPedidoListo(mapOf("pedido_id" to pedidoId))
                if (r.isSuccessful && r.body()?.success == true) {
                    _pedidosActivos.value = _pedidosActivos.value.filter { it.id != pedidoId }
                    onDone(true, null)
                } else {
                    onDone(false, r.body()?.error ?: "Error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "marcarPedidoListo", e)
                onDone(false, "Error de conexión")
            }
        }
    }

    fun marcarPlato(
        pedidoProductoId: Int,
        nuevoEstado: String,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.marcarPlato(
                    mapOf("pedido_producto_id" to pedidoProductoId, "estado" to nuevoEstado)
                )
                if (r.isSuccessful && r.body()?.success == true) onDone(true, null)
                else onDone(false, r.body()?.error ?: "Error")
            } catch (e: Exception) {
                Log.e(TAG, "marcarPlato", e)
                onDone(false, "Error de conexión")
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
