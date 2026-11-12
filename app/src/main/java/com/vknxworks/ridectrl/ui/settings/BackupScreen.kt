package com.vknxworks.ridectrl.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.container
import com.vknxworks.ridectrl.data.BackupException
import com.vknxworks.ridectrl.data.BackupFile
import com.vknxworks.ridectrl.data.CsvExport
import com.vknxworks.ridectrl.data.ReadBackup
import com.vknxworks.ridectrl.ui.components.ConfirmDialog
import com.vknxworks.ridectrl.ui.components.CtrlAlertDialog
import com.vknxworks.ridectrl.ui.components.Group
import com.vknxworks.ridectrl.ui.components.GroupRow
import com.vknxworks.ridectrl.ui.components.ScreenTopBar
import com.vknxworks.ridectrl.ui.components.ctrlFieldColors
import com.vknxworks.ridectrl.ui.lock.AppLock
import com.vknxworks.ridectrl.ui.theme.CtrlColors
import com.vknxworks.ridectrl.ui.theme.CtrlShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

private const val MIN_PASSWORD = 8

/**
 * Backup and export, all through Android's file picker: one file with every log and photo (optionally
 * encrypted with a password), restore from such a file, and CSV files for spreadsheets.
 */
@Composable
fun BackupScreen() {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    val data by c.logs.data.collectAsStateWithLifecycle()
    var busy by remember { mutableStateOf(false) }
    var askEncrypt by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf<CharArray?>(null) }
    var restoreBytes by remember { mutableStateOf<ByteArray?>(null) }
    var askRestorePassword by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<ReadBackup?>(null) }
    var problem by remember { mutableStateOf<Int?>(null) }
    var csvLog by remember { mutableStateOf(CsvExport.Log.FUEL) }

    fun toast(res: Int) = Toast.makeText(context, context.getString(res), Toast.LENGTH_SHORT).show()

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val pw = password
        password = null
        if (uri == null) { pw?.fill('\u0000'); return@rememberLauncherForActivityResult }
        busy = true
        scope.launch {
            val settings = c.settings.stored.first()
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                        BackupFile.write(out, c.logs.data.value, settings, c.photos.dir, pw)
                    } ?: error("no file")
                }.isSuccess
            }
            pw?.fill('\u0000')
            busy = false
            toast(if (ok) R.string.backup_saved else R.string.backup_save_failed)
        }
    }

    fun read(bytes: ByteArray, pw: CharArray?) {
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.Default) { runCatching { BackupFile.read(bytes, pw) } }
            pw?.fill('\u0000')
            busy = false
            result.onSuccess { pending = it; restoreBytes = null }.onFailure { e ->
                val reason = (e as? BackupException)?.reason
                if (reason == BackupException.Reason.NEEDS_PASSWORD) {
                    askRestorePassword = true
                } else {
                    restoreBytes = null
                    problem = when (reason) {
                        BackupException.Reason.WRONG_PASSWORD -> R.string.restore_wrong_password
                        BackupException.Reason.NEWER -> R.string.restore_newer
                        BackupException.Reason.DAMAGED -> R.string.restore_damaged
                        BackupException.Reason.TOO_BIG -> R.string.restore_too_big
                        else -> R.string.restore_not_backup
                    }
                }
            }
        }
    }

    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { BackupFile.readAll(it) } }.getOrNull()
            }
            busy = false
            if (bytes == null) {
                problem = R.string.restore_not_backup
            } else {
                restoreBytes = bytes
                read(bytes, null)
            }
        }
    }

    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val log = csvLog
        scope.launch {
            val settings = c.settings.stored.first()
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val text = CsvExport.build(c.logs.data.value, log, settings.currency)
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } ?: error("no file")
                }.isSuccess
            }
            toast(if (ok) R.string.csv_saved else R.string.backup_save_failed)
        }
    }

    fun startBackup(encrypted: Boolean) {
        AppLock.expectReturn()
        val date = LocalDate.now().toString()
        save.launch(if (encrypted) "RideCtrl-$date.ridectrl" else "RideCtrl-$date.zip")
    }

    fun exportCsv(log: CsvExport.Log, name: String) {
        csvLog = log
        AppLock.expectReturn()
        csv.launch("RideCtrl-$name-${LocalDate.now()}.csv")
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.backup_title)) {
            if (busy) CircularProgressIndicator(color = CtrlColors.AccentText, modifier = Modifier.padding(end = 8.dp))
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
            Group(stringResource(R.string.backup_group), footer = stringResource(R.string.backup_footer)) {
                GroupRow(
                    stringResource(R.string.backup_to_file),
                    stringResource(R.string.backup_to_file_sub, data.vehicles.size, data.photoNames().size),
                    Icons.Outlined.Backup,
                    showChevron = true,
                    onClick = { if (!busy) startBackup(false) },
                )
                GroupRow(
                    stringResource(R.string.backup_encrypted),
                    stringResource(R.string.backup_encrypted_sub),
                    Icons.Outlined.Lock,
                    showChevron = true,
                    onClick = { if (!busy) askEncrypt = true },
                )
                GroupRow(
                    stringResource(R.string.backup_restore),
                    stringResource(R.string.backup_restore_sub),
                    Icons.Outlined.Restore,
                    showChevron = true,
                    onClick = {
                        if (!busy) {
                            AppLock.expectReturn()
                            open.launch(arrayOf("*/*"))
                        }
                    },
                )
            }
            Group(stringResource(R.string.csv_group), footer = stringResource(R.string.csv_footer)) {
                GroupRow(stringResource(R.string.csv_fuel), null, Icons.Outlined.LocalGasStation, showChevron = true, onClick = { exportCsv(CsvExport.Log.FUEL, "fuel") })
                GroupRow(stringResource(R.string.csv_services), null, Icons.Outlined.Build, showChevron = true, onClick = { exportCsv(CsvExport.Log.SERVICES, "services") })
                GroupRow(stringResource(R.string.csv_parts), null, Icons.Outlined.Inventory2, showChevron = true, onClick = { exportCsv(CsvExport.Log.PARTS, "parts") })
                GroupRow(stringResource(R.string.csv_temps), null, Icons.Outlined.Thermostat, showChevron = true, onClick = { exportCsv(CsvExport.Log.TEMPERATURES, "temperatures") })
            }
        }
    }

    if (askEncrypt) {
        PasswordDialog(
            title = stringResource(R.string.backup_password_title),
            body = stringResource(R.string.backup_password_body, MIN_PASSWORD),
            confirmTwice = true,
            onDismiss = { askEncrypt = false },
        ) { pw ->
            askEncrypt = false
            password = pw
            startBackup(true)
        }
    }

    if (askRestorePassword) {
        PasswordDialog(
            title = stringResource(R.string.restore_password_title),
            body = stringResource(R.string.restore_password_body),
            confirmTwice = false,
            onDismiss = { askRestorePassword = false; restoreBytes = null },
        ) { pw ->
            askRestorePassword = false
            restoreBytes?.let { read(it, pw) }
        }
    }

    pending?.let { backup ->
        val m = backup.manifest
        val when_ = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(m.created))
        ConfirmDialog(
            title = stringResource(R.string.restore_confirm_title),
            body = stringResource(
                R.string.restore_confirm_body,
                when_,
                m.data.vehicles.size,
                m.data.fuel.size + m.data.services.size + m.data.mods.size + m.data.temps.size + m.data.odometer.size,
                backup.photos.size,
            ),
            confirm = stringResource(R.string.restore_confirm),
            onDismiss = { pending = null },
        ) {
            pending = null
            busy = true
            scope.launch {
                withContext(Dispatchers.IO) {
                    // Photos first, then the data that refers to them; old photos go afterwards.
                    backup.photos.forEach { (name, bytes) -> File(c.photos.dir, name).writeBytes(bytes) }
                    c.logs.replaceAll(m.data)
                    val keep = m.data.photoNames()
                    c.photos.dir.listFiles()?.forEach { if (it.name !in keep) it.delete() }
                }
                c.settings.update { m.applyTo(it) }
                busy = false
                toast(R.string.restore_done)
            }
        }
    }

    problem?.let { reason ->
        CtrlAlertDialog(
            onDismissRequest = { problem = null },
            title = { Text(stringResource(R.string.restore_failed_title)) },
            text = { Text(stringResource(reason)) },
            confirmButton = { TextButton(onClick = { problem = null }) { Text(stringResource(R.string.lock_ok)) } },
        )
    }
}

/** Asks for a password (twice when setting one). The result is a char array the caller wipes after use. */
@Composable
private fun PasswordDialog(title: String, body: String, confirmTwice: Boolean, onDismiss: () -> Unit, onDone: (CharArray) -> Unit) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    val ok = if (confirmTwice) first.length >= MIN_PASSWORD && first == second else first.isNotEmpty()
    CtrlAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(body)
                PasswordField(stringResource(R.string.password), first) { first = it }
                if (confirmTwice) {
                    PasswordField(stringResource(R.string.password_again), second) { second = it }
                    if (second.isNotEmpty() && first != second) Text(stringResource(R.string.password_mismatch), color = CtrlColors.Error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val pw = first.toCharArray()
                first = ""
                second = ""
                onDone(pw)
            }, enabled = ok) { Text(stringResource(R.string.lock_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        guard = true,
    )
}

@Composable
private fun PasswordField(label: String, value: String, onChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = CtrlShapes.Field,
        colors = ctrlFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

