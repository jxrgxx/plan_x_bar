package com.los_jorges.plan_bar.network

import com.los_jorges.plan_bar.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Auth ---
    @POST("auth/auth_login.php")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/verificar_pin.php")
    suspend fun verificarPin(@Body request: PinVerifyRequest): Response<PinVerifyResponse>

    @POST("auth/set_pin.php")
    suspend fun setPin(@Body request: SetPinRequest): Response<SetPinResponse>

    @POST("restaurante/restaurante_registrar.php")
    suspend fun registrarRestaurante(@Body request: RegisterRequest): Response<RegisterResponse>

    // --- Mesas ---
    @GET("mesas/mesas_obtener.php")
    suspend fun getMesas(@Query("restaurante_id") restauranteId: Int): Response<MesasResponse>

    @POST("mesas/mesas_crear.php")
    suspend fun crearMesa(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<CreateResponse>

    @POST("mesas/mesas_editar.php")
    suspend fun editarMesa(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    @POST("mesas/mesas_eliminar.php")
    suspend fun eliminarMesa(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    @POST("mesas/mesas_actualizar_posicion.php")
    suspend fun actualizarPosicionMesa(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    // --- Productos ---
    @GET("productos/productos_obtener.php")
    suspend fun getProductos(@Query("restaurante_id") restauranteId: Int): Response<ProductosResponse>

    @POST("productos/productos_crear.php")
    suspend fun crearProducto(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<CreateResponse>

    @POST("productos/productos_editar.php")
    suspend fun editarProducto(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    @POST("productos/productos_eliminar.php")
    suspend fun eliminarProducto(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    // --- Pedidos ---
    @POST("pedidos/pedidos_crear.php")
    suspend fun crearPedido(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<CrearPedidoResponse>

    @GET("pedidos/pedidos_obtener.php")
    suspend fun getPedidoPorMesa(@Query("mesa_id") mesaId: Int): Response<ObtenerPedidoResponse>

    @GET("pedidos/pedidos_obtener.php")
    suspend fun getPedidoCompleto(@Query("pedido_id") pedidoId: Int): Response<ObtenerPedidoResponse>

    @POST("pedidos/pedidos_agregar_producto.php")
    suspend fun agregarProducto(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<AgregarProductoResponse>

    @POST("pedidos/pedidos_cerrar.php")
    suspend fun cerrarPedido(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    @POST("pedidos/pedidos_cancelar.php")
    suspend fun cancelarPedido(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    @POST("pedidos/pedidos_enviar_cocina.php")
    suspend fun enviarACocina(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    @GET("pedidos/pedidos_activos_cocina.php")
    suspend fun getPedidosActivosCocina(@Query("restaurante_id") restauranteId: Int): Response<PedidosActivosResponse>

    @POST("pedidos/pedidos_marcar_plato.php")
    suspend fun marcarPlato(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    @POST("pedidos/pedidos_marcar_listo.php")
    suspend fun marcarPedidoListo(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    @POST("pedidos/pedidos_eliminar_producto.php")
    suspend fun eliminarProductoPedido(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    @POST("pedidos/pedidos_actualizar_cantidad.php")
    suspend fun actualizarCantidadProducto(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    // --- Reservas ---
    @GET("reservas/reservas_obtener.php")
    suspend fun getReservas(
        @Query("restaurante_id") restauranteId: Int,
        @Query("fecha") fecha: String
    ): Response<ReservasResponse>

    @POST("reservas/reservas_crear.php")
    suspend fun crearReserva(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<CrearReservaResponse>

    @POST("reservas/reservas_editar.php")
    suspend fun editarReserva(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    @POST("reservas/reservas_eliminar.php")
    suspend fun eliminarReserva(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    @GET("restaurante/restaurantes_obtener.php")
    suspend fun getRestaurantes(): Response<RestaurantesResponse>

    // --- Estructuras ---
    @GET("estructuras/estructuras_obtener.php")
    suspend fun getEstructuras(@Query("restaurante_id") restauranteId: Int): Response<EstructurasResponse>

    @POST("estructuras/estructuras_crear.php")
    suspend fun crearEstructura(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<CreateResponse>

    @POST("estructuras/estructuras_editar.php")
    suspend fun editarEstructura(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    @POST("estructuras/estructuras_eliminar.php")
    suspend fun eliminarEstructura(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    // --- Trabajadores ---
    @GET("trabajadores/trabajadores_obtener.php")
    suspend fun getTrabajadores(@Query("restaurante_id") restauranteId: Int): Response<TrabajadoresResponse>

    @POST("trabajadores/trabajadores_crear.php")
    suspend fun crearTrabajador(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<CreateResponse>

    @POST("trabajadores/trabajadores_editar.php")
    suspend fun editarTrabajador(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    @POST("trabajadores/trabajadores_eliminar.php")
    suspend fun eliminarTrabajador(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>
}
