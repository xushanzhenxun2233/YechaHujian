package com.medivault.app.util

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ImageStorageHelper(private val context: Context) {

    val recordImagesDir: File
        get() = File(context.filesDir, "images").apply { if (!exists()) mkdirs() }

    val medicationImagesDir: File
        get() = File(context.filesDir, "medications").apply { if (!exists()) mkdirs() }

    private val cameraCacheDir: File
        get() = File(context.cacheDir, "camera").apply { if (!exists()) mkdirs() }

    fun getRecordImageFile(fileName: String): File = File(recordImagesDir, fileName)

    fun getMedicationImageFile(fileName: String): File = File(medicationImagesDir, fileName)

    /**
     * 为系统相机拍照创建临时 File 与 content:// URI。
     */
    fun createCameraTempFile(): Pair<File, Uri> {
        val tempFile = File(cameraCacheDir, "cam_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        return tempFile to uri
    }

    /**
     * 将相机拍摄的临时文件持久化到应用私有目录（病例照片或药品照片）。
     */
    fun commitCameraFile(tempFile: File, isMedication: Boolean): String? {
        if (!tempFile.exists() || tempFile.length() == 0L) return null
        val targetDir = if (isMedication) medicationImagesDir else recordImagesDir
        val prefix = if (isMedication) "med" else "rec"
        val fileName = "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
        val destFile = File(targetDir, fileName)
        tempFile.copyTo(destFile, overwrite = true)
        tempFile.delete()
        return fileName
    }

    /**
     * 将相册选中的图片 URI 复制到应用私有目录，确保卸载前和迁移备份时图片永不丢失。
     */
    fun copyUriToStorage(uri: Uri, isMedication: Boolean): String? {
        val targetDir = if (isMedication) medicationImagesDir else recordImagesDir
        val prefix = if (isMedication) "med" else "rec"
        val fileName = "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
        val destFile = File(targetDir, fileName)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0L) fileName else null
        } catch (e: Exception) {
            destFile.delete()
            null
        }
    }

    /**
     * 将病例或药品照片保存到手机系统相册（Pictures/夜叉护笺）。
     */
    suspend fun saveImageToGallery(imageFile: File, displayPrefix: String = "夜叉护笺"): Result<String> =
        withContext(Dispatchers.IO) {
            if (!imageFile.exists()) {
                return@withContext Result.failure(IllegalStateException("图片文件不存在"))
            }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "${displayPrefix}_${stamp}.jpg"

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/夜叉护笺"
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext Result.failure(IllegalStateException("无法写入系统相册"))

                    resolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(imageFile).use { input ->
                            input.copyTo(out)
                        }
                    } ?: return@withContext Result.failure(IllegalStateException("无法打开相册输出流"))

                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    Result.success("Pictures/夜叉护笺/$fileName")
                } else {
                    @Suppress("DEPRECATION")
                    val picturesDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "夜叉护笺"
                    ).apply { if (!exists()) mkdirs() }
                    val target = File(picturesDir, fileName)
                    imageFile.copyTo(target, overwrite = true)
                    MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("image/jpeg"), null)
                    Result.success(target.absolutePath)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * 调用系统分享面板分享照片（支持微信、QQ、邮件等所有系统分享目标）。
     */
    fun shareImage(activityContext: Context, imageFile: File, caption: String = "") {
        if (!imageFile.exists()) return
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            if (caption.isNotBlank()) {
                putExtra(Intent.EXTRA_TEXT, caption)
            }
            clipData = ClipData.newRawUri("病例照片", contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "分享照片").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activityContext.startActivity(chooser)
    }

    fun deleteRecordImage(fileName: String) {
        getRecordImageFile(fileName).takeIf { it.exists() }?.delete()
    }

    fun deleteMedicationImage(fileName: String?) {
        if (!fileName.isNullOrBlank()) {
            getMedicationImageFile(fileName).takeIf { it.exists() }?.delete()
        }
    }

    fun calculateStorageBytes(): Long {
        val recSize = recordImagesDir.listFiles()?.sumOf { it.length() } ?: 0L
        val medSize = medicationImagesDir.listFiles()?.sumOf { it.length() } ?: 0L
        return recSize + medSize
    }
}
