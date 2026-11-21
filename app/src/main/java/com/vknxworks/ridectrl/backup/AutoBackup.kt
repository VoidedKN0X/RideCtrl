package com.vknxworks.ridectrl.backup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.vknxworks.ridectrl.container
import com.vknxworks.ridectrl.data.BackupFile
import com.vknxworks.ridectrl.data.crypto.SealedFile
import com.vknxworks.ridectrl.data.settings.AutoBackupEvery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Scheduled local backups: once a day or once a week, RideCtrl writes the same backup file as "Back up to a file"
 * (every log and photo; with a password, the encrypted kind) into a folder you picked with Android's folder
 * picker, and keeps only the newest few of its own automatic backups there. Fully offline: an inexact alarm
 * (no exact-alarm permission) checks once a night whether one is due.
 *
 * The password for encrypted automatic backups is kept on the phone sealed with the app's data key
 * (`backup.pw`, like the logs), never in the settings.
 */
object AutoBackup {
    private const val ACTION_RUN = "com.vknxworks.ridectrl.AUTO_BACKUP"
    private const val PREFS = "auto_backup"
    private const val KEY_LAST = "last_ok"
    private const val KEY_LAST_TRY = "last_try"
    private const val KEY_LAST_ERROR = "last_error"
    private const val PASSWORD_FILE = "backup.pw"
    private const val RUN_HOUR = 3
    /** Our own file names: "RideCtrl-auto-2026-11-21-0330.zip" or ".ridectrl". Only these are ever removed. */
    const val PREFIX = "RideCtrl-auto-"
    private val NAME = Regex("""RideCtrl-auto-\d{4}-\d{2}-\d{2}-\d{4}\.(zip|ridectrl)""")
    private val STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")

    enum class Problem { NO_FOLDER, FOLDER_GONE, LOGS_UNREADABLE, NO_PASSWORD, WRITE_FAILED }

    /** When the last automatic backup was made (epoch millis), and what went wrong with the last try, if anything. */
    data class Status(val lastOk: Long?, val lastTry: Long?, val problem: Problem?)

    private val mutex = Mutex()

    fun status(context: Context): Status {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Status(
            p.getLong(KEY_LAST, 0L).takeIf { it > 0 },
            p.getLong(KEY_LAST_TRY, 0L).takeIf { it > 0 },
            p.getString(KEY_LAST_ERROR, null)?.let { n -> Problem.entries.firstOrNull { it.name == n } },
        )
    }

    private fun runIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        2,
        Intent(context, AutoBackupReceiver::class.java).setAction(ACTION_RUN),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Sets the next nightly check (around 03:00), or cancels it when automatic backups are off. */
    suspend fun schedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = runIntent(context)
        val s = context.container.settings.current()
        if (s.autoBackup == AutoBackupEvery.OFF || s.autoBackupFolder == null) {
            am.cancel(pi)
            return
        }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(RUN_HOUR, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), pi)
    }

    /** Runs a backup if one is due ([force]: now, whatever the schedule). Returns the problem, or null when it worked or wasn't due. */
    suspend fun runIfDue(context: Context, force: Boolean = false): Problem? = mutex.withLock {
        val c = context.container
        val s = c.settings.current()
        if (!force && s.autoBackup == AutoBackupEvery.OFF) return@withLock null
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST, 0L)
        // An hour of slack, so a nightly alarm that comes a little early still counts.
        val due = last == 0L || System.currentTimeMillis() - last >= s.autoBackup.days * 86_400_000L - 3_600_000L
        if (!force && !due) return@withLock null
        val problem = write(context, s)
        prefs.edit().apply {
            putLong(KEY_LAST_TRY, System.currentTimeMillis())
            if (problem == null) {
                putLong(KEY_LAST, System.currentTimeMillis())
                remove(KEY_LAST_ERROR)
            } else {
                putString(KEY_LAST_ERROR, problem.name)
            }
        }.apply()
        problem
    }

    private fun write(context: Context, settings: com.vknxworks.ridectrl.data.settings.AppSettings): Problem? {
        val c = context.container
        val keep = settings.autoBackupKeep
        val tree = settings.autoBackupFolder?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return Problem.NO_FOLDER
        // Never back up an empty store in place of logs that couldn't be opened.
        val data = c.logs.readSaved() ?: return Problem.LOGS_UNREADABLE
        val password = if (hasPassword(context)) (readPassword(context) ?: return Problem.NO_PASSWORD) else null
        val resolver = context.contentResolver
        val parent = runCatching {
            DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        }.getOrNull() ?: return Problem.FOLDER_GONE
        val name = PREFIX + LocalDateTime.now().format(STAMP) + if (password != null) ".ridectrl" else ".zip"
        val doc = try {
            DocumentsContract.createDocument(resolver, parent, "application/octet-stream", name)
        } catch (e: Exception) {
            null
        }
        if (doc == null) {
            password?.fill('\u0000')
            return Problem.FOLDER_GONE
        }
        val ok = try {
            resolver.openOutputStream(doc, "wt")?.use { out ->
                BackupFile.write(out, data, settings, { n -> c.photos.read(n) }, password)
            } != null
        } catch (e: Exception) {
            false
        } finally {
            password?.fill('\u0000')
        }
        if (!ok) {
            // A half-written file is no backup: it goes, so it can't be mistaken for one.
            runCatching { DocumentsContract.deleteDocument(resolver, doc) }
            return Problem.WRITE_FAILED
        }
        prune(context, tree, keep)
        return null
    }

    /** Removes RideCtrl's own older automatic backups in the folder beyond the newest [keep]. Nothing else is touched. */
    private fun prune(context: Context, tree: Uri, keep: Int) {
        val resolver = context.contentResolver
        val children = runCatching {
            DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        }.getOrNull() ?: return
        val ours = mutableListOf<Pair<String, String>>()
        runCatching {
            resolver.query(
                children,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cur ->
                while (cur.moveToNext()) {
                    val id = cur.getString(0) ?: continue
                    val n = cur.getString(1) ?: continue
                    if (NAME.matches(n)) ours += id to n
                }
            }
        }
        // The names sort by date and time.
        ours.sortedByDescending { it.second }.drop(keep.coerceAtLeast(1)).forEach { (id, _) ->
            runCatching { DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, id)) }
        }
    }

    // ---------------------------------------------------------------- password

    private fun passwordFile(context: Context) = File(context.filesDir, PASSWORD_FILE)

    fun hasPassword(context: Context): Boolean = passwordFile(context).exists()

    /** Saves the password for encrypted automatic backups, sealed with the data key. False when there's no data key. */
    fun setPassword(context: Context, password: CharArray): Boolean {
        val key = context.container.dataKey.get(create = true) ?: return false
        val plain = String(password).toByteArray(Charsets.UTF_8)
        return try {
            val sealed = SealedFile.sealChecked(key, plain) ?: return false
            SealedFile.writeAtomic(passwordFile(context), File(context.filesDir, "$PASSWORD_FILE.tmp"), sealed)
            true
        } catch (e: Exception) {
            false
        } finally {
            plain.fill(0)
        }
    }

    fun clearPassword(context: Context) {
        runCatching { passwordFile(context).delete() }
    }

    private fun readPassword(context: Context): CharArray? = try {
        val key = context.container.dataKey.get(create = false)
        val bytes = passwordFile(context).readBytes()
        val plain = if (SealedFile.isSealed(bytes)) key?.let { SealedFile.open(it, bytes) } else null
        plain?.let { p -> String(p, Charsets.UTF_8).toCharArray().also { p.fill(0) } }
    } catch (e: Exception) {
        null
    }
}

/** The nightly check, and re-arming it. */
class AutoBackupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { AutoBackup.runIfDue(context.applicationContext) }
                runCatching { AutoBackup.schedule(context.applicationContext) }
            } finally {
                pending.finish()
            }
        }
    }
}
