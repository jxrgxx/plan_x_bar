package com.los_jorges.plan_bar.session

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.los_jorges.plan_bar.model.Trabajador
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SessionManager {

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    private val _admin = MutableStateFlow<Trabajador?>(null)
    val admin: StateFlow<Trabajador?> = _admin

    private val _trabajador = MutableStateFlow<Trabajador?>(null)
    val trabajador: StateFlow<Trabajador?> = _trabajador

    fun init(context: Context) {
        prefs = context.getSharedPreferences("planbar_session", Context.MODE_PRIVATE)
        _admin.value = cargar("session_admin")
        _trabajador.value = cargar("session_trabajador")
    }

    fun loginAdmin(t: Trabajador) {
        _admin.value = t
        _trabajador.value = null
        guardar("session_admin", t)
        prefs.edit().remove("session_trabajador").apply()
    }

    fun loginTrabajador(t: Trabajador) {
        _trabajador.value = t
        guardar("session_trabajador", t)
    }

    fun cerrarSesionTrabajador() {
        _trabajador.value = null
        prefs.edit().remove("session_trabajador").apply()
    }

    fun cerrarSesionTotal() {
        _admin.value = null
        _trabajador.value = null
        prefs.edit().remove("session_admin").remove("session_trabajador").apply()
    }

    val restauranteId: Int get() = _admin.value?.restaurante_id ?: 1
    val adminEmail: String get() = _admin.value?.email ?: ""
    val restauranteNombre: String get() = _admin.value?.restaurante_nombre ?: ""
    val hayTrabajadorActivo: Boolean get() = _trabajador.value != null

    private fun guardar(key: String, t: Trabajador) {
        prefs.edit().putString(key, gson.toJson(t)).apply()
    }

    private fun cargar(key: String): Trabajador? {
        val json = prefs.getString(key, null) ?: return null
        return runCatching { gson.fromJson(json, Trabajador::class.java) }.getOrNull()
    }
}
