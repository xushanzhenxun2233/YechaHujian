package com.medivault.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalPhotoDao {
    @Query("SELECT * FROM medical_photos ORDER BY sortOrder ASC, createdAt DESC")
    fun observeAll(): Flow<List<MedicalPhotoEntity>>

    @Query("SELECT * FROM medical_photos ORDER BY sortOrder ASC, createdAt DESC")
    suspend fun getAllSync(): List<MedicalPhotoEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM medical_photos")
    suspend fun getMaxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: MedicalPhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<MedicalPhotoEntity>)

    @Update
    suspend fun update(photo: MedicalPhotoEntity)

    @Update
    suspend fun updateAll(photos: List<MedicalPhotoEntity>)

    @Delete
    suspend fun delete(photo: MedicalPhotoEntity)

    @Query("DELETE FROM medical_photos")
    suspend fun deleteAll()
}

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY isActive DESC, purchasedAt DESC")
    fun observeAll(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications ORDER BY purchasedAt DESC")
    suspend fun getAllSync(): List<MedicationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: MedicationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medications: List<MedicationEntity>)

    @Update
    suspend fun update(medication: MedicationEntity)

    @Delete
    suspend fun delete(medication: MedicationEntity)

    @Query("DELETE FROM medications")
    suspend fun deleteAll()
}

@Dao
interface MedicationCheckInDao {
    @Query("SELECT * FROM medication_checkins WHERE dateKey = :dateKey ORDER BY checkedAt ASC")
    fun observeByDate(dateKey: String): Flow<List<MedicationCheckInEntity>>

    @Query("SELECT * FROM medication_checkins ORDER BY checkedAt DESC")
    fun observeAll(): Flow<List<MedicationCheckInEntity>>

    @Query("SELECT * FROM medication_checkins ORDER BY checkedAt ASC")
    suspend fun getAllSync(): List<MedicationCheckInEntity>

    @Query("SELECT * FROM medication_checkins WHERE medicationId = :medicationId AND dateKey = :dateKey ORDER BY doseIndex ASC")
    suspend fun getForMedicationOnDate(medicationId: Long, dateKey: String): List<MedicationCheckInEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(checkIn: MedicationCheckInEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(checkIns: List<MedicationCheckInEntity>)

    @Delete
    suspend fun delete(checkIn: MedicationCheckInEntity)

    @Query("DELETE FROM medication_checkins")
    suspend fun deleteAll()
}

@Dao
interface InsuranceConfigDao {
    @Query("SELECT * FROM insurance_config WHERE id = 1")
    fun observeConfig(): Flow<InsuranceConfigEntity?>

    @Query("SELECT * FROM insurance_config WHERE id = 1")
    suspend fun getConfigSync(): InsuranceConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: InsuranceConfigEntity)

    @Query("DELETE FROM insurance_config")
    suspend fun deleteAll()
}
