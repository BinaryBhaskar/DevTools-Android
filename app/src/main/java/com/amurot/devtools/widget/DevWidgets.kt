package com.amurot.devtools.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.amurot.devtools.MainActivity
import com.amurot.devtools.R
import com.amurot.devtools.core.DevAction
import com.amurot.devtools.core.DevController
import com.amurot.devtools.tile.TileRefresher

/** A 1×1 one-tap widget for a single [DevAction]. */
abstract class ToggleWidgetProvider(val action: DevAction) : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = WidgetRefresher.toggleViews(context, action)
        ids.forEach { manager.updateAppWidget(it, views) }
        SettingsWatchJob.schedule(context)
    }

    override fun onEnabled(context: Context) = SettingsWatchJob.schedule(context)
}

class DevOptionsWidget : ToggleWidgetProvider(DevAction.DEV_OPTIONS)
class UsbDebuggingWidget : ToggleWidgetProvider(DevAction.USB_DEBUGGING)
class WirelessDebuggingWidget : ToggleWidgetProvider(DevAction.WIRELESS_DEBUGGING)
class AllWidget : ToggleWidgetProvider(DevAction.ALL)

/** A 4×1 panel with all four main switches. */
class PanelWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = WidgetRefresher.panelViews(context)
        ids.forEach { manager.updateAppWidget(it, views) }
        SettingsWatchJob.schedule(context)
    }

    override fun onEnabled(context: Context) = SettingsWatchJob.schedule(context)
}

/** Receives widget taps and runs the action. */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra(EXTRA_ACTION)?.let { runCatching { DevAction.valueOf(it) }.getOrNull() } ?: return
        DevController(context).perform(action)
    }

    companion object {
        const val EXTRA_ACTION = "com.amurot.devtools.extra.ACTION"
    }
}

object WidgetRefresher {
    data class WidgetInfo(val provider: Class<out AppWidgetProvider>, val label: Int, val preview: Int)

    val widgets = listOf(
        WidgetInfo(PanelWidget::class.java, R.string.widget_panel, R.drawable.ic_all),
        WidgetInfo(AllWidget::class.java, R.string.tile_all, R.drawable.ic_all),
        WidgetInfo(DevOptionsWidget::class.java, R.string.tile_dev_options, R.drawable.ic_dev_options),
        WidgetInfo(UsbDebuggingWidget::class.java, R.string.tile_usb, R.drawable.ic_usb),
        WidgetInfo(WirelessDebuggingWidget::class.java, R.string.tile_wireless, R.drawable.ic_wireless),
    )

    private val toggleProviders = mapOf(
        DevAction.DEV_OPTIONS to DevOptionsWidget::class.java,
        DevAction.USB_DEBUGGING to UsbDebuggingWidget::class.java,
        DevAction.WIRELESS_DEBUGGING to WirelessDebuggingWidget::class.java,
        DevAction.ALL to AllWidget::class.java,
    )

    val panelActions = listOf(DevAction.ALL, DevAction.DEV_OPTIONS, DevAction.USB_DEBUGGING, DevAction.WIRELESS_DEBUGGING)

    private val cellIds = listOf(
        Cell(R.id.cell_0, R.id.icon_0, R.id.label_0, R.id.status_0),
        Cell(R.id.cell_1, R.id.icon_1, R.id.label_1, R.id.status_1),
        Cell(R.id.cell_2, R.id.icon_2, R.id.label_2, R.id.status_2),
        Cell(R.id.cell_3, R.id.icon_3, R.id.label_3, R.id.status_3),
    )

    private data class Cell(val root: Int, val icon: Int, val label: Int, val status: Int)

    fun hasAnyWidget(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return widgets.any { manager.getAppWidgetIds(ComponentName(context, it.provider)).isNotEmpty() }
    }

    fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        toggleProviders.forEach { (action, provider) ->
            val ids = manager.getAppWidgetIds(ComponentName(context, provider))
            if (ids.isNotEmpty()) {
                val views = toggleViews(context, action)
                ids.forEach { manager.updateAppWidget(it, views) }
            }
        }
        val panelIds = manager.getAppWidgetIds(ComponentName(context, PanelWidget::class.java))
        if (panelIds.isNotEmpty()) {
            val views = panelViews(context)
            panelIds.forEach { manager.updateAppWidget(it, views) }
        }
        TileRefresher.refreshAll(context)
    }

    fun toggleViews(context: Context, action: DevAction): RemoteViews {
        val controller = DevController(context)
        return RemoteViews(context.packageName, R.layout.widget_toggle).apply {
            bindCell(context, controller, action, Cell(R.id.widget_root, R.id.widget_icon, R.id.widget_label, R.id.widget_status))
        }
    }

    fun panelViews(context: Context): RemoteViews {
        val controller = DevController(context)
        return RemoteViews(context.packageName, R.layout.widget_panel).apply {
            panelActions.forEachIndexed { index, action -> bindCell(context, controller, action, cellIds[index]) }
        }
    }

    private fun RemoteViews.bindCell(context: Context, controller: DevController, action: DevAction, cell: Cell) {
        val granted = controller.hasPermission()
        val on = granted && controller.isActive(action)
        val info = TileRefresher.tiles.first { it.action == action }
        val fg = context.getColor(if (on) R.color.widget_on_fg else R.color.widget_off_fg)
        setInt(cell.root, "setBackgroundResource", if (on) R.drawable.widget_bg_on else R.drawable.widget_bg_off)
        setImageViewResource(cell.icon, info.icon)
        setInt(cell.icon, "setColorFilter", fg)
        setTextViewText(cell.label, context.getString(widgetLabel(action)))
        setTextColor(cell.label, fg)
        setTextViewText(cell.status, controller.status(action))
        setTextColor(cell.status, fg)
        setOnClickPendingIntent(cell.root, clickIntent(context, action, granted))
        setContentDescription(cell.root, "${context.getString(info.label)}: ${controller.status(action)}")
    }

    private fun widgetLabel(action: DevAction): Int = when (action) {
        DevAction.DEV_OPTIONS -> R.string.widget_short_dev
        DevAction.USB_DEBUGGING -> R.string.widget_short_usb
        DevAction.WIRELESS_DEBUGGING -> R.string.widget_short_wireless
        DevAction.ALL -> R.string.widget_short_all
        else -> TileRefresher.tiles.first { it.action == action }.label
    }

    private fun clickIntent(context: Context, action: DevAction, granted: Boolean): PendingIntent {
        if (!granted) {
            val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(context, 1000 + action.ordinal, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val tap = Intent(context, WidgetActionReceiver::class.java).putExtra(WidgetActionReceiver.EXTRA_ACTION, action.name)
        return PendingIntent.getBroadcast(context, action.ordinal, tap, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
