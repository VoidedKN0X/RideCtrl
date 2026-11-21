package com.vknxworks.ridectrl.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class RideLogsTest {
    private fun millis(date: String, hour: Int = 12) = LocalDate.parse(date).atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun dotCodeIsWeekThenYear() {
        val d = dotDate("DOT XX YY 2423")!!
        assertEquals(2023, d.year)
        assertEquals(24L, d.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR).toLong())
        assertNull(dotDate("12"))
        assertNull(dotDate("6023"))
        // Week 53 in a year with 52 ISO weeks is not a real code.
        assertNull(dotDate("5323"))
    }

    @Test
    fun dotAgeInMonths() {
        assertEquals(12L, dotAgeMonths("2423", LocalDate.of(2024, 6, 20)))
        assertNull(dotAgeMonths("2499", LocalDate.of(2024, 6, 20)))
    }

    @Test
    fun jettingPeriodsTakeTheReadingsUntilTheNextSetting() {
        val data = AppData(
            jetting = listOf(
                JettingEntry("a", "v", "2026-05-01", mainJet = "80"),
                JettingEntry("b", "v", "2026-05-10", mainJet = "85"),
            ),
            temps = listOf(
                TempReading("t0", "v", millis("2026-04-30"), 70.0),
                TempReading("t1", "v", millis("2026-05-01"), 90.0),
                TempReading("t2", "v", millis("2026-05-09"), 100.0),
                TempReading("t3", "v", millis("2026-05-10"), 80.0),
                TempReading("other", "w", millis("2026-05-11"), 50.0),
            ),
        )
        val p = data.jettingPeriods("v", ZoneOffset.UTC)
        assertEquals(listOf("b", "a"), p.map { it.entry.id })
        assertEquals(1, p[0].temps!!.count)
        assertEquals(80.0, p[0].temps!!.avg, 0.0)
        assertEquals(2, p[1].temps!!.count)
        assertEquals(95.0, p[1].temps!!.avg, 0.0)
        assertEquals(90.0, p[1].temps!!.min, 0.0)
        assertEquals(LocalDate.of(2026, 5, 10), p[1].until)
    }

    @Test
    fun tyreStatusUsesTheLastFittedTyreAndItsChecks() {
        val data = AppData(
            tyres = listOf(
                TyreEntry("old", "v", TyrePosition.REAR, TyreEvent.FITTED, "2025-01-01", odometer = 1000.0),
                TyreEntry("c1", "v", TyrePosition.REAR, TyreEvent.CHECK, "2025-03-01", pressureBar = 2.0),
                TyreEntry("new", "v", TyrePosition.REAR, TyreEvent.FITTED, "2026-01-01", odometer = 5000.0),
                TyreEntry("front", "v", TyrePosition.FRONT, TyreEvent.CHECK, "2026-02-01", pressureBar = 1.8),
            ),
            odometer = listOf(OdometerReading("o", "v", "2026-03-01", 6200.0)),
        )
        val rear = data.tyreStatus("v", TyrePosition.REAR)
        assertEquals("new", rear.fitted!!.id)
        assertEquals(1200.0, rear.kmOnTyre!!, 0.001)
        // The check was on the old tyre.
        assertNull(rear.lastCheck)
        val front = data.tyreStatus("v", TyrePosition.FRONT)
        assertNull(front.fitted)
        assertEquals("front", front.lastCheck!!.id)
    }

    @Test
    fun kmInYearSplitsAStretchOverNewYear() {
        val pts = listOf(
            OdoPoint(LocalDate.of(2025, 12, 22), 1000.0),
            OdoPoint(LocalDate.of(2026, 1, 11), 1200.0), // 20 days, 10 of them in 2026
            OdoPoint(LocalDate.of(2026, 6, 1), 2000.0),
            OdoPoint(LocalDate.of(2026, 7, 1), 100.0), // gauge swap
            OdoPoint(LocalDate.of(2026, 8, 1), 300.0),
        )
        assertEquals(100.0 + 800.0 + 200.0, kmInYear(pts, 2026), 0.001)
        assertEquals(100.0, kmInYear(pts, 2025), 0.001)
        assertEquals(0.0, kmInYear(pts, 2024), 0.001)
    }
}
