package com.vknxworks.ridectrl.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 */
class LogRepository(context: Context, private val scope: CoroutineScope) {
    private val file = File(context.filesDir, "data.json")
    private val writeLock = Mutex()

    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    /** Called after every change (reminders and the widget refresh from here). */
    var onChanged: (AppData) -> Unit = {}

    init {
        scope.launch(Dispatchers.IO) {
            _data.value = load(file)
            _ready.value = true
        }
    }

    fun update(transform: (AppData) -> AppData) {
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
                runCatching {
                    val tmp = File(file.parentFile, "data.json.tmp")
                    tmp.writeText(dataJson.encodeToString(AppData.serializer(), snapshot))
                    if (!tmp.renameTo(file)) {
                        file.delete()
                        tmp.renameTo(file)
                    }
                }
            }
        }
    }

    companion object {
        /** Reads the store; an unreadable file is kept aside instead of being overwritten. */
        fun load(file: File): AppData = runCatching {
            if (file.exists()) dataJson.decodeFromString(AppData.serializer(), file.readText()) else AppData()
        }.getOrElse {
            runCatching { file.renameTo(File(file.parentFile, "data.json.unreadable")) }
            AppData()
        }

        /** Reads the saved data without the app running (reminders, widget). */
        fun read(context: Context): AppData = load(File(context.filesDir, "data.json"))
    }
}
