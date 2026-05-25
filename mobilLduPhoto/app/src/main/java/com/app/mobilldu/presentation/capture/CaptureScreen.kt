package com.app.mobilldu.presentation.capture

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    state: CaptureState,
    onIntent: (CaptureIntent) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var marketplaceExpanded by remember { mutableStateOf(false) }
    val marketplaces = listOf("OZON", "OZON фреш", "WB", "ЯМ")

    // Temp file URI for camera
    var pendingFileUri by remember { mutableStateOf<Uri?>(null) }
    var pendingFilePath by remember { mutableStateOf<String?>(null) }

    val getCameraUri = {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File.createTempFile("LDU_${timeStamp}_", ".jpg", storageDir)
            pendingFilePath = file.absolutePath
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingFileUri = uri
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                val uri = pendingFileUri
                val path = pendingFilePath
                if (uri != null && path != null) {
                    onIntent(CaptureIntent.PhotoTaken(uri, path))
                }
            }
            pendingFileUri = null
            pendingFilePath = null
        }
    )

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val uri = getCameraUri()
                if (uri != null) takePictureLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Разрешение на камеру отклонено", Toast.LENGTH_SHORT).show()
            }
        }
    )

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val uri = getCameraUri()
            if (uri != null) takePictureLauncher.launch(uri)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Pager for carousel
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { maxOf(state.photos.size, 1) }
    )

    // Sync pager with state.currentPage
    LaunchedEffect(state.currentPage) {
        if (state.photos.isNotEmpty() && pagerState.currentPage != state.currentPage) {
            pagerState.animateScrollToPage(state.currentPage.coerceIn(0, state.photos.size - 1))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "LduPhoto",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Съёмка и отправка фото",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { showSettingsDialog = true },
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = MaterialTheme.colorScheme.primary)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        // --- PHOTO CAROUSEL ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.photos.isEmpty()) {
                    // Empty placeholder — tap to shoot
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { launchCamera() },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Сделать фото",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Нажмите, чтобы сделать фото",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Carousel
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val entry = state.photos.getOrNull(page)
                        if (entry != null) {
                            AsyncImage(
                                model = entry.uri,
                                contentDescription = "Фото ${page + 1}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Page indicator dots
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(state.photos.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (pagerState.currentPage == index) 10.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pagerState.currentPage == index)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            Color.White.copy(alpha = 0.6f)
                                    )
                            )
                        }
                    }

                    // Counter badge (top left)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${state.photos.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Delete current photo button (top right)
                    IconButton(
                        onClick = { onIntent(CaptureIntent.RemovePhoto(pagerState.currentPage)) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить фото", tint = Color.White)
                    }
                }
            }
        }

        // --- ADD PHOTO BUTTON ---
        OutlinedButton(
            onClick = { launchCamera() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.AddAPhoto, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (state.photos.isEmpty()) "Сделать фото"
                else "Добавить ещё фото"
            )
        }

        // --- MARKETPLACE DROPDOWN ---
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.marketplace,
                onValueChange = {},
                readOnly = true,
                label = { Text("Маркетплейс (префикс)") },
                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.clickable { marketplaceExpanded = true }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Box(modifier = Modifier.matchParentSize().clickable { marketplaceExpanded = true })

            DropdownMenu(
                expanded = marketplaceExpanded,
                onDismissRequest = { marketplaceExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                marketplaces.forEach { mp ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                mp,
                                fontWeight = if (state.marketplace == mp) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onIntent(CaptureIntent.MarketplaceSelected(mp))
                            marketplaceExpanded = false
                        }
                    )
                }
            }
        }

        // --- SKU INPUT ---
        OutlinedTextField(
            value = state.sku,
            onValueChange = { onIntent(CaptureIntent.SkuChanged(it)) },
            label = { Text("Артикул товара (SKU)") },
            placeholder = { Text("Например: 123456") },
            leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
            trailingIcon = {
                if (state.sku.isNotEmpty()) {
                    IconButton(onClick = { onIntent(CaptureIntent.SkuChanged("")) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Очистить")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // --- UPLOAD PROGRESS ---
        AnimatedVisibility(visible = state.isUploading) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Загружено ${state.uploadProgress} из ${state.photos.size} фото...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = {
                        if (state.photos.isEmpty()) 0f
                        else state.uploadProgress.toFloat() / state.photos.size
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // --- STATUS MESSAGE ---
        AnimatedVisibility(
            visible = state.successMessage != null || state.errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val isSuccess = state.successMessage != null
            val message = state.successMessage ?: state.errorMessage ?: ""
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSuccess)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onIntent(CaptureIntent.DismissMessage) }) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // --- UPLOAD BUTTON ---
        Button(
            onClick = { onIntent(CaptureIntent.UploadAll) },
            enabled = !state.isUploading && state.photos.isNotEmpty() && state.sku.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (state.isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Text(
                        text = if (state.photos.size > 1)
                            "Отправить ${state.photos.size} фото в БД"
                        else
                            "Отправить фото в БД",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    // --- SETTINGS DIALOG ---
    if (showSettingsDialog) {
        var urlInput by remember { mutableStateOf(state.serverUrl) }
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Настройки сервера", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Укажите адрес бэкенда (например: http://192.168.1.100:3000)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Адрес API") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onIntent(CaptureIntent.ServerUrlChanged(urlInput.trim()))
                    showSettingsDialog = false
                    Toast.makeText(context, "Адрес сохранён", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("Отмена") }
            }
        )
    }
}
