package com.vknxworks.ridectrl.ui.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.PlaylistAdd
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
import com.vknxworks.ridectrl.data.AppData
import com.vknxworks.ridectrl.data.DoneBy
import com.vknxworks.ridectrl.data.DueState
import com.vknxworks.ridectrl.data.PartUse
import com.vknxworks.ridectrl.data.SCOOTER_TEMPLATE
import com.vknxworks.ridectrl.data.ServiceItem
import com.vknxworks.ridectrl.data.ServiceRecord
import com.vknxworks.ridectrl.data.currentKm
import com.vknxworks.ridectrl.data.dueList
import com.vknxworks.ridectrl.data.newId
import com.vknxworks.ridectrl.data.parseInt
import com.vknxworks.ridectrl.data.parseNumber
import com.vknxworks.ridectrl.data.partsCost
import com.vknxworks.ridectrl.data.today
import com.vknxworks.ridectrl.data.withStockChange
import com.vknxworks.ridectrl.ui.components.ChoiceInput
import com.vknxworks.ridectrl.ui.components.CtrlSquareButton
import com.vknxworks.ridectrl.ui.components.DateInput
import com.vknxworks.ridectrl.ui.components.DueTag
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
import com.vknxworks.ridectrl.ui.components.ScreenTopBar
import com.vknxworks.ridectrl.ui.components.TextInput
import com.vknxworks.ridectrl.ui.components.rememberFmt
import com.vknxworks.ridectrl.ui.nav.LocalNavigator
import com.vknxworks.ridectrl.ui.nav.ServiceEditRoute
import com.vknxworks.ridectrl.ui.nav.ServiceItemEditRoute
import com.vknxworks.ridectrl.ui.theme.CtrlColors
import com.vknxworks.ridectrl.ui.theme.Gutter

/**
 * Maintenance for one vehicle: every service item with its state (OK, soon, overdue), then the service
 * history. Intervals are the user's own; nothing here is a manufacturer figure.
 */
@Composable
fun MaintenanceScreen(vehicleId: String) {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val data by c.logs.data.collectAsStateWithLifecycle()
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    val fmt = rememberFmt()
    val due = remember(data, settings) { data.dueList(vehicleId, settings.warnDays, settings.warnKm) }
    val records = data.services.filter { it.vehicleId == vehicleId }.sortedWith(compareByDescending<ServiceRecord> { it.date }.thenByDescending { it.odometer ?: 0.0 })
    val itemNames = data.serviceItems.associate { it.id to it.name }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.maintenance_title)) {
            CtrlSquareButton(Icons.Outlined.PlaylistAdd, stringResource(R.string.item_add), onClick = { nav.navigate(ServiceItemEditRoute(vehicleId)) })
            CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.service_add), onClick = { nav.navigate(ServiceEditRoute(vehicleId)) })
        }
        if (due.isEmpty() && records.isEmpty()) {
            MessageView(
                Icons.Outlined.Build,
                stringResource(R.string.maintenance_empty_title),
                stringResource(R.string.maintenance_empty_body),
                actionLabel = stringResource(R.string.maintenance_add_template),
                onAction = {
                    c.logs.update { d ->
                        d.copy(serviceItems = d.serviceItems + SCOOTER_TEMPLATE.map { (n, note) -> ServiceItem(newId(), vehicleId, context.getString(n), notes = note?.let { context.getString(it) } ?: "") })
                    }
                },
            )
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Text(
                    stringResource(R.string.maintenance_intro, settings.warnDays, fmt.distance(settings.warnKm)),
                    style = MaterialTheme.typography.bodySmall,
                    color = CtrlColors.Muted,
                    modifier = Modifier.padding(horizontal = Gutter, vertical = 4.dp),
                )
            }
            item {
                Group(stringResource(R.string.maintenance_items)) {
                    due.forEach { d ->
                        val interval = listOfNotNull(
                            d.item.intervalKm?.let { stringResource(R.string.every_km, fmt.distance(it)) },
                            d.item.intervalDays?.let { pluralStringResource(R.plurals.every_days, it, it) },
                        ).joinToString(" · ").ifBlank { stringResource(R.string.no_interval) }
                        val status = when {
                            d.state == DueState.UNKNOWN && (d.item.intervalKm != null || d.item.intervalDays != null) -> stringResource(R.string.never_logged)
                            else -> listOfNotNull(
                                d.kmLeft?.let { if (it <= 0) stringResource(R.string.due_km_over, fmt.distance(-it)) else stringResource(R.string.due_km_left, fmt.distance(it)) },
                                d.dueDate?.let { stringResource(R.string.due_on, fmt.date(it.toString())) },
                            ).joinToString(" · ")
                        }
                        EntryRow(
                            title = d.item.name,
                            subtitle = listOf(interval, status).filter { it.isNotBlank() }.joinToString("\n"),
                            value = null,
                            onClick = { nav.navigate(ServiceItemEditRoute(vehicleId, d.item.id)) },
                            trailing = { DueTag(d.state) },
                        )
                    }
                }
            }
            item {
                Group(stringResource(R.string.maintenance_history)) {
                    if (records.isEmpty()) {
                        EntryRow(stringResource(R.string.service_none), stringResource(R.string.service_none_sub), null, onClick = { nav.navigate(ServiceEditRoute(vehicleId)) })
                    }
                    records.forEach { r ->
                        val cost = (r.labourCost ?: 0.0) + (r.otherPartsCost ?: 0.0) + data.partsCost(r.parts)
                        EntryRow(
                            title = listOfNotNull(fmt.date(r.date), r.odometer?.let { fmt.distance(it) }).joinToString(" · "),
                            subtitle = r.itemIds.mapNotNull { itemNames[it] }.joinToString(", ").ifBlank { r.notes.take(80) },
                            value = if (cost > 0) fmt.money(cost) else null,
                            onClick = { nav.navigate(ServiceEditRoute(vehicleId, r.id)) },
                        )
                    }
                }
            }
        }
    }
}

/** A service item: name, intervals (km and/or days, both optional), when it was last done before RideCtrl. */
@Composable
fun ServiceItemEditScreen(vehicleId: String, id: String?) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val fmt = rememberFmt()
    val data = c.logs.data.value
    val old = remember(id) { data.serviceItems.firstOrNull { it.id == id } }

    var name by rememberSaveable { mutableStateOf(old?.name ?: "") }
    var km by rememberSaveable { mutableStateOf(fmt.distanceInput(old?.intervalKm)) }
    var months by rememberSaveable { mutableStateOf(old?.intervalDays?.let { if (it % 30 == 0) (it / 30).toString() else "" } ?: "") }
    var days by rememberSaveable { mutableStateOf(old?.intervalDays?.let { if (it % 30 != 0) it.toString() else "" } ?: "") }
    var baseDate by rememberSaveable { mutableStateOf(old?.baselineDate) }
    var baseKm by rememberSaveable { mutableStateOf(fmt.distanceInput(old?.baselineKm)) }
    var notes by rememberSaveable { mutableStateOf(old?.notes ?: "") }

    val intervalDays = parseInt(days) ?: parseInt(months)?.let { it * 30 }
    FormScreen(
        title = stringResource(if (old == null) R.string.item_add else R.string.item_edit),
        canSave = name.isNotBlank(),
        onSave = {
            val item = ServiceItem(
                id = old?.id ?: newId(),
                vehicleId = vehicleId,
                name = name.trim(),
                intervalKm = parseNumber(km)?.takeIf { it > 0 }?.let { fmt.userToKm(it) },
                intervalDays = intervalDays?.takeIf { it > 0 },
                notes = notes.trim(),
                baselineDate = baseDate,
                baselineKm = parseNumber(baseKm)?.let { fmt.userToKm(it) },
            )
            c.logs.update { d -> d.copy(serviceItems = if (old == null) d.serviceItems + item else d.serviceItems.map { if (it.id == item.id) item else it }) }
            nav.back()
        },
        onDelete = old?.let { o ->
            {
                c.logs.update { d ->
                    d.copy(
                        serviceItems = d.serviceItems.filterNot { it.id == o.id },
                        services = d.services.map { s -> if (o.id in s.itemIds) s.copy(itemIds = s.itemIds - o.id) else s },
                    )
                }
                nav.back()
            }
        },
    ) {
        TextInput(stringResource(R.string.item_name), name, { name = it.take(60) }, sentences = true)
        FieldHeading(stringResource(R.string.item_interval))
        NumberInput(stringResource(R.string.item_every_distance), km, { km = it }, suffix = fmt.distanceUnit)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberInput(stringResource(R.string.item_every_months), months, { months = it; if (it.isNotBlank()) days = "" }, Modifier.weight(1f), decimals = false)
            NumberInput(stringResource(R.string.item_or_days), days, { days = it; if (it.isNotBlank()) months = "" }, Modifier.weight(1f), decimals = false)
        }
        FieldNote(stringResource(R.string.item_interval_note))
        FieldHeading(stringResource(R.string.item_last_done))
        DateInput(stringResource(R.string.date), baseDate, { baseDate = it }, optional = true)
        NumberInput(stringResource(R.string.vehicle_odometer), baseKm, { baseKm = it }, suffix = fmt.distanceUnit)
        FieldNote(stringResource(R.string.item_last_done_note))
        TextInput(stringResource(R.string.notes), notes, { notes = it.take(1000) }, singleLine = false, minLines = 2, sentences = true)
    }
}

/**
 * A service record: what was done, with which parts, what it cost and who did it. Saving it resets the
 * "next due" of every item done, and takes the inventory parts used out of stock.
 */
@Composable
fun ServiceEditScreen(vehicleId: String, id: String?, itemId: String?) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val fmt = rememberFmt()
    val data = c.logs.data.value
    val old = remember(id) { data.services.firstOrNull { it.id == id } }
    val items = data.serviceItems.filter { it.vehicleId == vehicleId }
    val parts = data.parts.filter { it.vehicleIds.isEmpty() || vehicleId in it.vehicleIds }

    var date by rememberSaveable { mutableStateOf(old?.date ?: today()) }
    var odo by rememberSaveable { mutableStateOf(fmt.distanceInput(old?.odometer ?: if (old == null) data.currentKm(vehicleId) else null)) }
    var done by remember { mutableStateOf(old?.itemIds?.toSet() ?: setOfNotNull(itemId)) }
    var used by remember { mutableStateOf(old?.parts ?: emptyList()) }
    var otherParts by rememberSaveable { mutableStateOf(fmt.moneyInput(old?.otherPartsCost)) }
    var labour by rememberSaveable { mutableStateOf(fmt.moneyInput(old?.labourCost)) }
    var doneBy by rememberSaveable { mutableStateOf(old?.doneBy ?: DoneBy.ME) }
    var shop by rememberSaveable { mutableStateOf(old?.shop ?: "") }
    var photos by remember { mutableStateOf(old?.photos ?: emptyList()) }
    var notes by rememberSaveable { mutableStateOf(old?.notes ?: "") }

    val odoKm = parseNumber(odo)?.let { fmt.userToKm(it) }
    val total = (parseNumber(labour) ?: 0.0) + (parseNumber(otherParts) ?: 0.0) + data.partsCost(used)
    FormScreen(
        title = stringResource(if (old == null) R.string.service_add else R.string.service_edit),
        canSave = (done.isNotEmpty() || notes.isNotBlank()) && (odo.isBlank() || odoKm != null),
        onSave = {
            val rec = ServiceRecord(
                id = old?.id ?: newId(),
                vehicleId = vehicleId,
                date = date,
                odometer = odoKm,
                itemIds = items.filter { it.id in done }.map { it.id },
                parts = used,
                otherPartsCost = parseNumber(otherParts),
                labourCost = parseNumber(labour),
                doneBy = doneBy,
                shop = if (doneBy == DoneBy.SHOP) shop.trim() else "",
                photos = photos,
                notes = notes.trim(),
            )
            c.logs.update { d ->
                d.withStockChange(old?.parts ?: emptyList(), used)
                    .let { n -> n.copy(services = if (old == null) n.services + rec else n.services.map { if (it.id == rec.id) rec else it }) }
            }
            nav.back()
        },
        onDelete = old?.let { o ->
            {
                c.logs.update { d -> d.withStockChange(o.parts, emptyList()).let { n -> n.copy(services = n.services.filterNot { it.id == o.id }) } }
                nav.back()
            }
        },
        deleteBody = stringResource(R.string.service_delete_body),
    ) {
        DateInput(stringResource(R.string.date), date, { if (it != null) date = it })
        NumberInput(stringResource(R.string.vehicle_odometer), odo, { odo = it }, suffix = fmt.distanceUnit)
        MultiSelectInput(
            stringResource(R.string.service_items_done),
            items.map { it.id to it.name },
            done,
            stringResource(R.string.service_no_items),
        ) { done = it }
        if (items.isEmpty()) FieldNote(stringResource(R.string.service_items_hint))
        PartUsesInput(stringResource(R.string.service_parts_used), parts, used) { used = it }
        FieldHeading(stringResource(R.string.service_cost))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberInput(stringResource(R.string.service_other_parts), otherParts, { otherParts = it }, Modifier.weight(1f), suffix = fmt.currencySymbol)
            NumberInput(stringResource(R.string.service_labour), labour, { labour = it }, Modifier.weight(1f), suffix = fmt.currencySymbol)
        }
        FieldNote(stringResource(R.string.service_cost_note, fmt.money(total)))
        ChoiceInput(
            stringResource(R.string.service_done_by),
            listOf(DoneBy.ME to stringResource(R.string.done_by_me), DoneBy.SHOP to stringResource(R.string.done_by_shop)),
            doneBy,
        ) { doneBy = it }
        if (doneBy == DoneBy.SHOP) TextInput(stringResource(R.string.service_shop), shop, { shop = it.take(60) }, words = true)
        PhotoStrip(stringResource(R.string.photos), photos, { photos = it })
        TextInput(stringResource(R.string.notes), notes, { notes = it.take(2000) }, singleLine = false, minLines = 3, sentences = true)
    }
}
