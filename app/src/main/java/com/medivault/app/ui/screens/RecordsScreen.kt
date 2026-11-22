package com.medivault.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.medivault.app.data.db.MedicalPhotoEntity
import com.medivault.app.ui.components.RECORD_CATEGORIES
import com.medivault.app.ui.components.RecordDetailDialog
import com.medivault.app.ui.viewmodel.MediVaultViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordsScreen(viewModel: MediVaultViewModel) {
    val context = LocalContext.current
    val allPhotos by viewModel.medicalPhotos.collectAsState()

    var gridColumns by remember { mutableIntStateOf(2) }
    var isReorderMode by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("全部") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPhotoForDetail by remember { mutableStateOf<MedicalPhotoEntity?>(null) }

    // 拍照/相册选择弹窗 & 备注输入弹窗
    var showAddSourceSheet by remember { mutableStateOf(false) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraCapturedFile by remember { mutableStateOf<File?>(null) }
    var pendingGalleryUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // 本地可拖拽排序列表（仅在全部且无搜索过滤时允许自由拖拽调换位置）
    val canReorder = selectedCategory == "全部" && searchQuery.isBlank()
    var localList by remember { mutableStateOf(allPhotos) }
    LaunchedEffect(allPhotos) {
        localList = allPhotos
    }

    val displayedPhotos = remember(localList, selectedCategory, searchQuery) {
        localList.filter { photo ->
            val matchCat = selectedCategory == "全部" || photo.category == selectedCategory
            val matchQuery = searchQuery.isBlank() ||
                photo.note.contains(searchQuery, ignoreCase = true) ||
                photo.category.contains(searchQuery, ignoreCase = true)
            matchCat && matchQuery
        }
    }

    // 相机拍照 Launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        if (success && file != null && file.exists()) {
            pendingCameraCapturedFile = file
        } else {
            file?.delete()
        }
        pendingCameraFile = null
    }

    val launchCamera = {
        val (file, uri) = viewModel.repository.imageStorage.createCameraTempFile()
        pendingCameraFile = file
        takePictureLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        }
    }

    // 系统相册多选 Launcher
    val pickPhotosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            pendingGalleryUris = uris
        }
    }

    // 拖拽状态
    val gridState = rememberLazyGridState()
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("病例相册", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isReorderMode) "排序模式：长按拖拽或点箭头调换（图文同步移动）"
                            else "共 ${allPhotos.size} 份病例 · 长按卡片可拖拽调换位置",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = { gridColumns = if (gridColumns == 2) 3 else 2 }) {
                        Icon(
                            Icons.Default.GridView,
                            contentDescription = "切换平铺列数",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            if (canReorder) {
                                isReorderMode = !isReorderMode
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = "排序模式",
                            tint = if (isReorderMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSourceSheet = true },
                icon = { Icon(Icons.Default.AddAPhoto, contentDescription = null) },
                text = { Text("添加病例") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 搜索框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (it.isNotBlank()) isReorderMode = false
                },
                placeholder = { Text("搜索病例备注或分类...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            // 分类筛选栏
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("全部") + RECORD_CATEGORIES) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = cat
                            if (cat != "全部") isReorderMode = false
                        },
                        label = { Text(cat) }
                    )
                }
            }

            if (isReorderMode) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "图文已绑定：移动任意照片时，备注与分类将随照片一起同步移动",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { isReorderMode = false }) {
                            Text("完成")
                        }
                    }
                }
            }

            if (displayedPhotos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (allPhotos.isEmpty()) "还没有保存任何病例照片" else "没有匹配的病例记录",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "点击右下角「添加病例」，可调用系统相机拍照或从相册导入，支持自定义备注并平铺管理",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(gridColumns),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(canReorder, localList.size) {
                            if (!canReorder) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = { startOffset ->
                                    val hitItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                        startOffset.x.toInt() in item.offset.x..(item.offset.x + item.size.width) &&
                                            startOffset.y.toInt() in item.offset.y..(item.offset.y + item.size.height)
                                    }
                                    draggedIndex = hitItem?.index
                                    dragOffset = Offset.Zero
                                },
                                onDragEnd = {
                                    if (draggedIndex != null) {
                                        viewModel.reorderRecordPhotos(localList)
                                    }
                                    draggedIndex = null
                                    dragOffset = Offset.Zero
                                },
                                onDragCancel = {
                                    draggedIndex = null
                                    dragOffset = Offset.Zero
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val currentIdx = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                    dragOffset += dragAmount

                                    val currentInfo = gridState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.index == currentIdx } ?: return@detectDragGesturesAfterLongPress
                                    val centerX = currentInfo.offset.x + currentInfo.size.width / 2f + dragOffset.x
                                    val centerY = currentInfo.offset.y + currentInfo.size.height / 2f + dragOffset.y

                                    val targetInfo = gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                        item.index != currentIdx &&
                                            centerX.toInt() in item.offset.x..(item.offset.x + item.size.width) &&
                                            centerY.toInt() in item.offset.y..(item.offset.y + item.size.height)
                                    }

                                    if (targetInfo != null && targetInfo.index in localList.indices) {
                                        val mutable = localList.toMutableList()
                                        val moved = mutable.removeAt(currentIdx)
                                        mutable.add(targetInfo.index, moved)
                                        localList = mutable
                                        draggedIndex = targetInfo.index
                                        dragOffset = Offset.Zero
                                    }
                                }
                            )
                        }
                ) {
                    itemsIndexed(
                        items = displayedPhotos,
                        key = { _, item -> item.id }
                    ) { index, photo ->
                        val isDragging = draggedIndex == index
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 12.dp else 2.dp,
                            label = "drag_elevation"
                        )
                        val imageFile = remember(photo.imageFileName) {
                            viewModel.repository.imageStorage.getRecordImageFile(photo.imageFileName)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 10f else 0f)
                                .graphicsLayer {
                                    if (isDragging) {
                                        translationX = dragOffset.x
                                        translationY = dragOffset.y
                                        scaleX = 1.04f
                                        scaleY = 1.04f
                                    }
                                }
                                .clickable(enabled = !isReorderMode) {
                                    selectedPhotoForDetail = photo
                                },
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
                            border = if (isDragging || isReorderMode) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else null,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .background(Color(0xFFE2E8F0))
                                ) {
                                    AsyncImage(
                                        model = imageFile,
                                        contentDescription = photo.note,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // 左上角分类标签
                                    Surface(
                                        color = Color(0xCC0F172A),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = photo.category,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // 右上角图文绑定/序号徽标
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                if (isReorderMode) Icons.Default.DragIndicator else Icons.Default.Link,
                                                contentDescription = "图文绑定",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "#${index + 1}",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }

                                // 绑定的文字备注区（与照片在同一 Card 内强绑定）
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = photo.note.ifBlank { "点击添加备注..." },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (photo.note.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                                        color = if (photo.note.isNotBlank()) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (isReorderMode && canReorder) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.swapRecordPhotos(index, index - 1) },
                                                enabled = index > 0,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "前移",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.swapRecordPhotos(index, 0) },
                                                enabled = index > 0,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.VerticalAlignTop,
                                                    contentDescription = "置顶",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.swapRecordPhotos(index, index + 1) },
                                                enabled = index < localList.lastIndex,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = "后移",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 选择拍照或相册 BottomSheet
    if (showAddSourceSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSourceSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "添加病例照片",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        showAddSourceSheet = false
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) launchCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("调用系统相机拍照")
                }
                OutlinedButton(
                    onClick = {
                        showAddSourceSheet = false
                        pickPhotosLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("从系统相册选择照片（支持多选）")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 拍照或相册选择后的绑定备注录入对话框
    if (pendingCameraCapturedFile != null || pendingGalleryUris.isNotEmpty()) {
        var noteInput by remember { mutableStateOf("") }
        var categoryInput by remember { mutableStateOf("病历") }
        val count = if (pendingCameraCapturedFile != null) 1 else pendingGalleryUris.size

        AlertDialog(
            onDismissRequest = {
                pendingCameraCapturedFile?.delete()
                pendingCameraCapturedFile = null
                pendingGalleryUris = emptyList()
            },
            title = { Text("为 $count 张病例照片绑定备注") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "备注将与照片强绑定，后续自由调整顺序时图文将一起移动。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RECORD_CATEGORIES.forEach { cat ->
                            FilterChip(
                                selected = categoryInput == cat,
                                onClick = { categoryInput = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("自定义备注（如：2026-11 协和医院复诊血常规）") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val camFile = pendingCameraCapturedFile
                        if (camFile != null) {
                            viewModel.addRecordFromCamera(camFile, noteInput, categoryInput)
                            pendingCameraCapturedFile = null
                        } else if (pendingGalleryUris.isNotEmpty()) {
                            viewModel.addRecordsFromGallery(pendingGalleryUris, noteInput, categoryInput)
                            pendingGalleryUris = emptyList()
                        }
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingCameraCapturedFile?.delete()
                        pendingCameraCapturedFile = null
                        pendingGalleryUris = emptyList()
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

    // 病例大图查看与备注编辑
    selectedPhotoForDetail?.let { photo ->
        val file = viewModel.repository.imageStorage.getRecordImageFile(photo.imageFileName)
        RecordDetailDialog(
            photo = photo,
            imageFile = file,
            imageStorage = viewModel.repository.imageStorage,
            onDismiss = { selectedPhotoForDetail = null },
            onSave = { updated -> viewModel.updateRecordPhoto(updated) },
            onDelete = { toDelete -> viewModel.deleteRecordPhoto(toDelete) }
        )
    }
}
