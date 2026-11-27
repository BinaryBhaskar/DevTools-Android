package com.amurot.devtools.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.amurot.devtools.widget.WidgetRefresher

/** Everything a tile, widget or screen can do. */
enum class DevAction { DEV_OPTIONS, USB_DEBUGGING, WIRELESS_DEBUGGING, ALL, STAY_AWAKE, SHOW_TAPS, ANIMATIONS }

/**
 * Android-facing entry point: runs [DevAction]s through [DevToggles], then refreshes
 * every tile and widget so they all show the new state.
 */
class DevController(context: Context) {
    private val app = context.applicationContext

    val toggles = DevToggles(
        store = AndroidSettingsStore(app.contentResolver),
        hasPermission = ::hasPermission,
        isWifiConnected = ::isWifiConnected,
    )

    fun hasPermission(): Boolean =
        app.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    fun isWifiConnected(): Boolean {
        val cm = app.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun isActive(action: DevAction): Boolean = when (action) {
        DevAction.DEV_OPTIONS -> toggles.isOn(Toggle.DEV_OPTIONS)
        DevAction.USB_DEBUGGING -> toggles.isOn(Toggle.USB_DEBUGGING)
        DevAction.WIRELESS_DEBUGGING -> toggles.isOn(Toggle.WIRELESS_DEBUGGING)
        DevAction.ALL -> toggles.isAllOn()
        DevAction.STAY_AWAKE -> toggles.isOn(Toggle.STAY_AWAKE)
        DevAction.SHOW_TAPS -> toggles.isOn(Toggle.SHOW_TAPS)
        DevAction.ANIMATIONS -> toggles.animationScale() != 0f
    }

    /** Short status for tile subtitles and widgets. */
    fun status(action: DevAction): String = when {
        action == DevAction.ANIMATIONS -> AnimationScale.label(toggles.animationScale())
        !hasPermission() -> "Setup needed"
        isActive(action) -> "On"
        else -> "Off"
    }

    fun perform(action: DevAction, showFeedback: Boolean = true): Outcome {
        val outcome = when (action) {
            DevAction.DEV_OPTIONS -> toggles.toggle(Toggle.DEV_OPTIONS)
            DevAction.USB_DEBUGGING -> toggles.toggle(Toggle.USB_DEBUGGING)
            DevAction.WIRELESS_DEBUGGING -> toggles.toggle(Toggle.WIRELESS_DEBUGGING)
            DevAction.ALL -> toggles.toggleAll()
            DevAction.STAY_AWAKE -> toggles.toggle(Toggle.STAY_AWAKE)
            DevAction.SHOW_TAPS -> toggles.toggle(Toggle.SHOW_TAPS)
            DevAction.ANIMATIONS -> toggles.cycleAnimationScale()
        }
        if (showFeedback) outcome.problem?.let { toast(message(it, action)) }
        refreshSurfaces()
        return outcome
    }

    fun set(toggle: Toggle, on: Boolean): Outcome =
        toggles.set(toggle, on).also { it.problem?.let { p -> toast(message(p, null)) }; refreshSurfaces() }

    fun setAll(on: Boolean): Outcome =
        toggles.setAll(on).also { it.problem?.let { p -> toast(message(p, DevAction.ALL)) }; refreshSurfaces() }

    fun setAnimationScale(scale: Float): Outcome =
        toggles.setAnimationScale(scale).also { it.problem?.let { p -> toast(message(p, null)) }; refreshSurfaces() }

    fun refreshSurfaces() {
        // Also asks every tile to re-read its state.
        WidgetRefresher.refreshAll(app)
    }

    private fun message(problem: Problem, action: DevAction?): String = when (problem) {
        Problem.NO_PERMISSION -> "DevTools needs the WRITE_SECURE_SETTINGS grant. Open the app for setup."
        Problem.NO_WIFI ->
            if (action == DevAction.ALL) "Developer options and USB debugging are on. Connect to Wi-Fi for wireless debugging."
            else "Connect to Wi-Fi to turn on wireless debugging."
        Problem.WRITE_REFUSED -> "Android refused this change on this device."
    }

    private fun toast(text: String) {
        Handler(Looper.getMainLooper()).post { Toast.makeText(app, text, Toast.LENGTH_LONG).show() }
    }

    companion object {
        /** All settings DevTools reads, for content observers and job triggers. */
        val watchedKeys: List<SettingKey> = Toggle.entries.map { it.key } + AnimationScale.keys.first()

        fun observe(context: Context, onChange: () -> Unit): ContentObserver {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) = onChange()
            }
            watchedKeys.forEach {
                context.contentResolver.registerContentObserver(AndroidSettingsStore.uriFor(it), false, observer)
            }
            return observer
        }
    }
}
