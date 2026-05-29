package com.app.mobilldu.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.mobilldu.domain.usecase.GetHistoryUseCase
import com.app.mobilldu.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val getHistoryUseCase: GetHistoryUseCase,
    private val repository: PhotoRepository,
    private val serverUrl: String
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState(serverUrl = serverUrl))
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    init {
        loadServerHistory()
    }

    fun processIntent(intent: HistoryIntent) {
        when (intent) {
            is HistoryIntent.LoadServerHistory -> loadServerHistory()

            is HistoryIntent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = intent.query) }
            }

            is HistoryIntent.SelectServerRecord -> {
                val record = if (intent.id == null) null
                else _state.value.serverRecords.find { it.id == intent.id }
                _state.update { it.copy(selectedServerRecord = record) }
            }

            is HistoryIntent.DeleteServerRecord -> {
                viewModelScope.launch {
                    val result = repository.deleteServerRecord(intent.id, serverUrl)
                    if (result.isSuccess) {
                        // Убираем из локального списка без перезагрузки
                        _state.update { s ->
                            s.copy(serverRecords = s.serverRecords.filter { it.id != intent.id })
                        }
                    }
                }
            }

            // ниже — заглушки для компилятора, не используются в UI
            is HistoryIntent.LoadHistory -> {}
            is HistoryIntent.DeleteRecord -> {
                viewModelScope.launch { repository.deleteRecord(intent.id) }
            }
            is HistoryIntent.SelectRecord -> {}
        }
    }

    private fun loadServerHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isServerLoading = true, serverError = null) }
            val result = repository.getServerHistory(serverUrl)
            if (result.isSuccess) {
                _state.update {
                    it.copy(
                        serverRecords = result.getOrDefault(emptyList()),
                        isServerLoading = false
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isServerLoading = false,
                        serverError = result.exceptionOrNull()?.message ?: "Ошибка загрузки с сервера"
                    )
                }
            }
        }
    }

    class Factory(
        private val getHistoryUseCase: GetHistoryUseCase,
        private val repository: PhotoRepository,
        private val serverUrl: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(getHistoryUseCase, repository, serverUrl) as T
        }
    }
}
