package com.vknxworks.ridectrl.ui.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.container
import com.vknxworks.ridectrl.data.DueState
import com.vknxworks.ridectrl.data.Vehicle
import com.vknxworks.ridectrl.data.currentKm
import com.vknxworks.ridectrl.data.dueList
import com.vknxworks.ridectrl.ui.components.CtrlCard
import com.vknxworks.ridectrl.ui.components.CtrlHeroCard
import com.vknxworks.ridectrl.ui.components.CtrlSquareButton
import com.vknxworks.ridectrl.ui.components.DueTag
import com.vknxworks.ridectrl.ui.components.LocalBottomClearance
import com.vknxworks.ridectrl.ui.components.MessageView
import com.vknxworks.ridectrl.ui.components.PhotoThumb
import com.vknxworks.ridectrl.ui.components.TabTitle
import com.vknxworks.ridectrl.ui.components.rememberFmt
import com.vknxworks.ridectrl.ui.nav.EconomyRoute
import com.vknxworks.ridectrl.ui.nav.LocalNavigator
import com.vknxworks.ridectrl.ui.nav.VehicleEditRoute
import com.vknxworks.ridectrl.ui.nav.VehicleRoute
import com.vknxworks.ridectrl.ui.theme.CtrlColors
import com.vknxworks.ridectrl.ui.theme.CtrlType
import com.vknxworks.ridectrl.ui.theme.Gutter

/** The Garage tab: every vehicle, each opening its own dashboard. The main vehicle is the hero card. */
@Composable
fun GarageScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val data by c.logs.data.collectAsStateWithLifecycle()
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    val fmt = rememberFmt()
    val vehicles = data.vehicles.sortedWith(compareByDescending<Vehicle> { it.id == settings.mainVehicleId }.thenBy { it.created })

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        if (vehicles.isEmpty()) {
            TabTitle(stringResource(R.string.tab_garage)) {
                CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.vehicle_add), onClick = { nav.navigate(VehicleEditRoute()) })
            }
            MessageView(
                Icons.Outlined.TwoWheeler,
                stringResource(R.string.garage_empty_title),
                stringResource(R.string.garage_empty_body),
                modifier = Modifier.padding(bottom = LocalBottomClearance.current),
                actionLabel = stringResource(R.string.vehicle_add),
                onAction = { nav.navigate(VehicleEditRoute()) },
            )
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp + LocalBottomClearance.current),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "title") {
                TabTitle(
                    stringResource(R.string.tab_garage),
                    label = pluralStringResource(R.plurals.vehicle_count, vehicles.size, vehicles.size),
                ) {
                    if (data.fuel.isNotEmpty()) {
                        CtrlSquareButton(Icons.AutoMirrored.Outlined.ShowChart, stringResource(R.string.economy_title), onClick = { nav.navigate(EconomyRoute) })
                    }
                    CtrlSquareButton(Icons.Outlined.Add, stringResource(R.string.vehicle_add), onClick = { nav.navigate(VehicleEditRoute()) })
                }
            }
            val first = vehicles.first()
            item(key = "hero-${first.id}") {
                val due = data.dueList(first.id, settings.warnDays, settings.warnKm).firstOrNull { it.state != DueState.UNKNOWN }
                CtrlHeroCard(
                    label = listOfNotNull(first.make.ifBlank { null }, first.model.ifBlank { null }, first.year?.toString()).joinToString(" ").ifBlank { stringResource(R.string.vehicle) },
                    meta = first.plate.ifBlank { null },
                    onClick = { nav.navigate(VehicleRoute(first.id)) },
                    modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(first.name, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(fmt.distance(data.currentKm(first.id)), style = CtrlType.Readout)
                        }
                        if (first.photo != null) {
                            Spacer(Modifier.width(12.dp))
                            PhotoThumb(first.photo, Modifier.size(84.dp))
                        }
                    }
                    Text(
                        if (due == null) stringResource(R.string.dash_nothing_due)
                        else stringResource(R.string.dash_next_due, due.item.name),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (vehicles.size > 1) {
                item(key = "others") {
                    Text(
                        stringResource(R.string.garage_other_vehicles),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = Gutter, end = Gutter, top = 18.dp, bottom = 8.dp),
                    )
                }
                items(vehicles.drop(1), key = { "vehicle-${it.id}" }) { v ->
                    val worst = data.dueList(v.id, settings.warnDays, settings.warnKm).firstOrNull()?.state ?: DueState.UNKNOWN
                    CtrlCard(
                        Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 5.dp),
                        onClick = { nav.navigate(VehicleRoute(v.id)) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (v.photo != null) {
                                PhotoThumb(v.photo, Modifier.size(52.dp))
                            } else {
                                Icon(Icons.Outlined.TwoWheeler, null, Modifier.size(28.dp), tint = CtrlColors.Muted)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(v.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(fmt.distance(data.currentKm(v.id)), style = CtrlType.Data, color = CtrlColors.Muted)
                            }
                            if (worst == DueState.SOON || worst == DueState.OVERDUE) DueTag(worst)
                        }
                    }
                }
            }
        }
    }
}

