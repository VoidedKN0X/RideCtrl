package com.vknxworks.ridectrl.data.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vknxworks.ridectrl.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "ridectrl")

/** How long RideCtrl can be in the background before the app lock asks again (-1: only after a restart). */
enum class LockTimeout(@StringRes val label: Int, val millis: Long) {
    IMMEDIATELY(R.string.lock_again_immediately, 0L),
    MINUTE_1(R.string.lock_again_1m, 60_000L),
    MINUTES_5(R.string.lock_again_5m, 5 * 60_000L),
    MINUTES_15(R.string.lock_again_15m, 15 * 60_000L),
    HOUR_1(R.string.lock_again_1h, 60 * 60_000L),
    RESTART(R.string.lock_again_restart, -1L),
}

/** Distance unit for odometers, intervals and trips. Everything is stored in km. */
enum class DistanceUnit(@StringRes val label: Int, @StringRes val short: Int) {
    KM(R.string.unit_km_long, R.string.unit_km),
    MI(R.string.unit_mi_long, R.string.unit_mi),
}

/** Fuel volume unit. Everything is stored in litres. */
enum class VolumeUnit(@StringRes val label: Int, @StringRes val short: Int) {
    LITRE(R.string.unit_l_long, R.string.unit_l),
    US_GALLON(R.string.unit_usgal_long, R.string.unit_usgal),
    UK_GALLON(R.string.unit_ukgal_long, R.string.unit_ukgal),
}

/** Temperature unit. Everything is stored in °C. */
enum class TempUnit(@StringRes val label: Int, @StringRes val short: Int) {
    CELSIUS(R.string.unit_c_long, R.string.unit_c),
    FAHRENHEIT(R.string.unit_f_long, R.string.unit_f),
}

data class AppSettings(
    val distance: DistanceUnit = DistanceUnit.KM,
    val volume: VolumeUnit = VolumeUnit.LITRE,
    val temperature: TempUnit = TempUnit.CELSIUS,
    /** ISO 4217 currency code for every amount (purchase, fuel, parts, services). */
    val currency: String = "EUR",
    /** App lock: when it asks again after RideCtrl was in the background. */
    val lockTimeout: LockTimeout = LockTimeout.IMMEDIATELY,
    /** "Hide in screenshots": FLAG_SECURE on the app's windows (screenshots, recordings and the Recents preview). */
    val hideScreenshots: Boolean = false,
    /** "Hide in recent apps": the Recents view shows a blank card. Screenshots still work. */
    val hideRecents: Boolean = false,
    /** Local notifications for maintenance and documents. */
    val reminders: Boolean = true,
    /** Warn this many days before something is due. */
    val warnDays: Int = 14,
    /** Warn this many km before a service item is due (stored in km). */
    val warnKm: Double = 300.0,
    /** The vehicle the widget and shortcuts use; null means the first one. */
    val mainVehicleId: String? = null,
)

class SettingsRepository(private val context: Context, scope: CoroutineScope) {
    private object K {
        val distance = stringPreferencesKey("unit_distance")
        val volume = stringPreferencesKey("unit_volume")
        val temperature = stringPreferencesKey("unit_temperature")
        val currency = stringPreferencesKey("currency")
        val lockTimeout = stringPreferencesKey("lock_timeout")
        val hideScreenshots = booleanPreferencesKey("hide_screenshots")
        val hideRecents = booleanPreferencesKey("hide_recents")
        val reminders = booleanPreferencesKey("reminders")
        val warnDays = stringPreferencesKey("warn_days")
        val warnKm = stringPreferencesKey("warn_km")
        val mainVehicle = stringPreferencesKey("main_vehicle")
    }

    /** The saved settings; if the settings file can't be read, the defaults instead of a crash. */
    private val prefs: Flow<Preferences> = context.appDataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val settings: StateFlow<AppSettings> = prefs
        .map { it.toSettings() }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    /** The stored settings as they're read and changed, without [settings]' placeholder defaults at start-up. */
    val stored: Flow<AppSettings> = prefs.map { it.toSettings() }

    private fun Preferences.toSettings() = AppSettings(
        distance = enumOr(this[K.distance], DistanceUnit.KM),
        volume = enumOr(this[K.volume], VolumeUnit.LITRE),
        temperature = enumOr(this[K.temperature], TempUnit.CELSIUS),
        currency = this[K.currency]?.takeIf { it.length == 3 } ?: "EUR",
        lockTimeout = enumOr(this[K.lockTimeout], LockTimeout.IMMEDIATELY),
        hideScreenshots = this[K.hideScreenshots] ?: false,
        hideRecents = this[K.hideRecents] ?: false,
        reminders = this[K.reminders] ?: true,
        warnDays = this[K.warnDays]?.toIntOrNull()?.coerceIn(0, 365) ?: 14,
        warnKm = this[K.warnKm]?.toDoubleOrNull()?.coerceIn(0.0, 100_000.0) ?: 300.0,
        mainVehicleId = this[K.mainVehicle]?.takeIf { it.isNotBlank() },
    )

    /** The saved settings read straight from disk (reminder alarms and the widget run without the UI). */
    suspend fun current(): AppSettings = stored.first()

    /** Saves a change. If the phone's storage fails, the change is dropped instead of closing the app. */
    suspend fun update(transform: (AppSettings) -> AppSettings) {
        try {
            context.appDataStore.edit { p ->
                val s = transform(p.toSettings())
                p[K.distance] = s.distance.name
                p[K.volume] = s.volume.name
                p[K.temperature] = s.temperature.name
                p[K.currency] = s.currency
                p[K.lockTimeout] = s.lockTimeout.name
                p[K.hideScreenshots] = s.hideScreenshots
                p[K.hideRecents] = s.hideRecents
                p[K.reminders] = s.reminders
                p[K.warnDays] = s.warnDays.toString()
                p[K.warnKm] = s.warnKm.toString()
                if (s.mainVehicleId != null) p[K.mainVehicle] = s.mainVehicleId else p.remove(K.mainVehicle)
            }
        } catch (e: IOException) {
            // Nothing to do: the settings stay as they were.
        }
    }
}

private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
    name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
