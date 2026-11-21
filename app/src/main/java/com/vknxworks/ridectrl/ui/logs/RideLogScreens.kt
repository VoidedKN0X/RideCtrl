package com.vknxworks.ridectrl.ui.logs

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.TireRepair
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.container
import com.vknxworks.ridectrl.data.ChecklistItem
import com.vknxworks.ridectrl.data.EngineType
import com.vknxworks.ridectrl.data.JettingEntry
import com.vknxworks.ridectrl.data.PlugColour
import com.vknxworks.ridectrl.data.PreRideCheck
import com.vknxworks.ridectrl.data.TempSummary
import com.vknxworks.ridectrl.data.TyreEntry
import com.vknxworks.ridectrl.data.TyreEvent
import com.vknxworks.ridectrl.data.TyrePosition
import com.vknxworks.ridectrl.data.TyreStatus
import com.vknxworks.ridectrl.data.checklistFor
import com.vknxworks.ridectrl.data.currentKm
import com.vknxworks.ridectrl.data.dotAgeMonths
import com.vknxworks.ridectrl.data.jettingPeriods
import com.vknxworks.ridectrl.data.newId
import com.vknxworks.ridectrl.data.parseInt
import com.vknxworks.ridectrl.data.parseNumber
import com.vknxworks.ridectrl.data.today
import com.vknxworks.ridectrl.data.tyreStatus
import com.vknxworks.ridectrl.ui.components.ChoiceInput
import com.vknxworks.ridectrl.ui.components.ConfirmDialog
import com.vknxworks.ridectrl.ui.components.CtrlButton
import com.vknxworks.ridectrl.ui.components.CtrlCard
import com.vknxworks.ridectrl.ui.components.CtrlSecondaryButton
import com.vknxworks.ridectrl.ui.components.CtrlSquareButton
import com.vknxworks.ridectrl.ui.components.DateInput
import com.vknxworks.ridectrl.ui.components.EntryRow
import com.vknxworks.ridectrl.ui.components.FieldHeading
import com.vknxworks.ridectrl.ui.components.FieldNote
import com.vknxworks.ridectrl.ui.components.FormScreen
import com.vknxworks.ridectrl.ui.components.Group
import com.vknxworks.ridectrl.ui.components.GroupRow
import com.vknxworks.ridectrl.ui.components.MessageView
import com.vknxworks.ridectrl.ui.components.NameDialog
import com.vknxworks.ridectrl.ui.components.NumberInput
import com.vknxworks.ridectrl.ui.components.ScreenTopBar
import com.vknxworks.ridectrl.ui.components.SectionLabel
import com.vknxworks.ridectrl.ui.components.TextInput
import com.vknxworks.ridectrl.ui.components.rememberFmt
import com.vknxworks.ridectrl.ui.nav.ChecklistEditRoute
import com.vknxworks.ridectrl.ui.nav.JettingEditRoute
import com.vknxworks.ridectrl.ui.nav.LocalNavigator
import com.vknxworks.ridectrl.ui.nav.TyreEditRoute
import com.vknxworks.ridectrl.ui.theme.CtrlColors
import com.vknxworks.ridectrl.ui.theme.CtrlType
import com.vknxworks.ridectrl.ui.theme.Gutter

@Composable
fun plugColourLabel(p: PlugColour?): String = stringResource(
    when (p) {
        null -> R.string.plug_not_checked
        PlugColour.WHITE -> R.string.plug_white
        PlugColour.LIGHT_TAN -> R.string.plug_light_tan
        PlugColour.TAN -> R.string.plug_tan
        PlugColour.DARK_BROWN -> R.string.plug_dark_brown
        PlugColour.BLACK_DRY -> R.string.plug_black_dry
        PlugColour.BLACK_WET -> R.string.plug_black_wet
    },
)

@Composable
fun tyrePositionLabel(p: TyrePosition): String = stringResource(if (p == TyrePosition.FRONT) R.string.tyre_front else R.string.tyre_rear)

@Composable
fun tyreEventLabel(e: TyreEvent): String = stringResource(if (e == TyreEvent.FITTED) R.string.tyre_event_fitted else R.string.tyre_event_check)

/** "Main 85 · idle 38 · clip 3", leaving out what wasn't filled in. */
@Composable
private fun jetsLine(e: JettingEntry): String = listOfNotNull(
    e.mainJet.ifBlank { null }?.let { stringResource(R.string.jetting_main_short, it) },
    e.idleJet.ifBlank { null }?.let { stringResource(R.string.jetting_idle_short, it) },
    e.needleClip.ifBlank { null }?.let { stringResource(R.string.jetting_clip_short, it) },
).joinToString(" · ").ifBlank { stringResource(R.string.jetting_no_jets) }

@Composable
private fun tempsLine(t: TempSummary?): String {
    val fmt = rememberFmt()
    if (t == null) return stringResource(R.string.jetting_no_temps)
    return pluralStringResource(R.plurals.reading_count, t.count, t.count) + " · " +
        stringResource(R.string.temps_min_avg_max, fmt.temp(t.min), fmt.temp(t.avg), fmt.temp(t.max))
}

// ---------------------------------------------------------------- Jetting

/** The jetting log: every carburettor setting tried, newest first, each with the temperatures it ran at. */
@Composable
fun JettingScreen(vehicleId: String) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val data by c.logs.data.collectAsStateWithLifecycle()
    val fmt = rememberFmt()
    val periods = remember(data, vehicleId) { data.jettingPeriods(vehicleId) }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.jetting_title)) {
            CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.jetting_add), onClick = { nav.navigate(JettingEditRoute(vehicleId)) })
        }
        if (periods.isEmpty()) {
            MessageView(
                Icons.Outlined.Science,
                stringResource(R.string.jetting_empty_title),
                stringResource(R.string.jetting_empty_body),
                actionLabel = stringResource(R.string.jetting_add),
                onAction = { nav.navigate(JettingEditRoute(vehicleId)) },
            )
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
            val current = periods.first()
            item(key = "current") {
                CtrlCard(
                    Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp),
                    onClick = { nav.navigate(JettingEditRoute(vehicleId, current.entry.id)) },
                ) {
                    SectionLabel(stringResource(R.string.jetting_current, fmt.date(current.entry.date)))
                    Spacer(Modifier.size(8.dp))
                    Text(jetsLine(current.entry), style = MaterialTheme.typography.titleLarge, color = CtrlColors.Text)
                    Spacer(Modifier.size(4.dp))
                    Text(stringResource(R.string.jetting_plug_is, plugColourLabel(current.entry.plugColour)), style = MaterialTheme.typography.bodyMedium, color = CtrlColors.Text)
                    Text(tempsLine(current.temps), style = CtrlType.Data, color = CtrlColors.Muted)
                }
            }
            item(key = "history") {
                Group(stringResource(R.string.jetting_history), footer = stringResource(R.string.jetting_history_note)) {
                    periods.forEach { p ->
                        val e = p.entry
                        EntryRow(
                            title = jetsLine(e),
                            subtitle = listOfNotNull(
                                fmt.date(e.date),
                                plugColourLabel(e.plugColour).takeIf { e.plugColour != null },
                                weatherLine(e),
                                tempsLine(p.temps),
                            ).joinToString("\n"),
                            value = p.temps?.let { fmt.temp(it.avg) },
                            onClick = { nav.navigate(JettingEditRoute(vehicleId, e.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun weatherLine(e: JettingEntry): String? {
    val fmt = rememberFmt()
    return listOfNotNull(
        e.ambientC?.let { fmt.temp(it) },
        e.humidity?.let { stringResource(R.string.jetting_humidity_short, it) },
        e.weather.ifBlank { null },
    ).joinToString(" · ").ifBlank { null }
}

@Composable
fun JettingEditScreen(vehicleId: String, id: String?) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val fmt = rememberFmt()
    val data = c.logs.data.value
    val old = remember(id) { data.jetting.firstOrNull { it.id == id } }
    // A new entry starts from the last setting, so only what changed needs typing.
    val last = remember(data, vehicleId) { data.jetting.filter { it.vehicleId == vehicleId }.maxByOrNull { it.date } }
    val period = remember(data, vehicleId, id) { data.jettingPeriods(vehicleId).firstOrNull { it.entry.id == id } }
    val odoHint = remember(data, vehicleId) { data.currentKm(vehicleId)?.let { fmt.distanceInput(it) } }

    var date by rememberSaveable { mutableStateOf(old?.date ?: today()) }
    var odo by rememberSaveable { mutableStateOf(fmt.distanceInput(old?.odometer)) }
    var main by rememberSaveable { mutableStateOf(old?.mainJet ?: last?.mainJet ?: "") }
    var idle by rememberSaveable { mutableStateOf(old?.idleJet ?: last?.idleJet ?: "") }
    var clip by rememberSaveable { mutableStateOf(old?.needleClip ?: last?.needleClip ?: "") }
    var ambient by rememberSaveable { mutableStateOf(fmt.tempInput(old?.ambientC)) }
    var humidity by rememberSaveable { mutableStateOf(old?.humidity?.toString() ?: "") }
    var weather by rememberSaveable { mutableStateOf(old?.weather ?: "") }
    var plug by rememberSaveable { mutableStateOf(old?.plugColour) }
    var notes by rememberSaveable { mutableStateOf(old?.notes ?: "") }

    val odoKm = parseNumber(odo)?.let { fmt.userToKm(it) }
    val hum = parseInt(humidity)
    val canSave = (main.isNotBlank() || idle.isNotBlank() || clip.isNotBlank()) &&
        (odo.isBlank() || odoKm != null) &&
        (ambient.isBlank() || parseNumber(ambient) != null) &&
        (humidity.isBlank() || (hum != null && hum in 0..100))

    FormScreen(
        title = stringResource(if (old == null) R.string.jetting_add else R.string.jetting_edit),
        canSave = canSave,
        onSave = {
            val e = JettingEntry(
                id = old?.id ?: newId(),
                vehicleId = vehicleId,
                date = date,
                odometer = odoKm,
                mainJet = main.trim(),
                idleJet = idle.trim(),
                needleClip = clip.trim(),
                ambientC = parseNumber(ambient)?.let { fmt.userToC(it) },
                humidity = hum,
                weather = weather.trim(),
                plugColour = plug,
                notes = notes.trim(),
            )
            c.logs.update { d -> d.copy(jetting = if (old == null) d.jetting + e else d.jetting.map { if (it.id == e.id) e else it }) }
            nav.back()
        },
        onDelete = old?.let { o -> { c.logs.update { d -> d.copy(jetting = d.jetting.filterNot { it.id == o.id }) }; nav.back() } },
    ) {
        DateInput(stringResource(R.string.date), date, { if (it != null) date = it })
        NumberInput(stringResource(R.string.vehicle_odometer), odo, { odo = it }, suffix = fmt.distanceUnit, placeholder = odoHint)
        FieldHeading(stringResource(R.string.jetting_carb))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextInput(stringResource(R.string.jetting_main), main, { main = it.take(12) }, Modifier.weight(1f))
            TextInput(stringResource(R.string.jetting_idle), idle, { idle = it.take(12) }, Modifier.weight(1f))
        }
        TextInput(stringResource(R.string.jetting_clip), clip, { clip = it.take(30) }, placeholder = stringResource(R.string.jetting_clip_hint))
        FieldNote(stringResource(R.string.jetting_carb_note))
        FieldHeading(stringResource(R.string.jetting_weather))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberInput(stringResource(R.string.temp_ambient), ambient, { ambient = it }, Modifier.weight(1f), suffix = fmt.tempUnit)
            NumberInput(stringResource(R.string.jetting_humidity), humidity, { humidity = it }, Modifier.weight(1f), suffix = "%", decimals = false)
        }
        TextInput(stringResource(R.string.jetting_weather_text), weather, { weather = it.take(80) }, sentences = true, placeholder = stringResource(R.string.jetting_weather_hint))
        FieldHeading(stringResource(R.string.jetting_plug))
        ChoiceInput(
            stringResource(R.string.jetting_plug),
            listOf<PlugColour?>(null).plus(PlugColour.entries).map { it to plugColourLabel(it) },
            plug,
        ) { plug = it }
        FieldNote(stringResource(R.string.jetting_plug_note))
        TextInput(stringResource(R.string.notes), notes, { notes = it.take(1000) }, singleLine = false, minLines = 2, sentences = true)
        if (old != null) {
            FieldHeading(stringResource(R.string.jetting_temps))
            Text(tempsLine(period?.temps), style = MaterialTheme.typography.bodyMedium, color = CtrlColors.Text, modifier = Modifier.padding(horizontal = 4.dp))
            FieldNote(stringResource(R.string.jetting_history_note))
        }
    }
}

// ---------------------------------------------------------------- Tyres

/** The tyre log: the tyre that's on front and rear, km and age, the last pressure and tread check, and the history. */
@Composable
fun TyresScreen(vehicleId: String) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val data by c.logs.data.collectAsStateWithLifecycle()
    val fmt = rememberFmt()
    val entries = remember(data, vehicleId) {
        data.tyres.filter { it.vehicleId == vehicleId }.sortedWith(compareByDescending<TyreEntry> { it.date }.thenByDescending { it.odometer ?: 0.0 })
    }
    val status = remember(data, vehicleId) { TyrePosition.entries.map { data.tyreStatus(vehicleId, it) } }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.tyres_title)) {
            CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.tyre_add), onClick = { nav.navigate(TyreEditRoute(vehicleId)) })
        }
        if (entries.isEmpty()) {
            MessageView(
                Icons.Outlined.TireRepair,
                stringResource(R.string.tyres_empty_title),
                stringResource(R.string.tyres_empty_body),
                actionLabel = stringResource(R.string.tyre_add_fitted),
                onAction = { nav.navigate(TyreEditRoute(vehicleId, event = TyreEvent.FITTED.name)) },
            )
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
            status.forEach { s ->
                item(key = "pos-${s.position}") { TyreCard(vehicleId, s) }
            }
            item(key = "history") {
                Group(stringResource(R.string.tyres_history)) {
                    entries.forEach { t ->
                        EntryRow(
                            title = tyrePositionLabel(t.position) + " · " + tyreEventLabel(t.event),
                            subtitle = listOfNotNull(
                                fmt.date(t.date),
                                listOf(t.brand, t.model, t.size).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null },
                                t.pressureBar?.let { stringResource(R.string.tyre_pressure_value, fmt.num(it, 2)) },
                                t.treadMm?.let { stringResource(R.string.tyre_tread_value, fmt.num(it, 1)) },
                            ).joinToString(" · "),
                            value = t.odometer?.let { fmt.distance(it) },
                            onClick = { nav.navigate(TyreEditRoute(vehicleId, t.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TyreCard(vehicleId: String, s: TyreStatus) {
    val nav = LocalNavigator.current
    val fmt = rememberFmt()
    CtrlCard(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp)) {
        SectionLabel(tyrePositionLabel(s.position))
        Spacer(Modifier.size(8.dp))
        val f = s.fitted
        if (f == null) {
            Text(stringResource(R.string.tyre_none_fitted), style = MaterialTheme.typography.bodyMedium, color = CtrlColors.Muted)
        } else {
            Text(
                listOf(f.brand, f.model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { stringResource(R.string.tyre_unnamed) },
                style = MaterialTheme.typography.titleLarge,
                color = CtrlColors.Text,
            )
            if (f.size.isNotBlank()) Text(f.size, style = CtrlType.Data, color = CtrlColors.Muted)
            Spacer(Modifier.size(4.dp))
            Text(
                listOfNotNull(
                    stringResource(R.string.tyre_fitted_on, fmt.date(f.date)),
                    s.kmOnTyre?.let { stringResource(R.string.tyre_km_on, fmt.distance(it)) },
                    dotAgeMonths(f.dotCode)?.let { m -> pluralStringResource(R.plurals.tyre_age_months, m.toInt(), m.toInt()) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = CtrlColors.Text,
            )
        }
        val check = s.lastCheck
        Text(
            if (check == null) {
                stringResource(R.string.tyre_no_check)
            } else {
                stringResource(
                    R.string.tyre_last_check,
                    fmt.date(check.date),
                    listOfNotNull(
                        check.pressureBar?.let { stringResource(R.string.tyre_pressure_value, fmt.num(it, 2)) },
                        check.treadMm?.let { stringResource(R.string.tyre_tread_value, fmt.num(it, 1)) },
                    ).joinToString(" · ").ifBlank { "–" },
                )
            },
            style = CtrlType.Data,
            color = CtrlColors.Muted,
        )
        Spacer(Modifier.size(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CtrlSecondaryButton(
                onClick = { nav.navigate(TyreEditRoute(vehicleId, position = s.position.name, event = TyreEvent.CHECK.name)) },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.tyre_add_check), maxLines = 1) }
            CtrlSecondaryButton(
                onClick = { nav.navigate(TyreEditRoute(vehicleId, position = s.position.name, event = TyreEvent.FITTED.name)) },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.tyre_add_fitted), maxLines = 1) }
        }
    }
}

@Composable
fun TyreEditScreen(vehicleId: String, id: String?, position: String?, event: String?) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val fmt = rememberFmt()
    val data = c.logs.data.value
    val old = remember(id) { data.tyres.firstOrNull { it.id == id } }
    val odoHint = remember(data, vehicleId) { data.currentKm(vehicleId)?.let { fmt.distanceInput(it) } }

    var pos by rememberSaveable { mutableStateOf(old?.position ?: TyrePosition.entries.firstOrNull { it.name == position } ?: TyrePosition.FRONT) }
    var ev by rememberSaveable { mutableStateOf(old?.event ?: TyreEvent.entries.firstOrNull { it.name == event } ?: TyreEvent.CHECK) }
    var date by rememberSaveable { mutableStateOf(old?.date ?: today()) }
    var odo by rememberSaveable { mutableStateOf(fmt.distanceInput(old?.odometer)) }
    var brand by rememberSaveable { mutableStateOf(old?.brand ?: "") }
    var model by rememberSaveable { mutableStateOf(old?.model ?: "") }
    var size by rememberSaveable { mutableStateOf(old?.size ?: "") }
    var dot by rememberSaveable { mutableStateOf(old?.dotCode ?: "") }
    var cost by rememberSaveable { mutableStateOf(fmt.moneyInput(old?.cost)) }
    var pressure by rememberSaveable { mutableStateOf(old?.pressureBar?.let { fmt.plain(it, 2) } ?: "") }
    var tread by rememberSaveable { mutableStateOf(old?.treadMm?.let { fmt.plain(it, 1) } ?: "") }
    var notes by rememberSaveable { mutableStateOf(old?.notes ?: "") }

    val odoKm = parseNumber(odo)?.let { fmt.userToKm(it) }
    val bar = parseNumber(pressure)
    val mm = parseNumber(tread)
    val fitted = ev == TyreEvent.FITTED
    val canSave = (odo.isBlank() || odoKm != null) &&
        (pressure.isBlank() || (bar != null && bar in 0.0..20.0)) &&
        (tread.isBlank() || (mm != null && mm in 0.0..50.0)) &&
        (!fitted || cost.isBlank() || parseNumber(cost) != null) &&
        (fitted || bar != null || mm != null || notes.isNotBlank())

    FormScreen(
        title = stringResource(if (old == null) R.string.tyre_add else R.string.tyre_edit),
        canSave = canSave,
        onSave = {
            val t = TyreEntry(
                id = old?.id ?: newId(),
                vehicleId = vehicleId,
                position = pos,
                event = ev,
                date = date,
                odometer = odoKm,
                brand = if (fitted) brand.trim() else "",
                model = if (fitted) model.trim() else "",
                size = if (fitted) size.trim() else "",
                dotCode = if (fitted) dot.trim() else "",
                cost = if (fitted) parseNumber(cost) else null,
                pressureBar = bar,
                treadMm = mm,
                notes = notes.trim(),
            )
            c.logs.update { d -> d.copy(tyres = if (old == null) d.tyres + t else d.tyres.map { if (it.id == t.id) t else it }) }
            nav.back()
        },
        onDelete = old?.let { o -> { c.logs.update { d -> d.copy(tyres = d.tyres.filterNot { it.id == o.id }) }; nav.back() } },
    ) {
        ChoiceInput(stringResource(R.string.tyre_position), TyrePosition.entries.map { it to tyrePositionLabel(it) }, pos) { pos = it }
        ChoiceInput(stringResource(R.string.tyre_event), TyreEvent.entries.map { it to tyreEventLabel(it) }, ev) { ev = it }
        DateInput(stringResource(R.string.date), date, { if (it != null) date = it })
        NumberInput(stringResource(R.string.vehicle_odometer), odo, { odo = it }, suffix = fmt.distanceUnit, placeholder = odoHint)
        if (fitted) {
            FieldHeading(stringResource(R.string.tyre_which))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextInput(stringResource(R.string.tyre_brand), brand, { brand = it.take(40) }, Modifier.weight(1f), words = true)
                TextInput(stringResource(R.string.tyre_model), model, { model = it.take(40) }, Modifier.weight(1f), words = true)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextInput(stringResource(R.string.tyre_size), size, { size = it.take(30) }, Modifier.weight(1f), placeholder = "120/70-12")
                TextInput(stringResource(R.string.tyre_dot), dot, { dot = it.take(12) }, Modifier.weight(1f), placeholder = "2423")
            }
            FieldNote(stringResource(R.string.tyre_dot_note))
            NumberInput(stringResource(R.string.tyre_cost), cost, { cost = it }, suffix = fmt.currencySymbol)
        }
        FieldHeading(stringResource(R.string.tyre_measured))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberInput(stringResource(R.string.tyre_pressure), pressure, { pressure = it }, Modifier.weight(1f), suffix = stringResource(R.string.unit_bar))
            NumberInput(stringResource(R.string.tyre_tread), tread, { tread = it }, Modifier.weight(1f), suffix = stringResource(R.string.unit_mm))
        }
        if (!fitted) FieldNote(stringResource(R.string.tyre_check_note))
        TextInput(stringResource(R.string.notes), notes, { notes = it.take(1000) }, singleLine = false, minLines = 2, sentences = true)
    }
}

// ---------------------------------------------------------------- Pre-ride checklist

/** The starting checklist for a vehicle, by engine type. The user can change every item afterwards. */
private fun starterChecklist(engine: EngineType): List<Int> = buildList {
    add(R.string.check_tyres)
    add(R.string.check_brakes)
    add(R.string.check_lights)
    add(R.string.check_horn)
    add(R.string.check_mirrors)
    add(R.string.check_throttle)
    when (engine) {
        EngineType.FOUR_STROKE -> add(R.string.check_engine_oil)
        EngineType.TWO_STROKE_INJECTION -> add(R.string.check_two_stroke_oil)
        else -> Unit
    }
    add(if (engine == EngineType.ELECTRIC) R.string.check_battery_charge else R.string.check_fuel)
    add(R.string.check_leaks)
}

/** Go through the pre-ride checklist: tick what's OK, save the check, and see the earlier ones. */
@Composable
fun ChecklistScreen(vehicleId: String) {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val data by c.logs.data.collectAsStateWithLifecycle()
    val fmt = rememberFmt()
    val items = remember(data, vehicleId) { data.checklistFor(vehicleId) }
    val history = remember(data, vehicleId) { data.checks.filter { it.vehicleId == vehicleId }.sortedByDescending { it.time } }
    var ticked by remember(items) { mutableStateOf(emptySet<String>()) }
    var notes by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<PreRideCheck?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.checklist_title)) {
            CtrlSquareButton(Icons.Outlined.Edit, stringResource(R.string.checklist_edit), onClick = { nav.navigate(ChecklistEditRoute(vehicleId)) })
        }
        if (items.isEmpty() && history.isEmpty()) {
            val engine = data.vehicles.firstOrNull { it.id == vehicleId }?.engine ?: EngineType.TWO_STROKE_PREMIX
            MessageView(
                Icons.Outlined.Checklist,
                stringResource(R.string.checklist_empty_title),
                stringResource(R.string.checklist_empty_body),
                actionLabel = stringResource(R.string.checklist_use_starter),
                onAction = {
                    c.logs.update { d ->
                        d.copy(checklist = d.checklist + starterChecklist(engine).mapIndexed { i, res -> ChecklistItem(newId(), vehicleId, context.getString(res), i) })
                    }
                },
            )
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (items.isEmpty()) {
                item(key = "noitems") {
                    Text(
                        stringResource(R.string.checklist_no_items),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CtrlColors.Muted,
                        modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp),
                    )
                }
            } else {
                item(key = "run") {
                    Group(stringResource(R.string.checklist_now, ticked.size, items.size)) {
                        items.forEach { item ->
                            val ok = item.id in ticked
                            GroupRow(
                                item.text,
                                onClick = { ticked = if (ok) ticked - item.id else ticked + item.id },
                                trailing = {
                                    Icon(
                                        if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                                        stringResource(if (ok) R.string.checklist_ok else R.string.checklist_not_ok),
                                        Modifier.size(24.dp),
                                        tint = if (ok) CtrlColors.AccentText else CtrlColors.Inactive,
                                    )
                                },
                            )
                        }
                    }
                }
                item(key = "save") {
                    Column(Modifier.padding(horizontal = Gutter)) {
                        TextInput(stringResource(R.string.notes), notes, { notes = it.take(500) }, singleLine = false, minLines = 1, sentences = true)
                        Spacer(Modifier.size(10.dp))
                        CtrlButton(
                            stringResource(R.string.checklist_save),
                            onClick = {
                                val check = PreRideCheck(
                                    id = newId(),
                                    vehicleId = vehicleId,
                                    time = System.currentTimeMillis(),
                                    items = items.map { it.text },
                                    ok = items.indices.filter { items[it].id in ticked },
                                    notes = notes.trim(),
                                )
                                c.logs.update { d -> d.copy(checks = d.checks + check) }
                                ticked = emptySet()
                                notes = ""
                                Toast.makeText(context, context.getString(R.string.checklist_saved), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (ticked.size < items.size) {
                            Spacer(Modifier.size(6.dp))
                            Text(stringResource(R.string.checklist_save_note), style = MaterialTheme.typography.bodySmall, color = CtrlColors.Muted)
                        }
                    }
                }
            }
            if (history.isNotEmpty()) {
                item(key = "history") {
                    Group(stringResource(R.string.checklist_history), footer = stringResource(R.string.checklist_history_note)) {
                        history.forEach { h ->
                            val missed = h.items.filterIndexed { i, _ -> i !in h.ok }
                            EntryRow(
                                title = fmt.dateTime(h.time),
                                subtitle = listOfNotNull(
                                    if (missed.isEmpty()) stringResource(R.string.checklist_all_ok) else stringResource(R.string.checklist_not_ok_list, missed.joinToString(", ")),
                                    h.notes.ifBlank { null },
                                ).joinToString("\n"),
                                value = stringResource(R.string.checklist_score, h.ok.size, h.items.size),
                                onClick = { deleting = h },
                            )
                        }
                    }
                }
            }
        }
    }
    deleting?.let { h ->
        ConfirmDialog(
            title = stringResource(R.string.checklist_delete_title),
            body = stringResource(R.string.checklist_delete_body, fmt.dateTime(h.time)),
            confirm = stringResource(R.string.delete),
            onDismiss = { deleting = null },
        ) {
            deleting = null
            c.logs.update { d -> d.copy(checks = d.checks.filterNot { it.id == h.id }) }
        }
    }
}

/** Change the checklist: add, rename, remove and reorder items. Earlier checks keep their own wording. */
@Composable
fun ChecklistEditScreen(vehicleId: String) {
    val c = LocalContext.current.container
    val data by c.logs.data.collectAsStateWithLifecycle()
    val items = remember(data, vehicleId) { data.checklistFor(vehicleId) }
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<ChecklistItem?>(null) }
    var removing by remember { mutableStateOf<ChecklistItem?>(null) }

    /** Saves [list] in its order (0, 1, 2...) for this vehicle. */
    fun saveOrder(list: List<ChecklistItem>) {
        val ordered = list.mapIndexed { i, it -> it.copy(order = i) }.associateBy { it.id }
        c.logs.update { d -> d.copy(checklist = d.checklist.map { ordered[it.id] ?: it }) }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.checklist_edit)) {
            CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.checklist_add_item), onClick = { adding = true })
        }
        if (items.isEmpty()) {
            MessageView(
                Icons.Outlined.Checklist,
                stringResource(R.string.checklist_no_items),
                actionLabel = stringResource(R.string.checklist_add_item),
                onAction = { adding = true },
            )
        } else {
            LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item(key = "items") {
                    Group(footer = stringResource(R.string.checklist_edit_note)) {
                        items.forEachIndexed { i, entry ->
                            GroupRow(
                                entry.text,
                                onClick = { renaming = entry },
                                trailing = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { if (i > 0) saveOrder(items.toMutableList().apply { add(i - 1, removeAt(i)) }) }, enabled = i > 0) {
                                            Icon(Icons.Outlined.ArrowUpward, stringResource(R.string.checklist_move_up), tint = if (i > 0) CtrlColors.Text else CtrlColors.Inactive)
                                        }
                                        IconButton(onClick = { if (i < items.lastIndex) saveOrder(items.toMutableList().apply { add(i + 1, removeAt(i)) }) }, enabled = i < items.lastIndex) {
                                            Icon(Icons.Outlined.ArrowDownward, stringResource(R.string.checklist_move_down), tint = if (i < items.lastIndex) CtrlColors.Text else CtrlColors.Inactive)
                                        }
                                        IconButton(onClick = { removing = entry }) {
                                            Icon(Icons.Outlined.Delete, stringResource(R.string.delete), tint = CtrlColors.Error)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
    if (adding) {
        NameDialog(stringResource(R.string.checklist_add_item), "", confirm = stringResource(R.string.checklist_add), onDismiss = { adding = false }) { text ->
            adding = false
            val next = (items.maxOfOrNull { it.order } ?: -1) + 1
            c.logs.update { d -> d.copy(checklist = d.checklist + ChecklistItem(newId(), vehicleId, text, next)) }
        }
    }
    renaming?.let { item ->
        NameDialog(stringResource(R.string.checklist_rename), item.text, onDismiss = { renaming = null }) { text ->
            renaming = null
            c.logs.update { d -> d.copy(checklist = d.checklist.map { if (it.id == item.id) it.copy(text = text) else it }) }
        }
    }
    removing?.let { item ->
        ConfirmDialog(
            title = stringResource(R.string.checklist_remove_title),
            body = stringResource(R.string.checklist_remove_body, item.text),
            confirm = stringResource(R.string.delete),
            onDismiss = { removing = null },
        ) {
            removing = null
            c.logs.update { d -> d.copy(checklist = d.checklist.filterNot { it.id == item.id }) }
        }
    }
}
