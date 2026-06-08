package com.opendesign.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.opendesign.data.model.ApiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        private val KEY_PROVIDER = stringPreferencesKey("provider")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_MODEL = stringPreferencesKey("model")
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
    }

    val apiConfig: Flow<ApiConfig> = context.dataStore.data.map { prefs ->
        ApiConfig(
            provider = prefs[KEY_PROVIDER] ?: "anthropic",
            apiKey = prefs[KEY_API_KEY] ?: "",
            model = prefs[KEY_MODEL] ?: "claude-3-5-sonnet-20241022",
            baseUrl = prefs[KEY_BASE_URL] ?: ""
        )
    }

    suspend fun saveApiConfig(config: ApiConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROVIDER] = config.provider
            prefs[KEY_API_KEY] = config.apiKey
            prefs[KEY_MODEL] = config.model
            prefs[KEY_BASE_URL] = config.baseUrl
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
