package com.los_jorges.plan_bar.model

data class Trabajador(
    val id: Int,
    val restaurante_id: Int,
    val nombre: String,
    val rol: String,        // "admin", "camarero", "cocina"
    val email: String,
    val activo: Boolean = true,
    val restaurante_nombre: String? = null
)

data class Mesa(
    val id: Int,
    val restaurante_id: Int,
    val codigo: String,
    val capacidad: Int,
    val estado: String,     // "libre", "ocupada", "reservada"
    val posX: Float,
    val posY: Float
)

data class Producto(
    val id: Int,
    val restaurante_id: Int,
    val nombre: String,
    val categoria: String,  // "entrante", "principal", "postre", "bebida"
    val descripcion: String,
    val precio: Double,
    val disponible: Boolean
)

// --- Auth ---
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val success: Boolean, val trabajador: Trabajador?)

data class RegisterRequest(
    val nombre: String,
    val email: String,
    val direccion: String,
    val telefono: String,
    val admin_nombre: String,
    val admin_email: String,
    val admin_password: String
)
data class RegisterResponse(
    val success: Boolean,
    val restaurante_id: Int?,
    val trabajador: Trabajador?,
    val error: String?
)

// --- Respuestas genéricas ---
data class SimpleResponse(val success: Boolean, val error: String? = null)
data class CreateResponse(val success: Boolean, val id: Int? = null, val error: String? = null)

// --- Listas ---
data class MesasResponse(val mesas: List<Mesa>)
data class ProductosResponse(val productos: List<Producto>)
data class TrabajadoresResponse(val trabajadores: List<Trabajador>)

// --- Pedidos ---
data class PedidoProducto(
    val id: Int,
    val cantidad: Int,
    val precio_unitario: Double,
    val observaciones: String?,
    val fecha_agregado: String?,
    val nombre: String,
    val categoria: String
)

data class Pedido(
    val id: Int,
    val estado: String,
    val fecha_apertura: String?,
    val total: Double?,
    val trabajador_id: Int?,
    val mesa_id: Int,
    val mesa_codigo: String?,
    val productos: List<PedidoProducto> = emptyList()
)

data class CrearPedidoResponse(val success: Boolean, val pedido_id: Int?, val error: String?)
data class ObtenerPedidoResponse(val pedido: Pedido?)
data class AgregarProductoResponse(val success: Boolean, val precio_unitario: Double?, val error: String?)
