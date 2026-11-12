package com.vknxworks.ridectrl.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.container
import com.vknxworks.ridectrl.data.DueState
import com.vknxworks.ridectrl.data.Fmt
import com.vknxworks.ridectrl.data.Part
import com.vknxworks.ridectrl.data.PartUse
import com.vknxworks.ridectrl.data.parseDate
import com.vknxworks.ridectrl.ui.lock.AppLock
import com.vknxworks.ridectrl.ui.theme.CtrlColors
import com.vknxworks.ridectrl.ui.theme.CtrlShapes
import com.vknxworks.ridectrl.ui.theme.CtrlType
import com.vknxworks.ridectrl.ui.theme.Gutter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** The user's units, money and date formatting, following Settings. */
@Composable
fun rememberFmt(): Fmt {
    val settings by LocalContext.current.container.settings.settings.collectAsStateWithLifecycle()
    return remember(settings) { Fmt(settings) }
}

// ---------- Form screens ----------

/**
 * A full-screen editor: top bar (with Delete for an existing entry), scrolling fields, and the Save button
 * at the bottom above the keyboard.
 */
@Composable
fun FormScreen(
    title: String,
    canSave: Boolean,
    onSave: () -> Unit,
    onDelete: (() -> Unit)? = null,
    deleteTitle: String = stringResource(R.string.delete_entry_title),
    deleteBody: String = stringResource(R.string.delete_entry_body),
    content: @Composable ColumnScope.() -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenTopBar(title) {
            if (onDelete != null) {
                CtrlSquareButton(Icons.Outlined.Delete, stringResource(R.string.delete), onClick = { confirmDelete = true }, tint = CtrlColors.Error)
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Gutter)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
        CtrlButton(
            stringResource(R.string.save),
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Gutter, vertical = 12.dp),
        )
    }
    if (confirmDelete && onDelete != null) {
        ConfirmDialog(deleteTitle, deleteBody, stringResource(R.string.delete), onDismiss = { confirmDelete = false }) {
            confirmDelete = false
            onDelete()
        }
    }
}

/** A small mono heading between groups of fields. */
@Composable
fun FieldHeading(text: String) {
    SectionLabel(text, Modifier.padding(start = 4.dp, top = 8.dp))
}

/** A note under a group of fields. */
@Composable
fun FieldNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = CtrlColors.Muted, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
fun TextInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    words: Boolean = false,
    sentences: Boolean = false,
    suffix: String? = null,
    placeholder: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    CtrlTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder) }
        } else {
            null
        },
        trailingIcon = if (suffix != null) {
            { Text(suffix, style = CtrlType.Data, color = CtrlColors.Muted, modifier = Modifier.padding(end = 12.dp)) }
        } else {
            null
        },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboard,
            capitalization = when {
                words -> KeyboardCapitalization.Words
                sentences -> KeyboardCapitalization.Sentences
                else -> KeyboardCapitalization.None
            },
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** A number field: accepts digits with a comma or a period, nothing else. */
@Composable
fun NumberInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    decimals: Boolean = true,
    placeholder: String? = null,
) {
    TextInput(
        label,
        value,
        onChange = { t ->
            val ok = if (decimals) t.all { it.isDigit() || it == ',' || it == '.' || it == '-' } else t.all { it.isDigit() }
            if (ok && t.length <= 14) onChange(t)
        },
        modifier = modifier,
        suffix = suffix,
        placeholder = placeholder,
        keyboard = if (decimals) KeyboardType.Decimal else KeyboardType.Number,
    )
}

/** A tappable field row in the field style (date pickers, choices). */
@Composable
fun PickerField(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClear: (() -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(CtrlShapes.Field)
            .background(CtrlColors.Surface)
            .border(1.dp, CtrlColors.ControlOutline, CtrlShapes.Field)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = CtrlColors.Muted)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = CtrlColors.Text)
        }
        if (onClear != null) {
            CtrlSquareButton(Icons.Outlined.Close, stringResource(R.string.clear), onClick = onClear, size = 36.dp, tint = CtrlColors.Muted)
        } else if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp), tint = CtrlColors.Muted)
        }
    }
}

private fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.pickerToDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** A date field (stored as yyyy-MM-dd). With [optional], it can be cleared. */
@Composable
fun DateInput(label: String, iso: String?, onChange: (String?) -> Unit, optional: Boolean = false) {
    val fmt = rememberFmt()
    var open by remember { mutableStateOf(false) }
    PickerField(
        label,
        if (iso == null) stringResource(R.string.not_set) else fmt.date(iso),
        onClick = { open = true },
        icon = Icons.Outlined.CalendarMonth,
        onClear = if (optional && iso != null) ({ onChange(null) }) else null,
    )
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (parseDate(iso) ?: LocalDate.now()).toPickerMillis())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(it.pickerToDate().toString()) }
                    open = false
                }) { Text(stringResource(R.string.lock_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state) }
    }
}

/** A date and time field (epoch millis). */
@Composable
fun DateTimeInput(label: String, millis: Long, onChange: (Long) -> Unit) {
    val fmt = rememberFmt()
    val zone = ZoneId.systemDefault()
    val current = Instant.ofEpochMilli(millis).atZone(zone)
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    PickerField(label, fmt.dateTime(millis), onClick = { pickDate = true }, icon = Icons.Outlined.CalendarMonth)
    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = current.toLocalDate().toPickerMillis())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { d ->
                        onChange(d.pickerToDate().atTime(current.toLocalTime()).atZone(zone).toInstant().toEpochMilli())
                    }
                    pickDate = false
                    pickTime = true
                }) { Text(stringResource(R.string.lock_next)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state) }
    }
    if (pickTime) {
        val state = rememberTimePickerState(initialHour = current.hour, initialMinute = current.minute, is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current))
        CtrlAlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = {
                TextButton(onClick = {
                    val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                    onChange(date.atTime(LocalTime.of(state.hour, state.minute)).atZone(zone).toInstant().toEpochMilli())
                    pickTime = false
                }) { Text(stringResource(R.string.lock_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.cancel)) } },
            text = { TimePicker(state) },
        )
    }
}

/** A choice field that opens a sheet with the options. */
@Composable
fun <T> ChoiceInput(label: String, options: List<Pair<T, String>>, selected: T, onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    PickerField(label, options.firstOrNull { it.first == selected }?.second ?: "", onClick = { open = true })
    if (open) ChoiceSheet(label, options, selected, onDismiss = { open = false }) { onPick(it); open = false }
}

/** A switch row in a card, for forms. */
@Composable
fun SwitchInput(label: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    CtrlCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        GroupSwitch(label, subtitle, checked = checked, onChange = onChange)
    }
}

/** Pick any number of options (service items done in a record, vehicles a part fits). */
@Composable
fun MultiSelectInput(label: String, options: List<Pair<String, String>>, selected: Set<String>, emptyText: String, onChange: (Set<String>) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val names = options.filter { it.first in selected }.joinToString(", ") { it.second }
    PickerField(label, names.ifBlank { emptyText }, onClick = { open = true })
    if (open) {
        var picked by remember { mutableStateOf(selected) }
        CtrlBottomSheet(onDismissRequest = { onChange(picked); open = false }) {
            Text(label, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            if (options.isEmpty()) {
                Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = CtrlColors.Muted, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            }
            Column(Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false)) {
                options.forEach { (id, name) ->
                    val on = id in picked
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable { picked = if (on) picked - id else picked + id }
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (on) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                            null,
                            Modifier.size(22.dp),
                            tint = if (on) CtrlColors.AccentText else CtrlColors.Muted,
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    }
                }
            }
            CtrlButton(
                stringResource(R.string.lock_done),
                onClick = { onChange(picked); open = false },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}

/** Parts used from the inventory, with a quantity each. */
@Composable
fun PartUsesInput(label: String, parts: List<Part>, uses: List<PartUse>, onChange: (List<PartUse>) -> Unit) {
    val fmt = rememberFmt()
    var open by remember { mutableStateOf(false) }
    val byId = parts.associateBy { it.id }
    val summary = uses.mapNotNull { u -> byId[u.partId]?.let { "${it.name} ×${u.quantity}" } }.joinToString(", ")
    PickerField(label, summary.ifBlank { stringResource(R.string.parts_none_used) }, onClick = { open = true })
    if (open) {
        var picked by remember { mutableStateOf(uses.associate { it.partId to it.quantity }) }
        fun done() {
            onChange(picked.filter { it.value > 0 }.map { PartUse(it.key, it.value) })
            open = false
        }
        CtrlBottomSheet(onDismissRequest = { done() }) {
            Text(label, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            if (parts.isEmpty()) {
                Text(stringResource(R.string.parts_inventory_empty), style = MaterialTheme.typography.bodyMedium, color = CtrlColors.Muted, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            }
            Column(Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false)) {
                parts.forEach { p ->
                    val q = picked[p.id] ?: 0
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 24.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                listOfNotNull(p.brand.ifBlank { null }, p.price?.let { fmt.money(it) }, stringResource(R.string.parts_in_stock, p.quantity)).joinToString(" · "),
                                style = CtrlType.Data,
                                color = CtrlColors.Muted,
                            )
                        }
                        CtrlSquareButton(Icons.Outlined.Remove, stringResource(R.string.less), onClick = { picked = picked + (p.id to (q - 1).coerceAtLeast(0)) }, size = 36.dp, enabled = q > 0)
                        Text("$q", style = CtrlType.Data, modifier = Modifier.width(32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.more), onClick = { picked = picked + (p.id to q + 1) }, size = 36.dp)
                    }
                }
            }
            CtrlButton(
                stringResource(R.string.lock_done),
                onClick = { done() },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}

// ---------- Photos ----------

/** A photo from RideCtrl's private photo folder, decoded off the main thread. */
@Composable
fun PhotoThumb(name: String, modifier: Modifier = Modifier, maxSide: Int = 512, onClick: (() -> Unit)? = null) {
    val photos = LocalContext.current.container.photos
    val bitmap by produceState<Bitmap?>(null, name, maxSide) {
        value = withContext(Dispatchers.IO) { photos.thumbnail(name, maxSide) }
    }
    Box(
        modifier
            .clip(CtrlShapes.Control)
            .background(CtrlColors.Raised)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

/** A photo shown full screen, with Delete when [onDelete] is given. */
@Composable
fun PhotoViewer(name: String, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
            val photos = LocalContext.current.container.photos
            val bitmap by produceState<Bitmap?>(null, name) {
                value = withContext(Dispatchers.IO) { photos.thumbnail(name, 2560) }
            }
            bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
            Row(
                Modifier.align(Alignment.TopEnd).padding(16.dp).padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onDelete != null) CtrlSquareButton(Icons.Outlined.Delete, stringResource(R.string.delete), onClick = onDelete, tint = CtrlColors.Error)
                CtrlSquareButton(Icons.Outlined.Close, stringResource(R.string.close), onClick = onDismiss)
            }
        }
    }
}

/**
 * Photos attached to an entry: thumbnails, then buttons to pick from the gallery or take one with the
 * camera app. Uses Android's photo picker and camera intents, so RideCtrl needs no permission for either.
 */
@Composable
fun PhotoStrip(label: String, photos: List<String>, onChange: (List<String>) -> Unit, single: Boolean = false) {
    val context = LocalContext.current
    val store = context.container.photos
    val scope = rememberCoroutineScope()
    var viewing by remember { mutableStateOf<String?>(null) }
    var captureUri by remember { mutableStateOf<Uri?>(null) }

    fun add(uri: Uri) {
        scope.launch {
            val name = withContext(Dispatchers.IO) { store.import(uri) }
            if (name != null) onChange(if (single) listOf(name) else photos + name)
        }
    }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::add) }
    val take = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = captureUri
        if (ok && uri != null) add(uri)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(label, Modifier.padding(start = 4.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            photos.forEach { name -> PhotoThumb(name, Modifier.size(84.dp), onClick = { viewing = name }) }
            PhotoButton(Icons.Outlined.AddPhotoAlternate, stringResource(R.string.photo_pick)) {
                AppLock.expectReturn()
                pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            PhotoButton(Icons.Outlined.PhotoCamera, stringResource(R.string.photo_take)) {
                val uri = store.newCaptureUri()
                captureUri = uri
                AppLock.expectReturn()
                runCatching { take.launch(uri) }
            }
        }
    }
    viewing?.let { name ->
        PhotoViewer(name, onDismiss = { viewing = null }, onDelete = {
            onChange(photos - name)
            viewing = null
        })
    }
}

@Composable
private fun PhotoButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .size(84.dp)
            .clip(CtrlShapes.Control)
            .background(CtrlColors.Surface)
            .border(1.dp, CtrlColors.ControlOutline, CtrlShapes.Control)
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = CtrlColors.Text)
        Spacer(Modifier.size(4.dp))
        Text(label, style = CtrlType.Status, color = CtrlColors.Muted, maxLines = 1)
    }
}

// ---------- Lists and status ----------

/** OK / soon / overdue as a mono tag with its colour square. */
@Composable
fun DueTag(state: DueState) {
    when (state) {
        DueState.OK -> StatusTag(stringResource(R.string.state_ok), active = true)
        DueState.SOON -> StatusTag(stringResource(R.string.state_soon), active = true, color = CtrlColors.Warning)
        DueState.OVERDUE -> StatusTag(stringResource(R.string.state_overdue), active = true, color = CtrlColors.Error)
        DueState.UNKNOWN -> StatusTag(stringResource(R.string.state_unknown), active = false)
    }
}

/** A row in a log list: title and details on the left, a value on the right. */
@Composable
fun EntryRow(title: String, subtitle: String?, value: String?, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    GroupRow(title, subtitle, value = value, onClick = onClick, trailing = trailing, showChevron = trailing == null && value == null)
}

/** A small stat: mono label above a value. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null) {
    CtrlCard(modifier, padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        SectionLabel(label)
        Spacer(Modifier.size(6.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = CtrlColors.Text, maxLines = 1)
        if (sub != null) Text(sub, style = CtrlType.Data, color = CtrlColors.Muted, maxLines = 1)
    }
}

/** Two stat tiles side by side. */
@Composable
fun StatRow(a: @Composable (Modifier) -> Unit, b: @Composable (Modifier) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        a(Modifier.weight(1f))
        b(Modifier.weight(1f))
    }
}
