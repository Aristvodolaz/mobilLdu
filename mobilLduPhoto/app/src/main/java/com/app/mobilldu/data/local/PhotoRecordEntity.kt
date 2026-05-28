package com.app.mobilldu.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_records")
data class PhotoRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sku: String,
    val marketplace: String,
    val imagePath: String,           // Локальный путь к фото
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "pending",  // "pending" | "uploaded" | "failed"
    val errorMessage: String? = null
)
