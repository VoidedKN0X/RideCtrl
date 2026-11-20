package com.vknxworks.ridectrl

import android.content.Context
import com.vknxworks.ridectrl.data.AppData
import com.vknxworks.ridectrl.data.LogRepository
import com.vknxworks.ridectrl.data.PhotoStore
import com.vknxworks.ridectrl.data.crypto.DataKeyStore
import com.vknxworks.ridectrl.data.crypto.KeystoreWrapper
import com.vknxworks.ridectrl.data.lock.LockVault
import com.vknxworks.ridectrl.reminders.Reminders
import com.vknxworks.ridectrl.data.settings.AppSettings
import com.vknxworks.ridectrl.data.settings.SettingsRepository
import com.vknxworks.ridectrl.ui.lock.AppLock
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
    /** The key that encrypts the logs and photos at rest; itself kept by the Android Keystore. */
    private val dataKey = DataKeyStore(context.filesDir, KeystoreWrapper("ridectrl_data_at_rest"))
    val logs = LogRepository(context, appScope, dataKey)
    val photos = PhotoStore(context, dataKey)

    /** An action from a launcher shortcut or a reminder, waiting for the UI to open it. */
    val pendingAction = MutableStateFlow<String?>(null)

    private var reminderCheck: Job? = null

    init {
        // Reminders follow every change to the logs.
        logs.onChanged = {
            appScope.launch { Reminders.schedule(context) }
            // A new odometer value (a fill, a reading, a trip) can reach a service's km limit: check right away,
            // a few seconds after the last change, instead of waiting for the daily check.
            reminderCheck?.cancel()
            reminderCheck = appScope.launch {
                delay(REMINDER_CHECK_DELAY_MS)
                runCatching { Reminders.check(context, logs.data.value) }
            }
        }
        appScope.launch {
            // Photos that nothing refers to any more (a deleted record, an editor closed without saving).
            logs.ready.first { it }
            // Never with an empty store: if the data file couldn't be read, the photos must survive for a restore.
            val data = logs.data.value
            if (data.vehicles.isNotEmpty()) withContext(Dispatchers.IO) { photos.sweep(data.photoNames()) }
            // Photos from before encryption at rest are sealed now, one by one. Not while the logs can't be opened.
            if (!logs.unreadable.value) withContext(Dispatchers.IO) { runCatching { photos.sealAll() } }
        }
    }

    /** The app lock's PIN and fingerprint key (the shared Ctrl series lock). */
    val vault = LockVault(context, "ridectrl")

    /** Turns the app lock on with [pin]. */
    suspend fun enableLock(pin: String) {
        // Unlocked first, so the lock screen doesn't flash up while the PIN is being saved.
        AppLock.markUnlocked()
        withContext(Dispatchers.Default) { vault.setUp(pin) }
        // Reminders posted before the lock still show what's due: post them again without the details.
        runCatching { Reminders.hideDetails(context) }
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
        // Logs that couldn't even be opened go too (the file is deleted, not set aside).
        logs.startEmpty(keep = false)
        logs.replaceAll(AppData())
        withContext(Dispatchers.IO) { photos.deleteAll() }
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

private const val REMINDER_CHECK_DELAY_MS = 3_000L

val Context.container: AppContainer
    get() = (applicationContext as RideCtrlApp).container
