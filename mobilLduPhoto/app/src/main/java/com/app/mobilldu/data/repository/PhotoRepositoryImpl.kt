package com.app.mobilldu.data.repository

import com.app.mobilldu.data.local.PhotoRecordDao
import com.app.mobilldu.data.local.PhotoRecordEntity
import com.app.mobilldu.data.remote.NetworkClient
import com.app.mobilldu.domain.model.PhotoRecord
import com.app.mobilldu.domain.model.ServerRecord
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

    override suspend fun uploadPhotos(
        sku: String,
        marketplace: String,
        files: List<File>,
        serverUrl: String
    ): Result<Unit> {
        return try {
            val service = NetworkClient.getService(serverUrl)

            val articleBody = sku.trim().toRequestBody("text/plain".toMediaTypeOrNull())
            val marketplaceBody = marketplace.toRequestBody("text/plain".toMediaTypeOrNull())

            val photoParts = files.map { file ->
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("photo", file.name, requestFile)
            }

            val response = service.uploadPhotos(photoParts, articleBody, marketplaceBody)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Server error ${response.code()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getServerHistory(serverUrl: String): Result<List<ServerRecord>> {
        return try {
            val service = NetworkClient.getService(serverUrl)
            val response = service.getPhotos()
            if (response.isSuccessful) {
                val records = response.body()?.map { dto ->
                    ServerRecord(
                        id = dto.Id,
                        article = dto.Article,
                        marketplace = dto.Marketplace ?: "",
                        photoPath = dto.PhotoPath,
                        uploadedAt = dto.UploadedAt
                    )
                } ?: emptyList()
                Result.success(records)
            } else {
                Result.failure(Exception("Server error ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteServerRecord(id: Long, serverUrl: String): Result<Unit> {
        return try {
            val service = NetworkClient.getService(serverUrl)
            val response = service.deletePhoto(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete error ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMarketplace(article: String, marketplace: String, serverUrl: String): Result<Unit> {
        return try {
            val service = NetworkClient.getService(serverUrl)
            val requestBody = mapOf("article" to article, "marketplace" to marketplace)
            val response = service.updateMarketplace(requestBody)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Update marketplace error ${response.code()}"))
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
