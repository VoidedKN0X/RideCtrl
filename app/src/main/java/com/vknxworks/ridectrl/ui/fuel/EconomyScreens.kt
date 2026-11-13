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
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.container
import com.vknxworks.ridectrl.data.Fmt
import com.vknxworks.ridectrl.data.FuelSegment
import com.vknxworks.ridectrl.data.Vehicle
import com.vknxworks.ridectrl.data.averagePer100
import com.vknxworks.ridectrl.data.fuelSegments
import com.vknxworks.ridectrl.data.parseDate
import com.vknxworks.ridectrl.ui.components.BarChart
import com.vknxworks.ridectrl.ui.components.CtrlCard
import com.vknxworks.ridectrl.ui.components.MessageView
import com.vknxworks.ridectrl.ui.components.ScreenTopBar
import com.vknxworks.ridectrl.ui.components.SectionLabel
import com.vknxworks.ridectrl.ui.components.TimeChart
import com.vknxworks.ridectrl.ui.components.rememberFmt
import com.vknxworks.ridectrl.ui.nav.FuelRoute
import com.vknxworks.ridectrl.ui.nav.LocalNavigator
import com.vknxworks.ridectrl.ui.theme.CtrlColors
import com.vknxworks.ridectrl.ui.theme.CtrlType
import com.vknxworks.ridectrl.ui.theme.Gutter
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Consumption over time for one vehicle: one point per full-to-full interval, at the date of the fill that
 * closed it, in L/100 km or mpg as set, with the distance-weighted average as a dashed line.
 */
@Composable
fun EconomyChart(segments: List<FuelSegment>, average: Double?, fmt: Fmt) {
    val points = remember(segments, fmt.settings) {
        segments.mapNotNull { s -> parseDate(s.to.date)?.let { it to fmt.consumptionValue(s.litresPer100) } }
    }
    val avg = average?.let { fmt.consumptionValue(it) }
    val dateFmt = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT) }
    TimeChart(
        points,
        average = avg,
        averageLabel = avg?.let { stringResource(R.string.economy_average, fmt.num(it, 1)) },
        labelFor = { fmt.num(it, 1) },
        dateLabel = { it.format(dateFmt) },
    )
}

/** The fuel log's chart card: heading with the unit, then the chart. */
@Composable
fun EconomyCard(segments: List<FuelSegment>, average: Double?, fmt: Fmt) {
    SectionLabel(stringResource(R.string.fuel_chart_consumption, fmt.consumptionUnit))
    Spacer(Modifier.size(10.dp))
    EconomyChart(segments, average, fmt)
}

private data class VehicleEconomy(val vehicle: Vehicle, val segments: List<FuelSegment>, val average: Double?)

/**
 * Fuel economy for every vehicle: the averages side by side, then each vehicle's consumption over time with
 * its average. Only full-to-full intervals count. Tapping a vehicle opens its fuel log.
 */
@Composable
fun EconomyScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val data by c.logs.data.collectAsStateWithLifecycle()
    val fmt = rememberFmt()
    val rows = remember(data) {
        data.vehicles.sortedBy { it.created }.map { v ->
            val segs = fuelSegments(data.fuel.filter { it.vehicleId == v.id })
            VehicleEconomy(v, segs, averagePer100(segs))
        }.filter { it.segments.isNotEmpty() }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.economy_title))
        if (rows.isEmpty()) {
            MessageView(Icons.Outlined.LocalGasStation, stringResource(R.string.economy_empty_title), stringResource(R.string.economy_empty_body))
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Text(
                    stringResource(R.string.economy_intro, fmt.consumptionUnit),
                    style = MaterialTheme.typography.bodySmall,
                    color = CtrlColors.Muted,
                    modifier = Modifier.padding(horizontal = Gutter, vertical = 6.dp),
                )
            }
            if (rows.size >= 2) {
                item {
                    CtrlCard(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp)) {
                        SectionLabel(stringResource(R.string.economy_compare, fmt.consumptionUnit))
                        Spacer(Modifier.size(10.dp))
                        BarChart(
                            rows.map { r -> r.vehicle.name.take(10) to (r.average?.let { fmt.consumptionValue(it) } ?: 0.0) },
                            labelFor = { fmt.num(it, 1) },
                        )
                    }
                }
            }
            items(rows, key = { it.vehicle.id }) { r ->
                CtrlCard(
                    Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp),
                    onClick = { nav.navigate(FuelRoute(r.vehicle.id)) },
                ) {
                    Row {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(r.vehicle.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                pluralStringResource(R.plurals.economy_intervals, r.segments.size, r.segments.size) + " · " +
                                    fmt.distance(r.segments.sumOf { it.km }),
                                style = CtrlType.Data, color = CtrlColors.Muted,
                            )
                        }
                        Column {
                            SectionLabel(stringResource(R.string.economy_avg_short))
                            Text(fmt.consumption(r.average), style = CtrlType.LabelStrong, color = CtrlColors.AccentText)
                        }
                    }
                    if (r.segments.size >= 2) {
                        Spacer(Modifier.size(12.dp))
                        EconomyChart(r.segments, r.average, fmt)
                    }
                }
            }
        }
    }
}
