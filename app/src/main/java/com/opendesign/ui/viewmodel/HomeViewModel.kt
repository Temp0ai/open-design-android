package com.opendesign.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opendesign.data.model.Artifact
import com.opendesign.data.model.Project
import com.opendesign.data.repository.DesignRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DesignRepository(application)

    val projects: StateFlow<List<Project>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentArtifacts: StateFlow<List<Artifact>> = repository.getAllArtifacts()
        .map { artifacts -> artifacts.sortedByDescending { it.createdAt }.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createProject(name: String, designSystemId: String = "") {
        viewModelScope.launch {
            repository.createProject(name, designSystemId)
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }
}
