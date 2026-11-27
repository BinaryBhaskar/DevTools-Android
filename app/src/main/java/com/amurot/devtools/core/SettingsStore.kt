package com.amurot.devtools.core

/** The two Android settings tables DevTools writes to. */
enum class SettingsTable { GLOBAL, SYSTEM }

/** A key in one of the Android settings tables. */
data class SettingKey(val table: SettingsTable, val name: String)

/**
 * Thin read/write abstraction over [android.provider.Settings], so the toggle rules in
 * [DevToggles] can be unit tested without a device.
 */
interface SettingsStore {
    fun getString(key: SettingKey): String?

    /** Writes [value]. Returns false when the platform refused the write. */
    fun putString(key: SettingKey, value: String): Boolean
}

fun SettingsStore.getInt(key: SettingKey, default: Int = 0): Int =
    getString(key)?.trim()?.toIntOrNull() ?: default

fun SettingsStore.getFloat(key: SettingKey, default: Float = 1f): Float =
    getString(key)?.trim()?.toFloatOrNull() ?: default
