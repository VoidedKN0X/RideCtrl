package com.vknxworks.ridectrl.ui.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.container
import com.vknxworks.ridectrl.data.CostCategory
import com.vknxworks.ridectrl.data.DueState
import com.vknxworks.ridectrl.data.EngineType
import com.vknxworks.ridectrl.data.costLines
import com.vknxworks.ridectrl.data.currentKm
import com.vknxworks.ridectrl.data.dueList
import com.vknxworks.ridectrl.data.fuelStats
import com.vknxworks.ridectrl.data.runningCostPerKm
import com.vknxworks.ridectrl.data.state
import com.vknxworks.ridectrl.ui.components.CtrlCard
import com.vknxworks.ridectrl.ui.components.CtrlHeroCard
import com.vknxworks.ridectrl.ui.components.CtrlSquareButton
import com.vknxworks.ridectrl.ui.components.DueTag
import com.vknxworks.ridectrl.ui.components.Group
import com.vknxworks.ridectrl.ui.components.GroupRow
import com.vknxworks.ridectrl.ui.components.GroupSwitch
import com.vknxworks.ridectrl.ui.components.MessageView
import com.vknxworks.ridectrl.ui.components.PhotoThumb
import com.vknxworks.ridectrl.ui.components.PhotoViewer
import com.vknxworks.ridectrl.ui.components.ScreenTopBar
import com.vknxworks.ridectrl.ui.components.SectionLabel
import com.vknxworks.ridectrl.ui.components.StatRow
import com.vknxworks.ridectrl.ui.components.StatTile
import com.vknxworks.ridectrl.ui.components.rememberFmt
import com.vknxworks.ridectrl.ui.nav.CostsRoute
import com.vknxworks.ridectrl.ui.nav.DocsRoute
import com.vknxworks.ridectrl.ui.nav.FuelEditRoute
import com.vknxworks.ridectrl.ui.nav.FuelRoute
import com.vknxworks.ridectrl.ui.nav.LocalNavigator
import com.vknxworks.ridectrl.ui.nav.MaintenanceRoute
import com.vknxworks.ridectrl.ui.nav.ModsRoute
import com.vknxworks.ridectrl.ui.nav.NoteEditRoute
import com.vknxworks.ridectrl.ui.nav.NotesRoute
import com.vknxworks.ridectrl.ui.nav.OdometerEditRoute
import com.vknxworks.ridectrl.ui.nav.OdometerRoute
import com.vknxworks.ridectrl.ui.nav.PartsRoute
import com.vknxworks.ridectrl.ui.nav.PremixRoute
import com.vknxworks.ridectrl.ui.nav.ServiceEditRoute
import com.vknxworks.ridectrl.ui.nav.SpecsRoute
import com.vknxworks.ridectrl.ui.nav.TempEditRoute
import com.vknxworks.ridectrl.ui.nav.TempsRoute
import com.vknxworks.ridectrl.ui.nav.VehicleEditRoute
import com.vknxworks.ridectrl.ui.theme.CtrlColors
import com.vknxworks.ridectrl.ui.theme.CtrlShapes
import com.vknxworks.ridectrl.ui.theme.CtrlType
import com.vknxworks.ridectrl.ui.theme.Gutter
import kotlinx.coroutines.launch

/** A vehicle's dashboard: odometer, what's due next, the key numbers, quick-add buttons and every log. */
@Composable
fun VehicleScreen(id: String) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val data by c.logs.data.collectAsStateWithLifecycle()
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    val fmt = rememberFmt()
    val v = data.vehicles.firstOrNull { it.id == id }
    if (v == null) {
        Column(Modifier.fillMaxSize()) {
            ScreenTopBar(stringResource(R.string.vehicle))
            MessageView(Icons.Outlined.Speed, stringResource(R.string.vehicle_gone))
        }
        return
    }
    var showVin by remember { mutableStateOf(false) }
    var viewPhoto by remember { mutableStateOf(false) }
    val due = remember(data, settings) { data.dueList(id, settings.warnDays, settings.warnKm) }
    val fuel = remember(data) { data.fuelStats(id) }
    val running = remember(data) { data.costLines(id).filter { it.category != CostCategory.PURCHASE }.sumOf { it.amount } }
    val perKm = remember(data) { data.runningCostPerKm(id) }
    val lastTemp = data.temps.filter { it.vehicleId == id }.maxByOrNull { it.time }
    val docsDue = data.docs.filter { it.vehicleId == id }.count { it.state() == DueState.SOON || it.state() == DueState.OVERDUE }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(v.name) {
            CtrlSquareButton(Icons.Outlined.Edit, stringResource(R.string.vehicle_edit), onClick = { nav.navigate(VehicleEditRoute(id)) })
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
            CtrlHeroCard(
                label = listOfNotNull(v.make.ifBlank { null }, v.model.ifBlank { null }, v.year?.toString()).joinToString(" ").ifBlank { engineLabel(v.engine) },
                meta = v.plate.ifBlank { null },
                onClick = { nav.navigate(OdometerRoute(id)) },
                modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel(stringResource(R.string.vehicle_odometer), color = CtrlColors.OnAccent)
                        Text(fmt.distance(data.currentKm(id)), style = CtrlType.Readout, maxLines = 1)
                    }
                    if (v.photo != null) {
                        Spacer(Modifier.width(12.dp))
                        PhotoThumb(v.photo, Modifier.size(92.dp), onClick = { viewPhoto = true })
                    }
                }
            }

            // Quick add
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Gutter, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickAdd(Icons.Outlined.LocalGasStation, stringResource(R.string.quick_fuel)) { nav.navigate(FuelEditRoute(id)) }
                QuickAdd(Icons.Outlined.Speed, stringResource(R.string.quick_odometer)) { nav.navigate(OdometerEditRoute(id)) }
                QuickAdd(Icons.Outlined.Build, stringResource(R.string.quick_service)) { nav.navigate(ServiceEditRoute(id)) }
                QuickAdd(Icons.Outlined.Thermostat, stringResource(R.string.quick_temp)) { nav.navigate(TempEditRoute(id)) }
                QuickAdd(Icons.AutoMirrored.Outlined.Notes, stringResource(R.string.quick_note)) { nav.navigate(NoteEditRoute(id)) }
            }

            // Next maintenance
            val upcoming = due.filter { it.state != DueState.UNKNOWN }.take(3)
            CtrlCard(
                Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp),
                onClick = { nav.navigate(MaintenanceRoute(id)) },
            ) {
                SectionLabel(stringResource(R.string.dash_next_maintenance))
                Spacer(Modifier.size(8.dp))
                if (upcoming.isEmpty()) {
                    Text(
                        stringResource(if (due.isEmpty()) R.string.dash_no_items else R.string.dash_set_intervals),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CtrlColors.Muted,
                    )
                }
                upcoming.forEach { d ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(d.item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOfNotNull(
                                    d.kmLeft?.let { if (it <= 0) stringResource(R.string.due_km_over, fmt.distance(-it)) else stringResource(R.string.due_km_left, fmt.distance(it)) },
                                    d.dueDate?.let { stringResource(R.string.due_on, fmt.date(it.toString())) },
                                ).joinToString(" · "),
                                style = CtrlType.Data,
                                color = CtrlColors.Muted,
                            )
                        }
                        DueTag(d.state)
                    }
                }
            }

            StatRow(
                { m -> StatTile(stringResource(R.string.stat_consumption), fmt.consumption(fuel.avgPer100), m, sub = fmt.economy(fuel.avgPer100)) },
                { m -> StatTile(stringResource(R.string.stat_cost_per_km), fmt.moneyPerDistance(perKm), m) },
            )
            StatRow(
                { m -> StatTile(stringResource(R.string.stat_running_cost), fmt.money(running), m, sub = stringResource(R.string.stat_running_cost_sub)) },
                { m -> StatTile(stringResource(R.string.stat_last_temp), fmt.temp(lastTemp?.tempC), m, sub = lastTemp?.let { fmt.dateTime(it.time) }) },
            )

            Group(stringResource(R.string.dash_logs)) {
                GroupRow(stringResource(R.string.fuel_title), fuel.lastPer100?.let { stringResource(R.string.fuel_last_consumption, fmt.consumption(it)) }, Icons.Outlined.LocalGasStation, showChevron = true, onClick = { nav.navigate(FuelRoute(id)) })
                GroupRow(
                    stringResource(R.string.maintenance_title),
                    when {
                        due.any { it.state == DueState.OVERDUE } -> stringResource(R.string.maintenance_overdue_count, due.count { it.state == DueState.OVERDUE })
                        due.any { it.state == DueState.SOON } -> stringResource(R.string.maintenance_soon_count, due.count { it.state == DueState.SOON })
                        else -> null
                    },
                    Icons.Outlined.Build,
                    showChevron = true,
                    onClick = { nav.navigate(MaintenanceRoute(id)) },
                )
                GroupRow(stringResource(R.string.mods_title), null, Icons.Outlined.Tune, showChevron = true, onClick = { nav.navigate(ModsRoute(id)) })
                GroupRow(stringResource(R.string.temps_title), null, Icons.Outlined.Thermostat, showChevron = true, onClick = { nav.navigate(TempsRoute(id)) })
                GroupRow(stringResource(R.string.odo_title), null, Icons.Outlined.Speed, showChevron = true, onClick = { nav.navigate(OdometerRoute(id)) })
                GroupRow(
                    stringResource(R.string.docs_title),
                    if (docsDue > 0) stringResource(R.string.docs_due_count, docsDue) else null,
                    Icons.Outlined.Description,
                    showChevron = true,
                    onClick = { nav.navigate(DocsRoute(id)) },
                )
                GroupRow(stringResource(R.string.notes_title), null, Icons.AutoMirrored.Outlined.Notes, showChevron = true, onClick = { nav.navigate(NotesRoute(id)) })
            }
            Group(stringResource(R.string.dash_vehicle)) {
                GroupRow(stringResource(R.string.specs_title), stringResource(R.string.specs_sub), Icons.Outlined.ListAlt, showChevron = true, onClick = { nav.navigate(SpecsRoute(id)) })
                GroupRow(stringResource(R.string.costs_title), fmt.money(data.costLines(id).sumOf { it.amount }), Icons.Outlined.Payments, showChevron = true, onClick = { nav.navigate(CostsRoute(id)) })
                GroupRow(stringResource(R.string.parts_title), null, Icons.Outlined.Inventory2, showChevron = true, onClick = { nav.navigate(PartsRoute) })
                if (v.engine == EngineType.TWO_STROKE_PREMIX) {
                    GroupRow(stringResource(R.string.premix_title), v.premixRatio?.let { "$it:1" + if (v.oilBrand.isNotBlank()) " · ${v.oilBrand}" else "" }, Icons.Outlined.Calculate, showChevron = true, onClick = { nav.navigate(PremixRoute(id)) })
                }
                if (v.vin.isNotBlank()) {
                    GroupRow(
                        stringResource(R.string.vehicle_vin),
                        if (showVin) v.vin else stringResource(R.string.vehicle_vin_hidden),
                        Icons.Outlined.Visibility,
                        onClick = { showVin = !showVin },
                    )
                }
                if (data.vehicles.size > 1) {
                    GroupSwitch(
                        stringResource(R.string.vehicle_main),
                        stringResource(R.string.vehicle_main_sub),
                        checked = settings.mainVehicleId == id || (settings.mainVehicleId == null && data.vehicles.minByOrNull { it.created }?.id == id),
                        icon = Icons.Outlined.Star,
                    ) { on -> c.appScope.launch { c.settings.update { it.copy(mainVehicleId = if (on) id else null) } } }
                }
            }
            if (v.notes.isNotBlank()) {
                CtrlCard(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp)) {
                    SectionLabel(stringResource(R.string.notes))
                    Spacer(Modifier.size(6.dp))
                    Text(v.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    if (viewPhoto && v.photo != null) PhotoViewer(v.photo, onDismiss = { viewPhoto = false })
}

@Composable
private fun QuickAdd(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .width(76.dp)
            .clip(CtrlShapes.Control)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CtrlShapes.Control)
                .background(CtrlColors.Surface)
                .border(1.dp, CtrlColors.ControlOutline, CtrlShapes.Control),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(24.dp), tint = CtrlColors.Text) }
        Spacer(Modifier.size(6.dp))
        Text(label, style = CtrlType.Status, color = CtrlColors.Muted, maxLines = 1)
    }
}
