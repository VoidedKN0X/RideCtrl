package com.vknxworks.ridectrl.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuelEconomyTest {
    private var n = 0
    private fun fill(date: String, odo: Double?, litres: Double, full: Boolean = true, vehicle: String = "v") =
        FuelEntry(id = "f${n++}", vehicleId = vehicle, date = date, odometer = odo, litres = litres, fullTank = full)

    @Test
    fun fullToFull() {
        val s = fuelSegments(listOf(fill("2026-01-01", 1000.0, 5.0), fill("2026-01-10", 1200.0, 6.0)))
        assertEquals(1, s.size)
        assertEquals(200.0, s[0].km, 1e-9)
        assertEquals(6.0, s[0].litres, 1e-9)
        assertEquals(3.0, s[0].litresPer100, 1e-9)
    }

    @Test
    fun partialFillsAddUpUntilTheNextFullTank() {
        val s = fuelSegments(
            listOf(
                fill("2026-01-01", 1000.0, 5.0),
                fill("2026-01-05", 1100.0, 2.0, full = false),
                fill("2026-01-08", 1250.0, 1.5, full = false),
                fill("2026-01-10", 1400.0, 4.5),
            ),
        )
        assertEquals(1, s.size)
        assertEquals(400.0, s[0].km, 1e-9)
        assertEquals(8.0, s[0].litres, 1e-9) // 2 + 1.5 + 4.5; the first full fill isn't counted
        assertEquals(2.0, s[0].litresPer100, 1e-9)
    }

    @Test
    fun partialFillsBeforeTheFirstFullTankDontCount() {
        val s = fuelSegments(
            listOf(
                fill("2026-01-01", 900.0, 3.0, full = false),
                fill("2026-01-02", 1000.0, 5.0),
                fill("2026-01-10", 1200.0, 6.0),
            ),
        )
        assertEquals(1, s.size)
        assertEquals(6.0, s[0].litres, 1e-9)
    }

    @Test
    fun partialFillsAfterTheLastFullTankWaitForTheNextOne() {
        val entries = listOf(
            fill("2026-01-01", 1000.0, 5.0),
            fill("2026-01-10", 1200.0, 6.0),
            fill("2026-01-15", 1300.0, 2.0, full = false),
        )
        val s = fuelSegments(entries)
        assertEquals(1, s.size)
        assertEquals(3.0, s.last().litresPer100, 1e-9)
    }

    @Test
    fun onlyPartialFillsGiveNoConsumption() {
        val s = fuelSegments(listOf(fill("2026-01-01", 1000.0, 3.0, full = false), fill("2026-01-05", 1100.0, 2.0, full = false)))
        assertTrue(s.isEmpty())
        assertNull(averagePer100(s))
    }

    @Test
    fun oneFullTankIsNotEnough() {
        assertTrue(fuelSegments(listOf(fill("2026-01-01", 1000.0, 5.0))).isEmpty())
    }

    @Test
    fun fillsWithoutOdometerCountInTheirInterval() {
        val s = fuelSegments(
            listOf(
                fill("2026-01-01", 1000.0, 5.0),
                fill("2026-01-04", null, 2.0, full = false),
                fill("2026-01-10", 1200.0, 4.0),
                fill("2026-01-12", null, 1.0, full = false),
                fill("2026-01-20", 1400.0, 5.0),
            ),
        )
        assertEquals(2, s.size)
        assertEquals(6.0, s[0].litres, 1e-9) // 2 (no odometer) + 4
        assertEquals(6.0, s[1].litres, 1e-9) // 1 (no odometer) + 5
    }

    @Test
    fun aFillWithoutOdometerCountsOnlyOnce() {
        // On the day one interval ends and the next starts: it belongs to the interval it closes.
        val s = fuelSegments(
            listOf(
                fill("2026-01-01", 1000.0, 5.0),
                fill("2026-01-10", null, 1.0, full = false),
                fill("2026-01-10", 1200.0, 4.0),
                fill("2026-01-20", 1400.0, 5.0),
            ),
        )
        assertEquals(5.0, s[0].litres, 1e-9)
        assertEquals(5.0, s[1].litres, 1e-9)
    }

    @Test
    fun gaugeSwapIntervalIsLeftOut() {
        val s = fuelSegments(
            listOf(
                fill("2026-01-01", 1000.0, 5.0),
                fill("2026-01-10", 1200.0, 4.0),
                fill("2026-01-20", 1200.0, 3.0), // the odometer didn't move
            ),
        )
        assertEquals(1, s.size)
    }

    @Test
    fun averageIsWeightedByDistance() {
        val s = fuelSegments(
            listOf(
                fill("2026-01-01", 1000.0, 5.0),
                fill("2026-01-10", 1100.0, 2.0), // 100 km, 2 L: 2 L/100
                fill("2026-01-20", 1400.0, 12.0), // 300 km, 12 L: 4 L/100
            ),
        )
        assertEquals(2, s.size)
        // (2 + 12) / 400 km = 3.5 L/100, not the plain mean of 2 and 4.
        assertEquals(3.5, averagePer100(s)!!, 1e-9)
        val r = economyRange(s)!!
        assertEquals(2.0, r.best.litresPer100, 1e-9)
        assertEquals(4.0, r.worst.litresPer100, 1e-9)
    }

    @Test
    fun fuelStatsUsesOnlyTheVehiclesOwnFills() {
        val data = AppData(
            fuel = listOf(
                fill("2026-01-01", 1000.0, 5.0, vehicle = "a"),
                fill("2026-01-05", 5000.0, 9.0, vehicle = "b"),
                fill("2026-01-10", 1200.0, 6.0, vehicle = "a"),
            ),
        )
        val st = data.fuelStats("a")
        assertEquals(3.0, st.avgPer100!!, 1e-9)
        assertEquals(3.0, st.lastPer100!!, 1e-9)
        assertNull(data.fuelStats("b").avgPer100)
    }

    @Test
    fun mpgConversion() {
        // 4.7 L/100 km is about 50 US mpg and 60 imperial mpg.
        assertEquals(50.05, per100ToMpg(4.7, imperial = false), 0.01)
        assertEquals(60.10, per100ToMpg(4.7, imperial = true), 0.01)
        // Half the consumption is twice the mpg.
        assertEquals(per100ToMpg(5.0, false) * 2, per100ToMpg(2.5, false), 1e-9)
    }
}
