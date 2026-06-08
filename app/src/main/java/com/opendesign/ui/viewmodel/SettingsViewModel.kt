package com.opendesign.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opendesign.data.model.ApiConfig
import com.opendesign.data.preferences.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application)

    val apiConfig: StateFlow<ApiConfig> = settingsManager.apiConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ApiConfig())

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    fun saveProvider(provider: String) {
        viewModelScope.launch {
            val current = apiConfig.value
            settingsManager.saveApiConfig(current.copy(provider = provider))
        }
    }

    fun saveApiKey(apiKey: String) {
        viewModelScope.launch {
            val current = apiConfig.value
            settingsManager.saveApiConfig(current.copy(apiKey = apiKey))
        }
    }

    fun saveModel(model: String) {
        viewModelScope.launch {
            val current = apiConfig.value
            settingsManager.saveApiConfig(current.copy(model = model))
        }
    }

    fun saveBaseUrl(baseUrl: String) {
        viewModelScope.launch {
            val current = apiConfig.value
            settingsManager.saveApiConfig(current.copy(baseUrl = baseUrl))
        }
    }

    suspend fun findOllama(): String? {
        return withContext(Dispatchers.IO) {
            settingsManager.findOllama()
        }
    }

    fun resetDefaults() {
        viewModelScope.launch {
            settingsManager.saveApiConfig(ApiConfig())
            _connectionStatus.value = ConnectionStatus.Idle
        }
    }
}

sealed class ConnectionStatus {
    object Idle : ConnectionStatus()
    object Testing : ConnectionStatus()
    data class Success(val message: String) : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}
