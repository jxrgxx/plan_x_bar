package com.los_jorges.plan_bar.ui.screens.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.los_jorges.plan_bar.ui.theme.LocalStrings
import com.los_jorges.plan_bar.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel()
) {
    val s = LocalStrings.current
    val darkMode by vm.darkMode.collectAsState()
    val language by vm.language.collectAsState()
    val sonidoCocina by vm.sonidoCocina.collectAsState()

    val bg = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    val onSurf = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val divider = MaterialTheme.colorScheme.outlineVariant

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.ajustes, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.volver)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surface,
                    titleContentColor = onSurf,
                    navigationIconContentColor = muted
                )
            )
        },
        containerColor = bg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Apariencia ────────────────────────────────────────────────
            SectionLabel(s.apariencia.uppercase(), muted)

            SettingsToggleRow(
                icon = if (darkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                title = if (darkMode) s.modoOscuro else s.modoClaroLabel,
                subtitle = s.cambiaTema,
                checked = darkMode,
                onChecked = { vm.setDarkMode(it) },
                surface = surface,
                primary = primary,
                onSurf = onSurf,
                muted = muted
            )

            Spacer(Modifier.height(12.dp))

            // ── Idioma ────────────────────────────────────────────────────
            SectionLabel(s.idioma.uppercase(), muted)

            LanguageSelectorCard(
                selected = language,
                onSelect = { vm.setLanguage(it) },
                surface = surface,
                primary = primary,
                onSurf = onSurf,
                muted = muted,
                divider = divider
            )

            Spacer(Modifier.height(12.dp))

            // ── Cocina ────────────────────────────────────────────────────
            SectionLabel(s.cocina.uppercase(), muted)

            SettingsToggleRow(
                icon = Icons.Default.NotificationsActive,
                title = s.sonidoPedido,
                subtitle = s.suenaPedido,
                checked = sonidoCocina,
                onChecked = { vm.setSonidoCocina(it) },
                surface = surface,
                primary = primary,
                onSurf = onSurf,
                muted = muted
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Componentes internos ──────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    surface: Color,
    primary: Color,
    onSurf: Color,
    muted: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = primary, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = onSurf, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text(subtitle, color = muted, fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onChecked,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.background,
                    checkedTrackColor = primary,
                    uncheckedThumbColor = muted,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
private fun LanguageSelectorCard(
    selected: String,
    onSelect: (String) -> Unit,
    surface: Color,
    primary: Color,
    onSurf: Color,
    muted: Color,
    divider: Color
) {
    val langs = listOf(
        Triple("es", "Español", "🇪🇸"),
        Triple("en", "English", "🇬🇧"),
        Triple("fr", "Français", "🇫🇷")
    )
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            langs.forEachIndexed { idx, (code, label, flag) ->
                val sel = selected == code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(code) }
                        .background(if (sel) primary.copy(alpha = 0.10f) else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(flag, fontSize = 22.sp)
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        color = if (sel) onSurf else muted,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 15.sp
                    )
                    if (sel) Icon(
                        Icons.Default.Check,
                        null,
                        tint = primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (idx < langs.lastIndex) HorizontalDivider(color = divider, thickness = 0.5.dp)
            }
        }
    }
}
