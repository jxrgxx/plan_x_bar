package com.los_jorges.plan_bar.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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

    // ── Configuración de espacios de trabajo ─────────────────────────────────

    /** Claves internas fijas de las zonas (compatibles con datos existentes) */
    val ZONA_KEYS = listOf("piso1", "piso2", "terraza", "zona4")
    private val ZONA_NOMBRES_DEFAULT = listOf("Piso 1", "Piso 2", "Terraza", "Zona 4")

    var numZonas: Int = 3
        private set

    var zonaNombres: List<String> = ZONA_NOMBRES_DEFAULT
        private set

    /** Lista de (key, nombre) activos según la configuración actual */
    val zonas: List<Pair<String, String>>
        get() = ZONA_KEYS.take(numZonas).mapIndexed { i, key ->
            key to (zonaNombres.getOrElse(i) { "Zona ${i + 1}" })
        }

    fun saveZonaConfig(num: Int, nombres: List<String>) {
        numZonas = num.coerceIn(1, 4)
        zonaNombres = (nombres + ZONA_NOMBRES_DEFAULT).take(4)
        prefs.edit()
            .putInt("num_zonas", numZonas)
            .putString("zona_nombres", zonaNombres.joinToString("|"))
            .apply()
    }

    fun init(context: Context) {
        prefs = context.getSharedPreferences("planbar_session", Context.MODE_PRIVATE)
        _admin.value = cargar("session_admin")
        // El trabajador seleccionado NO se restaura al abrir la app —
        // siempre hay que pasar por el selector de personal
        _trabajador.value = null
        _pinVerificado.value = false
        // Cargar configuración de zonas
        numZonas = prefs.getInt("num_zonas", 3)
        zonaNombres = prefs.getString("zona_nombres", null)
            ?.split("|")
            ?.let { saved -> (saved + ZONA_NOMBRES_DEFAULT).take(4) }
            ?: ZONA_NOMBRES_DEFAULT
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
    }

    /** Llama esto después de verificar el PIN correctamente */
    fun confirmarPin() {
        _pinVerificado.value = true
        pinVerificadoTimestamp = System.currentTimeMillis()
    }

    /**
     * ¿Hay que pedir PIN ahora?
     * - Jefe de sala: siempre (por cada acceso)
     * - Camarero/cocina: solo si no se ha verificado en las últimas 24 horas
     */
    fun necesitaPin(): Boolean {
        val t = _trabajador.value ?: return true
        if (t.rol == "jefe_sala") return true
        if (pinVerificadoTimestamp == 0L) return true
        val elapsed = System.currentTimeMillis() - pinVerificadoTimestamp
        return elapsed >= 24 * 60 * 60 * 1000L
    }

    // ── Cerrar sesiones ───────────────────────────────────────────────────────

    /** Vuelve al selector de personal (desactiva trabajador actual) */
    fun desactivarPersonal() {
        _trabajador.value = null
        _pinVerificado.value = false
        pinVerificadoTimestamp = 0L
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

    val restauranteId: Int get() {
        val id = _admin.value?.restaurante_id
        if (id == null) Log.e("SessionManager", "restauranteId accessed with no admin logged in — falling back to 1")
        return id ?: 1
    }
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
