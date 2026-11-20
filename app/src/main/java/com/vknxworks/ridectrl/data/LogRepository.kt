package com.vknxworks.ridectrl.data

import android.content.Context
import com.vknxworks.ridectrl.data.crypto.DataKeyStore
import com.vknxworks.ridectrl.data.crypto.SealedFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

val dataJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

fun newId(): String = UUID.randomUUID().toString()

/**
 * Every vehicle and log, kept in one JSON file in the app's private storage. Changes show at once and are
 * written in the background; the file is replaced in one step, so a crash mid-save keeps the last good copy.
 *
 * The file is encrypted at rest with the app's data key ([DataKeyStore], [SealedFile]). A file from before
 * encryption is still read, and is then replaced by its encrypted copy (see [SealedFile.sealInPlace]). When
 * there is no data key to be had, the app keeps reading and writing the ordinary file, as before.
 */
class LogRepository(context: Context, private val scope: CoroutineScope, private val keys: DataKeyStore) {
    private val file = File(context.filesDir, FILE)
    private val writeLock = Mutex()

    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    private val _unreadable = MutableStateFlow(false)
    /**
     * The saved logs are encrypted and can't be opened (the key in the phone's secure storage is gone or
     * changed, or the file is damaged). The file is left exactly as it is, nothing is written while this is
     * true, and the app shows what happened instead of an empty garage.
     */
    val unreadable: StateFlow<Boolean> = _unreadable

    /** Called after every change (reminders refresh from here). */
    var onChanged: (AppData) -> Unit = {}

    init {
        scope.launch(Dispatchers.IO) { open() }
    }

    private suspend fun open() = writeLock.withLock {
        val loaded = load(file, keys, migrate = true)
        if (loaded != null) _data.value = loaded
        _unreadable.value = loaded == null
        _ready.value = true
    }

    fun update(transform: (AppData) -> AppData) {
        // Never over logs that couldn't be opened: they stay on disk until they open again or are given up.
        if (_unreadable.value) return
        val next = synchronized(this) {
            val n = transform(_data.value)
            _data.value = n
            n
        }
        persist(next)
        runCatching { onChanged(next) }
    }

    /** Replaces everything (restore from a backup, or "forgot PIN"). */
    fun replaceAll(data: AppData) = update { data }

    private fun persist(snapshot: AppData) {
        scope.launch(Dispatchers.IO) {
            writeLock.withLock {
                // Only the newest state matters; an older snapshot queued behind a newer one is skipped.
                if (snapshot !== _data.value) return@withLock
                if (_unreadable.value) return@withLock
                runCatching {
                    val plain = dataJson.encodeToString(AppData.serializer(), snapshot).toByteArray(Charsets.UTF_8)
                    // Encrypted when there's a data key; the ordinary file otherwise, as before.
                    val sealed = keys.get(create = true)?.let { SealedFile.sealChecked(it, plain) }
                    SealedFile.writeAtomic(file, File(file.parentFile, TMP), sealed ?: plain)
                }
            }
        }
    }

    /** The saved logs read straight from disk (reminders run without the UI), or null when they can't be opened. */
    fun readSaved(): AppData? = load(file, keys, migrate = false)

    /** Tries to open the saved logs again, after [unreadable]. */
    fun retry() {
        scope.launch(Dispatchers.IO) {
            keys.reset()
            open()
        }
    }

    /**
     * Gives up on saved logs that can't be opened and starts empty, so a backup can be restored. With [keep]
     * the file is set aside as `data.json.unreadable` (and the key file as `data.key.unreadable`) instead of
     * deleted. Does nothing unless the logs are [unreadable].
     */
    suspend fun startEmpty(keep: Boolean) = withContext(Dispatchers.IO) {
        writeLock.withLock {
            if (!_unreadable.value) return@withLock
            runCatching {
                if (keep) setAside(file) else file.delete()
                File(file.parentFile, TMP).delete()
            }
            // A key that still works stays (the photos are sealed with it); one that's gone makes way for a new one.
            if (keys.get(create = false) == null) keys.discard(keep)
            _data.value = AppData()
            _unreadable.value = false
        }
    }

    companion object {
        private const val FILE = "data.json"
        private const val TMP = "data.json.tmp"
        private const val ASIDE = "data.json.unreadable"

        private fun decode(bytes: ByteArray): AppData = dataJson.decodeFromString(AppData.serializer(), String(bytes, Charsets.UTF_8))

        /** Moves an unreadable file out of the way without deleting it (or an earlier one set aside). */
        private fun setAside(file: File) {
            var aside = File(file.parentFile, ASIDE)
            if (aside.exists()) aside = File(file.parentFile, "$ASIDE.${System.currentTimeMillis()}")
            file.renameTo(aside)
        }

        /** The contents of a sealed or ordinary store file, or null when it's sealed and can't be opened. */
        private fun plainOf(bytes: ByteArray, keys: DataKeyStore): ByteArray? {
            if (!SealedFile.isSealed(bytes)) return bytes
            val key = keys.get(create = false) ?: return null
            return try {
                SealedFile.open(key, bytes)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * When `data.json` is missing, puts back a complete copy that was left next to it:
         * - `data.json.tmp`: the newest save, when the app died after writing it and before it took the
         *   file's place;
         * - `data.json.unreadable`, only when it's an encrypted file that opens: a version of RideCtrl from
         *   before encryption sets the encrypted file aside under that name because it can't read it.
         * Only a copy that opens and reads as logs is put back; anything else is left where it is.
         */
        private fun recoverMissing(file: File, keys: DataKeyStore) {
            val tmp = File(file.parentFile, TMP)
            val aside = File(file.parentFile, ASIDE)
            for (candidate in listOf(tmp, aside)) {
                try {
                    if (!candidate.exists()) continue
                    val bytes = candidate.readBytes()
                    if (candidate == aside && !SealedFile.isSealed(bytes)) continue
                    val plain = plainOf(bytes, keys) ?: continue
                    decode(plain)
                    if (candidate.renameTo(file)) return
                } catch (e: Exception) {
                    // Not a usable copy.
                }
            }
        }

        /**
         * Reads the store. Null means the file is encrypted and can't be opened; it is left untouched. An
         * ordinary or opened file that doesn't read as logs is kept aside instead of being overwritten, and
         * the store starts empty, as before. With [migrate] (the app's one load at start) an ordinary file
         * that reads fine is then replaced by its encrypted copy; if that fails for any reason the ordinary
         * file simply stays in use.
         */
        fun load(file: File, keys: DataKeyStore, migrate: Boolean): AppData? {
            if (migrate && !file.exists()) recoverMissing(file, keys)
            if (!file.exists()) return AppData()
            val bytes = try {
                file.readBytes()
            } catch (e: Exception) {
                if (migrate) runCatching { setAside(file) }
                return AppData()
            }
            val sealed = SealedFile.isSealed(bytes)
            val plain = plainOf(bytes, keys) ?: return null
            val data = try {
                decode(plain)
            } catch (e: Exception) {
                if (migrate) runCatching { setAside(file) }
                return AppData()
            }
            if (migrate && !sealed) {
                try {
                    val key = keys.get(create = true)
                    if (key != null) SealedFile.sealInPlace(key, file, File(file.parentFile, TMP))
                } catch (e: Throwable) {
                    // The ordinary file stays in use.
                }
            }
            return data
        }
    }
}
