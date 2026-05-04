package com.los_jorges.plan_bar.session

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.los_jorges.plan_bar.model.Trabajador
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Gestión de sesión en 3 niveles:
 *  1. Restaurante (admin) — persiste en SharedPreferences
 *  2. Personal activo (trabajador seleccionado) — en memoria + prefs
 *  3. PIN verificado — solo en memoria (se pide cada vez o 1 vez/día según rol)
 */
object SessionManager {

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    // Nivel 1: Admin / Restaurante
    private val _admin = MutableStateFlow<Trabajador?>(null)
    val admin: StateFlow<Trabajador?> = _admin

    // Nivel 2: Trabajador seleccionado en el selector
    private val _trabajador = MutableStateFlow<Trabajador?>(null)
    val trabajador: StateFlow<Trabajador?> = _trabajador

    // Nivel 3: PIN verificado (en memoria — se resetea al cerrar la app)
    private val _pinVerificado = MutableStateFlow(false)
    val pinVerificado: StateFlow<Boolean> = _pinVerificado

    // Timestamp del último PIN verificado (para camareros: válido durante el día)
    private var pinVerificadoTimestamp: Long = 0L

    fun init(context: Context) {
        prefs = context.getSharedPreferences("planbar_session", Context.MODE_PRIVATE)
        _admin.value = cargar("session_admin")
        _trabajador.value = cargar("session_trabajador")
        // Si había un trabajador activo, consideramos el PIN ya verificado
        _pinVerificado.value = _trabajador.value != null
        if (_pinVerificado.value) pinVerificadoTimestamp = System.currentTimeMillis()
    }

    // ── Nivel 1: Restaurante ──────────────────────────────────────────────────

    fun loginAdmin(t: Trabajador) {
        _admin.value = t
        _trabajador.value = null
        _pinVerificado.value = false
        pinVerificadoTimestamp = 0L
        guardar("session_admin", t)
        prefs.edit().remove("session_trabajador").apply()
    }

    // ── Nivel 2: Selector de personal ────────────────────────────────────────

    /** Selecciona un trabajador para pedir PIN a continuación */
    fun seleccionarTrabajador(t: Trabajador) {
        _trabajador.value = t
        _pinVerificado.value = false
        pinVerificadoTimestamp = 0L
        guardar("session_trabajador", t)
    }

    /** Llama esto después de verificar el PIN correctamente */
    fun confirmarPin() {
        _pinVerificado.value = true
        pinVerificadoTimestamp = System.currentTimeMillis()
    }

    /**
     * ¿Hay que pedir PIN ahora?
     * - Jefe de sala: siempre
     * - Camarero/cocina: solo si no se ha verificado hoy
     */
    fun necesitaPin(): Boolean = true

    // ── Cerrar sesiones ───────────────────────────────────────────────────────

    /** Vuelve al selector de personal (desactiva trabajador actual) */
    fun desactivarPersonal() {
        _trabajador.value = null
        _pinVerificado.value = false
        pinVerificadoTimestamp = 0L
        prefs.edit().remove("session_trabajador").apply()
    }

    /** Cierra todo (logout completo) */
    fun cerrarSesionTotal() {
        _admin.value = null
        _trabajador.value = null
        _pinVerificado.value = false
        pinVerificadoTimestamp = 0L
        prefs.edit().remove("session_admin").remove("session_trabajador").apply()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    val restauranteId: Int get() = _admin.value?.restaurante_id ?: 1
    val adminEmail: String get() = _admin.value?.email ?: ""
    val restauranteNombre: String get() = _admin.value?.restaurante_nombre ?: ""
    val hayTrabajadorActivo: Boolean get() = _trabajador.value != null && _pinVerificado.value
    val hayRestauranteActivo: Boolean get() = _admin.value != null

    private fun guardar(key: String, t: Trabajador) {
        prefs.edit().putString(key, gson.toJson(t)).apply()
    }

    private fun cargar(key: String): Trabajador? {
        val json = prefs.getString(key, null) ?: return null
        return runCatching { gson.fromJson(json, Trabajador::class.java) }.getOrNull()
    }
}
