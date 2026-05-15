package com.los_jorges.plan_bar

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.los_jorges.plan_bar.data.SettingsRepository
import com.los_jorges.plan_bar.ui.navigation.NavGraph
import com.los_jorges.plan_bar.ui.theme.Plan_BarTheme
import com.los_jorges.plan_bar.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Aplicar locale antes de que se construya la UI
        val lang = runBlocking { SettingsRepository(this@MainActivity).language.first() }
        applyLocale(lang)

        enableEdgeToEdge()
        setContent {
            val settingsVm: SettingsViewModel = viewModel()
            val darkMode  by settingsVm.darkMode.collectAsState()
            val language  by settingsVm.language.collectAsState()

            Plan_BarTheme(darkTheme = darkMode, language = language) {
                Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun applyLocale(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
