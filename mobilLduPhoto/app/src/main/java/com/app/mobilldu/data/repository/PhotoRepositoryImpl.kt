package com.app.mobilldu.data.repository

import com.app.mobilldu.data.local.PhotoRecordDao
import com.app.mobilldu.data.local.PhotoRecordEntity
import com.app.mobilldu.data.remote.NetworkClient
import com.app.mobilldu.domain.model.PhotoRecord
import com.app.mobilldu.domain.model.UploadStatus
import com.app.mobilldu.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class PhotoRepositoryImpl(
    private val dao: PhotoRecordDao
) : PhotoRepository {

    override suspend fun saveLocalRecord(sku: String, marketplace: String, imagePath: String): Long {
        val entity = PhotoRecordEntity(
            sku = sku,
            marketplace = marketplace,
            imagePath = imagePath,
            status = "pending"
        )
        return dao.insert(entity)
    }

    override suspend fun uploadPhoto(
        sku: String,
        marketplace: String,
        file: File,
        serverUrl: String
    ): Result<Long> {
        return try {
            val service = NetworkClient.getService(serverUrl)

            val skuBody = sku.trim().toRequestBody("text/plain".toMediaTypeOrNull())
            val marketplaceBody = marketplace.toRequestBody("text/plain".toMediaTypeOrNull())
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val photoPart = MultipartBody.Part.createFormData("photo", file.name, requestFile)

            val response = service.uploadPhoto(photoPart, skuBody, marketplaceBody)

            if (response.isSuccessful) {
                Result.success(response.code().toLong())
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Server error ${response.code()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getHistory(): Flow<List<PhotoRecord>> {
        return dao.getAllRecords().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun deleteRecord(id: Long) {
        dao.deleteById(id)
    }

    private fun PhotoRecordEntity.toDomain() = PhotoRecord(
        id = id,
        sku = sku,
        marketplace = marketplace,
        imagePath = imagePath,
        createdAt = createdAt,
        status = when (status) {
            "uploaded" -> UploadStatus.UPLOADED
            "failed" -> UploadStatus.FAILED
            else -> UploadStatus.PENDING
        },
        errorMessage = errorMessage
    )
}
