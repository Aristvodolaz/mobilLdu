package com.app.mobilldu.presentation.history

import com.app.mobilldu.domain.model.PhotoRecord

data class HistoryState(
    val records: List<PhotoRecord> = emptyList(),
    val isLoading: Boolean = true,
    val selectedRecord: PhotoRecord? = null   // для полноэкранного просмотра
)
