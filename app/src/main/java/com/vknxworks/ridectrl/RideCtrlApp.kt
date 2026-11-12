package com.vknxworks.ridectrl

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.vknxworks.ridectrl.reminders.Reminders
import com.vknxworks.ridectrl.ui.lock.AppLock
import kotlinx.coroutines.launch

class RideCtrlApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Reminders: the notification channel, and the next daily check (also after an update).
        Reminders.createChannel(this)
        container.appScope.launch { runCatching { Reminders.schedule(this@RideCtrlApp) } }
        // The app lock: locks after the chosen time away ("Lock again").
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                AppLock.onForeground(container.vault.enabled, container.settings.settings.value.lockTimeout.millis)
            }

            override fun onStop(owner: LifecycleOwner) {
                AppLock.onBackground(container.vault.enabled, container.settings.settings.value.lockTimeout.millis)
            }
        })
    }
}
