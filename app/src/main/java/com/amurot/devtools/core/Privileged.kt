package com.amurot.devtools.core

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.edit
import rikka.shizuku.Shizuku
import java.util.concurrent.TimeUnit

/**
 * Optional shell-level access through Shizuku or root.
 *
 * DevTools uses it to grant itself WRITE_SECURE_SETTINGS without a computer (`pm grant`).
 */
object Privileged {
    private const val TAG = "DevTools"
    private const val PREFS = "privileged"
    private const val KEY_ROOT = "root_enabled"
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    const val SHIZUKU_URL = "https://shizuku.rikka.app/"

    enum class ShizukuState { NOT_INSTALLED, NOT_RUNNING, NEEDS_PERMISSION, READY }

    fun shizukuState(context: Context): ShizukuState = when {
        shizukuReady() -> ShizukuState.READY
        shizukuRunning() -> ShizukuState.NEEDS_PERMISSION
        isInstalled(context, SHIZUKU_PACKAGE) -> ShizukuState.NOT_RUNNING
        else -> ShizukuState.NOT_INSTALLED
    }

    fun shizukuRunning(): Boolean = runCatching { Shizuku.pingBinder() && !Shizuku.isPreV11() }.getOrDefault(false)

    fun shizukuReady(): Boolean = shizukuRunning() &&
        runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)

    fun requestShizuku(requestCode: Int = 1) {
        runCatching { Shizuku.requestPermission(requestCode) }.onFailure { Log.w(TAG, "Shizuku request failed", it) }
    }

    /** Root is only used after the user opted in, so `su` never prompts unexpectedly. */
    fun rootEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ROOT, false)

    /** Asks `su` for access (may show the root manager's prompt). Blocking: call off the main thread. */
    fun enableRoot(context: Context): Boolean {
        val ok = runAsRoot("id") == 0
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_ROOT, ok) }
        return ok
    }

    /** Runs a shell command through Shizuku, or root. Returns true on exit code 0. */
    fun run(context: Context, command: String): Boolean {
        if (shizukuReady()) {
            val code = runWithShizuku(command)
            if (code == 0) return true
        }
        if (rootEnabled(context)) return runAsRoot(command) == 0
        return false
    }

    fun grantSecureSettings(context: Context): Boolean =
        run(context, "pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS")

    private fun runWithShizuku(command: String): Int = try {
        // Shizuku 13 hides newProcess behind its UserService API; the method is still there and
        // is the lightest way to run a one-off shell command.
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java,
        )
        method.isAccessible = true
        val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
        // ShizukuRemoteProcess has no timed waitFor; shell commands here return quickly.
        drain(process)
        process.waitFor()
    } catch (e: Throwable) {
        Log.w(TAG, "Shizuku command failed", e)
        -1
    }

    private fun runAsRoot(command: String): Int = try {
        val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
        drain(process)
        // A root manager prompt can wait for the user; don't block forever.
        if (process.waitFor(20, TimeUnit.SECONDS)) process.exitValue() else { process.destroy(); -1 }
    } catch (e: Throwable) {
        Log.w(TAG, "Root command failed", e)
        -1
    }

    /** Drains output so a chatty command can't block on a full pipe. */
    private fun drain(process: Process) {
        process.inputStream.bufferedReader().use { it.readText() }
    }

    private fun isInstalled(context: Context, pkg: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(pkg, 0); true }.getOrDefault(false)
}
