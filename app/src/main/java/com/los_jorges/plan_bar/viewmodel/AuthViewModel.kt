package com.los_jorges.plan_bar.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.los_jorges.plan_bar.model.LoginRequest
import com.los_jorges.plan_bar.model.RegisterRequest
import com.los_jorges.plan_bar.model.Trabajador
import com.los_jorges.plan_bar.network.RetrofitClient
import com.los_jorges.plan_bar.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "PlanBar_Auth"

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val trabajador: Trabajador) : AuthState()
    data class Error(val mensaje: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Rellena todos los campos")
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
                        _state.value =
                            AuthState.Error("Solo los administradores pueden acceder aquí")
                        return@launch
                    }
                    SessionManager.loginAdmin(trabajador)
                    _state.value = AuthState.Success(trabajador)
                } else {
                    _state.value = AuthState.Error("Email o contraseña incorrectos")
                }
            } catch (e: Exception) {
                Log.e(TAG, "LOGIN: excepcion", e)
                _state.value = AuthState.Error("Error de conexión. Revisa el internet")
            }
        }
    }

    fun loginTrabajador(email: String, password: String, onSuccess: (Trabajador) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Rellena todos los campos")
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
                        _state.value =
                            AuthState.Error("Este trabajador no pertenece a este restaurante")
                        return@launch
                    }
                    if (trabajador.rol == "admin") {
                        _state.value =
                            AuthState.Error("Usa el acceso de administrador para entrar como admin")
                        return@launch
                    }
                    SessionManager.loginTrabajador(trabajador)
                    _state.value = AuthState.Idle
                    onSuccess(trabajador)
                } else {
                    _state.value = AuthState.Error("Email o contraseña incorrectos")
                }
            } catch (e: Exception) {
                Log.e(TAG, "LOGIN_TRABAJADOR: excepcion", e)
                _state.value = AuthState.Error("Error de conexión")
            }
        }
    }

    fun verificarAdmin(password: String, onSuccess: () -> Unit) {
        if (password.isBlank()) {
            _state.value = AuthState.Error("Introduce la contraseña")
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
                    _state.value = AuthState.Error("Contraseña de administrador incorrecta")
                }
            } catch (e: Exception) {
                Log.e(TAG, "VERIFICAR_ADMIN: excepcion", e)
                _state.value = AuthState.Error("Error de conexión")
            }
        }
    }

    fun registrarRestaurante(
        nombre: String, email: String, direccion: String, telefono: String,
        adminNombre: String, adminEmail: String, adminPassword: String,
        onSuccess: () -> Unit
    ) {
        if (nombre.isBlank() || email.isBlank() || adminNombre.isBlank() ||
            adminEmail.isBlank() || adminPassword.isBlank()
        ) {
            _state.value = AuthState.Error("Rellena todos los campos obligatorios")
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

    fun resetState() {
        _state.value = AuthState.Idle
    }
}
