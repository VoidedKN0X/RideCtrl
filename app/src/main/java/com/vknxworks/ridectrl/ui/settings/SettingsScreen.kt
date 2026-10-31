package com.vknxworks.ridectrl.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.container
import com.vknxworks.ridectrl.data.lock.LockVault
import com.vknxworks.ridectrl.data.settings.AppSettings
import com.vknxworks.ridectrl.data.settings.DistanceUnit
import com.vknxworks.ridectrl.data.settings.LockTimeout
import com.vknxworks.ridectrl.data.settings.TempUnit
import com.vknxworks.ridectrl.data.settings.VolumeUnit
import com.vknxworks.ridectrl.ui.components.Group
import com.vknxworks.ridectrl.ui.components.GroupChoice
import com.vknxworks.ridectrl.ui.components.GroupRow
import com.vknxworks.ridectrl.ui.components.GroupSwitch
import com.vknxworks.ridectrl.ui.components.ScreenTopBar
import com.vknxworks.ridectrl.ui.lock.AppLock
import com.vknxworks.ridectrl.ui.lock.PinRequest
import com.vknxworks.ridectrl.ui.lock.findActivity
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Settings, opened from the You page (tap the profile picture). Backup and About are on the You page itself.
 * RideCtrl follows the phone's dark theme setting (Ctrl / Signal), so there are no theme settings.
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    fun set(transform: (AppSettings) -> AppSettings) {
        scope.launch { c.settings.update(transform) }
    }
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.settings))
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(top = 4.dp, bottom = 24.dp),
        ) {
            PrivacyAndSecurityGroup(settings)

            UnitsGroup(settings) { t -> set(t) }
        }
    }
}

/** Units and currency. Everything is stored metric; these only change how numbers are shown and typed. */
@Composable
private fun UnitsGroup(settings: AppSettings, set: ((AppSettings) -> AppSettings) -> Unit) {
    Group(stringResource(R.string.group_units), footer = stringResource(R.string.units_footer)) {
        GroupChoice(
            stringResource(R.string.unit_distance),
            DistanceUnit.entries.map { it to stringResource(it.label) },
            settings.distance,
            icon = Icons.Outlined.Straighten,
        ) { u -> set { it.copy(distance = u) } }
        GroupChoice(
            stringResource(R.string.unit_volume),
            VolumeUnit.entries.map { it to stringResource(it.label) },
            settings.volume,
            icon = Icons.Outlined.LocalGasStation,
        ) { u -> set { it.copy(volume = u) } }
        GroupChoice(
            stringResource(R.string.unit_temperature),
            TempUnit.entries.map { it to stringResource(it.label) },
            settings.temperature,
            icon = Icons.Outlined.Thermostat,
        ) { u -> set { it.copy(temperature = u) } }
        GroupChoice(
            stringResource(R.string.unit_currency),
            CURRENCIES.map { it to currencyLabel(it) },
            settings.currency,
            icon = Icons.Outlined.Payments,
        ) { cur -> set { it.copy(currency = cur) } }
    }
}

/** Common currencies; the amount is stored as typed, in the chosen currency. */
private val CURRENCIES = listOf("EUR", "USD", "GBP", "CHF", "SEK", "NOK", "DKK", "PLN", "CZK", "HUF", "CAD", "AUD", "JPY")

private fun currencyLabel(code: String): String =
    runCatching { java.util.Currency.getInstance(code) }.getOrNull()
        ?.let { "${it.getDisplayName(Locale.getDefault())} (${it.getSymbol(Locale.getDefault())})" }
        ?: code

/** Privacy and security: the shared Ctrl series app lock, then hiding RideCtrl in screenshots. */
@Composable
private fun PrivacyAndSecurityGroup(settings: AppSettings) {
    val context = LocalContext.current
    val c = context.container
    val bioAvailable = remember { AppLock.biometricAvailable(context) }
    val lockOn = c.vault.enabled
    Group(stringResource(R.string.group_privacy), footer = stringResource(R.string.app_lock_footer)) {
        GroupSwitch(
            stringResource(R.string.app_lock),
            stringResource(R.string.app_lock_subtitle),
            checked = lockOn,
            icon = Icons.Outlined.Lock,
        ) { on -> AppLock.request = if (on) PinRequest.ENABLE else PinRequest.DISABLE }
        if (lockOn) {
            GroupRow(
                stringResource(R.string.lock_change_pin),
                stringResource(R.string.lock_change_pin_subtitle, LockVault.PIN_MIN),
                icon = Icons.Outlined.Password,
                showChevron = true,
                onClick = { AppLock.request = PinRequest.CHANGE },
            )
            GroupSwitch(
                stringResource(R.string.lock_fingerprint),
                stringResource(if (bioAvailable || c.vault.biometricEnabled) R.string.lock_fingerprint_subtitle else R.string.lock_fingerprint_unavailable),
                checked = c.vault.biometricEnabled,
                icon = Icons.Outlined.Fingerprint,
            ) { on ->
                if (!on) {
                    c.vault.disableBiometric()
                } else if (bioAvailable) {
                    val activity = context.findActivity()
                    val cipher = c.vault.biometricEnrolCipher()
                    if (activity == null || cipher == null) {
                        Toast.makeText(context, context.getString(R.string.lock_fingerprint_setup_failed), Toast.LENGTH_SHORT).show()
                    } else {
                        AppLock.biometricPrompt(
                            activity,
                            context.getString(R.string.lock_fingerprint_enable_title),
                            context.getString(R.string.lock_fingerprint_enable_subtitle),
                            cipher,
                        ) { unlocked ->
                            if (!c.vault.finishBiometricEnrol(unlocked)) {
                                Toast.makeText(context, context.getString(R.string.lock_fingerprint_setup_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            GroupChoice(
                stringResource(R.string.lock_again),
                LockTimeout.entries.map { it to stringResource(it.label) },
                settings.lockTimeout,
                icon = Icons.Outlined.Timer,
            ) { t -> c.appScope.launch { c.settings.update { it.copy(lockTimeout = t) } } }
        }
        GroupSwitch(
            stringResource(R.string.hide_screenshots),
            stringResource(R.string.hide_screenshots_subtitle),
            checked = settings.hideScreenshots,
            icon = Icons.Outlined.VisibilityOff,
        ) { on -> c.appScope.launch { c.settings.update { it.copy(hideScreenshots = on) } } }
        GroupSwitch(
            stringResource(R.string.hide_recents),
            stringResource(R.string.hide_recents_subtitle),
            checked = settings.hideRecents,
            icon = Icons.Outlined.Layers,
        ) { on -> c.appScope.launch { c.settings.update { it.copy(hideRecents = on) } } }
    }
}
