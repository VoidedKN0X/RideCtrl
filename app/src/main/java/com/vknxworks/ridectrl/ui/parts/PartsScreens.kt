package com.vknxworks.ridectrl.ui.parts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.container
import com.vknxworks.ridectrl.data.Mod
import com.vknxworks.ridectrl.data.Part
import com.vknxworks.ridectrl.data.SPEC_KEYS
import com.vknxworks.ridectrl.data.SpecField
import com.vknxworks.ridectrl.data.currentKm
import com.vknxworks.ridectrl.data.newId
import com.vknxworks.ridectrl.data.parseInt
import com.vknxworks.ridectrl.data.parseNumber
import com.vknxworks.ridectrl.data.partsCost
import com.vknxworks.ridectrl.data.today
import com.vknxworks.ridectrl.data.withStockChange
import com.vknxworks.ridectrl.ui.components.CtrlButton
import com.vknxworks.ridectrl.ui.components.CtrlCard
import com.vknxworks.ridectrl.ui.components.CtrlSquareButton
import com.vknxworks.ridectrl.ui.components.DateInput
import com.vknxworks.ridectrl.ui.components.EntryRow
import com.vknxworks.ridectrl.ui.components.FieldHeading
import com.vknxworks.ridectrl.ui.components.FieldNote
import com.vknxworks.ridectrl.ui.components.FormScreen
import com.vknxworks.ridectrl.ui.components.Group
import com.vknxworks.ridectrl.ui.components.MessageView
import com.vknxworks.ridectrl.ui.components.MultiSelectInput
import com.vknxworks.ridectrl.ui.components.NumberInput
import com.vknxworks.ridectrl.ui.components.PartUsesInput
import com.vknxworks.ridectrl.ui.components.PhotoStrip
import com.vknxworks.ridectrl.ui.components.PhotoThumb
import com.vknxworks.ridectrl.ui.components.ScreenTopBar
import com.vknxworks.ridectrl.ui.components.SectionLabel
import com.vknxworks.ridectrl.ui.components.StatusTag
import com.vknxworks.ridectrl.ui.components.SwitchInput
import com.vknxworks.ridectrl.ui.components.TextInput
import com.vknxworks.ridectrl.ui.components.rememberFmt
import com.vknxworks.ridectrl.ui.nav.LocalNavigator
import com.vknxworks.ridectrl.ui.nav.ModEditRoute
import com.vknxworks.ridectrl.ui.nav.PartEditRoute
import com.vknxworks.ridectrl.ui.theme.CtrlColors
import com.vknxworks.ridectrl.ui.theme.Gutter

fun Part.isLow(): Boolean = lowStockAt != null && quantity <= lowStockAt

/** The parts inventory, shared by every vehicle: low-stock parts first. */
@Composable
fun PartsScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val data by c.logs.data.collectAsStateWithLifecycle()
    val fmt = rememberFmt()
    val names = data.vehicles.associate { it.id to it.name }
    val parts = data.parts.sortedWith(compareByDescending<Part> { it.isLow() }.thenBy { it.name.lowercase() })

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.parts_title)) {
            CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.part_add), onClick = { nav.navigate(PartEditRoute()) })
        }
        if (parts.isEmpty()) {
            MessageView(
                Icons.Outlined.Inventory2,
                stringResource(R.string.parts_empty_title),
                stringResource(R.string.parts_empty_body),
                actionLabel = stringResource(R.string.part_add),
                onAction = { nav.navigate(PartEditRoute()) },
            )
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Group {
                    parts.forEach { p ->
                        EntryRow(
                            title = p.name,
                            subtitle = listOfNotNull(
                                listOf(p.brand, p.partNumber).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null },
                                p.vehicleIds.mapNotNull { names[it] }.joinToString(", ").ifBlank { stringResource(R.string.part_fits_any) },
                            ).joinToString("\n"),
                            value = null,
                            onClick = { nav.navigate(PartEditRoute(p.id)) },
                            trailing = {
                                StatusTag(
                                    stringResource(R.string.part_stock_tag, p.quantity),
                                    active = !p.isLow(),
                                    color = if (p.isLow()) CtrlColors.Warning else CtrlColors.AccentText,
                                )
                            },
                        )
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.parts_total_value, fmt.money(data.parts.sumOf { (it.price ?: 0.0) * it.quantity })),
                    style = MaterialTheme.typography.bodySmall,
                    color = CtrlColors.Muted,
                    modifier = Modifier.padding(horizontal = Gutter, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
fun PartEditScreen(id: String?, vehicleId: String?) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val fmt = rememberFmt()
    val data = c.logs.data.value
    val old = remember(id) { data.parts.firstOrNull { it.id == id } }

    var name by rememberSaveable { mutableStateOf(old?.name ?: "") }
    var brand by rememberSaveable { mutableStateOf(old?.brand ?: "") }
    var number by rememberSaveable { mutableStateOf(old?.partNumber ?: "") }
    var where by rememberSaveable { mutableStateOf(old?.whereBought ?: "") }
    var price by rememberSaveable { mutableStateOf(fmt.moneyInput(old?.price)) }
    var qty by rememberSaveable { mutableStateOf((old?.quantity ?: 1).toString()) }
    var fits by remember { mutableStateOf(old?.vehicleIds?.toSet() ?: setOfNotNull(vehicleId)) }
    var lowOn by rememberSaveable { mutableStateOf(old?.lowStockAt != null) }
    var lowAt by rememberSaveable { mutableStateOf((old?.lowStockAt ?: 1).toString()) }
    var notes by rememberSaveable { mutableStateOf(old?.notes ?: "") }

    val q = parseInt(qty)
    FormScreen(
        title = stringResource(if (old == null) R.string.part_add else R.string.part_edit),
        canSave = name.isNotBlank() && q != null && q >= 0,
        onSave = {
            val p = Part(
                id = old?.id ?: newId(),
                name = name.trim(),
                brand = brand.trim(),
                partNumber = number.trim(),
                whereBought = where.trim(),
                price = parseNumber(price),
                quantity = q ?: 0,
                vehicleIds = data.vehicles.filter { it.id in fits }.map { it.id },
                lowStockAt = if (lowOn) parseInt(lowAt)?.coerceAtLeast(0) else null,
                notes = notes.trim(),
            )
            c.logs.update { d -> d.copy(parts = if (old == null) d.parts + p else d.parts.map { if (it.id == p.id) p else it }) }
            nav.back()
        },
        onDelete = old?.let { o ->
            {
                // Records keep their history; the part just no longer counts towards stock or costs.
                c.logs.update { d -> d.copy(parts = d.parts.filterNot { it.id == o.id }) }
                nav.back()
            }
        },
        deleteBody = stringResource(R.string.part_delete_body),
    ) {
        TextInput(stringResource(R.string.part_name), name, { name = it.take(80) }, sentences = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextInput(stringResource(R.string.part_brand), brand, { brand = it.take(40) }, Modifier.weight(1f), words = true)
            TextInput(stringResource(R.string.part_number), number, { number = it.take(40) }, Modifier.weight(1f))
        }
        TextInput(stringResource(R.string.part_where), where, { where = it.take(80) }, words = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberInput(stringResource(R.string.part_price), price, { price = it }, Modifier.weight(1f), suffix = fmt.currencySymbol)
            NumberInput(stringResource(R.string.part_quantity), qty, { qty = it }, Modifier.weight(1f), decimals = false)
        }
        MultiSelectInput(stringResource(R.string.part_fits), data.vehicles.map { it.id to it.name }, fits, stringResource(R.string.part_fits_any)) { fits = it }
        SwitchInput(stringResource(R.string.part_low_stock), lowOn, stringResource(R.string.part_low_stock_sub)) { lowOn = it }
        if (lowOn) NumberInput(stringResource(R.string.part_low_at), lowAt, { lowAt = it }, decimals = false)
        TextInput(stringResource(R.string.notes), notes, { notes = it.take(1000) }, singleLine = false, minLines = 2, sentences = true)
    }
}

/** The mods log: what changed on the vehicle, and how it rides since. */
@Composable
fun ModsScreen(vehicleId: String) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val data by c.logs.data.collectAsStateWithLifecycle()
    val fmt = rememberFmt()
    val mods = data.mods.filter { it.vehicleId == vehicleId }.sortedByDescending { it.date }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.mods_title)) {
            CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.mod_add), onClick = { nav.navigate(ModEditRoute(vehicleId)) })
        }
        if (mods.isEmpty()) {
            MessageView(
                Icons.Outlined.Tune,
                stringResource(R.string.mods_empty_title),
                stringResource(R.string.mods_empty_body),
                actionLabel = stringResource(R.string.mod_add),
                onAction = { nav.navigate(ModEditRoute(vehicleId)) },
            )
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
            mods.forEach { m ->
                item(key = m.id) {
                    CtrlCard(
                        Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 5.dp),
                        onClick = { nav.navigate(ModEditRoute(vehicleId, m.id)) },
                    ) {
                        SectionLabel(listOfNotNull(fmt.date(m.date), m.odometer?.let { fmt.distance(it) }).joinToString(" · "))
                        Spacer(Modifier.size(6.dp))
                        Text(m.title, style = MaterialTheme.typography.titleMedium)
                        if (m.description.isNotBlank()) Text(m.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                        val cost = (m.cost ?: 0.0) + data.partsCost(m.parts)
                        if (cost > 0) Text(fmt.money(cost), style = MaterialTheme.typography.bodySmall, color = CtrlColors.Muted)
                        val photos = m.beforePhotos + m.afterPhotos
                        if (photos.isNotEmpty()) {
                            Spacer(Modifier.size(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                photos.take(4).forEach { PhotoThumb(it, Modifier.size(56.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModEditScreen(vehicleId: String, id: String?) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val fmt = rememberFmt()
    val data = c.logs.data.value
    val old = remember(id) { data.mods.firstOrNull { it.id == id } }
    val parts = data.parts.filter { it.vehicleIds.isEmpty() || vehicleId in it.vehicleIds }

    var date by rememberSaveable { mutableStateOf(old?.date ?: today()) }
    var odo by rememberSaveable { mutableStateOf(fmt.distanceInput(old?.odometer ?: if (old == null) data.currentKm(vehicleId) else null)) }
    var title by rememberSaveable { mutableStateOf(old?.title ?: "") }
    var desc by rememberSaveable { mutableStateOf(old?.description ?: "") }
    var used by remember { mutableStateOf(old?.parts ?: emptyList()) }
    var cost by rememberSaveable { mutableStateOf(fmt.moneyInput(old?.cost)) }
    var before by remember { mutableStateOf(old?.beforePhotos ?: emptyList()) }
    var after by remember { mutableStateOf(old?.afterPhotos ?: emptyList()) }
    var tuning by rememberSaveable { mutableStateOf(old?.tuningNotes ?: "") }

    val odoKm = parseNumber(odo)?.let { fmt.userToKm(it) }
    FormScreen(
        title = stringResource(if (old == null) R.string.mod_add else R.string.mod_edit),
        canSave = title.isNotBlank() && (odo.isBlank() || odoKm != null),
        onSave = {
            val m = Mod(
                id = old?.id ?: newId(),
                vehicleId = vehicleId,
                date = date,
                odometer = odoKm,
                title = title.trim(),
                description = desc.trim(),
                parts = used,
                cost = parseNumber(cost),
                beforePhotos = before,
                afterPhotos = after,
                tuningNotes = tuning.trim(),
            )
            c.logs.update { d ->
                d.withStockChange(old?.parts ?: emptyList(), used)
                    .let { n -> n.copy(mods = if (old == null) n.mods + m else n.mods.map { if (it.id == m.id) m else it }) }
            }
            nav.back()
        },
        onDelete = old?.let { o ->
            {
                c.logs.update { d -> d.withStockChange(o.parts, emptyList()).let { n -> n.copy(mods = n.mods.filterNot { it.id == o.id }) } }
                nav.back()
            }
        },
    ) {
        TextInput(stringResource(R.string.mod_title_field), title, { title = it.take(80) }, sentences = true, placeholder = stringResource(R.string.mod_title_hint))
        DateInput(stringResource(R.string.date), date, { if (it != null) date = it })
        NumberInput(stringResource(R.string.vehicle_odometer), odo, { odo = it }, suffix = fmt.distanceUnit)
        TextInput(stringResource(R.string.mod_description), desc, { desc = it.take(4000) }, singleLine = false, minLines = 3, sentences = true)
        PartUsesInput(stringResource(R.string.service_parts_used), parts, used) { used = it }
        NumberInput(stringResource(R.string.mod_other_cost), cost, { cost = it }, suffix = fmt.currencySymbol)
        PhotoStrip(stringResource(R.string.mod_before), before, { before = it })
        PhotoStrip(stringResource(R.string.mod_after), after, { after = it })
        TextInput(stringResource(R.string.mod_tuning), tuning, { tuning = it.take(4000) }, singleLine = false, minLines = 3, sentences = true, placeholder = stringResource(R.string.mod_tuning_hint))
    }
}

/**
 * The specs sheet: the user's own values (tyres, plug, jetting, rollers...), each with where it came from.
 * Nothing is pre-filled or presented as official.
 */
@Composable
fun SpecsScreen(vehicleId: String) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val vehicle = c.logs.data.value.vehicles.firstOrNull { it.id == vehicleId } ?: return
    val values = remember { mutableStateMapOf<String, String>().apply { vehicle.specs.forEach { put(it.key, it.value) } } }
    val sources = remember { mutableStateMapOf<String, String>().apply { vehicle.specs.forEach { put(it.key, it.source) } } }
    var dirty by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenTopBar(stringResource(R.string.specs_title))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Gutter).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FieldNote(stringResource(R.string.specs_note))
            SPEC_KEYS.forEach { (key, label) ->
                FieldHeading(stringResource(label))
                TextInput(stringResource(R.string.spec_value), values[key] ?: "", { values[key] = it.take(400); dirty = true }, singleLine = key != "torques" && key != "other", minLines = if (key == "torques" || key == "other") 3 else 1)
                TextInput(stringResource(R.string.spec_source), sources[key] ?: "", { sources[key] = it.take(200); dirty = true }, placeholder = stringResource(R.string.spec_source_hint))
            }
        }
        CtrlButton(
            stringResource(R.string.save),
            onClick = {
                val specs = SPEC_KEYS.map { (k, _) -> SpecField(k, (values[k] ?: "").trim(), (sources[k] ?: "").trim()) }
                    .filter { it.value.isNotBlank() || it.source.isNotBlank() }
                c.logs.update { d -> d.copy(vehicles = d.vehicles.map { if (it.id == vehicleId) it.copy(specs = specs) else it }) }
                nav.back()
            },
            enabled = dirty,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Gutter, vertical = 12.dp),
        )
    }
}
