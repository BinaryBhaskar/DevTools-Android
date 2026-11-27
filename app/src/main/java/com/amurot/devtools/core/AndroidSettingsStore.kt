package com.amurot.devtools.core

import android.content.ContentResolver
import android.net.Uri
import android.provider.Settings
import android.util.Log

/**
 * Real Settings access. When Android refuses a write (for example Show taps, which apps can't
 * change even with WRITE_SECURE_SETTINGS), [fallback] gets a chance to write it another way.
 */
class AndroidSettingsStore(
    private val resolver: ContentResolver,
    private val fallback: ((SettingKey, String) -> Boolean)? = null,
) : SettingsStore {
    override fun getString(key: SettingKey): String? = when (key.table) {
        SettingsTable.GLOBAL -> Settings.Global.getString(resolver, key.name)
        SettingsTable.SYSTEM -> Settings.System.getString(resolver, key.name)
    }

    override fun putString(key: SettingKey, value: String): Boolean =
        putDirect(key, value) || fallback?.invoke(key, value) == true

    private fun putDirect(key: SettingKey, value: String): Boolean = try {
        when (key.table) {
            SettingsTable.GLOBAL -> Settings.Global.putString(resolver, key.name, value)
            SettingsTable.SYSTEM -> Settings.System.putString(resolver, key.name, value)
        }
    } catch (e: SecurityException) {
        Log.w(TAG, "Write refused for ${key.name}", e)
        false
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "Write refused for ${key.name}", e)
        false
    }

    companion object {
        private const val TAG = "DevTools"

        fun uriFor(key: SettingKey): Uri = when (key.table) {
            SettingsTable.GLOBAL -> Settings.Global.getUriFor(key.name)
            SettingsTable.SYSTEM -> Settings.System.getUriFor(key.name)
        }
    }
}
