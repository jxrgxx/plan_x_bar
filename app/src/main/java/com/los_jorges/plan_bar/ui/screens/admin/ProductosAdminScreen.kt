package com.los_jorges.plan_bar.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.los_jorges.plan_bar.model.Producto
import com.los_jorges.plan_bar.viewmodel.ProductosViewModel
import com.los_jorges.plan_bar.ui.theme.Platinum40
import com.los_jorges.plan_bar.ui.theme.premiumInputColors
import com.los_jorges.plan_bar.ui.theme.LocalStrings

private val CATEGORIAS = listOf("bebida", "entrante", "primero", "segundo", "postre")
private val ProductoAccent = Color(0xFF83C9A5)

private fun categoriaColor(cat: String): Color = when (cat) {
    "bebida" -> Color(0xFF06B6D4)
    "entrante" -> Color(0xFF83C9A5)
    "primero" -> Color(0xFFF4A261)
    "segundo" -> Color(0xFFD4A853)
    "postre" -> Color(0xFF8B7AE8)
    else -> Color(0xFF83C9A5)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductosAdminScreen(
    restauranteId: Int,
    onBack: () -> Unit,
    vm: ProductosViewModel = viewModel()
) {
    val s = LocalStrings.current
    val productos by vm.productos.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var productoEditar by remember { mutableStateOf<Producto?>(null) }
    var productoEliminar by remember { mutableStateOf<Producto?>(null) }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(restauranteId) {
        while (true) {
            vm.cargar(restauranteId)
            delay(5_000)
        }
    }
    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            s.productos,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            s.gestionaLaCarta, style = MaterialTheme.typography.labelSmall,
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
                actions = {
                    IconButton(onClick = { productoEditar = null; showDialog = true }) {
                        Icon(
                            Icons.Default.Add, s.nuevoProducto,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ProductoAccent)
            }
            return@Scaffold
        }

        val agrupados = productos.groupBy { it.categoria }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (productos.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            s.sinProductosPulsaPlus,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            CATEGORIAS.forEach { cat ->
                val lista = agrupados[cat] ?: return@forEach
                item {
                    Row(
                        modifier = Modifier.padding(start = 2.dp, top = 12.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(categoriaColor(cat))
                        )
                        Text(
                            cat.uppercase(), style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
                items(lista, key = { it.id }) { producto ->
                    ProductoItem(
                        producto = producto, accentColor = categoriaColor(cat),
                        onEditar = { productoEditar = producto; showDialog = true },
                        onEliminar = { productoEliminar = producto })
                }
            }
        }
    }

    if (showDialog) {
        ProductoDialog(
            producto = productoEditar,
            onDismiss = { showDialog = false },
            onConfirm = { nombre, categoria, descripcion, precio, disponible ->
                if (productoEditar == null) {
                    vm.crear(
                        restauranteId,
                        nombre,
                        categoria,
                        descripcion,
                        precio,
                        disponible
                    ) { ok, err ->
                        snackMsg = if (ok) s.productoCreado else err ?: "Error"
                    }
                } else {
                    vm.editar(
                        restauranteId,
                        productoEditar!!.id,
                        nombre,
                        categoria,
                        descripcion,
                        precio,
                        disponible
                    ) { ok, err ->
                        snackMsg = if (ok) s.productoActualizado else err ?: "Error"
                    }
                }
                showDialog = false
            }
        )
    }

    productoEliminar?.let { p ->
        AlertDialog(
            onDismissRequest = { productoEliminar = null },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(s.eliminarProducto) },
            text = { Text("${s.confirmarEliminarProducto} \"${p.nombre}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.eliminar(restauranteId, p.id) { ok, err ->
                            snackMsg = if (ok) s.productoEliminado else err ?: "Error"
                        }
                        productoEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(s.eliminar) }
            },
            dismissButton = {
                TextButton(onClick = {
                    productoEliminar = null
                }) { Text(s.cancelar) }
            }
        )
    }

    error?.let { snackMsg = it; vm.clearError() }
}

// ── Item de producto ──────────────────────────────────────────────────────────

@Composable
private fun ProductoItem(
    producto: Producto,
    accentColor: Color,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val s = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Restaurant,
                    null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    producto.nombre, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "%.2f €".format(producto.precio),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = accentColor
                    )
                    if (!producto.disponible) {
                        Text(
                            "·",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            s.noDisponible,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            IconButton(onClick = onEditar, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Edit,
                    s.editar,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    s.eliminar,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── Diálogo crear / editar producto ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductoDialog(
    producto: Producto?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double, Boolean) -> Unit
) {
    val s = LocalStrings.current
    var nombre by remember { mutableStateOf(producto?.nombre ?: "") }
    var categoria by remember { mutableStateOf(producto?.categoria ?: CATEGORIAS[0]) }
    var descripcion by remember { mutableStateOf(producto?.descripcion ?: "") }
    var precio by remember { mutableStateOf(producto?.precio?.toString() ?: "") }
    var disponible by remember { mutableStateOf(producto?.disponible ?: true) }
    var expandedCat by remember { mutableStateOf(false) }

    val accentColor = categoriaColor(categoria)

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
                            Icons.Default.Restaurant,
                            null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            if (producto == null) s.nuevoProducto else s.editarProducto,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (producto == null) s.anadirPlatoOBebida else "${s.modificando} ${producto.nombre}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ── Campos ────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = nombre, onValueChange = { nombre = it },
                        label = { Text(s.nombreObligatorio) }, singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Badge,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
                    )

                    // Selector de categoría con indicador de color
                    ExposedDropdownMenuBox(
                        expanded = expandedCat,
                        onExpandedChange = { expandedCat = it }) {
                        OutlinedTextField(
                            value = categoria.replaceFirstChar { it.uppercase() },
                            onValueChange = {}, readOnly = true,
                            label = { Text(s.categoria) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(accentColor)
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCat) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCat,
                            onDismissRequest = { expandedCat = false }) {
                            CATEGORIAS.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(categoriaColor(cat))
                                            )
                                            Text(cat.replaceFirstChar { it.uppercase() })
                                        }
                                    },
                                    onClick = { categoria = cat; expandedCat = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = descripcion, onValueChange = { descripcion = it },
                        label = { Text(s.descripcion) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Notes,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
                    )
                    OutlinedTextField(
                        value = precio, onValueChange = { precio = it },
                        label = { Text(s.precioEur) }, singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Euro,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), colors = premiumInputColors()
                    )

                    // Toggle disponible
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (disponible) accentColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (disponible) accentColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                s.disponibleEnCarta,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (disponible) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = disponible, onCheckedChange = { disponible = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = accentColor,
                                    checkedTrackColor = accentColor.copy(alpha = 0.3f)
                                )
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
                            val p = precio.replace(",", ".").toDoubleOrNull() ?: 0.0
                            if (nombre.isNotBlank() && p > 0) onConfirm(
                                nombre,
                                categoria,
                                descripcion,
                                p,
                                disponible
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s.guardar, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
