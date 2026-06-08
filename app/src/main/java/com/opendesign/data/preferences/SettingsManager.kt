package com.opendesign.data.preferences

import android.content.Context
import android.net.wifi.WifiManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.opendesign.data.model.ApiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.InetSocketAddress
import java.net.Socket

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
            provider = prefs[KEY_PROVIDER] ?: "local",
            apiKey = prefs[KEY_API_KEY] ?: "",
            model = prefs[KEY_MODEL] ?: "gemma-2b-it-q4",
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

    fun getDeviceIp(): String {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return "192.168.1.1"
            val linkProperties = connectivityManager.getLinkProperties(network) ?: return "192.168.1.1"
            val ip = linkProperties.linkAddresses.firstOrNull { 
                it.address is java.net.Inet4Address 
            }?.address?.hostAddress
            return ip ?: "192.168.1.1"
        } catch (_: Exception) {
            return "192.168.1.1"
        }
    }

    suspend fun findOllama(): String? {
        val ip = getDeviceIp()
        val base = ip.substringBeforeLast(".")
        val ports = listOf(11434)

        for (suffix in 1..254) {
            for (port in ports) {
                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress("$base.$suffix", port), 200)
                    socket.close()
                    return "http://$base.$suffix:$port/v1"
                } catch (_: Exception) {}
            }
        }
        return null
    }
}
