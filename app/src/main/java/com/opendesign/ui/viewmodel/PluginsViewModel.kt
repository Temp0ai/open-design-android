package com.opendesign.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.opendesign.data.model.Plugin
import com.opendesign.data.repository.DesignRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PluginsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DesignRepository(application)

    private val _plugins = MutableStateFlow<List<Plugin>>(emptyList())
    val plugins: StateFlow<List<Plugin>> = _plugins.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    init {
        loadPlugins()
    }

    private fun loadPlugins() {
        val allPlugins = repository.getPlugins()
        _plugins.value = allPlugins
        _categories.value = allPlugins.map { it.category }.distinct().sorted()
    }
}
