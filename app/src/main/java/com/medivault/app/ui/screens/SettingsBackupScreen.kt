package com.medivault.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medivault.app.ui.viewmodel.MediVaultViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackupScreen(viewModel: MediVaultViewModel) {
    val photos by viewModel.medicalPhotos.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val checkIns by viewModel.allCheckIns.collectAsState()
    val insuranceConfig by viewModel.insuranceConfig.collectAsState()
    val isBusy by viewModel.isBackupBusy.collectAsState()
    val restorePreview by viewModel.pendingRestorePreview.collectAsState()

    val storageSizeLabel = remember(photos.size, medications.size) {
        val bytes = viewModel.repository.getStorageBytes()
        when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    // 导出完整 ZIP 备份包
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToUri(uri)
        }
    }

    // 导入 ZIP 备份包
    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.inspectBackupFile(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("设备迁移与备份", fontWeight = FontWeight.Bold)
                        Text(
                            text = "全量离线打包迁移 · 保护个人医疗隐私",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. 当前设备数据概览
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "本机已保存的数据",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DataMetricItem("病例照片", "${photos.size} 张")
                            DataMetricItem("药品记录", "${medications.size} 种")
                            DataMetricItem("服药打卡", "${checkIns.size} 次")
                            DataMetricItem("图片占用", storageSizeLabel)
                        }

                        HorizontalDivider()

                        Text(
                            text = "当前医保配置：${insuranceConfig.regionName}（起付线 ¥${insuranceConfig.deductible.toInt()}，报销 ${insuranceConfig.reimbursementRate.toInt()}%）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. 一键导出 & 一键导入恢复
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FolderZip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "完整备份包迁移 (.zip)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "备份包内包含所有病例原图、图文绑定备注与排列顺序、药品照片与价格、全部服药打卡记录以及医保参数设置。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isBusy) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("正在处理备份包，请稍候...")
                            }
                        }

                        Button(
                            onClick = {
                                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                createBackupLauncher.launch("夜叉护笺_备份_$stamp.zip")
                            },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("一键导出完整备份包 (.zip)")
                        }

                        OutlinedButton(
                            onClick = {
                                openBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                            },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("从备份包导入/恢复到本机")
                        }
                    }
                }
            }

            // 3. 跨设备迁移步骤说明 & 隐私说明
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "换机迁移指南与隐私保障",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "1. 在旧手机点击「一键导出完整备份包」，保存 .zip 文件到手机下载目录。\n" +
                                "2. 通过微信文件传输助手、手机互传、蓝牙或数据线将该 .zip 文件发给新手机。\n" +
                                "3. 在新手机打开夜叉护笺，点击「从备份包导入/恢复到本机」并选中该文件，即可完整还原所有照片与记录。\n" +
                                "4. 所有病例与用药数据仅保存在您的本地设备，不上传任何第三方云端服务器。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    // 恢复确认弹窗（展示 manifest 摘要，支持覆盖恢复或合并导入）
    restorePreview?.let { (_, manifest) ->
        val exportTime = remember(manifest.exportedAt) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(manifest.exportedAt))
        }
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestorePreview() },
            title = { Text("确认导入备份包") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("来源设备：${manifest.deviceModel.ifBlank { "未知设备" }}")
                    Text("导出时间：$exportTime")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("• 病例照片（含绑定备注与排序）：${manifest.photoCount} 张")
                    Text("• 药品记录（含药盒照片与价格）：${manifest.medicationCount} 种")
                    Text("• 服药剂量打卡记录：${manifest.checkInCount} 条")
                    Text("• 医保报销配置：${if (manifest.hasInsuranceConfig) "包含" else "无"}")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "提示：「覆盖恢复」将用备份包完整替换本机数据（适合新手机）；「合并导入」会保留本机已有记录并追加备份内容。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.confirmImportBackup(replaceExisting = false) }) {
                        Text("合并导入")
                    }
                    Button(onClick = { viewModel.confirmImportBackup(replaceExisting = true) }) {
                        Text("覆盖恢复")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRestorePreview() }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun DataMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
