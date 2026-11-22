package com.medivault.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.medivault.app.data.model.InsuranceCalculator
import com.medivault.app.data.model.InsuranceRegionPresets
import com.medivault.app.ui.viewmodel.MediVaultViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InsuranceScreen(viewModel: MediVaultViewModel) {
    val context = LocalContext.current
    val config by viewModel.insuranceConfig.collectAsState()
    val isLocating by viewModel.isLocating.collectAsState()
    val lastLocationNote by viewModel.lastLocationNote.collectAsState()
    val medications by viewModel.medications.collectAsState()

    var regionMenuExpanded by remember { mutableStateOf(false) }
    var showCustomParamsDialog by remember { mutableStateOf(false) }

    // 费用输入
    var catAText by remember { mutableStateOf("2000") }
    var catBText by remember { mutableStateOf("800") }
    var catCText by remember { mutableStateOf("200") }
    var alreadyReimbursedText by remember { mutableStateOf("0") }

    val result = remember(config, catAText, catBText, catCText, alreadyReimbursedText) {
        InsuranceCalculator.calculate(
            config = config,
            categoryAAmount = catAText.toDoubleOrNull() ?: 0.0,
            categoryBAmount = catBText.toDoubleOrNull() ?: 0.0,
            categoryCAmount = catCText.toDoubleOrNull() ?: 0.0,
            alreadyReimbursedThisYear = alreadyReimbursedText.toDoubleOrNull() ?: 0.0
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.detectRegionByGps()
        }
    }

    val triggerGpsDetect = {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            viewModel.detectRegionByGps()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("医保报销计算器", fontWeight = FontWeight.Bold)
                        Text(
                            text = "GPS 推断当地医保政策 · 支持手动微调参数",
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
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. 地区定位与医保政策卡片
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = config.regionName + if (config.isCustomOverride) " (已自定义)" else "",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = lastLocationNote ?: "可点击右侧 GPS 定位自动匹配当地比例",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = triggerGpsDetect,
                                enabled = !isLocating,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isLocating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isLocating) "定位中..." else "GPS 自动推断")
                            }

                            OutlinedButton(
                                onClick = { regionMenuExpanded = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("切换地区")
                                DropdownMenu(
                                    expanded = regionMenuExpanded,
                                    onDismissRequest = { regionMenuExpanded = false }
                                ) {
                                    InsuranceRegionPresets.allRegions.forEach { preset ->
                                        DropdownMenuItem(
                                            text = { Text(preset.regionName) },
                                            onClick = {
                                                regionMenuExpanded = false
                                                viewModel.applyRegionPreset(
                                                    regionName = preset.regionName,
                                                    insuranceType = config.insuranceType,
                                                    visitType = config.visitType,
                                                    hospitalLevel = config.hospitalLevel
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 医保类型 & 就诊类型 & 医院等级
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = config.insuranceType == "EMPLOYEE",
                                onClick = {
                                    viewModel.applyRegionPreset(
                                        config.regionName, "EMPLOYEE", config.visitType, config.hospitalLevel
                                    )
                                },
                                label = { Text("职工医保") }
                            )
                            FilterChip(
                                selected = config.insuranceType == "RESIDENT",
                                onClick = {
                                    viewModel.applyRegionPreset(
                                        config.regionName, "RESIDENT", config.visitType, config.hospitalLevel
                                    )
                                },
                                label = { Text("居民医保") }
                            )
                            FilterChip(
                                selected = config.visitType == "OUTPATIENT",
                                onClick = {
                                    viewModel.applyRegionPreset(
                                        config.regionName, config.insuranceType, "OUTPATIENT", config.hospitalLevel
                                    )
                                },
                                label = { Text("门诊") }
                            )
                            FilterChip(
                                selected = config.visitType == "INPATIENT",
                                onClick = {
                                    viewModel.applyRegionPreset(
                                        config.regionName, config.insuranceType, "INPATIENT", config.hospitalLevel
                                    )
                                },
                                label = { Text("住院") }
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "LEVEL_1" to "一级/社区医院",
                                "LEVEL_2" to "二级医院",
                                "LEVEL_3" to "三级医院"
                            ).forEach { (code, label) ->
                                FilterChip(
                                    selected = config.hospitalLevel == code,
                                    onClick = {
                                        viewModel.applyRegionPreset(
                                            config.regionName, config.insuranceType, config.visitType, code
                                        )
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }

                        // 当前生效参数摘要 + 手动微调入口
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "起付线 ¥${config.deductible.toInt()} · 报销比例 ${config.reimbursementRate.toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "年度封顶 ¥${(config.annualCap / 10000.0).let { String.format(Locale.US, "%.1f", it) }}万 · 乙类先行自付 ${config.categoryBCopayRate.toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { showCustomParamsDialog = true }) {
                                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("调参数")
                                }
                            }
                        }
                    }
                }
            }

            // 2. 费用录入卡片
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "本次医疗费用明细",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val totalMedSpend = medications.sumOf { it.totalPrice }
                            if (totalMedSpend > 0) {
                                TextButton(
                                    onClick = {
                                        catAText = String.format(Locale.US, "%.2f", totalMedSpend)
                                        catBText = "0"
                                        catCText = "0"
                                    }
                                ) {
                                    Text("带入开药费 ¥${String.format(Locale.US, "%.0f", totalMedSpend)}")
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = catAText,
                                onValueChange = { catAText = it },
                                label = { Text("甲类费用 (元)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = catBText,
                                onValueChange = { catBText = it },
                                label = { Text("乙类费用 (元)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = catCText,
                                onValueChange = { catCText = it },
                                label = { Text("丙类全自费 (元)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = alreadyReimbursedText,
                                onValueChange = { alreadyReimbursedText = it },
                                label = { Text("本年已报销 (元)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3. 报销测算结果卡片
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "预计医保报销测算结果",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "预计医保报销",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "¥${String.format(Locale.US, "%.2f", result.reimbursedAmount)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "预计个人自付",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "¥${String.format(Locale.US, "%.2f", result.personalTotalPay)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { (result.effectiveRatioPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )
                        Text(
                            text = "综合实际报销比例：${String.format(Locale.US, "%.1f", result.effectiveRatioPercent)}%（总费用 ¥${String.format(Locale.US, "%.2f", result.totalCost)}）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

                        BreakdownRow("纳入医保统筹基数", result.eligibleBase)
                        BreakdownRow("起付线个人承担", result.deductibleUsed)
                        BreakdownRow("乙类先行自付 (${config.categoryBCopayRate.toInt()}%)", result.categoryBCopay)
                        BreakdownRow("丙类全自费", result.categoryCSelfPay)
                        if (result.overCapSelfPay > 0) {
                            BreakdownRow("超年度封顶线自付", result.overCapSelfPay)
                        }

                        Text(
                            text = "注：各地医保细则可能随年度调整，GPS 匹配结果为参考政策，可点击「调参数」按当地医保局最新标准精准修正。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    // 自定义参数设置弹窗
    if (showCustomParamsDialog) {
        var deductibleInput by remember { mutableStateOf(config.deductible.toString()) }
        var rateInput by remember { mutableStateOf(config.reimbursementRate.toString()) }
        var capInput by remember { mutableStateOf(config.annualCap.toString()) }
        var catBInput by remember { mutableStateOf(config.categoryBCopayRate.toString()) }

        AlertDialog(
            onDismissRequest = { showCustomParamsDialog = false },
            title = { Text("自定义医保报销参数") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = deductibleInput,
                        onValueChange = { deductibleInput = it },
                        label = { Text("起付线 (元)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it },
                        label = { Text("报销比例 (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = capInput,
                        onValueChange = { capInput = it },
                        label = { Text("年度封顶线 (元)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = catBInput,
                        onValueChange = { catBInput = it },
                        label = { Text("乙类先行自付比例 (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCustomInsuranceParams(
                            deductible = deductibleInput.toDoubleOrNull() ?: config.deductible,
                            reimbursementRate = rateInput.toDoubleOrNull() ?: config.reimbursementRate,
                            annualCap = capInput.toDoubleOrNull() ?: config.annualCap,
                            categoryBCopayRate = catBInput.toDoubleOrNull() ?: config.categoryBCopayRate
                        )
                        showCustomParamsDialog = false
                    }
                ) {
                    Text("保存参数")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomParamsDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun BreakdownRow(label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
        )
        Text(
            text = "¥${String.format(Locale.US, "%.2f", amount)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
