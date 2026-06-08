package com.opendesign.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opendesign.data.model.Artifact
import com.opendesign.data.repository.DesignRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DesignRepository(application)

    private val _activeFilter = MutableStateFlow("All")
    val activeFilter: StateFlow<String> = _activeFilter.asStateFlow()

    val artifacts: StateFlow<List<Artifact>> = repository.getAllArtifacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredArtifacts: StateFlow<List<Artifact>> = combine(artifacts, _activeFilter) { list, filter ->
        if (filter == "All") list
        else list.filter {
            when (filter) {
                "Prototypes" -> it.mode == "prototype"
                "Dashboards" -> it.skill == "dashboard"
                "Decks" -> it.mode == "deck"
                "Images" -> it.mode == "image"
                else -> true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: String) {
        _activeFilter.value = filter
    }

    fun deleteArtifact(artifact: Artifact) {
        viewModelScope.launch {
            repository.deleteArtifact(artifact)
        }
    }
}
