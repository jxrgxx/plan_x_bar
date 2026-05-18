package com.los_jorges.plan_bar.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.los_jorges.plan_bar.ui.theme.premiumInputColors
import com.los_jorges.plan_bar.ui.theme.LocalStrings
import com.los_jorges.plan_bar.model.MenuDiaLineaRequest
import com.los_jorges.plan_bar.model.Producto
import com.los_jorges.plan_bar.viewmodel.MenuDiaViewModel
import com.los_jorges.plan_bar.viewmodel.ProductosViewModel

// Cursos con etiqueta, categoría de producto, color e icono
private data class CursoConfig(
    val key: String,
    val label: String,
    val categoria: String,
    val color: Color,
    val icon: ImageVector
)

@Composable
private fun cursosConfig(): List<CursoConfig> {
    val s = LocalStrings.current
    return listOf(
        CursoConfig("bebida", s.cursoBebida, "bebida", Color(0xFF06B6D4), Icons.Default.LocalBar),
        CursoConfig(
            "primero",
            s.cursoPrimero,
            "primero",
            Color(0xFFF4A261),
            Icons.Default.DinnerDining
        ),
        CursoConfig(
            "segundo",
            s.cursoSegundo,
            "segundo",
            Color(0xFFD4A853),
            Icons.Default.Restaurant
        ),
        CursoConfig("postre", s.cursoPostre, "postre", Color(0xFF8B7AE8), Icons.Default.Cake)
    )
}

private data class LineaLocal(
    val producto_id: Int,
    val nombre: String,
    val precio: Double,
    val curso: String,
    var cantidad: Int = -1  // -1 = sin límite, 0 = agotado, >0 = stock
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuDiaAdminScreen(
    restauranteId: Int,
    onBack: () -> Unit
) {
    val menuVm: MenuDiaViewModel = viewModel()
    val productosVm: ProductosViewModel = viewModel()

    val menu by menuVm.menu.collectAsState()
    val loading by menuVm.loading.collectAsState()
    val productos by productosVm.productos.collectAsState()

    val s = LocalStrings.current
    val cursosConfig = cursosConfig()
    val snackbarHostState = remember { SnackbarHostState() }
    var snackMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(restauranteId) {
        while (true) {
            menuVm.cargar(restauranteId)
            productosVm.cargar(restauranteId)
            delay(5_000)
        }
    }
    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    var precioTexto by remember { mutableStateOf("") }
    val lineas = remember { mutableStateListOf<LineaLocal>() }
    var inicializado by remember { mutableStateOf(false) }

    LaunchedEffect(menu, productos) {
        if (!inicializado && menu != null && productos.isNotEmpty()) {
            if (precioTexto.isEmpty()) precioTexto = menu!!.precio.toString()
            lineas.clear()
            menu!!.lineas.forEach { l ->
                val prod = productos.firstOrNull { it.id == l.producto_id }
                lineas.add(
                    LineaLocal(
                        producto_id = l.producto_id,
                        nombre = l.nombre,
                        precio = prod?.precio ?: l.precio,
                        curso = l.curso,
                        cantidad = l.cantidad
                    )
                )
            }
            inicializado = true
        }
    }

    var cursoDialogo by remember { mutableStateOf<CursoConfig?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            s.menuDelDia,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            s.configurarOfertaHoy,
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val precio = precioTexto.replace(',', '.').toDoubleOrNull()
                    if (precio == null || precio <= 0) {
                        snackMsg = s.introducePrecioValido
                        return@ExtendedFloatingActionButton
                    }
                    if (loading) return@ExtendedFloatingActionButton
                    menuVm.guardar(
                        restauranteId = restauranteId,
                        precio = precio,
                        lineas = lineas.map {
                            MenuDiaLineaRequest(
                                it.producto_id,
                                it.curso,
                                it.cantidad
                            )
                        }
                    ) { ok, err ->
                        snackMsg = if (ok) s.menuGuardado else err ?: s.errorAlGuardar
                        if (ok) {
                            inicializado = false; menuVm.cargar(restauranteId)
                        }
                    }
                },
                icon = { Icon(Icons.Default.Check, null) },
                text = { Text(s.guardarMenu, fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->

        if (loading && menu == null && productos.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFF4A261))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Precio del menú ───────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Euro, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    OutlinedTextField(
                        value = precioTexto,
                        onValueChange = { precioTexto = it.replace(',', '.') },
                        label = { Text(s.precioDelMenu) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Secciones por curso ───────────────────────────────────────────
            cursosConfig.forEach { config ->
                val lineasCurso = lineas.filter { it.curso == config.key }
                val prodDisponibles =
                    productos.filter { it.categoria == config.categoria && it.disponible }
                val prodNoAñadidos =
                    prodDisponibles.filter { p -> lineasCurso.none { l -> l.producto_id == p.id } }

                CursoSection(
                    config = config,
                    lineas = lineasCurso,
                    hayProductos = prodDisponibles.isNotEmpty(),
                    onCantidadCambia = { linea, nueva ->
                        val idx = lineas.indexOf(linea)
                        if (idx >= 0) lineas[idx] = linea.copy(cantidad = nueva)
                    },
                    onEliminar = { lineas.remove(it) },
                    onAñadir = {
                        if (prodNoAñadidos.isNotEmpty()) cursoDialogo = config
                        else snackMsg = s.noHayMasProductosCat
                    }
                )
            }

            Spacer(Modifier.height(72.dp))
        }
    }

    cursoDialogo?.let { config ->
        val prodDisponibles = productos.filter { it.categoria == config.categoria && it.disponible }
        val prodNoAñadidos =
            prodDisponibles.filter { p -> lineas.none { it.curso == config.key && it.producto_id == p.id } }

        AñadirProductoDialog(
            cursoLabel = config.label,
            opciones = prodNoAñadidos,
            accentColor = config.color,
            onDismiss = { cursoDialogo = null },
            onConfirm = { producto, cantidad ->
                lineas.add(
                    LineaLocal(
                        producto_id = producto.id,
                        nombre = producto.nombre,
                        precio = producto.precio,
                        curso = config.key,
                        cantidad = cantidad
                    )
                )
                cursoDialogo = null
            }
        )
    }
}

// ── Sección de un curso ───────────────────────────────────────────────────────

@Composable
private fun CursoSection(
    config: CursoConfig,
    lineas: List<LineaLocal>,
    hayProductos: Boolean,
    onCantidadCambia: (LineaLocal, Int) -> Unit,
    onEliminar: (LineaLocal) -> Unit,
    onAñadir: () -> Unit
) {
    val s = LocalStrings.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabecera del curso con icono de color
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(config.color.copy(alpha = 0.12f))
                        .border(1.dp, config.color.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(config.icon, null, tint = config.color, modifier = Modifier.size(18.dp))
                }
                Text(
                    config.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (hayProductos) {
                    FilledTonalIconButton(
                        onClick = onAñadir,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            "${s.anadir} ${config.label}",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (lineas.isEmpty()) {
                Text(
                    if (hayProductos) s.pulsaPlusParaAnadir
                    else s.sinProductosEnCategoria,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 50.dp, bottom = 4.dp)
                )
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                lineas.forEach { linea ->
                    LineaItem(
                        linea = linea,
                        accentColor = config.color,
                        onCantidadCambia = { nueva -> onCantidadCambia(linea, nueva) },
                        onEliminar = { onEliminar(linea) }
                    )
                }
            }
        }
    }
}

// ── Fila de una línea ─────────────────────────────────────────────────────────

@Composable
private fun LineaItem(
    linea: LineaLocal,
    accentColor: Color,
    onCantidadCambia: (Int) -> Unit,
    onEliminar: () -> Unit
) {
    val s = LocalStrings.current
    var cantidadTexto by remember(linea.producto_id, linea.curso) {
        mutableStateOf(linea.cantidad.toString())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                linea.nombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "%.2f €".format(linea.precio),
                style = MaterialTheme.typography.bodySmall,
                color = accentColor
            )
        }

        OutlinedTextField(
            value = cantidadTexto,
            onValueChange = { v ->
                // Permite "-1" para sin límite, o cualquier número positivo
                val filtrado = v.filter { it.isDigit() || it == '-' }.take(4)
                cantidadTexto = filtrado
                onCantidadCambia(filtrado.toIntOrNull() ?: -1)
            },
            label = { Text(s.udsLabel, style = MaterialTheme.typography.labelSmall) },
            placeholder = { Text("∞") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.width(82.dp),
            shape = RoundedCornerShape(10.dp)
        )

        IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Delete, s.quitar,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Diálogo añadir producto ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AñadirProductoDialog(
    cursoLabel: String,
    opciones: List<Producto>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Producto, Int) -> Unit
) {
    val s = LocalStrings.current
    var productoSeleccionado by remember { mutableStateOf<Producto?>(opciones.firstOrNull()) }
    var cantidadTexto by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // ── Cabecera ──────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accentColor.copy(alpha = 0.07f))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(
                                1.dp,
                                accentColor.copy(alpha = 0.30f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AddCircleOutline,
                            null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            "${s.anadir} $cursoLabel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            s.seleccionaProductoYUnidades,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ── Campos ────────────────────────────────────────────────
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = productoSeleccionado?.nombre ?: "",
                            onValueChange = {}, readOnly = true,
                            label = { Text(s.productoLabel) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Restaurant,
                                    null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }) {
                            opciones.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(p.nombre)
                                            Text(
                                                "%.2f €".format(p.precio),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = accentColor
                                            )
                                        }
                                    },
                                    onClick = { productoSeleccionado = p; expanded = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = cantidadTexto,
                        onValueChange = {
                            cantidadTexto = it.filter { c -> c.isDigit() || c == '-' }.take(4)
                        },
                        label = { Text(s.cantidadDisponible) },
                        placeholder = { Text(s.dejarVacioSinLimite) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Numbers,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.06f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            accentColor.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                null,
                                tint = accentColor.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(top = 1.dp)
                            )
                            Text(
                                s.stockHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ── Acciones ──────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) { Text(s.cancelar) }
                    Button(
                        onClick = {
                            productoSeleccionado?.let {
                                onConfirm(
                                    it,
                                    cantidadTexto.toIntOrNull() ?: -1
                                )
                            }
                        },
                        enabled = productoSeleccionado != null,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s.anadirLabel, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
