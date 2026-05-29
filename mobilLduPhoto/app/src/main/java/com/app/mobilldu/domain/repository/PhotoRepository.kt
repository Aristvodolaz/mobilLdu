package com.app.mobilldu.domain.repository

import com.app.mobilldu.domain.model.PhotoRecord
import kotlinx.coroutines.flow.Flow
import java.io.File

interface PhotoRepository {
    /** Отправить список файлов одним запросом */
    suspend fun uploadPhotos(
        sku: String,
        marketplace: String,
        files: List<File>,
        serverUrl: String
    ): Result<Unit>

    fun getHistory(): Flow<List<PhotoRecord>>
    suspend fun deleteRecord(id: Long)
    suspend fun saveLocalRecord(sku: String, marketplace: String, imagePath: String): Long
}
