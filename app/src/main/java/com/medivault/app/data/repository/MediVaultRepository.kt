package com.medivault.app.data.repository

import android.net.Uri
import com.medivault.app.data.backup.BackupManager
import com.medivault.app.data.db.InsuranceConfigEntity
import com.medivault.app.data.db.MediVaultDatabase
import com.medivault.app.data.db.MedicalPhotoEntity
import com.medivault.app.data.db.MedicationCheckInEntity
import com.medivault.app.data.db.MedicationEntity
import com.medivault.app.data.model.BackupManifest
import com.medivault.app.data.model.InsuranceRegionPresets
import com.medivault.app.util.ImageStorageHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MediVaultRepository(
    private val database: MediVaultDatabase,
    val imageStorage: ImageStorageHelper,
    private val backupManager: BackupManager
) {
    private val photoDao = database.medicalPhotoDao()
    private val medicationDao = database.medicationDao()
    private val checkInDao = database.medicationCheckInDao()
    private val insuranceDao = database.insuranceConfigDao()

    // --- 病例照片 ---
    val medicalPhotos: Flow<List<MedicalPhotoEntity>> = photoDao.observeAll()

    suspend fun addMedicalPhoto(fileName: String, note: String, category: String): Long {
        val nextOrder = photoDao.getMaxSortOrder() + 1
        return photoDao.insert(
            MedicalPhotoEntity(
                imageFileName = fileName,
                note = note.trim(),
                category = category,
                sortOrder = nextOrder,
                isBound = true
            )
        )
    }

    suspend fun updateMedicalPhoto(photo: MedicalPhotoEntity) {
        photoDao.update(photo.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * 调换或重新排列病例照片顺序：因为照片与文字备注在同一实体中强绑定，
     * 更新 sortOrder 时图文始终绑定同步移动。
     */
    suspend fun persistPhotoOrder(orderedPhotos: List<MedicalPhotoEntity>) {
        val updated = orderedPhotos.mapIndexed { index, item ->
            item.copy(sortOrder = index, updatedAt = System.currentTimeMillis())
        }
        photoDao.updateAll(updated)
    }

    suspend fun deleteMedicalPhoto(photo: MedicalPhotoEntity) {
        photoDao.delete(photo)
        imageStorage.deleteRecordImage(photo.imageFileName)
    }

    // --- 药品管理与服药打卡 ---
    val medications: Flow<List<MedicationEntity>> = medicationDao.observeAll()

    val allCheckIns: Flow<List<MedicationCheckInEntity>> = checkInDao.observeAll()

    fun observeCheckInsForDate(dateKey: String): Flow<List<MedicationCheckInEntity>> =
        checkInDao.observeByDate(dateKey)

    suspend fun saveMedication(medication: MedicationEntity): Long {
        return if (medication.id == 0L) {
            medicationDao.insert(medication)
        } else {
            medicationDao.update(medication)
            medication.id
        }
    }

    suspend fun deleteMedication(medication: MedicationEntity) {
        medicationDao.delete(medication)
        imageStorage.deleteMedicationImage(medication.imageFileName)
    }

    /**
     * 切换某药品今日第 doseIndex 次服药的打卡状态。
     */
    suspend fun toggleDoseCheckIn(medicationId: Long, dateKey: String, doseIndex: Int) {
        val existing = checkInDao.getForMedicationOnDate(medicationId, dateKey)
            .firstOrNull { it.doseIndex == doseIndex }
        if (existing != null) {
            checkInDao.delete(existing)
        } else {
            checkInDao.insert(
                MedicationCheckInEntity(
                    medicationId = medicationId,
                    dateKey = dateKey,
                    doseIndex = doseIndex
                )
            )
        }
    }

    /**
     * 一键记录下一次服药打卡（如今日已打卡 1/3，点击直接打第 2 次）。
     */
    suspend fun quickCheckInNextDose(medication: MedicationEntity, dateKey: String): Boolean {
        val existing = checkInDao.getForMedicationOnDate(medication.id, dateKey)
        val takenIndices = existing.map { it.doseIndex }.toSet()
        val nextIndex = (1..medication.timesPerDay).firstOrNull { it !in takenIndices } ?: return false
        checkInDao.insert(
            MedicationCheckInEntity(
                medicationId = medication.id,
                dateKey = dateKey,
                doseIndex = nextIndex
            )
        )
        return true
    }

    // --- 医保报销配置 ---
    val insuranceConfig: Flow<InsuranceConfigEntity> = insuranceDao.observeConfig().map { existing ->
        existing ?: InsuranceRegionPresets.buildConfig(
            preset = InsuranceRegionPresets.allRegions.last(),
            insuranceType = "EMPLOYEE",
            visitType = "OUTPATIENT",
            hospitalLevel = "LEVEL_3"
        )
    }

    suspend fun saveInsuranceConfig(config: InsuranceConfigEntity) {
        insuranceDao.upsert(config.copy(id = 1, updatedAt = System.currentTimeMillis()))
    }

    // --- 备份与迁移 ---
    suspend fun exportBackup(uri: Uri): Result<BackupManifest> = backupManager.exportToZip(uri)

    suspend fun inspectBackup(uri: Uri): Result<BackupManifest> = backupManager.inspectBackupZip(uri)

    suspend fun importBackup(uri: Uri, replaceExisting: Boolean): Result<BackupManifest> =
        backupManager.importFromZip(uri, replaceExisting)

    fun getStorageBytes(): Long = imageStorage.calculateStorageBytes()
}
