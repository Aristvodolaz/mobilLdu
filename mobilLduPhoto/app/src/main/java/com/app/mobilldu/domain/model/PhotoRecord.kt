package com.app.mobilldu.domain.model

data class PhotoRecord(
    val id: Long,
    val sku: String,
    val marketplace: String,
    val imagePath: String,
    val createdAt: Long,
    val status: UploadStatus,
    val errorMessage: String? = null
)

enum class UploadStatus {
    PENDING, UPLOADED, FAILED
}
