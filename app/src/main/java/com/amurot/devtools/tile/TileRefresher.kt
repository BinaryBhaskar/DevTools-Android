package com.amurot.devtools.tile

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import com.amurot.devtools.R
import com.amurot.devtools.core.DevAction

/** Metadata for each tile, also used by the in-app "Add tile" buttons. */
data class TileInfo(val action: DevAction, val service: Class<out TileService>, val label: Int, val icon: Int)

object TileRefresher {
    val tiles = listOf(
        TileInfo(DevAction.DEV_OPTIONS, DevOptionsTileService::class.java, R.string.tile_dev_options, R.drawable.ic_dev_options),
        TileInfo(DevAction.USB_DEBUGGING, UsbDebuggingTileService::class.java, R.string.tile_usb, R.drawable.ic_usb),
        TileInfo(DevAction.WIRELESS_DEBUGGING, WirelessDebuggingTileService::class.java, R.string.tile_wireless, R.drawable.ic_wireless),
        TileInfo(DevAction.ALL, AllTileService::class.java, R.string.tile_all, R.drawable.ic_all),
        TileInfo(DevAction.STAY_AWAKE, StayAwakeTileService::class.java, R.string.tile_stay_awake, R.drawable.ic_stay_awake),
        TileInfo(DevAction.SHOW_TAPS, ShowTapsTileService::class.java, R.string.tile_show_taps, R.drawable.ic_show_taps),
        TileInfo(DevAction.ANIMATIONS, AnimationsTileService::class.java, R.string.tile_animations, R.drawable.ic_animations),
    )

    /** Asks the system to rebind every tile so it re-reads its state. */
    fun refreshAll(context: Context) {
        tiles.forEach {
            runCatching { TileService.requestListeningState(context, ComponentName(context, it.service)) }
        }
    }
}
