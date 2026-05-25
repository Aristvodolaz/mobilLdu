package com.app.mobilldu.presentation.capture

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.mobilldu.domain.usecase.UploadPhotoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class CaptureViewModel(
    private val uploadPhotoUseCase: UploadPhotoUseCase,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(CaptureState())
    val state: StateFlow<CaptureState> = _state.asStateFlow()

    fun processIntent(intent: CaptureIntent) {
        when (intent) {
            is CaptureIntent.PhotoTaken -> {
                _state.update { current ->
                    current.copy(
                        photos = current.photos + PhotoEntry(intent.uri, intent.filePath),
                        currentPage = current.photos.size // перейти на новое (последнее) фото
                    )
                }
            }

            is CaptureIntent.RemovePhoto -> {
                _state.update { current ->
                    val newPhotos = current.photos.toMutableList()
                    newPhotos.removeAt(intent.index)
                    val newPage = if (current.currentPage >= newPhotos.size && newPhotos.isNotEmpty())
                        newPhotos.size - 1
                    else
                        current.currentPage
                    current.copy(photos = newPhotos, currentPage = newPage)
                }
            }

            is CaptureIntent.SkuChanged -> {
                _state.update { it.copy(sku = intent.sku) }
            }

            is CaptureIntent.MarketplaceSelected -> {
                _state.update { it.copy(marketplace = intent.marketplace) }
            }

            is CaptureIntent.ServerUrlChanged -> {
                _state.update { it.copy(serverUrl = intent.url) }
                saveServerUrl(intent.url)
            }

            is CaptureIntent.UploadAll -> {
                uploadAllPhotos()
            }

            is CaptureIntent.ClearAll -> {
                _state.update {
                    it.copy(
                        photos = emptyList(),
                        currentPage = 0,
                        sku = "",
                        uploadProgress = 0,
                        successMessage = null,
                        errorMessage = null
                    )
                }
            }

            is CaptureIntent.DismissMessage -> {
                _state.update { it.copy(successMessage = null, errorMessage = null) }
            }
        }
    }

    private fun uploadAllPhotos() {
        val current = _state.value
        if (current.sku.isBlank()) {
            _state.update { it.copy(errorMessage = "Заполните артикул (SKU)") }
            return
        }
        if (current.photos.isEmpty()) {
            _state.update { it.copy(errorMessage = "Сделайте хотя бы одно фото") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isUploading = true, uploadProgress = 0, errorMessage = null, successMessage = null) }

            var successCount = 0
            var failCount = 0
            val errors = mutableListOf<String>()

            current.photos.forEachIndexed { index, photoEntry ->
                val file = File(photoEntry.filePath)
                if (!file.exists()) {
                    failCount++
                    errors.add("Фото ${index + 1}: файл не найден")
                    return@forEachIndexed
                }

                val result = uploadPhotoUseCase(
                    sku = current.sku,
                    marketplace = current.marketplace,
                    file = file,
                    serverUrl = current.serverUrl
                )

                if (result.isSuccess) {
                    successCount++
                } else {
                    failCount++
                    errors.add("Фото ${index + 1}: ${result.exceptionOrNull()?.message ?: "Ошибка"}")
                }

                _state.update { it.copy(uploadProgress = index + 1) }
            }

            val message = when {
                failCount == 0 -> "✅ Все $successCount фото успешно загружены!"
                successCount == 0 -> "❌ Загрузка не удалась: ${errors.joinToString("; ")}"
                else -> "⚠️ Загружено $successCount из ${current.photos.size}. Ошибки: ${errors.joinToString("; ")}"
            }

            _state.update {
                it.copy(
                    isUploading = false,
                    successMessage = if (failCount == 0) message else null,
                    errorMessage = if (failCount > 0) message else null
                )
            }

            // Auto clear after full success
            if (failCount == 0) {
                _state.update {
                    it.copy(photos = emptyList(), currentPage = 0, sku = "", uploadProgress = 0)
                }
            }
        }
    }

    private fun saveServerUrl(url: String) {
        val prefs = appContext.getSharedPreferences("LduPhotoPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("server_url", url).apply()
    }

    fun loadServerUrl() {
        val prefs = appContext.getSharedPreferences("LduPhotoPrefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("server_url", "http://10.171.12.36:3030") ?: "http://10.171.12.36:3030"
        _state.update { it.copy(serverUrl = saved) }
    }

    class Factory(
        private val uploadPhotoUseCase: UploadPhotoUseCase,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CaptureViewModel(uploadPhotoUseCase, context.applicationContext) as T
        }
    }
}
