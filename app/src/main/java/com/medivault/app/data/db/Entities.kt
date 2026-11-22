package com.medivault.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 病例照片实体：照片文件与文字备注作为同一记录绑定存储，
 * 拖拽调换位置时 sortOrder 随整条记录一起更新，确保图文始终同步移动。
 */
@Serializable
@Entity(tableName = "medical_photos")
data class MedicalPhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imageFileName: String,
    val note: String = "",
    val category: String = "病历",
    val sortOrder: Int = 0,
    val isBound: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 药品记录实体：支持拍照保存药品图片、单价与盒数、每次服用剂量、每日服用次数。
 */
@Serializable
@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val imageFileName: String? = null,
    val unitPrice: Double = 0.0,
    val quantity: Int = 1,
    val dosageText: String = "1片/次",
    val timesPerDay: Int = 3,
    val scheduleHint: String = "早中晚饭后",
    val note: String = "",
    val isActive: Boolean = true,
    val purchasedAt: Long = System.currentTimeMillis()
) {
    val totalPrice: Double
        get() = unitPrice * quantity
}

/**
 * 服药剂量打卡记录实体。
 */
@Serializable
@Entity(
    tableName = "medication_checkins",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicationId"), Index("dateKey")]
)
data class MedicationCheckInEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val dateKey: String, // yyyy-MM-dd
    val doseIndex: Int,  // 1..timesPerDay
    val checkedAt: Long = System.currentTimeMillis()
)

/**
 * 医保报销参数配置实体（单行配置表，id 固定为 1，支持 GPS 推断和手动微调，并随备份迁移）。
 */
@Serializable
@Entity(tableName = "insurance_config")
data class InsuranceConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val regionName: String = "全国通用参考",
    val insuranceType: String = "EMPLOYEE", // EMPLOYEE(职工医保) / RESIDENT(居民医保)
    val visitType: String = "OUTPATIENT",   // OUTPATIENT(门诊) / INPATIENT(住院)
    val hospitalLevel: String = "LEVEL_3",  // LEVEL_1(一级/社区) / LEVEL_2(二级) / LEVEL_3(三级)
    val deductible: Double = 1000.0,        // 起付线 (元)
    val reimbursementRate: Double = 70.0,   // 报销比例 (%)
    val annualCap: Double = 200000.0,       // 年度封顶线 (元)
    val categoryBCopayRate: Double = 10.0,  // 乙类先行自付比例 (%)
    val isCustomOverride: Boolean = false,
    val lastGpsLatitude: Double? = null,
    val lastGpsLongitude: Double? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
