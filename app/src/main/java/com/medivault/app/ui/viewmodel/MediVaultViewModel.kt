package com.medivault.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medivault.app.data.db.InsuranceConfigEntity
import com.medivault.app.data.db.MedicalPhotoEntity
import com.medivault.app.data.db.MedicationCheckInEntity
import com.medivault.app.data.db.MedicationEntity
import com.medivault.app.data.model.BackupManifest
import com.medivault.app.data.model.InsuranceRegionPresets
import com.medivault.app.data.repository.MediVaultRepository
import com.medivault.app.util.LocationRegionResolver
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MediVaultViewModel(
    val repository: MediVaultRepository,
    private val locationResolver: LocationRegionResolver
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _selectedDateKey = MutableStateFlow(LocalDate.now().format(dateFormatter))
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    val medicalPhotos: StateFlow<List<MedicalPhotoEntity>> = repository.medicalPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medications: StateFlow<List<MedicationEntity>> = repository.medications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCheckIns: StateFlow<List<MedicationCheckInEntity>> = repository.allCheckIns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedDateCheckIns: StateFlow<List<MedicationCheckInEntity>> = _selectedDateKey
        .flatMapLatest { dateKey -> repository.observeCheckInsForDate(dateKey) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val insuranceConfig: StateFlow<InsuranceConfigEntity> = repository.insuranceConfig
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            InsuranceRegionPresets.buildConfig(
                preset = InsuranceRegionPresets.allRegions.last(),
                insuranceType = "EMPLOYEE",
                visitType = "OUTPATIENT",
                hospitalLevel = "LEVEL_3"
            )
        )

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private val _lastLocationNote = MutableStateFlow<String?>(null)
    val lastLocationNote: StateFlow<String?> = _lastLocationNote.asStateFlow()

    private val _isBackupBusy = MutableStateFlow(false)
    val isBackupBusy: StateFlow<Boolean> = _isBackupBusy.asStateFlow()

    private val _pendingRestorePreview = MutableStateFlow<Pair<Uri, BackupManifest>?>(null)
    val pendingRestorePreview: StateFlow<Pair<Uri, BackupManifest>?> = _pendingRestorePreview.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    // --- 病例照片操作 ---
    fun addRecordFromCamera(tempFile: File, note: String, category: String) {
        viewModelScope.launch {
            val savedName = repository.imageStorage.commitCameraFile(tempFile, isMedication = false)
            if (savedName != null) {
                repository.addMedicalPhoto(savedName, note, category)
                _snackbarMessages.tryEmit("病例照片已保存并绑定备注")
            } else {
                _snackbarMessages.tryEmit("保存拍照图片失败，请重试")
            }
        }
    }

    fun addRecordsFromGallery(uris: List<Uri>, defaultNote: String, category: String) {
        viewModelScope.launch {
            var count = 0
            for (uri in uris) {
                val savedName = repository.imageStorage.copyUriToStorage(uri, isMedication = false)
                if (savedName != null) {
                    repository.addMedicalPhoto(savedName, defaultNote, category)
                    count++
                }
            }
            if (count > 0) {
                _snackbarMessages.tryEmit("已导入 $count 张病例照片")
            } else {
                _snackbarMessages.tryEmit("未能读取选中的图片")
            }
        }
    }

    fun updateRecordPhoto(photo: MedicalPhotoEntity) {
        viewModelScope.launch {
            repository.updateMedicalPhoto(photo)
            _snackbarMessages.tryEmit("备注已更新")
        }
    }

    fun deleteRecordPhoto(photo: MedicalPhotoEntity) {
        viewModelScope.launch {
            repository.deleteMedicalPhoto(photo)
            _snackbarMessages.tryEmit("已删除病例照片")
        }
    }

    fun reorderRecordPhotos(newOrder: List<MedicalPhotoEntity>) {
        viewModelScope.launch {
            repository.persistPhotoOrder(newOrder)
        }
    }

    fun swapRecordPhotos(fromIndex: Int, toIndex: Int) {
        val current = medicalPhotos.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        reorderRecordPhotos(current)
    }

    // --- 药品管理与服药打卡 ---
    fun selectDate(dateKey: String) {
        _selectedDateKey.value = dateKey
    }

    fun saveMedication(medication: MedicationEntity) {
        viewModelScope.launch {
            repository.saveMedication(medication)
            _snackbarMessages.tryEmit("药品「${medication.name}」已保存")
        }
    }

    fun deleteMedication(medication: MedicationEntity) {
        viewModelScope.launch {
            repository.deleteMedication(medication)
            _snackbarMessages.tryEmit("已删除药品「${medication.name}」")
        }
    }

    fun toggleDoseCheckIn(medicationId: Long, doseIndex: Int) {
        viewModelScope.launch {
            repository.toggleDoseCheckIn(medicationId, _selectedDateKey.value, doseIndex)
        }
    }

    fun quickCheckInNextDose(medication: MedicationEntity) {
        viewModelScope.launch {
            val done = repository.quickCheckInNextDose(medication, _selectedDateKey.value)
            if (done) {
                _snackbarMessages.tryEmit("已打卡：${medication.name} (${medication.dosageText})")
            } else {
                _snackbarMessages.tryEmit("「${medication.name}」今日剂量已全部完成")
            }
        }
    }

    // --- 医保计算器与 GPS 地区推断 ---
    fun detectRegionByGps() {
        viewModelScope.launch {
            _isLocating.value = true
            val result = locationResolver.detectCurrentRegion()
            _isLocating.value = false
            result.onSuccess { gps ->
                val current = insuranceConfig.value
                val newConfig = InsuranceRegionPresets.buildConfig(
                    preset = gps.preset,
                    insuranceType = current.insuranceType,
                    visitType = current.visitType,
                    hospitalLevel = current.hospitalLevel,
                    lat = gps.latitude,
                    lng = gps.longitude
                )
                repository.saveInsuranceConfig(newConfig)
                _lastLocationNote.value = "GPS 已识别：${gps.detectedAddressLabel} → 已匹配「${gps.preset.regionName}」参考政策"
                _snackbarMessages.tryEmit("已根据 GPS 自动匹配${gps.preset.regionName}医保比例")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "GPS 定位失败，请手动选择地区")
            }
        }
    }

    fun applyRegionPreset(
        regionName: String,
        insuranceType: String,
        visitType: String,
        hospitalLevel: String
    ) {
        viewModelScope.launch {
            val current = insuranceConfig.value
            val preset = InsuranceRegionPresets.findByName(regionName)
            val updated = InsuranceRegionPresets.buildConfig(
                preset = preset,
                insuranceType = insuranceType,
                visitType = visitType,
                hospitalLevel = hospitalLevel,
                lat = current.lastGpsLatitude,
                lng = current.lastGpsLongitude
            )
            repository.saveInsuranceConfig(updated)
        }
    }

    fun saveCustomInsuranceParams(
        deductible: Double,
        reimbursementRate: Double,
        annualCap: Double,
        categoryBCopayRate: Double
    ) {
        viewModelScope.launch {
            val current = insuranceConfig.value
            repository.saveInsuranceConfig(
                current.copy(
                    deductible = deductible.coerceAtLeast(0.0),
                    reimbursementRate = reimbursementRate.coerceIn(0.0, 100.0),
                    annualCap = annualCap.coerceAtLeast(0.0),
                    categoryBCopayRate = categoryBCopayRate.coerceIn(0.0, 100.0),
                    isCustomOverride = true
                )
            )
            _snackbarMessages.tryEmit("自定义医保参数已保存")
        }
    }

    // --- 全量设备迁移 (ZIP 导出 / 导入) ---
    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            _isBackupBusy.value = true
            val result = repository.exportBackup(uri)
            _isBackupBusy.value = false
            result.onSuccess { manifest ->
                _snackbarMessages.tryEmit(
                    "备份导出成功！含 ${manifest.photoCount} 张病例、${manifest.medicationCount} 种药品、${manifest.checkInCount} 条打卡"
                )
            }.onFailure { err ->
                _snackbarMessages.tryEmit("导出失败：${err.message}")
            }
        }
    }

    fun inspectBackupFile(uri: Uri) {
        viewModelScope.launch {
            _isBackupBusy.value = true
            val result = repository.inspectBackup(uri)
            _isBackupBusy.value = false
            result.onSuccess { manifest ->
                _pendingRestorePreview.value = uri to manifest
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "无法识别备份包")
            }
        }
    }

    fun dismissRestorePreview() {
        _pendingRestorePreview.value = null
    }

    fun confirmImportBackup(replaceExisting: Boolean) {
        val pending = _pendingRestorePreview.value ?: return
        _pendingRestorePreview.value = null
        viewModelScope.launch {
            _isBackupBusy.value = true
            val result = repository.importBackup(pending.first, replaceExisting)
            _isBackupBusy.value = false
            result.onSuccess { manifest ->
                val mode = if (replaceExisting) "覆盖恢复" else "合并导入"
                _snackbarMessages.tryEmit(
                    "${mode}成功！已迁移 ${manifest.photoCount} 张病例、${manifest.medicationCount} 种药品"
                )
            }.onFailure { err ->
                _snackbarMessages.tryEmit("恢复失败：${err.message}")
            }
        }
    }

    class Factory(
        private val repository: MediVaultRepository,
        private val locationResolver: LocationRegionResolver
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MediVaultViewModel(repository, locationResolver) as T
        }
    }
}
