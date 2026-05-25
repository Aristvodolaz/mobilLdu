package com.app.mobilldu.presentation.history

sealed class HistoryIntent {
    object LoadHistory : HistoryIntent()
    data class DeleteRecord(val id: Long) : HistoryIntent()
    data class SelectRecord(val id: Long?) : HistoryIntent()
}
