package com.app.mobilldu.domain.repository

import com.app.mobilldu.domain.model.PhotoRecord
import kotlinx.coroutines.flow.Flow
import java.io.File

interface PhotoRepository {
    suspend fun uploadPhoto(sku: String, marketplace: String, file: File, serverUrl: String): Result<Long>
    fun getHistory(): Flow<List<PhotoRecord>>
    suspend fun deleteRecord(id: Long)
    suspend fun saveLocalRecord(sku: String, marketplace: String, imagePath: String): Long
}
