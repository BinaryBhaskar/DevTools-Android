package com.amurot.devtools.core

import android.content.ContentResolver
import android.net.Uri
import android.provider.Settings
import android.util.Log

/** Real Settings access. */
class AndroidSettingsStore(private val resolver: ContentResolver) : SettingsStore {
    override fun getString(key: SettingKey): String? = when (key.table) {
        SettingsTable.GLOBAL -> Settings.Global.getString(resolver, key.name)
    }

    override fun putString(key: SettingKey, value: String): Boolean = try {
        when (key.table) {
            SettingsTable.GLOBAL -> Settings.Global.putString(resolver, key.name, value)
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
        }
    }
}
