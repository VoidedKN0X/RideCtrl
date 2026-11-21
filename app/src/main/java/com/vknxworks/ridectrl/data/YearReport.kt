package com.vknxworks.ridectrl.data

import java.time.LocalDate

/** One service, mod or tyre line in the yearly report. */
data class ReportLine(val date: String, val odometer: Double?, val text: String, val cost: Double?, val position: TyrePosition? = null)

/**
 * Everything the yearly report shows for one vehicle and one calendar year, worked out from the logs. Amounts are
 * in the currency of the logs, distances in km, volumes in litres; the PDF converts for display.
 */
data class YearReport(
    val vehicle: Vehicle,
    val year: Int,
    val kmRidden: Double,
    /** The last odometer value known on or before 31 December (or today, for the current year). */
    val odometerAtEnd: Double?,
    val fills: Int,
    val litres: Double,
    val fuelCost: Double,
    /** Average over the full-to-full intervals that end in the year (L/100 km). */
    val avgPer100: Double?,
    val costsByCategory: List<Pair<CostCategory, Double>>,
    val totalCost: Double,
    val services: List<ReportLine>,
    val mods: List<ReportLine>,
    val tyres: List<ReportLine>,
    val jettingChanges: Int,
    val tempReadings: Int,
    val checks: Int,
)

private fun inYear(date: String?, year: Int): Boolean = parseDate(date)?.year == year

/** The yearly report for [vehicleId] in [year], or null when the vehicle doesn't exist. */
fun AppData.yearReport(vehicleId: String, year: Int, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): YearReport? {
    val v = vehicles.firstOrNull { it.id == vehicleId } ?: return null
    val points = odoPoints(vehicleId)
    val yearEnd = LocalDate.of(year, 12, 31)
    val fillsInYear = fuel.filter { it.vehicleId == vehicleId && inYear(it.date, year) }
    val segments = fuelSegments(fuel.filter { it.vehicleId == vehicleId }).filter { inYear(it.to.date, year) }
    val costs = costLines(vehicleId).filter { inYear(it.date, year) }
    val byCategory = CostCategory.entries.map { c -> c to costs.filter { it.category == c }.sumOf { it.amount } }.filter { it.second > 0 }
    val itemNames = serviceItems.associate { it.id to it.name }
    val partNames = parts.associate { it.id to it.name }
    val services = services.filter { it.vehicleId == vehicleId && inYear(it.date, year) }.sortedBy { it.date }.map { s ->
        val what = (s.itemIds.mapNotNull { itemNames[it] } + s.parts.mapNotNull { p -> partNames[p.partId]?.let { if (p.quantity > 1) "$it x${p.quantity}" else it } })
            .joinToString(", ").ifBlank { s.notes.lineSequence().firstOrNull().orEmpty() }
        val cost = (s.labourCost ?: 0.0) + (s.otherPartsCost ?: 0.0) + partsCost(s.parts)
        ReportLine(s.date, s.odometer, what, cost.takeIf { it > 0 })
    }
    val mods = mods.filter { it.vehicleId == vehicleId && inYear(it.date, year) }.sortedBy { it.date }.map { m ->
        val cost = (m.cost ?: 0.0) + partsCost(m.parts)
        ReportLine(m.date, m.odometer, m.title, cost.takeIf { it > 0 })
    }
    val tyres = tyres.filter { it.vehicleId == vehicleId && it.event == TyreEvent.FITTED && inYear(it.date, year) }.sortedBy { it.date }.map { t ->
        ReportLine(t.date, t.odometer, listOf(t.brand, t.model, t.size).filter { it.isNotBlank() }.joinToString(" "), t.cost, t.position)
    }
    return YearReport(
        vehicle = v,
        year = year,
        kmRidden = kmInYear(points, year),
        odometerAtEnd = points.lastOrNull { !it.date.isAfter(yearEnd) }?.km,
        fills = fillsInYear.size,
        litres = fillsInYear.sumOf { it.litres },
        fuelCost = fillsInYear.sumOf { it.cost() },
        avgPer100 = averagePer100(segments),
        costsByCategory = byCategory,
        totalCost = byCategory.sumOf { it.second },
        services = services,
        mods = mods,
        tyres = tyres,
        jettingChanges = jetting.count { it.vehicleId == vehicleId && inYear(it.date, year) },
        tempReadings = temps.count { it.vehicleId == vehicleId && java.time.Instant.ofEpochMilli(it.time).atZone(zone).year == year },
        checks = checks.count { it.vehicleId == vehicleId && java.time.Instant.ofEpochMilli(it.time).atZone(zone).year == year },
    )
}
