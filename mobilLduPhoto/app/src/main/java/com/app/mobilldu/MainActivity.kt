package com.app.mobilldu

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.app.mobilldu.ui.theme.MobilLduTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobilLduTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LduPhotoScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Data Model for History Items
data class UploadedItem(
    val id: Int,
    val article: String,
    val photoPath: String,
    val uploadedAt: String
)

// Custom parser to parse json array response without org.json dependency
fun parseUploadedItems(json: String): List<UploadedItem> {
    val items = mutableListOf<UploadedItem>()
    val trimmed = json.trim()
    if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return items

    var braceCount = 0
    var startIdx = -1
    var inString = false
    var isEscaped = false

    val objects = mutableListOf<String>()

    for (i in trimmed.indices) {
        val c = trimmed[i]
        if (isEscaped) {
            isEscaped = false
            continue
        }
        if (c == '\\') {
            isEscaped = true
            continue
        }
        if (c == '"') {
            inString = !inString
            continue
        }
        if (!inString) {
            if (c == '{') {
                if (braceCount == 0) {
                    startIdx = i
                }
                braceCount++
            } else if (c == '}') {
                braceCount--
                if (braceCount == 0 && startIdx != -1) {
                    objects.add(trimmed.substring(startIdx + 1, i))
                    startIdx = -1
                }
            }
        }
    }

    for (objStr in objects) {
        var id = 0
        var article = ""
        var photoPath = ""
        var uploadedAt = ""

        val idRegex = """"Id"\s*:\s*(\d+)""".toRegex(RegexOption.IGNORE_CASE)
        val articleRegex = """"Article"\s*:\s*"((?:[^"\\]|\\.)*)"""".toRegex(RegexOption.IGNORE_CASE)
        val photoPathRegex = """"PhotoPath"\s*:\s*"((?:[^"\\]|\\.)*)"""".toRegex(RegexOption.IGNORE_CASE)
        val uploadedAtRegex = """"UploadedAt"\s*:\s*"((?:[^"\\]|\\.)*)"""".toRegex(RegexOption.IGNORE_CASE)

        idRegex.find(objStr)?.groupValues?.get(1)?.toIntOrNull()?.let { id = it }
        articleRegex.find(objStr)?.groupValues?.get(1)?.let {
            article = it.replace("\\\"", "\"").replace("\\\\", "\\")
        }
        photoPathRegex.find(objStr)?.groupValues?.get(1)?.let {
            photoPath = it.replace("\\\"", "\"").replace("\\\\", "\\")
        }
        uploadedAtRegex.find(objStr)?.groupValues?.get(1)?.let {
            uploadedAt = it.replace("\\\"", "\"").replace("\\\\", "\\")
        }

        items.add(UploadedItem(id, article, photoPath, uploadedAt))
    }

    return items
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LduPhotoScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Shared Preferences for Server URL persistence
    val sharedPrefs: SharedPreferences = remember {
        context.getSharedPreferences("LduPhotoPrefs", Context.MODE_PRIVATE)
    }

    // States
    var serverUrl by remember {
        mutableStateOf(sharedPrefs.getString("server_url", "http://10.171.12.36:3030") ?: "http://10.171.12.36:3030")
    }
    var article by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    var isUploading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isStatusSuccess by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }

    // Navigation and History states
    var currentTab by remember { mutableStateOf("upload") } // "upload" or "history"
    var historyList by remember { mutableStateOf<List<UploadedItem>>(emptyList()) }
    var isHistoryLoading by remember { mutableStateOf(false) }
    var historyError by remember { mutableStateOf<String?>(null) }

    // Load Bitmap whenever Uri changes
    LaunchedEffect(photoUri) {
        if (photoUri != null) {
            withContext(Dispatchers.IO) {
                photoBitmap = try {
                    if (Build.VERSION.SDK_INT < 28) {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, photoUri)
                    } else {
                        val source = ImageDecoder.createSource(context.contentResolver, photoUri!!)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.isMutableRequired = true
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        } else {
            photoBitmap = null
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            statusMessage = null
            photoUri = pendingPhotoUri
            tempPhotoFile = pendingPhotoFile
        } else {
            pendingPhotoUri = null
            pendingPhotoFile = null
            Toast.makeText(context, "Фотосъемка отменена", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper to generate photo URI and launch camera
    fun launchCamera() {
        try {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File.createTempFile("LDU_${timeStamp}_", ".jpg", storageDir)
            
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            
            pendingPhotoFile = file
            pendingPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Ошибка подготовки камеры: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Permission launcher for Camera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(context, "Доступ к камере необходим для съемки товаров", Toast.LENGTH_LONG).show()
        }
    }

    // Check and request runtime Camera permission
    fun checkAndLaunchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Helper to fetch history list
    fun fetchHistory() {
        isHistoryLoading = true
        historyError = null
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val formattedServerUrl = if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                    "http://$serverUrl"
                } else {
                    serverUrl
                }

                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url("$formattedServerUrl/api/photos")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseString = response.body?.string()
                    if (response.isSuccessful && responseString != null) {
                        val itemsList = parseUploadedItems(responseString)
                        withContext(Dispatchers.Main) {
                            historyList = itemsList
                            isHistoryLoading = false
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            historyError = "Ошибка сервера: ${response.code}"
                            isHistoryLoading = false
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    historyError = "Ошибка соединения с сервером"
                    isHistoryLoading = false
                }
            }
        }
    }

    // Auto-fetch history when History tab selected
    LaunchedEffect(currentTab) {
        if (currentTab == "history") {
            fetchHistory()
        }
    }

    // Helper to upload photo
    fun uploadData() {
        if (photoUri == null || tempPhotoFile == null) {
            statusMessage = "Сделайте фотографию перед отправкой"
            isStatusSuccess = false
            return
        }
        if (article.isBlank()) {
            statusMessage = "Введите артикул товара"
            isStatusSuccess = false
            return
        }

        isUploading = true
        statusMessage = "Отправка на сервер..."
        isStatusSuccess = true

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val formattedServerUrl = if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                    "http://$serverUrl"
                } else {
                    serverUrl
                }

                val cacheFile = File(context.cacheDir, "upload_temp.jpg")
                context.contentResolver.openInputStream(photoUri!!)?.use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("article", article.trim())
                    .addFormDataPart(
                        "photo",
                        "photo.jpg",
                        cacheFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url("$formattedServerUrl/api/photos")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    withContext(Dispatchers.Main) {
                        isUploading = false
                        if (response.isSuccessful) {
                            statusMessage = "Данные успешно отправлены в БД!"
                            isStatusSuccess = true
                            photoUri = null
                            tempPhotoFile = null
                            article = ""
                        } else {
                            statusMessage = "Ошибка сервера: ${response.code} ${response.message}"
                            isStatusSuccess = false
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isUploading = false
                    statusMessage = "Ошибка соединения: Проверьте сеть и адрес сервера"
                    isStatusSuccess = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isUploading = false
                    statusMessage = "Критическая ошибка: ${e.message}"
                    isStatusSuccess = false
                }
            }
        }
    }

    // Helper to delete photo
    fun deleteItem(item: UploadedItem) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val formattedServerUrl = if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                    "http://$serverUrl"
                } else {
                    serverUrl
                }

                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url("$formattedServerUrl/api/photos/${item.id}")
                    .delete()
                    .build()

                client.newCall(request).execute().use { response ->
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Товар успешно удален", Toast.LENGTH_SHORT).show()
                            fetchHistory()
                        } else {
                            Toast.makeText(context, "Не удалось удалить товар: ${response.code}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка соединения при удалении", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Комус ЛДУ Фотоотчет",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                actions = {
                    if (currentTab == "history") {
                        IconButton(onClick = { fetchHistory() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Обновить список",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == "upload",
                    onClick = { currentTab = "upload" },
                    label = { Text("Загрузка", fontWeight = if (currentTab == "upload") FontWeight.Bold else FontWeight.Normal) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Загрузить товар"
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "history",
                    onClick = { currentTab = "history" },
                    label = { Text("История", fontWeight = if (currentTab == "history") FontWeight.Bold else FontWeight.Normal) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Список товаров"
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Settings Section
            AnimatedVisibility(visible = showSettings) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Настройки подключения",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = {
                                serverUrl = it
                                sharedPrefs.edit().putString("server_url", it).apply()
                            },
                            label = { Text("Адрес сервера API") },
                            placeholder = { Text("http://10.171.12.36:3030") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Text(
                            text = "Используйте IP-адрес вашего ПК в локальной сети, например: http://192.168.1.50:3030.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showSettings = false },
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Закрыть")
                        }
                    }
                }
            }

            if (currentTab == "upload") {
                UploadFormScreen(
                    context = context,
                    article = article,
                    onArticleChange = { article = it },
                    photoBitmap = photoBitmap,
                    photoUri = photoUri,
                    onClearPhoto = {
                        photoUri = null
                        tempPhotoFile = null
                    },
                    onLaunchCamera = { checkAndLaunchCamera() },
                    isUploading = isUploading,
                    statusMessage = statusMessage,
                    isStatusSuccess = isStatusSuccess,
                    onUpload = { uploadData() }
                )
            } else {
                HistoryListScreen(
                    serverUrl = serverUrl,
                    historyList = historyList,
                    isLoading = isHistoryLoading,
                    error = historyError,
                    onRetry = { fetchHistory() },
                    onDeleteItem = { deleteItem(it) }
                )
            }
        }
    }
}

@Composable
fun UploadFormScreen(
    context: Context,
    article: String,
    onArticleChange: (String) -> Unit,
    photoBitmap: Bitmap?,
    photoUri: Uri?,
    onClearPhoto: () -> Unit,
    onLaunchCamera: () -> Unit,
    isUploading: Boolean,
    statusMessage: String?,
    isStatusSuccess: Boolean,
    onUpload: () -> Unit
) {
    val scrollState = rememberScrollState()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Photo Preview & Camera Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .animateContentSize(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = photoBitmap == null) { onLaunchCamera() },
                contentAlignment = Alignment.Center
            ) {
                if (photoBitmap != null) {
                    Image(
                        bitmap = photoBitmap.asImageBitmap(),
                        contentDescription = "Снимок товара",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Overlay buttons gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.2f))
                                )
                            )
                    )
                    // Overlay Clear button
                    IconButton(
                        onClick = onClearPhoto,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить фото",
                            tint = Color.White
                        )
                    }
                    // Overlay Retake button
                    androidx.compose.material3.FilledTonalButton(
                        onClick = onLaunchCamera,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.85f),
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Переснять фото", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Коснитесь, чтобы сделать фото",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Фотография товара обязательна для отправки",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Input Form Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp).size(28.dp)
                    )
                    Text(
                        text = "Детали товара",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                OutlinedTextField(
                    value = article,
                    onValueChange = onArticleChange,
                    label = { Text("Артикул товара") },
                    placeholder = { Text("Введите артикул") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    ),
                    trailingIcon = {
                        if (article.isNotEmpty()) {
                            IconButton(onClick = { onArticleChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Очистить артикул",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }

        // Status Messages Card
        AnimatedVisibility(visible = statusMessage != null) {
            val containerColor = if (isStatusSuccess) {
                Color(0xFFE8F5E9) // Light green
            } else {
                Color(0xFFFFEBEE) // Light red
            }
            val contentColor = if (isStatusSuccess) {
                Color(0xFF2E7D32) // Dark green
            } else {
                Color(0xFFC62828) // Dark red
            }
            val icon = if (isStatusSuccess) {
                Icons.Default.CheckCircle
            } else {
                Icons.Default.Error
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = contentColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = statusMessage ?: "",
                        color = contentColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Submit Button
        Button(
            onClick = onUpload,
            enabled = !isUploading && photoUri != null && article.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp)
        ) {
            if (isUploading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp).padding(end = 8.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 12.dp).size(24.dp)
                )
            }
            Text(
                text = if (isUploading) "Отправка данных..." else "Отправить в БД",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun HistoryListScreen(
    serverUrl: String,
    historyList: List<UploadedItem>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onDeleteItem: (UploadedItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredHistory = remember(historyList, searchQuery) {
        if (searchQuery.isBlank()) {
            historyList
        } else {
            historyList.filter { item ->
                item.article.contains(searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (error != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
            Button(onClick = onRetry) {
                Text("Повторить попытку")
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск по артикулу...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Поиск",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Очистить поиск",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            if (historyList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                ) {
                    Text(
                        text = "История пуста",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Сделайте фото и отправьте товары в БД, чтобы они появились здесь.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Обновить")
                    }
                }
            } else if (filteredHistory.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Ничего не найдено",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Товаров с артикулом \"$searchQuery\" не найдено в истории.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { searchQuery = "" },
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Text("Сбросить поиск")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredHistory, key = { it.id }) { item ->
                        HistoryItemCard(
                            serverUrl = serverUrl,
                            item = item,
                            onDeleteClick = { onDeleteItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(serverUrl: String, item: UploadedItem, onDeleteClick: () -> Unit) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Удаление записи") },
            text = { Text("Вы уверены, что хотите удалить товар с артикулом \"${item.article}\"? Это действие также удалит фото с сервера.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onDeleteClick()
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageUrl = if (item.photoPath.startsWith("http")) {
                item.photoPath
            } else {
                val base = serverUrl.trimEnd('/')
                val path = if (item.photoPath.startsWith("/")) item.photoPath else "/${item.photoPath}"
                "$base$path"
            }
            
            NetworkImage(
                url = imageUrl,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Артикул: ${item.article}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "ID записи: ${item.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                val formattedDate = formatUploadedDate(item.uploadedAt)
                if (formattedDate.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = formattedDate,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.background(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить запись",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun NetworkImage(url: String, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(url) { mutableStateOf(true) }
    var isError by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        isLoading = true
        isError = false
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null) {
                            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } else {
                            isError = true
                        }
                    } else {
                        isError = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isError = true
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else if (isError || bitmap == null) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        } else {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

fun formatUploadedDate(dateStr: String?): String {
    if (dateStr == null || dateStr.isBlank()) return ""
    return try {
        val cleanDateStr = dateStr.substringBefore('.')
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val date = inputFormat.parse(cleanDateStr)
        val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        if (date != null) outputFormat.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}