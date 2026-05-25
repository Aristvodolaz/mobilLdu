package com.app.mobilldu.presentation.capture

import android.net.Uri

data class CaptureState(
    /** Список отснятых фото (URI + путь к файлу) */
    val photos: List<PhotoEntry> = emptyList(),
    /** Текущая страница карусели */
    val currentPage: Int = 0,
    /** Артикул */
    val sku: String = "",
    /** Выбранный маркетплейс */
    val marketplace: String = "OZON",
    /** Идёт загрузка */
    val isUploading: Boolean = false,
    /** Сколько фото уже загружено в текущей сессии */
    val uploadProgress: Int = 0,
    /** Сообщение об успехе */
    val successMessage: String? = null,
    /** Сообщение об ошибке */
    val errorMessage: String? = null,
    /** URL сервера */
    val serverUrl: String = "http://10.171.12.36:3030"
)

data class PhotoEntry(
    val uri: Uri,
    val filePath: String
)
