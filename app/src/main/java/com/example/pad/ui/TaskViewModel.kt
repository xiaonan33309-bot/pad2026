package com.example.pad.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pad.data.TaskEntity
import com.example.pad.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val titleInput: String = "",
    val detailInput: String = "",
    val keyword: String = ""
)

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {
    private val uiState = MutableStateFlow(UiState())

    val state: StateFlow<UiState> = uiState
    val tasks = combine(repository.observeTasks(), uiState) { list, ui ->
        if (ui.keyword.isBlank()) list
        else list.filter {
            it.title.contains(ui.keyword, ignoreCase = true) ||
                it.detail.contains(ui.keyword, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateTitle(value: String) = uiState.update { it.copy(titleInput = value) }

    fun updateDetail(value: String) = uiState.update { it.copy(detailInput = value) }

    fun updateKeyword(value: String) = uiState.update { it.copy(keyword = value) }

    fun addTask() {
        val current = uiState.value
        if (current.titleInput.isBlank()) return
        viewModelScope.launch {
            repository.addTask(current.titleInput, current.detailInput)
            uiState.update { it.copy(titleInput = "", detailInput = "") }
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch { repository.toggleTask(task) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { repository.deleteTask(task) }
    }
}

class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
