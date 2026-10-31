package com.vknxworks.ridectrl

import android.content.Context
import com.vknxworks.ridectrl.data.AppData
import com.vknxworks.ridectrl.data.LogRepository
import com.vknxworks.ridectrl.data.PhotoStore
import com.vknxworks.ridectrl.data.lock.LockVault
import com.vknxworks.ridectrl.reminders.Reminders
import com.vknxworks.ridectrl.widget.RideWidget
import com.vknxworks.ridectrl.data.settings.AppSettings
import com.vknxworks.ridectrl.data.settings.SettingsRepository
import com.vknxworks.ridectrl.ui.lock.AppLock
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Manual dependency container; one per process. */
class AppContainer(val context: Context) {
    /**
     * Background work that outlives a screen (saving logs and settings). A failure there (for example a full
     * disk) must never close the app, so it's dropped here instead of crashing.
     */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, _ -> })

    val settings = SettingsRepository(context, appScope)
    val logs = LogRepository(context, appScope)
    val photos = PhotoStore(context)

    /** An action from a launcher shortcut or the widget, waiting for the UI to open it. */
    val pendingAction = MutableStateFlow<String?>(null)

    init {
        // Reminders and the widget follow every change to the logs.
        logs.onChanged = {
            appScope.launch {
                Reminders.schedule(context)
                RideWidget.updateAll(context)
            }
        }
        appScope.launch {
            // Photos that nothing refers to any more (a deleted record, an editor closed without saving).
            logs.ready.first { it }
            // Never with an empty store: if the data file couldn't be read, the photos must survive for a restore.
            val data = logs.data.value
            if (data.vehicles.isNotEmpty()) withContext(Dispatchers.IO) { photos.sweep(data.photoNames()) }
        }
    }

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
        logs.replaceAll(AppData())
        withContext(Dispatchers.IO) { photos.dir.listFiles()?.forEach { it.delete() } }
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
