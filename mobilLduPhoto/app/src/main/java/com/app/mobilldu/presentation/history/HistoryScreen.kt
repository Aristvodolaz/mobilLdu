package com.app.mobilldu.presentation.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.app.mobilldu.domain.model.ServerRecord

@Composable
fun HistoryScreen(
    state: HistoryState,
    onIntent: (HistoryIntent) -> Unit
) {
    var recordToDelete by remember { mutableStateOf<ServerRecord?>(null) }
    var groupToDelete by remember { mutableStateOf<List<ServerRecord>?>(null) }

    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Удалить фото", fontWeight = FontWeight.Bold) },
            text = { Text("Вы действительно хотите удалить это фото?") },
            confirmButton = {
                Button(
                    onClick = {
                        recordToDelete?.let { onIntent(HistoryIntent.DeleteServerRecord(it.id)) }
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (groupToDelete != null) {
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Удалить группу", fontWeight = FontWeight.Bold) },
            text = { Text("Вы действительно хотите удалить все фото (${groupToDelete?.size} шт.) для артикула ${groupToDelete?.firstOrNull()?.article}?") },
            confirmButton = {
                Button(
                    onClick = {
                        groupToDelete?.forEach { record ->
                            onIntent(HistoryIntent.DeleteServerRecord(record.id))
                        }
                        groupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить всё")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize()) {

            // ── HEADER ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "История",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!state.isServerLoading && state.serverRecords.isNotEmpty()) {
                        Text(
                            text = "${state.serverRecords.size} фото на сервере",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { onIntent(HistoryIntent.LoadServerHistory) }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Обновить",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // ── ПОИСК ───────────────────────────────────────────────────────
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { onIntent(HistoryIntent.SearchQueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Поиск по артикулу...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onIntent(HistoryIntent.SearchQueryChanged("")) }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // ── CONTENT ─────────────────────────────────────────────────────
            when {
                state.isServerLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text("Загрузка истории...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                state.serverError != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.CloudOff, null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                            )
                            Text(
                                "Нет подключения к серверу",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                state.serverError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = { onIntent(HistoryIntent.LoadServerHistory) }) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Повторить")
                            }
                        }
                    }
                }

                state.serverRecords.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary, null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                            Text(
                                "Нет записей",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                else -> {
                    // Фильтрация по поисковому запросу
                    val filtered = if (state.searchQuery.isBlank()) state.serverRecords
                    else state.serverRecords.filter {
                        it.article.contains(state.searchQuery.trim(), ignoreCase = true)
                    }

                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.SearchOff, null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Text(
                                    "Артикул «${state.searchQuery}» не найден",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        ServerRecordsList(
                            records = filtered,
                            serverUrl = state.serverUrl,
                            onTap = { onIntent(HistoryIntent.SelectServerRecord(it)) },
                            onDeleteRecord = { recordToDelete = it },
                            onDeleteGroup = { groupToDelete = it }
                        )
                    }
                }
            }
        }

        // ── FULLSCREEN VIEWER ────────────────────────────────────────────────
        if (state.selectedServerRecord != null) {
            ServerFullScreenViewer(
                record = state.selectedServerRecord,
                allRecords = state.serverRecords,
                serverUrl = state.serverUrl,
                onDeleteRecord = {
                    recordToDelete = it
                    onIntent(HistoryIntent.SelectServerRecord(null))
                },
                onDismiss = { onIntent(HistoryIntent.SelectServerRecord(null)) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Список с группировкой по артикулу
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ServerRecordsList(
    records: List<ServerRecord>,
    serverUrl: String,
    onTap: (Long) -> Unit,
    onDeleteRecord: (ServerRecord) -> Unit,
    onDeleteGroup: (List<ServerRecord>) -> Unit
) {
    // Группируем по артикулу (сохраняем порядок — первый вариант)
    val grouped = remember(records) {
        records.groupBy { it.article }
            .entries.sortedByDescending { entry -> entry.value.first().id }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = grouped, key = { it.key }) { (article, group) ->
            ArticleGroup(
                article = article,
                records = group,
                serverUrl = serverUrl,
                onTap = onTap,
                onDeleteRecord = onDeleteRecord,
                onDeleteGroup = onDeleteGroup
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ArticleGroup(
    article: String,
    records: List<ServerRecord>,
    serverUrl: String,
    onTap: (Long) -> Unit,
    onDeleteRecord: (ServerRecord) -> Unit,
    onDeleteGroup: (List<ServerRecord>) -> Unit
) {
    val marketplace = records.firstOrNull()?.marketplace?.takeIf { it.isNotBlank() }
    val date = records.firstOrNull()?.uploadedAt?.take(10) ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ── Заголовок группы ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val mpText = if (marketplace.isNullOrBlank()) "Не указан" else marketplace
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (marketplace.isNullOrBlank()) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = mpText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (marketplace.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = article,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // счётчик фото
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${records.size} фото",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { onDeleteGroup(records) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить группу",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ── Горизонтальный ряд миниатюр ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                records.take(5).forEach { record ->
                    val imageUrl = serverUrl.trimEnd('/') + record.photoPath
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = article,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onTap(record.id) },
                            contentScale = ContentScale.Crop,
                            onState = {}
                        )
                        IconButton(
                            onClick = { onDeleteRecord(record) },
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Удалить фото",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
                // Если больше 5 — показываем "+N"
                if (records.size > 5) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { onTap(records[5].id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+${records.size - 5}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Полноэкранный просмотр с каруселью
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServerFullScreenViewer(
    record: ServerRecord,
    allRecords: List<ServerRecord>,
    serverUrl: String,
    onDeleteRecord: (ServerRecord) -> Unit,
    onDismiss: () -> Unit
) {
    // Показываем только фото из той же группы (тот же артикул)
    val groupRecords = remember(allRecords, record) {
        allRecords.filter { it.article == record.article }
    }
    val initialPage = groupRecords.indexOfFirst { it.id == record.id }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { groupRecords.size }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // ── Карусель фото ──
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageRecord = groupRecords.getOrNull(page)
                if (pageRecord != null) {
                    val imageUrl = serverUrl.trimEnd('/') + pageRecord.photoPath
                    var loading by remember { mutableStateOf(true) }

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = pageRecord.article,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            onState = { state ->
                                loading = state is AsyncImagePainter.State.Loading
                            }
                        )
                        if (loading) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }
            }

            // ── Верхняя панель с инфо ──
            val currentRecord = groupRecords.getOrNull(pagerState.currentPage)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val mp = currentRecord?.marketplace?.takeIf { it.isNotBlank() }
                            if (mp != null) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = mp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Text(
                                text = currentRecord?.article ?: "",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = currentRecord?.uploadedAt?.take(19)?.replace("T", "  ") ?: "",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }

                    // Кнопка удаления текущего фото
                    IconButton(
                        onClick = {
                            if (currentRecord != null) {
                                onDeleteRecord(currentRecord)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить фото",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.width(8.dp))

                    // Счётчик страниц
                    Text(
                        text = "${pagerState.currentPage + 1} / ${groupRecords.size}",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
                    }
                }
            }

            // ── Индикатор точек внизу ──
            if (groupRecords.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    groupRecords.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) Color.White
                                    else Color.White.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }
        }
    }
}
