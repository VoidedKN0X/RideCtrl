package com.vknxworks.ridectrl.ui.logs

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
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.container
import com.vknxworks.ridectrl.data.CostCategory
import com.vknxworks.ridectrl.data.DocType
import com.vknxworks.ridectrl.data.DueState
import com.vknxworks.ridectrl.data.OdometerReading
import com.vknxworks.ridectrl.data.RideType
import com.vknxworks.ridectrl.data.TempReading
import com.vknxworks.ridectrl.data.Trip
import com.vknxworks.ridectrl.data.VehicleDoc
import com.vknxworks.ridectrl.data.VehicleNote
import com.vknxworks.ridectrl.data.costLines
import com.vknxworks.ridectrl.data.currentKm
import com.vknxworks.ridectrl.data.daysLeft
import com.vknxworks.ridectrl.data.kmPerDay
import com.vknxworks.ridectrl.data.newId
import com.vknxworks.ridectrl.data.odoPoints
import com.vknxworks.ridectrl.data.parseInt
import com.vknxworks.ridectrl.data.parseNumber
import com.vknxworks.ridectrl.data.ridden
import com.vknxworks.ridectrl.data.state
import com.vknxworks.ridectrl.data.tempGroups
import com.vknxworks.ridectrl.data.today
import com.vknxworks.ridectrl.ui.components.BarChart
import com.vknxworks.ridectrl.ui.components.ChoiceInput
import com.vknxworks.ridectrl.ui.components.CtrlCard
import com.vknxworks.ridectrl.ui.components.CtrlSquareButton
import com.vknxworks.ridectrl.ui.components.DateInput
import com.vknxworks.ridectrl.ui.components.DateTimeInput
import com.vknxworks.ridectrl.ui.components.DueTag
import com.vknxworks.ridectrl.ui.components.EntryRow
import com.vknxworks.ridectrl.ui.components.FieldHeading
import com.vknxworks.ridectrl.ui.components.FieldNote
import com.vknxworks.ridectrl.ui.components.FormScreen
import com.vknxworks.ridectrl.ui.components.Group
import com.vknxworks.ridectrl.ui.components.LineChart
import com.vknxworks.ridectrl.ui.components.MessageView
import com.vknxworks.ridectrl.ui.components.NumberInput
import com.vknxworks.ridectrl.ui.components.PhotoStrip
import com.vknxworks.ridectrl.ui.components.ScreenTopBar
import com.vknxworks.ridectrl.ui.components.SectionLabel
import com.vknxworks.ridectrl.ui.components.StatRow
import com.vknxworks.ridectrl.ui.components.StatTile
import com.vknxworks.ridectrl.ui.components.TextInput
import com.vknxworks.ridectrl.ui.components.rememberFmt
import com.vknxworks.ridectrl.ui.nav.DocEditRoute
import com.vknxworks.ridectrl.ui.nav.LocalNavigator
import com.vknxworks.ridectrl.ui.nav.NoteEditRoute
import com.vknxworks.ridectrl.ui.nav.OdometerEditRoute
import com.vknxworks.ridectrl.ui.nav.TempEditRoute
import com.vknxworks.ridectrl.ui.nav.TripEditRoute
import com.vknxworks.ridectrl.ui.theme.CtrlColors
import com.vknxworks.ridectrl.ui.theme.CtrlType
import com.vknxworks.ridectrl.ui.theme.Gutter

@Composable
fun rideTypeLabel(t: RideType): String = stringResource(
    when (t) {
        RideType.CITY -> R.string.ride_city
        RideType.HIGHWAY -> R.string.ride_highway
        RideType.UPHILL -> R.string.ride_uphill
        RideType.IDLE -> R.string.ride_idle
        RideType.MIXED -> R.string.ride_mixed
    },
)

@Composable
fun docTypeLabel(t: DocType): String = stringResource(
    when (t) {
        DocType.INSURANCE -> R.string.doc_insurance
        DocType.REGISTRATION -> R.string.doc_registration
        DocType.INSPECTION -> R.string.doc_inspection
        DocType.OTHER -> R.string.doc_other
    },
)

@Composable
fun costLabel(c: CostCategory): String = stringResource(
    when (c) {
        CostCategory.PURCHASE -> R.string.cost_purchase
        CostCategory.FUEL -> R.string.cost_fuel
        CostCategory.OIL -> R.string.cost_oil
        CostCategory.MAINTENANCE -> R.string.cost_maintenance
        CostCategory.PARTS -> R.string.cost_parts
        CostCategory.MODS -> R.string.cost_mods
        CostCategory.DOCUMENTS -> R.string.cost_documents
    },
)

// ---------------------------------------------------------------- Temperature

/** Temperature readings from the dash gauge: a chart over time and min/avg/max per setup. */
@Composable
fun TempsScreen(vehicleId: String) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val data by c.logs.data.collectAsStateWithLifecycle()
    val fmt = rememberFmt()
    val readings = data.temps.filter { it.vehicleId == vehicleId }.sortedBy { it.time }
    val groups = remember(data) { data.tempGroups(vehicleId) }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.temps_title)) {
            CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.action_add_temp), onClick = { nav.navigate(TempEditRoute(vehicleId)) })
        }
        if (readings.isEmpty()) {
            MessageView(
                Icons.Outlined.Thermostat,
                stringResource(R.string.temps_empty_title),
                stringResource(R.string.temps_empty_body),
                actionLabel = stringResource(R.string.action_add_temp),
                onAction = { nav.navigate(TempEditRoute(vehicleId)) },
            )
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (readings.size >= 2) {
                item {
                    CtrlCard(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp)) {
                        SectionLabel(stringResource(R.string.temps_chart, fmt.tempUnit))
                        Spacer(Modifier.size(10.dp))
                        LineChart(
                            readings.takeLast(60).map { fmt.cToUser(it.tempC) },
                            labelFor = { fmt.num(it, 0) },
                            startLabel = fmt.dateTime(readings.takeLast(60).first().time).substringBefore(','),
                            endLabel = fmt.dateTime(readings.last().time).substringBefore(','),
                        )
                    }
                }
            }
            item {
                Group(stringResource(R.string.temps_by_setup), footer = stringResource(R.string.temps_by_setup_note)) {
                    groups.forEach { g ->
                        EntryRow(
                            title = g.setup.ifBlank { stringResource(R.string.temps_no_setup) },
                            subtitle = stringResource(R.string.temps_min_avg_max, fmt.temp(g.min), fmt.temp(g.avg), fmt.temp(g.max)),
                            value = pluralStringResource(R.plurals.reading_count, g.count, g.count),
                            onClick = {},
                        )
                    }
                }
            }
            item {
                Group(stringResource(R.string.temps_readings)) {
                    readings.reversed().forEach { t ->
                        EntryRow(
                            title = fmt.temp(t.tempC),
                            subtitle = listOfNotNull(
                                fmt.dateTime(t.time),
                                rideTypeLabel(t.rideType),
                                t.ambientC?.let { stringResource(R.string.temps_ambient_short, fmt.temp(it)) },
                                t.setup.ifBlank { null },
                            ).joinToString(" · "),
                            value = t.odometer?.let { fmt.distance(it) },
                            onClick = { nav.navigate(TempEditRoute(vehicleId, t.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TempEditScreen(vehicleId: String, id: String?) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val fmt = rememberFmt()
    val data = c.logs.data.value
    val old = remember(id) { data.temps.firstOrNull { it.id == id } }
    // A new reading starts with the setup of the last one, so readings group without retyping.
    val lastSetup = data.temps.filter { it.vehicleId == vehicleId }.maxByOrNull { it.time }?.setup ?: ""

    var time by rememberSaveable { mutableStateOf(old?.time ?: System.currentTimeMillis()) }
    var temp by rememberSaveable { mutableStateOf(fmt.tempInput(old?.tempC)) }
    var odo by rememberSaveable { mutableStateOf(fmt.distanceInput(old?.odometer)) }
    var ambient by rememberSaveable { mutableStateOf(fmt.tempInput(old?.ambientC)) }
    var ride by rememberSaveable { mutableStateOf(old?.rideType ?: RideType.CITY) }
    var setup by rememberSaveable { mutableStateOf(old?.setup ?: lastSetup) }
    var notes by rememberSaveable { mutableStateOf(old?.notes ?: "") }

    val t = parseNumber(temp)
    val odoKm = parseNumber(odo)?.let { fmt.userToKm(it) }
    FormScreen(
        title = stringResource(if (old == null) R.string.action_add_temp else R.string.temp_edit),
        canSave = t != null && (odo.isBlank() || odoKm != null),
        onSave = {
            val r = TempReading(
                id = old?.id ?: newId(),
                vehicleId = vehicleId,
                time = time,
                tempC = fmt.userToC(t!!),
                odometer = odoKm,
                ambientC = parseNumber(ambient)?.let { fmt.userToC(it) },
                rideType = ride,
                setup = setup.trim(),
                notes = notes.trim(),
            )
            c.logs.update { d -> d.copy(temps = if (old == null) d.temps + r else d.temps.map { if (it.id == r.id) r else it }) }
            nav.back()
        },
        onDelete = old?.let { o -> { c.logs.update { d -> d.copy(temps = d.temps.filterNot { it.id == o.id }) }; nav.back() } },
    ) {
        NumberInput(stringResource(R.string.temp_reading), temp, { temp = it }, suffix = fmt.tempUnit)
        DateTimeInput(stringResource(R.string.temp_when), time) { time = it }
        NumberInput(stringResource(R.string.vehicle_odometer), odo, { odo = it }, suffix = fmt.distanceUnit, placeholder = data.currentKm(vehicleId)?.let { fmt.distanceInput(it) })
        FieldHeading(stringResource(R.string.temp_conditions))
        NumberInput(stringResource(R.string.temp_ambient), ambient, { ambient = it }, suffix = fmt.tempUnit)
        ChoiceInput(stringResource(R.string.temp_ride_type), RideType.entries.map { it to rideTypeLabel(it) }, ride) { ride = it }
        TextInput(stringResource(R.string.temp_setup), setup, { setup = it.take(120) }, sentences = true, placeholder = stringResource(R.string.temp_setup_hint))
        FieldNote(stringResource(R.string.temp_setup_note))
        TextInput(stringResource(R.string.notes), notes, { notes = it.take(1000) }, singleLine = false, minLines = 2, sentences = true)
    }
}

// ---------------------------------------------------------------- Odometer and trips

/** Odometer history (from every log), the riding average, and simple trips. */
@Composable
fun OdometerScreen(vehicleId: String) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val data by c.logs.data.collectAsStateWithLifecycle()
    val fmt = rememberFmt()
    val readings = data.odometer.filter { it.vehicleId == vehicleId }.sortedByDescending { it.date }
    val trips = data.trips.filter { it.vehicleId == vehicleId }.sortedByDescending { it.date }
    val points = remember(data) { data.odoPoints(vehicleId) }
    val perDay = remember(data) { data.kmPerDay(vehicleId) }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.odo_title)) {
            CtrlSquareButton(Icons.Outlined.Route, stringResource(R.string.trip_add), onClick = { nav.navigate(TripEditRoute(vehicleId)) })
            CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.odo_add), onClick = { nav.navigate(OdometerEditRoute(vehicleId)) })
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                StatRow(
                    { m -> StatTile(stringResource(R.string.vehicle_odometer), fmt.distance(data.currentKm(vehicleId)), m) },
                    { m -> StatTile(stringResource(R.string.odo_per_day), perDay?.let { fmt.distance(it, 1) } ?: "–", m, sub = perDay?.let { stringResource(R.string.odo_per_month, fmt.distance(it * 30.4)) }) },
                )
                StatRow(
                    { m -> StatTile(stringResource(R.string.odo_ridden), fmt.distance(ridden(points)), m, sub = stringResource(R.string.odo_ridden_sub)) },
                    { m -> StatTile(stringResource(R.string.odo_trips), fmt.distance(trips.sumOf { it.km }), m, sub = pluralStringResource(R.plurals.trip_count, trips.size, trips.size)) },
                )
                Text(
                    stringResource(R.string.odo_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = CtrlColors.Muted,
                    modifier = Modifier.padding(horizontal = Gutter, vertical = 6.dp),
                )
            }
            if (points.size >= 2) {
                item {
                    CtrlCard(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp)) {
                        SectionLabel(stringResource(R.string.odo_chart, fmt.distanceUnit))
                        Spacer(Modifier.size(10.dp))
                        LineChart(
                            points.takeLast(60).map { fmt.kmToUser(it.km) },
                            labelFor = { fmt.num(it, 0) },
                            startLabel = fmt.shortDate(points.takeLast(60).first().date.toString()),
                            endLabel = fmt.shortDate(points.last().date.toString()),
                        )
                    }
                }
            }
            item {
                Group(stringResource(R.string.odo_readings)) {
                    if (readings.isEmpty()) EntryRow(stringResource(R.string.odo_none), null, null, onClick = { nav.navigate(OdometerEditRoute(vehicleId)) })
                    readings.forEach { r ->
                        EntryRow(fmt.distance(r.km), listOfNotNull(fmt.date(r.date), r.note.ifBlank { null }).joinToString(" · "), null, onClick = { nav.navigate(OdometerEditRoute(vehicleId, r.id)) })
                    }
                }
            }
            item {
                Group(stringResource(R.string.trips_title)) {
                    if (trips.isEmpty()) EntryRow(stringResource(R.string.trips_none), stringResource(R.string.trips_none_sub), null, onClick = { nav.navigate(TripEditRoute(vehicleId)) })
                    trips.forEach { t ->
                        EntryRow(t.purpose.ifBlank { stringResource(R.string.trip) }, fmt.date(t.date), fmt.distance(t.km, 1), onClick = { nav.navigate(TripEditRoute(vehicleId, t.id)) })
                    }
                }
            }
        }
    }
}

@Composable
fun OdometerEditScreen(vehicleId: String, id: String?) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val fmt = rememberFmt()
    val data = c.logs.data.value
    val old = remember(id) { data.odometer.firstOrNull { it.id == id } }
    val current = data.currentKm(vehicleId)

    var date by rememberSaveable { mutableStateOf(old?.date ?: today()) }
    var km by rememberSaveable { mutableStateOf(fmt.distanceInput(old?.km)) }
    var note by rememberSaveable { mutableStateOf(old?.note ?: "") }
    val v = parseNumber(km)?.let { fmt.userToKm(it) }
    val back = old == null && v != null && current != null && v < current

    FormScreen(
        title = stringResource(if (old == null) R.string.odo_add else R.string.odo_edit),
        canSave = v != null && v >= 0,
        onSave = {
            val r = OdometerReading(old?.id ?: newId(), vehicleId, date, v!!, note.trim())
            c.logs.update { d -> d.copy(odometer = if (old == null) d.odometer + r else d.odometer.map { if (it.id == r.id) r else it }) }
            nav.back()
        },
        onDelete = old?.let { o -> { c.logs.update { d -> d.copy(odometer = d.odometer.filterNot { it.id == o.id }) }; nav.back() } },
    ) {
        NumberInput(stringResource(R.string.vehicle_odometer), km, { km = it }, suffix = fmt.distanceUnit, placeholder = current?.let { fmt.distanceInput(it) })
        if (back) {
            CtrlCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.odo_back_title), style = MaterialTheme.typography.titleMedium, color = CtrlColors.Warning)
                Text(stringResource(R.string.odo_back_body, fmt.distance(current)), style = MaterialTheme.typography.bodyMedium)
            }
        }
        DateInput(stringResource(R.string.date), date, { if (it != null) date = it })
        TextInput(stringResource(R.string.odo_note), note, { note = it.take(120) }, sentences = true, placeholder = stringResource(R.string.odo_note_hint))
    }
}

@Composable
fun TripEditScreen(vehicleId: String, id: String?) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val fmt = rememberFmt()
    val old = remember(id) { c.logs.data.value.trips.firstOrNull { it.id == id } }
    var date by rememberSaveable { mutableStateOf(old?.date ?: today()) }
    var km by rememberSaveable { mutableStateOf(fmt.distanceInput(old?.km)) }
    var purpose by rememberSaveable { mutableStateOf(old?.purpose ?: "") }
    val v = parseNumber(km)?.let { fmt.userToKm(it) }
    FormScreen(
        title = stringResource(if (old == null) R.string.trip_add else R.string.trip_edit),
        canSave = v != null && v > 0,
        onSave = {
            val t = Trip(old?.id ?: newId(), vehicleId, date, v!!, purpose.trim())
            c.logs.update { d -> d.copy(trips = if (old == null) d.trips + t else d.trips.map { if (it.id == t.id) t else it }) }
            nav.back()
        },
        onDelete = old?.let { o -> { c.logs.update { d -> d.copy(trips = d.trips.filterNot { it.id == o.id }) }; nav.back() } },
    ) {
        NumberInput(stringResource(R.string.trip_distance), km, { km = it }, suffix = fmt.distanceUnit)
        DateInput(stringResource(R.string.date), date, { if (it != null) date = it })
        TextInput(stringResource(R.string.trip_purpose), purpose, { purpose = it.take(200) }, singleLine = false, minLines = 2, sentences = true)
        FieldNote(stringResource(R.string.trip_note))
    }
}

// ---------------------------------------------------------------- Documents

@Composable
fun DocsScreen(vehicleId: String) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val data by c.logs.data.collectAsStateWithLifecycle()
    val fmt = rememberFmt()
    val docs = data.docs.filter { it.vehicleId == vehicleId }.sortedBy { it.expiry ?: "9999" }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.docs_title)) {
            CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.doc_add), onClick = { nav.navigate(DocEditRoute(vehicleId)) })
        }
        if (docs.isEmpty()) {
            MessageView(
                Icons.Outlined.Description,
                stringResource(R.string.docs_empty_title),
                stringResource(R.string.docs_empty_body),
                actionLabel = stringResource(R.string.doc_add),
                onAction = { nav.navigate(DocEditRoute(vehicleId)) },
            )
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Group {
                    docs.forEach { d ->
                        val days = d.daysLeft()
                        EntryRow(
                            title = d.title.ifBlank { docTypeLabel(d.type) },
                            subtitle = listOfNotNull(
                                docTypeLabel(d.type).takeIf { d.title.isNotBlank() },
                                when {
                                    days == null -> stringResource(R.string.doc_no_expiry)
                                    days < 0 -> stringResource(R.string.doc_expired_on, fmt.date(d.expiry))
                                    else -> pluralStringResource(R.plurals.doc_expires_in, days.toInt(), days.toInt(), fmt.date(d.expiry))
                                },
                            ).joinToString(" · "),
                            value = null,
                            onClick = { nav.navigate(DocEditRoute(vehicleId, d.id)) },
                            trailing = { if (d.state() != DueState.UNKNOWN) DueTag(d.state()) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocEditScreen(vehicleId: String, id: String?) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val fmt = rememberFmt()
    val old = remember(id) { c.logs.data.value.docs.firstOrNull { it.id == id } }
    var type by rememberSaveable { mutableStateOf(old?.type ?: DocType.INSURANCE) }
    var title by rememberSaveable { mutableStateOf(old?.title ?: "") }
    var expiry by rememberSaveable { mutableStateOf(old?.expiry) }
    var warn by rememberSaveable { mutableStateOf((old?.warnDays ?: 30).toString()) }
    var cost by rememberSaveable { mutableStateOf(fmt.moneyInput(old?.cost)) }
    var photos by remember { mutableStateOf(old?.photos ?: emptyList()) }
    var notes by rememberSaveable { mutableStateOf(old?.notes ?: "") }

    FormScreen(
        title = stringResource(if (old == null) R.string.doc_add else R.string.doc_edit),
        canSave = true,
        onSave = {
            val d = VehicleDoc(
                id = old?.id ?: newId(),
                vehicleId = vehicleId,
                type = type,
                title = title.trim(),
                expiry = expiry,
                cost = parseNumber(cost),
                warnDays = parseInt(warn)?.coerceIn(0, 365) ?: 30,
                photos = photos,
                notes = notes.trim(),
            )
            c.logs.update { data -> data.copy(docs = if (old == null) data.docs + d else data.docs.map { if (it.id == d.id) d else it }) }
            nav.back()
        },
        onDelete = old?.let { o -> { c.logs.update { d -> d.copy(docs = d.docs.filterNot { it.id == o.id }) }; nav.back() } },
    ) {
        ChoiceInput(stringResource(R.string.doc_type), DocType.entries.map { it to docTypeLabel(it) }, type) { type = it }
        TextInput(stringResource(R.string.doc_title), title, { title = it.take(80) }, sentences = true, placeholder = stringResource(R.string.doc_title_hint))
        DateInput(stringResource(R.string.doc_expiry), expiry, { expiry = it }, optional = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberInput(stringResource(R.string.doc_warn_days), warn, { warn = it }, Modifier.weight(1f), decimals = false, suffix = stringResource(R.string.days_short))
            NumberInput(stringResource(R.string.doc_cost), cost, { cost = it }, Modifier.weight(1f), suffix = fmt.currencySymbol)
        }
        FieldNote(stringResource(R.string.doc_note))
        PhotoStrip(stringResource(R.string.doc_photos), photos, { photos = it })
        TextInput(stringResource(R.string.notes), notes, { notes = it.take(2000) }, singleLine = false, minLines = 3, sentences = true)
    }
}

// ---------------------------------------------------------------- Notes

@Composable
fun NotesScreen(vehicleId: String) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val data by c.logs.data.collectAsStateWithLifecycle()
    val fmt = rememberFmt()
    val notes = data.notes.filter { it.vehicleId == vehicleId }.sortedByDescending { it.date }
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.notes_title)) {
            CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.note_add), onClick = { nav.navigate(NoteEditRoute(vehicleId)) })
        }
        if (notes.isEmpty()) {
            MessageView(
                Icons.AutoMirrored.Outlined.Notes,
                stringResource(R.string.notes_empty_title),
                stringResource(R.string.notes_empty_body),
                actionLabel = stringResource(R.string.note_add),
                onAction = { nav.navigate(NoteEditRoute(vehicleId)) },
            )
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
            notes.forEach { n ->
                item(key = n.id) {
                    CtrlCard(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 5.dp), onClick = { nav.navigate(NoteEditRoute(vehicleId, n.id)) }) {
                        SectionLabel(fmt.date(n.date))
                        Spacer(Modifier.size(6.dp))
                        Text(n.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun NoteEditScreen(vehicleId: String, id: String?) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val old = remember(id) { c.logs.data.value.notes.firstOrNull { it.id == id } }
    var date by rememberSaveable { mutableStateOf(old?.date ?: today()) }
    var text by rememberSaveable { mutableStateOf(old?.text ?: "") }
    FormScreen(
        title = stringResource(if (old == null) R.string.note_add else R.string.note_edit),
        canSave = text.isNotBlank(),
        onSave = {
            val n = VehicleNote(old?.id ?: newId(), vehicleId, date, text.trim())
            c.logs.update { d -> d.copy(notes = if (old == null) d.notes + n else d.notes.map { if (it.id == n.id) n else it }) }
            nav.back()
        },
        onDelete = old?.let { o -> { c.logs.update { d -> d.copy(notes = d.notes.filterNot { it.id == o.id }) }; nav.back() } },
    ) {
        DateInput(stringResource(R.string.date), date, { if (it != null) date = it })
        TextInput(stringResource(R.string.note_text), text, { text = it.take(8000) }, singleLine = false, minLines = 6, sentences = true)
    }
}

// ---------------------------------------------------------------- Costs

/** Total cost of ownership: per category, per year, and per km ridden. */
@Composable
fun CostsScreen(vehicleId: String) {
    val c = LocalContext.current.container
    val data by c.logs.data.collectAsStateWithLifecycle()
    val fmt = rememberFmt()
    val lines = remember(data) { data.costLines(vehicleId) }
    val total = lines.sumOf { it.amount }
    val running = lines.filter { it.category != CostCategory.PURCHASE }.sumOf { it.amount }
    val km = remember(data) { ridden(data.odoPoints(vehicleId)) }
    val byCat = CostCategory.entries.map { cat -> cat to lines.filter { it.category == cat }.sumOf { it.amount } }.filter { it.second > 0 }
    val byYear = lines.filter { it.category != CostCategory.PURCHASE && it.date != null }
        .groupBy { it.date!!.take(4) }.map { (y, l) -> y to l.sumOf { it.amount } }.sortedBy { it.first }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.costs_title))
        if (lines.isEmpty()) {
            MessageView(Icons.Outlined.Payments, stringResource(R.string.costs_empty_title), stringResource(R.string.costs_empty_body))
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                StatRow(
                    { m -> StatTile(stringResource(R.string.costs_total), fmt.money(total), m, sub = stringResource(R.string.costs_total_sub)) },
                    { m -> StatTile(stringResource(R.string.stat_running_cost), fmt.money(running), m, sub = stringResource(R.string.stat_running_cost_sub)) },
                )
                StatRow(
                    { m -> StatTile(stringResource(R.string.stat_cost_per_km), fmt.moneyPerDistance(if (km > 0) running / km else null), m, sub = stringResource(R.string.costs_per_km_sub)) },
                    { m -> StatTile(stringResource(R.string.odo_ridden), fmt.distance(km), m) },
                )
            }
            item {
                Group(stringResource(R.string.costs_by_category)) {
                    byCat.forEach { (cat, sum) ->
                        EntryRow(costLabel(cat), if (total > 0) "${fmt.num(sum / total * 100, 0)} %" else null, fmt.money(sum), onClick = {})
                    }
                }
            }
            if (byCat.size > 1) {
                item {
                    CtrlCard(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp)) {
                        SectionLabel(stringResource(R.string.costs_by_category))
                        Spacer(Modifier.size(10.dp))
                        BarChart(byCat.map { (cat, v) -> costLabel(cat).take(5) to v }, labelFor = { fmt.money(it).replace(Regex("[.,]\\d\\d(?=\\D*$)"), "") })
                    }
                }
            }
            if (byYear.isNotEmpty()) {
                item {
                    CtrlCard(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp)) {
                        SectionLabel(stringResource(R.string.costs_by_year))
                        Spacer(Modifier.size(10.dp))
                        BarChart(byYear, labelFor = { fmt.money(it).replace(Regex("[.,]\\d\\d(?=\\D*$)"), "") })
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.costs_note),
                    style = CtrlType.Data,
                    color = CtrlColors.Muted,
                    modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp),
                )
            }
        }
    }
}
