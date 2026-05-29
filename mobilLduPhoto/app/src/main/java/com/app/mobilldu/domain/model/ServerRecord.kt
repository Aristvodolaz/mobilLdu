package com.app.mobilldu.domain.model

data class ServerRecord(
    val id: Long,
    val article: String,
    val marketplace: String,
    val photoPath: String,    // путь вида /uploads/photo-xxx.jpg
    val uploadedAt: String    // строка даты с сервера
)
