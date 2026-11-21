package com.vknxworks.ridectrl.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset

class YearReportTest {
    private val v = Vehicle(id = "v", name = "Booster")

    @Test
    fun countsOnlyTheChosenYear() {
        val data = AppData(
            vehicles = listOf(v),
            fuel = listOf(
                FuelEntry("f1", "v", "2025-12-20", odometer = 1000.0, litres = 5.0, total = 10.0),
                FuelEntry("f2", "v", "2026-01-10", odometer = 1100.0, litres = 4.0, total = 8.0),
                FuelEntry("f3", "v", "2026-02-10", odometer = 1300.0, litres = 6.0, total = 12.0),
            ),
            services = listOf(ServiceRecord("s1", "v", "2026-03-01", odometer = 1400.0, labourCost = 20.0, notes = "Belt")),
            tyres = listOf(TyreEntry("t", "v", TyrePosition.REAR, TyreEvent.FITTED, "2026-04-01", cost = 40.0, brand = "Michelin")),
        )
        val r = data.yearReport("v", 2026, ZoneOffset.UTC)!!
        assertEquals(2, r.fills)
        assertEquals(10.0, r.litres, 0.0)
        assertEquals(20.0, r.fuelCost, 0.0)
        // 1000 -> 1100 spans 20 Dec - 10 Jan: 9 of 21 days fall in 2026.
        assertEquals(100.0 * 9 / 21 + 200.0 + 100.0, r.kmRidden, 0.001)
        assertEquals(1400.0, r.odometerAtEnd!!, 0.0)
        assertEquals(1, r.services.size)
        assertEquals("Belt", r.services[0].text)
        assertEquals(TyrePosition.REAR, r.tyres[0].position)
        assertEquals(20.0 + 20.0 + 40.0, r.totalCost, 0.001)
        // Both full-to-full intervals end in 2026: (4 + 6) litres over 300 km.
        assertEquals(10.0 / 300.0 * 100.0, r.avgPer100!!, 0.001)
        assertNull(data.yearReport("x", 2026))
    }
}
