package com.medivault.app

import android.app.Application
import com.medivault.app.data.backup.BackupManager
import com.medivault.app.data.db.MediVaultDatabase
import com.medivault.app.data.repository.MediVaultRepository
import com.medivault.app.util.ImageStorageHelper
import com.medivault.app.util.LocationRegionResolver

class MediVaultApp : Application() {
    val database by lazy { MediVaultDatabase.getInstance(this) }
    val imageStorage by lazy { ImageStorageHelper(this) }
    val backupManager by lazy { BackupManager(this, database, imageStorage) }
    val repository by lazy { MediVaultRepository(database, imageStorage, backupManager) }
    val locationResolver by lazy { LocationRegionResolver(this) }
}
