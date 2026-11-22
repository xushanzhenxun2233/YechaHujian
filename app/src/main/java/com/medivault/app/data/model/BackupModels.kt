package com.medivault.app.data.model

import com.medivault.app.data.db.InsuranceConfigEntity
import com.medivault.app.data.db.MedicalPhotoEntity
import com.medivault.app.data.db.MedicationCheckInEntity
import com.medivault.app.data.db.MedicationEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupManifest(
    val formatVersion: Int = 1,
    val appName: String = "夜叉护笺",
    val exportedAt: Long = System.currentTimeMillis(),
    val deviceModel: String = "",
    val photoCount: Int = 0,
    val medicationCount: Int = 0,
    val checkInCount: Int = 0,
    val hasInsuranceConfig: Boolean = false
)

@Serializable
data class BackupPayload(
    val manifest: BackupManifest,
    val medicalPhotos: List<MedicalPhotoEntity> = emptyList(),
    val medications: List<MedicationEntity> = emptyList(),
    val checkIns: List<MedicationCheckInEntity> = emptyList(),
    val insuranceConfig: InsuranceConfigEntity? = null
)
