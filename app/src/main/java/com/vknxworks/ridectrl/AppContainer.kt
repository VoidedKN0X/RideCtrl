package com.vknxworks.ridectrl

import android.content.Context
import com.vknxworks.ridectrl.data.lock.LockVault
import com.vknxworks.ridectrl.data.settings.AppSettings
import com.vknxworks.ridectrl.data.settings.SettingsRepository
import com.vknxworks.ridectrl.ui.lock.AppLock
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

/** Manual dependency container; one per process. */
class AppContainer(val context: Context) {
    /**
     * Background work that outlives a screen (saving logs and settings). A failure there (for example a full
     * disk) must never close the app, so it's dropped here instead of crashing.
     */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, _ -> })

    val settings = SettingsRepository(context, appScope)

    /** The app lock's PIN and fingerprint key (the shared Ctrl series lock). */
    val vault = LockVault(context, "ridectrl")

    /** Turns the app lock on with [pin]. */
    suspend fun enableLock(pin: String) {
        // Unlocked first, so the lock screen doesn't flash up while the PIN is being saved.
        AppLock.markUnlocked()
        withContext(Dispatchers.Default) { vault.setUp(pin) }
    }

    /** Turns the app lock off (after the PIN was confirmed). */
    fun disableLock() {
        vault.disable()
    }

    /**
     * "Forgot PIN?": the PIN can't be recovered, and skipping it would make the lock pointless, so every
     * log, photo and setting on the phone is deleted and the lock turned off.
     */
    suspend fun forgetEverything() {
        withContext(Dispatchers.IO) {
            context.filesDir.listFiles()?.forEach { f ->
                // The settings store lives under datastore/ and is reset below; everything else is RideCtrl data.
                if (f.name != "datastore") f.deleteRecursively()
            }
        }
        settings.update { AppSettings() }
        vault.disable()
        AppLock.markUnlocked()
    }

    fun onLocked() {
        vault.close()
    }

    fun onUnlocked() {
        AppLock.markUnlocked()
    }

    init {
        AppLock.onLocked = { onLocked() }
    }
}

val Context.container: AppContainer
    get() = (applicationContext as RideCtrlApp).container
