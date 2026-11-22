package com.medivault.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.medivault.app.data.db.MedicationEntity
import com.medivault.app.ui.components.SimplePhotoPreviewDialog
import com.medivault.app.ui.viewmodel.MediVaultViewModel
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MedicationsScreen(viewModel: MediVaultViewModel) {
    val medications by viewModel.medications.collectAsState()
    val selectedDateKey by viewModel.selectedDateKey.collectAsState()
    val checkInsForDate by viewModel.selectedDateCheckIns.collectAsState()

    var editingMedication by remember { mutableStateOf<MedicationEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var previewMedPhoto by remember { mutableStateOf<Pair<String, File>?>(null) }

    // 统计数据
    val totalSpend = remember(medications) { medications.sumOf { it.totalPrice } }
    val currentMonth = remember { YearMonth.now() }
    val monthSpend = remember(medications) {
        medications.filter { med ->
            val ym = YearMonth.from(
                Instant.ofEpochMilli(med.purchasedAt).atZone(ZoneId.systemDefault()).toLocalDate()
            )
            ym == currentMonth
        }.sumOf { it.totalPrice }
    }

    val activeMeds = remember(medications) { medications.filter { it.isActive } }
    val totalDosesTarget = remember(activeMeds) { activeMeds.sumOf { it.timesPerDay } }
    val completedDoses = remember(activeMeds, checkInsForDate) {
        val activeIds = activeMeds.map { it.id }.toSet()
        checkInsForDate.count { it.medicationId in activeIds }
    }
    val progress = if (totalDosesTarget > 0) (completedDoses.toFloat() / totalDosesTarget).coerceIn(0f, 1f) else 0f

    // 最近 7 天日期列表
    val recentDates = remember {
        val today = LocalDate.now()
        (6 downTo 0).map { offset -> today.minusDays(offset.toLong()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("开药统计与服药打卡", fontWeight = FontWeight.Bold)
                        Text(
                            text = "在服 ${activeMeds.size} 种 · 累计开药 ${medications.size} 种",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingMedication = null
                    showAddDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("添加药品") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 顶部费用与打卡统计卡片
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatSummaryCard(
                        title = "本月开药花费",
                        value = "¥${String.format(Locale.US, "%.2f", monthSpend)}",
                        subtitle = "累计 ¥${String.format(Locale.US, "%.2f", totalSpend)}",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryCard(
                        title = "当日服药打卡",
                        value = "$completedDoses / $totalDosesTarget 次",
                        subtitle = if (totalDosesTarget > 0) "完成率 ${(progress * 100).toInt()}%" else "暂无在服药品",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 今日进度条 + 7天日期选择条
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "打卡日期：$selectedDateKey",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(recentDates) { date ->
                                val key = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                val isToday = date == LocalDate.now()
                                val label = if (isToday) "今天" else "${date.monthValue}/${date.dayOfMonth}"
                                FilterChip(
                                    selected = selectedDateKey == key,
                                    onClick = { viewModel.selectDate(key) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            if (medications.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Medication,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "还没有添加任何药品",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "点击右下角「添加药品」，支持拍照保存药盒、记录价格与每次剂量，并每日打卡追踪",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(medications, key = { it.id }) { med ->
                    val takenSet = remember(checkInsForDate, med.id) {
                        checkInsForDate
                            .filter { it.medicationId == med.id }
                            .map { it.doseIndex }
                            .toSet()
                    }
                    val medFile = remember(med.imageFileName) {
                        med.imageFileName?.let { viewModel.repository.imageStorage.getMedicationImageFile(it) }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 药品照片缩略图（点击放大查看，长按可保存到手机或分享）
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable(enabled = medFile != null && medFile.exists()) {
                                            if (medFile != null && medFile.exists()) {
                                                previewMedPhoto = med.name to medFile
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (medFile != null && medFile.exists()) {
                                        AsyncImage(
                                            model = medFile,
                                            contentDescription = med.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Medication,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = med.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Surface(
                                            color = if (med.isActive) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (med.isActive) "在服" else "已停药",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (med.isActive) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "剂量：${med.dosageText} · 每日 ${med.timesPerDay} 次（${med.scheduleHint}）",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "价格：¥${String.format(Locale.US, "%.2f", med.unitPrice)} × ${med.quantity} = ¥${String.format(Locale.US, "%.2f", med.totalPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        editingMedication = med
                                        showAddDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "编辑药品")
                                }
                            }

                            if (med.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "备注：${med.note}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 每日剂量打卡区
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    for (doseIdx in 1..med.timesPerDay) {
                                        val checked = doseIdx in takenSet
                                        FilterChip(
                                            selected = checked,
                                            onClick = { viewModel.toggleDoseCheckIn(med.id, doseIdx) },
                                            label = { Text("第${doseIdx}次") },
                                            leadingIcon = if (checked) {
                                                {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            } else null,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = Color.White,
                                                selectedLeadingIconColor = Color.White
                                            )
                                        )
                                    }
                                }

                                val allDone = takenSet.size >= med.timesPerDay
                                TextButton(
                                    onClick = { viewModel.quickCheckInNextDose(med) },
                                    enabled = !allDone
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (allDone) "已完成" else "打卡")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        MedicationEditorDialog(
            initial = editingMedication,
            viewModel = viewModel,
            onDismiss = {
                showAddDialog = false
                editingMedication = null
            },
            onSave = { med ->
                viewModel.saveMedication(med)
                showAddDialog = false
                editingMedication = null
            },
            onDelete = { med ->
                viewModel.deleteMedication(med)
                showAddDialog = false
                editingMedication = null
            }
        )
    }

    previewMedPhoto?.let { (medTitle, file) ->
        SimplePhotoPreviewDialog(
            title = medTitle,
            imageFile = file,
            imageStorage = viewModel.repository.imageStorage,
            onDismiss = { previewMedPhoto = null }
        )
    }
}

@Composable
private fun StatSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.85f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun MedicationEditorDialog(
    initial: MedicationEntity?,
    viewModel: MediVaultViewModel,
    onDismiss: () -> Unit,
    onSave: (MedicationEntity) -> Unit,
    onDelete: (MedicationEntity) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var unitPriceText by remember { mutableStateOf(initial?.unitPrice?.takeIf { it > 0 }?.toString() ?: "") }
    var quantityText by remember { mutableStateOf((initial?.quantity ?: 1).toString()) }
    var dosageText by remember { mutableStateOf(initial?.dosageText ?: "1片/次") }
    var timesPerDay by remember { mutableIntStateOf(initial?.timesPerDay ?: 3) }
    var scheduleHint by remember { mutableStateOf(initial?.scheduleHint ?: "早中晚饭后") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var isActive by remember { mutableStateOf(initial?.isActive ?: true) }
    var imageFileName by remember { mutableStateOf(initial?.imageFileName) }

    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        if (success && file != null && file.exists()) {
            val saved = viewModel.repository.imageStorage.commitCameraFile(file, isMedication = true)
            if (saved != null) imageFileName = saved
        } else {
            file?.delete()
        }
        pendingCameraFile = null
    }

    val launchMedCamera = {
        val (file, uri) = viewModel.repository.imageStorage.createCameraTempFile()
        pendingCameraFile = file
        cameraLauncher.launch(uri)
    }

    val camPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchMedCamera()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val saved = viewModel.repository.imageStorage.copyUriToStorage(uri, isMedication = true)
            if (saved != null) imageFileName = saved
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "添加药品" else "编辑药品") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 药品照片预览与拍照/相册按钮
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val file = imageFileName?.let { viewModel.repository.imageStorage.getMedicationImageFile(it) }
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (file != null && file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.Medication, contentDescription = null)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) launchMedCamera()
                                else camPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("拍照保存药盒")
                        }
                        OutlinedButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("从相册选择")
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("药品名称 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unitPriceText,
                        onValueChange = { unitPriceText = it },
                        label = { Text("单价 (元)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("数量 (盒/瓶)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = dosageText,
                    onValueChange = { dosageText = it },
                    label = { Text("每次服用剂量（如：2片/次、1袋/次）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 每日次数选择
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("每日打卡次数：", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1, 2, 3, 4).forEach { count ->
                            FilterChip(
                                selected = timesPerDay == count,
                                onClick = { timesPerDay = count },
                                label = { Text("${count}次") }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = scheduleHint,
                    onValueChange = { scheduleHint = it },
                    label = { Text("服用时间提示（如：早晚饭后）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("医嘱/备注") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("当前正在服用（纳入每日打卡）", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }

                if (initial != null) {
                    TextButton(
                        onClick = { onDelete(initial) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("删除此药品")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    val entity = (initial ?: MedicationEntity(name = name.trim())).copy(
                        name = name.trim(),
                        imageFileName = imageFileName,
                        unitPrice = unitPriceText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                        quantity = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                        dosageText = dosageText.ifBlank { "1片/次" },
                        timesPerDay = timesPerDay,
                        scheduleHint = scheduleHint.ifBlank { "按医嘱服用" },
                        note = note.trim(),
                        isActive = isActive
                    )
                    onSave(entity)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
