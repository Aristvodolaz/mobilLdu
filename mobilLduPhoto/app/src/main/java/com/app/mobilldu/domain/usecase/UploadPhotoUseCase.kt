package com.app.mobilldu.domain.usecase

import com.app.mobilldu.data.local.PhotoRecordDao
import com.app.mobilldu.domain.repository.PhotoRepository
import java.io.File

class UploadPhotoUseCase(
    private val repository: PhotoRepository,
    private val dao: PhotoRecordDao
) {
    suspend operator fun invoke(
        sku: String,
        marketplace: String,
        file: File,
        serverUrl: String
    ): Result<Unit> {
        val recordId = repository.saveLocalRecord(sku, marketplace, file.absolutePath)

        val result = repository.uploadPhoto(sku, marketplace, file, serverUrl)

        return if (result.isSuccess) {
            dao.updateStatus(recordId, "uploaded")
            Result.success(Unit)
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
            dao.updateStatus(recordId, "failed", errorMsg)
            Result.failure(result.exceptionOrNull() ?: Exception("Upload failed"))
        }
    }
}
