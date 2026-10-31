package com.vknxworks.ridectrl

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.ridectrl.ui.lock.AppLockGate
import com.vknxworks.ridectrl.ui.lock.lockCovering
import com.vknxworks.ridectrl.ui.nav.AppRoot
import com.vknxworks.ridectrl.ui.theme.RideCtrlTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val c = container
        setContent {
            // Follows the phone's dark theme setting. Status and navigation bars take the background colour,
            // with light icons in dark and dark icons in light.
            val dark = isSystemInDarkTheme()
            DisposableEffect(dark) {
                val bars = if (dark) SystemBarStyle.dark(DARK_BAR) else SystemBarStyle.light(LIGHT_BAR, DARK_BAR)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                onDispose { }
            }
            val settings by c.settings.settings.collectAsStateWithLifecycle()
            // "Hide in screenshots": FLAG_SECURE (also blanks the Recents preview). Always on, whatever the
            // switch says, while the lock screen or a PIN screen shows.
            val secure = settings.hideScreenshots || lockCovering(c.vault)
            // "Hide in recent apps": a blank Recents card; screenshots still work. Always with the app lock on,
            // so Recents never shows what's behind the lock.
            val blankRecents = settings.hideRecents || c.vault.enabled
            LaunchedEffect(secure, blankRecents) {
                if (secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                setRecentsScreenshotEnabled(!blankRecents)
            }
            RideCtrlTheme(dark) {
                // Nothing of the app is shown while the app lock is locked.
                AppLockGate { AppRoot() }
            }
        }
    }
}

/** The Ctrl / Signal backgrounds behind the status and navigation bars: dark #08080A, light #F5F5F1. */
private val DARK_BAR = android.graphics.Color.rgb(8, 8, 10)
private val LIGHT_BAR = android.graphics.Color.rgb(245, 245, 241)
