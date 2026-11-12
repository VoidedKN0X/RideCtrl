package com.vknxworks.ridectrl.data

import com.vknxworks.ridectrl.data.settings.AppSettings
import com.vknxworks.ridectrl.data.settings.DistanceUnit
import com.vknxworks.ridectrl.data.settings.TempUnit
import com.vknxworks.ridectrl.data.settings.VolumeUnit
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

const val KM_PER_MILE = 1.609344
const val LITRES_PER_US_GAL = 3.785411784
const val LITRES_PER_UK_GAL = 4.54609

/**
 * Reads a number typed with either a comma or a period as the decimal separator ("2,5" and "2.5" are both
 * 2.5). Spaces and apostrophes used as thousands separators are ignored. Blank or invalid gives null.
 */
fun parseNumber(text: String): Double? {
    val t = text.trim().replace(" ", "").replace(" ", "").replace(" ", "").replace("'", "")
    if (t.isEmpty()) return null
    val lastComma = t.lastIndexOf(',')
    val lastDot = t.lastIndexOf('.')
    val normalized = when {
        // Both used: the last one is the decimal separator, the other groups thousands.
        lastComma >= 0 && lastDot >= 0 ->
            if (lastComma > lastDot) t.replace(".", "").replace(',', '.') else t.replace(",", "")
        lastComma >= 0 -> t.replace(',', '.')
        else -> t
    }
    return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
}

fun parseInt(text: String): Int? = parseNumber(text)?.let { if (it % 1.0 == 0.0) it.toInt() else null }

/** Today's date as stored (yyyy-MM-dd). */
fun today(): String = LocalDate.now().toString()

fun parseDate(s: String?): LocalDate? = s?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/** Formats numbers, units, money and dates for the user's settings and language. */
class Fmt(val settings: AppSettings, private val locale: Locale = Locale.getDefault()) {
    private fun number(v: Double, decimals: Int): String =
        NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = decimals
            minimumFractionDigits = 0
        }.format(v)

    private fun fixed(v: Double, decimals: Int): String =
        NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = decimals
            minimumFractionDigits = decimals
        }.format(v)

    // ----- Distance -----
    val distanceUnit: String get() = if (settings.distance == DistanceUnit.MI) "mi" else "km"
    fun kmToUser(km: Double): Double = if (settings.distance == DistanceUnit.MI) km / KM_PER_MILE else km
    fun userToKm(v: Double): Double = if (settings.distance == DistanceUnit.MI) v * KM_PER_MILE else v
    fun distance(km: Double?, decimals: Int = 0): String = km?.let { "${number(kmToUser(it), decimals)} $distanceUnit" } ?: "–"
    /** A km value as the user would type it (no unit, no grouping). */
    fun distanceInput(km: Double?): String = km?.let { plain(kmToUser(it), 1) } ?: ""

    // ----- Volume -----
    val volumeUnit: String
        get() = when (settings.volume) {
            VolumeUnit.LITRE -> "L"
            VolumeUnit.US_GALLON -> "gal"
            VolumeUnit.UK_GALLON -> "imp gal"
        }
    fun litresToUser(l: Double): Double = when (settings.volume) {
        VolumeUnit.LITRE -> l
        VolumeUnit.US_GALLON -> l / LITRES_PER_US_GAL
        VolumeUnit.UK_GALLON -> l / LITRES_PER_UK_GAL
    }
    fun userToLitres(v: Double): Double = when (settings.volume) {
        VolumeUnit.LITRE -> v
        VolumeUnit.US_GALLON -> v * LITRES_PER_US_GAL
        VolumeUnit.UK_GALLON -> v * LITRES_PER_UK_GAL
    }
    fun volume(l: Double?, decimals: Int = 2): String = l?.let { "${number(litresToUser(it), decimals)} $volumeUnit" } ?: "–"
    fun volumeInput(l: Double?): String = l?.let { plain(litresToUser(it), 3) } ?: ""

    /** Consumption in the user's units: L/100 km and km/L for metric, mpg for miles. */
    fun consumption(litresPer100km: Double?): String {
        if (litresPer100km == null || litresPer100km <= 0) return "–"
        return if (settings.distance == DistanceUnit.MI) {
            val gal = if (settings.volume == VolumeUnit.UK_GALLON) LITRES_PER_UK_GAL else LITRES_PER_US_GAL
            val mpg = 100.0 / litresPer100km * gal / KM_PER_MILE
            "${number(mpg, 1)} mpg"
        } else {
            "${number(litresPer100km, 2)} L/100 km"
        }
    }
    /** The number [consumption] shows (L/100 km, or mpg with miles), for charts. */
    fun consumptionValue(litresPer100km: Double): Double {
        if (settings.distance != DistanceUnit.MI || litresPer100km <= 0) return litresPer100km
        val gal = if (settings.volume == VolumeUnit.UK_GALLON) LITRES_PER_UK_GAL else LITRES_PER_US_GAL
        return 100.0 / litresPer100km * gal / KM_PER_MILE
    }
    val consumptionUnit: String get() = if (settings.distance == DistanceUnit.MI) "mpg" else "L/100 km"

    fun economy(litresPer100km: Double?): String? {
        if (litresPer100km == null || litresPer100km <= 0 || settings.distance == DistanceUnit.MI) return null
        return "${number(100.0 / litresPer100km, 1)} km/L"
    }

    // ----- Temperature -----
    val tempUnit: String get() = if (settings.temperature == TempUnit.FAHRENHEIT) "°F" else "°C"
    fun cToUser(c: Double): Double = if (settings.temperature == TempUnit.FAHRENHEIT) c * 9.0 / 5.0 + 32.0 else c
    fun userToC(v: Double): Double = if (settings.temperature == TempUnit.FAHRENHEIT) (v - 32.0) * 5.0 / 9.0 else v
    fun temp(c: Double?): String = c?.let { "${number(cToUser(it), 0)} $tempUnit" } ?: "–"
    fun tempInput(c: Double?): String = c?.let { plain(cToUser(it), 1) } ?: ""

    // ----- Money -----
    private val currency: Currency = runCatching { Currency.getInstance(settings.currency) }.getOrDefault(Currency.getInstance("EUR"))
    val currencySymbol: String get() = currency.getSymbol(locale)
    fun money(v: Double?): String = v?.let {
        NumberFormat.getCurrencyInstance(locale).apply { this.currency = this@Fmt.currency }.format(it)
    } ?: "–"
    /** Money per distance unit, e.g. "€0.042 / km". */
    fun moneyPerDistance(perKm: Double?): String = perKm?.let {
        val perUnit = if (settings.distance == DistanceUnit.MI) it * KM_PER_MILE else it
        "$currencySymbol${fixed(perUnit, 3)} / $distanceUnit"
    } ?: "–"
    fun moneyInput(v: Double?): String = v?.let { plain(it, 2) } ?: ""

    // ----- Plain numbers -----
    fun num(v: Double?, decimals: Int = 1): String = v?.let { number(it, decimals) } ?: "–"

    /** A number to put back into a text field: no grouping, comma or period as the phone's language uses. */
    fun plain(v: Double, decimals: Int): String {
        val s = NumberFormat.getNumberInstance(locale).apply {
            isGroupingUsed = false
            maximumFractionDigits = decimals
            minimumFractionDigits = 0
        }.format(v)
        return s
    }

    // ----- Dates -----
    fun date(iso: String?): String = parseDate(iso)?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) ?: "–"
    fun shortDate(iso: String?): String = parseDate(iso)?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)) ?: "–"
    fun dateTime(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale))
}
