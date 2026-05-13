package com.los_jorges.plan_bar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.los_jorges.plan_bar.model.EstadisticasResponse
import com.los_jorges.plan_bar.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EstadisticasViewModel : ViewModel() {

    enum class Periodo { HOY, SEMANA, MES }

    private val _periodo = MutableStateFlow(Periodo.HOY)
    val periodo: StateFlow<Periodo> = _periodo

    private val _stats = MutableStateFlow<EstadisticasResponse?>(null)
    val stats: StateFlow<EstadisticasResponse?> = _stats

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun cargar(restauranteId: Int, periodo: Periodo = _periodo.value) {
        _periodo.value = periodo
        val hoy = Calendar.getInstance()
        val fin = fmt.format(hoy.time)
        val inicio = when (periodo) {
            Periodo.HOY -> fin
            Periodo.SEMANA -> {
                hoy.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                fmt.format(hoy.time)
            }
            Periodo.MES -> {
                hoy.set(Calendar.DAY_OF_MONTH, 1)
                fmt.format(hoy.time)
            }
        }
        viewModelScope.launch {
            _loading.value = true
            _error.value   = null
            try {
                val r = RetrofitClient.api.getEstadisticas(restauranteId, inicio, fin)
                if (r.isSuccessful) _stats.value = r.body()
                else _error.value = "Error al cargar estadísticas"
            } catch (e: Exception) {
                _error.value = "Error de conexión"
            }
            _loading.value = false
        }
    }

    fun clearError() { _error.value = null }
}
