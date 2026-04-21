package com.los_jorges.plan_bar.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.los_jorges.plan_bar.model.Pedido
import com.los_jorges.plan_bar.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "PlanBar_Pedidos"

class PedidosViewModel : ViewModel() {

    private val _pedido  = MutableStateFlow<Pedido?>(null)
    val pedido: StateFlow<Pedido?> = _pedido

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error   = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** Abre el pedido abierto de la mesa o crea uno nuevo si no existe */
    fun abrirOCrearPedido(
        restauranteId: Int,
        mesaId: Int,
        trabajadorId: Int?,
        onPedidoId: (Int) -> Unit
    ) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val r = RetrofitClient.api.getPedidoPorMesa(mesaId)
                if (r.isSuccessful) {
                    val existente = r.body()?.pedido
                    if (existente != null) {
                        _loading.value = false
                        onPedidoId(existente.id)
                    } else {
                        val body = mutableMapOf<String, Any>(
                            "restaurante_id" to restauranteId,
                            "mesa_id"        to mesaId
                        )
                        if (trabajadorId != null) body["trabajador_id"] = trabajadorId
                        val cr = RetrofitClient.api.crearPedido(body)
                        _loading.value = false
                        if (cr.isSuccessful && cr.body()?.success == true) {
                            onPedidoId(cr.body()!!.pedido_id!!)
                        } else {
                            _error.value = cr.body()?.error ?: "Error al abrir la mesa"
                        }
                    }
                } else {
                    _loading.value = false
                    _error.value = "Error al consultar la mesa"
                }
            } catch (e: Exception) {
                Log.e(TAG, "abrirOCrearPedido", e)
                _loading.value = false
                _error.value = "Error de conexión"
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
                    "pedido_id"   to pedidoId,
                    "producto_id" to productoId,
                    "cantidad"    to cantidad
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

    fun actualizarCantidad(pedidoProductoId: Int, cantidad: Int, pedidoId: Int, onDone: (Boolean, String?) -> Unit) {
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

    fun clearError() { _error.value = null }
}
