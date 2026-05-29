package com.app.mobilldu.presentation.history

sealed class HistoryIntent {
    object LoadServerHistory : HistoryIntent()
    data class SelectServerRecord(val id: Long?) : HistoryIntent()
    data class SearchQueryChanged(val query: String) : HistoryIntent()
    data class DeleteServerRecord(val id: Long) : HistoryIntent()
    // локальная история больше не нужна в UI
    object LoadHistory : HistoryIntent()
    data class DeleteRecord(val id: Long) : HistoryIntent()
    data class SelectRecord(val id: Long?) : HistoryIntent()
}
