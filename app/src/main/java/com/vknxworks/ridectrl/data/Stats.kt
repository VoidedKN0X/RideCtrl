package com.vknxworks.ridectrl.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max

/** One known odometer value on a date, from any log. */
data class OdoPoint(val date: LocalDate, val km: Double)

/** Every odometer value known for a vehicle: readings, fuel, services, mods and temperature readings. */
fun AppData.odoPoints(vehicleId: String): List<OdoPoint> {
    val pts = mutableListOf<OdoPoint>()
    fun add(d: String?, km: Double?) {
        val date = parseDate(d) ?: return
        if (km != null && km >= 0) pts += OdoPoint(date, km)
    }
    odometer.filter { it.vehicleId == vehicleId }.forEach { add(it.date, it.km) }
    fuel.filter { it.vehicleId == vehicleId }.forEach { add(it.date, it.odometer) }
    services.filter { it.vehicleId == vehicleId }.forEach { add(it.date, it.odometer) }
    mods.filter { it.vehicleId == vehicleId }.forEach { add(it.date, it.odometer) }
    temps.filter { it.vehicleId == vehicleId }.forEach {
        add(java.time.Instant.ofEpochMilli(it.time).atZone(ZoneId.systemDefault()).toLocalDate().toString(), it.odometer)
    }
    return pts.sortedWith(compareBy<OdoPoint> { it.date }.thenBy { it.km })
}

/** The current odometer: the newest value (after a gauge swap that may be lower than an older one). */
fun AppData.currentKm(vehicleId: String): Double? = odoPoints(vehicleId).lastOrNull()?.km

/**
 * Kilometres actually ridden: the sum of every increase between consecutive values, so a gauge swap
 * (the odometer going back) doesn't count as negative riding.
 */
fun ridden(points: List<OdoPoint>): Double {
    var total = 0.0
    for (i in 1 until points.size) {
        val d = points[i].km - points[i - 1].km
        if (d > 0) total += d
    }
    return total
}

/**
 * Average km per day: over the last 90 days when that window has at least two values a week or more apart,
 * otherwise over the whole history. Null until there are two values on different days.
 */
fun AppData.kmPerDay(vehicleId: String, today: LocalDate = LocalDate.now()): Double? {
    val all = odoPoints(vehicleId)
    val recent = all.filter { !it.date.isBefore(today.minusDays(90)) }
    fun rate(p: List<OdoPoint>): Double? {
        if (p.size < 2) return null
        val days = ChronoUnit.DAYS.between(p.first().date, p.last().date)
        if (days < 1) return null
        return ridden(p) / days
    }
    val recentSpan = if (recent.size >= 2) ChronoUnit.DAYS.between(recent.first().date, recent.last().date) else 0
    return if (recentSpan >= 7) rate(recent) else rate(all)
}

// ---------------------------------------------------------------- Maintenance

enum class DueState { OK, SOON, OVERDUE, UNKNOWN }

data class Due(
    val item: ServiceItem,
    val lastDate: LocalDate?,
    val lastKm: Double?,
    val nextDate: LocalDate?,
    val nextKm: Double?,
    val kmLeft: Double?,
    val daysLeft: Long?,
    /** When the km limit is expected to be reached, from the average km per day. */
    val projectedDate: LocalDate?,
    val state: DueState,
) {
    /** The date it's due: the earlier of the time limit and the projected km limit. */
    val dueDate: LocalDate? get() = listOfNotNull(nextDate, projectedDate).minOrNull()
}

fun AppData.due(
    item: ServiceItem,
    warnDays: Int,
    warnKm: Double,
    today: LocalDate = LocalDate.now(),
    current: Double? = currentKm(item.vehicleId),
    perDay: Double? = kmPerDay(item.vehicleId, today),
): Due {
    val last = services.filter { it.vehicleId == item.vehicleId && item.id in it.itemIds }
        .maxWithOrNull(compareBy<ServiceRecord> { it.date }.thenBy { it.odometer ?: 0.0 })
    val lastDate = parseDate(last?.date) ?: parseDate(item.baselineDate)
    val lastKm = if (last != null) last.odometer ?: item.baselineKm.takeIf { last.date == item.baselineDate } else item.baselineKm
    val nextDate = if (item.intervalDays != null && item.intervalDays > 0 && lastDate != null) lastDate.plusDays(item.intervalDays.toLong()) else null
    val nextKm = if (item.intervalKm != null && item.intervalKm > 0 && lastKm != null) lastKm + item.intervalKm else null
    val kmLeft = if (nextKm != null && current != null) nextKm - current else null
    val daysLeft = nextDate?.let { ChronoUnit.DAYS.between(today, it) }
    val projected = if (kmLeft != null && perDay != null && perDay > 0.01) today.plusDays(max(0.0, kmLeft / perDay).toLong()) else null
    val projectedDays = projected?.let { ChronoUnit.DAYS.between(today, it) }
    val state = when {
        nextDate == null && nextKm == null -> DueState.UNKNOWN
        (kmLeft != null && kmLeft <= 0) || (daysLeft != null && daysLeft < 0) -> DueState.OVERDUE
        (kmLeft != null && kmLeft <= warnKm) || (daysLeft != null && daysLeft <= warnDays) ||
            (projectedDays != null && projectedDays <= warnDays) -> DueState.SOON
        else -> DueState.OK
    }
    return Due(item, lastDate, lastKm, nextDate, nextKm, kmLeft, daysLeft, projected, state)
}

/** Every service item of a vehicle with its state, most urgent first. */
fun AppData.dueList(vehicleId: String, warnDays: Int, warnKm: Double, today: LocalDate = LocalDate.now()): List<Due> {
    val current = currentKm(vehicleId)
    val perDay = kmPerDay(vehicleId, today)
    return serviceItems.filter { it.vehicleId == vehicleId }
        .map { due(it, warnDays, warnKm, today, current, perDay) }
        .sortedWith(
            compareBy<Due> {
                when (it.state) {
                    DueState.OVERDUE -> 0
                    DueState.SOON -> 1
                    DueState.OK -> 2
                    DueState.UNKNOWN -> 3
                }
            }.thenBy { it.dueDate ?: LocalDate.MAX }.thenBy { it.kmLeft ?: Double.MAX_VALUE },
        )
}

// ---------------------------------------------------------------- Fuel

data class FuelSegment(val from: FuelEntry, val to: FuelEntry, val km: Double, val litres: Double) {
    val litresPer100: Double get() = litres / km * 100.0
}

data class FuelStats(
    val segments: List<FuelSegment>,
    /** Average consumption over full-to-full segments, L/100 km. */
    val avgPer100: Double?,
    val lastPer100: Double?,
    val totalLitres: Double,
    val totalCost: Double,
    val costPerKm: Double?,
    /** Fuel and oil spend per month (yyyy-MM) and per year. */
    val monthly: List<Pair<String, Double>>,
    val yearly: List<Pair<String, Double>>,
)

fun FuelEntry.cost(): Double = (total ?: pricePerLitre?.let { it * litres } ?: 0.0) + (oilCost ?: 0.0)

/**
 * Consumption is only measured between two full tanks: everything poured after the first full fill, up to
 * and including the next full fill, was burnt over that distance. Fills without an odometer are skipped.
 */
fun AppData.fuelStats(vehicleId: String): FuelStats {
    val entries = fuel.filter { it.vehicleId == vehicleId }
    val withOdo = entries.filter { it.odometer != null }.sortedWith(compareBy<FuelEntry> { it.odometer }.thenBy { it.date })
    val segments = mutableListOf<FuelSegment>()
    var lastFull: FuelEntry? = null
    var poured = 0.0
    for (e in withOdo) {
        val start = lastFull
        if (start != null) {
            poured += e.litres
            if (e.fullTank) {
                val km = e.odometer!! - start.odometer!!
                if (km > 0) segments += FuelSegment(start, e, km, poured)
            }
        }
        if (e.fullTank) {
            lastFull = e
            poured = 0.0
        }
    }
    val segKm = segments.sumOf { it.km }
    val segL = segments.sumOf { it.litres }
    val totalCost = entries.sumOf { it.cost() }
    val odoSpan = if (withOdo.size >= 2) withOdo.last().odometer!! - withOdo.first().odometer!! else 0.0
    // Cost per km: fuel bought between the first and last fill with an odometer, over that distance
    // (the last fill's fuel hasn't been ridden yet, so it's left out).
    val costPerKm = if (odoSpan > 0) withOdo.dropLast(1).sumOf { it.cost() } / odoSpan else null
    val monthly = entries.groupBy { it.date.take(7) }.map { (k, v) -> k to v.sumOf { it.cost() } }.sortedBy { it.first }
    val yearly = entries.groupBy { it.date.take(4) }.map { (k, v) -> k to v.sumOf { it.cost() } }.sortedBy { it.first }
    return FuelStats(
        segments = segments,
        avgPer100 = if (segKm > 0) segL / segKm * 100.0 else null,
        lastPer100 = segments.lastOrNull()?.litresPer100,
        totalLitres = entries.sumOf { it.litres },
        totalCost = totalCost,
        costPerKm = costPerKm,
        monthly = monthly,
        yearly = yearly,
    )
}

/** Premix: millilitres of oil for [litres] of fuel at [ratio]:1, and the reverse. */
fun oilFor(litres: Double, ratio: Double): Double = if (ratio > 0) litres * 1000.0 / ratio else 0.0
fun fuelFor(oilMl: Double, ratio: Double): Double = oilMl * ratio / 1000.0

// ---------------------------------------------------------------- Costs

enum class CostCategory { PURCHASE, FUEL, OIL, MAINTENANCE, PARTS, MODS, DOCUMENTS }

data class CostLine(val category: CostCategory, val date: String?, val amount: Double)

fun AppData.partPrice(id: String): Double = parts.firstOrNull { it.id == id }?.price ?: 0.0
fun AppData.partsCost(uses: List<PartUse>): Double = uses.sumOf { partPrice(it.partId) * it.quantity }

/** Every cost of owning a vehicle, as dated lines. Inventory parts count when they're fitted (service or mod). */
fun AppData.costLines(vehicleId: String): List<CostLine> {
    val v = vehicles.firstOrNull { it.id == vehicleId } ?: return emptyList()
    val out = mutableListOf<CostLine>()
    v.purchasePrice?.let { out += CostLine(CostCategory.PURCHASE, v.purchaseDate, it) }
    fuel.filter { it.vehicleId == vehicleId }.forEach { f ->
        val fuelCost = f.total ?: f.pricePerLitre?.let { it * f.litres }
        if (fuelCost != null) out += CostLine(CostCategory.FUEL, f.date, fuelCost)
        f.oilCost?.let { out += CostLine(CostCategory.OIL, f.date, it) }
    }
    services.filter { it.vehicleId == vehicleId }.forEach { s ->
        val m = (s.labourCost ?: 0.0) + (s.otherPartsCost ?: 0.0)
        if (m > 0) out += CostLine(CostCategory.MAINTENANCE, s.date, m)
        val p = partsCost(s.parts)
        if (p > 0) out += CostLine(CostCategory.PARTS, s.date, p)
    }
    mods.filter { it.vehicleId == vehicleId }.forEach { m ->
        m.cost?.let { if (it > 0) out += CostLine(CostCategory.MODS, m.date, it) }
        val p = partsCost(m.parts)
        if (p > 0) out += CostLine(CostCategory.PARTS, m.date, p)
    }
    docs.filter { it.vehicleId == vehicleId }.forEach { d ->
        d.cost?.let { if (it > 0) out += CostLine(CostCategory.DOCUMENTS, d.expiry, it) }
    }
    return out
}

/** Running cost (everything except the purchase) per km ridden. */
fun AppData.runningCostPerKm(vehicleId: String): Double? {
    val km = ridden(odoPoints(vehicleId))
    if (km <= 0) return null
    return costLines(vehicleId).filter { it.category != CostCategory.PURCHASE }.sumOf { it.amount } / km
}

// ---------------------------------------------------------------- Stock

/** Applies a change in parts used to the stock: parts taken out go down, parts given back go up. */
fun AppData.withStockChange(before: List<PartUse>, after: List<PartUse>): AppData {
    val delta = mutableMapOf<String, Int>()
    before.forEach { delta[it.partId] = (delta[it.partId] ?: 0) + it.quantity }
    after.forEach { delta[it.partId] = (delta[it.partId] ?: 0) - it.quantity }
    if (delta.values.all { it == 0 }) return this
    return copy(parts = parts.map { p -> delta[p.id]?.let { d -> p.copy(quantity = (p.quantity + d).coerceAtLeast(0)) } ?: p })
}

// ---------------------------------------------------------------- Temperatures

data class TempGroup(val setup: String, val count: Int, val min: Double, val avg: Double, val max: Double)

fun AppData.tempGroups(vehicleId: String): List<TempGroup> =
    temps.filter { it.vehicleId == vehicleId }
        .groupBy { it.setup.trim() }
        .map { (setup, list) ->
            TempGroup(setup, list.size, list.minOf { it.tempC }, list.sumOf { it.tempC } / list.size, list.maxOf { it.tempC })
        }
        .sortedByDescending { g -> temps.filter { it.vehicleId == vehicleId && it.setup.trim() == g.setup }.maxOf { it.time } }

// ---------------------------------------------------------------- Documents

fun VehicleDoc.daysLeft(today: LocalDate = LocalDate.now()): Long? = parseDate(expiry)?.let { ChronoUnit.DAYS.between(today, it) }

fun VehicleDoc.state(today: LocalDate = LocalDate.now()): DueState {
    val d = daysLeft(today) ?: return DueState.UNKNOWN
    return when {
        d < 0 -> DueState.OVERDUE
        d <= warnDays -> DueState.SOON
        else -> DueState.OK
    }
}
