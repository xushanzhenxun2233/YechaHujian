package com.medivault.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MedicalPhotoEntity::class,
        MedicationEntity::class,
        MedicationCheckInEntity::class,
        InsuranceConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MediVaultDatabase : RoomDatabase() {
    abstract fun medicalPhotoDao(): MedicalPhotoDao
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationCheckInDao(): MedicationCheckInDao
    abstract fun insuranceConfigDao(): InsuranceConfigDao

    companion object {
        @Volatile
        private var INSTANCE: MediVaultDatabase? = null

        fun getInstance(context: Context): MediVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MediVaultDatabase::class.java,
                    "medivault.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
