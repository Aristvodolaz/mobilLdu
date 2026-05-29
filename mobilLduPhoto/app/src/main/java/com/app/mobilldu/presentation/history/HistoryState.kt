package com.app.mobilldu.presentation.history

import com.app.mobilldu.domain.model.PhotoRecord
import com.app.mobilldu.domain.model.ServerRecord

data class HistoryState(
    val serverRecords: List<ServerRecord> = emptyList(),
    val isServerLoading: Boolean = true,
    val serverError: String? = null,
    val selectedServerRecord: ServerRecord? = null,
    val serverUrl: String = "http://10.171.12.36:3030",
    val searchQuery: String = "",

    // оставим для совместимости с UseCase
    val records: List<PhotoRecord> = emptyList(),
    val isLoading: Boolean = false,
    val selectedRecord: PhotoRecord? = null
)
