package com.app.mobilldu.domain.usecase

import com.app.mobilldu.domain.model.PhotoRecord
import com.app.mobilldu.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow

class GetHistoryUseCase(
    private val repository: PhotoRepository
) {
    operator fun invoke(): Flow<List<PhotoRecord>> = repository.getHistory()
}
