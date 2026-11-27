package com.amurot.devtools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.amurot.devtools.ui.DevToolsTheme
import com.amurot.devtools.ui.HomeScreen
import com.amurot.devtools.widget.SettingsWatchJob

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SettingsWatchJob.schedule(this)
        setContent { DevToolsTheme { HomeScreen() } }
    }
}
