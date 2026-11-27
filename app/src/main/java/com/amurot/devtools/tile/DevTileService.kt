package com.amurot.devtools.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.amurot.devtools.MainActivity
import com.amurot.devtools.core.DevAction
import com.amurot.devtools.core.DevController

/** One Quick Settings tile per [DevAction]. Each subclass is declared in the manifest. */
abstract class DevTileService(private val action: DevAction) : TileService() {
    private val controller by lazy { DevController(this) }
    private var observer: ContentObserver? = null

    override fun onStartListening() {
        super.onStartListening()
        observer = DevController.observe(this) { render() }
        render()
    }

    override fun onStopListening() {
        observer?.let { contentResolver.unregisterContentObserver(it) }
        observer = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        if (!controller.hasPermission()) {
            openApp()
            return
        }
        controller.perform(action)
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val granted = controller.hasPermission()
        tile.state = when {
            !granted -> Tile.STATE_INACTIVE
            controller.isActive(action) -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.subtitle = controller.status(action)
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

class DevOptionsTileService : DevTileService(DevAction.DEV_OPTIONS)
class UsbDebuggingTileService : DevTileService(DevAction.USB_DEBUGGING)
class WirelessDebuggingTileService : DevTileService(DevAction.WIRELESS_DEBUGGING)
class AllTileService : DevTileService(DevAction.ALL)
class StayAwakeTileService : DevTileService(DevAction.STAY_AWAKE)
class AnimationsTileService : DevTileService(DevAction.ANIMATIONS)
