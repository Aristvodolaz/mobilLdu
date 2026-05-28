package com.app.mobilldu.presentation.history


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.app.mobilldu.domain.model.PhotoRecord
import com.app.mobilldu.domain.model.UploadStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    state: HistoryState,
    onIntent: (HistoryIntent) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- HEADER ---
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
                Text(
                    text = "История",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Все отправленные и сохранённые фото",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }

            // --- LIST ---
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (state.records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            "Нет записей",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            "Записи появятся после отправки фото",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                // Summary row
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val uploaded = state.records.count { it.status == UploadStatus.UPLOADED }
                    val failed = state.records.count { it.status == UploadStatus.FAILED }
                    val pending = state.records.count { it.status == UploadStatus.PENDING }

                    SummaryChip(count = uploaded, label = "Загружено", color = MaterialTheme.colorScheme.primary)
                    if (failed > 0) SummaryChip(count = failed, label = "Ошибка", color = MaterialTheme.colorScheme.error)
                    if (pending > 0) SummaryChip(count = pending, label = "Ожидание", color = MaterialTheme.colorScheme.tertiary)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = state.records,
                        key = { it.id }
                    ) { record ->
                        HistoryRecordCard(
                            record = record,
                            dateFormat = dateFormat,
                            onTap = { onIntent(HistoryIntent.SelectRecord(record.id)) },
                            onDelete = { onIntent(HistoryIntent.DeleteRecord(record.id)) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) } // space for bottom nav
                }
            }
        }

        // --- FULL SCREEN PHOTO VIEWER DIALOG ---
        if (state.selectedRecord != null) {
            FullScreenPhotoDialog(
                record = state.selectedRecord,
                allRecords = state.records,
                dateFormat = dateFormat,
                onDismiss = { onIntent(HistoryIntent.SelectRecord(null)) }
            )
        }
    }
}

@Composable
private fun SummaryChip(count: Int, label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = count.toString(),
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 13.sp
            )
            Text(
                text = label,
                color = color,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun HistoryRecordCard(
    record: PhotoRecord,
    dateFormat: SimpleDateFormat,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Thumbnail
            val imageFile = File(record.imagePath)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (imageFile.exists()) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = "Фото",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.BrokenImage,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                // Status overlay badge
                val (statusIcon, statusColor) = when (record.status) {
                    UploadStatus.UPLOADED -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
                    UploadStatus.FAILED -> Icons.Default.Error to MaterialTheme.colorScheme.error
                    UploadStatus.PENDING -> Icons.Default.HourglassTop to Color(0xFFFFC107)
                }
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .size(18.dp)
                        .background(Color.White, RoundedCornerShape(50))
                )
            }

            Spacer(Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Marketplace badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = record.marketplace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = record.sku,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = dateFormat.format(Date(record.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (record.status == UploadStatus.FAILED && record.errorMessage != null) {
                    Text(
                        text = record.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1
                    )
                }
            }

            // Delete button
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить запись?") },
            text = { Text("Запись артикула «${record.sku}» будет удалена из истории.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FullScreenPhotoDialog(
    record: PhotoRecord,
    allRecords: List<PhotoRecord>,
    dateFormat: SimpleDateFormat,
    onDismiss: () -> Unit
) {
    val initialPage = allRecords.indexOfFirst { it.id == record.id }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { allRecords.size }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val pageRecord = allRecords.getOrNull(page)
                if (pageRecord != null) {
                    val file = File(pageRecord.imagePath)
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = "Просмотр фото",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color.White, modifier = Modifier.size(72.dp))
                        }
                    }
                }
            }

            // Top info bar
            val currentRecord = allRecords.getOrNull(pagerState.currentPage)
            if (currentRecord != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentRecord.marketplace,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = currentRecord.sku,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Text(
                                text = dateFormat.format(Date(currentRecord.createdAt)),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "${pagerState.currentPage + 1} / ${allRecords.size}",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
