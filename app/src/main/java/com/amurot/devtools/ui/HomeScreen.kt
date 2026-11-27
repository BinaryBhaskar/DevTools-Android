package com.amurot.devtools.ui

import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.amurot.devtools.BuildConfig
import com.amurot.devtools.R
import com.amurot.devtools.core.AnimationScale
import com.amurot.devtools.core.DevController
import com.amurot.devtools.core.Toggle
import com.amurot.devtools.tile.TileRefresher
import com.amurot.devtools.widget.WidgetRefresher

const val REPO_URL = "https://github.com/BinaryBhaskar/DevTools-Android"

private data class DevState(
    val granted: Boolean = false,
    val wifi: Boolean = false,
    val on: Map<Toggle, Boolean> = emptyMap(),
    val allOn: Boolean = false,
    val animationScale: Float = 1f,
)

private fun DevController.snapshot() = DevState(
    granted = hasPermission(),
    wifi = isWifiConnected(),
    on = Toggle.entries.associateWith { toggles.isOn(it) },
    allOn = toggles.isAllOn(),
    animationScale = toggles.animationScale(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val controller = remember { DevController(context) }
    var state by remember { mutableStateOf(controller.snapshot()) }
    val refresh = { state = controller.snapshot() }

    DisposableEffect(Unit) {
        val observer = DevController.observe(context) { refresh() }
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    // The grant happens outside the app (over adb), so re-check whenever we come back.
    LifecycleResumeEffect(Unit) {
        refresh()
        controller.refreshSurfaces()
        onPauseOrDispose { }
    }

    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                        Text(
                            "by Amurot",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { if (state.granted) GrantedBanner() else SetupCard(onCheck = refresh) }

            item { SectionTitle("One-tap switches") }
            item {
                AllThreeCard(
                    on = state.allOn,
                    enabled = state.granted,
                    onClick = { controller.setAll(!state.allOn); refresh() },
                )
            }
            item {
                ToggleGroup {
                    ToggleRow(Icons.Rounded.Code, "Developer options", "Shows the Developer options menu in Settings",
                        state.on[Toggle.DEV_OPTIONS] == true, state.granted) { controller.set(Toggle.DEV_OPTIONS, it); refresh() }
                    ToggleRow(Icons.Rounded.Usb, "USB debugging", "ADB over a cable. Also turns on Developer options",
                        state.on[Toggle.USB_DEBUGGING] == true, state.granted) { controller.set(Toggle.USB_DEBUGGING, it); refresh() }
                    ToggleRow(Icons.Rounded.Wifi, "Wireless debugging",
                        if (state.wifi) "ADB over Wi-Fi. Also turns on Developer options" else "Connect to Wi-Fi to use this",
                        state.on[Toggle.WIRELESS_DEBUGGING] == true, state.granted) { controller.set(Toggle.WIRELESS_DEBUGGING, it); refresh() }
                }
            }

            item { SectionTitle("More tools") }
            item {
                ToggleGroup {
                    ToggleRow(Icons.Rounded.Lightbulb, "Stay awake", "Screen never sleeps while charging",
                        state.on[Toggle.STAY_AWAKE] == true, state.granted) { controller.set(Toggle.STAY_AWAKE, it); refresh() }
                    ToggleRow(Icons.Rounded.TouchApp, "Show taps", "Visual feedback for touches, for demos and recordings",
                        state.on[Toggle.SHOW_TAPS] == true, state.granted) { controller.set(Toggle.SHOW_TAPS, it); refresh() }
                    AnimationRow(state.animationScale, state.granted) { controller.setAnimationScale(it); refresh() }
                }
            }

            item { SectionTitle("Quick Settings tiles") }
            item { AddTilesCard(context) }

            item { SectionTitle("Home-screen widgets") }
            item { AddWidgetsCard(context) }

            item { SectionTitle("About") }
            item { AboutCard(context) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp),
    )
}

@Composable
private fun GrantedBanner() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Ready", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    "WRITE_SECURE_SETTINGS is granted. Tiles and widgets work with one tap.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SetupCard(onCheck: () -> Unit) {
    val context = LocalContext.current
    val command = "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("One-time setup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Android only lets an app change these switches after you grant it WRITE_SECURE_SETTINGS from a computer. " +
                    "You do this once; it stays until you uninstall DevTools.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Step(1, "Turn on Developer options", "Settings › About phone › tap Build number 7 times.") {
                TextButton(onClick = { context.openSettings(Settings.ACTION_DEVICE_INFO_SETTINGS) }) { Text("Open About phone") }
            }
            Step(2, "Turn on USB debugging", "Settings › System › Developer options › USB debugging. On Xiaomi, also turn on USB debugging (Security settings).") {
                TextButton(onClick = { context.openSettings(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) }) { Text("Open Developer options") }
            }
            Step(3, "Connect and run this on your computer", "Install Android platform-tools, plug in the phone, accept the prompt, then run:") {
                CommandBox(command) { context.copy(command) }
            }
            Step(4, "Come back here", "DevTools picks up the grant automatically.") {
                FilledTonalButton(onClick = onCheck) { Text("Check again") }
            }
            Text(
                "Already use wireless debugging? \"adb pair\" and \"adb connect\" work too; no cable needed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Step(number: Int, title: String, body: String, action: @Composable () -> Unit) {
    Row {
        Box(
            Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            action()
        }
    }
}

@Composable
private fun CommandBox(command: String, onCopy: () -> Unit) {
    Row(
        Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.small)
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            command,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            softWrap = false,
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
        )
        IconButton(onClick = onCopy) { Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy command") }
    }
}

@Composable
private fun AllThreeCard(on: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = if (on) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    }
    Card(onClick = onClick, enabled = enabled, colors = colors, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Bolt, null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("All three", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    if (on) "Developer options, USB and wireless debugging are on. Tap to turn all off."
                    else "Tap to turn on Developer options, USB and wireless debugging together.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(if (on) "ON" else "OFF", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ToggleGroup(content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun AnimationRow(scale: Float, enabled: Boolean, onSelect: (Float) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Speed, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Animation scale", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Window, transition and animator scales together. The tile cycles 1x › 0.5x › Off.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        val options = listOf(0f, 0.5f, 1f, 2f)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, value ->
                SegmentedButton(
                    selected = scale == value,
                    onClick = { onSelect(value) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(AnimationScale.label(value)) }
            }
        }
    }
}

@Composable
private fun AddTilesCard(context: Context) {
    ToggleGroup {
        Text(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "Tap Add, or edit your Quick Settings panel and drag them in."
            else "Pull down Quick Settings, tap the pencil (edit) and drag the DevTools tiles in.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        TileRefresher.tiles.forEach { tile ->
            val label = stringResource(tile.label)
            AddRow(label) { context.requestTile(ComponentName(context, tile.service), label, tile.icon) }
        }
    }
}

@Composable
private fun AddWidgetsCard(context: Context) {
    ToggleGroup {
        Text(
            "A 4×1 panel, or single 1×1 buttons. Long-press your home screen › Widgets › DevTools also works.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        WidgetRefresher.widgets.forEach { widget ->
            AddRow(stringResource(widget.label), icon = Icons.Rounded.Widgets) { context.requestWidget(ComponentName(context, widget.provider)) }
        }
    }
}

@Composable
private fun AddRow(label: String, icon: ImageVector = Icons.Rounded.Add, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onAdd) { Text("Add") }
    }
}

@Composable
private fun AboutCard(context: Context) {
    ToggleGroup {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("DevTools ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
            Text(
                "Made by Amurot. Free and open source under the Apache License 2.0. No ads, no tracking, no internet access.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { context.openUrl(REPO_URL) }) {
                Icon(Icons.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Source code on GitHub")
            }
        }
    }
}

// ---- Android helpers ----

private fun Context.openSettings(action: String) {
    try {
        startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        copy(url)
    }
}

private fun Context.copy(text: String) {
    getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("DevTools", text))
    // Android 13+ shows its own clipboard confirmation.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
}

private fun Context.requestTile(component: ComponentName, label: String, icon: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val sbm = getSystemService(StatusBarManager::class.java)
        sbm?.requestAddTileService(component, label, Icon.createWithResource(this, icon), mainExecutor) { result ->
            if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                Toast.makeText(this, "$label is already in Quick Settings", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        Toast.makeText(this, "Edit Quick Settings and drag \"$label\" in", Toast.LENGTH_LONG).show()
    }
}

private fun Context.requestWidget(component: ComponentName) {
    val manager = AppWidgetManager.getInstance(this)
    if (manager.isRequestPinAppWidgetSupported) {
        manager.requestPinAppWidget(component, null, null)
    } else {
        Toast.makeText(this, "Long-press your home screen › Widgets › DevTools", Toast.LENGTH_LONG).show()
    }
}
