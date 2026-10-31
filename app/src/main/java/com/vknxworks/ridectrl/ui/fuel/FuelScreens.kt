package com.vknxworks.ridectrl.ui.fuel

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.LocalGasStation
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.container
import com.vknxworks.ridectrl.data.EngineType
import com.vknxworks.ridectrl.data.FuelEntry
import com.vknxworks.ridectrl.data.currentKm
import com.vknxworks.ridectrl.data.fuelStats
import com.vknxworks.ridectrl.data.newId
import com.vknxworks.ridectrl.data.oilFor
import com.vknxworks.ridectrl.data.parseInt
import com.vknxworks.ridectrl.data.parseNumber
import com.vknxworks.ridectrl.data.today
import com.vknxworks.ridectrl.ui.components.BarChart
import com.vknxworks.ridectrl.ui.components.CtrlCard
import com.vknxworks.ridectrl.ui.components.CtrlSquareButton
import com.vknxworks.ridectrl.ui.components.DateInput
import com.vknxworks.ridectrl.ui.components.EntryRow
import com.vknxworks.ridectrl.ui.components.FieldHeading
import com.vknxworks.ridectrl.ui.components.FieldNote
import com.vknxworks.ridectrl.ui.components.FormScreen
import com.vknxworks.ridectrl.ui.components.Group
import com.vknxworks.ridectrl.ui.components.LineChart
import com.vknxworks.ridectrl.ui.components.MessageView
import com.vknxworks.ridectrl.ui.components.NumberInput
import com.vknxworks.ridectrl.ui.components.ScreenTopBar
import com.vknxworks.ridectrl.ui.components.SectionLabel
import com.vknxworks.ridectrl.ui.components.StatRow
import com.vknxworks.ridectrl.ui.components.StatTile
import com.vknxworks.ridectrl.ui.components.SwitchInput
import com.vknxworks.ridectrl.ui.components.TextInput
import com.vknxworks.ridectrl.ui.components.rememberFmt
import com.vknxworks.ridectrl.ui.nav.FuelEditRoute
import com.vknxworks.ridectrl.ui.nav.LocalNavigator
import com.vknxworks.ridectrl.ui.nav.PremixRoute
import com.vknxworks.ridectrl.ui.theme.CtrlColors
import com.vknxworks.ridectrl.ui.theme.CtrlType
import com.vknxworks.ridectrl.ui.theme.Gutter

/** The fuel log: consumption, cost per km, spend per month and year, and every fill. */
@Composable
fun FuelScreen(vehicleId: String) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val data by c.logs.data.collectAsStateWithLifecycle()
    val fmt = rememberFmt()
    val vehicle = data.vehicles.firstOrNull { it.id == vehicleId }
    val entries = data.fuel.filter { it.vehicleId == vehicleId }.sortedWith(compareByDescending<FuelEntry> { it.date }.thenByDescending { it.odometer ?: 0.0 })
    val stats = remember(data) { data.fuelStats(vehicleId) }
    var yearly by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.fuel_title)) {
            if (vehicle?.engine == EngineType.TWO_STROKE_PREMIX) {
                CtrlSquareButton(Icons.Outlined.Calculate, stringResource(R.string.premix_title), onClick = { nav.navigate(PremixRoute(vehicleId)) })
            }
            CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.action_add_fuel), onClick = { nav.navigate(FuelEditRoute(vehicleId)) })
        }
        if (entries.isEmpty()) {
            MessageView(
                Icons.Outlined.LocalGasStation,
                stringResource(R.string.fuel_empty_title),
                stringResource(R.string.fuel_empty_body),
                actionLabel = stringResource(R.string.action_add_fuel),
                onAction = { nav.navigate(FuelEditRoute(vehicleId)) },
            )
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                StatRow(
                    { m -> StatTile(stringResource(R.string.stat_consumption), fmt.consumption(stats.avgPer100), m, sub = fmt.economy(stats.avgPer100)) },
                    { m -> StatTile(stringResource(R.string.fuel_last_fill_stat), fmt.consumption(stats.lastPer100), m, sub = fmt.economy(stats.lastPer100)) },
                )
                StatRow(
                    { m -> StatTile(stringResource(R.string.stat_cost_per_km), fmt.moneyPerDistance(stats.costPerKm), m, sub = stringResource(R.string.fuel_cost_per_km_sub)) },
                    { m -> StatTile(stringResource(R.string.fuel_total), fmt.money(stats.totalCost), m, sub = fmt.volume(stats.totalLitres, 1)) },
                )
                if (stats.segments.isEmpty()) {
                    Text(
                        stringResource(R.string.fuel_need_two_full),
                        style = MaterialTheme.typography.bodySmall,
                        color = CtrlColors.Muted,
                        modifier = Modifier.padding(horizontal = Gutter, vertical = 6.dp),
                    )
                }
            }
            if (stats.segments.size >= 2) {
                item {
                    CtrlCard(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp)) {
                        SectionLabel(stringResource(R.string.fuel_chart_consumption, fmt.consumptionUnit))
                        Spacer(Modifier.size(10.dp))
                        val values = stats.segments.map { it.litresPer100 }
                        LineChart(
                            values.map { fmt.consumptionValue(it) },
                            labelFor = { fmt.num(it, 1) },
                            startLabel = fmt.shortDate(stats.segments.first().to.date),
                            endLabel = fmt.shortDate(stats.segments.last().to.date),
                        )
                    }
                }
            }
            val spend = if (yearly) stats.yearly else stats.monthly.takeLast(12)
            if (spend.isNotEmpty()) {
                item {
                    CtrlCard(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp), onClick = { yearly = !yearly }) {
                        Row {
                            SectionLabel(stringResource(if (yearly) R.string.fuel_spend_yearly else R.string.fuel_spend_monthly), Modifier.weight(1f))
                            Text(stringResource(R.string.tap_to_switch), style = CtrlType.Status, color = CtrlColors.Muted)
                        }
                        Spacer(Modifier.size(10.dp))
                        BarChart(
                            spend.map { (k, v) -> (if (yearly) k else k.takeLast(2)) to v },
                            labelFor = { fmt.money(it).replace(Regex("[.,]\\d\\d(?=\\D*$)"), "") },
                        )
                    }
                }
            }
            item {
                Group(stringResource(R.string.fuel_fills)) {
                    entries.forEach { e ->
                        EntryRow(
                            title = listOfNotNull(fmt.date(e.date), e.station.ifBlank { null }).joinToString(" · "),
                            subtitle = listOfNotNull(
                                e.odometer?.let { fmt.distance(it) },
                                if (e.fullTank) stringResource(R.string.fuel_full) else stringResource(R.string.fuel_partial),
                                e.premixRatio?.let { "$it:1" },
                            ).joinToString(" · "),
                            value = listOfNotNull(fmt.volume(e.litres), e.total?.let { fmt.money(it) }).joinToString("\n"),
                            onClick = { nav.navigate(FuelEditRoute(vehicleId, e.id)) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Add or edit a fill. Type the price per litre or the total and the other is worked out. For premix
 * vehicles the oil to add is shown straight away from the ratio.
 */
@Composable
fun FuelEditScreen(vehicleId: String, id: String?) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val fmt = rememberFmt()
    val data = c.logs.data.value
    val vehicle = data.vehicles.firstOrNull { it.id == vehicleId }
    val old = remember(id) { data.fuel.firstOrNull { it.id == id } }
    val premix = vehicle?.engine == EngineType.TWO_STROKE_PREMIX

    var date by rememberSaveable { mutableStateOf(old?.date ?: today()) }
    var odo by rememberSaveable { mutableStateOf(fmt.distanceInput(old?.odometer)) }
    var litres by rememberSaveable { mutableStateOf(fmt.volumeInput(old?.litres)) }
    // Price per user volume unit (per litre with litres).
    var price by rememberSaveable { mutableStateOf(old?.pricePerLitre?.let { fmt.plain(it / fmt.litresToUser(1.0), 3) } ?: "") }
    var total by rememberSaveable { mutableStateOf(fmt.moneyInput(old?.total)) }
    var station by rememberSaveable { mutableStateOf(old?.station ?: "") }
    var full by rememberSaveable { mutableStateOf(old?.fullTank ?: true) }
    var ratio by rememberSaveable { mutableStateOf((old?.premixRatio ?: if (old == null && premix) vehicle?.premixRatio else null)?.toString() ?: "") }
    var oilCost by rememberSaveable { mutableStateOf(fmt.moneyInput(old?.oilCost)) }
    var notes by rememberSaveable { mutableStateOf(old?.notes ?: "") }
    // Which of price/total the user typed last; the other one follows.
    var lastTyped by rememberSaveable { mutableStateOf(if (old != null && old.total != null && old.pricePerLitre == null) "total" else "price") }

    val volUser = parseNumber(litres)
    val l = volUser?.let { fmt.userToLitres(it) }
    val priceUser = parseNumber(price)
    val totalV = parseNumber(total)
    val shownTotal = if (lastTyped == "price" && priceUser != null && volUser != null) priceUser * volUser else null
    val shownPrice = if (lastTyped == "total" && totalV != null && volUser != null && volUser > 0) totalV / volUser else null
    val r = parseInt(ratio)?.takeIf { it in 10..200 }
    val odoKm = parseNumber(odo)?.let { fmt.userToKm(it) }
    val current = data.currentKm(vehicleId)
    val goesBack = odoKm != null && current != null && old == null && odoKm < current
    val canSave = l != null && l > 0 && (odo.isBlank() || odoKm != null)

    FormScreen(
        title = stringResource(if (old == null) R.string.action_add_fuel else R.string.fuel_edit),
        canSave = canSave,
        onSave = {
            val finalTotal = totalV.takeIf { lastTyped == "total" } ?: shownTotal
            val finalPricePerUser = priceUser.takeIf { lastTyped == "price" } ?: shownPrice
            val entry = FuelEntry(
                id = old?.id ?: newId(),
                vehicleId = vehicleId,
                date = date,
                odometer = odoKm,
                litres = l!!,
                // Stored per litre whatever unit was typed.
                pricePerLitre = finalPricePerUser?.let { it * fmt.litresToUser(1.0) },
                total = finalTotal,
                station = station.trim(),
                fullTank = full,
                premixRatio = r,
                oilMl = r?.let { oilFor(l, it.toDouble()) },
                oilCost = parseNumber(oilCost),
                notes = notes.trim(),
            )
            c.logs.update { d -> d.copy(fuel = if (old == null) d.fuel + entry else d.fuel.map { if (it.id == entry.id) entry else it }) }
            nav.back()
        },
        onDelete = old?.let { o -> { c.logs.update { d -> d.copy(fuel = d.fuel.filterNot { it.id == o.id }) }; nav.back() } },
    ) {
        DateInput(stringResource(R.string.date), date, { if (it != null) date = it })
        NumberInput(stringResource(R.string.vehicle_odometer), odo, { odo = it }, suffix = fmt.distanceUnit, placeholder = current?.let { fmt.distanceInput(it) })
        if (goesBack) FieldNote(stringResource(R.string.odo_goes_back_warning, fmt.distance(current)))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberInput(stringResource(R.string.fuel_amount), litres, { litres = it }, Modifier.weight(1f), suffix = fmt.volumeUnit)
            NumberInput(
                stringResource(R.string.fuel_price_per, fmt.volumeUnit),
                if (lastTyped == "total" && shownPrice != null) fmt.plain(shownPrice, 3) else price,
                { price = it; lastTyped = "price" },
                Modifier.weight(1f),
                suffix = fmt.currencySymbol,
            )
        }
        NumberInput(
            stringResource(R.string.fuel_total_paid),
            if (lastTyped == "price" && shownTotal != null) fmt.plain(shownTotal, 2) else total,
            { total = it; lastTyped = "total" },
            suffix = fmt.currencySymbol,
        )
        SwitchInput(stringResource(R.string.fuel_full_tank), full, stringResource(R.string.fuel_full_tank_sub)) { full = it }
        TextInput(stringResource(R.string.fuel_station), station, { station = it.take(60) }, words = true)
        if (premix || old?.premixRatio != null) {
            FieldHeading(stringResource(R.string.premix_title))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberInput(stringResource(R.string.vehicle_ratio), ratio, { ratio = it }, Modifier.weight(1f), suffix = ":1", decimals = false)
                NumberInput(stringResource(R.string.fuel_oil_cost), oilCost, { oilCost = it }, Modifier.weight(1f), suffix = fmt.currencySymbol)
            }
            if (l != null && r != null) {
                CtrlCard(Modifier.fillMaxWidth()) {
                    SectionLabel(stringResource(R.string.premix_oil_to_add))
                    Text(stringResource(R.string.premix_ml, fmt.num(oilFor(l, r.toDouble()), 0)), style = CtrlType.Readout, color = CtrlColors.AccentText)
                    vehicle?.oilBrand?.takeIf { it.isNotBlank() }?.let { Text(it, style = CtrlType.Data, color = CtrlColors.Muted) }
                }
            }
        }
        TextInput(stringResource(R.string.notes), notes, { notes = it.take(1000) }, singleLine = false, minLines = 2, sentences = true)
    }
}
