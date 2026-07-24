package xyz.xiao6.myboard.pack

import android.content.Context
import java.io.File

/**
 * Process-wide package state composition root shared by the IME and settings UI.
 * The persisted state lives in app-private storage and therefore needs no storage permission.
 */
object PackageStoreProvider {
    @Volatile
    private var instance: PackageStore? = null

    fun get(context: Context): PackageStore = instance ?: synchronized(this) {
        instance ?: PackageStore(
            FilePackagePersistence(
                File(context.applicationContext.filesDir, PACKAGE_DIRECTORY)
            )
        ).also { instance = it }
    }

    private const val PACKAGE_DIRECTORY = "language-packages"
}
