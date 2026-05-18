package com.los_jorges.plan_bar.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.viewmodel.AuthErrorMessages
import com.los_jorges.plan_bar.viewmodel.AuthState
import com.los_jorges.plan_bar.viewmodel.AuthViewModel
import com.los_jorges.plan_bar.ui.theme.LocalStrings

private const val PIN_LENGTH = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinScreen(
    trabajadorId: Int,
    trabajadorNombre: String,
    tienePinPrevio: Boolean,           // false = primera vez → flujo de creación
    viewModel: AuthViewModel,
    onPinCorrecto: () -> Unit,
    onBack: () -> Unit
) {
    val s = LocalStrings.current
    val state by viewModel.state.collectAsState()

    // Flujo: si no tiene PIN → "crear" (paso 1) → "confirmar" (paso 2) → guardar
    // Si tiene PIN → "verificar" directamente
    var paso by remember { mutableStateOf(if (tienePinPrevio) Paso.VERIFICAR else Paso.CREAR) }
    var pin by remember { mutableStateOf("") }
    var pinCreado by remember { mutableStateOf("") } // guarda el primer PIN en la confirmación

    val authMsgs = AuthErrorMessages(
        introducePin = s.introducePin,
        pinIncorrecto = s.pinIncorrecto,
        pinDebeTener4Digitos = s.pinDebeTener4Digitos,
        errorGuardarPin = s.errorGuardarPin,
        errorDeConexion = s.errorDeConexion
    )

    // Limpiar error al escribir (solo si el usuario teclea, no al borrar por fallo)
    LaunchedEffect(pin) {
        if (pin.isNotEmpty() && state is AuthState.Error) viewModel.resetState()
    }

    // Borrar PIN al fallar
    LaunchedEffect(state) {
        if (state is AuthState.Error) pin = ""
    }

    val titulo = when (paso) {
        Paso.VERIFICAR -> s.introducePin
        Paso.CREAR -> s.creaTuPin
        Paso.CONFIRMAR -> s.confirmaTuPin
    }
    val subtitulo = when (paso) {
        Paso.VERIFICAR -> "${s.hola} $trabajadorNombre"
        Paso.CREAR -> s.primeraVezPinFmt.format(PIN_LENGTH)
        Paso.CONFIRMAR -> s.repitePin
    }

    fun onDigito(d: String) {
        if (pin.length < PIN_LENGTH) pin += d
        // Avance automático al completar los 4 dígitos
        if (pin.length == PIN_LENGTH) {
            when (paso) {
                Paso.VERIFICAR -> viewModel.verificarPin(
                    trabajadorId,
                    pin,
                    onSuccess = onPinCorrecto,
                    msgs = authMsgs
                )

                Paso.CREAR -> {
                    pinCreado = pin; pin = ""; paso = Paso.CONFIRMAR
                }

                Paso.CONFIRMAR -> {
                    if (pin == pinCreado) {
                        viewModel.setPin(
                            trabajadorId,
                            pin,
                            onSuccess = onPinCorrecto,
                            msgs = authMsgs
                        )
                    } else {
                        viewModel.resetState()
                        // Volver a crear si no coinciden
                        pin = ""
                        pinCreado = ""
                        paso = Paso.CREAR
                        // Mostramos error usando el state
                        viewModel.resetState() // se lanzará desde el setter
                    }
                }
            }
        }
    }

    // Manejar el caso de que no coincidan (mostrar error)
    LaunchedEffect(paso) {
        if (paso == Paso.CREAR && pinCreado.isNotEmpty()) {
            // Significa que volvimos al paso crear porque no coincidían
            pinCreado = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetState()
                        SessionManager.desactivarPersonal()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, s.volver)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Puntos visuales
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                repeat(PIN_LENGTH) { i ->
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (i < pin.length)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(22.dp)
                    ) {}
                }
            }

            // Mensaje de error
            if (state is AuthState.Error) {
                Text(
                    text = (state as AuthState.Error).mensaje,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            // Mensaje de no coinciden (cuando volvemos al paso CREAR)
            if (paso == Paso.CREAR && pinCreado.isEmpty() && pin.isEmpty() && state !is AuthState.Error) {
                // ya reseteado, nada que mostrar
            }

            Spacer(Modifier.height(16.dp))

            // Teclado numérico
            PinNumpad(
                onDigit = ::onDigito,
                onBorrar = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                cargando = state is AuthState.Loading
            )
        }
    }
}

private enum class Paso { VERIFICAR, CREAR, CONFIRMAR }

@Composable
fun PinNumpad(onDigit: (String) -> Unit, onBorrar: () -> Unit, cargando: Boolean = false) {
    val filas = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        filas.forEach { fila ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                fila.forEach { label ->
                    when (label) {
                        "" -> Spacer(Modifier.weight(1f))
                        "⌫" -> OutlinedButton(
                            onClick = onBorrar,
                            enabled = !cargando,
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                        ) {
                            Icon(Icons.Default.Backspace, "Borrar")
                        }

                        else -> Button(
                            onClick = { onDigit(label) },
                            enabled = !cargando,
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.5f
                                )
                            )
                        ) {
                            if (cargando && label == "0") {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(label, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
