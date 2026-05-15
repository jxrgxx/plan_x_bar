package com.los_jorges.plan_bar.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.los_jorges.plan_bar.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val darkMode = repo.darkMode.stateIn(
        viewModelScope, SharingStarted.Eagerly, true
    )
    val language = repo.language.stateIn(
        viewModelScope, SharingStarted.Eagerly, "es"
    )
    val sonidoCocina = repo.sonidoCocina.stateIn(
        viewModelScope, SharingStarted.Eagerly, true
    )

    fun setDarkMode(v: Boolean) = viewModelScope.launch { repo.setDarkMode(v) }

    fun setLanguage(lang: String) = viewModelScope.launch { repo.setLanguage(lang) }

    fun setSonidoCocina(v: Boolean) = viewModelScope.launch { repo.setSonidoCocina(v) }
}
