package com.los_jorges.plan_bar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ajustes")

object SettingsKeys {
    val DARK_MODE      = booleanPreferencesKey("dark_mode")
    val LANGUAGE       = stringPreferencesKey("language")
    val SONIDO_COCINA  = booleanPreferencesKey("sonido_cocina")
}

class SettingsRepository(private val context: Context) {

    val darkMode: Flow<Boolean> = context.settingsDataStore.data.map {
        it[SettingsKeys.DARK_MODE] ?: true
    }

    val language: Flow<String> = context.settingsDataStore.data.map {
        it[SettingsKeys.LANGUAGE] ?: "es"
    }

    val sonidoCocina: Flow<Boolean> = context.settingsDataStore.data.map {
        it[SettingsKeys.SONIDO_COCINA] ?: true
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.DARK_MODE] = enabled }
    }

    suspend fun setLanguage(lang: String) {
        context.settingsDataStore.edit { it[SettingsKeys.LANGUAGE] = lang }
    }

    suspend fun setSonidoCocina(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.SONIDO_COCINA] = enabled }
    }
}
