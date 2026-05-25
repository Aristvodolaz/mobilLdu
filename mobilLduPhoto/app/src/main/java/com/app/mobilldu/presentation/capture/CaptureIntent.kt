package com.app.mobilldu.presentation.capture

import android.net.Uri

sealed class CaptureIntent {
    /** Пользователь сделал очередное фото */
    data class PhotoTaken(val uri: Uri, val filePath: String) : CaptureIntent()
    /** Пользователь удалил фото из карусели */
    data class RemovePhoto(val index: Int) : CaptureIntent()
    /** Изменение текста артикула */
    data class SkuChanged(val sku: String) : CaptureIntent()
    /** Выбор маркетплейса */
    data class MarketplaceSelected(val marketplace: String) : CaptureIntent()
    /** Загрузить все фото в БД */
    object UploadAll : CaptureIntent()
    /** Сбросить всё после загрузки */
    object ClearAll : CaptureIntent()
    /** Изменить URL сервера */
    data class ServerUrlChanged(val url: String) : CaptureIntent()
    /** Сбросить сообщения об ошибке/успехе */
    object DismissMessage : CaptureIntent()
}
