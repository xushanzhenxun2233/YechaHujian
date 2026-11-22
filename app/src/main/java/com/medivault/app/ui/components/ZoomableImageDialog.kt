package com.medivault.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.medivault.app.data.db.MedicalPhotoEntity
import com.medivault.app.util.ImageStorageHelper
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val RECORD_CATEGORIES = listOf("病历", "化验单", "处方", "影像", "费用清单", "其他")

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailDialog(
    photo: MedicalPhotoEntity,
    imageFile: File,
    imageStorage: ImageStorageHelper,
    onDismiss: () -> Unit,
    onSave: (MedicalPhotoEntity) -> Unit,
    onDelete: (MedicalPhotoEntity) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var noteText by remember(photo.id) { mutableStateOf(photo.note) }
    var selectedCategory by remember(photo.id) { mutableStateOf(photo.category) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showImageActionsSheet by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val dateLabel = remember(photo.createdAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(photo.createdAt))
    }

    val saveToPhoneGallery = {
        scope.launch {
            val result = imageStorage.saveImageToGallery(imageFile, "夜叉护笺_${photo.category}")
            result.onSuccess {
                Toast.makeText(context, "已保存到手机相册（Pictures/夜叉护笺）", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, "保存失败：${err.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val sharePhoto = {
        val caption = buildString {
            append("[${photo.category}]")
            if (noteText.isNotBlank()) append(" ${noteText.trim()}")
        }
        imageStorage.shareImage(context, imageFile, caption)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部工具栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xCC0F172A))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "长按图片可保存/分享",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "拍摄/导入于 $dateLabel · 双指缩放",
                            color = Color(0xFFCBD5E1),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = { saveToPhoneGallery() }) {
                        Icon(Icons.Default.Download, contentDescription = "保存到手机", tint = Color.White)
                    }
                    IconButton(onClick = { sharePhoto() }) {
                        Icon(Icons.Default.Share, contentDescription = "分享", tint = Color.White)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFFFCA5A5))
                    }
                }

                // 中间可缩放图片查看区（支持长按弹出保存/分享菜单、双击重置缩放）
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showImageActionsSheet = true
                                },
                                onDoubleTap = {
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale
                                offset = if (newScale > 1f) {
                                    offset + pan
                                } else {
                                    Offset.Zero
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = photo.note,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }

                // 底部图文绑定备注编辑面板
                Surface(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "已与照片强绑定（调换位置时备注随照片一起移动）",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            RECORD_CATEGORIES.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("自定义备注（如：血常规报告、医生医嘱等）") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("取消")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onSave(
                                        photo.copy(
                                            note = noteText.trim(),
                                            category = selectedCategory,
                                            isBound = true
                                        )
                                    )
                                    onDismiss()
                                }
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("保存绑定备注")
                            }
                        }
                    }
                }
            }
        }
    }

    // 长按图片弹出的保存/分享操作面板
    if (showImageActionsSheet) {
        ModalBottomSheet(onDismissRequest = { showImageActionsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "图片操作",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        showImageActionsSheet = false
                        saveToPhoneGallery()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存到手机相册")
                }
                OutlinedButton(
                    onClick = {
                        showImageActionsSheet = false
                        sharePhoto()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("调用系统分享（微信/QQ/其他应用）")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除病例照片？") },
            text = { Text("将同时删除照片文件与其绑定的文字备注，删除后不可恢复。") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(photo)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 药品照片全屏查看弹窗：同样支持双指缩放、长按保存到手机相册与调用系统分享。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplePhotoPreviewDialog(
    title: String,
    imageFile: File,
    imageStorage: ImageStorageHelper,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val saveToPhone = {
        scope.launch {
            val result = imageStorage.saveImageToGallery(imageFile, "夜叉护笺_药品")
            result.onSuccess {
                Toast.makeText(context, "已保存到手机相册（Pictures/夜叉护笺）", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, "保存失败：${err.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val sharePhoto = {
        imageStorage.shareImage(context, imageFile, title)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0F172A)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xCC0F172A))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("长按图片可保存到手机或分享", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { saveToPhone() }) {
                        Icon(Icons.Default.Download, contentDescription = "保存到手机", tint = Color.White)
                    }
                    IconButton(onClick = { sharePhoto() }) {
                        Icon(Icons.Default.Share, contentDescription = "分享", tint = Color.White)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showSheet = true
                                },
                                onDoubleTap = {
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale
                                offset = if (newScale > 1f) offset + pan else Offset.Zero
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageFile,
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("图片操作", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = {
                        showSheet = false
                        saveToPhone()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存到手机相册")
                }
                OutlinedButton(
                    onClick = {
                        showSheet = false
                        sharePhoto()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("调用系统分享")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
