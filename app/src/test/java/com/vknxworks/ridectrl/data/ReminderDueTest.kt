package com.vknxworks.ridectrl.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ReminderDueTest {
    private val today = LocalDate.of(2026, 6, 1)
    private val warnDays = 14
    private val warnKm = 300.0

    private fun item(km: Double? = null, days: Int? = null, baselineDate: String? = null, baselineKm: Double? = null) =
        ServiceItem(id = "oil", vehicleId = "v", name = "Oil", intervalKm = km, intervalDays = days, baselineDate = baselineDate, baselineKm = baselineKm)

    private fun service(date: String, odo: Double?) = ServiceRecord(id = "s$date", vehicleId = "v", date = date, odometer = odo, itemIds = listOf("oil"))

    private fun odo(date: String, km: Double) = OdometerReading(id = "o$date$km", vehicleId = "v", date = date, km = km)

    private fun due(data: AppData, it: ServiceItem) = data.copy(serviceItems = listOf(it)).due(it, warnDays, warnKm, today)

    @Test
    fun noIntervalIsUnknown() {
        val d = due(AppData(services = listOf(service("2026-01-01", 1000.0))), item())
        assertEquals(DueState.UNKNOWN, d.state)
    }

    @Test
    fun neverDoneAndNoBaselineIsUnknown() {
        assertEquals(DueState.UNKNOWN, due(AppData(), item(km = 3000.0, days = 365)).state)
    }

    @Test
    fun byDate() {
        val data = AppData(services = listOf(service("2025-07-01", null)))
        // Due 2026-06-28: 27 days left, more than 14.
        val ok = due(data, item(days = 362))
        assertEquals(LocalDate.of(2026, 6, 28), ok.nextDate)
        assertEquals(27L, ok.daysLeft)
        assertEquals(DueState.OK, ok.state)
        // Due 2026-06-10: 9 days left.
        assertEquals(DueState.SOON, due(data, item(days = 344)).state)
        // Due on today: still soon, not overdue.
        assertEquals(DueState.SOON, due(data, item(days = 335)).state)
        // Due yesterday.
        assertEquals(DueState.OVERDUE, due(data, item(days = 334)).state)
    }

    @Test
    fun byMileageFromTheLoggedOdometer() {
        // Serviced a year ago at 10 000 km; about 6 km a day since.
        val base = AppData(services = listOf(service("2025-06-01", 10_000.0)))
        // Next at 13 000 km.
        val ok = due(base.copy(odometer = listOf(odo("2026-05-20", 12_000.0))), item(km = 3000.0))
        assertEquals(13_000.0, ok.nextKm!!, 1e-9)
        assertEquals(1000.0, ok.kmLeft!!, 1e-9)
        assertEquals(DueState.OK, ok.state)
        // Within 300 km.
        assertEquals(DueState.SOON, due(base.copy(odometer = listOf(odo("2026-05-20", 12_750.0))), item(km = 3000.0)).state)
        // Reached exactly, and passed.
        assertEquals(DueState.OVERDUE, due(base.copy(odometer = listOf(odo("2026-05-20", 13_000.0))), item(km = 3000.0)).state)
        assertEquals(DueState.OVERDUE, due(base.copy(odometer = listOf(odo("2026-05-20", 13_400.0))), item(km = 3000.0)).state)
    }

    @Test
    fun anyLoggedOdometerCounts() {
        // A fuel fill with the odometer is as good as a reading.
        val data = AppData(
            services = listOf(service("2026-05-01", 10_000.0)),
            fuel = listOf(FuelEntry(id = "f", vehicleId = "v", date = "2026-05-30", odometer = 12_900.0, litres = 5.0)),
        )
        assertEquals(DueState.SOON, due(data, item(km = 3000.0)).state)
    }

    @Test
    fun dateOrMileageWhicheverComesFirst() {
        val data = AppData(
            services = listOf(service("2025-06-15", 10_000.0)),
            odometer = listOf(odo("2026-05-31", 10_500.0)),
        )
        // Far from the km limit, but the year is up on 2026-06-15 (14 days): soon.
        assertEquals(DueState.SOON, due(data, item(km = 5000.0, days = 365)).state)
        // Km limit passed while the date is far away: overdue.
        val passed = data.copy(odometer = listOf(odo("2026-05-31", 15_100.0)))
        assertEquals(DueState.OVERDUE, due(passed, item(km = 5000.0, days = 730)).state)
    }

    @Test
    fun projectedFromAverageRiding() {
        // 20 km a day over the last 90 days; 400 km left means about 20 days.
        val data = AppData(
            services = listOf(service("2026-03-03", 10_000.0)),
            odometer = listOf(odo("2026-03-03", 10_000.0), odo("2026-06-01", 11_800.0)),
        )
        val d = due(data, item(km = 2200.0))
        assertEquals(400.0, d.kmLeft!!, 1e-9)
        assertEquals(today.plusDays(20), d.projectedDate)
        assertEquals(DueState.OK, d.state)
        // 250 km left: within the km warning.
        assertEquals(DueState.SOON, due(data, item(km = 2050.0)).state)
        // With a 100 km warning, 260 km left at 20 km a day is 13 days away: soon by the projected date.
        val d2 = data.copy(serviceItems = listOf(item(km = 2060.0))).due(item(km = 2060.0), warnDays, 100.0, today)
        assertEquals(today.plusDays(13), d2.projectedDate)
        assertEquals(DueState.SOON, d2.state)
    }

    @Test
    fun hugeIntervalDoesNotOverflow() {
        val data = AppData(
            services = listOf(service("2026-01-01", 0.0)),
            odometer = listOf(odo("2026-01-01", 0.0), odo("2026-06-01", 10.0)),
        )
        val d = due(data, item(km = 1e12))
        assertEquals(DueState.OK, d.state)
        assertEquals(today.plusDays(36_500), d.projectedDate)
    }

    @Test
    fun baselineUsedUntilAServiceIsLogged() {
        val it = item(km = 3000.0, baselineDate = "2026-01-01", baselineKm = 5000.0)
        val data = AppData(odometer = listOf(odo("2026-05-01", 7900.0)))
        val d = due(data, it)
        assertEquals(8000.0, d.nextKm!!, 1e-9)
        assertEquals(DueState.SOON, d.state)
        // A later service replaces the baseline.
        val done = data.copy(services = listOf(service("2026-05-01", 7900.0)))
        assertEquals(10_900.0, due(done, it).nextKm!!, 1e-9)
        assertEquals(DueState.OK, due(done, it).state)
    }

    @Test
    fun latestServiceWins() {
        val data = AppData(
            services = listOf(service("2025-01-01", 1000.0), service("2026-04-01", 4000.0)),
            odometer = listOf(odo("2026-05-01", 4100.0)),
        )
        val d = due(data, item(km = 3000.0))
        assertEquals(LocalDate.of(2026, 4, 1), d.lastDate)
        assertEquals(7000.0, d.nextKm!!, 1e-9)
    }

    @Test
    fun gaugeSwapUsesTheNewestOdometer() {
        val data = AppData(
            services = listOf(service("2026-01-01", 20_000.0)),
            odometer = listOf(odo("2026-02-01", 21_000.0), odo("2026-03-01", 100.0)),
        )
        assertEquals(100.0, data.currentKm("v")!!, 1e-9)
        assertNull(AppData().currentKm("v"))
    }

    @Test
    fun dueListPutsTheMostUrgentFirst() {
        val items = listOf(
            ServiceItem(id = "a", vehicleId = "v", name = "A", intervalDays = 400),
            ServiceItem(id = "b", vehicleId = "v", name = "B", intervalDays = 100),
            ServiceItem(id = "c", vehicleId = "v", name = "C"),
            ServiceItem(id = "d", vehicleId = "v", name = "D", intervalDays = 160),
        )
        val data = AppData(
            serviceItems = items,
            services = listOf(ServiceRecord(id = "s", vehicleId = "v", date = "2026-01-01", itemIds = listOf("a", "b", "c", "d"))),
        )
        assertEquals(listOf("b", "d", "a", "c"), data.dueList("v", warnDays, warnKm, today).map { it.item.id })
    }
}
