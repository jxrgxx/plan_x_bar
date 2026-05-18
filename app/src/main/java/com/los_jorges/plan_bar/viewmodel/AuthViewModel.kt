package com.los_jorges.plan_bar.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.los_jorges.plan_bar.model.LoginRequest
import com.los_jorges.plan_bar.model.PinVerifyRequest
import com.los_jorges.plan_bar.model.SetPinRequest
import com.los_jorges.plan_bar.model.RegisterRequest
import com.los_jorges.plan_bar.model.Trabajador
import com.los_jorges.plan_bar.network.RetrofitClient
import com.los_jorges.plan_bar.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "PlanBar_Auth"

private suspend fun cargarZonasEnBackground(restauranteId: Int) {
    try {
        val r = RetrofitClient.api.getZonas(restauranteId)
        if (r.isSuccessful) {
            r.body()?.zonas?.let { SessionManager.actualizarZonasDB(it) }
        }
    } catch (e: Exception) {
        Log.w(TAG, "cargarZonasEnBackground: no se pudieron cargar las zonas", e)
        // No es crítico — la app sigue funcionando con los datos de SharedPrefs
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val trabajador: Trabajador) : AuthState()
    data class Error(val mensaje: String) : AuthState()
}

data class AuthErrorMessages(
    val rellenaTodosLosCampos: String = "Rellena todos los campos",
    val rellenaCamposObligatorios: String = "Rellena todos los campos obligatorios",
    val soloAdministradores: String = "Solo los administradores pueden acceder aquí",
    val emailOContrasenaIncorrectos: String = "Email o contraseña incorrectos",
    val errorDeConexionRevisa: String = "Error de conexión. Revisa el internet",
    val trabajadorNoPertenece: String = "Este trabajador no pertenece a este restaurante",
    val usaAccesoAdmin: String = "Usa el acceso de administrador para entrar como admin",
    val errorDeConexion: String = "Error de conexión",
    val introduceContrasena: String = "Introduce la contraseña",
    val contrasenaAdminIncorrecta: String = "Contraseña de administrador incorrecta",
    val introducePin: String = "Introduce tu PIN",
    val pinIncorrecto: String = "PIN incorrecto",
    val pinDebeTener4Digitos: String = "El PIN debe tener 4 dígitos",
    val errorGuardarPin: String = "Error al guardar el PIN"
)

class AuthViewModel : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    fun login(email: String, password: String, msgs: AuthErrorMessages = AuthErrorMessages()) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error(msgs.rellenaTodosLosCampos)
            return
        }
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                Log.d(TAG, "LOGIN: enviando email=$email")
                val response = RetrofitClient.api.login(LoginRequest(email, password))
                Log.d(
                    TAG,
                    "LOGIN: code=${response.code()} body=${response.body()} error=${
                        response.errorBody()?.string()
                    }"
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    val trabajador = response.body()!!.trabajador!!
                    if (trabajador.rol != "admin") {
                        _state.value = AuthState.Error(msgs.soloAdministradores)
                        return@launch
                    }
                    SessionManager.loginAdmin(trabajador)
                    launch { cargarZonasEnBackground(trabajador.restaurante_id) }
                    _state.value = AuthState.Success(trabajador)
                } else {
                    _state.value = AuthState.Error(msgs.emailOContrasenaIncorrectos)
                }
            } catch (e: Exception) {
                Log.e(TAG, "LOGIN: excepcion", e)
                _state.value = AuthState.Error(msgs.errorDeConexionRevisa)
            }
        }
    }

    fun loginTrabajador(
        email: String,
        password: String,
        onSuccess: (Trabajador) -> Unit,
        msgs: AuthErrorMessages = AuthErrorMessages()
    ) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error(msgs.rellenaTodosLosCampos)
            return
        }
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                Log.d(TAG, "LOGIN_TRABAJADOR: email=$email")
                val response = RetrofitClient.api.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body()?.success == true) {
                    val trabajador = response.body()!!.trabajador!!
                    if (trabajador.restaurante_id != SessionManager.restauranteId) {
                        _state.value = AuthState.Error(msgs.trabajadorNoPertenece)
                        return@launch
                    }
                    if (trabajador.rol == "admin") {
                        _state.value = AuthState.Error(msgs.usaAccesoAdmin)
                        return@launch
                    }
                    SessionManager.seleccionarTrabajador(trabajador)
                    if (!SessionManager.zonasDBCargadas) {
                        launch { cargarZonasEnBackground(trabajador.restaurante_id) }
                    }
                    _state.value = AuthState.Idle
                    onSuccess(trabajador)
                } else {
                    _state.value = AuthState.Error(msgs.emailOContrasenaIncorrectos)
                }
            } catch (e: Exception) {
                Log.e(TAG, "LOGIN_TRABAJADOR: excepcion", e)
                _state.value = AuthState.Error(msgs.errorDeConexion)
            }
        }
    }

    fun verificarAdmin(
        password: String,
        onSuccess: () -> Unit,
        msgs: AuthErrorMessages = AuthErrorMessages()
    ) {
        if (password.isBlank()) {
            _state.value = AuthState.Error(msgs.introduceContrasena)
            return
        }
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val email = SessionManager.adminEmail
                Log.d(TAG, "VERIFICAR_ADMIN: email=$email")
                val response = RetrofitClient.api.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body()?.success == true) {
                    _state.value = AuthState.Idle
                    onSuccess()
                } else {
                    _state.value = AuthState.Error(msgs.contrasenaAdminIncorrecta)
                }
            } catch (e: Exception) {
                Log.e(TAG, "VERIFICAR_ADMIN: excepcion", e)
                _state.value = AuthState.Error(msgs.errorDeConexion)
            }
        }
    }

    fun registrarRestaurante(
        nombre: String, email: String, direccion: String, telefono: String,
        adminNombre: String, adminEmail: String, adminPassword: String,
        onSuccess: () -> Unit,
        msgs: AuthErrorMessages = AuthErrorMessages()
    ) {
        if (nombre.isBlank() || email.isBlank() || adminNombre.isBlank() ||
            adminEmail.isBlank() || adminPassword.isBlank()
        ) {
            _state.value = AuthState.Error(msgs.rellenaCamposObligatorios)
            return
        }
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                Log.d(TAG, "REGISTER: enviando nombre=$nombre email=$email adminEmail=$adminEmail")
                val response = RetrofitClient.api.registrarRestaurante(
                    RegisterRequest(
                        nombre,
                        email,
                        direccion,
                        telefono,
                        adminNombre,
                        adminEmail,
                        adminPassword
                    )
                )
                val errorBody = response.errorBody()?.string()
                Log.d(
                    TAG,
                    "REGISTER: code=${response.code()} body=${response.body()} errorBody=$errorBody"
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    _state.value = AuthState.Idle
                    onSuccess()
                } else {
                    val error = response.body()?.error ?: errorBody ?: "Error al registrar"
                    Log.e(TAG, "REGISTER: fallo -> $error")
                    _state.value = AuthState.Error(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "REGISTER: excepcion", e)
                _state.value = AuthState.Error("Error: ${e.message}")
            }
        }
    }

    fun verificarPin(
        trabajadorId: Int,
        pin: String,
        onSuccess: () -> Unit,
        msgs: AuthErrorMessages = AuthErrorMessages()
    ) {
        if (pin.isBlank()) {
            _state.value = AuthState.Error(msgs.introducePin)
            return
        }
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                Log.d(TAG, "VERIFICAR_PIN: trabajador_id=$trabajadorId")
                val response = RetrofitClient.api.verificarPin(PinVerifyRequest(trabajadorId, pin))
                if (response.isSuccessful && response.body()?.success == true) {
                    SessionManager.confirmarPin()
                    _state.value = AuthState.Idle
                    onSuccess()
                } else {
                    val msg = response.body()?.error ?: msgs.pinIncorrecto
                    _state.value = AuthState.Error(msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "VERIFICAR_PIN: excepcion", e)
                _state.value = AuthState.Error(msgs.errorDeConexion)
            }
        }
    }

    fun setPin(
        trabajadorId: Int,
        pin: String,
        onSuccess: () -> Unit,
        msgs: AuthErrorMessages = AuthErrorMessages()
    ) {
        if (pin.length != 4) {
            _state.value = AuthState.Error(msgs.pinDebeTener4Digitos)
            return
        }
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                Log.d(TAG, "SET_PIN: trabajador_id=$trabajadorId")
                val response = RetrofitClient.api.setPin(SetPinRequest(trabajadorId, pin))
                if (response.isSuccessful && response.body()?.success == true) {
                    SessionManager.confirmarPin()
                    _state.value = AuthState.Idle
                    onSuccess()
                } else {
                    val msg = response.body()?.error ?: msgs.errorGuardarPin
                    _state.value = AuthState.Error(msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "SET_PIN: excepcion", e)
                _state.value = AuthState.Error(msgs.errorDeConexion)
            }
        }
    }

    fun resetState() {
        _state.value = AuthState.Idle
    }
}
