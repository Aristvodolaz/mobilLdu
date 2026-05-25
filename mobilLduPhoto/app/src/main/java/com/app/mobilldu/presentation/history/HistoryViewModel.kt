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
    private val repository: PhotoRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    init {
        processIntent(HistoryIntent.LoadHistory)
    }

    fun processIntent(intent: HistoryIntent) {
        when (intent) {
            is HistoryIntent.LoadHistory -> {
                viewModelScope.launch {
                    _state.update { it.copy(isLoading = true) }
                    getHistoryUseCase().collect { records ->
                        _state.update { it.copy(records = records, isLoading = false) }
                    }
                }
            }

            is HistoryIntent.DeleteRecord -> {
                viewModelScope.launch {
                    repository.deleteRecord(intent.id)
                }
            }

            is HistoryIntent.SelectRecord -> {
                val record = if (intent.id == null) null
                else _state.value.records.find { it.id == intent.id }
                _state.update { it.copy(selectedRecord = record) }
            }
        }
    }

    class Factory(
        private val getHistoryUseCase: GetHistoryUseCase,
        private val repository: PhotoRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(getHistoryUseCase, repository) as T
        }
    }
}
