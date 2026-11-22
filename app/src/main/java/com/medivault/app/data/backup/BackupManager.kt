package com.medivault.app.data.backup

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.room.withTransaction
import com.medivault.app.data.db.MediVaultDatabase
import com.medivault.app.data.model.BackupManifest
import com.medivault.app.data.model.BackupPayload
import com.medivault.app.util.ImageStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(
    private val context: Context,
    private val database: MediVaultDatabase,
    private val imageStorage: ImageStorageHelper
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * 将全部病例照片、图文备注与排序、药品及照片、服药打卡记录、医保参数配置导出为完整 ZIP 备份包。
     */
    suspend fun exportToZip(targetUri: Uri): Result<BackupManifest> = withContext(Dispatchers.IO) {
        try {
            val photos = database.medicalPhotoDao().getAllSync()
            val medications = database.medicationDao().getAllSync()
            val checkIns = database.medicationCheckInDao().getAllSync()
            val insuranceConfig = database.insuranceConfigDao().getConfigSync()

            val manifest = BackupManifest(
                formatVersion = 1,
                exportedAt = System.currentTimeMillis(),
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                photoCount = photos.size,
                medicationCount = medications.size,
                checkInCount = checkIns.size,
                hasInsuranceConfig = insuranceConfig != null
            )

            val payload = BackupPayload(
                manifest = manifest,
                medicalPhotos = photos,
                medications = medications,
                checkIns = checkIns,
                insuranceConfig = insuranceConfig
            )

            val outputStream = context.contentResolver.openOutputStream(targetUri)
                ?: return@withContext Result.failure(IllegalStateException("无法打开目标文件进行写入"))

            ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
                // 1. manifest.json
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(json.encodeToString(manifest).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 2. data.json
                zip.putNextEntry(ZipEntry("data.json"))
                zip.write(json.encodeToString(payload).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 3. 病例照片文件
                val exportedRecordFiles = mutableSetOf<String>()
                for (photo in photos) {
                    val file = imageStorage.getRecordImageFile(photo.imageFileName)
                    if (file.exists() && exportedRecordFiles.add(photo.imageFileName)) {
                        writeFileToZip(zip, file, "images/${photo.imageFileName}")
                    }
                }

                // 4. 药品照片文件
                val exportedMedFiles = mutableSetOf<String>()
                for (med in medications) {
                    val fileName = med.imageFileName ?: continue
                    val file = imageStorage.getMedicationImageFile(fileName)
                    if (file.exists() && exportedMedFiles.add(fileName)) {
                        writeFileToZip(zip, file, "medications/$fileName")
                    }
                }
            }

            Result.success(manifest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 预检备份包，读取 manifest.json 以在恢复前展示数据概览。
     */
    suspend fun inspectBackupZip(sourceUri: Uri): Result<BackupManifest> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(IllegalStateException("无法读取所选备份文件"))

            var manifestJson: String? = null
            ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "manifest.json") {
                        manifestJson = zip.readBytes().toString(Charsets.UTF_8)
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val raw = manifestJson
                ?: return@withContext Result.failure(IllegalArgumentException("该 ZIP 文件不是有效的夜叉护笺备份包（缺少 manifest.json）"))
            Result.success(json.decodeFromString<BackupManifest>(raw))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从完整 ZIP 备份包恢复数据到当前设备。
     * @param replaceExisting true=清空当前数据后完整覆盖恢复；false=保留现有数据并追加合并。
     */
    suspend fun importFromZip(sourceUri: Uri, replaceExisting: Boolean): Result<BackupManifest> =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                    ?: return@withContext Result.failure(IllegalStateException("无法打开备份文件"))

                var payload: BackupPayload? = null
                val tempExtractDir = File(context.cacheDir, "restore_${System.currentTimeMillis()}").apply {
                    mkdirs()
                }
                val tempRecordDir = File(tempExtractDir, "images").apply { mkdirs() }
                val tempMedDir = File(tempExtractDir, "medications").apply { mkdirs() }

                try {
                    ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                when {
                                    entry.name == "data.json" -> {
                                        val raw = zip.readBytes().toString(Charsets.UTF_8)
                                        payload = json.decodeFromString<BackupPayload>(raw)
                                    }
                                    entry.name.startsWith("images/") -> {
                                        val safeName = File(entry.name).name
                                        if (safeName.isNotBlank()) {
                                            extractEntryToFile(zip, File(tempRecordDir, safeName))
                                        }
                                    }
                                    entry.name.startsWith("medications/") -> {
                                        val safeName = File(entry.name).name
                                        if (safeName.isNotBlank()) {
                                            extractEntryToFile(zip, File(tempMedDir, safeName))
                                        }
                                    }
                                }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }

                    val parsed = payload
                        ?: return@withContext Result.failure(IllegalArgumentException("备份包中缺少核心数据 data.json"))

                    // 将解压出的图片复制到应用私有目录
                    if (replaceExisting) {
                        imageStorage.recordImagesDir.listFiles()?.forEach { it.delete() }
                        imageStorage.medicationImagesDir.listFiles()?.forEach { it.delete() }
                    }
                    tempRecordDir.listFiles()?.forEach { file ->
                        file.copyTo(File(imageStorage.recordImagesDir, file.name), overwrite = true)
                    }
                    tempMedDir.listFiles()?.forEach { file ->
                        file.copyTo(File(imageStorage.medicationImagesDir, file.name), overwrite = true)
                    }

                    // 原子事务写入数据库
                    database.withTransaction {
                        if (replaceExisting) {
                            database.medicationCheckInDao().deleteAll()
                            database.medicationDao().deleteAll()
                            database.medicalPhotoDao().deleteAll()
                            database.insuranceConfigDao().deleteAll()

                            database.medicalPhotoDao().insertAll(parsed.medicalPhotos)
                            database.medicationDao().insertAll(parsed.medications)
                            database.medicationCheckInDao().insertAll(parsed.checkIns)
                            parsed.insuranceConfig?.let { database.insuranceConfigDao().upsert(it) }
                        } else {
                            // 合并模式：病例照片追加到末尾，药品重新分配 ID 并同步映射打卡外键
                            val startOrder = database.medicalPhotoDao().getMaxSortOrder() + 1
                            val mergedPhotos = parsed.medicalPhotos.mapIndexed { idx, p ->
                                p.copy(id = 0, sortOrder = startOrder + idx)
                            }
                            database.medicalPhotoDao().insertAll(mergedPhotos)

                            val oldToNewMedId = mutableMapOf<Long, Long>()
                            for (med in parsed.medications) {
                                val newId = database.medicationDao().insert(med.copy(id = 0))
                                oldToNewMedId[med.id] = newId
                            }

                            val mergedCheckIns = parsed.checkIns.mapNotNull { checkIn ->
                                val mappedMedId = oldToNewMedId[checkIn.medicationId] ?: return@mapNotNull null
                                checkIn.copy(id = 0, medicationId = mappedMedId)
                            }
                            database.medicationCheckInDao().insertAll(mergedCheckIns)

                            if (database.insuranceConfigDao().getConfigSync() == null) {
                                parsed.insuranceConfig?.let { database.insuranceConfigDao().upsert(it) }
                            }
                        }
                    }

                    Result.success(parsed.manifest)
                } finally {
                    tempExtractDir.deleteRecursively()
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun writeFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { input ->
            input.copyTo(zip)
        }
        zip.closeEntry()
    }

    private fun extractEntryToFile(zip: ZipInputStream, destFile: File) {
        FileOutputStream(destFile).use { output ->
            zip.copyTo(output)
        }
    }
}
