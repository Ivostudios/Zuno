package com.example.consolelauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.hardware.input.InputManager
import android.os.Build
import android.view.InputDevice
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class LauncherApp(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap,
    val isGame: Boolean,
)

class AppRepository(private val context: Context) {
    /** App installate con icona launcher, giochi per primi. */
    @Suppress("DEPRECATION")
    fun load(): List<LauncherApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { ri ->
                val info = ri.activityInfo.applicationInfo
                val isGame = info.category == ApplicationInfo.CATEGORY_GAME ||
                    (info.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                LauncherApp(
                    label = ri.loadLabel(pm).toString(),
                    packageName = ri.activityInfo.packageName,
                    icon = ri.loadIcon(pm).toBitmap(256, 256).asImageBitmap(),
                    isGame = isGame,
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(compareByDescending<LauncherApp> { it.isGame }.thenBy { it.label.lowercase() })
    }

    fun launch(app: LauncherApp) {
        context.packageManager.getLaunchIntentForPackage(app.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let(context::startActivity)
    }
}

data class ControllerInfo(val name: String, val batteryPercent: Int?)

/** Primo gamepad collegato (Bluetooth/USB), null se assente. */
fun readController(): ControllerInfo? {
    for (id in InputDevice.getDeviceIds()) {
        val d = InputDevice.getDevice(id) ?: continue
        val isPad = (d.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        if (d.isVirtual || !isPad) continue
        val battery = if (Build.VERSION.SDK_INT >= 31) {
            d.batteryState.takeIf { it.isPresent }?.capacity?.times(100)?.toInt()
        } else null
        return ControllerInfo(d.name, battery)
    }
    return null
}

/** Stato "in riproduzione". Collega qui MediaSessionManager + NotificationListenerService. */
data class NowPlaying(val title: String, val position: String, val progress: Float)

val sampleNowPlaying = NowPlaying("Muck Warfare", "0:58", 0.37f)
