package com.los_jorges.plan_bar.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.los_jorges.plan_bar.model.Zona
import com.los_jorges.plan_bar.session.SessionManager
import com.los_jorges.plan_bar.ui.theme.LocalStrings
import com.los_jorges.plan_bar.viewmodel.ZonasViewModel

private val EspacioAccent = Color(0xFF60A5FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EspaciosAdminScreen(
    restauranteId: Int,
    onBack: () -> Unit,
    vm: ZonasViewModel = viewModel()
) {
    val s = LocalStrings.current
    val zonasDB by vm.zonas.collectAsState()
    val loading by vm.loading.collectAsState()
    val guardado by vm.guardado.collectAsState()
    val error by vm.error.collectAsState()

    // Dialog de error (se muestra cuando el servidor bloquea el guardado)
    var errorDialog by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(error) {
        if (error != null) errorDialog = error
    }

    // Cargar y polling
    LaunchedEffect(restauranteId) {
        while (true) {
            vm.cargar(restauranteId)
            delay(5_000)
        }
    }

    val CLAVES = SessionManager.ZONA_KEYS
    val DEFAULTS = listOf("Piso 1", "Piso 2", "Terraza", "Zona 4")

    // ── Estado editable ────────────────────────────────────────────────────────
    // Se inicializa UNA SOLA VEZ con los mejores datos disponibles en ese momento
    // y se actualiza SOLO la primera vez que llegan datos reales de la BD.
    // Así el usuario puede escribir libremente (incluyendo ñ) sin que Compose
    // resetee la lista cuando llega la respuesta de red.
    val nombres = remember { mutableStateListOf(*DEFAULTS.toTypedArray()) }
    var numActivas by remember { mutableStateOf(3) }
    // Flag para no machacar ediciones del usuario si la BD tarda
    var datosAplicados by remember { mutableStateOf(false) }

    LaunchedEffect(zonasDB) {
        if (datosAplicados) return@LaunchedEffect   // ya cargamos, no pisar ediciones

        val fuente: List<Zona> = if (zonasDB.isNotEmpty()) {
            zonasDB.sortedBy { it.orden }
        } else {
            // Fallback desde SessionManager (SharedPrefs / cache anterior)
            SessionManager.zonasActivas
        }

        fuente.forEachIndexed { i, z ->
            if (i < nombres.size) nombres[i] = z.nombre
        }
        numActivas = fuente.count { it.activo }.coerceIn(1, 4)

        if (zonasDB.isNotEmpty()) datosAplicados = true  // solo bloqueamos tras carga real
    }

    // Reset del badge "guardado" cuando el usuario edita
    var editado by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            s.espaciosDeTrabajo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            s.zonasYDistribucion,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Indicador de carga ────────────────────────────────────────────
            if (loading && zonasDB.isEmpty()) {
                Box(Modifier
                    .fillMaxWidth()
                    .padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EspacioAccent)
                }
                return@Column
            }

            // ── Error ─────────────────────────────────────────────────────────
            if (error != null && zonasDB.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline, null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ── Número de espacios ────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Cabecera con icono
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EspacioAccent.copy(alpha = 0.12f))
                                .border(
                                    1.dp,
                                    EspacioAccent.copy(alpha = 0.25f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.GridView, null,
                                tint = EspacioAccent, modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                s.numeroDeEspacios,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                s.hasta4Zonas,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Selector 1–4
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        (1..4).forEach { n ->
                            val selected = numActivas == n
                            Surface(
                                onClick = {
                                    numActivas = n
                                    editado = true
                                    vm.resetGuardado()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) EspacioAccent.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) EspacioAccent
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        "$n",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) EspacioAccent
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Nombres de cada espacio ───────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cabecera con icono
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EspacioAccent.copy(alpha = 0.12f))
                                .border(
                                    1.dp,
                                    EspacioAccent.copy(alpha = 0.25f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DriveFileRenameOutline, null,
                                tint = EspacioAccent, modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                s.nombresDeLosEspacios,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                s.personalizaCadaZona,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    (0 until 4).forEach { i ->
                        val isActive = i < numActivas
                        OutlinedTextField(
                            value = nombres.getOrElse(i) { DEFAULTS[i] },
                            onValueChange = { v ->
                                while (nombres.size <= i) nombres.add("")
                                nombres[i] = v
                                editado = true
                                vm.resetGuardado()
                            },
                            label = {
                                Text(
                                    if (isActive) "${s.espacio} ${i + 1}"
                                    else "${s.espacio} ${i + 1} (${s.inactivo})"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.TableBar, null,
                                    tint = if (isActive) EspacioAccent
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            enabled = isActive,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Botón guardar ─────────────────────────────────────────────────
            val yaGuardado = guardado && !editado
            Button(
                onClick = {
                    // Construir la lista desde los datos de BD (con id real) + ediciones locales
                    val baseZonas = if (zonasDB.isNotEmpty()) zonasDB.sortedBy { it.orden }
                    else SessionManager.zonasActivas
                    val zonasActualizadas = CLAVES.mapIndexed { i, clave ->
                        val base = baseZonas.find { it.clave == clave }
                            ?: Zona(
                                id = 0,
                                clave = clave,
                                nombre = DEFAULTS[i],
                                orden = i + 1,
                                activo = i < numActivas
                            )
                        base.copy(
                            nombre = nombres.getOrElse(i) { DEFAULTS[i] }.ifBlank { DEFAULTS[i] },
                            activo = i < numActivas
                        )
                    }
                    editado = false
                    vm.guardar(restauranteId, zonasActualizadas)
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (yaGuardado) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.primary,
                    contentColor = if (yaGuardado) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        if (yaGuardado) Icons.Default.Check else Icons.Default.Save,
                        null, modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (yaGuardado) s.configuracionGuardada else s.guardarConfiguracion,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            if (yaGuardado) {
                Text(
                    s.cambiosSincronizan,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

        }
    }

    // ── Dialog de error prominente ────────────────────────────────────────────
    errorDialog?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorDialog = null; vm.resetError() },
            icon = {
                Icon(
                    Icons.Default.Warning, null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text(s.noSePuedeGuardar, fontWeight = FontWeight.SemiBold) },
            text = { Text(msg, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = { errorDialog = null; vm.resetGuardado() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) { Text(s.entendido) }
            },
            dismissButton = null
        )
    }
}
