package com.amurot.devtools.core

/** On/off developer switches. */
enum class Toggle(val key: SettingKey) {
    DEV_OPTIONS(SettingKey(SettingsTable.GLOBAL, "development_settings_enabled")),
    USB_DEBUGGING(SettingKey(SettingsTable.GLOBAL, "adb_enabled")),
    WIRELESS_DEBUGGING(SettingKey(SettingsTable.GLOBAL, "adb_wifi_enabled")),
    STAY_AWAKE(SettingKey(SettingsTable.GLOBAL, "stay_on_while_plugged_in")),
    SHOW_TAPS(SettingKey(SettingsTable.SYSTEM, "show_touches")),
}

/** The three window/transition/animator scales, changed together. */
object AnimationScale {
    val keys = listOf(
        SettingKey(SettingsTable.GLOBAL, "window_animation_scale"),
        SettingKey(SettingsTable.GLOBAL, "transition_animation_scale"),
        SettingKey(SettingsTable.GLOBAL, "animator_duration_scale"),
    )

    /** Values the tile cycles through, in order. */
    val steps = listOf(1f, 0.5f, 0f)

    fun next(current: Float): Float {
        val index = steps.indexOfFirst { it == current }
        return if (index == -1) steps.first() else steps[(index + 1) % steps.size]
    }

    fun label(scale: Float): String = when (scale) {
        0f -> "Off"
        1f -> "1x"
        else -> "${scale.toString().removeSuffix(".0")}x"
    }
}

/** Why a change did not (fully) happen. */
enum class Problem { NO_PERMISSION, NO_WIFI, WRITE_REFUSED }

data class Outcome(val problem: Problem? = null) {
    val ok get() = problem == null
}

/**
 * The rules behind every tile, widget and switch.
 *
 * - Turning on USB or wireless debugging also turns on Developer options, as Settings would.
 * - Turning off Developer options leaves ADB alone, so a tap never silently cuts the
 *   connection you are working over.
 * - Wireless debugging needs Wi-Fi; Android resets it to off otherwise.
 */
class DevToggles(
    private val store: SettingsStore,
    private val hasPermission: () -> Boolean,
    private val isWifiConnected: () -> Boolean,
) {
    fun isOn(toggle: Toggle): Boolean = store.getInt(toggle.key) != 0

    fun set(toggle: Toggle, on: Boolean): Outcome {
        if (!hasPermission()) return Outcome(Problem.NO_PERMISSION)
        if (on && toggle == Toggle.WIRELESS_DEBUGGING && !isWifiConnected()) return Outcome(Problem.NO_WIFI)
        if (on && (toggle == Toggle.USB_DEBUGGING || toggle == Toggle.WIRELESS_DEBUGGING)) {
            if (!write(Toggle.DEV_OPTIONS, true)) return Outcome(Problem.WRITE_REFUSED)
        }
        return if (write(toggle, on)) Outcome() else Outcome(Problem.WRITE_REFUSED)
    }

    fun toggle(toggle: Toggle): Outcome = set(toggle, !isOn(toggle))

    /**
     * "All three" counts as on when Developer options and USB debugging are on, and wireless
     * debugging is on too (or cannot be, because there is no Wi-Fi).
     */
    fun isAllOn(): Boolean =
        isOn(Toggle.DEV_OPTIONS) && isOn(Toggle.USB_DEBUGGING) &&
            (isOn(Toggle.WIRELESS_DEBUGGING) || !isWifiConnected())

    /** Turns all three off when they are all on, otherwise turns all three on. */
    fun toggleAll(): Outcome = setAll(!isAllOn())

    fun setAll(on: Boolean): Outcome {
        if (!hasPermission()) return Outcome(Problem.NO_PERMISSION)
        return if (on) {
            val written = write(Toggle.DEV_OPTIONS, true) && write(Toggle.USB_DEBUGGING, true)
            when {
                !written -> Outcome(Problem.WRITE_REFUSED)
                !isWifiConnected() -> Outcome(Problem.NO_WIFI)
                !write(Toggle.WIRELESS_DEBUGGING, true) -> Outcome(Problem.WRITE_REFUSED)
                else -> Outcome()
            }
        } else {
            // Wireless first, Developer options last: the reverse of turning on.
            val written = write(Toggle.WIRELESS_DEBUGGING, false) &&
                write(Toggle.USB_DEBUGGING, false) &&
                write(Toggle.DEV_OPTIONS, false)
            if (written) Outcome() else Outcome(Problem.WRITE_REFUSED)
        }
    }

    fun animationScale(): Float = store.getFloat(AnimationScale.keys.first())

    fun setAnimationScale(scale: Float): Outcome {
        if (!hasPermission()) return Outcome(Problem.NO_PERMISSION)
        val value = scale.toString()
        val written = AnimationScale.keys.all { store.putString(it, value) }
        return if (written) Outcome() else Outcome(Problem.WRITE_REFUSED)
    }

    fun cycleAnimationScale(): Outcome = setAnimationScale(AnimationScale.next(animationScale()))

    private fun write(toggle: Toggle, on: Boolean): Boolean =
        store.putString(toggle.key, if (on) onValue(toggle) else "0")

    private fun onValue(toggle: Toggle): String = when (toggle) {
        // Stay awake while charging from AC, USB and wireless chargers (bit flags 1 | 2 | 4).
        Toggle.STAY_AWAKE -> "7"
        else -> "1"
    }
}
