package com.los_jorges.plan_bar.network

import com.los_jorges.plan_bar.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Auth ---
    @POST("auth/auth_login.php")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

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

    // --- Productos ---
    @GET("productos/productos_obtener.php")
    suspend fun getProductos(@Query("restaurante_id") restauranteId: Int): Response<ProductosResponse>

    @POST("productos/productos_crear.php")
    suspend fun crearProducto(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<CreateResponse>

    @POST("productos/productos_editar.php")
    suspend fun editarProducto(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

    @POST("productos/productos_eliminar.php")
    suspend fun eliminarProducto(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<SimpleResponse>

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
