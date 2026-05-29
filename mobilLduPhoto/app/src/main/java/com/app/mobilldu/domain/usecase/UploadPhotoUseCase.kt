package com.app.mobilldu.domain.usecase

import com.app.mobilldu.data.local.PhotoRecordDao
import com.app.mobilldu.domain.repository.PhotoRepository
import java.io.File

class UploadPhotoUseCase(
    private val repository: PhotoRepository,
    private val dao: PhotoRecordDao
) {
    /**
     * Сохраняет все локальные записи и отправляет все файлы одним запросом.
     * Возвращает Result.success если все файлы загружены, иначе failure.
     */
    suspend operator fun invoke(
        sku: String,
        marketplace: String,
        files: List<File>,
        serverUrl: String
    ): Result<Unit> {
        // 1. Сохраняем каждое фото в локальную БД как pending
        val recordIds = files.map { file ->
            repository.saveLocalRecord(sku, marketplace, file.absolutePath)
        }

        // 2. Отправляем все файлы одним запросом
        val result = repository.uploadPhotos(sku, marketplace, files, serverUrl)

        // 3. Обновляем статусы в локальной БД
        if (result.isSuccess) {
            recordIds.forEach { id -> dao.updateStatus(id, "uploaded") }
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: "Upload failed"
            recordIds.forEach { id -> dao.updateStatus(id, "failed", errorMsg) }
        }

        return result
    }
}
