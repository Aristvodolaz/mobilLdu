package com.app.mobilldu.domain.repository

import com.app.mobilldu.domain.model.PhotoRecord
import com.app.mobilldu.domain.model.ServerRecord
import kotlinx.coroutines.flow.Flow
import java.io.File

interface PhotoRepository {
    suspend fun uploadPhotos(
        sku: String,
        marketplace: String,
        files: List<File>,
        serverUrl: String
    ): Result<Unit>

    fun getHistory(): Flow<List<PhotoRecord>>
    suspend fun deleteRecord(id: Long)
    suspend fun saveLocalRecord(sku: String, marketplace: String, imagePath: String): Long

    /** Получить историю загрузок прямо с сервера */
    suspend fun getServerHistory(serverUrl: String): Result<List<ServerRecord>>

    /** Удалить запись с сервера по ID */
    suspend fun deleteServerRecord(id: Long, serverUrl: String): Result<Unit>

    /** Обновить маркетплейс для артикула на сервере */
    suspend fun updateMarketplace(article: String, marketplace: String, serverUrl: String): Result<Unit>
}

