package com.app.mobilldu.data.remote

data class ServerPhotoDto(
    val Id: Long,
    val Article: String,
    val Marketplace: String?,
    val PhotoPath: String,
    val UploadedAt: String
)
