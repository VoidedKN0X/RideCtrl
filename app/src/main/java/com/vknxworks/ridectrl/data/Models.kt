package com.vknxworks.ridectrl.data

import kotlinx.serialization.Serializable

/*
 * Everything RideCtrl stores, as plain serializable data. All distances are kilometres, volumes litres,
 * temperatures °C; dates are ISO yyyy-MM-dd strings (local dates, no time zone) and moments are epoch millis.
 * Money is stored as typed, in the currency chosen in Settings.
 */

@Serializable
enum class EngineType { TWO_STROKE_PREMIX, TWO_STROKE_INJECTION, FOUR_STROKE, ELECTRIC }

val EngineType.isTwoStroke: Boolean get() = this == EngineType.TWO_STROKE_PREMIX || this == EngineType.TWO_STROKE_INJECTION

@Serializable
data class Vehicle(
    val id: String,
    val name: String,
    val make: String = "",
    val model: String = "",
    val year: Int? = null,
    val engine: EngineType = EngineType.TWO_STROKE_PREMIX,
    val plate: String = "",
    /** VIN or frame number; shown only after a tap. */
    val vin: String = "",
    val purchaseDate: String? = null,
    val purchasePrice: Double? = null,
    val photo: String? = null,
    val notes: String = "",
    /** Premix: the ratio this vehicle normally runs (40 means 40:1), and the oil used. */
    val premixRatio: Int? = null,
    val oilBrand: String = "",
    /** The specs sheet: every field is filled in by the user, nothing is "official". */
    val specs: List<SpecField> = emptyList(),
    val created: Long = 0L,
)

@Serializable
data class SpecField(val key: String, val value: String = "", val source: String = "")

@Serializable
data class FuelEntry(
    val id: String,
    val vehicleId: String,
    val date: String,
    val odometer: Double? = null,
    val litres: Double,
    val pricePerLitre: Double? = null,
    val total: Double? = null,
    val station: String = "",
    val fullTank: Boolean = true,
    /** Premix ratio used for this fill (40 means 40:1), if any. */
    val premixRatio: Int? = null,
    val oilMl: Double? = null,
    val oilCost: Double? = null,
    val notes: String = "",
)

@Serializable
data class OdometerReading(
    val id: String,
    val vehicleId: String,
    val date: String,
    val km: Double,
    val note: String = "",
)

@Serializable
data class Trip(
    val id: String,
    val vehicleId: String,
    val date: String,
    val km: Double,
    val purpose: String = "",
)

@Serializable
data class ServiceItem(
    val id: String,
    val vehicleId: String,
    val name: String,
    val intervalKm: Double? = null,
    val intervalDays: Int? = null,
    val notes: String = "",
    /** When it was last done before RideCtrl knew about it (used until a service record includes it). */
    val baselineDate: String? = null,
    val baselineKm: Double? = null,
)

@Serializable
enum class DoneBy { ME, SHOP }

@Serializable
data class PartUse(val partId: String, val quantity: Int = 1)

@Serializable
data class ServiceRecord(
    val id: String,
    val vehicleId: String,
    val date: String,
    val odometer: Double? = null,
    val itemIds: List<String> = emptyList(),
    val parts: List<PartUse> = emptyList(),
    /** Parts that didn't come from the inventory. Inventory parts are counted from their own price. */
    val otherPartsCost: Double? = null,
    val labourCost: Double? = null,
    val doneBy: DoneBy = DoneBy.ME,
    val shop: String = "",
    val photos: List<String> = emptyList(),
    val notes: String = "",
)

@Serializable
data class Part(
    val id: String,
    val name: String,
    val brand: String = "",
    val partNumber: String = "",
    val whereBought: String = "",
    /** Price per piece. */
    val price: Double? = null,
    val quantity: Int = 0,
    val vehicleIds: List<String> = emptyList(),
    /** Warn when the stock is at or below this number. */
    val lowStockAt: Int? = null,
    val notes: String = "",
)

@Serializable
data class Mod(
    val id: String,
    val vehicleId: String,
    val date: String,
    val odometer: Double? = null,
    val title: String,
    val description: String = "",
    val parts: List<PartUse> = emptyList(),
    /** Cost besides inventory parts. */
    val cost: Double? = null,
    val beforePhotos: List<String> = emptyList(),
    val afterPhotos: List<String> = emptyList(),
    val tuningNotes: String = "",
)

@Serializable
enum class RideType { CITY, HIGHWAY, UPHILL, IDLE, MIXED }

@Serializable
data class TempReading(
    val id: String,
    val vehicleId: String,
    val time: Long,
    val tempC: Double,
    val odometer: Double? = null,
    val ambientC: Double? = null,
    val rideType: RideType = RideType.CITY,
    /** The setup at the time (jetting, exhaust...), used to group readings. */
    val setup: String = "",
    val notes: String = "",
)

@Serializable
enum class DocType { INSURANCE, REGISTRATION, INSPECTION, OTHER }

@Serializable
data class VehicleDoc(
    val id: String,
    val vehicleId: String,
    val type: DocType = DocType.INSURANCE,
    val title: String = "",
    val expiry: String? = null,
    val cost: Double? = null,
    /** Warn this many days before it expires. */
    val warnDays: Int = 30,
    val photos: List<String> = emptyList(),
    val notes: String = "",
)

@Serializable
data class VehicleNote(
    val id: String,
    val vehicleId: String,
    val date: String,
    val text: String,
)

/** The whole store: one JSON file. */
@Serializable
data class AppData(
    val version: Int = 1,
    val vehicles: List<Vehicle> = emptyList(),
    val fuel: List<FuelEntry> = emptyList(),
    val odometer: List<OdometerReading> = emptyList(),
    val trips: List<Trip> = emptyList(),
    val serviceItems: List<ServiceItem> = emptyList(),
    val services: List<ServiceRecord> = emptyList(),
    val parts: List<Part> = emptyList(),
    val mods: List<Mod> = emptyList(),
    val temps: List<TempReading> = emptyList(),
    val docs: List<VehicleDoc> = emptyList(),
    val notes: List<VehicleNote> = emptyList(),
) {
    /** Every photo file name referenced anywhere. */
    fun photoNames(): Set<String> = buildSet {
        vehicles.forEach { v -> v.photo?.let(::add) }
        services.forEach { addAll(it.photos) }
        mods.forEach { addAll(it.beforePhotos); addAll(it.afterPhotos) }
        docs.forEach { addAll(it.photos) }
    }

    /** Removes a vehicle and everything that belongs only to it. Parts stay, minus the link. */
    fun withoutVehicle(id: String): AppData = copy(
        vehicles = vehicles.filterNot { it.id == id },
        fuel = fuel.filterNot { it.vehicleId == id },
        odometer = odometer.filterNot { it.vehicleId == id },
        trips = trips.filterNot { it.vehicleId == id },
        serviceItems = serviceItems.filterNot { it.vehicleId == id },
        services = services.filterNot { it.vehicleId == id },
        parts = parts.map { p -> if (id in p.vehicleIds) p.copy(vehicleIds = p.vehicleIds - id) else p },
        mods = mods.filterNot { it.vehicleId == id },
        temps = temps.filterNot { it.vehicleId == id },
        docs = docs.filterNot { it.vehicleId == id },
        notes = notes.filterNot { it.vehicleId == id },
    )
}

/** The editable 2-stroke scooter starting list. Every interval is blank: the user fills in their own. */
val SCOOTER_TEMPLATE: List<Pair<Int, Int?>> = listOf(
    com.vknxworks.ridectrl.R.string.tpl_spark_plug to null,
    com.vknxworks.ridectrl.R.string.tpl_drive_belt to null,
    com.vknxworks.ridectrl.R.string.tpl_variator to null,
    com.vknxworks.ridectrl.R.string.tpl_clutch to null,
    com.vknxworks.ridectrl.R.string.tpl_gear_oil to null,
    com.vknxworks.ridectrl.R.string.tpl_air_filter to null,
    com.vknxworks.ridectrl.R.string.tpl_fuel_filter to null,
    com.vknxworks.ridectrl.R.string.tpl_brakes_front to null,
    com.vknxworks.ridectrl.R.string.tpl_brakes_rear to null,
    com.vknxworks.ridectrl.R.string.tpl_brake_fluid to null,
    com.vknxworks.ridectrl.R.string.tpl_tyre_front to com.vknxworks.ridectrl.R.string.tpl_tyre_note,
    com.vknxworks.ridectrl.R.string.tpl_tyre_rear to com.vknxworks.ridectrl.R.string.tpl_tyre_note,
    com.vknxworks.ridectrl.R.string.tpl_carb to null,
    com.vknxworks.ridectrl.R.string.tpl_decoke to null,
    com.vknxworks.ridectrl.R.string.tpl_cables to null,
    com.vknxworks.ridectrl.R.string.tpl_battery to null,
)

/** The specs sheet's fields, in order. Values are the user's own; the label says what goes in it. */
val SPEC_KEYS: List<Pair<String, Int>> = listOf(
    "tyre_front" to com.vknxworks.ridectrl.R.string.spec_tyre_front,
    "tyre_rear" to com.vknxworks.ridectrl.R.string.spec_tyre_rear,
    "pressure_front" to com.vknxworks.ridectrl.R.string.spec_pressure_front,
    "pressure_rear" to com.vknxworks.ridectrl.R.string.spec_pressure_rear,
    "spark_plug" to com.vknxworks.ridectrl.R.string.spec_spark_plug,
    "plug_gap" to com.vknxworks.ridectrl.R.string.spec_plug_gap,
    "main_jet" to com.vknxworks.ridectrl.R.string.spec_main_jet,
    "pilot_jet" to com.vknxworks.ridectrl.R.string.spec_pilot_jet,
    "needle_clip" to com.vknxworks.ridectrl.R.string.spec_needle_clip,
    "rollers" to com.vknxworks.ridectrl.R.string.spec_rollers,
    "belt" to com.vknxworks.ridectrl.R.string.spec_belt,
    "bulbs" to com.vknxworks.ridectrl.R.string.spec_bulbs,
    "gear_oil" to com.vknxworks.ridectrl.R.string.spec_gear_oil,
    "two_stroke_oil" to com.vknxworks.ridectrl.R.string.spec_two_stroke_oil,
    "fuel_tank" to com.vknxworks.ridectrl.R.string.spec_fuel_tank,
    "brake_fluid" to com.vknxworks.ridectrl.R.string.spec_brake_fluid,
    "torques" to com.vknxworks.ridectrl.R.string.spec_torques,
    "other" to com.vknxworks.ridectrl.R.string.spec_other,
)
