package com.vknxworks.ridectrl.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// ---------------------------------------------------------------- Jetting

/** The temperature readings taken while a jetting setting was in use: how many, and min/avg/max in °C. */
data class TempSummary(val count: Int, val min: Double, val avg: Double, val max: Double)

/** A jetting entry with the readings from its period (null when there are none). */
data class JettingPeriod(val entry: JettingEntry, val until: LocalDate?, val temps: TempSummary?)

/**
 * Every jetting entry of a vehicle, newest first, each with the temperature readings taken from its date up
 * to (not including) the date of the next entry. The newest entry runs until now. Readings are matched by
 * their local date, so a reading on the day of a change counts for the new setting.
 */
fun AppData.jettingPeriods(vehicleId: String, zone: ZoneId = ZoneId.systemDefault()): List<JettingPeriod> {
    val entries = jetting.filter { it.vehicleId == vehicleId }
        .mapNotNull { e -> parseDate(e.date)?.let { it to e } }
        .sortedBy { it.first }
    val readings = temps.filter { it.vehicleId == vehicleId }
        .map { java.time.Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate() to it.tempC }
    return entries.mapIndexed { i, (from, e) ->
        val until = entries.getOrNull(i + 1)?.first
        val inPeriod = readings.filter { (d, _) -> !d.isBefore(from) && (until == null || d.isBefore(until)) }.map { it.second }
        JettingPeriod(e, until, summary(inPeriod))
    }.reversed()
}

fun summary(values: List<Double>): TempSummary? =
    if (values.isEmpty()) null else TempSummary(values.size, values.min(), values.sum() / values.size, values.max())

/** A short line describing a jetting setting, for lists ("Main 85 · idle 38 · clip 3"), built by the caller's labels. */
fun JettingEntry.hasJets(): Boolean = mainJet.isNotBlank() || idleJet.isNotBlank() || needleClip.isNotBlank()

// ---------------------------------------------------------------- Tyres

/** Where a tyre position stands: the tyre that's on (the last one fitted), km on it, and the latest check. */
data class TyreStatus(
    val position: TyrePosition,
    val fitted: TyreEntry?,
    val kmOnTyre: Double?,
    val lastCheck: TyreEntry?,
)

fun AppData.tyreStatus(vehicleId: String, position: TyrePosition): TyreStatus {
    val list = tyres.filter { it.vehicleId == vehicleId && it.position == position }
        .sortedWith(compareBy<TyreEntry> { it.date }.thenBy { it.odometer ?: 0.0 })
    val fitted = list.lastOrNull { it.event == TyreEvent.FITTED }
    // Checks of an older tyre don't describe the one that's on now.
    val lastCheck = list.lastOrNull { it.event == TyreEvent.CHECK && (fitted == null || it.date >= fitted.date) }
    val now = currentKm(vehicleId)
    val from = fitted?.odometer
    val km = if (from != null && now != null && now >= from) now - from else null
    return TyreStatus(position, fitted, km, lastCheck)
}

/**
 * The production date from a DOT date code (the last four digits: week then year, "2423" is week 24 of
 * 2023), as the Monday of roughly that week. Null for anything that isn't a plausible code.
 */
fun dotDate(code: String): LocalDate? {
    val digits = code.filter { it.isDigit() }
    if (digits.length < 4) return null
    val last = digits.takeLast(4)
    val week = last.take(2).toInt()
    val year = 2000 + last.takeLast(2).toInt()
    if (week !in 1..53) return null
    val d = runCatching {
        LocalDate.of(year, 1, 4).with(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR, week.toLong())
            .with(java.time.DayOfWeek.MONDAY)
    }.getOrNull() ?: return null
    // Week 53 in a year that has only 52 weeks rolls into the next year: not a real code.
    return d.takeIf { it.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR) == year }
}

/** A tyre's age in whole months from its DOT code, or null. */
fun dotAgeMonths(code: String, today: LocalDate = LocalDate.now()): Long? =
    dotDate(code)?.let { d -> if (d.isAfter(today)) null else ChronoUnit.MONTHS.between(d, today) }

// ---------------------------------------------------------------- Pre-ride checklists

/** A vehicle's checklist in its own order. */
fun AppData.checklistFor(vehicleId: String): List<ChecklistItem> =
    checklist.filter { it.vehicleId == vehicleId }.sortedWith(compareBy<ChecklistItem> { it.order }.thenBy { it.text.lowercase() })

// ---------------------------------------------------------------- Yearly report

/**
 * Kilometres ridden in [year]: the increases between consecutive odometer values inside the year, plus the
 * stretch from the last value before the year to the first one in it, counted for the part that falls in
 * the year (spread evenly over the days between them). A gauge swap (the odometer going back) counts as 0.
 */
fun kmInYear(points: List<OdoPoint>, year: Int): Double {
    val start = LocalDate.of(year, 1, 1)
    val end = LocalDate.of(year + 1, 1, 1)
    var total = 0.0
    for (i in 1 until points.size) {
        val a = points[i - 1]
        val b = points[i]
        val d = b.km - a.km
        if (d <= 0) continue
        if (!b.date.isBefore(start) && a.date.isBefore(end)) {
            val days = ChronoUnit.DAYS.between(a.date, b.date)
            if (days <= 0) {
                if (!a.date.isBefore(start)) total += d
                continue
            }
            val from = if (a.date.isBefore(start)) start else a.date
            val to = if (b.date.isAfter(end)) end else b.date
            val inYear = ChronoUnit.DAYS.between(from, to).coerceAtLeast(0)
            total += d * inYear / days
        }
    }
    return total
}

/** The years a vehicle has anything logged in, newest first (always at least the current year). */
fun AppData.loggedYears(vehicleId: String, today: LocalDate = LocalDate.now()): List<Int> {
    val dates = buildList {
        fuel.filter { it.vehicleId == vehicleId }.forEach { add(it.date) }
        services.filter { it.vehicleId == vehicleId }.forEach { add(it.date) }
        mods.filter { it.vehicleId == vehicleId }.forEach { add(it.date) }
        odometer.filter { it.vehicleId == vehicleId }.forEach { add(it.date) }
        trips.filter { it.vehicleId == vehicleId }.forEach { add(it.date) }
        tyres.filter { it.vehicleId == vehicleId }.forEach { add(it.date) }
        jetting.filter { it.vehicleId == vehicleId }.forEach { add(it.date) }
    }
    val years = dates.mapNotNull { parseDate(it)?.year }.toMutableSet()
    years += today.year
    return years.sortedDescending()
}
